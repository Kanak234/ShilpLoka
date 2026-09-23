package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates traditional Indian rural village settlements (Gaon).
 * Features a central Panchayat Chaupal with sacred community tree, deep village well (Kuaan),
 * and rustic artisan huts equipped with authentic crafting workstations.
 *
 * WHAT:
 * Places a complete 16x16 village layout safely within chunk boundaries.
 *
 * USED FOR:
 * Called by HistoricalStructureManager / WorldGenerator when generating village chunks.
 */
public final class GaonGenerator {

    private GaonGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        // 1. Central Panchayat Chaupal (6x6 elevated earthen Chabutara at center)
        int cx = startX + 5;
        int cz = startZ + 5;

        for (int dx = 0; dx < 6; dx++) {
            for (int dz = 0; dz < 6; dz++) {
                setBlockSafe(column, cx + dx, baseY, cz + dz, BlockIds.FIRED_BRICK);
                setBlockSafe(column, cx + dx, baseY + 1, cz + dz, BlockIds.ALLUVIAL_SOIL);
            }
        }

        // Sacred Tree at center of Chaupal
        for (int ty = 2; ty <= 5; ty++) {
            setBlockSafe(column, cx + 2, baseY + ty, cz + 2, BlockIds.SAL_LOG);
        }
        for (int lx = -1; lx <= 1; lx++) {
            for (int lz = -1; lz <= 1; lz++) {
                setBlockSafe(column, cx + 2 + lx, baseY + 6, cz + 2 + lz, BlockIds.SAL_LEAVES);
            }
        }
        // Wooden seating benches on Chaupal perimeter
        setBlockSafe(column, cx, baseY + 2, cz + 2, BlockIds.SAL_PLANKS);
        setBlockSafe(column, cx + 5, baseY + 2, cz + 2, BlockIds.SAL_PLANKS);
        setBlockSafe(column, cx + 2, baseY + 2, cz, BlockIds.SAL_PLANKS);
        setBlockSafe(column, cx + 2, baseY + 2, cz + 5, BlockIds.SAL_PLANKS);

        // Clay Diyas on Chaupal corners
        setBlockSafe(column, cx, baseY + 2, cz, BlockIds.DIYA);
        setBlockSafe(column, cx + 5, baseY + 2, cz + 5, BlockIds.DIYA);

        // 2. Village Community Well (Kuaan) at northwest (startX + 1, startZ + 1)
        int wx = startX + 1;
        int wz = startZ + 1;
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                boolean isEdge = (dx == 0 || dx == 2 || dz == 0 || dz == 2);
                for (int y = baseY - 5; y <= baseY + 1; y++) {
                    if (isEdge) {
                        setBlockSafe(column, wx + dx, y, wz + dz, BlockIds.GRANITE);
                    } else {
                        setBlockSafe(column, wx + dx, y, wz + dz, BlockIds.WATER);
                    }
                }
            }
        }
        // Well roof crossbar & Diya
        setBlockSafe(column, wx, baseY + 2, wz + 1, BlockIds.SAL_PLANKS);
        setBlockSafe(column, wx + 2, baseY + 2, wz + 1, BlockIds.SAL_PLANKS);
        setBlockSafe(column, wx + 1, baseY + 3, wz + 1, BlockIds.DIYA);

        // 3. Artisan Hut 1 (Kumhar's Pottery Workshop at northeast)
        buildHut(column, startX + 11, baseY, startZ + 1, BlockIds.CHAAK, BlockIds.PAKKI_BHATTI);

        // 4. Artisan Hut 2 (Lohar's Blacksmith Forge at southwest)
        buildHut(column, startX + 1, baseY, startZ + 11, BlockIds.NIHAI, BlockIds.SMELTING_BHATTI);

        // 5. Artisan Hut 3 (Julaha's Weaving & Milling Workshop at southeast)
        buildHut(column, startX + 11, baseY, startZ + 11, BlockIds.KARGAH, BlockIds.CHAKKI);
    }

    private static void buildHut(ChunkColumn column, int bx, int baseY, int bz, int workstationA, int workstationB) {
        int w = 4;
        int d = 4;

        // Earthen walls (height 3)
        for (int dy = 1; dy <= 3; dy++) {
            for (int dx = 0; dx < w; dx++) {
                for (int dz = 0; dz < d; dz++) {
                    boolean isWall = (dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1);
                    if (isWall) {
                        setBlockSafe(column, bx + dx, baseY + dy, bz + dz, BlockIds.ALLUVIAL_SOIL);
                    } else {
                        setBlockSafe(column, bx + dx, baseY + dy, bz + dz, BlockIds.AIR);
                    }
                }
            }
        }

        // Door opening
        setBlockSafe(column, bx + 1, baseY + 1, bz, BlockIds.AIR);
        setBlockSafe(column, bx + 1, baseY + 2, bz, BlockIds.AIR);

        // Timber plank roof
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                setBlockSafe(column, bx + dx, baseY + 4, bz + dz, BlockIds.SAL_PLANKS);
            }
        }

        // Install specialized workstations inside hut
        setBlockSafe(column, bx + 2, baseY + 1, bz + 2, workstationA);
        setBlockSafe(column, bx + 1, baseY + 1, bz + 2, workstationB);
        // Diya on table
        setBlockSafe(column, bx + 2, baseY + 2, bz + 2, BlockIds.DIYA);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
