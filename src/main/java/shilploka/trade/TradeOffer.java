package shilploka.trade;

import shilploka.item.ItemStack;

/**
 * WHY:
 * Encapsulates an authentic barter trade agreement between the player and an Indian villager (Gramwasi).
 * Supports dual input costs (e.g. Copper Mudras + raw grain -> finished goods) and uses tracking.
 *
 * WHAT:
 * Validates inventory inputs, decrements given currency/goods, and dispenses outputs.
 *
 * USED FOR:
 * Referenced by GramwasiEntity and BarterScreen.
 */
public class TradeOffer {

    private final ItemStack costA;
    private final ItemStack costB;
    private final ItemStack result;
    private int uses;
    private final int maxUses;

    public TradeOffer(ItemStack costA, ItemStack costB, ItemStack result, int maxUses) {
        this.costA = (costA != null) ? costA.copy() : ItemStack.EMPTY;
        this.costB = (costB != null) ? costB.copy() : ItemStack.EMPTY;
        this.result = (result != null) ? result.copy() : ItemStack.EMPTY;
        this.uses = 0;
        this.maxUses = maxUses;
    }

    public TradeOffer(ItemStack costA, ItemStack result, int maxUses) {
        this(costA, ItemStack.EMPTY, result, maxUses);
    }

    public ItemStack getCostA() {
        return costA;
    }

    public ItemStack getCostB() {
        return costB;
    }

    public ItemStack getResult() {
        return result;
    }

    public int getUses() {
        return uses;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public boolean isOutOfStock() {
        return uses >= maxUses;
    }

    public void restock() {
        this.uses = 0;
    }

    /**
     * Checks whether the provided input items satisfy the trade requirements.
     */
    public boolean matches(ItemStack inputA, ItemStack inputB) {
        if (isOutOfStock()) return false;

        boolean matchesA = satisfiesSlot(costA, inputA);
        boolean matchesB = satisfiesSlot(costB, inputB);

        return matchesA && matchesB;
    }

    private boolean satisfiesSlot(ItemStack required, ItemStack given) {
        if (required.isEmpty()) {
            return true; // No requirement for this slot
        }
        if (given == null || given.isEmpty()) {
            return false;
        }
        return given.getItem() == required.getItem() && given.getCount() >= required.getCount();
    }

    /**
     * Executes the trade if the provided input items are sufficient.
     * Decrements the input item counts and increments the trade use counter.
     *
     * @return The resulting output ItemStack, or ItemStack.EMPTY if unsatisfied.
     */
    public ItemStack execute(ItemStack inputA, ItemStack inputB) {
        if (!matches(inputA, inputB)) {
            return ItemStack.EMPTY;
        }

        if (!costA.isEmpty() && inputA != null) {
            inputA.shrink(costA.getCount());
        }
        if (!costB.isEmpty() && inputB != null) {
            inputB.shrink(costB.getCount());
        }

        this.uses++;
        return result.copy();
    }
}
