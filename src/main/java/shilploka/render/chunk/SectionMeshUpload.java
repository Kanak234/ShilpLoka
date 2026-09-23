package shilploka.render.chunk;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * WHY:
 * OpenGL context is strictly bound to the main thread. Background worker threads
 * compute greedy meshing into native byte buffers, which must be safely transferred
 * to the main thread for GPU upload without stalls.
 *
 * WHAT:
 * Immutable carrier holding section coordinates, populated native buffers, and quad count.
 *
 * USED FOR:
 * Pushed to uploadQueue by worker threads and drained by main thread in ChunkManager.update().
 */
public record SectionMeshUpload(
        int chunkX,
        int sectionY,
        int chunkZ,
        FloatBuffer vertexBuffer,
        IntBuffer indexBuffer,
        int quadCount
) {}
