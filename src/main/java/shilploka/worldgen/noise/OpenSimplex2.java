package shilploka.worldgen.noise;

/**
 * WHY:
 * Classic Perlin noise exhibits directional grid-aligned artifacts and patent constraints.
 * OpenSimplex2 provides isotropic, smooth, continuous gradient noise across 2D and 3D space
 * with zero heap allocations during query loops.
 *
 * WHAT:
 * Implements 2D simplex skew/unskew and 3D BCC (Body-Centered Cubic) lattice evaluation
 * using hash-based gradient orientation. Deterministically seeded with 64-bit seed integers.
 *
 * USED FOR:
 * Continental heightmaps, regional hill noise, temperature/moisture fields, and 3D cave carvers.
 *
 * WHERE NEXT:
 * Wrapped by OctaveNoise to build Fractal Brownian Motion (fBm) multi-octave terrain.
 */
public class OpenSimplex2 {

    private static final double STRETCH_2D = -0.211324865405187; // (1/sqrt(2+1)-1)/2
    private static final double SQUISH_2D = 0.366025403784439;   // (sqrt(2+1)-1)/2
    private static final double NORM_2D = 1.0 / 47.0;

    private static final double STRETCH_3D = -1.0 / 6.0;         // (1/sqrt(3+1)-1)/3
    private static final double SQUISH_3D = 1.0 / 3.0;           // (sqrt(3+1)-1)/3
    private static final double NORM_3D = 1.0 / 103.0;

    // 2D Gradient directions (unit circle vectors)
    private static final double[] GRADIENTS_2D = {
            5, 2, 2, 5, -5, 2, -2, 5,
            5, -2, 2, -5, -5, -2, -2, -5
    };

    // 3D Gradient directions (rhombic dodecahedron vertices)
    private static final double[] GRADIENTS_3D = {
            1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1, 0,
            1, 0, 1, -1, 0, 1, 1, 0, -1, -1, 0, -1,
            0, 1, 1, 0, -1, 1, 0, 1, -1, 0, -1, -1,
            1, 1, 0, 0, -1, 1, -1, 1, 0, 0, -1, -1
    };

    private final long seed;

    public OpenSimplex2(long seed) {
        this.seed = seed;
    }

    /**
     * WHY:
     * Evaluates continuous 2D simplex noise for planar terrain heightmaps and climate fields.
     *
     * WHAT:
     * Skews (x, y) into triangular grid cells, determines simplicial sub-triangle,
     * evaluates 3 kernel contributions, and returns a value normalized to [-1.0, 1.0].
     *
     * @param x World X coordinate
     * @param y World Z coordinate
     * @return Noise value between -1.0 and 1.0
     */
    public double noise2(double x, double y) {
        double stretchOffset = (x + y) * STRETCH_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);

        double squishOffset = (xsb + ysb) * SQUISH_2D;
        double dx0 = x - (xsb + squishOffset);
        double dy0 = y - (ysb + squishOffset);

        double xins = xs - xsb;
        double yins = ys - ysb;

        double inSum = xins + yins;
        double value = 0.0;

        // Contribution (1, 0) or (0, 1)
        int xsv_ext, ysv_ext;
        double dx_ext, dy_ext;
        if (xins > yins) {
            xsv_ext = xsb + 1;
            ysv_ext = ysb;
            dx_ext = dx0 - 1 - SQUISH_2D;
            dy_ext = dy0 - SQUISH_2D;
        } else {
            xsv_ext = xsb;
            ysv_ext = ysb + 1;
            dx_ext = dx0 - SQUISH_2D;
            dy_ext = dy0 - 1 - SQUISH_2D;
        }

        // Vertex 0: (0, 0)
        double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * grad2(hash2D(seed, xsb, ysb), dx0, dy0);
        }

        // Vertex 1: (1, 0) or (0, 1)
        double attn1 = 2.0 - dx_ext * dx_ext - dy_ext * dy_ext;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * grad2(hash2D(seed, xsv_ext, ysv_ext), dx_ext, dy_ext);
        }

        // Vertex 2: (1, 1)
        double dx2 = dx0 - 1.0 - 2.0 * SQUISH_2D;
        double dy2 = dy0 - 1.0 - 2.0 * SQUISH_2D;
        double attn2 = 2.0 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * grad2(hash2D(seed, xsb + 1, ysb + 1), dx2, dy2);
        }

        return Math.max(-1.0, Math.min(1.0, value * NORM_2D));
    }

    /**
     * WHY:
     * Evaluates continuous 3D noise for underground cave carving and ore vein clustering.
     *
     * WHAT:
     * Projects 3D space onto a tetrahedral lattice, computes 4 kernel contributions,
     * and returns a normalized value between [-1.0, 1.0].
     */
    public double noise3(double x, double y, double z) {
        double stretchOffset = (x + y + z) * STRETCH_3D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);

        double squishOffset = (xsb + ysb + zsb) * SQUISH_3D;
        double dx0 = x - (xsb + squishOffset);
        double dy0 = y - (ysb + squishOffset);
        double dz0 = z - (zsb + squishOffset);

        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;

        double value = 0.0;

        // Base vertex (0, 0, 0)
        double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * grad3(hash3D(seed, xsb, ysb, zsb), dx0, dy0, dz0);
        }

        // Determine simplex traversal path based on largest coordinate offsets
        int x1 = 0, y1 = 0, z1 = 0;
        int x2 = 0, y2 = 0, z2 = 0;

        if (xins >= yins) {
            if (yins >= zins) {
                x1 = 1; y1 = 0; z1 = 0; x2 = 1; y2 = 1; z2 = 0; // X, Y, Z
            } else if (xins >= zins) {
                x1 = 1; y1 = 0; z1 = 0; x2 = 1; y2 = 0; z2 = 1; // X, Z, Y
            } else {
                x1 = 0; y1 = 0; z1 = 1; x2 = 1; y2 = 0; z2 = 1; // Z, X, Y
            }
        } else {
            if (yins < zins) {
                x1 = 0; y1 = 0; z1 = 1; x2 = 0; y2 = 1; z2 = 1; // Z, Y, X
            } else if (xins < zins) {
                x1 = 0; y1 = 1; z1 = 0; x2 = 0; y2 = 1; z2 = 1; // Y, Z, X
            } else {
                x1 = 0; y1 = 1; z1 = 0; x2 = 1; y2 = 1; z2 = 0; // Y, X, Z
            }
        }

        // Vertex 1
        double dx1 = dx0 - x1 - SQUISH_3D;
        double dy1 = dy0 - y1 - SQUISH_3D;
        double dz1 = dz0 - z1 - SQUISH_3D;
        double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * grad3(hash3D(seed, xsb + x1, ysb + y1, zsb + z1), dx1, dy1, dz1);
        }

        // Vertex 2
        double dx2 = dx0 - x2 - 2.0 * SQUISH_3D;
        double dy2 = dy0 - y2 - 2.0 * SQUISH_3D;
        double dz2 = dz0 - z2 - 2.0 * SQUISH_3D;
        double attn2 = 2.0 - dx2 * dx2 - dy2 * dy2 - dz2 * dz2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * grad3(hash3D(seed, xsb + x2, ysb + y2, zsb + z2), dx2, dy2, dz2);
        }

        // Vertex 3: (1, 1, 1)
        double dx3 = dx0 - 1.0 - 3.0 * SQUISH_3D;
        double dy3 = dy0 - 1.0 - 3.0 * SQUISH_3D;
        double dz3 = dz0 - 1.0 - 3.0 * SQUISH_3D;
        double attn3 = 2.0 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
        if (attn3 > 0) {
            attn3 *= attn3;
            value += attn3 * attn3 * grad3(hash3D(seed, xsb + 1, ysb + 1, zsb + 1), dx3, dy3, dz3);
        }

        return Math.max(-1.0, Math.min(1.0, value * NORM_3D));
    }

    private static double grad2(int hash, double dx, double dy) {
        int index = (hash & 7) << 1;
        return GRADIENTS_2D[index] * dx + GRADIENTS_2D[index + 1] * dy;
    }

    private static double grad3(int hash, double dx, double dy, double dz) {
        int index = (hash & 15) * 3;
        return GRADIENTS_3D[index] * dx + GRADIENTS_3D[index + 1] * dy + GRADIENTS_3D[index + 2] * dz;
    }

    private static int hash2D(long seed, int x, int y) {
        long h = seed + x * 374761393L + y * 668265263L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return (int) (h ^ (h >>> 16));
    }

    private static int hash3D(long seed, int x, int y, int z) {
        long h = seed + x * 374761393L + y * 668265263L + z * 1013904223L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return (int) (h ^ (h >>> 16));
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    public long getSeed() {
        return seed;
    }
}
