package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates the magnificent Buddhist Great Stupa (Sanchi architecture).
 * Features a circular/square Medhi plinth, solid hemispherical sandstone Anda dome,
 * Harmika square terrace railing, and triple Chhatra umbrella spire crowned with the Dharma Chakra.
 *
 * WHAT:
 * Places a 14x14 voxel monument safely within chunk boundaries.
 */
public final class StupaGenerator {

    private StupaGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        // 1. Square Medhi Base Plinth (14x14)
        for (int dx = 0; dx < 14; dx++) {
            for (int dz = 0; dz < 14; dz++) {
                setBlockSafe(column, startX + dx, baseY, startZ + dz, BlockIds.RED_SANDSTONE);
                setBlockSafe(column, startX + dx, baseY - 1, startZ + dz, BlockIds.RED_SANDSTONE);
            }
        }

        // 2. Pradakshina Path outer railing (boundary blocks)
        for (int dx = 0; dx < 14; dx++) {
            // Railing with breaks for Torana gates
            if (dx != 6 && dx != 7) {
                setBlockSafe(column, startX + dx, baseY + 1, startZ, BlockIds.CARVED_STONE);
                setBlockSafe(column, startX + dx, baseY + 1, startZ + 13, BlockIds.CARVED_STONE);
            }
        }
        for (int dz = 0; dz < 14; dz++) {
            if (dz != 6 && dz != 7) {
                setBlockSafe(column, startX, baseY + 1, startZ + dz, BlockIds.CARVED_STONE);
                setBlockSafe(column, startX + 13, baseY + 1, startZ + dz, BlockIds.CARVED_STONE);
            }
        }

        // 3. Hemispherical Anda (Dome)
        // Center of dome at (startX + 6.5, startZ + 6.5)
        double centerX = startX + 6.5;
        double centerZ = startZ + 6.5;
        int domeRadius = 5;

        for (int dy = 1; dy <= 5; dy++) {
            double currentRadius = Math.sqrt(Math.max(0, domeRadius * domeRadius - (dy - 1) * (dy - 1) * 1.5));
            for (int dx = 0; dx < 14; dx++) {
                for (int dz = 0; dz < 14; dz++) {
                    double dist = Math.hypot(startX + dx + 0.5 - centerX, startZ + dz + 0.5 - centerZ);
                    if (dist <= currentRadius) {
                        int block = (dy == 1) ? BlockIds.CARVED_STONE : BlockIds.RED_SANDSTONE;
                        setBlockSafe(column, startX + dx, baseY + dy, startZ + dz, block);
                    }
                }
            }
        }

        // 4. Central Relic Chamber inside dome
        int cx = (int) centerX;
        int cz = (int) centerZ;
        setBlockSafe(column, cx, baseY + 1, cz, BlockIds.ANCIENT_CHEST);
        setBlockSafe(column, cx, baseY + 2, cz, BlockIds.DIYA);

        // 5. Harmika (Square Balcony) at dome apex (baseY + 6)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                setBlockSafe(column, cx + dx, baseY + 6, cz + dz, BlockIds.CARVED_STONE);
            }
        }

        // 6. Central Chhatra Spire (Umbrella tiers rising upward)
        // Central mast
        setBlockSafe(column, cx, baseY + 7, cz, BlockIds.CARVED_STONE);
        setBlockSafe(column, cx, baseY + 8, cz, BlockIds.CARVED_STONE);
        setBlockSafe(column, cx, baseY + 9, cz, BlockIds.CARVED_STONE);
        setBlockSafe(column, cx, baseY + 10, cz, BlockIds.ASHOKA_CHAKRA); // Top Chakra finial

        // Chhatra umbrella disc tiers
        setBlockSafe(column, cx - 1, baseY + 8, cz, BlockIds.RED_SANDSTONE);
        setBlockSafe(column, cx + 1, baseY + 8, cz, BlockIds.RED_SANDSTONE);
        setBlockSafe(column, cx, baseY + 8, cz - 1, BlockIds.RED_SANDSTONE);
        setBlockSafe(column, cx, baseY + 8, cz + 1, BlockIds.RED_SANDSTONE);

        // 7. Torana Pillars at Southern entrance (dx=6..7, dz=0)
        setBlockSafe(column, startX + 5, baseY + 1, startZ, BlockIds.CARVED_STONE);
        setBlockSafe(column, startX + 5, baseY + 2, startZ, BlockIds.CARVED_STONE);
        setBlockSafe(column, startX + 5, baseY + 3, startZ, BlockIds.DIYA);

        setBlockSafe(column, startX + 8, baseY + 1, startZ, BlockIds.CARVED_STONE);
        setBlockSafe(column, startX + 8, baseY + 2, startZ, BlockIds.CARVED_STONE);
        setBlockSafe(column, startX + 8, baseY + 3, startZ, BlockIds.DIYA);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
