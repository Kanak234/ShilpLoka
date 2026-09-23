package shilploka.world.fluid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating cellular automata fluid spreading (downward cascade and lateral flow),
 * water-lava solidification into Deccan Basalt, and gravitational falling of Thar Sand.
 */
public class FluidSimulationTest {

    private ChunkManager chunkManager;
    private FluidSimulator fluidSimulator;

    @BeforeEach
    void setUp() {
        WorldGenerator worldGen = new WorldGenerator(42L);
        chunkManager = new ChunkManager(worldGen);
        fluidSimulator = chunkManager.getFluidSimulator();
    }

    @Test
    @DisplayName("Water placed in open air flows downwards creating a vertical cascade")
    void testDownwardWaterCascade() {
        // Place stone floor at Y=20, leaving air at Y=21..25
        chunkManager.setBlock(10, 20, 10, BlockIds.STONE);
        chunkManager.setBlock(10, 21, 10, BlockIds.AIR);
        chunkManager.setBlock(10, 22, 10, BlockIds.AIR);

        // Place water source at (10, 23, 10)
        chunkManager.setBlock(10, 23, 10, BlockIds.WATER);

        // Tick simulation
        fluidSimulator.tick(chunkManager);

        // Water should have cascaded down to Y=22
        assertEquals(BlockIds.WATER_FLOWING, chunkManager.getBlock(10, 22, 10),
                "Water must cascade down into air beneath it");
    }

    @Test
    @DisplayName("Water touching Lava source transforms Lava into volcanic Deccan Basalt")
    void testWaterLavaReaction() {
        // Solid base
        chunkManager.setBlock(10, 20, 10, BlockIds.STONE);
        chunkManager.setBlock(11, 20, 10, BlockIds.STONE);

        // Place Lava at (10, 21, 10) and Water at (11, 21, 10)
        chunkManager.setBlock(10, 21, 10, BlockIds.LAVA);
        chunkManager.setBlock(11, 21, 10, BlockIds.WATER);

        // Trigger fluid simulation tick
        fluidSimulator.scheduleFluidUpdate(11, 21, 10, BlockIds.WATER, 8);
        fluidSimulator.tick(chunkManager);

        // Contact reaction should have turned the lava into Deccan Basalt
        assertEquals(BlockIds.DECCAN_BASALT, chunkManager.getBlock(10, 21, 10),
                "Water-lava contact reaction must cool molten magma into Deccan Basalt");
    }

    @Test
    @DisplayName("Thar Sand placed in mid-air falls under gravity to the nearest solid block")
    void testFallingSandGravity() {
        // Solid ground at Y=15
        chunkManager.setBlock(15, 15, 15, BlockIds.STONE);
        chunkManager.setBlock(15, 16, 15, BlockIds.AIR);
        chunkManager.setBlock(15, 17, 15, BlockIds.AIR);
        chunkManager.setBlock(15, 18, 15, BlockIds.AIR);

        // Place Thar Sand floating at Y=18
        chunkManager.setBlock(15, 18, 15, BlockIds.THAR_SAND);

        // Applying falling check should collapse the sand down to Y=16 (resting on stone at 15)
        assertEquals(BlockIds.AIR, chunkManager.getBlock(15, 18, 15), "Original floating position must become AIR");
        assertEquals(BlockIds.THAR_SAND, chunkManager.getBlock(15, 16, 15), "Sand must settle directly above stone floor");
    }
}
