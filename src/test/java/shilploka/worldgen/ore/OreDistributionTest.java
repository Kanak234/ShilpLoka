package shilploka.worldgen.ore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkColumn;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies that the subterranean ore distribution populates all 5 essential Indian ores
 * and respects geological elevation limits (e.g. Diamonds exclusively in deep kimberlite).
 *
 * WHAT:
 * Tests that Koyla (coal), Taamba (copper), Loha (iron), Rajavarta (lapis), and Hira (diamond)
 * are all deposited in chunk strata, and validates that deep ores never generate at surface heights.
 *
 * USED FOR:
 * Automated CI verification of mineral economy and metallurgical progression.
 */
class OreDistributionTest {

    @Test
    @DisplayName("All 5 mineral ores generate naturally across a 5x5 chunk area")
    void testAllOresSpawn() {
        WorldGenerator gen = new WorldGenerator(5555L);
        Map<Integer, Integer> oreCounts = new HashMap<>();

        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                ChunkColumn col = gen.generateChunkColumn(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 4; y < 140; y++) {
                            int block = col.getBlock(x, y, z);
                            if (block >= BlockIds.KOYLA_ORE && block <= BlockIds.HIRA_ORE) {
                                oreCounts.put(block, oreCounts.getOrDefault(block, 0) + 1);
                            }
                        }
                    }
                }
            }
        }

        assertTrue(oreCounts.getOrDefault(BlockIds.KOYLA_ORE, 0) > 0, "Koyla ore must spawn");
        assertTrue(oreCounts.getOrDefault(BlockIds.TAAMBA_ORE, 0) > 0, "Taamba ore must spawn");
        assertTrue(oreCounts.getOrDefault(BlockIds.LOHA_ORE, 0) > 0, "Loha ore must spawn");
        assertTrue(oreCounts.getOrDefault(BlockIds.RAJAVARTA_ORE, 0) > 0, "Rajavarta ore must spawn");
        assertTrue(oreCounts.getOrDefault(BlockIds.HIRA_ORE, 0) > 0, "Hira ore must spawn");
    }

    @Test
    @DisplayName("Hira (diamond) ore generates strictly in deep subterranean strata (Y <= 20)")
    void testHiraDepthConstraint() {
        WorldGenerator gen = new WorldGenerator(98765L);

        for (int cx = -2; cx <= 2; cx++) {
            for (int cz = -2; cz <= 2; cz++) {
                ChunkColumn col = gen.generateChunkColumn(cx, cz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 0; y < 320; y++) {
                            if (col.getBlock(x, y, z) == BlockIds.HIRA_ORE) {
                                assertTrue(y <= 20, "Hira ore found at illegal high elevation: Y=" + y);
                                assertTrue(y >= 4, "Hira ore found replacing bedrock: Y=" + y);
                            }
                        }
                    }
                }
            }
        }
    }
}
