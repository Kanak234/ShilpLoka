package shilploka.crafting;

import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WHY:
 * Encapsulates crafting transformation rules for shaping raw resources into tools,
 * building blocks, and items across both 2x2 personal crafting and 3x3 Karyashala workbenches.
 *
 * WHAT:
 * Supports sized shaped patterns (up to 3x3) with offset matching and shapeless crafting recipes.
 *
 * USED FOR:
 * Evaluated by CraftingGrid2x2 and CraftingGrid3x3 to determine output ItemStack.
 *
 * WHERE NEXT:
 * Registered in RecipeManager.
 */
public class Recipe {

    private final String id;
    private final ItemStack output;
    private final String[][] shapedPattern; // row x col pattern, or null if shapeless
    private final int patternWidth;
    private final int patternHeight;
    private final List<String> shapelessIngredients; // or null if shaped

    private Recipe(String id, ItemStack output, String[][] shapedPattern, int width, int height, List<String> shapelessIngredients) {
        this.id = id;
        this.output = output;
        this.shapedPattern = shapedPattern;
        this.patternWidth = width;
        this.patternHeight = height;
        this.shapelessIngredients = shapelessIngredients;
    }

    public static Recipe createShaped(String id, ItemStack output, String row0col0, String row0col1,
                                      String row1col0, String row1col1) {
        String[][] pattern = new String[][]{
                { row0col0, row0col1 },
                { row1col0, row1col1 }
        };
        return new Recipe(id, output, pattern, 2, 2, null);
    }

    public static Recipe createShaped3x3(String id, ItemStack output,
                                         String r0c0, String r0c1, String r0c2,
                                         String r1c0, String r1c1, String r1c2,
                                         String r2c0, String r2c1, String r2c2) {
        String[][] pattern = new String[][]{
                { r0c0, r0c1, r0c2 },
                { r1c0, r1c1, r1c2 },
                { r2c0, r2c1, r2c2 }
        };
        return new Recipe(id, output, pattern, 3, 3, null);
    }

    public static Recipe createShapeless(String id, ItemStack output, String... ingredientIds) {
        return new Recipe(id, output, null, 0, 0, Arrays.asList(ingredientIds));
    }

    public boolean matches(ItemStack[] grid) {
        if (grid == null) return false;
        int gridDimension = (int) Math.round(Math.sqrt(grid.length));
        if (gridDimension * gridDimension != grid.length) return false;

        if (shapedPattern != null) {
            return matchesShaped(grid, gridDimension);
        } else if (shapelessIngredients != null) {
            return matchesShapeless(grid);
        }
        return false;
    }

    private boolean matchesShaped(ItemStack[] grid, int gridDim) {
        if (patternWidth > gridDim || patternHeight > gridDim) {
            return false;
        }

        int maxOffsetX = gridDim - patternWidth;
        int maxOffsetY = gridDim - patternHeight;

        for (int offY = 0; offY <= maxOffsetY; offY++) {
            for (int offX = 0; offX <= maxOffsetX; offX++) {
                if (checkPatternAtOffset(grid, gridDim, offX, offY)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkPatternAtOffset(ItemStack[] grid, int gridDim, int offX, int offY) {
        for (int gy = 0; gy < gridDim; gy++) {
            for (int gx = 0; gx < gridDim; gx++) {
                int slot = gy * gridDim + gx;
                ItemStack actualStack = grid[slot];

                // Check if this grid coordinate falls within the pattern bounds
                if (gx >= offX && gx < offX + patternWidth && gy >= offY && gy < offY + patternHeight) {
                    int px = gx - offX;
                    int py = gy - offY;
                    String expectedId = shapedPattern[py][px];

                    if (expectedId == null || expectedId.isEmpty()) {
                        if (actualStack != null && !actualStack.isEmpty()) {
                            return false;
                        }
                    } else {
                        if (actualStack == null || actualStack.isEmpty()) {
                            return false;
                        }
                        if (!expectedId.equals(actualStack.getItem().getId())) {
                            return false;
                        }
                    }
                } else {
                    // Any slot outside the pattern must be empty
                    if (actualStack != null && !actualStack.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean matchesShapeless(ItemStack[] grid) {
        List<String> required = new ArrayList<>(shapelessIngredients);

        for (ItemStack stack : grid) {
            if (stack != null && !stack.isEmpty()) {
                String id = stack.getItem().getId();
                if (!required.remove(id)) {
                    return false; // Extra unneeded item present in grid
                }
            }
        }
        return required.isEmpty();
    }

    public String getId() { return id; }
    public ItemStack getOutput() { return output.copy(); }
}
