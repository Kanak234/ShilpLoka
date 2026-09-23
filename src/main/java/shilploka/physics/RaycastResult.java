package shilploka.physics;

/**
 * WHY:
 * Encapsulates the immutable result of a 3D raymarching query into the voxel world.
 *
 * WHAT:
 * Contains the hit status, exact world block coordinates hit, the face normal vector,
 * the adjacent coordinate where a new block would be placed, the block ID hit, and distance.
 *
 * USED FOR:
 * Player crosshair targeting, block breaking, and block placement.
 */
public record RaycastResult(
        boolean hasHit,
        int blockX,
        int blockY,
        int blockZ,
        int normalX,
        int normalY,
        int normalZ,
        int placeX,
        int placeY,
        int placeZ,
        int blockId,
        double distance
) {

    public static final RaycastResult MISS = new RaycastResult(
            false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Double.POSITIVE_INFINITY
    );

    public static RaycastResult hit(int bx, int by, int bz, int nx, int ny, int nz, int blockId, double dist) {
        return new RaycastResult(
                true,
                bx, by, bz,
                nx, ny, nz,
                bx + nx, by + ny, bz + nz,
                blockId,
                dist
        );
    }
}
