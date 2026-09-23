package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates the rugged Maratha-Rajput Hill Fort Ruin (Giri Durg).
 * Features solid stone and basalt ramparts, crenellated battlements, corner watchtower bastions,
 * interior armory with Wootz crucible steel loot, and defensive arrow slits.
 *
 * WHAT:
 * Places a 14x14 fortified military structure safely within chunk boundaries.
 */
public final class HillFortGenerator {

    private HillFortGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        int width = 14;
        int depth = 14;

        // 1. Solid stone foundation (baseY and beneath)
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                setBlockSafe(column, startX + dx, baseY, startZ + dz, BlockIds.STONE);
                setBlockSafe(column, startX + dx, baseY - 1, startZ + dz, BlockIds.STONE);
            }
        }

        // 2. Thick Outer Ramparts (Height 4)
        for (int dy = 1; dy <= 4; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int block = (dy % 2 == 0) ? BlockIds.RED_SANDSTONE : BlockIds.DECCAN_BASALT;
                setBlockSafe(column, startX + dx, baseY + dy, startZ, block);
                setBlockSafe(column, startX + dx, baseY + dy, startZ + depth - 1, block);
            }
            for (int dz = 0; dz < depth; dz++) {
                int block = (dy % 2 == 0) ? BlockIds.RED_SANDSTONE : BlockIds.DECCAN_BASALT;
                setBlockSafe(column, startX, baseY + dy, startZ + dz, block);
                setBlockSafe(column, startX + width - 1, baseY + dy, startZ + dz, block);
            }
        }

        // 3. Fort Entrance Gate (South wall opening)
        setBlockSafe(column, startX + 6, baseY + 1, startZ + depth - 1, BlockIds.AIR);
        setBlockSafe(column, startX + 6, baseY + 2, startZ + depth - 1, BlockIds.AIR);
        setBlockSafe(column, startX + 7, baseY + 1, startZ + depth - 1, BlockIds.AIR);
        setBlockSafe(column, startX + 7, baseY + 2, startZ + depth - 1, BlockIds.AIR);

        // 4. Crenellated Battlements atop ramparts (baseY + 5)
        for (int dx = 0; dx < width; dx += 2) {
            setBlockSafe(column, startX + dx, baseY + 5, startZ, BlockIds.CARVED_STONE);
            setBlockSafe(column, startX + dx, baseY + 5, startZ + depth - 1, BlockIds.CARVED_STONE);
        }
        for (int dz = 0; dz < depth; dz += 2) {
            setBlockSafe(column, startX, baseY + 5, startZ + dz, BlockIds.CARVED_STONE);
            setBlockSafe(column, startX + width - 1, baseY + 5, startZ + dz, BlockIds.CARVED_STONE);
        }

        // 5. Corner Bastion Watchtowers (dx=0..2, dz=0..2)
        buildBastion(column, startX, baseY, startZ);
        buildBastion(column, startX + width - 3, baseY, startZ);
        buildBastion(column, startX, baseY, startZ + depth - 3);
        buildBastion(column, startX + width - 3, baseY, startZ + depth - 3);

        // 6. Interior Armory & Treasury Alcove
        // Small stone vault inside courtyard
        for (int dx = 5; dx <= 8; dx++) {
            for (int dz = 5; dz <= 8; dz++) {
                setBlockSafe(column, startX + dx, baseY + 1, startZ + dz, BlockIds.SAL_PLANKS);
            }
        }
        setBlockSafe(column, startX + 6, baseY + 2, startZ + 6, BlockIds.ANCIENT_CHEST);
        setBlockSafe(column, startX + 7, baseY + 2, startZ + 6, BlockIds.DIYA);
    }

    private static void buildBastion(ChunkColumn column, int bx, int baseY, int bz) {
        for (int dy = 1; dy <= 6; dy++) {
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    if (dx == 1 && dz == 1 && dy <= 5) {
                        setBlockSafe(column, bx + dx, baseY + dy, bz + dz, BlockIds.AIR); // Tower interior
                    } else {
                        setBlockSafe(column, bx + dx, baseY + dy, bz + dz, BlockIds.DECCAN_BASALT);
                    }
                }
            }
        }
        // Arrow slits
        setBlockSafe(column, bx + 1, baseY + 3, bz, BlockIds.AIR);
        setBlockSafe(column, bx, baseY + 3, bz + 1, BlockIds.AIR);
        // Diya on tower roof
        setBlockSafe(column, bx + 1, baseY + 7, bz + 1, BlockIds.DIYA);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
