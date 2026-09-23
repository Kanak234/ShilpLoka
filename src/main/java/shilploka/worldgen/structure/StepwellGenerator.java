package shilploka.worldgen.structure;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Generates the ancient Indian Stepwell (Baoli / Kalyani / Vav) architecture.
 * Features deep subterranean descending terraces with geometric stepped staircases,
 * carved stone gallery pillars, pristine natural water reservoir at the bottom, and relic chest.
 *
 * WHAT:
 * Carves a 12x12 subterranean stepped well down 10 blocks below ground.
 */
public final class StepwellGenerator {

    private StepwellGenerator() {}

    public static void generate(ChunkColumn column, int startX, int baseY, int startZ, Random rand) {
        int width = 12;
        int depth = 12;
        int bottomY = Math.max(10, baseY - 10);

        // 1. Excavate well chamber from baseY down to bottomY
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < depth; dz++) {
                int px = startX + dx;
                int pz = startZ + dz;

                // Solid outer retaining walls
                boolean isEdge = (dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1);
                for (int y = bottomY - 2; y <= baseY + 1; y++) {
                    if (isEdge) {
                        setBlockSafe(column, px, y, pz, BlockIds.YELLOW_SANDSTONE);
                    } else if (y <= bottomY + 1) {
                        // Subterranean reservoir water floor
                        setBlockSafe(column, px, y, pz, (y <= bottomY) ? BlockIds.DECCAN_BASALT : BlockIds.WATER);
                    } else {
                        // Open air chamber
                        setBlockSafe(column, px, y, pz, BlockIds.AIR);
                    }
                }
            }
        }

        // 2. Geometric Stepped Terraces (Tiered steps descending along inner edges)
        // 3 tiers: baseY - 2, baseY - 5, baseY - 8
        for (int tier = 1; tier <= 3; tier++) {
            int tierY = baseY - (tier * 2);
            int inset = tier;

            for (int dx = inset; dx < width - inset; dx++) {
                setBlockSafe(column, startX + dx, tierY, startZ + inset, BlockIds.YELLOW_SANDSTONE);
                setBlockSafe(column, startX + dx, tierY, startZ + depth - 1 - inset, BlockIds.YELLOW_SANDSTONE);
            }
            for (int dz = inset; dz < depth - inset; dz++) {
                setBlockSafe(column, startX + inset, tierY, startZ + dz, BlockIds.YELLOW_SANDSTONE);
                setBlockSafe(column, startX + width - 1 - inset, tierY, startZ + dz, BlockIds.YELLOW_SANDSTONE);
            }

            // Carved pillars on corner landings
            int px1 = startX + inset;
            int px2 = startX + width - 1 - inset;
            int pz1 = startZ + inset;
            int pz2 = startZ + depth - 1 - inset;

            setBlockSafe(column, px1, tierY + 1, pz1, BlockIds.CARVED_STONE);
            setBlockSafe(column, px2, tierY + 1, pz1, BlockIds.CARVED_STONE);
            setBlockSafe(column, px1, tierY + 1, pz2, BlockIds.CARVED_STONE);
            setBlockSafe(column, px2, tierY + 1, pz2, BlockIds.CARVED_STONE);

            // Diya on landings
            if (tier == 2) {
                setBlockSafe(column, px1, tierY + 2, pz1, BlockIds.DIYA);
                setBlockSafe(column, px2, tierY + 2, pz2, BlockIds.DIYA);
            }
        }

        // 3. Subterranean Relic Alcove at bottom reservoir edge
        setBlockSafe(column, startX + 2, bottomY + 2, startZ + 2, BlockIds.ANCIENT_CHEST);
        setBlockSafe(column, startX + 2, bottomY + 3, startZ + 2, BlockIds.DIYA);

        // 4. Ground-level safety parapet wall
        for (int dx = 0; dx < width; dx++) {
            setBlockSafe(column, startX + dx, baseY + 1, startZ, BlockIds.YELLOW_SANDSTONE);
            setBlockSafe(column, startX + dx, baseY + 1, startZ + depth - 1, BlockIds.YELLOW_SANDSTONE);
        }
        for (int dz = 0; dz < depth; dz++) {
            setBlockSafe(column, startX, baseY + 1, startZ + dz, BlockIds.YELLOW_SANDSTONE);
            setBlockSafe(column, startX + width - 1, baseY + 1, startZ + dz, BlockIds.YELLOW_SANDSTONE);
        }
        // Entrance opening on south side
        setBlockSafe(column, startX + 5, baseY + 1, startZ + depth - 1, BlockIds.AIR);
        setBlockSafe(column, startX + 6, baseY + 1, startZ + depth - 1, BlockIds.AIR);
    }

    private static void setBlockSafe(ChunkColumn column, int x, int y, int z, int blockId) {
        if (x >= 0 && x < 16 && z >= 0 && z < 16 && y >= 0 && y < 320) {
            column.setBlock(x, y, z, blockId);
        }
    }
}
