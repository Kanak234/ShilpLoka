package shilploka.worldgen.cave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkColumn;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Subterranean cave carvers must create natural underground cavities and tunnel networks
 * without compromising bedrock integrity (Y=0..3).
 *
 * WHAT:
 * Tests that caves successfully carve air blocks into stone strata between Y=8 and Y=56,
 * while bedrock layers (Y=0..3) remain 100% impenetrable Deccan Basalt and Granite.
 *
 * USED FOR:
 * Automated CI verification of subterranean cave generation.
 */
class CaveCarverTest {

    @Test
    @DisplayName("Cave carver generates subterranean air cavities within stone strata")
    void testCaveCarvingOccurs() {
        WorldGenerator gen = new WorldGenerator(1947L);
        boolean foundSubterraneanCaveAir = false;

        // Check a 5x5 chunk area to locate underground cave cavities
        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                ChunkColumn col = gen.generateChunkColumn(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 10; y <= 50; y++) {
                            if (col.getBlock(x, y, z) == BlockIds.AIR) {
                                foundSubterraneanCaveAir = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(foundSubterraneanCaveAir, "World generation must carve underground cave air voids");
    }

    @Test
    @DisplayName("Bedrock strata (Y=0..3) is strictly preserved and never carved by caves")
    void testBedrockPreservation() {
        WorldGenerator gen = new WorldGenerator(777L);

        for (int cx = -3; cx <= 3; cx++) {
            for (int cz = -3; cz <= 3; cz++) {
                ChunkColumn col = gen.generateChunkColumn(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y <= 3; y++) {
                            int block = col.getBlock(x, y, z);
                            assertNotEquals(BlockIds.AIR, block,
                                    String.format("Bedrock breached at chunk (%d,%d) local (%d,%d,%d)", cx, cz, x, y, z));
                            assertTrue(block == BlockIds.DECCAN_BASALT || block == BlockIds.GRANITE,
                                    "Bedrock must remain Deccan Basalt or Granite");
                        }
                    }
                }
            }
        }
    }
}
