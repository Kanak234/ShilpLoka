package shilploka.world.light;

import shilploka.world.BlockGetter;
import shilploka.world.LightAccess;

/**
 * WHY:
 * Lighting calculations require mutable access to voxel block states, Sky Light values,
 * and Block Light values across chunk boundaries without binding directly to OpenGL
 * render threads or specific data storage implementations.
 *
 * WHAT:
 * Extends BlockGetter and LightAccess with mutation primitives (setSkyLight, setBlockLight)
 * and spatial validity queries (isLoaded).
 *
 * USED FOR:
 * Implemented by ChunkManager in the live game, and implemented by test fixtures in unit tests.
 *
 * WHERE NEXT:
 * LightingEngine invokes these methods to perform cellular automata BFS flood-fill light
 * propagation and de-propagation.
 */
public interface WorldLightingAccess extends BlockGetter, LightAccess {

    /**
     * Mutates the 4-bit Sky Light level (0 to 15) at global coordinates.
     */
    void setSkyLight(int x, int y, int z, int value);

    /**
     * Mutates the 4-bit Block Light level (0 to 15) at global coordinates.
     */
    void setBlockLight(int x, int y, int z, int value);

    /**
     * Returns true if the chunk column at chunk coordinates (cx, cz) is currently loaded.
     */
    boolean isLoaded(int cx, int cz);
}
