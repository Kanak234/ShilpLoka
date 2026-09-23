package shilploka.world.light;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating cellular light propagation (Dhoop & Prakash),
 * sunlight column casting, cave shadows, Diya block light radius, and light removal.
 */
public class LightPropagationTest {

    private static class MockLightingWorld implements WorldLightingAccess {
        private final Map<Long, Integer> blocks = new HashMap<>();
        private final Map<Long, Integer> skyLight = new HashMap<>();
        private final Map<Long, Integer> blockLight = new HashMap<>();

        private long key(int x, int y, int z) {
            return (((long) x) << 42) | (((long) (y & 0x3FF)) << 32) | (z & 0xFFFFFFFFL);
        }

        public void setBlock(int x, int y, int z, int id) {
            blocks.put(key(x, y, z), id);
        }

        @Override
        public int getBlock(int x, int y, int z) {
            return blocks.getOrDefault(key(x, y, z), BlockIds.AIR);
        }

        @Override
        public int getSkyLight(int x, int y, int z) {
            return skyLight.getOrDefault(key(x, y, z), 0);
        }

        @Override
        public int getBlockLight(int x, int y, int z) {
            return blockLight.getOrDefault(key(x, y, z), 0);
        }

        @Override
        public void setSkyLight(int x, int y, int z, int value) {
            skyLight.put(key(x, y, z), value);
        }

        @Override
        public void setBlockLight(int x, int y, int z, int value) {
            blockLight.put(key(x, y, z), value);
        }

        @Override
        public boolean isLoaded(int cx, int cz) {
            return true;
        }
    }

    private LightingEngine lightingEngine;
    private MockLightingWorld world;

    @BeforeEach
    void setUp() {
        lightingEngine = new LightingEngine();
        world = new MockLightingWorld();
    }

    @Test
    @DisplayName("Sunlight column casts 15 down to first solid block and leaves cave beneath at 0")
    void testSunlightColumnAndCaveShadow() {
        ChunkColumn column = new ChunkColumn(0, 0);

        // Place a solid stone roof at Y = 60
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                column.setBlock(x, 60, z, BlockIds.STONE);
            }
        }

        // Initialize sunlight
        lightingEngine.initializeColumnSunlight(column, world);

        // Voxels above roof (e.g. Y=61 to 319) must have Sky Light = 15
        assertEquals(15, column.getSkyLight(8, 100, 8), "Open sky must receive full sunlight (15)");
        assertEquals(15, column.getSkyLight(8, 61, 8), "Voxel directly above roof must receive full sunlight (15)");

        // Voxels directly beneath roof (e.g. Y=59 down to 0) must remain at 0 in complete cave shadow
        assertEquals(0, column.getSkyLight(8, 59, 8), "Cave beneath unbroken solid roof must have 0 Sky Light");
        assertEquals(0, column.getSkyLight(8, 10, 8), "Deep subterranean chamber must have 0 Sky Light");
    }

    @Test
    @DisplayName("Diya (oil lamp) emits block light 14 and attenuates to 0 across 14 blocks")
    void testDiyaBlockLightPropagation() {
        // Place Diya in an open room at (10, 20, 10)
        world.setBlock(10, 20, 10, BlockIds.DIYA);
        lightingEngine.onBlockPlaced(10, 20, 10, BlockIds.DIYA, world);

        // Diya source voxel must be 14
        assertEquals(14, world.getBlockLight(10, 20, 10), "Diya position must have light level 14");

        // Orthogonal neighbors must have 13
        assertEquals(13, world.getBlockLight(11, 20, 10));
        assertEquals(13, world.getBlockLight(9, 20, 10));
        assertEquals(13, world.getBlockLight(10, 21, 10));
        assertEquals(13, world.getBlockLight(10, 19, 10));

        // Distance 2 must have 12
        assertEquals(12, world.getBlockLight(12, 20, 10));

        // Distance 14 must have 0 (14 - 14 = 0)
        assertEquals(0, world.getBlockLight(24, 20, 10));
    }

    @Test
    @DisplayName("Breaking a Diya removes its block light from all illuminated voxels")
    void testDiyaBlockLightRemoval() {
        world.setBlock(10, 20, 10, BlockIds.DIYA);
        lightingEngine.onBlockPlaced(10, 20, 10, BlockIds.DIYA, world);
        assertTrue(world.getBlockLight(11, 20, 10) > 0);

        // Break Diya
        world.setBlock(10, 20, 10, BlockIds.AIR);
        lightingEngine.onBlockBroken(10, 20, 10, BlockIds.DIYA, world);

        // All voxels must return to 0
        assertEquals(0, world.getBlockLight(10, 20, 10), "Broken Diya source must be 0");
        assertEquals(0, world.getBlockLight(11, 20, 10), "Neighbor must return to 0");
        assertEquals(0, world.getBlockLight(10, 21, 10), "Neighbor above must return to 0");
    }
}
