package shilploka.entity;

import shilploka.crafting.CraftingGrid2x2;
import shilploka.item.ItemStack;

/**
 * WHY:
 * Manages player item storage across 36 slots (9 hotbar + 27 backpack) and the personal
 * 2x2 crafting grid.
 *
 * WHAT:
 * Encapsulates slot indexing, automatic item merging, stack size limits, and inventory querying.
 *
 * USED FOR:
 * Carried by Player instance, queried by Hotbar, and manipulated in InventoryScreen.
 *
 * WHERE NEXT:
 * Serialized into playerdata/player.json by SaveManager.
 */
public class PlayerInventory {

    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_SIZE = 27;
    public static final int TOTAL_SLOTS = HOTBAR_SIZE + MAIN_SIZE; // 36 slots

    private final ItemStack[] slots = new ItemStack[TOTAL_SLOTS];
    private final CraftingGrid2x2 craftingGrid = new CraftingGrid2x2();

    public PlayerInventory() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }

    public ItemStack getSlot(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) return ItemStack.EMPTY;
        return slots[slot];
    }

    public void setSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SLOTS) {
            slots[slot] = (stack != null) ? stack : ItemStack.EMPTY;
        }
    }

    /**
     * Attempts to insert stack into inventory. Returns remaining items that could not fit.
     */
    public ItemStack addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack remaining = stack.copy();

        // 1. Try to merge into existing compatible stacks
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            ItemStack slot = slots[i];
            if (!slot.isEmpty() && slot.isStackableWith(remaining) && !slot.isFull()) {
                int space = slot.getItem().getMaxStackSize() - slot.getCount();
                int toAdd = Math.min(space, remaining.getCount());
                slot.grow(toAdd);
                remaining.shrink(toAdd);

                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        // 2. Insert into first available empty slots
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (slots[i].isEmpty()) {
                int toAdd = Math.min(remaining.getItem().getMaxStackSize(), remaining.getCount());
                slots[i] = new ItemStack(remaining.getItem(), toAdd, remaining.getDamage());
                remaining.shrink(toAdd);

                if (remaining.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
        }

        return remaining;
    }

    public CraftingGrid2x2 getCraftingGrid() {
        return craftingGrid;
    }

    public int getSlotCount() {
        return TOTAL_SLOTS;
    }

    public void clear() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }
}
