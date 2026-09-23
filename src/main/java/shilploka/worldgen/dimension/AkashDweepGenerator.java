package shilploka.worldgen.dimension;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.noise.OctaveNoise;

/**
 * WHY:
 * Generates the serene celestial floating islands of Akash Dweep (आकाश द्वीप / स्वर्गलोक).
 * The peaceful heavenly realm accessed after overcoming the subterranean trials of Paataal.
 *
 * WHAT:
 * Generates floating islands of ivory Akash Marble (AKASH_MARBLE) adorned with glowing
 * divine crystals (AKASH_CRYSTAL) suspended in open celestial skies between Y=70 and Y=130.
 */
public class AkashDweepGenerator {

    private final long seed;
    private final OctaveNoise islandNoise;

    public AkashDweepGenerator(long seed) {
        this.seed = seed;
        this.islandNoise = new OctaveNoise(seed + 7777L, 3, 0.5, 2.0);
    }

    /**
     * Generates a 16x256x16 celestial floating island chunk.
     */
    public void generateColumn(ChunkColumn col) {
        int cx = col.getChunkX();
        int cz = col.getChunkZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = (cx << 4) + lx;
                int wz = (cz << 4) + lz;

                // Central island presence boost near origin
                double distFromOrigin = Math.hypot(wx, wz);
                double centerBias = Math.max(0.0, 1.0 - (distFromOrigin / 80.0));

                double noiseVal = islandNoise.noise2(wx * 0.03, wz * 0.03) + (centerBias * 0.6);

                if (noiseVal > 0.15) {
                    int baseHeight = 85 + (int) (noiseVal * 25.0);
                    int thickness = 8 + (int) (noiseVal * 16.0);

                    for (int dy = 0; dy < thickness; dy++) {
                        int y = baseHeight - dy;
                        if (y >= 0 && y < 256) {
                            if (dy == 0) {
                                // Top surface: Marble with rare crystal spikes
                                if ((wx * 7 + wz * 13 + y) % 19 == 0) {
                                    col.setBlock(lx, y, lz, BlockIds.AKASH_CRYSTAL);
                                } else {
                                    col.setBlock(lx, y, lz, BlockIds.AKASH_MARBLE);
                                }
                            } else {
                                col.setBlock(lx, y, lz, BlockIds.AKASH_MARBLE);
                            }
                        }
                    }
                }
            }
        }

        // Return portal gate on origin central island
        if (cx == 0 && cz == 0) {
            generateCelestialPortal(col);
        }
    }

    private void generateCelestialPortal(ChunkColumn col) {
        // Construct marble shrine with celestial portal at (7, 100, 7)
        int py = 95;
        for (int x = 5; x <= 10; x++) {
            for (int z = 5; z <= 10; z++) {
                col.setBlock(x, py, z, BlockIds.AKASH_MARBLE);
            }
        }
        // Pillars of Crystal
        col.setBlock(5, py + 1, 5, BlockIds.AKASH_CRYSTAL);
        col.setBlock(5, py + 2, 5, BlockIds.AKASH_CRYSTAL);
        col.setBlock(10, py + 1, 5, BlockIds.AKASH_CRYSTAL);
        col.setBlock(10, py + 2, 5, BlockIds.AKASH_CRYSTAL);
        col.setBlock(5, py + 1, 10, BlockIds.AKASH_CRYSTAL);
        col.setBlock(5, py + 2, 10, BlockIds.AKASH_CRYSTAL);
        col.setBlock(10, py + 1, 10, BlockIds.AKASH_CRYSTAL);
        col.setBlock(10, py + 2, 10, BlockIds.AKASH_CRYSTAL);

        // Center Portal Gateway
        col.setBlock(7, py + 1, 7, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(8, py + 1, 7, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(7, py + 1, 8, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(8, py + 1, 8, BlockIds.DIMENSIONAL_PORTAL);
    }
}
