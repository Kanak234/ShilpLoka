package shilploka.world;

/**
 * WHY:
 * Lighting calculations for greedy meshing, entity rendering, mob spawning, and HUD
 * telemetry need a decoupled query interface to inspect 4-bit Sky Light (धूप / Dhoop)
 * and 4-bit Block Light (प्रकाश / Prakash) at any world coordinate without hard-coupling
 * to internal chunk column collections or OpenGL pipelines.
 *
 * WHAT:
 * Declares query methods for Sky Light (0 to 15) and Block Light (0 to 15), alongside
 * convenience calculations for combined illumination and ambient starlight minimums.
 *
 * USED FOR:
 * Implemented by ChunkManager and WorldView, and queried by GreedyMesher, Camera,
 * and Entity renderers.
 *
 * WHERE NEXT:
 * GreedyMesher calls getSkyLight and getBlockLight to calculate smooth vertex lighting
 * and corner ambient occlusion across chunk section boundaries.
 */
public interface LightAccess {

    /**
     * Retrieves the 4-bit Sky Light level (0 to 15) originating from the celestial sky.
     *
     * @param x Global world X coordinate.
     * @param y Global world Y coordinate (0 to 319).
     * @param z Global world Z coordinate.
     * @return Sky light level between 0 (complete darkness / subterranean cave) and 15 (direct sunlight).
     */
    int getSkyLight(int x, int y, int z);

    /**
     * Retrieves the 4-bit Block Light level (0 to 15) originating from placed light sources
     * like Diyas (oil lamps) or volcanic Lava.
     *
     * @param x Global world X coordinate.
     * @param y Global world Y coordinate (0 to 319).
     * @param z Global world Z coordinate.
     * @return Block light level between 0 (unlit) and 15 (maximum source emission).
     */
    int getBlockLight(int x, int y, int z);

    /**
     * Computes the combined illumination factor for a voxel given the celestial sunlight factor.
     *
     * @param x              Global world X coordinate.
     * @param y              Global world Y coordinate.
     * @param z              Global world Z coordinate.
     * @param sunlightFactor Current time-of-day sky light multiplier (0.15 at night to 1.0 at noon).
     * @return Normalized brightness between 0.08 (ambient starlight) and 1.0 (full illumination).
     */
    default float getCombinedLight(int x, int y, int z, float sunlightFactor) {
        float effectiveSky = (getSkyLight(x, y, z) / 15.0f) * sunlightFactor;
        float block = getBlockLight(x, y, z) / 15.0f;
        return Math.max(0.08f, Math.min(1.0f, Math.max(effectiveSky, block)));
    }
}
