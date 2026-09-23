package shilploka.world.chunk;

import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.render.Camera;
import shilploka.render.FrustumCuller;
import shilploka.render.Shader;
import shilploka.render.chunk.GreedyMesher;
import shilploka.render.chunk.SectionMesh;
import shilploka.render.chunk.SectionMeshUpload;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;
import shilploka.world.WorldGenerator;
import shilploka.world.fluid.FluidSimulator;
import shilploka.world.light.LightingEngine;
import shilploka.world.light.WorldLightingAccess;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * WHY:
 * Infinite horizontal voxel worlds require streaming chunk columns in and out around the player,
 * offloading generation and greedy meshing onto background worker threads, uploading completed
 * meshes without main-thread frame hitches, and discarding distant geometry to stay strictly
 * within the 2.0 GB heap ceiling.
 *
 * WHAT:
 * Manages active ChunkColumn instances in a spatial map, schedules multi-threaded terrain
 * generation, cellular lighting propagation (Dhoop & Prakash), and greedy meshing tasks
 * via a dedicated worker pool, stages GPU mesh uploads on a thread-safe queue, and executes
 * frustum-culled rendering per frame.
 *
 * USED FOR:
 * Core world streaming manager in Game.
 *
 * WHERE NEXT:
 * In Game.render(), ChunkManager.render() draws all visible section meshes through the chunk shader.
 */
public class ChunkManager implements WorldLightingAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkManager.class);

    public static final int DEFAULT_RENDER_DISTANCE = 4; // Radius in chunks (4 chunks = 9x9 = 81 columns)
    private static final int MAX_UPLOADS_PER_FRAME = 24;  // Cap GPU uploads per frame to prevent micro-stutter

    private final WorldGenerator worldGen;
    private shilploka.world.save.SaveManager saveManager;
    private shilploka.world.dimension.DimensionManager dimensionManager;
    private final LightingEngine lightingEngine = new LightingEngine();
    private final FluidSimulator fluidSimulator = new FluidSimulator();
    private int fluidTickTimer = 0;

    private final Map<Long, ChunkColumn> loadedChunks = new ConcurrentHashMap<>();
    private final Set<Long> generatingChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Long, SectionMesh> sectionMeshes = new HashMap<>(); // Main-thread only

    // Asynchronous worker pool & upload queue
    private final ExecutorService workerPool;
    private final ConcurrentLinkedQueue<SectionMeshUpload> uploadQueue = new ConcurrentLinkedQueue<>();

    // Real-time telemetry counters for F3 display
    private int renderedSections = 0;
    private int renderedQuads = 0;
    private int culledSections = 0;
    private int totalLoadedMeshes = 0;

    public ChunkManager(WorldGenerator worldGen) {
        this(worldGen, null);
    }

    public ChunkManager(WorldGenerator worldGen, shilploka.world.save.SaveManager saveManager) {
        this.worldGen = worldGen;
        this.saveManager = saveManager;
        int workerCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.workerPool = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r, "Chunk-Worker");
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("Initialized ChunkManager with {} background worker threads", workerCount);
    }

    public shilploka.world.dimension.DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    public void setDimensionManager(shilploka.world.dimension.DimensionManager dimensionManager) {
        this.dimensionManager = dimensionManager;
    }

    public void unloadAllChunks() {
        loadedChunks.clear();
        for (SectionMesh mesh : sectionMeshes.values()) {
            if (mesh != null) mesh.cleanup();
        }
        sectionMeshes.clear();
    }

    public shilploka.world.save.SaveManager getSaveManager() {
        return saveManager;
    }

    public void setSaveManager(shilploka.world.save.SaveManager saveManager) {
        this.saveManager = saveManager;
    }

    /**
     * Coordinate packing: maps (chunkX, chunkZ) to a 64-bit primitive long.
     */
    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    /**
     * Coordinate packing: maps (chunkX, sectionY, chunkZ) to a 64-bit primitive long.
     */
    public static long sectionKey(int cx, int sy, int cz) {
        return (((long) cx) << 42) | (((long) (sy & 0x3F)) << 32) | (cz & 0xFFFFFFFFL);
    }

    /**
     * WHY:
     * Dispatches background chunk generation/meshing tasks and drains finished GPU uploads.
     *
     * WHAT:
     * 1. Finds camera chunk coordinates.
     * 2. Schedules generation for new chunks within render distance.
     * 3. Drains up to MAX_UPLOADS_PER_FRAME from uploadQueue into OpenGL VAO/VBOs.
     * 4. Unloads chunks beyond render distance + padding.
     *
     * USED FOR:
     * Called on the main thread once per frame in Game.update().
     */
    public void update(Camera camera, int renderDistance) {
        int playerChunkX = (int) Math.floor(camera.getPosition().x) >> 4;
        int playerChunkZ = (int) Math.floor(camera.getPosition().z) >> 4;

        // 1. Schedule background generation for missing chunks in radius
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                if (dx * dx + dz * dz > renderDistance * renderDistance) continue; // Circular radius

                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                long key = chunkKey(cx, cz);

                if (!loadedChunks.containsKey(key) && generatingChunks.add(key)) {
                    workerPool.submit(() -> generateAndMeshChunk(cx, cz));
                }
            }
        }

        // 2. Main Thread: Drain completed mesh uploads and upload to GPU
        int uploadsHandled = 0;
        while (uploadsHandled < MAX_UPLOADS_PER_FRAME) {
            SectionMeshUpload upload = uploadQueue.poll();
            if (upload == null) {
                break;
            }
            long sKey = sectionKey(upload.chunkX(), upload.sectionY(), upload.chunkZ());
            SectionMesh mesh = sectionMeshes.computeIfAbsent(sKey,
                    k -> new SectionMesh(upload.chunkX(), upload.sectionY(), upload.chunkZ()));

            mesh.upload(upload.vertexBuffer(), upload.indexBuffer(), upload.quadCount());

            // Free native memory allocated on the worker thread
            MemoryUtil.memFree(upload.vertexBuffer());
            MemoryUtil.memFree(upload.indexBuffer());

            uploadsHandled++;
        }

        // 3. Fluid simulation periodic tick (~4 times per second)
        fluidTickTimer++;
        if (fluidTickTimer >= 5) {
            fluidSimulator.tick(this);
            fluidTickTimer = 0;
        }

        // 4. Unload distant chunks (beyond render distance + 2 chunks)
        int unloadDistSq = (renderDistance + 2) * (renderDistance + 2);
        loadedChunks.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            ChunkColumn column = entry.getValue();
            int cx = (int) (key >> 32);
            int cz = (int) (long) key;
            int distSq = (cx - playerChunkX) * (cx - playerChunkX) + (cz - playerChunkZ) * (cz - playerChunkZ);

            if (distSq > unloadDistSq) {
                if (saveManager != null && column != null && column.isDirty()) {
                    try {
                        saveManager.saveChunk(column);
                    } catch (Exception e) {
                        LOGGER.error("Failed to save chunk ({}, {}) during unload: {}", cx, cz, e.getMessage());
                    }
                }

                // Remove all associated section meshes on the main thread
                for (int sy = 0; sy < ChunkColumn.SECTION_COUNT; sy++) {
                    long sKey = sectionKey(cx, sy, cz);
                    SectionMesh mesh = sectionMeshes.remove(sKey);
                    if (mesh != null) {
                        mesh.cleanup();
                    }
                }
                return true;
            }
            return false;
        });

        this.totalLoadedMeshes = sectionMeshes.size();
    }

    /**
     * WHY:
     * Worker thread execution: loads chunk from disk or generates terrain blocks, computes
     * cellular lighting, and meshes sections.
     *
     * WHAT:
     * Checks SaveManager for saved chunk data; falls back to WorldGenerator if absent,
     * initializes sunlight columns and block light, sweeps non-empty sections
     * with GreedyMesher (passing ambient occlusion and light data), and pushes payloads to uploadQueue.
     */
    private void generateAndMeshChunk(int cx, int cz) {
        try {
            ChunkColumn column = null;
            if (saveManager != null) {
                try {
                    column = saveManager.loadChunk(cx, cz);
                } catch (Exception e) {
                    LOGGER.warn("Failed to load chunk at ({}, {}) from disk, regenerating: {}", cx, cz, e.getMessage());
                }
            }

            if (column == null) {
                if (dimensionManager != null && dimensionManager.getCurrentDimension() != shilploka.world.dimension.DimensionType.PRITHVI) {
                    column = new ChunkColumn(cx, cz);
                    dimensionManager.populateChunk(column, worldGen);
                } else {
                    column = worldGen.generateChunkColumn(cx, cz);
                }
                // Initialize cellular lighting (Dhoop & Prakash)
                lightingEngine.initializeColumnSunlight(column, this);
                lightingEngine.initializeColumnBlockLight(column, this);
            }

            loadedChunks.put(chunkKey(cx, cz), column);

            // Mesh all non-empty sections in this column
            for (int sy = 0; sy < ChunkColumn.SECTION_COUNT; sy++) {
                ChunkSection section = column.getSection(sy);
                if (section == null || section.isEmpty()) continue;

                // Allocate native buffers for meshing
                int maxQuads = 16 * 16 * 16 * 6;
                FloatBuffer vBuf = MemoryUtil.memAllocFloat(maxQuads * 4 * SectionMesh.FLOATS_PER_VERTEX);
                IntBuffer iBuf = MemoryUtil.memAllocInt(maxQuads * 6);

                int quads = GreedyMesher.meshSection(section, column, this, vBuf, iBuf);
                if (quads > 0) {
                    vBuf.flip();
                    iBuf.flip();
                    uploadQueue.add(new SectionMeshUpload(cx, sy, cz, vBuf, iBuf, quads));
                } else {
                    MemoryUtil.memFree(vBuf);
                    MemoryUtil.memFree(iBuf);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Error generating/loading chunk at ({}, {})", cx, cz, t);
        } finally {
            generatingChunks.remove(chunkKey(cx, cz));
        }
    }

    /**
     * WHY:
     * Renders all visible chunk sections with GPU frustum culling.
     *
     * WHAT:
     * Evaluates SectionMesh AABB against frustum planes. Skips culled sections,
     * updates u_ChunkOffset uniform, and executes draw calls for visible sections.
     *
     * USED FOR:
     * Called in Game.render().
     */
    public void render(Shader shader, FrustumCuller culler) {
        renderedSections = 0;
        renderedQuads = 0;
        culledSections = 0;

        for (SectionMesh mesh : sectionMeshes.values()) {
            if (!mesh.hasMesh()) continue;

            // Frustum Culling: Test section bounding box against 6 view planes
            if (!culler.isBoxVisible(mesh.minX, mesh.minY, mesh.minZ, mesh.maxX, mesh.maxY, mesh.maxZ)) {
                culledSections++;
                continue;
            }

            // Bind section world offset
            shader.setVec3("u_ChunkOffset", mesh.minX, mesh.minY, mesh.minZ);

            mesh.render();
            renderedSections++;
            renderedQuads += mesh.getQuadCount();
        }
    }

    /**
     * WHY:
     * Releases worker threads, frees queued native buffers, and cleans up OpenGL meshes.
     */
    public void cleanup() {
        LOGGER.info("Shutting down ChunkManager worker pool...");
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(2, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Save any remaining dirty chunks before discarding
        if (saveManager != null) {
            for (ChunkColumn column : loadedChunks.values()) {
                if (column != null && column.isDirty()) {
                    try {
                        saveManager.saveChunk(column);
                    } catch (Exception e) {
                        LOGGER.error("Error saving chunk at ({}, {}) during shutdown: {}",
                                column.getChunkX(), column.getChunkZ(), e.getMessage());
                    }
                }
            }
        }

        // Drain any remaining buffers in uploadQueue and free native memory
        SectionMeshUpload upload;
        while ((upload = uploadQueue.poll()) != null) {
            MemoryUtil.memFree(upload.vertexBuffer());
            MemoryUtil.memFree(upload.indexBuffer());
        }

        // Clean up all GPU meshes
        for (SectionMesh mesh : sectionMeshes.values()) {
            mesh.cleanup();
        }
        sectionMeshes.clear();
        loadedChunks.clear();
        generatingChunks.clear();

        LOGGER.info("ChunkManager disposed successfully.");
    }

    @Override
    public int getBlock(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= ChunkColumn.CHUNK_HEIGHT) {
            return BlockIds.AIR;
        }
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.get(chunkKey(cx, cz));
        if (column == null) {
            return BlockIds.AIR;
        }
        int lx = worldX & 15;
        int lz = worldZ & 15;
        return column.getBlock(lx, worldY, lz);
    }

    /**
     * WHY:
     * Enables dynamic player voxel modifications (breaking and placing blocks).
     *
     * WHAT:
     * Mutates the targeted ChunkColumn voxel state, and immediately re-meshes
     * the affected section (and adjacent sections if boundary voxels were modified).
     */
    public boolean setBlock(int worldX, int worldY, int worldZ, int blockId) {
        return setBlock(worldX, worldY, worldZ, blockId, true);
    }

    /**
     * WHY:
     * Enables dynamic player voxel modifications (breaking and placing blocks).
     *
     * WHAT:
     * Mutates the targeted ChunkColumn voxel state, and immediately re-meshes
     * the affected section (and adjacent sections if boundary voxels were modified).
     */
    public boolean setBlock(int worldX, int worldY, int worldZ, int blockId, boolean triggerPhysics) {
        if (worldY < 0 || worldY >= ChunkColumn.CHUNK_HEIGHT) {
            return false;
        }
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.computeIfAbsent(chunkKey(cx, cz), k -> {
            if (dimensionManager != null && dimensionManager.getCurrentDimension() != shilploka.world.dimension.DimensionType.PRITHVI) {
                ChunkColumn col = new ChunkColumn(cx, cz);
                dimensionManager.populateChunk(col, worldGen);
                return col;
            }
            return worldGen.generateChunkColumn(cx, cz);
        });

        int lx = worldX & 15;
        int lz = worldZ & 15;
        int oldBlockId = column.getBlock(lx, worldY, lz);
        if (oldBlockId == blockId) {
            return false;
        }

        column.setBlock(lx, worldY, lz, blockId);

        // Update cellular lighting (Dhoop & Prakash)
        if (blockId == BlockIds.AIR) {
            lightingEngine.onBlockBroken(worldX, worldY, worldZ, oldBlockId, this);
        } else {
            lightingEngine.onBlockPlaced(worldX, worldY, worldZ, blockId, this);
        }

        if (triggerPhysics) {
            // Falling block gravity simulation (Thar Sand & Bajri)
            fluidSimulator.checkAndApplyFalling(worldX, worldY, worldZ, this);
            if (worldY + 1 < ChunkColumn.CHUNK_HEIGHT) {
                fluidSimulator.checkAndApplyFalling(worldX, worldY + 1, worldZ, this);
            }

            // Cellular automata fluid simulation trigger
            if (BlockIds.isFluid(blockId)) {
                fluidSimulator.scheduleFluidUpdate(worldX, worldY, worldZ, blockId, 8);
            }
        }

        // Immediate remesh of the modified section on the main thread
        int sy = worldY >> 4;
        remeshSectionSync(column, cx, sy, cz);

        // Remesh boundary neighbors if necessary
        if ((worldY & 15) == 0 && sy > 0) remeshSectionSync(column, cx, sy - 1, cz);
        if ((worldY & 15) == 15 && sy < ChunkColumn.SECTION_COUNT - 1) remeshSectionSync(column, cx, sy + 1, cz);

        // Horizontal neighbor remeshing if on chunk edge
        if (lx == 0) remeshNeighborSectionSync(cx - 1, sy, cz);
        if (lx == 15) remeshNeighborSectionSync(cx + 1, sy, cz);
        if (lz == 0) remeshNeighborSectionSync(cx, sy, cz - 1);
        if (lz == 15) remeshNeighborSectionSync(cx, sy, cz + 1);

        return true;
    }

    private void remeshNeighborSectionSync(int ncx, int nsy, int ncz) {
        ChunkColumn neighbor = loadedChunks.get(chunkKey(ncx, ncz));
        if (neighbor != null) {
            remeshSectionSync(neighbor, ncx, nsy, ncz);
        }
    }

    private void remeshSectionSync(ChunkColumn column, int cx, int sy, int cz) {
        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable t) {
            // Headless unit test environment: skip GPU remeshing
            return;
        }

        ChunkSection section = column.getSection(sy);
        long sKey = sectionKey(cx, sy, cz);
        SectionMesh mesh = sectionMeshes.computeIfAbsent(sKey, k -> new SectionMesh(cx, sy, cz));

        if (section.isEmpty()) {
            mesh.cleanup();
            return;
        }

        int maxQuads = 16 * 16 * 16 * 6;
        FloatBuffer vBuf = MemoryUtil.memAllocFloat(maxQuads * 4 * SectionMesh.FLOATS_PER_VERTEX);
        IntBuffer iBuf = MemoryUtil.memAllocInt(maxQuads * 6);
        try {
            int quads = GreedyMesher.meshSection(section, column, this, vBuf, iBuf);
            if (quads > 0) {
                vBuf.flip();
                iBuf.flip();
                mesh.upload(vBuf, iBuf, quads);
            } else {
                mesh.cleanup();
            }
        } finally {
            MemoryUtil.memFree(vBuf);
            MemoryUtil.memFree(iBuf);
        }
    }

    @Override
    public int getSkyLight(int worldX, int worldY, int worldZ) {
        if (worldY < 0) return 0;
        if (worldY >= ChunkColumn.CHUNK_HEIGHT) return 15;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.get(chunkKey(cx, cz));
        if (column == null) return 15;
        return column.getSkyLight(worldX & 15, worldY, worldZ & 15);
    }

    @Override
    public int getBlockLight(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= ChunkColumn.CHUNK_HEIGHT) return 0;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.get(chunkKey(cx, cz));
        if (column == null) return 0;
        return column.getBlockLight(worldX & 15, worldY, worldZ & 15);
    }

    @Override
    public void setSkyLight(int worldX, int worldY, int worldZ, int value) {
        if (worldY < 0 || worldY >= ChunkColumn.CHUNK_HEIGHT) return;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.get(chunkKey(cx, cz));
        if (column != null) {
            column.setSkyLight(worldX & 15, worldY, worldZ & 15, value);
        }
    }

    @Override
    public void setBlockLight(int worldX, int worldY, int worldZ, int value) {
        if (worldY < 0 || worldY >= ChunkColumn.CHUNK_HEIGHT) return;
        int cx = worldX >> 4;
        int cz = worldZ >> 4;
        ChunkColumn column = loadedChunks.get(chunkKey(cx, cz));
        if (column != null) {
            column.setBlockLight(worldX & 15, worldY, worldZ & 15, value);
        }
    }

    @Override
    public boolean isLoaded(int cx, int cz) {
        return loadedChunks.containsKey(chunkKey(cx, cz));
    }

    public LightingEngine getLightingEngine() {
        return lightingEngine;
    }

    public FluidSimulator getFluidSimulator() {
        return fluidSimulator;
    }

    public Collection<ChunkColumn> getLoadedChunks() {
        return loadedChunks.values();
    }

    public void saveAllChunks() {
        if (saveManager == null) return;
        for (ChunkColumn column : loadedChunks.values()) {
            if (column != null && column.isDirty()) {
                try {
                    saveManager.saveChunk(column);
                } catch (Exception e) {
                    LOGGER.error("Failed to save chunk at ({}, {}): {}",
                            column.getChunkX(), column.getChunkZ(), e.getMessage());
                }
            }
        }
    }

    public int getLoadedChunkCount() { return loadedChunks.size(); }
    public int getTotalLoadedMeshes() { return totalLoadedMeshes; }
    public int getRenderedSections() { return renderedSections; }
    public int getRenderedQuads() { return renderedQuads; }
    public int getCulledSections() { return culledSections; }
}
