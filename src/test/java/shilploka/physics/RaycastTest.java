package shilploka.physics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies 3D Fast Voxel Traversal (Amanatides-Woo DDA) raymarching accuracy,
 * face normal determination, and block placement coordinate extraction.
 *
 * WHAT:
 * Tests cardinal ray traces along X, Y, and Z axes against mock world setups,
 * confirming hit identification, reach clamping, and empty void misses.
 *
 * USED FOR:
 * Automated CI verification of first-person crosshair interaction.
 */
class RaycastTest {

    @Test
    @DisplayName("Raycast along +X axis accurately detects block, face normal, and placement coordinate")
    void testRaycastHitAlongX() {
        Map<String, Integer> worldBlocks = new HashMap<>();
        worldBlocks.put("3,0,0", BlockIds.STONE);
        BlockGetter world = (x, y, z) -> worldBlocks.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        // Cast ray from (0.5, 0.5, 0.5) in direction (+1, 0, 0)
        RaycastResult result = Raycaster.raycast(0.5, 0.5, 0.5, 1.0, 0.0, 0.0, 5.0, world);

        assertTrue(result.hasHit(), "Ray must hit solid stone block at (3, 0, 0)");
        assertEquals(3, result.blockX());
        assertEquals(0, result.blockY());
        assertEquals(0, result.blockZ());
        assertEquals(BlockIds.STONE, result.blockId());

        // Hit West face of block, normal must point toward -X
        assertEquals(-1, result.normalX());
        assertEquals(0, result.normalY());
        assertEquals(0, result.normalZ());

        // Block placement coordinate must be immediately adjacent at (2, 0, 0)
        assertEquals(2, result.placeX());
        assertEquals(0, result.placeY());
        assertEquals(0, result.placeZ());

        assertEquals(2.5, result.distance(), 1e-4);
    }

    @Test
    @DisplayName("Raycast upward along +Y axis accurately identifies bottom face normal")
    void testRaycastHitAlongY() {
        Map<String, Integer> worldBlocks = new HashMap<>();
        worldBlocks.put("0,4,0", BlockIds.SAL_PLANKS);
        BlockGetter world = (x, y, z) -> worldBlocks.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        // Cast ray upward from (0.5, 1.0, 0.5) in direction (0, 1, 0)
        RaycastResult result = Raycaster.raycast(0.5, 1.0, 0.5, 0.0, 1.0, 0.0, 5.0, world);

        assertTrue(result.hasHit());
        assertEquals(0, result.blockX());
        assertEquals(4, result.blockY());
        assertEquals(0, result.blockZ());

        // Hit bottom face, normal points down (-Y)
        assertEquals(0, result.normalX());
        assertEquals(-1, result.normalY());
        assertEquals(0, result.normalZ());

        assertEquals(0, result.placeX());
        assertEquals(3, result.placeY());
        assertEquals(0, result.placeZ());
    }

    @Test
    @DisplayName("Raycast into open air or beyond reach returns MISS")
    void testRaycastMiss() {
        BlockGetter emptyWorld = (x, y, z) -> BlockIds.AIR;

        RaycastResult result = Raycaster.raycast(0.0, 64.0, 0.0, 0.0, 0.0, 1.0, 5.0, emptyWorld);
        assertFalse(result.hasHit(), "Raycast into empty air must return MISS");

        // Target is at 8 blocks, but reach is capped at 5 blocks
        Map<String, Integer> worldBlocks = new HashMap<>();
        worldBlocks.put("0,0,8", BlockIds.STONE);
        BlockGetter distantWorld = (x, y, z) -> worldBlocks.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        RaycastResult distantResult = Raycaster.raycast(0.5, 0.5, 0.5, 0.0, 0.0, 1.0, 5.0, distantWorld);
        assertFalse(distantResult.hasHit(), "Raycast exceeding reach distance must return MISS");
    }
}
