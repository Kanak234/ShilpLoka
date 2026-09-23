package shilploka.workstation;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * WHY:
 * Simulates the traditional Indian rotary stone quern mill (Atta Chakki).
 * Grinds whole grains into flour (Chawal -> Atta) and whole herbs into fine powders (Haldi -> Haldi Powder).
 *
 * WHAT:
 * Workstation model that grinds 1 input item on each manual rotation pass.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Chakki block.
 */
public class ChakkiManager {

    private static final Map<String, String> GRIND_RECIPES = new HashMap<>();

    static {
        GRIND_RECIPES.put("shilploka:chawal", "shilploka:atta");
        GRIND_RECIPES.put("shilploka:haldi_rhizome", "shilploka:haldi_powder");
    }

    private ItemStack grainSlot = ItemStack.EMPTY;
    private ItemStack flourSlot = ItemStack.EMPTY;

    public boolean canGrind() {
        if (grainSlot.isEmpty()) return false;
        String outputId = GRIND_RECIPES.get(grainSlot.getItem().getId());
        if (outputId == null) {
            // Default grain grinding: chawal produces atta
            if (grainSlot.getItem().getId().equals("shilploka:chawal")) {
                outputId = "shilploka:atta";
            } else {
                return false;
            }
        }

        if (flourSlot.isEmpty()) return true;
        if (!flourSlot.getItem().getId().equals(outputId)) return false;
        return !flourSlot.isFull();
    }

    public boolean grind() {
        if (!canGrind()) return false;

        String outputId = GRIND_RECIPES.getOrDefault(grainSlot.getItem().getId(), "shilploka:atta");

        grainSlot.shrink(1);
        if (grainSlot.isEmpty()) grainSlot = ItemStack.EMPTY;

        if (flourSlot.isEmpty()) {
            flourSlot = new ItemStack(ItemRegistry.getItem(outputId), 1);
        } else {
            flourSlot.grow(1);
        }
        return true;
    }

    public ItemStack getGrainSlot() { return grainSlot; }
    public void setGrainSlot(ItemStack grainSlot) { this.grainSlot = (grainSlot != null) ? grainSlot : ItemStack.EMPTY; }

    public ItemStack getFlourSlot() { return flourSlot; }
    public void setFlourSlot(ItemStack flourSlot) { this.flourSlot = (flourSlot != null) ? flourSlot : ItemStack.EMPTY; }
}
