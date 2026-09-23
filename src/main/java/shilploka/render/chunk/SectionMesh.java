package shilploka.render.chunk;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * WHY:
 * Represents the uploaded OpenGL geometry buffers (VAO, VBO, EBO) for an individual 16³
 * chunk section in world space, enabling precise section-level frustum culling.
 *
 * WHAT:
 * Manages GPU vertex and index buffers, tracks bounding box coordinates (min/max X, Y, Z),
 * and issues glDrawElements during rendering.
 *
 * USED FOR:
 * Managed inside ChunkManager and rendered per frame.
 *
 * WHERE NEXT:
 * Drawn via SectionMesh.render() in ChunkManager.render().
 */
public class SectionMesh {

    public static final int FLOATS_PER_VERTEX = 10;
    public static final int VERTEX_STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

    private final int chunkX;
    private final int sectionY;
    private final int chunkZ;

    // World-space bounding box
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;

    private int vaoId;
    private int vboId;
    private int eboId;
    private int indexCount = 0;
    private int quadCount = 0;

    public SectionMesh(int chunkX, int sectionY, int chunkZ) {
        this.chunkX = chunkX;
        this.sectionY = sectionY;
        this.chunkZ = chunkZ;

        this.minX = chunkX * 16.0f;
        this.minY = sectionY * 16.0f;
        this.minZ = chunkZ * 16.0f;

        this.maxX = this.minX + 16.0f;
        this.maxY = this.minY + 16.0f;
        this.maxZ = this.minZ + 16.0f;
    }

    /**
     * WHY:
     * Uploads greedy-meshed vertex and index data from native buffers to GPU VRAM.
     *
     * WHAT:
     * Generates VAO, VBO, EBO handles and configures vertex attributes on the main thread.
     */
    public void upload(FloatBuffer vertexBuffer, IntBuffer indexBuffer, int quadCount) {
        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable t) {
            // Headless unit test environment: skip GPU upload
            return;
        }

        cleanup(); // Clean up existing handles if re-uploading

        this.quadCount = quadCount;
        this.indexCount = quadCount * 6;

        if (this.indexCount == 0) return;

        this.vaoId = glGenVertexArrays();
        glBindVertexArray(this.vaoId);

        this.vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        this.eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, this.eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Attribute 0: Position (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_STRIDE, 0);
        glEnableVertexAttribArray(0);

        // Attribute 1: UV (vec2)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, VERTEX_STRIDE, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Attribute 2: Layer (float)
        glVertexAttribPointer(2, 1, GL_FLOAT, false, VERTEX_STRIDE, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Attribute 3: LightData (vec4: SkyLight, BlockLight, AmbientOcclusion, DirectionalShade)
        glVertexAttribPointer(3, 4, GL_FLOAT, false, VERTEX_STRIDE, 6 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
    }

    /**
     * WHY:
     * Draws the section mesh.
     */
    public void render() {
        if (this.indexCount == 0 || this.vaoId == 0) return;

        glBindVertexArray(this.vaoId);
        glDrawElements(GL_TRIANGLES, this.indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * WHY:
     * Releases OpenGL buffers.
     */
    public void cleanup() {
        try {
            org.lwjgl.opengl.GL.getCapabilities();
        } catch (Throwable t) {
            this.vaoId = 0;
            this.vboId = 0;
            this.eboId = 0;
            this.indexCount = 0;
            this.quadCount = 0;
            return;
        }

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

    public int getChunkX() { return chunkX; }
    public int getSectionY() { return sectionY; }
    public int getChunkZ() { return chunkZ; }
    public int getQuadCount() { return quadCount; }
    public boolean hasMesh() { return vaoId != 0 && indexCount > 0; }
}
