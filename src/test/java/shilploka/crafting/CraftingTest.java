package shilploka.crafting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates 2x2 personal crafting matrix mechanics, shaped and shapeless recipe matching,
 * output result generation, and ingredient consumption.
 *
 * WHAT:
 * Tests logs -> planks (shapeless), planks -> sticks (shaped), clay -> diya, and weapons.
 *
 * USED FOR:
 * Verifies Phase P7 crafting pipeline.
 */
public class CraftingTest {

    private CraftingGrid2x2 grid;

    @BeforeAll
    static void initRegistry() {
        ItemRegistry.init();
        RecipeManager.init();
    }

    @BeforeEach
    void setUp() {
        grid = new CraftingGrid2x2();
    }

    @Test
    @DisplayName("Shapeless craft: 1 Sal Log produces 4 Sal Wood Planks")
    void testSalLogToPlanks() {
        // Place 1 Sal Log in top-left slot (index 0)
        grid.setMatrixSlot(0, new ItemStack(ItemRegistry.getItem("shilploka:sal_log"), 1));

        ItemStack result = grid.getResultSlot();
        assertFalse(result.isEmpty());
        assertEquals("shilploka:sal_planks", result.getItem().getId());
        assertEquals(4, result.getCount());

        // Take output result
        ItemStack taken = grid.takeResult();
        assertEquals(4, taken.getCount());
        assertEquals("shilploka:sal_planks", taken.getItem().getId());

        // Log slot should now be empty
        assertTrue(grid.getMatrixSlot(0).isEmpty());
        assertTrue(grid.getResultSlot().isEmpty());
    }

    @Test
    @DisplayName("Shaped craft: 2 vertical Sal Planks produce 4 Lakdi Chhad (Sticks)")
    void testPlanksToSticks() {
        // Slot 0 (top-left) and Slot 2 (bottom-left)
        grid.setMatrixSlot(0, new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 2));
        grid.setMatrixSlot(2, new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 2));

        ItemStack result = grid.getResultSlot();
        assertFalse(result.isEmpty());
        assertEquals("shilploka:lakdi_chhad", result.getItem().getId());
        assertEquals(4, result.getCount());

        // Take 1 set of sticks
        ItemStack taken = grid.takeResult();
        assertEquals(4, taken.getCount());

        // Ingredients should be decremented by 1, leaving 1 plank each
        assertEquals(1, grid.getMatrixSlot(0).getCount());
        assertEquals(1, grid.getMatrixSlot(2).getCount());

        // Second set should still be available
        assertFalse(grid.getResultSlot().isEmpty());
    }

    @Test
    @DisplayName("Shapeless craft: Mitti Clay lump crafts Clay Diya")
    void testClayToDiya() {
        // Place Mitti clay in slot 3 (bottom-right)
        grid.setMatrixSlot(3, new ItemStack(ItemRegistry.MITTI_CLAY, 1));

        ItemStack result = grid.getResultSlot();
        assertFalse(result.isEmpty());
        assertEquals("shilploka:diya", result.getItem().getId());
        assertEquals(1, result.getCount());

        ItemStack taken = grid.takeResult();
        assertEquals("shilploka:diya", taken.getItem().getId());
        assertTrue(grid.getMatrixSlot(3).isEmpty());
    }

    @Test
    @DisplayName("Invalid arrangements return empty result")
    void testInvalidArrangement() {
        // Random incompatible mismatch
        grid.setMatrixSlot(0, new ItemStack(ItemRegistry.getItem("shilploka:thar_sand"), 1));
        grid.setMatrixSlot(1, new ItemStack(ItemRegistry.getItem("shilploka:hira"), 1));

        assertTrue(grid.getResultSlot().isEmpty());
    }

    @Test
    @DisplayName("Clearing crafting grid returns all uncrafted items")
    void testClearAndReturn() {
        grid.setMatrixSlot(0, new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 5));
        grid.setMatrixSlot(3, new ItemStack(ItemRegistry.MITTI_CLAY, 2));

        var returned = grid.clearAndReturnItems();
        assertEquals(2, returned.size());
        assertTrue(grid.getMatrixSlot(0).isEmpty());
        assertTrue(grid.getMatrixSlot(3).isEmpty());
        assertTrue(grid.getResultSlot().isEmpty());
    }
}
