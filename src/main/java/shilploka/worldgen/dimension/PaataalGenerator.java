package shilploka.worldgen.dimension;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.noise.OctaveNoise;
import shilploka.worldgen.noise.OpenSimplex2;

/**
 * WHY:
 * Generates the subterranean volcanic Paataal realm (पाताल लोक).
 * A hellish cavernous dimension situated beneath the mantle of ancient Bharat,
 * featuring vast seas of bubbling lava, towering basalt columns, and burning Agni stone.
 *
 * WHAT:
 * Sculptures subterranean 3D caverns between Y=0 and Y=128 bounded by solid basalt bedrock
 * at floor and ceiling, with deep lava oceans at Y <= 32 and burning Brimstone terrain.
 */
public class PaataalGenerator {

    private final long seed;
    private final OctaveNoise caveNoise3D;
    private final OctaveNoise lavaCrustNoise;

    public PaataalGenerator(long seed) {
        this.seed = seed;
        this.caveNoise3D = new OctaveNoise(seed + 9999L, 3, 0.5, 2.0);
        this.lavaCrustNoise = new OctaveNoise(seed + 8888L, 2, 0.5, 2.0);
    }

    /**
     * Generates a 16x128x16 Paataal subterranean chunk.
     */
    public void generateColumn(ChunkColumn col) {
        int cx = col.getChunkX();
        int cz = col.getChunkZ();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = (cx << 4) + lx;
                int wz = (cz << 4) + lz;

                // 1. Bedrock boundaries (Y=0..3 and Y=125..127)
                col.setBlock(lx, 0, lz, BlockIds.DECCAN_BASALT);
                col.setBlock(lx, 1, lz, BlockIds.DECCAN_BASALT);
                col.setBlock(lx, 2, lz, BlockIds.DECCAN_BASALT);
                col.setBlock(lx, 126, lz, BlockIds.DECCAN_BASALT);
                col.setBlock(lx, 127, lz, BlockIds.DECCAN_BASALT);

                // 2. Subterranean 3D cavern volume (Y=3 to 125)
                for (int y = 3; y < 126; y++) {
                    // Density gradient: denser near ceiling and floor, open in center
                    double floorCeilBias = (Math.abs(y - 64.0) / 64.0); // 0 at y=64, ~1 at top/bottom
                    double n3d = caveNoise3D.noise3(wx * 0.04, y * 0.04, wz * 0.04);
                    double density = n3d + (floorCeilBias * 1.3) - 0.45;

                    if (density > 0) {
                        // Solid terrain
                        if (y <= 36 && (wx + wz + y) % 7 == 0) {
                            col.setBlock(lx, y, lz, BlockIds.PAATAAL_LAVA_GEL);
                        } else if ((wx * 13 + wz * 7 + y) % 43 == 0) {
                            col.setBlock(lx, y, lz, BlockIds.SONA_ORE);
                        } else if ((wx * 5 + wz * 11 + y) % 29 == 0) {
                            col.setBlock(lx, y, lz, BlockIds.KOYLA_ORE);
                        } else {
                            col.setBlock(lx, y, lz, BlockIds.PAATAAL_AGNI_ROCK);
                        }
                    } else {
                        // Cavity / Air or Lava Sea
                        if (y <= 32) {
                            col.setBlock(lx, y, lz, BlockIds.LAVA);
                        } else {
                            col.setBlock(lx, y, lz, BlockIds.AIR);
                        }
                    }
                }
            }
        }

        // 3. Central Boss Altar at Chunk (0, 0)
        if (cx == 0 && cz == 0) {
            generateBossArenaPlatform(col);
        }
    }

    private void generateBossArenaPlatform(ChunkColumn col) {
        // Construct circular ritual arena for Andhakaar Naag boss encounter
        for (int x = 2; x <= 13; x++) {
            for (int z = 2; z <= 13; z++) {
                double dist = Math.hypot(x - 7.5, z - 7.5);
                if (dist <= 6.0) {
                    // Clear cavern space above platform
                    for (int y = 41; y <= 60; y++) {
                        col.setBlock(x, y, z, BlockIds.AIR);
                    }
                    // Platform floor
                    col.setBlock(x, 40, z, BlockIds.DECCAN_BASALT);
                    if (dist > 5.0) {
                        col.setBlock(x, 41, z, BlockIds.PAATAAL_LAVA_GEL); // Perimeter ring
                    }
                }
            }
        }
        // Central Dimensional Portal Gate
        col.setBlock(7, 41, 7, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(8, 41, 7, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(7, 41, 8, BlockIds.DIMENSIONAL_PORTAL);
        col.setBlock(8, 41, 8, BlockIds.DIMENSIONAL_PORTAL);
    }
}
