package shilploka.world;

/**
 * WHY:
 * Provides a decoupled, lightweight functional abstraction for 3D voxel coordinate lookups.
 * Allows raycasting, physics, and AI systems to query block states without hard-coupling
 * to full world management or OpenGL rendering pipelines.
 *
 * WHAT:
 * Single-method interface getBlock(x, y, z) returning global block ID.
 */
@FunctionalInterface
public interface BlockGetter {

    /**
     * Queries the block ID at global world coordinates (x, y, z).
     * Returns BlockIds.AIR (0) if out of bounds or unloaded.
     */
    int getBlock(int x, int y, int z);
}
