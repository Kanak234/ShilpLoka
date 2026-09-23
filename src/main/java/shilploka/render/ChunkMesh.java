package shilploka.render;

import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * WHY:
 * Renders a 16³ voxel chunk section efficiently by generating optimized polygonal geometry
 * with face-culling (only rendering faces bordering air or chunk edges) and uploading
 * vertex attributes directly into GPU VRAM via VAO/VBO.
 *
 * WHAT:
 * Scans a 16x16x16 block ID array, checks 6 face directions for exposure, builds quad
 * vertices with 3D positions, UV texture coordinates, texture array layer indices, and
 * directional light shading, and binds them into an OpenGL Vertex Array Object.
 *
 * USED FOR:
 * Instantiated for each active chunk section and rendered in the main render loop.
 *
 * WHERE NEXT:
 * Bound and drawn via glDrawElements() in Game.render() using the chunk shader.
 */
public class ChunkMesh {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkMesh.class);

    // Number of floats per vertex: Pos(3) + UV(2) + Layer(1) + Light(1) = 7 floats
    public static final int FLOATS_PER_VERTEX = 7;
    public static final int VERTEX_STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

    private int vaoId;
    private int vboId;
    private int eboId;
    private int indexCount = 0;
    private int quadCount = 0;

    /**
     * WHY:
     * Constructs a chunk mesh directly from a 16x16x16 block grid.
     *
     * WHAT:
     * Calls rebuildMesh() to generate vertex attributes and upload to the GPU.
     *
     * USED FOR:
     * Chunk loading and dynamic block modification re-meshing.
     */
    public ChunkMesh(int[][][] blockGrid) {
        rebuildMesh(blockGrid);
    }

    /**
     * WHY:
     * Evaluates exposed voxel faces and generates OpenGL geometry buffers.
     *
     * WHAT:
     * Iterates all 16x16x16 block positions, checks 6 face neighbors, collects visible
     * face quads, uploads them to VBO and EBO buffers, and configures vertex attributes.
     *
     * USED FOR:
     * Initial mesh creation and refreshing geometry when blocks are placed or broken.
     *
     * WHERE NEXT:
     * Geometry is stored in GPU memory and ready for immediate drawing via {@link #render()}.
     */
    public void rebuildMesh(int[][][] blockGrid) {
        // Clean up previous buffers if rebuilding
        if (vaoId != 0) {
            cleanup();
        }

        // Maximum possible visible faces in a 16³ chunk section: 16*16*16 * 6 = 24,576 faces
        // 4 vertices per quad * 7 floats = 28 floats per face; 6 indices per face
        // Pre-allocate temporary native buffers
        int maxQuads = 16 * 16 * 16 * 6;
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(maxQuads * 4 * FLOATS_PER_VERTEX);
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(maxQuads * 6);

        int currentQuad = 0;

        try {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int blockId = blockGrid[x][y][z];
                        if (blockId <= 0) continue; // Skip air blocks

                        float layerIndex = mapBlockToLayer(blockId);

                        // Check all 6 faces: +Y (Top), -Y (Bottom), +Z (North), -Z (South), +X (East), -X (West)
                        // Top (+Y)
                        if (isAir(blockGrid, x, y + 1, z)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x, y + 1, z + 1,   x + 1, y + 1, z + 1,   x + 1, y + 1, z,   x, y + 1, z,
                                    layerIndex, 1.0f); // Top is brightest
                        }
                        // Bottom (-Y)
                        if (isAir(blockGrid, x, y - 1, z)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x, y, z,   x + 1, y, z,   x + 1, y, z + 1,   x, y, z + 1,
                                    layerIndex, 0.5f); // Bottom receives minimum light
                        }
                        // North (+Z)
                        if (isAir(blockGrid, x, y, z + 1)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x, y, z + 1,   x + 1, y, z + 1,   x + 1, y + 1, z + 1,   x, y + 1, z + 1,
                                    layerIndex, 0.8f);
                        }
                        // South (-Z)
                        if (isAir(blockGrid, x, y, z - 1)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x + 1, y, z,   x, y, z,   x, y + 1, z,   x + 1, y + 1, z,
                                    layerIndex, 0.8f);
                        }
                        // East (+X)
                        if (isAir(blockGrid, x + 1, y, z)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x + 1, y, z + 1,   x + 1, y, z,   x + 1, y + 1, z,   x + 1, y + 1, z + 1,
                                    layerIndex, 0.65f);
                        }
                        // West (-X)
                        if (isAir(blockGrid, x - 1, y, z)) {
                            addFace(vertexBuffer, indexBuffer, currentQuad++,
                                    x, y, z,   x, y, z + 1,   x, y + 1, z + 1,   x, y + 1, z,
                                    layerIndex, 0.65f);
                        }
                    }
                }
            }

            this.quadCount = currentQuad;
            this.indexCount = currentQuad * 6;

            if (this.indexCount > 0) {
                vertexBuffer.flip();
                indexBuffer.flip();

                // Generate and bind VAO
                this.vaoId = glGenVertexArrays();
                glBindVertexArray(this.vaoId);

                // Upload Vertex Buffer
                this.vboId = glGenBuffers();
                glBindBuffer(GL_ARRAY_BUFFER, this.vboId);
                glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

                // Upload Index Buffer
                this.eboId = glGenBuffers();
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.eboId);
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

                // Attribute 0: Position (vec3)
                glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_STRIDE, 0);
                glEnableVertexAttribArray(0);

                // Attribute 1: UV coordinates (vec2)
                glVertexAttribPointer(1, 2, GL_FLOAT, false, VERTEX_STRIDE, 3 * Float.BYTES);
                glEnableVertexAttribArray(1);

                // Attribute 2: Texture Array Layer (float)
                glVertexAttribPointer(2, 1, GL_FLOAT, false, VERTEX_STRIDE, 5 * Float.BYTES);
                glEnableVertexAttribArray(2);

                // Attribute 3: Light Level & AO (float)
                glVertexAttribPointer(3, 1, GL_FLOAT, false, VERTEX_STRIDE, 6 * Float.BYTES);
                glEnableVertexAttribArray(3);

                glBindVertexArray(0);
                LOGGER.info("Built ChunkMesh: {} quads, {} vertices, {} indices",
                        this.quadCount, this.quadCount * 4, this.indexCount);
            }
        } finally {
            MemoryUtil.memFree(vertexBuffer);
            MemoryUtil.memFree(indexBuffer);
        }
    }

    /**
     * WHY:
     * Determines whether the neighboring block position is transparent air.
     *
     * WHAT:
     * Boundary check: if outside [0, 15], returns true (exterior face is visible).
     * Otherwise returns true if blockId == 0.
     */
    private boolean isAir(int[][][] grid, int x, int y, int z) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) {
            return true; // Assume air across chunk section boundaries for Phase P1
        }
        return grid[x][y][z] == 0;
    }

    /**
     * WHY:
     * Appends 4 vertex records and 6 triangle index references to native buffers.
     *
     * WHAT:
     * Pushes vertex positions, UVs (0,0 to 1,1), layer index, and light coefficient,
     * then links two triangles (0-1-2 and 2-3-0) for the quad.
     */
    private void addFace(FloatBuffer vBuf, IntBuffer iBuf, int quadIndex,
                        float x0, float y0, float z0,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float layer, float light) {
        // Vertex 0 (UV: 0.0, 1.0)
        vBuf.put(x0).put(y0).put(z0).put(0.0f).put(1.0f).put(layer).put(light);
        // Vertex 1 (UV: 1.0, 1.0)
        vBuf.put(x1).put(y1).put(z1).put(1.0f).put(1.0f).put(layer).put(light);
        // Vertex 2 (UV: 1.0, 0.0)
        vBuf.put(x2).put(y2).put(z2).put(1.0f).put(0.0f).put(layer).put(light);
        // Vertex 3 (UV: 0.0, 0.0)
        vBuf.put(x3).put(y3).put(z3).put(0.0f).put(0.0f).put(layer).put(light);

        // Two triangles forming the rectangular quad: 0-1-2 and 2-3-0
        int base = quadIndex * 4;
        iBuf.put(base).put(base + 1).put(base + 2);
        iBuf.put(base + 2).put(base + 3).put(base);
    }

    /**
     * WHY:
     * Maps raw integer block IDs to their corresponding TextureArray layer slice.
     *
     * WHAT:
     * Translates block ID integers into TextureArray constants.
     */
    private float mapBlockToLayer(int blockId) {
        return switch (blockId) {
            case 1 -> TextureArray.LAYER_STONE;
            case 2 -> TextureArray.LAYER_GRANITE;
            case 3 -> TextureArray.LAYER_RED_SANDSTONE;
            case 4 -> TextureArray.LAYER_ALLUVIAL_SOIL;
            case 5 -> TextureArray.LAYER_YELLOW_SANDSTONE;
            case 6 -> TextureArray.LAYER_DECCAN_BASALT;
            case 7 -> TextureArray.LAYER_FIRED_BRICK;
            case 8 -> TextureArray.LAYER_DIYA;
            case 9 -> TextureArray.LAYER_SAL_LOG_SIDE;
            case 10 -> TextureArray.LAYER_SAL_PLANKS;
            default -> TextureArray.LAYER_STONE;
        };
    }

    /**
     * WHY:
     * Executes the OpenGL draw call for this chunk mesh.
     *
     * WHAT:
     * Binds VAO and invokes glDrawElements with GL_TRIANGLES.
     *
     * USED FOR:
     * Called by the render loop in Game.render().
     */
    public void render() {
        if (this.indexCount == 0 || this.vaoId == 0) return;

        glBindVertexArray(this.vaoId);
        glDrawElements(GL_TRIANGLES, this.indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * WHY:
     * Deletes OpenGL buffers and frees GPU memory.
     *
     * WHAT:
     * Calls glDeleteVertexArrays and glDeleteBuffers.
     */
    public void cleanup() {
        if (this.vaoId != 0) {
            glDeleteVertexArrays(this.vaoId);
            glDeleteBuffers(this.vboId);
            glDeleteBuffers(this.eboId);
            this.vaoId = 0;
            this.vboId = 0;
            this.eboId = 0;
            this.indexCount = 0;
            this.quadCount = 0;
        }
    }

    public int getQuadCount() { return quadCount; }
    public int getIndexCount() { return indexCount; }
}
