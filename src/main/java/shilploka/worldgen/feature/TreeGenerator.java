package shilploka.worldgen.feature;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.biome.BiomeType;

import java.util.Random;

/**
 * WHY:
 * Trees provide visual verticality, biome identity, harvestable timber, and environmental shade.
 * ShilpLoka models authentic Indian dendrological species:
 * 1. Sal Tree (Shorea robusta): Sacred, towering dipterocarp timber tree of the Gangetic plains.
 * 2. Deodar Cedar (Cedrus deodara): Sacred Himalayan conifer ("timber of the gods") with tiered horizontal boughs.
 * 3. Khejri (Prosopis cineraria): The legendary life-giving Kalpavriksha of the Thar desert.
 *
 * WHAT:
 * Procedural voxel tree generators that construct trunk and foliage blocks deterministically
 * onto surface terrain within a ChunkColumn.
 *
 * USED FOR:
 * Called during the flora decoration stage of WorldGenerator.
 *
 * WHERE NEXT:
 * Leaves and wood logs are meshed by GreedyMesher and harvested by players in survival mode.
 */
public class TreeGenerator {

    /**
     * Attempts to generate an indigenous tree matching the regional biome at surface coordinates.
     *
     * @param column ChunkColumn receiving the tree
     * @param x      Local chunk X (0..15)
     * @param y      Surface Y coordinate (tree base sits at y + 1)
     * @param z      Local chunk Z (0..15)
     * @param biome  Local biome type
     * @param rand   Deterministic PRNG
     */
    public static void generateTree(ChunkColumn column, int x, int y, int z, BiomeType biome, Random rand) {
        if (y < 4 || y > 280) return; // Prevent out-of-bounds near chunk ceilings

        switch (biome) {
            case GANGA_MAIDAN, VINDHYA_JANGAL -> generateSalTree(column, x, y + 1, z, rand);
            case HIMALAYA_SHIKHAR -> generateDeodarCedar(column, x, y + 1, z, rand);
            case THAR_MARUSTHAL -> generateKhejriTree(column, x, y + 1, z, rand);
            case SAHYADRI_VARSHAVAN, DECCAN_PATHAR -> generateTeakTree(column, x, y + 1, z, rand);
            case SUNDARBAN -> generateMangroveTree(column, x, y + 1, z, rand);
            case POORVOTTAR_BAANS -> generateBambooGrove(column, x, y + 1, z, rand);
            default -> {}
        }
    }

    /**
     * Shorea robusta (Sal Tree):
     * Straight, towering central trunk (5 to 7 blocks) with a dense rounded canopy of broad leaves.
     */
    private static void generateSalTree(ChunkColumn column, int bx, int by, int bz, Random rand) {
        int height = 5 + rand.nextInt(3); // 5-7 blocks tall

        // 1. Build straight vertical trunk
        for (int ty = 0; ty < height; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.SAL_LOG);
        }

        // 2. Build rounded crown of Sal leaves
        int canopyStart = by + height - 3;
        int canopyTop = by + height + 1;

        for (int cy = canopyStart; cy <= canopyTop; cy++) {
            int radius = (cy == canopyTop || cy == canopyStart) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    // Cut corner voxels for a natural rounded crown
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && (radius > 1 || rand.nextBoolean())) {
                        continue;
                    }
                    setLeafSafe(column, bx + dx, cy, bz + dz, BlockIds.SAL_LEAVES);
                }
            }
        }
    }

    /**
     * Cedrus deodara (Deodar Cedar):
     * Towering coniferous evergreen (7 to 10 blocks) with tiered horizontal foliage disks
     * that taper upward into a sharp spire tip.
     */
    private static void generateDeodarCedar(ChunkColumn column, int bx, int by, int bz, Random rand) {
        int height = 7 + rand.nextInt(4); // 7-10 blocks tall

        // 1. Central cedar trunk
        for (int ty = 0; ty < height; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.SAL_LOG);
        }

        // 2. Tiered horizontal conifer tiers
        int topY = by + height;
        // Spire tip
        setLeafSafe(column, bx, topY, bz, BlockIds.DEODAR_LEAVES);
        setLeafSafe(column, bx, topY + 1, bz, BlockIds.DEODAR_LEAVES);

        // Tier 1 (near tip, radius 1)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setLeafSafe(column, bx + dx, topY - 1, bz + dz, BlockIds.DEODAR_LEAVES);
            }
        }

        // Tier 2 (middle tier, radius 2)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue; // Round corners
                setLeafSafe(column, bx + dx, topY - 3, bz + dz, BlockIds.DEODAR_LEAVES);
            }
        }

        // Tier 3 (lower tier, radius 2-3)
        if (height >= 8) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    setLeafSafe(column, bx + dx, topY - 5, bz + dz, BlockIds.DEODAR_LEAVES);
                }
            }
        }
    }

    /**
     * Prosopis cineraria (Khejri Tree):
     * Gnarled, drought-hardy desert tree with a short trunk (3-4 blocks) and an umbrella-like
     * sparse canopy of hardy leaves.
     */
    private static void generateKhejriTree(ChunkColumn column, int bx, int by, int bz, Random rand) {
        int height = 3 + rand.nextInt(2); // 3-4 blocks tall

        // Short twisted trunk
        for (int ty = 0; ty < height; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.SAL_LOG);
        }

        // Branching crown
        int crownY = by + height;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // Sparse irregular desert umbrella
                int distSq = (dx * dx) + (dz * dz);
                if (distSq <= 4 && rand.nextFloat() > 0.15f) {
                    setLeafSafe(column, bx + dx, crownY, bz + dz, BlockIds.KHEJRI_LEAVES);
                    if (distSq <= 2 && rand.nextBoolean()) {
                        setLeafSafe(column, bx + dx, crownY + 1, bz + dz, BlockIds.KHEJRI_LEAVES);
                    }
                }
            }
        }
    }

    /**
     * Tectona grandis (Teak / Sagwan Tree):
     * Towering hardwood of the Western Ghats with massive canopies.
     */
    private static void generateTeakTree(ChunkColumn column, int bx, int by, int bz, Random rand) {
        int height = 7 + rand.nextInt(4); // 7-10 blocks tall
        for (int ty = 0; ty < height; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.TEAK_LOG);
        }
        int topY = by + height;
        for (int ly = topY - 3; ly <= topY; ly++) {
            int radius = (ly == topY) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && rand.nextBoolean()) continue;
                    setLeafSafe(column, bx + dx, ly, bz + dz, BlockIds.TEAK_LEAVES);
                }
            }
        }
    }

    /**
     * Heritiera fomes (Sundari Mangrove Tree):
     * Mangrove with elevated stilt prop roots and salt-tolerant foliage.
     */
    private static void generateMangroveTree(ChunkColumn column, int bx, int by, int bz, Random rand) {
        int height = 5 + rand.nextInt(3);
        // Stilt prop roots
        setBlockSafe(column, bx - 1, by, bz, BlockIds.MANGROVE_LOG);
        setBlockSafe(column, bx + 1, by, bz, BlockIds.MANGROVE_LOG);
        setBlockSafe(column, bx, by, bz - 1, BlockIds.MANGROVE_LOG);
        setBlockSafe(column, bx, by, bz + 1, BlockIds.MANGROVE_LOG);

        for (int ty = 0; ty < height; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.MANGROVE_LOG);
        }
        int topY = by + height;
        for (int ly = topY - 2; ly <= topY + 1; ly++) {
            int radius = (ly >= topY) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    setLeafSafe(column, bx + dx, ly, bz + dz, BlockIds.MANGROVE_LEAVES);
                }
            }
        }
    }

    /**
     * Bambusa (Bamboo / Baans):
     * Tall, slender cluster of rapid-growing bamboo culms.
     */
    private static void generateBambooGrove(ChunkColumn column, int bx, int by, int bz, Random rand) {
        // Primary central culm
        int centerHeight = 8 + rand.nextInt(6);
        for (int ty = 0; ty < centerHeight; ty++) {
            setBlockSafe(column, bx, by + ty, bz, BlockIds.BAANS);
        }

        // Additional surrounding cluster culms
        int culms = 2 + rand.nextInt(4);
        for (int i = 0; i < culms; i++) {
            int ox = bx + rand.nextInt(3) - 1;
            int oz = bz + rand.nextInt(3) - 1;
            int bHeight = 8 + rand.nextInt(6); // 8-13 blocks tall
            for (int ty = 0; ty < bHeight; ty++) {
                setBlockSafe(column, ox, by + ty, oz, BlockIds.BAANS);
            }
        }
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }

    private static void setLeafSafe(ChunkColumn column, int x, int y, int z, int leafBlockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            int current = column.getBlock(x, y, z);
            // Leaves only replace Air, never solid wood or rock
            if (current == BlockIds.AIR) {
                column.setBlock(x, y, z, leafBlockId);
            }
        }
    }
}
