package shilploka.worldgen.structure;

import shilploka.item.Item;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WHY:
 * Provides era-accurate historical loot generation for ancient relic chests (प्राचीन मंजूषा)
 * discovered within historical monuments and ruins.
 *
 * WHAT:
 * Dispenses Harappan seals, Maurya edicts, copper plate inscriptions (Tamrapatra),
 * palm leaf manuscripts (Talpandulipi), ancient Mudra currencies, and crucible Wootz steel.
 *
 * USED FOR:
 * Called when an ANCIENT_CHEST block is placed or opened by the player.
 */
public final class HistoricalLoot {

    private HistoricalLoot() {
    }

    /**
     * Generates a collection of authentic artifacts tailored to the era of the structure.
     */
    public static List<ItemStack> generateLoot(StructureType type, Random rand) {
        List<ItemStack> loot = new ArrayList<>();

        switch (type) {
            case HARAPPAN_BATH -> {
                // Harappan Pashupati seals, copper mudra, terracotta pottery, fired bricks
                loot.add(new ItemStack(ItemRegistry.HARAPPAN_SEAL, 1 + rand.nextInt(2)));
                loot.add(new ItemStack(ItemRegistry.MUDRA_COPPER, 4 + rand.nextInt(8)));
                loot.add(new ItemStack(ItemRegistry.KULHAD, 2 + rand.nextInt(4)));
                if (rand.nextBoolean()) {
                    loot.add(new ItemStack(ItemRegistry.TAAMBA_INGOT, 1 + rand.nextInt(3)));
                }
            }
            case BUDDHIST_STUPA -> {
                // Maurya edict inscriptions, palm leaf manuscripts, silver mudra, lapis lazuli
                loot.add(new ItemStack(ItemRegistry.MAURYA_EDICT, 1));
                loot.add(new ItemStack(ItemRegistry.TALPANDULIPI, 1 + rand.nextInt(2)));
                loot.add(new ItemStack(ItemRegistry.MUDRA_SILVER, 3 + rand.nextInt(6)));
                loot.add(new ItemStack(ItemRegistry.BHASMA, 2 + rand.nextInt(5)));
            }
            case CHOLA_STEPWELL -> {
                // Ancient copper plate grant (Tamrapatra), cowrie shells (kauri), bronze ingots
                loot.add(new ItemStack(ItemRegistry.TAMRAPATRA, 1));
                loot.add(new ItemStack(ItemRegistry.KAURI, 8 + rand.nextInt(16)));
                loot.add(new ItemStack(ItemRegistry.KANSA_INGOT, 2 + rand.nextInt(4)));
                loot.add(new ItemStack(ItemRegistry.SURAHI, 1 + rand.nextInt(2)));
            }
            case HILL_FORT -> {
                // Wootz crucible steel ingots, Wootz weapons, gold/silver mudra, iron ingots
                loot.add(new ItemStack(ItemRegistry.WOOTZ_INGOT, 1 + rand.nextInt(3)));
                loot.add(new ItemStack(ItemRegistry.MUDRA_GOLD, 1 + rand.nextInt(3)));
                loot.add(new ItemStack(ItemRegistry.MUDRA_SILVER, 4 + rand.nextInt(6)));
                loot.add(new ItemStack(ItemRegistry.LOHA_INGOT, 3 + rand.nextInt(5)));
                if (rand.nextInt(3) == 0) {
                    loot.add(new ItemStack(ItemRegistry.WOOTZ_TALWAR, 1));
                }
            }
            case ASHOKA_STHAMBHA -> {
                // Edict inscriptions, silver mudra, sacred bhasma
                loot.add(new ItemStack(ItemRegistry.MAURYA_EDICT, 1));
                loot.add(new ItemStack(ItemRegistry.MUDRA_SILVER, 2 + rand.nextInt(4)));
                loot.add(new ItemStack(ItemRegistry.MOR_PANKH, 1 + rand.nextInt(3)));
            }
        }

        return loot;
    }
}
