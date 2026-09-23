package shilploka.workstation;

import shilploka.item.ItemStack;
import shilploka.item.ToolTier;

/**
 * WHY:
 * Simulates tool repairing and reinforcement at the Lohar Nihai (forged anvil).
 * Ancient Indian metallurgy allowed re-tempering damaged Wootz steel, iron, and bronze blades
 * using virgin ingots.
 *
 * WHAT:
 * Combines damaged tool + matching repair ingot to restore 50% durability plus a 10% bonus.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Nihai block.
 */
public class NihaiManager {

    private ItemStack toolSlot = ItemStack.EMPTY;
    private ItemStack materialSlot = ItemStack.EMPTY;
    private ItemStack resultSlot = ItemStack.EMPTY;

    public boolean canRepair() {
        if (toolSlot.isEmpty() || !toolSlot.getItem().isDamageable() || toolSlot.getDamage() == 0) {
            return false;
        }
        if (materialSlot.isEmpty()) {
            return false;
        }

        ToolTier tier = toolSlot.getItem().getToolTier();
        if (tier == null) return false;

        String requiredMaterial = switch (tier) {
            case WOOD -> "shilploka:sal_planks";
            case STONE -> "shilploka:granite";
            case COPPER -> "shilploka:taamba_ingot";
            case BRONZE -> "shilploka:kansa_ingot";
            case IRON -> "shilploka:loha_ingot";
            case WOOTZ -> "shilploka:wootz_ingot";
            case VAJRA -> "shilploka:hira";
        };

        return materialSlot.getItem().getId().equals(requiredMaterial);
    }

    public void updateRepairPreview() {
        if (!canRepair()) {
            resultSlot = ItemStack.EMPTY;
            return;
        }

        ItemStack repaired = toolSlot.copy();
        int maxDur = toolSlot.getItem().getMaxDurability();
        // Restore 50% of max durability + 10% bonus
        int repairAmount = (int) (maxDur * 0.60);
        int newDamage = Math.max(0, repaired.getDamage() - repairAmount);
        repaired.setDamage(newDamage);

        this.resultSlot = repaired;
    }

    public ItemStack takeRepairedTool() {
        if (resultSlot.isEmpty()) return ItemStack.EMPTY;

        ItemStack output = resultSlot.copy();

        toolSlot = ItemStack.EMPTY;
        materialSlot.shrink(1);
        if (materialSlot.isEmpty()) materialSlot = ItemStack.EMPTY;

        resultSlot = ItemStack.EMPTY;
        return output;
    }

    public ItemStack getToolSlot() { return toolSlot; }
    public void setToolSlot(ItemStack toolSlot) {
        this.toolSlot = (toolSlot != null) ? toolSlot : ItemStack.EMPTY;
        updateRepairPreview();
    }

    public ItemStack getMaterialSlot() { return materialSlot; }
    public void setMaterialSlot(ItemStack materialSlot) {
        this.materialSlot = (materialSlot != null) ? materialSlot : ItemStack.EMPTY;
        updateRepairPreview();
    }

    public ItemStack getResultSlot() { return resultSlot; }
}
