package shilploka.crafting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WHY:
 * Maintains the registry of all valid crafting recipes in ShilpLoka.
 *
 * WHAT:
 * Compiles shaped 2x2 recipes and shapeless recipes using authentic Indian resources.
 * Provides findMatchingRecipe(ItemStack[] grid).
 *
 * USED FOR:
 * Consulted by CraftingGrid2x2 when player modifies crafting matrix slots.
 *
 * WHERE NEXT:
 * Displayed in InventoryScreen.
 */
public final class RecipeManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeManager.class);
    private static final List<Recipe> RECIPES = new ArrayList<>();

    static {
        // 1. Planks from Logs (Shapeless)
        register(Recipe.createShapeless("sal_planks",
                new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 4),
                "shilploka:sal_log"));
        register(Recipe.createShapeless("deodar_planks",
                new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 4),
                "shilploka:deodar_log"));
        register(Recipe.createShapeless("khejri_planks",
                new ItemStack(ItemRegistry.getItem("shilploka:sal_planks"), 4),
                "shilploka:khejri_log"));

        // 2. Sticks (Lakdi Chhad) from Planks (Shaped: 2 vertical planks)
        register(Recipe.createShaped("sticks_col0",
                new ItemStack(ItemRegistry.LAKDI_CHHAD, 4),
                "shilploka:sal_planks", null,
                "shilploka:sal_planks", null));
        register(Recipe.createShaped("sticks_col1",
                new ItemStack(ItemRegistry.LAKDI_CHHAD, 4),
                null, "shilploka:sal_planks",
                null, "shilploka:sal_planks"));

        // 3. Clay Diya from Clay Lump
        register(Recipe.createShapeless("diya_from_clay",
                new ItemStack(ItemRegistry.getItem("shilploka:diya"), 1),
                "shilploka:mitti_clay"));

        // 4. Weapons & Tools in 2x2 Grid
        // Wood Talwar (Plank over Stick)
        register(Recipe.createShaped("wood_talwar_c0",
                new ItemStack(ItemRegistry.WOOD_TALWAR, 1),
                "shilploka:sal_planks", null,
                "shilploka:lakdi_chhad", null));
        register(Recipe.createShaped("wood_talwar_c1",
                new ItemStack(ItemRegistry.WOOD_TALWAR, 1),
                null, "shilploka:sal_planks",
                null, "shilploka:lakdi_chhad"));

        // Stone Talwar (Granite over Stick)
        register(Recipe.createShaped("stone_talwar_c0",
                new ItemStack(ItemRegistry.STONE_TALWAR, 1),
                "shilploka:granite", null,
                "shilploka:lakdi_chhad", null));
        register(Recipe.createShaped("stone_talwar_c1",
                new ItemStack(ItemRegistry.STONE_TALWAR, 1),
                null, "shilploka:granite",
                null, "shilploka:lakdi_chhad"));

        // Wood Kudaal (2 Planks top, 1 Stick bottom-left)
        register(Recipe.createShaped("wood_kudaal",
                new ItemStack(ItemRegistry.WOOD_KUDAAL, 1),
                "shilploka:sal_planks", "shilploka:sal_planks",
                "shilploka:lakdi_chhad", null));

        // Stone Kudaal (2 Granite top, 1 Stick bottom-left)
        register(Recipe.createShaped("stone_kudaal",
                new ItemStack(ItemRegistry.STONE_KUDAAL, 1),
                "shilploka:granite", "shilploka:granite",
                "shilploka:lakdi_chhad", null));

        // 5. Basic Food Crafting
        register(Recipe.createShapeless("chawal_to_roti",
                new ItemStack(ItemRegistry.ROTI, 2),
                "shilploka:chawal", "shilploka:chawal"));

        // 6. Workstation Construction Recipes (3x3 & 2x2)
        // Karyashala Workbench: 4 Planks in 2x2
        register(Recipe.createShaped("karyashala_workbench",
                new ItemStack(ItemRegistry.getItem("shilploka:karyashala"), 1),
                "shilploka:sal_planks", "shilploka:sal_planks",
                "shilploka:sal_planks", "shilploka:sal_planks"));

        // Pakki Bhatti (Kiln): 8 Harappan Fired Bricks around perimeter
        register(Recipe.createShaped3x3("pakki_bhatti_kiln",
                new ItemStack(ItemRegistry.getItem("shilploka:pakki_bhatti"), 1),
                "shilploka:fired_brick", "shilploka:fired_brick", "shilploka:fired_brick",
                "shilploka:fired_brick", null, "shilploka:fired_brick",
                "shilploka:fired_brick", "shilploka:fired_brick", "shilploka:fired_brick"));

        // Smelting Bhatti: 8 Deccan Basalt blocks around perimeter
        register(Recipe.createShaped3x3("smelting_bhatti_furnace",
                new ItemStack(ItemRegistry.getItem("shilploka:smelting_bhatti"), 1),
                "shilploka:deccan_basalt", "shilploka:deccan_basalt", "shilploka:deccan_basalt",
                "shilploka:deccan_basalt", null, "shilploka:deccan_basalt",
                "shilploka:deccan_basalt", "shilploka:deccan_basalt", "shilploka:deccan_basalt"));

        // Chaak (Potter's Wheel): Clay + Stone Disc
        register(Recipe.createShaped3x3("chaak_potters_wheel",
                new ItemStack(ItemRegistry.getItem("shilploka:chaak"), 1),
                null, "shilploka:mitti_clay", null,
                "shilploka:granite", "shilploka:lakdi_chhad", "shilploka:granite",
                "shilploka:sal_planks", "shilploka:sal_planks", "shilploka:sal_planks"));

        // Kargah (Loom): Planks + Sticks
        register(Recipe.createShaped3x3("kargah_handloom",
                new ItemStack(ItemRegistry.getItem("shilploka:kargah"), 1),
                "shilploka:sal_planks", "shilploka:lakdi_chhad", "shilploka:sal_planks",
                "shilploka:sal_planks", "shilploka:lakdi_chhad", "shilploka:sal_planks",
                "shilploka:sal_planks", "shilploka:sal_planks", "shilploka:sal_planks"));

        // Chakki (Stone Mill): Granite + Stick
        register(Recipe.createShaped3x3("chakki_stone_mill",
                new ItemStack(ItemRegistry.getItem("shilploka:chakki"), 1),
                "shilploka:granite", "shilploka:granite", "shilploka:granite",
                null, "shilploka:lakdi_chhad", null,
                "shilploka:granite", "shilploka:granite", "shilploka:granite"));

        // Nihai (Forged Anvil): 3 Loha Ingots top row, 1 center, 3 base
        register(Recipe.createShaped3x3("nihai_anvil",
                new ItemStack(ItemRegistry.getItem("shilploka:nihai"), 1),
                "shilploka:loha_ingot", "shilploka:loha_ingot", "shilploka:loha_ingot",
                null, "shilploka:loha_ingot", null,
                "shilploka:loha_ingot", "shilploka:loha_ingot", "shilploka:loha_ingot"));

        // 7. Advanced 3x3 Tool Crafting for High Tiers
        // Loha (Iron) Kudaal
        register(Recipe.createShaped3x3("loha_kudaal_3x3",
                new ItemStack(ItemRegistry.LOHA_KUDAAL, 1),
                "shilploka:loha_ingot", "shilploka:loha_ingot", "shilploka:loha_ingot",
                null, "shilploka:lakdi_chhad", null,
                null, "shilploka:lakdi_chhad", null));

        // Wootz Steel Kudaal
        register(Recipe.createShaped3x3("wootz_kudaal_3x3",
                new ItemStack(ItemRegistry.WOOTZ_KUDAAL, 1),
                "shilploka:wootz_ingot", "shilploka:wootz_ingot", "shilploka:wootz_ingot",
                null, "shilploka:lakdi_chhad", null,
                null, "shilploka:lakdi_chhad", null));

        // Wootz Talwar
        register(Recipe.createShaped3x3("wootz_talwar_3x3",
                new ItemStack(ItemRegistry.WOOTZ_TALWAR, 1),
                null, "shilploka:wootz_ingot", null,
                null, "shilploka:wootz_ingot", null,
                null, "shilploka:lakdi_chhad", null));

        // Vajra Kudaal
        register(Recipe.createShaped3x3("vajra_kudaal_3x3",
                new ItemStack(ItemRegistry.VAJRA_KUDAAL, 1),
                "shilploka:hira", "shilploka:hira", "shilploka:hira",
                null, "shilploka:lakdi_chhad", null,
                null, "shilploka:lakdi_chhad", null));

        // Vajra Talwar
        register(Recipe.createShaped3x3("vajra_talwar_3x3",
                new ItemStack(ItemRegistry.VAJRA_TALWAR, 1),
                null, "shilploka:hira", null,
                null, "shilploka:hira", null,
                null, "shilploka:lakdi_chhad", null));

        // Vajra Ingot Synthesis (Vajra Core + Wootz Ingot + Bhasma)
        register(Recipe.createShapeless("vajra_ingot_craft",
                new ItemStack(ItemRegistry.VAJRA_INGOT, 1),
                "shilploka:vajra_core", "shilploka:wootz_ingot", "shilploka:bhasma"));

        LOGGER.info("RecipeManager initialized with {} crafting recipes.", RECIPES.size());
    }

    private RecipeManager() {}

    public static void register(Recipe recipe) {
        RECIPES.add(recipe);
    }

    public static Recipe findMatchingRecipe(ItemStack[] grid) {
        for (Recipe recipe : RECIPES) {
            if (recipe.matches(grid)) {
                return recipe;
            }
        }
        return null;
    }

    public static List<Recipe> getRecipes() {
        return Collections.unmodifiableList(RECIPES);
    }

    public static void init() {
        // Triggers static classloader initialization
    }
}
