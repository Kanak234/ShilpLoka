package shilploka.worldgen.feature;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.biome.BiomeType;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies procedural indigenous tree generation across the 3 biomes of Bharat.
 *
 * WHAT:
 * Tests that Sal, Deodar, and Khejri tree generators construct realistic trunks and
 * species-specific leaf blocks within ChunkColumn bounds.
 *
 * USED FOR:
 * Automated CI verification of procedural flora synthesis.
 */
class TreeGeneratorTest {

    @Test
    @DisplayName("Sal tree generates vertical trunk and Sal leaves in Ganga Maidan")
    void testSalTreeGeneration() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(101L);

        TreeGenerator.generateTree(column, 8, 64, 8, BiomeType.GANGA_MAIDAN, rand);

        // Verify trunk
        assertEquals(BlockIds.SAL_LOG, column.getBlock(8, 65, 8));
        assertEquals(BlockIds.SAL_LOG, column.getBlock(8, 66, 8));

        // Verify Sal leaves canopy
        boolean foundSalLeaves = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 66; dy <= 73; dy++) {
                    if (column.getBlock(8 + dx, dy, 8 + dz) == BlockIds.SAL_LEAVES) {
                        foundSalLeaves = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundSalLeaves, "Sal tree must generate Shorea robusta leaves");
    }

    @Test
    @DisplayName("Deodar cedar generates tall trunk and Deodar leaves in Himalaya Shikhar")
    void testDeodarCedarGeneration() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(202L);

        TreeGenerator.generateTree(column, 8, 80, 8, BiomeType.HIMALAYA_SHIKHAR, rand);

        // Verify trunk
        assertEquals(BlockIds.SAL_LOG, column.getBlock(8, 81, 8));

        // Verify Deodar cedar needle leaves
        boolean foundDeodarLeaves = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 81; dy <= 95; dy++) {
                    if (column.getBlock(8 + dx, dy, 8 + dz) == BlockIds.DEODAR_LEAVES) {
                        foundDeodarLeaves = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundDeodarLeaves, "Deodar cedar must generate Cedrus deodara needle leaves");
    }

    @Test
    @DisplayName("Khejri tree generates desert wood and Khejri foliage in Thar Marusthal")
    void testKhejriTreeGeneration() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(303L);

        TreeGenerator.generateTree(column, 8, 70, 8, BiomeType.THAR_MARUSTHAL, rand);

        // Verify trunk
        assertEquals(BlockIds.SAL_LOG, column.getBlock(8, 71, 8));

        // Verify Khejri desert foliage
        boolean foundKhejriLeaves = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 71; dy <= 76; dy++) {
                    if (column.getBlock(8 + dx, dy, 8 + dz) == BlockIds.KHEJRI_LEAVES) {
                        foundKhejriLeaves = true;
                        break;
                    }
                }
            }
        }
        assertTrue(foundKhejriLeaves, "Khejri tree must generate Prosopis cineraria foliage");
    }
}
