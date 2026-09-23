package shilploka.crafting;

import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Provides the 3x3 (9 slots) crafting matrix for the Shilp Karyashala workbench.
 * Allows constructing complex multi-material recipes, advanced metallurgical tools,
 * and specialized workstations.
 *
 * WHAT:
 * Encapsulates a 9-element ItemStack array for the matrix and a 1-element result slot.
 * Automatically recalculates output upon ingredient modification using RecipeManager.
 *
 * USED FOR:
 * Bound to WorkstationScreen when player interacts with a Karyashala block.
 */
public class CraftingGrid3x3 {

    public static final int MATRIX_SIZE = 9; // 3x3 grid

    private final ItemStack[] matrix = new ItemStack[MATRIX_SIZE];
    private ItemStack result = ItemStack.EMPTY;

    public CraftingGrid3x3() {
        for (int i = 0; i < MATRIX_SIZE; i++) {
            matrix[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getMatrixSlot(int slot) {
        if (slot < 0 || slot >= MATRIX_SIZE) return ItemStack.EMPTY;
        return matrix[slot];
    }

    public void setMatrixSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < MATRIX_SIZE) {
            matrix[slot] = (stack != null) ? stack : ItemStack.EMPTY;
            updateResult();
        }
    }

    public ItemStack getResultSlot() {
        return result;
    }

    /**
     * Re-evaluates RecipeManager against the active 3x3 matrix.
     */
    public void updateResult() {
        Recipe match = RecipeManager.findMatchingRecipe(matrix);
        if (match != null) {
            this.result = match.getOutput();
        } else {
            this.result = ItemStack.EMPTY;
        }
    }

    /**
     * Decrements each ingredient in the matrix by 1 and returns the crafted output.
     */
    public ItemStack takeResult() {
        if (result.isEmpty()) return ItemStack.EMPTY;

        ItemStack output = result.copy();

        for (int i = 0; i < MATRIX_SIZE; i++) {
            ItemStack stack = matrix[i];
            if (!stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    matrix[i] = ItemStack.EMPTY;
                }
            }
        }

        updateResult();
        return output;
    }

    /**
     * Clears all matrix items and returns them (e.g. when player closes UI).
     */
    public List<ItemStack> clearAndReturnItems() {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < MATRIX_SIZE; i++) {
            if (!matrix[i].isEmpty()) {
                items.add(matrix[i]);
                matrix[i] = ItemStack.EMPTY;
            }
        }
        result = ItemStack.EMPTY;
        return items;
    }
}
