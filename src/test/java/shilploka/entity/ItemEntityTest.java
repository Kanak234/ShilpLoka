package shilploka.entity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates 3D ItemEntity physics: gravitational descent, terrain contact, player magnet attraction,
 * and inventory pickup absorption.
 *
 * WHAT:
 * Tests that floating items settle on ground blocks, accelerate toward nearby players,
 * and transfer item stacks into PlayerInventory.
 *
 * USED FOR:
 * Verifies Phase P7 item drop entity pipeline.
 */
public class ItemEntityTest {

    @BeforeAll
    static void initRegistry() {
        ItemRegistry.init();
    }

    @Test
    @DisplayName("ItemEntity descends under gravity and rests on solid ground block")
    void testGravityAndGroundCollision() {
        ItemStack stack = new ItemStack(ItemRegistry.getItem("shilploka:sal_log"), 1);
        ItemEntity entity = new ItemEntity(0.5, 10.0, 0.5, stack, 0.0, 0.0, 0.0);

        // Mock simple world where Y <= 1 is solid stone
        BlockGetter world = (x, y, z) -> (y <= 1) ? BlockIds.STONE : BlockIds.AIR;

        // Tick 20 frames (1 second)
        for (int i = 0; i < 20; i++) {
            entity.tick(0.05, world, null);
        }

        // Entity should have fallen and landed on top of Y=1 (Y=2.0)
        assertEquals(2.0, entity.getPosition().y, 0.01);
        assertEquals(0.0, entity.getVelocity().y, 0.01);
    }

    @Test
    @DisplayName("Magnet pulls item toward player when within 2.0 block radius")
    void testMagnetAttraction() {
        ItemStack stack = new ItemStack(ItemRegistry.getItem("shilploka:fired_brick"), 1);
        // Entity at (1.0, 2.0, 0.0)
        ItemEntity entity = new ItemEntity(1.0, 2.0, 0.0, stack, 0.0, 0.0, 0.0);
        entity.setPickupDelay(0); // Ready for pickup immediately

        Player player = new Player(0.0, 2.0, 0.0);
        BlockGetter world = (x, y, z) -> BlockIds.AIR;

        // Tick 1 frame
        entity.tick(0.05, world, player);

        // Velocity should point along negative X towards player at X=0
        assertTrue(entity.getVelocity().x < 0.0);
    }

    @Test
    @DisplayName("ItemEntity despawns after 6000 ticks (5 minutes)")
    void testDespawnTimer() {
        ItemStack stack = new ItemStack(ItemRegistry.getItem("shilploka:diya"), 1);
        ItemEntity entity = new ItemEntity(0.0, 5.0, 0.0, stack);
        assertFalse(entity.isDead());

        BlockGetter world = (x, y, z) -> BlockIds.AIR;
        for (int i = 0; i < 6000; i++) {
            entity.tick(0.05, world, null);
        }

        assertTrue(entity.isDead());
    }

    @Test
    @DisplayName("Player inventory absorbs ItemEntity stack upon contact")
    void testPickupAbsorption() {
        Player player = new Player(0.0, 2.0, 0.0);
        ItemStack stack = new ItemStack(ItemRegistry.getItem("shilploka:koyla"), 4);

        ItemEntity entity = new ItemEntity(0.1, 2.0, 0.1, stack, 0.0, 0.0, 0.0);
        entity.setPickupDelay(0);

        BlockGetter world = (x, y, z) -> BlockIds.AIR;
        entity.tick(0.05, world, player);

        // Distance is within pickup radius (0.8 blocks)
        double distSq = player.getPosition().distanceSquared(entity.getPosition());
        assertTrue(distSq <= ItemEntity.PICKUP_RADIUS * ItemEntity.PICKUP_RADIUS);

        ItemStack remaining = player.getInventory().addItem(entity.getItemStack());
        assertTrue(remaining.isEmpty());

        // Player now has 4 Koyla in inventory slot 0
        assertEquals(4, player.getInventory().getSlot(0).getCount());
        assertEquals("shilploka:koyla", player.getInventory().getSlot(0).getItem().getId());
    }
}
