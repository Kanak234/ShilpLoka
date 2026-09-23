package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates the iconic Sindhu-Saraswati (Harappan) Great Bath & Granary architectural complex.
 * Features fired brick masonry, recessed waterproof central bath with descending stairs,
 * surrounding pillared cloister, and relic chest holding Harappan seals and mudras.
 *
 * WHAT:
 * Places a 14x14 voxel monument safely within chunk boundaries.
 */
public final class GreatBathGenerator {

    private GreatBathGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        // 1. Foundation and platform (14x14)
        for (int dx = 0; dx < 14; dx++) {
            for (int dz = 0; dz < 14; dz++) {
                setBlockSafe(column, startX + dx, baseY, startZ + dz, BlockIds.FIRED_BRICK);
                // Bedrock foundation beneath
                for (int dy = -1; dy >= -3; dy--) {
                    setBlockSafe(column, startX + dx, baseY + dy, startZ + dz, BlockIds.FIRED_BRICK);
                }
            }
        }

        // 2. Central Sunken Bath (6x8 cavity, 2 blocks deep)
        // From dx=4..9, dz=3..10
        for (int dx = 4; dx <= 9; dx++) {
            for (int dz = 3; dz <= 10; dz++) {
                // Bitumen floor at baseY - 2
                setBlockSafe(column, startX + dx, baseY - 2, startZ + dz, BlockIds.DECCAN_BASALT);
                // Bath water
                setBlockSafe(column, startX + dx, baseY - 1, startZ + dz, BlockIds.WATER);
                setBlockSafe(column, startX + dx, baseY, startZ + dz, BlockIds.WATER);
                setBlockSafe(column, startX + dx, baseY + 1, startZ + dz, BlockIds.AIR);
                setBlockSafe(column, startX + dx, baseY + 2, startZ + dz, BlockIds.AIR);
            }
        }

        // 3. Descending Brick Steps at North (dz=3) and South (dz=10)
        for (int dx = 5; dx <= 8; dx++) {
            setBlockSafe(column, startX + dx, baseY, startZ + 3, BlockIds.FIRED_BRICK);
            setBlockSafe(column, startX + dx, baseY - 1, startZ + 4, BlockIds.FIRED_BRICK);

            setBlockSafe(column, startX + dx, baseY, startZ + 10, BlockIds.FIRED_BRICK);
            setBlockSafe(column, startX + dx, baseY - 1, startZ + 9, BlockIds.FIRED_BRICK);
        }

        // 4. Surrounding Colonnade Pillars (Height 4)
        int[] pillarX = {2, 2, 2, 2, 11, 11, 11, 11};
        int[] pillarZ = {2, 5, 8, 11, 2, 5, 8, 11};
        for (int i = 0; i < pillarX.length; i++) {
            int px = startX + pillarX[i];
            int pz = startZ + pillarZ[i];
            for (int py = 1; py <= 3; py++) {
                setBlockSafe(column, px, baseY + py, pz, BlockIds.FIRED_BRICK);
            }
            // Diya atop colonnade pillars
            setBlockSafe(column, px, baseY + 4, pz, BlockIds.DIYA);
        }

        // 5. Corner Granary Chambers & Walls
        // Enclosure perimeter walls (height 2)
        for (int dx = 0; dx < 14; dx++) {
            setBlockSafe(column, startX + dx, baseY + 1, startZ, BlockIds.FIRED_BRICK);
            setBlockSafe(column, startX + dx, baseY + 1, startZ + 13, BlockIds.FIRED_BRICK);
        }
        for (int dz = 0; dz < 14; dz++) {
            setBlockSafe(column, startX, baseY + 1, startZ + dz, BlockIds.FIRED_BRICK);
            setBlockSafe(column, startX + 13, baseY + 1, startZ + dz, BlockIds.FIRED_BRICK);
        }

        // 6. Relic Chest in corner cloister
        setBlockSafe(column, startX + 1, baseY + 1, startZ + 1, BlockIds.ANCIENT_CHEST);
        setBlockSafe(column, startX + 12, baseY + 1, startZ + 12, BlockIds.DIYA);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
