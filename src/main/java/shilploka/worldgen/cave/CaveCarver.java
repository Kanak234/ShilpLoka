package shilploka.worldgen.cave;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.noise.OpenSimplex2;

/**
 * WHY:
 * Subterranean Bharat features ancient natural limestone caverns, subterranean rivers,
 * and winding fissure networks. Caves must carve through deep stone strata while strictly
 * preserving bedrock foundations (Y=0..3) and not breaching the surface layer unexpectedly.
 *
 * WHAT:
 * Evaluates dual 3D continuous OpenSimplex2 worm-tunnel noise fields plus an intermittent
 * cavern chamber noise field. Where the intersection threshold is met between Y=8 and Y=56,
 * stone blocks are replaced with BlockIds.AIR.
 *
 * USED FOR:
 * Executed per chunk column during the subterranean carver pass of WorldGenerator.
 *
 * WHERE NEXT:
 * After caves are carved, OrePopulator deposits mineral veins into the exposed cavern walls.
 */
public class CaveCarver {

    private final OpenSimplex2 wormNoiseA;
    private final OpenSimplex2 wormNoiseB;
    private final OpenSimplex2 chamberNoise;

    // Cave noise frequencies
    private static final double WORM_FREQ_XZ = 0.025;
    private static final double WORM_FREQ_Y = 0.040;
    private static final double CHAMBER_FREQ = 0.018;

    // Tunnel width threshold (smaller = narrower tunnel, larger = wider tunnel)
    private static final double WORM_THRESHOLD = 0.085;
    private static final double CHAMBER_THRESHOLD = 0.60;

    public CaveCarver(long seed) {
        this.wormNoiseA = new OpenSimplex2(seed + 301L);
        this.wormNoiseB = new OpenSimplex2(seed + 402L);
        this.chamberNoise = new OpenSimplex2(seed + 503L);
    }

    /**
     * WHY:
     * Carves subterranean cave tunnels and chambers into a ChunkColumn.
     *
     * WHAT:
     * Iterates horizontal voxels (x, z) within the chunk, and vertical heights y from 8 to
     * min(56, surfaceHeight - 5). If dual worm noise criteria or cavern chamber criteria
     * are satisfied, the block is carved to AIR.
     *
     * @param column        The target ChunkColumn to carve.
     * @param chunkX        World chunk X coordinate.
     * @param chunkZ        World chunk Z coordinate.
     * @param heightMap16x16 Precomputed surface elevation heights for this chunk.
     */
    public void carveColumn(ChunkColumn column, int chunkX, int chunkZ, int[][] heightMap16x16) {
        for (int x = 0; x < 16; x++) {
            int worldX = (chunkX * 16) + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = (chunkZ * 16) + z;
                int surfaceHeight = heightMap16x16[x][z];
                int maxCaveY = Math.min(56, surfaceHeight - 5);

                for (int y = 8; y <= maxCaveY; y++) {
                    int currentBlock = column.getBlock(x, y, z);
                    // Only carve subterranean stone/granite, never bedrock
                    if (currentBlock != BlockIds.STONE && currentBlock != BlockIds.GRANITE) {
                        continue;
                    }

                    if (isCaveVoxel(worldX, y, worldZ)) {
                        column.setBlock(x, y, z, BlockIds.AIR);
                    }
                }
            }
        }
    }

    /**
     * WHY:
     * Checks if coordinates (worldX, y, worldZ) fall inside a 3D cave tunnel or chamber.
     *
     * WHAT:
     * Worm caves form where two independent 3D scalar fields simultaneously cross zero
     * (the intersection of two continuous 3D surfaces forms a 3D spline tube).
     * Chambers form at positive localized peaks of the chamber noise field.
     */
    public boolean isCaveVoxel(int worldX, int y, int worldZ) {
        if (y < 8 || y > 56) return false;

        double wx = worldX * WORM_FREQ_XZ;
        double wy = y * WORM_FREQ_Y;
        double wz = worldZ * WORM_FREQ_XZ;

        double nA = wormNoiseA.noise3(wx, wy, wz);
        double nB = wormNoiseB.noise3(wx, wy, wz);

        // 1. Dual worm tunnel intersection
        if (Math.abs(nA) < WORM_THRESHOLD && Math.abs(nB) < WORM_THRESHOLD) {
            return true;
        }

        // 2. Cavern room chambers (spherical hollows)
        double chamber = chamberNoise.noise3(worldX * CHAMBER_FREQ, y * CHAMBER_FREQ, worldZ * CHAMBER_FREQ);
        return chamber > CHAMBER_THRESHOLD;
    }
}
