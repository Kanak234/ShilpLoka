package shilploka.workstation;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * WHY:
 * Simulates high-temperature thermal baking in the Pakki Bhatti (Kiln) for ceramics,
 * terracotta pottery, Harappan fired bricks, and architectural silica glass.
 *
 * WHAT:
 * Encapsulates thermal cook timers, fuel burn times, and recipe definitions.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Pakki Bhatti block.
 */
public class KilnManager {

    public static final int DEFAULT_COOK_TICKS = 160; // 8 seconds per fired item

    private static final Map<String, String> RECIPES = new HashMap<>();
    private static final Map<String, Integer> FUELS = new HashMap<>();

    static {
        // 1. Ceramic & Glass Firing Recipes
        RECIPES.put("shilploka:mitti_clay", "shilploka:fired_brick");
        RECIPES.put("shilploka:alluvial_soil", "shilploka:fired_brick");
        RECIPES.put("shilploka:thar_sand", "shilploka:kaanch");

        // 2. Fuel Burn Times (in ticks at 20 ticks/sec)
        FUELS.put("shilploka:koyla", 1600);        // 80s (burns 10 items)
        FUELS.put("shilploka:sal_log", 300);       // 15s
        FUELS.put("shilploka:deodar_log", 300);    // 15s
        FUELS.put("shilploka:khejri_log", 300);    // 15s
        FUELS.put("shilploka:sal_planks", 150);    // 7.5s
        FUELS.put("shilploka:lakdi_chhad", 50);    // 2.5s
    }

    private ItemStack inputSlot = ItemStack.EMPTY;
    private ItemStack fuelSlot = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;

    private int burnTimeRemaining = 0;
    private int maxBurnTime = 0;
    private int cookProgress = 0;

    public static boolean isFuel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return FUELS.containsKey(stack.getItem().getId());
    }

    public static int getFuelBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return FUELS.getOrDefault(stack.getItem().getId(), 0);
    }

    public static String getOutput(String inputItemId) {
        return RECIPES.get(inputItemId);
    }

    /**
     * Ticks the kiln thermal cycle (advances cook progress, consumes fuel).
     */
    public void tick() {
        boolean isBurning = burnTimeRemaining > 0;

        if (isBurning) {
            burnTimeRemaining--;
        }

        boolean canCook = canCookCurrentItem();

        if (!isBurning && canCook) {
            // Attempt to consume 1 fuel item
            if (isFuel(fuelSlot)) {
                int fuelValue = getFuelBurnTime(fuelSlot);
                this.burnTimeRemaining = fuelValue;
                this.maxBurnTime = fuelValue;
                fuelSlot.shrink(1);
                if (fuelSlot.isEmpty()) {
                    fuelSlot = ItemStack.EMPTY;
                }
                isBurning = true;
            }
        }

        if (isBurning && canCook) {
            cookProgress++;
            if (cookProgress >= DEFAULT_COOK_TICKS) {
                cookProgress = 0;
                produceItem();
            }
        } else if (!canCook) {
            cookProgress = 0;
        }
    }

    private boolean canCookCurrentItem() {
        if (inputSlot.isEmpty()) return false;
        String outputId = RECIPES.get(inputSlot.getItem().getId());
        if (outputId == null) return false;

        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.getItem().getId().equals(outputId)) return false;
        return !outputSlot.isFull();
    }

    private void produceItem() {
        String outputId = RECIPES.get(inputSlot.getItem().getId());
        if (outputId == null) return;

        inputSlot.shrink(1);
        if (inputSlot.isEmpty()) {
            inputSlot = ItemStack.EMPTY;
        }

        if (outputSlot.isEmpty()) {
            outputSlot = new ItemStack(ItemRegistry.getItem(outputId), 1);
        } else {
            outputSlot.grow(1);
        }
    }

    public ItemStack getInputSlot() { return inputSlot; }
    public void setInputSlot(ItemStack inputSlot) { this.inputSlot = (inputSlot != null) ? inputSlot : ItemStack.EMPTY; }

    public ItemStack getFuelSlot() { return fuelSlot; }
    public void setFuelSlot(ItemStack fuelSlot) { this.fuelSlot = (fuelSlot != null) ? fuelSlot : ItemStack.EMPTY; }

    public ItemStack getOutputSlot() { return outputSlot; }
    public void setOutputSlot(ItemStack outputSlot) { this.outputSlot = (outputSlot != null) ? outputSlot : ItemStack.EMPTY; }

    public int getBurnTimeRemaining() { return burnTimeRemaining; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getCookProgress() { return cookProgress; }
    public boolean isBurning() { return burnTimeRemaining > 0; }
}
