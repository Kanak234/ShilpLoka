package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates the iconic Monolithic Ashokan Edict Pillar (अशोक स्तम्भ).
 * Features a carved stone plinth with Dhamma edicts, smooth monolithic stone shaft,
 * inverted bell lotus capital, abacus, and crowning Dharma Chakra wheel.
 *
 * WHAT:
 * Places a 6x6 monument rising 11 blocks high.
 */
public final class AshokaSthambhaGenerator {

    private AshokaSthambhaGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        int cx = startX + 3;
        int cz = startZ + 3;

        // 1. Carved Sandstone Plinth Platform (6x6)
        for (int dx = 0; dx < 6; dx++) {
            for (int dz = 0; dz < 6; dz++) {
                setBlockSafe(column, startX + dx, baseY, startZ + dz, BlockIds.RED_SANDSTONE);
                setBlockSafe(column, startX + dx, baseY - 1, startZ + dz, BlockIds.RED_SANDSTONE);
            }
        }

        // Stepped raised tier (4x4)
        for (int dx = 1; dx <= 4; dx++) {
            for (int dz = 1; dz <= 4; dz++) {
                setBlockSafe(column, startX + dx, baseY + 1, startZ + dz, BlockIds.CARVED_STONE);
            }
        }

        // 2. Corner Diyas on Plinth
        setBlockSafe(column, startX, baseY + 1, startZ, BlockIds.DIYA);
        setBlockSafe(column, startX + 5, baseY + 1, startZ, BlockIds.DIYA);
        setBlockSafe(column, startX, baseY + 1, startZ + 5, BlockIds.DIYA);
        setBlockSafe(column, startX + 5, baseY + 1, startZ + 5, BlockIds.DIYA);

        // 3. Monolithic Pillar Shaft (Height 7)
        for (int y = 2; y <= 8; y++) {
            setBlockSafe(column, cx, baseY + y, cz, BlockIds.CARVED_STONE);
            setBlockSafe(column, cx - 1, baseY + y, cz, BlockIds.CARVED_STONE);
            setBlockSafe(column, cx, baseY + y, cz - 1, BlockIds.CARVED_STONE);
            setBlockSafe(column, cx - 1, baseY + y, cz - 1, BlockIds.CARVED_STONE);
        }

        // 4. Inverted Bell Lotus Capital & Abacus
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                setBlockSafe(column, cx + dx, baseY + 9, cz + dz, BlockIds.CARVED_STONE);
            }
        }

        // 5. Crowning Ashoka Chakra Wheel
        setBlockSafe(column, cx, baseY + 10, cz, BlockIds.ASHOKA_CHAKRA);
        setBlockSafe(column, cx - 1, baseY + 10, cz, BlockIds.ASHOKA_CHAKRA);
        setBlockSafe(column, cx, baseY + 10, cz - 1, BlockIds.ASHOKA_CHAKRA);
        setBlockSafe(column, cx - 1, baseY + 10, cz - 1, BlockIds.ASHOKA_CHAKRA);

        // 6. Edict Relic Chest at pillar base
        setBlockSafe(column, cx, baseY + 2, cz + 1, BlockIds.ANCIENT_CHEST);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
