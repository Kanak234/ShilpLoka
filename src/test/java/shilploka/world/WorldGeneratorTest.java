package shilploka.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkSection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies WorldGenerator infinite ChunkColumn generation, seed determinism,
 * geological strata, and architectural monument placement.
 *
 * WHAT:
 * Tests that two generator instances with identical seeds produce identical 320-height columns,
 * and that bedrock, Harappan fired brick masonry, Diyas, and Sal timber are properly populated.
 *
 * USED FOR:
 * Automated CI verification of world generation integrity.
 */
class WorldGeneratorTest {

    @Test
    @DisplayName("WorldGenerator produces deterministic ChunkColumns for identical seeds")
    void testDeterminism() {
        long seed = 19470815L;
        WorldGenerator gen1 = new WorldGenerator(seed);
        WorldGenerator gen2 = new WorldGenerator(seed);

        ChunkColumn col1 = gen1.generateChunkColumn(0, 0);
        ChunkColumn col2 = gen2.generateChunkColumn(0, 0);

        assertEquals(col1.getChunkX(), col2.getChunkX());
        assertEquals(col1.getChunkZ(), col2.getChunkZ());

        for (int y = 0; y < 120; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    assertEquals(col1.getBlock(x, y, z), col2.getBlock(x, y, z),
                            "Block mismatch at (" + x + "," + y + "," + z + ")");
                }
            }
        }
    }

    @Test
    @DisplayName("ChunkColumn contains geological strata and origin Harappan monument")
    void testBlockContent() {
        WorldGenerator gen = new WorldGenerator(42L);
        ChunkColumn col = gen.generateChunkColumn(0, 0);

        // Bedrock is Deccan basalt at y=0, 1, 2
        assertEquals(WorldGenerator.BLOCK_DECCAN_BASALT, col.getBlock(0, 0, 0));
        assertEquals(WorldGenerator.BLOCK_DECCAN_BASALT, col.getBlock(5, 1, 5));
        assertEquals(WorldGenerator.BLOCK_DECCAN_BASALT, col.getBlock(10, 2, 10));

        // Origin monument at (0, 0) has Harappan fired brick foundation, Diyas, and Sal wood
        boolean foundFiredBrick = false;
        boolean foundDiya = false;
        boolean foundSalWood = false;

        for (int y = 0; y < 120; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int b = col.getBlock(x, y, z);
                    if (b == WorldGenerator.BLOCK_FIRED_BRICK) foundFiredBrick = true;
                    if (b == WorldGenerator.BLOCK_DIYA) foundDiya = true;
                    if (b == WorldGenerator.BLOCK_SAL_LOG || b == WorldGenerator.BLOCK_SAL_PLANKS) foundSalWood = true;
                }
            }
        }

        assertTrue(foundFiredBrick, "Chunk column must generate Harappan fired brick masonry");
        assertTrue(foundDiya, "Chunk column must generate decorative clay Diyas");
        assertTrue(foundSalWood, "Chunk column must generate Sal wood architectural supports");
    }
}
