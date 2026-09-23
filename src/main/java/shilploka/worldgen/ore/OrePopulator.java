package shilploka.worldgen.ore;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.Random;

/**
 * WHY:
 * Subterranean strata must contain metallurgic deposits to support craft progression
 * (kilns, smithing, tool forging, weaponry, and jewelry).
 *
 * WHAT:
 * Places deterministic 3D ellipsoidal mineral clusters of Koyla, Taamba, Loha, Rajavarta,
 * and Hira into subterranean rock strata within defined geological elevation limits.
 *
 * USED FOR:
 * Called per chunk column in WorldGenerator following cave carving.
 *
 * WHERE NEXT:
 * The exposed ore blocks will be mined by players using appropriate tool tiers.
 */
public class OrePopulator {

    private final long seed;

    public OrePopulator(long seed) {
        this.seed = seed;
    }

    /**
     * WHY:
     * Generates ore veins for a given chunk column.
     *
     * WHAT:
     * Scrambles the world seed with chunk coordinates into a deterministic Random instance,
     * iterates all OreTypes, and generates clusters embedded in solid host rock.
     */
    public void populateChunk(ChunkColumn column, int chunkX, int chunkZ) {
        long chunkSeed = this.seed ^ (((long) chunkX * 0x4f992ddfa112L) + ((long) chunkZ * 0x1d1f3b4c9a87L));
        Random rand = new Random(chunkSeed);

        for (OreType ore : OreType.values()) {
            int count = ore.getVeinsPerChunk();
            for (int i = 0; i < count; i++) {
                int ox = rand.nextInt(16);
                int oy = ore.getMinY() + rand.nextInt(Math.max(1, ore.getMaxY() - ore.getMinY() + 1));
                int oz = rand.nextInt(16);

                populateCluster(column, ox, oy, oz, ore, rand);
            }
        }
    }

    /**
     * Generates a single localized ore vein cluster around the origin point.
     */
    private void populateCluster(ChunkColumn column, int ox, int oy, int oz, OreType ore, Random rand) {
        int radius = Math.max(1, (int) Math.round(Math.cbrt(ore.getClusterSize())));

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int x = ox + dx;
                    int y = oy + dy;
                    int z = oz + dz;

                    // Bounds check within 16x320x16 chunk
                    if (x < 0 || x >= 16 || z < 0 || z >= 16 || y < 4 || y >= 320) {
                        continue;
                    }

                    // Ellipsoidal falloff with random noise
                    double distSq = (dx * dx) + (dy * dy * 1.5) + (dz * dz);
                    if (distSq > (radius * radius) + (rand.nextDouble() * 0.5)) {
                        continue;
                    }

                    int currentBlock = column.getBlock(x, y, z);
                    // Ores only replace subterranean host rock (Stone, Granite, Basalt)
                    if (currentBlock == BlockIds.STONE || currentBlock == BlockIds.GRANITE ||
                        (currentBlock == BlockIds.DECCAN_BASALT && y >= 4)) {
                        column.setBlock(x, y, z, ore.getBlockId());
                    }
                }
            }
        }
    }
}
