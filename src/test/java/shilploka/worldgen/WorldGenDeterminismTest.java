package shilploka.worldgen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkColumn;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Procedural generation must be 100% deterministic. Two players or server instances
 * loading the same 64-bit seed must experience the exact same terrain contours,
 * biomes, caves, ore veins, and trees down to the exact individual voxel.
 *
 * WHAT:
 * Tests that two separate WorldGenerator instances initialized with the same seed
 * produce bit-exact identical ChunkColumns across multiple chunk coordinates,
 * and that different seeds produce measurably different terrain.
 *
 * USED FOR:
 * Automated CI verification of world generation determinism.
 */
class WorldGenDeterminismTest {

    @Test
    @DisplayName("Identical seeds produce bit-exact identical ChunkColumns across multiple chunk positions")
    void testSeedDeterminismAcrossMultipleChunks() {
        long seed = 0xDEADBEEFCAFE1947L;
        WorldGenerator genA = new WorldGenerator(seed);
        WorldGenerator genB = new WorldGenerator(seed);

        int[][] testCoords = {
                {0, 0},
                {1, -1},
                {-4, 3},
                {7, 7}
        };

        for (int[] coord : testCoords) {
            int cx = coord[0];
            int cz = coord[1];

            ChunkColumn colA = genA.generateChunkColumn(cx, cz);
            ChunkColumn colB = genB.generateChunkColumn(cx, cz);

            assertEquals(colA.getChunkX(), colB.getChunkX());
            assertEquals(colA.getChunkZ(), colB.getChunkZ());

            for (int y = 0; y < 180; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        int blockA = colA.getBlock(x, y, z);
                        int blockB = colB.getBlock(x, y, z);
                        assertEquals(blockA, blockB,
                                String.format("Voxel mismatch at chunk (%d,%d) local (%d,%d,%d)",
                                        cx, cz, x, y, z));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Different seeds produce distinct terrain geometry and block layouts")
    void testDifferentSeedsProduceDifferentTerrain() {
        WorldGenerator gen1 = new WorldGenerator(1001L);
        WorldGenerator gen2 = new WorldGenerator(9999L);

        ChunkColumn col1 = gen1.generateChunkColumn(5, 5);
        ChunkColumn col2 = gen2.generateChunkColumn(5, 5);

        int differences = 0;
        for (int y = 10; y < 100; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    if (col1.getBlock(x, y, z) != col2.getBlock(x, y, z)) {
                        differences++;
                    }
                }
            }
        }

        assertTrue(differences > 100, "Different seeds must produce substantially distinct chunk data");
    }
}
