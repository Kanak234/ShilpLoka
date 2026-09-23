package shilploka.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.entity.living.fauna.GauEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkManager;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Ensures physical realism and mathematical accuracy for ItemEntity and LivingEntity
 * physics, including fluid buoyancy, viscous drag, falling gravity, and terrain collision.
 */
class EntityPhysicsTest {

    @Test
    @DisplayName("ItemEntity in air falls under gravity and lands on solid ground")
    void testItemFallingAndGroundLanding() {
        Map<String, Integer> worldBlocks = new HashMap<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                worldBlocks.put(x + ",0," + z, BlockIds.STONE);
            }
        }
        BlockGetter world = (x, y, z) -> worldBlocks.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        ItemStack stack = new ItemStack(ItemRegistry.LOHA_INGOT, 1);
        ItemEntity item = new ItemEntity(0.0, 5.0, 0.0, stack, 0.0, 0.0, 0.0);

        // Tick for 20 frames (1 second)
        double dt = 0.05;
        for (int i = 0; i < 20; i++) {
            item.tick(dt, world, null);
        }

        assertTrue(item.isOnGround(), "Item entity must be marked onGround after landing");
        assertEquals(1.0, item.getPosition().y, 1e-4, "Item entity must rest on top of solid block at Y=1.0");
        assertEquals(0.0, item.getVelocity().y, 1e-4, "Vertical velocity must zero out on landing");
    }

    @Test
    @DisplayName("ItemEntity submerged in water floats upward with fluid buoyancy")
    void testItemWaterBuoyancy() {
        // Water pool from Y=0 to Y=10
        BlockGetter waterWorld = (x, y, z) -> (y >= 0 && y <= 10) ? BlockIds.WATER : BlockIds.AIR;

        ItemStack stack = new ItemStack(ItemRegistry.LAKDI_CHHAD, 1);
        ItemEntity item = new ItemEntity(0.0, 2.0, 0.0, stack, 0.0, -1.0, 0.0);

        // Tick submerged in water
        double dt = 0.05;
        for (int i = 0; i < 15; i++) {
            item.tick(dt, waterWorld, null);
        }

        assertTrue(item.isInWater(), "Item entity submerged in water must detect water immersion");
        assertTrue(item.getVelocity().y > 0.0, "Fluid buoyancy must accelerate item upward");
        assertTrue(item.getPosition().y > 2.0, "Item entity must rise toward water surface");
    }

    @Test
    @DisplayName("LivingEntity submerged in water floats toward surface with fluid buoyancy")
    void testLivingEntityWaterBuoyancy() {
        ChunkManager chunkManager = new ChunkManager(new WorldGenerator(42L));
        // Fill a 3x3 column with water at Y=10..20
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 5; y <= 25; y++) {
                    chunkManager.setBlock(x, y, z, BlockIds.WATER, false);
                }
            }
        }

        GauEntity gau = new GauEntity(0.5, 12.0, 0.5, chunkManager);

        // Tick 10 frames
        float dt = 0.05f;
        for (int i = 0; i < 10; i++) {
            gau.tick(dt, chunkManager, null);
        }

        assertTrue(gau.isInWater(), "Gau inside water column must have inWater == true");
        assertTrue(gau.getPosition().y > 12.0, "Living entity must float upward in water rather than sinking");
    }

    @Test
    @DisplayName("LivingEntity falls under gravity and rests firmly on ground obstacle")
    void testLivingEntityGroundCollision() {
        ChunkManager chunkManager = new ChunkManager(new WorldGenerator(42L));
        // Build solid platform at Y=10
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                chunkManager.setBlock(x, 10, z, BlockIds.STONE, false);
                for (int y = 11; y <= 25; y++) {
                    chunkManager.setBlock(x, y, z, BlockIds.AIR, false);
                }
            }
        }

        GauEntity gau = new GauEntity(0.0, 15.0, 0.0, chunkManager);

        // Simulate 20 frames of falling
        float dt = 0.05f;
        for (int i = 0; i < 20; i++) {
            gau.tick(dt, chunkManager, null);
        }

        assertTrue(gau.isOnGround(), "LivingEntity must be onGround after landing on stone platform");
        assertEquals(11.0, gau.getY(), 1e-4, "LivingEntity feet must rest at Y=11.0 on top of stone at Y=10");
    }
}
