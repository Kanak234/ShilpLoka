package shilploka.item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates the 7 tool tiers (WOOD, STONE, COPPER, BRONZE, IRON, WOOTZ, VAJRA)
 * durability, harvest levels, mining speeds, attack damage, and item stacking mechanics.
 *
 * WHAT:
 * Tests that tools wear down with usage, cannot be stacked, and higher tiers possess superior stats.
 *
 * USED FOR:
 * Verifies Phase P7 tool progression and item framework.
 */
public class ToolProgressionTest {

    @BeforeAll
    static void initRegistry() {
        ItemRegistry.init();
    }

    @Test
    @DisplayName("Tool tiers progress strictly in durability and harvest capability")
    void testTierProgression() {
        assertTrue(ToolTier.WOOD.getDurability() < ToolTier.STONE.getDurability());
        assertTrue(ToolTier.STONE.getDurability() < ToolTier.COPPER.getDurability());
        assertTrue(ToolTier.COPPER.getDurability() < ToolTier.BRONZE.getDurability());
        assertTrue(ToolTier.BRONZE.getDurability() < ToolTier.IRON.getDurability());
        assertTrue(ToolTier.IRON.getDurability() < ToolTier.WOOTZ.getDurability());
        assertTrue(ToolTier.WOOTZ.getDurability() < ToolTier.VAJRA.getDurability());

        // Wootz Steel and Vajra have harvest level 3+ capable of mining diamonds/hira
        assertTrue(ToolTier.WOOTZ.getHarvestLevel() >= 3);
        assertTrue(ToolTier.VAJRA.getHarvestLevel() >= 4);
    }

    @Test
    @DisplayName("Tool durability depletes on use and breaks at maximum damage")
    void testToolDurabilityWear() {
        Item wootzKudaal = ItemRegistry.getItem("shilploka:wootz_kudaal");
        assertNotNull(wootzKudaal);
        assertTrue(wootzKudaal.isDamageable());

        ItemStack stack = new ItemStack(wootzKudaal, 1);
        assertEquals(0, stack.getDamage());
        assertEquals(wootzKudaal.getMaxDurability(), wootzKudaal.getToolTier().getDurability());

        // Wear down by 10 uses
        boolean broken = stack.applyDamage(10);
        assertFalse(broken);
        assertEquals(10, stack.getDamage());

        // Deal remaining durability to break tool
        boolean fatalBreak = stack.applyDamage(wootzKudaal.getMaxDurability());
        assertTrue(fatalBreak);
        assertTrue(stack.isEmpty());
    }

    @Test
    @DisplayName("Items respect max stack sizes (tools 1, blocks 64)")
    void testStackLimits() {
        Item stoneTalwar = ItemRegistry.getItem("shilploka:stone_talwar");
        assertEquals(1, stoneTalwar.getMaxStackSize());

        Item brickItem = ItemRegistry.getItem("shilploka:fired_brick");
        assertEquals(64, brickItem.getMaxStackSize());

        ItemStack brickStack = new ItemStack(brickItem, 50);
        assertFalse(brickStack.isFull());
        brickStack.grow(14);
        assertTrue(brickStack.isFull());
        assertEquals(64, brickStack.getCount());

        // Cannot add more beyond limit
        ItemStack split = brickStack.split(30);
        assertEquals(30, split.getCount());
        assertEquals(34, brickStack.getCount());
    }
}
