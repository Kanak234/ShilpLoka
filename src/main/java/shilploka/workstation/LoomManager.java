package shilploka.workstation;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

/**
 * WHY:
 * Simulates indigenous Indian textile production on the Kargah (Handloom).
 * Raw cotton (Kapaas) is spun into yarn (Soot), and yarn is woven into Khadi cloth (Vastra).
 *
 * WHAT:
 * Workstation model holding fiber input and producing woven fabrics.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Kargah block.
 */
public class LoomManager {

    private ItemStack fiberSlot = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;

    public boolean canSpinYarn() {
        if (fiberSlot.isEmpty() || !fiberSlot.getItem().getId().equals("shilploka:kapaas")) {
            return false;
        }
        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.getItem().getId().equals("shilploka:soot")) return false;
        return outputSlot.getCount() + 2 <= outputSlot.getItem().getMaxStackSize();
    }

    public boolean spinYarn() {
        if (!canSpinYarn()) return false;

        fiberSlot.shrink(1);
        if (fiberSlot.isEmpty()) fiberSlot = ItemStack.EMPTY;

        if (outputSlot.isEmpty()) {
            outputSlot = new ItemStack(ItemRegistry.SOOT, 2);
        } else {
            outputSlot.grow(2);
        }
        return true;
    }

    public boolean canWeaveCloth() {
        if (fiberSlot.isEmpty() || !fiberSlot.getItem().getId().equals("shilploka:soot") || fiberSlot.getCount() < 4) {
            return false;
        }
        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.getItem().getId().equals("shilploka:khadi_vastra")) return false;
        return outputSlot.getCount() + 1 <= outputSlot.getItem().getMaxStackSize();
    }

    public boolean weaveCloth() {
        if (!canWeaveCloth()) return false;

        fiberSlot.shrink(4);
        if (fiberSlot.isEmpty()) fiberSlot = ItemStack.EMPTY;

        if (outputSlot.isEmpty()) {
            outputSlot = new ItemStack(ItemRegistry.KHADI_VASTRA, 1);
        } else {
            outputSlot.grow(1);
        }
        return true;
    }

    public ItemStack getFiberSlot() { return fiberSlot; }
    public void setFiberSlot(ItemStack fiberSlot) { this.fiberSlot = (fiberSlot != null) ? fiberSlot : ItemStack.EMPTY; }

    public ItemStack getOutputSlot() { return outputSlot; }
    public void setOutputSlot(ItemStack outputSlot) { this.outputSlot = (outputSlot != null) ? outputSlot : ItemStack.EMPTY; }
}
