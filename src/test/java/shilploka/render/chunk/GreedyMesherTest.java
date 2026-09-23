package shilploka.render.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;
import shilploka.world.chunk.ChunkSection;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies GreedyMesher algorithm accuracy, face culling, and rectangular quad merging.
 *
 * WHAT:
 * Tests that an empty section produces 0 quads, a single 1x1x1 cube produces 6 quads,
 * and a flat 16x16x1 slab of identical blocks merges 576 naive cube faces into exactly 6 quads
 * (1 top, 1 bottom, 4 sides), achieving dramatic polygon reduction.
 *
 * USED FOR:
 * Automated CI verification of voxel mesh optimization.
 */
class GreedyMesherTest {

    @Test
    @DisplayName("Empty chunk section generates zero quads")
    void testEmptySectionProducesZeroQuads() {
        ChunkSection section = new ChunkSection(0);
        FloatBuffer vBuf = MemoryUtil.memAllocFloat(1024);
        IntBuffer iBuf = MemoryUtil.memAllocInt(1024);

        try {
            int quads = GreedyMesher.meshSection(section, vBuf, iBuf);
            assertEquals(0, quads);
            assertEquals(0, vBuf.position());
            assertEquals(0, iBuf.position());
        } finally {
            MemoryUtil.memFree(vBuf);
            MemoryUtil.memFree(iBuf);
        }
    }

    @Test
    @DisplayName("Single isolated 1x1x1 cube generates exactly 6 individual face quads")
    void testSingleVoxelProducesSixQuads() {
        ChunkSection section = new ChunkSection(0);
        section.setBlock(8, 8, 8, 1); // 1 = Stone

        FloatBuffer vBuf = MemoryUtil.memAllocFloat(4096);
        IntBuffer iBuf = MemoryUtil.memAllocInt(4096);

        try {
            int quads = GreedyMesher.meshSection(section, vBuf, iBuf);
            assertEquals(6, quads, "An isolated cube must expose all 6 cardinal faces");
            // Each quad has 4 vertices with 10 floats: X, Y, Z, U, V, Layer, SkyLight, BlockLight, AO, DirectionalShade
            assertEquals(6 * 4 * SectionMesh.FLOATS_PER_VERTEX, vBuf.position());
            // Each quad has 2 triangles (6 indices)
            assertEquals(6 * 6, iBuf.position());
        } finally {
            MemoryUtil.memFree(vBuf);
            MemoryUtil.memFree(iBuf);
        }
    }

    @Test
    @DisplayName("Greedy mesher coalesces 16x16x1 flat platform (256 blocks, 576 faces) into exactly 6 merged quads")
    void testCoplanarQuadMerging() {
        ChunkSection section = new ChunkSection(0);

        // Fill a complete 16x16 horizontal layer at y = 0
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                section.setBlock(x, 0, z, 7); // Harappan Fired Brick
            }
        }

        FloatBuffer vBuf = MemoryUtil.memAllocFloat(65536);
        IntBuffer iBuf = MemoryUtil.memAllocInt(65536);

        try {
            int quads = GreedyMesher.meshSection(section, vBuf, iBuf);
            // Without greedy meshing: 256 top + 256 bottom + 16*4 perimeter = 576 quads.
            // With greedy meshing:
            // 1 merged top (16x16)
            // 1 merged bottom (16x16)
            // 4 merged sides (each 16x1)
            // Total: exactly 6 quads!
            assertEquals(6, quads, "Greedy mesher must merge a 16x16x1 sheet into 6 quads");
        } finally {
            MemoryUtil.memFree(vBuf);
            MemoryUtil.memFree(iBuf);
        }
    }
}
