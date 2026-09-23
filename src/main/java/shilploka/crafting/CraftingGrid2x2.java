package shilploka.crafting;

import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Governs the 2x2 personal crafting matrix embedded in the player's inventory screen.
 *
 * WHAT:
 * Contains 4 input slots (0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right)
 * and 1 output result slot. Automatically evaluates recipes on changes and handles
 * ingredient consumption upon taking the crafted result.
 *
 * USED FOR:
 * Managed by PlayerInventory and rendered in InventoryScreen.
 *
 * WHERE NEXT:
 * Triggered on player mouse clicks in inventory GUI.
 */
public class CraftingGrid2x2 {

    private final ItemStack[] matrix = new ItemStack[4];
    private ItemStack result = ItemStack.EMPTY;

    public CraftingGrid2x2() {
        for (int i = 0; i < 4; i++) {
            matrix[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getMatrixSlot(int slot) {
        if (slot < 0 || slot >= 4) return ItemStack.EMPTY;
        return matrix[slot];
    }

    public void setMatrixSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < 4) {
            matrix[slot] = (stack != null) ? stack : ItemStack.EMPTY;
            recalculateResult();
        }
    }

    public ItemStack getResultSlot() {
        return result;
    }

    public void recalculateResult() {
        Recipe recipe = RecipeManager.findMatchingRecipe(matrix);
        if (recipe != null) {
            this.result = recipe.getOutput();
        } else {
            this.result = ItemStack.EMPTY;
        }
    }

    /**
     * WHY:
     * Consumes 1 item from each non-empty crafting ingredient slot when the player takes the output item.
     *
     * @return The crafted result stack.
     */
    public ItemStack takeResult() {
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack crafted = result.copy();

        // Shrink matrix ingredients
        for (int i = 0; i < 4; i++) {
            if (!matrix[i].isEmpty()) {
                matrix[i].shrink(1);
                if (matrix[i].isEmpty()) {
                    matrix[i] = ItemStack.EMPTY;
                }
            }
        }

        recalculateResult();
        return crafted;
    }

    /**
     * Clears all items from crafting grid and returns them to player inventory.
     */
    public List<ItemStack> clearAndReturnItems() {
        List<ItemStack> leftover = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (!matrix[i].isEmpty()) {
                leftover.add(matrix[i].copy());
                matrix[i] = ItemStack.EMPTY;
            }
        }
        recalculateResult();
        return leftover;
    }
}
