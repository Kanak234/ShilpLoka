package shilploka.workstation;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Simulates ancient Indian metallurgy in the Smelting Bhatti:
 * - Single ore smelting (Taamba/Copper, Loha/Iron, Sona/Gold).
 * - Ancient Indian bronze (Kansa) and brass (Peetal) alloy metallurgy.
 * - Legendary Wootz crucible steel synthesis (high carbon iron fusion).
 *
 * WHAT:
 * Dual-ingredient smelter with fuel consumption and smelting timers.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Smelting Bhatti block.
 */
public class AlloyManager {

    public static final int DEFAULT_SMELT_TICKS = 200; // 10 seconds per alloy ingot

    public record SmeltRecipe(String primaryId, String secondaryId, String resultId, int resultCount) {
        public boolean matches(String in1, String in2) {
            if (secondaryId == null) {
                return primaryId.equals(in1) && in2 == null;
            }
            return (primaryId.equals(in1) && secondaryId.equals(in2)) ||
                   (primaryId.equals(in2) && secondaryId.equals(in1));
        }
    }

    private static final List<SmeltRecipe> RECIPES = new ArrayList<>();

    static {
        // 1. Single Ore Smelting
        RECIPES.add(new SmeltRecipe("shilploka:taamba_ore", null, "shilploka:taamba_ingot", 1));
        RECIPES.add(new SmeltRecipe("shilploka:loha_ore", null, "shilploka:loha_ingot", 1));
        RECIPES.add(new SmeltRecipe("shilploka:sona_ore", null, "shilploka:sona_ingot", 1));

        // 2. Ancient Indian Alloys
        // Kansa (Bronze): Copper Ingot + Taamba Ore (flux) -> Kansa Ingot
        RECIPES.add(new SmeltRecipe("shilploka:taamba_ingot", "shilploka:taamba_ore", "shilploka:kansa_ingot", 2));

        // Peetal (Brass): Copper Ingot + Yellow Sandstone (calamine flux) -> Peetal Ingot
        RECIPES.add(new SmeltRecipe("shilploka:taamba_ingot", "shilploka:yellow_sandstone", "shilploka:peetal_ingot", 2));

        // Wootz Steel (Wootz Faulaad): Loha Ingot + Koyla Carbon -> Wootz Ingot
        RECIPES.add(new SmeltRecipe("shilploka:loha_ingot", "shilploka:koyla", "shilploka:wootz_ingot", 1));
    }

    private ItemStack primarySlot = ItemStack.EMPTY;
    private ItemStack secondarySlot = ItemStack.EMPTY;
    private ItemStack fuelSlot = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;

    private int burnTimeRemaining = 0;
    private int maxBurnTime = 0;
    private int smeltProgress = 0;

    public void tick() {
        boolean isBurning = burnTimeRemaining > 0;
        if (isBurning) {
            burnTimeRemaining--;
        }

        SmeltRecipe match = findMatchingRecipe();
        boolean canSmelt = match != null && canAcceptOutput(match);

        if (!isBurning && canSmelt) {
            if (KilnManager.isFuel(fuelSlot)) {
                int fuelVal = KilnManager.getFuelBurnTime(fuelSlot);
                this.burnTimeRemaining = fuelVal;
                this.maxBurnTime = fuelVal;
                fuelSlot.shrink(1);
                if (fuelSlot.isEmpty()) {
                    fuelSlot = ItemStack.EMPTY;
                }
                isBurning = true;
            }
        }

        if (isBurning && canSmelt) {
            smeltProgress++;
            if (smeltProgress >= DEFAULT_SMELT_TICKS) {
                smeltProgress = 0;
                produceAlloy(match);
            }
        } else if (!canSmelt) {
            smeltProgress = 0;
        }
    }

    private SmeltRecipe findMatchingRecipe() {
        String in1 = primarySlot.isEmpty() ? null : primarySlot.getItem().getId();
        String in2 = secondarySlot.isEmpty() ? null : secondarySlot.getItem().getId();

        if (in1 == null && in2 == null) return null;

        for (SmeltRecipe recipe : RECIPES) {
            if (recipe.matches(in1, in2)) {
                return recipe;
            }
        }
        return null;
    }

    private boolean canAcceptOutput(SmeltRecipe recipe) {
        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.getItem().getId().equals(recipe.resultId())) return false;
        return outputSlot.getCount() + recipe.resultCount() <= outputSlot.getItem().getMaxStackSize();
    }

    private void produceAlloy(SmeltRecipe recipe) {
        // Consume primary
        if (!primarySlot.isEmpty()) {
            primarySlot.shrink(1);
            if (primarySlot.isEmpty()) primarySlot = ItemStack.EMPTY;
        }

        // Consume secondary if recipe required one
        if (recipe.secondaryId() != null && !secondarySlot.isEmpty()) {
            secondarySlot.shrink(1);
            if (secondarySlot.isEmpty()) secondarySlot = ItemStack.EMPTY;
        }

        if (outputSlot.isEmpty()) {
            outputSlot = new ItemStack(ItemRegistry.getItem(recipe.resultId()), recipe.resultCount());
        } else {
            outputSlot.grow(recipe.resultCount());
        }
    }

    public ItemStack getPrimarySlot() { return primarySlot; }
    public void setPrimarySlot(ItemStack primarySlot) { this.primarySlot = (primarySlot != null) ? primarySlot : ItemStack.EMPTY; }

    public ItemStack getSecondarySlot() { return secondarySlot; }
    public void setSecondarySlot(ItemStack secondarySlot) { this.secondarySlot = (secondarySlot != null) ? secondarySlot : ItemStack.EMPTY; }

    public ItemStack getFuelSlot() { return fuelSlot; }
    public void setFuelSlot(ItemStack fuelSlot) { this.fuelSlot = (fuelSlot != null) ? fuelSlot : ItemStack.EMPTY; }

    public ItemStack getOutputSlot() { return outputSlot; }
    public void setOutputSlot(ItemStack outputSlot) { this.outputSlot = (outputSlot != null) ? outputSlot : ItemStack.EMPTY; }

    public int getBurnTimeRemaining() { return burnTimeRemaining; }
    public int getMaxBurnTime() { return maxBurnTime; }
    public int getSmeltProgress() { return smeltProgress; }
    public boolean isBurning() { return burnTimeRemaining > 0; }
}
