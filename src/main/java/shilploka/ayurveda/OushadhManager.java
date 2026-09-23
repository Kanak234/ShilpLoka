package shilploka.ayurveda;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

/**
 * WHY:
 * Encapsulates ancient Ayurvedic pharmaceutical preparation (Oushadh Vigyan).
 * Allows players to grind botanicals and brew healing kadhas, restorative tonics,
 * and protective lepa salves using traditional pottery vessels and mortars.
 *
 * WHAT:
 * Evaluates herbal recipes and brews authentic Ayurvedic medicines.
 *
 * USED FOR:
 * Consulted by Vaidya villager trade exchanges and player workstation brewing.
 */
public final class OushadhManager {

    private OushadhManager() {}

    /**
     * Attempts to brew an Ayurvedic remedy from herbal inputs and a vessel.
     */
    public static ItemStack brew(ItemStack ingredientA, ItemStack ingredientB) {
        if (ingredientA == null || ingredientB == null || ingredientA.isEmpty() || ingredientB.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var itemA = ingredientA.getItem();
        var itemB = ingredientB.getItem();

        // 1. Haldi Doodh (Turmeric + Milk)
        if ((itemA == ItemRegistry.DOODH && itemB == ItemRegistry.HALDI_POWDER) ||
            (itemA == ItemRegistry.HALDI_POWDER && itemB == ItemRegistry.DOODH)) {
            return new ItemStack(ItemRegistry.HALDI_DOODH, 1);
        }

        // 2. Tulsi Kadha (Haldi / Herbs + Earthen Kulhad)
        if ((itemA == ItemRegistry.HALDI_POWDER && itemB == ItemRegistry.KULHAD) ||
            (itemA == ItemRegistry.KULHAD && itemB == ItemRegistry.HALDI_POWDER)) {
            return new ItemStack(ItemRegistry.TULSI_KADHA, 1);
        }

        // 3. Neem Lep (Clay / Soil + Herbs)
        if ((itemA == ItemRegistry.MITTI_CLAY && itemB == ItemRegistry.HALDI_POWDER) ||
            (itemA == ItemRegistry.HALDI_POWDER && itemB == ItemRegistry.MITTI_CLAY)) {
            return new ItemStack(ItemRegistry.NEEM_LEP, 2);
        }

        // 4. Shilajit Rasayana (Sacred Bhasma + Milk)
        if ((itemA == ItemRegistry.BHASMA && itemB == ItemRegistry.DOODH) ||
            (itemA == ItemRegistry.DOODH && itemB == ItemRegistry.BHASMA)) {
            return new ItemStack(ItemRegistry.SHILAJIT_RASAYANA, 1);
        }

        return ItemStack.EMPTY;
    }
}
