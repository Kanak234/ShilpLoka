package shilploka.kaushal;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Manages the sacred Shilp Mandap altar enhancement mechanics.
 * Blesses player tools and weapons with Vedic Kaushal masteries using mineral catalysts.
 *
 * WHAT:
 * Validates tool eligibility, consumes Rajavarta lapis lazuli / Bhasma catalysts,
 * and attaches KaushalUpgradeType to the output stack.
 *
 * USED FOR:
 * Consulted by KaushalScreen and invoked when player performs an altar upgrade.
 */
public class KaushalManager {

    private ItemStack toolSlot = ItemStack.EMPTY;
    private ItemStack catalystSlot = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;

    public ItemStack getToolSlot() {
        return toolSlot;
    }

    public void setToolSlot(ItemStack stack) {
        this.toolSlot = (stack != null) ? stack : ItemStack.EMPTY;
        refreshAvailableUpgrades();
    }

    public ItemStack getCatalystSlot() {
        return catalystSlot;
    }

    public void setCatalystSlot(ItemStack stack) {
        this.catalystSlot = (stack != null) ? stack : ItemStack.EMPTY;
        refreshAvailableUpgrades();
    }

    public ItemStack getOutputSlot() {
        return outputSlot;
    }

    public void setOutputSlot(ItemStack stack) {
        this.outputSlot = (stack != null) ? stack : ItemStack.EMPTY;
    }

    /**
     * Determines which Kaushal enhancements can be bestowed upon the active tool.
     */
    public List<KaushalUpgradeType> getAvailableUpgrades() {
        List<KaushalUpgradeType> list = new ArrayList<>();
        if (toolSlot.isEmpty() || !toolSlot.getItem().isTool()) {
            return list;
        }

        switch (toolSlot.getItem().getToolType()) {
            case TALWAR -> {
                list.add(KaushalUpgradeType.TEJ_DHAAR);
                list.add(KaushalUpgradeType.MAJBOOT_SHILP);
            }
            case KUDAAL -> {
                list.add(KaushalUpgradeType.TEZ_KHANAN);
                list.add(KaushalUpgradeType.MAJBOOT_SHILP);
            }
            case PHAAVDA -> {
                list.add(KaushalUpgradeType.KRISHI_VRIDDHI);
                list.add(KaushalUpgradeType.TEZ_KHANAN);
                list.add(KaushalUpgradeType.MAJBOOT_SHILP);
            }
            case KULHADI -> {
                list.add(KaushalUpgradeType.TEZ_KHANAN);
                list.add(KaushalUpgradeType.MAJBOOT_SHILP);
            }
        }
        list.add(KaushalUpgradeType.JAL_SHWAAS);
        return list;
    }

    private void refreshAvailableUpgrades() {
        // Clear output if inputs become invalid
        if (toolSlot.isEmpty()) {
            outputSlot = ItemStack.EMPTY;
        }
    }

    /**
     * Attempts to apply the chosen Kaushal upgrade to the tool.
     * Consumes catalyst items from catalystSlot and generates upgraded tool in outputSlot.
     *
     * @return true if upgrade succeeded; false if catalyst was insufficient or slots were invalid.
     */
    public boolean applyUpgrade(KaushalUpgradeType upgrade) {
        if (upgrade == null || toolSlot.isEmpty() || !toolSlot.getItem().isTool()) {
            return false;
        }

        int requiredCost = upgrade.getLapisCost();
        // Catalyst can be Rajavarta Ore, Koyla, Bhasma, or Mudras
        if (catalystSlot.isEmpty() || catalystSlot.getCount() < requiredCost) {
            return false;
        }

        // Deduct catalyst
        catalystSlot.shrink(requiredCost);
        if (catalystSlot.isEmpty()) {
            catalystSlot = ItemStack.EMPTY;
        }

        // Create upgraded tool
        ItemStack enhanced = toolSlot.copy();
        enhanced.setKaushal(upgrade);
        outputSlot = enhanced;

        // Clear input tool
        toolSlot = ItemStack.EMPTY;
        return true;
    }
}
