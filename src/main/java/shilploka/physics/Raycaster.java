package shilploka.physics;

import org.joml.Vector3f;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

/**
 * WHY:
 * First-person voxel manipulation (breaking blocks, placing blocks, interacting with workstations)
 * requires identifying which voxel face the player's crosshair is targeting.
 *
 * WHAT:
 * Implements the Amanatides-Woo Fast Voxel Traversal (3D Digital Differential Analyzer)
 * raymarching algorithm. Efficiently steps through the discrete 3D voxel grid along
 * the camera sightline up to maxReach blocks without missing corner crossings or tunneling.
 *
 * USED FOR:
 * Player left-click block breaking and right-click block placing.
 *
 * WHERE NEXT:
 * Player.interact() queries Raycaster to modify voxel blocks in ChunkManager.
 */
public class Raycaster {

    public static final double DEFAULT_REACH = 5.0; // 5 blocks maximum reach

    /**
     * Casts a ray from eye position along look direction against the voxel world.
     *
     * @param origin    Camera eye world position (x, y, z)
     * @param direction Normalized gaze vector
     * @param maxDist   Maximum interaction distance in meters (blocks)
     * @param world     World block query interface
     * @return RaycastResult with hit coordinate, face normal, and placement coordinate
     */
    public static RaycastResult raycast(Vector3f origin, Vector3f direction, double maxDist, BlockGetter world) {
        return raycast(origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, maxDist, world);
    }

    /**
     * Primitive overload for zero-allocation raymarching.
     */
    public static RaycastResult raycast(double x0, double y0, double z0,
                                       double dx, double dy, double dz,
                                       double maxDist, BlockGetter world) {
        if (world == null) return RaycastResult.MISS;

        // Current voxel coordinates
        int x = (int) Math.floor(x0);
        int y = (int) Math.floor(y0);
        int z = (int) Math.floor(z0);

        // Check if origin itself is inside a solid block
        int startBlock = world.getBlock(x, y, z);
        if (BlockIds.isSolid(startBlock)) {
            return RaycastResult.hit(x, y, z, 0, 1, 0, startBlock, 0.0);
        }

        // Step directions along each axis
        int stepX = (dx > 0) ? 1 : ((dx < 0) ? -1 : 0);
        int stepY = (dy > 0) ? 1 : ((dy < 0) ? -1 : 0);
        int stepZ = (dz > 0) ? 1 : ((dz < 0) ? -1 : 0);

        // Distance along ray to next voxel boundary
        double tMaxX = (stepX > 0) ? (x + 1.0 - x0) / dx : ((stepX < 0) ? (x0 - x) / -dx : Double.POSITIVE_INFINITY);
        double tMaxY = (stepY > 0) ? (y + 1.0 - y0) / dy : ((stepY < 0) ? (y0 - y) / -dy : Double.POSITIVE_INFINITY);
        double tMaxZ = (stepZ > 0) ? (z + 1.0 - z0) / dz : ((stepZ < 0) ? (z0 - z) / -dz : Double.POSITIVE_INFINITY);

        // Voxel traversal distance delta along each axis
        double tDeltaX = (dx != 0) ? Math.abs(1.0 / dx) : Double.POSITIVE_INFINITY;
        double tDeltaY = (dy != 0) ? Math.abs(1.0 / dy) : Double.POSITIVE_INFINITY;
        double tDeltaZ = (dz != 0) ? Math.abs(1.0 / dz) : Double.POSITIVE_INFINITY;

        int normalX = 0;
        int normalY = 0;
        int normalZ = 0;
        double currentT = 0.0;

        // Step through voxel boundaries
        while (currentT <= maxDist) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                if (tMaxX > maxDist) break;
                x += stepX;
                currentT = tMaxX;
                tMaxX += tDeltaX;
                normalX = -stepX;
                normalY = 0;
                normalZ = 0;
            } else if (tMaxY < tMaxZ) {
                if (tMaxY > maxDist) break;
                y += stepY;
                currentT = tMaxY;
                tMaxY += tDeltaY;
                normalX = 0;
                normalY = -stepY;
                normalZ = 0;
            } else {
                if (tMaxZ > maxDist) break;
                z += stepZ;
                currentT = tMaxZ;
                tMaxZ += tDeltaZ;
                normalX = 0;
                normalY = 0;
                normalZ = -stepZ;
            }

            int block = world.getBlock(x, y, z);
            if (BlockIds.isSolid(block)) {
                return RaycastResult.hit(x, y, z, normalX, normalY, normalZ, block, currentT);
            }
        }

        return RaycastResult.MISS;
    }
}
