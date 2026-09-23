package shilploka.world.light;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

/**
 * WHY:
 * Realistic voxel sandbox atmospheres require light to propagate naturally: sunlight
 * (धूप / Dhoop) streaming down from the celestial sky into valleys and filtering through
 * leaves, subterranean caves remaining pitch black until lit, and placed oil lamps
 * (दीया / Diya) and volcanic magma (लावा / Lava) casting warm incandescent block light
 * (प्रकाश / Prakash).
 *
 * WHAT:
 * High-performance cellular automata lighting engine implementing Breadth-First Search (BFS)
 * flood fill for both 4-bit Sky Light (0 to 15) and 4-bit Block Light (0 to 15). Features
 * zero-allocation primitive ring queues, vertical sunlight casting, directional attenuation,
 * and light removal/re-propagation when blocks or light sources are altered.
 *
 * USED FOR:
 * Initializing lighting on newly generated chunk columns in background worker threads,
 * and recalculating local light gradients dynamically when players break or place blocks.
 *
 * WHERE NEXT:
 * GreedyMesher samples these light levels to shade voxel polygons and compute smooth
 * vertex ambient occlusion.
 */
public class LightingEngine {

    // Maximum queue capacity for localized BFS light waves (65,536 entries avoids heap resizing)
    private static final int QUEUE_CAPACITY = 65536;
    private static final int QUEUE_MASK = QUEUE_CAPACITY - 1;

    // Fast 6-directional orthogonal offset arrays (X, Y, Z)
    private static final int[] DX = { 1, -1,  0,  0,  0,  0 };
    private static final int[] DY = { 0,  0,  1, -1,  0,  0 };
    private static final int[] DZ = { 0,  0,  0,  0,  1, -1 };

    // Primitive ring buffers for light addition queue
    private final int[] addQueueX = new int[QUEUE_CAPACITY];
    private final int[] addQueueY = new int[QUEUE_CAPACITY];
    private final int[] addQueueZ = new int[QUEUE_CAPACITY];
    private final int[] addQueueLight = new int[QUEUE_CAPACITY];
    private int addHead = 0;
    private int addTail = 0;

    // Primitive ring buffers for light removal queue
    private final int[] removeQueueX = new int[QUEUE_CAPACITY];
    private final int[] removeQueueY = new int[QUEUE_CAPACITY];
    private final int[] removeQueueZ = new int[QUEUE_CAPACITY];
    private final int[] removeQueueLight = new int[QUEUE_CAPACITY];
    private int removeHead = 0;
    private int removeTail = 0;

    /**
     * WHY:
     * Initializes vertical sunlight columns and cave shadows for a newly generated chunk column.
     *
     * WHAT:
     * Traces downward from Y=319 for all 256 vertical columns in the chunk. As long as voxels
     * are transparent air, Sky Light is set to 15 (direct sunlight). When hitting solid terrain,
     * tracing halts, and perimeter nodes are queued for horizontal/downward BFS flood fill.
     *
     * @param column Target chunk column being populated.
     * @param world  Global lighting access for boundary neighbor queries.
     */
    public synchronized void initializeColumnSunlight(ChunkColumn column, WorldLightingAccess world) {
        int cx = column.getChunkX();
        int cz = column.getChunkZ();
        int baseX = cx << 4;
        int baseZ = cz << 4;

        addHead = 0;
        addTail = 0;

        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = baseX + lx;
                int wz = baseZ + lz;

                // Cast vertical sunlight column downwards from top of world
                for (int wy = ChunkColumn.CHUNK_HEIGHT - 1; wy >= 0; wy--) {
                    int block = column.getBlock(lx, wy, lz);
                    int absorption = BlockIds.getLightAbsorption(block);

                    if (absorption == 0) {
                        // Direct sunlight
                        column.setSkyLight(lx, wy, lz, 15);

                        // If bordering lower air or overhangs, enqueue for horizontal spread
                        if (wy < ChunkColumn.CHUNK_HEIGHT - 1) {
                            enqueueAdd(wx, wy, wz, 15);
                        }
                    } else if (absorption < 15) {
                        // Partially transparent block (e.g. leaves or shallow water)
                        int light = Math.max(0, 15 - absorption);
                        column.setSkyLight(lx, wy, lz, light);
                        if (light > 1) {
                            enqueueAdd(wx, wy, wz, light);
                        }
                    } else {
                        // Opaque solid bedrock/strata: column stops, beneath is in shadow
                        break;
                    }
                }
            }
        }

        // Spread sunlight into overhangs and caverns
        spreadSkyLight(world);
    }

    /**
     * WHY:
     * Scans a newly generated chunk column for intrinsic light-emitting blocks (Diyas, Lava)
     * and floods their block light outward.
     *
     * @param column Target chunk column.
     * @param world  Global lighting access.
     */
    public synchronized void initializeColumnBlockLight(ChunkColumn column, WorldLightingAccess world) {
        int cx = column.getChunkX();
        int cz = column.getChunkZ();
        int baseX = cx << 4;
        int baseZ = cz << 4;

        addHead = 0;
        addTail = 0;

        for (int sy = 0; sy < ChunkColumn.SECTION_COUNT; sy++) {
            var section = column.getSection(sy);
            if (section == null || section.isEmpty()) continue;

            int sectionBaseY = sy << 4;
            for (int ly = 0; ly < 16; ly++) {
                int wy = sectionBaseY + ly;
                for (int lz = 0; lz < 16; lz++) {
                    for (int lx = 0; lx < 16; lx++) {
                        int block = section.getBlock(lx, ly, lz);
                        int emission = BlockIds.getLightEmission(block);
                        if (emission > 0) {
                            section.setBlockLight(lx, ly, lz, emission);
                            enqueueAdd(baseX + lx, wy, baseZ + lz, emission);
                        }
                    }
                }
            }
        }

        spreadBlockLight(world);
    }

    /**
     * WHY:
     * Updates lighting grids when a block is placed by the player or generated dynamically.
     *
     * WHAT:
     * 1. If placed block is a light emitter (e.g. Diya=14, Lava=15): sets block light and spreads.
     * 2. If placed block is opaque solid: removes sky light and block light passing through the voxel,
     *    extinguishes shadow cones, and re-propagates surviving ambient light.
     */
    public synchronized void onBlockPlaced(int x, int y, int z, int blockId, WorldLightingAccess world) {
        int emission = BlockIds.getLightEmission(blockId);
        int absorption = BlockIds.getLightAbsorption(blockId);

        // 1. If light source was placed, flood its illumination outward
        if (emission > 0) {
            world.setBlockLight(x, y, z, emission);
            addHead = 0;
            addTail = 0;
            enqueueAdd(x, y, z, emission);
            spreadBlockLight(world);
        }

        // 2. If an opaque/absorbent block was placed, extinguish blocked light
        if (absorption > 0) {
            // Remove Block Light passing through this space
            int oldBlockLight = world.getBlockLight(x, y, z);
            if (oldBlockLight > 0) {
                removeHead = 0;
                removeTail = 0;
                addHead = 0;
                addTail = 0;
                enqueueRemove(x, y, z, oldBlockLight);
                world.setBlockLight(x, y, z, 0);
                unspreadBlockLight(world);
                spreadBlockLight(world);
            }

            // Remove Sky Light passing through this space
            int oldSkyLight = world.getSkyLight(x, y, z);
            if (oldSkyLight > 0) {
                removeHead = 0;
                removeTail = 0;
                addHead = 0;
                addTail = 0;
                enqueueRemove(x, y, z, oldSkyLight);
                world.setSkyLight(x, y, z, 0);
                unspreadSkyLight(world);
                spreadSkyLight(world);
            }
        }
    }

    /**
     * WHY:
     * Updates lighting grids when a block is broken by the player.
     *
     * WHAT:
     * 1. If broken block was a light emitter (Diya, Lava): extinguishes block light and re-propagates.
     * 2. If broken block was opaque: newly opened air voxel receives sky light and block light
     *    from brighter surrounding neighbors and floods inwards.
     */
    public synchronized void onBlockBroken(int x, int y, int z, int oldBlockId, WorldLightingAccess world) {
        int emission = BlockIds.getLightEmission(oldBlockId);

        // 1. Light emitter removed: extinguish its radiance
        if (emission > 0) {
            removeHead = 0;
            removeTail = 0;
            addHead = 0;
            addTail = 0;
            enqueueRemove(x, y, z, emission);
            world.setBlockLight(x, y, z, 0);
            unspreadBlockLight(world);
            spreadBlockLight(world);
        }

        // 2. Opaque barrier removed: propagate incident light into the newly opened voxel
        addHead = 0;
        addTail = 0;

        // Check if voxel has direct sky exposure from above
        boolean directSun = (y + 1 < ChunkColumn.CHUNK_HEIGHT && world.getSkyLight(x, y + 1, z) == 15);
        if (directSun) {
            // Direct sunlight penetrates down
            for (int wy = y; wy >= 0; wy--) {
                int b = world.getBlock(x, wy, z);
                int abs = BlockIds.getLightAbsorption(b);
                if (abs == 0) {
                    world.setSkyLight(x, wy, z, 15);
                    enqueueAdd(x, wy, z, 15);
                } else {
                    break;
                }
            }
        } else {
            // Find brightest neighboring sky light
            for (int i = 0; i < 6; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                int nz = z + DZ[i];
                if (ny >= 0 && ny < ChunkColumn.CHUNK_HEIGHT) {
                    int nSky = world.getSkyLight(nx, ny, nz);
                    if (nSky > 1) {
                        enqueueAdd(nx, ny, nz, nSky);
                    }
                }
            }
        }
        spreadSkyLight(world);

        // Check neighboring block light to illuminate opened hole
        addHead = 0;
        addTail = 0;
        for (int i = 0; i < 6; i++) {
            int nx = x + DX[i];
            int ny = y + DY[i];
            int nz = z + DZ[i];
            if (ny >= 0 && ny < ChunkColumn.CHUNK_HEIGHT) {
                int nBlock = world.getBlockLight(nx, ny, nz);
                if (nBlock > 1) {
                    enqueueAdd(nx, ny, nz, nBlock);
                }
            }
        }
        spreadBlockLight(world);
    }

    /**
     * Executes BFS flood fill for Sky Light across the world grid.
     */
    private void spreadSkyLight(WorldLightingAccess world) {
        while (addHead != addTail) {
            int x = addQueueX[addHead];
            int y = addQueueY[addHead];
            int z = addQueueZ[addHead];
            int light = addQueueLight[addHead];
            addHead = (addHead + 1) & QUEUE_MASK;

            if (light <= 1) continue;

            for (int i = 0; i < 6; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                int nz = z + DZ[i];

                if (ny < 0 || ny >= ChunkColumn.CHUNK_HEIGHT) continue;
                if (!world.isLoaded(nx >> 4, nz >> 4)) continue;

                int neighborBlock = world.getBlock(nx, ny, nz);
                int absorption = Math.max(1, BlockIds.getLightAbsorption(neighborBlock));
                int targetLight = light - absorption;

                if (targetLight > world.getSkyLight(nx, ny, nz)) {
                    world.setSkyLight(nx, ny, nz, targetLight);
                    enqueueAdd(nx, ny, nz, targetLight);
                }
            }
        }
    }

    /**
     * Executes BFS flood fill for Block Light across the world grid.
     */
    private void spreadBlockLight(WorldLightingAccess world) {
        while (addHead != addTail) {
            int x = addQueueX[addHead];
            int y = addQueueY[addHead];
            int z = addQueueZ[addHead];
            int light = addQueueLight[addHead];
            addHead = (addHead + 1) & QUEUE_MASK;

            if (light <= 1) continue;

            for (int i = 0; i < 6; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                int nz = z + DZ[i];

                if (ny < 0 || ny >= ChunkColumn.CHUNK_HEIGHT) continue;
                if (!world.isLoaded(nx >> 4, nz >> 4)) continue;

                int neighborBlock = world.getBlock(nx, ny, nz);
                int absorption = Math.max(1, BlockIds.getLightAbsorption(neighborBlock));
                int targetLight = light - absorption;

                if (targetLight > world.getBlockLight(nx, ny, nz)) {
                    world.setBlockLight(nx, ny, nz, targetLight);
                    enqueueAdd(nx, ny, nz, targetLight);
                }
            }
        }
    }

    /**
     * Executes BFS de-propagation when Sky Light paths are obstructed.
     */
    private void unspreadSkyLight(WorldLightingAccess world) {
        while (removeHead != removeTail) {
            int x = removeQueueX[removeHead];
            int y = removeQueueY[removeHead];
            int z = removeQueueZ[removeHead];
            int light = removeQueueLight[removeHead];
            removeHead = (removeHead + 1) & QUEUE_MASK;

            for (int i = 0; i < 6; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                int nz = z + DZ[i];

                if (ny < 0 || ny >= ChunkColumn.CHUNK_HEIGHT) continue;
                if (!world.isLoaded(nx >> 4, nz >> 4)) continue;

                int neighborLight = world.getSkyLight(nx, ny, nz);
                if (neighborLight != 0 && neighborLight < light) {
                    world.setSkyLight(nx, ny, nz, 0);
                    enqueueRemove(nx, ny, nz, neighborLight);
                } else if (neighborLight >= light) {
                    // Surviving independent light source
                    enqueueAdd(nx, ny, nz, neighborLight);
                }
            }
        }
    }

    /**
     * Executes BFS de-propagation when Block Light sources are extinguished.
     */
    private void unspreadBlockLight(WorldLightingAccess world) {
        while (removeHead != removeTail) {
            int x = removeQueueX[removeHead];
            int y = removeQueueY[removeHead];
            int z = removeQueueZ[removeHead];
            int light = removeQueueLight[removeHead];
            removeHead = (removeHead + 1) & QUEUE_MASK;

            for (int i = 0; i < 6; i++) {
                int nx = x + DX[i];
                int ny = y + DY[i];
                int nz = z + DZ[i];

                if (ny < 0 || ny >= ChunkColumn.CHUNK_HEIGHT) continue;
                if (!world.isLoaded(nx >> 4, nz >> 4)) continue;

                int neighborLight = world.getBlockLight(nx, ny, nz);
                if (neighborLight != 0 && neighborLight < light) {
                    world.setBlockLight(nx, ny, nz, 0);
                    enqueueRemove(nx, ny, nz, neighborLight);
                } else if (neighborLight >= light) {
                    // Surviving independent light source
                    enqueueAdd(nx, ny, nz, neighborLight);
                }
            }
        }
    }

    private void enqueueAdd(int x, int y, int z, int light) {
        int nextTail = (addTail + 1) & QUEUE_MASK;
        if (nextTail != addHead) {
            addQueueX[addTail] = x;
            addQueueY[addTail] = y;
            addQueueZ[addTail] = z;
            addQueueLight[addTail] = light;
            addTail = nextTail;
        }
    }

    private void enqueueRemove(int x, int y, int z, int light) {
        int nextTail = (removeTail + 1) & QUEUE_MASK;
        if (nextTail != removeHead) {
            removeQueueX[removeTail] = x;
            removeQueueY[removeTail] = y;
            removeQueueZ[removeTail] = z;
            removeQueueLight[removeTail] = light;
            removeTail = nextTail;
        }
    }
}
