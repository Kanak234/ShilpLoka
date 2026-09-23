package shilploka.trade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.entity.Player;
import shilploka.entity.living.npc.GramwasiEntity;
import shilploka.entity.living.npc.VillagerProfession;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.ui.BarterScreen;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies that the Indian village barter trading system behaves reliably and authentically.
 *
 * WHAT:
 * Tests all 9 villager professions, trade offer matching and execution,
 * stock limits, and interactive BarterScreen integration with Player inventory.
 */
class BarterTradingTest {

    @Test
    @DisplayName("All 9 VillagerProfessions have authentic Devanagari titles, workstations, and trade catalogs")
    void testProfessionsAndCatalogs() {
        for (VillagerProfession prof : VillagerProfession.values()) {
            assertNotNull(prof.getDisplayName(), "Profession title must not be null");
            assertNotNull(prof.getDescription(), "Profession description must not be null");
            assertTrue(prof.getWorkstationBlockId() > 0, "Profession must have an associated block/workstation");

            GramwasiEntity villager = new GramwasiEntity(10.0, 64.0, 10.0, prof);
            assertEquals(prof, villager.getProfession());
            assertFalse(villager.getTradeOffers().isEmpty(), "Villager of " + prof + " must have trade offers");
            assertEquals(20.0f, villager.getHealth());
        }
    }

    @Test
    @DisplayName("TradeOffer matches player items and decrements costs accurately upon execution")
    void testTradeOfferExecution() {
        TradeOffer offer = new TradeOffer(
                new ItemStack(ItemRegistry.MUDRA_COPPER, 2),
                new ItemStack(ItemRegistry.ROTI, 4),
                5
        );

        assertEquals(0, offer.getUses());
        assertFalse(offer.isOutOfStock());

        // Insufficient input
        ItemStack insufficient = new ItemStack(ItemRegistry.MUDRA_COPPER, 1);
        assertFalse(offer.matches(insufficient, ItemStack.EMPTY));

        // Sufficient input
        ItemStack payment = new ItemStack(ItemRegistry.MUDRA_COPPER, 5);
        assertTrue(offer.matches(payment, ItemStack.EMPTY));

        // Execute trade
        ItemStack result = offer.execute(payment, ItemStack.EMPTY);
        assertEquals(4, result.getCount());
        assertEquals(ItemRegistry.ROTI, result.getItem());
        assertEquals(3, payment.getCount(), "5 Mudras - 2 Mudras cost should leave 3 Mudras");
        assertEquals(1, offer.getUses());
    }

    @Test
    @DisplayName("TradeOffer respects max uses and can be restocked")
    void testStockLimits() {
        TradeOffer offer = new TradeOffer(
                new ItemStack(ItemRegistry.KAURI, 4),
                new ItemStack(ItemRegistry.MUDRA_COPPER, 1),
                2
        );

        ItemStack kauri = new ItemStack(ItemRegistry.KAURI, 20);
        offer.execute(kauri, ItemStack.EMPTY);
        assertFalse(offer.isOutOfStock());
        offer.execute(kauri, ItemStack.EMPTY);
        assertTrue(offer.isOutOfStock(), "Trade should be out of stock after reaching max uses");

        assertFalse(offer.matches(kauri, ItemStack.EMPTY));
        ItemStack failed = offer.execute(kauri, ItemStack.EMPTY);
        assertTrue(failed.isEmpty());

        offer.restock();
        assertFalse(offer.isOutOfStock(), "Restocked trade should be available again");
    }

    @Test
    @DisplayName("BarterScreen allows player to complete trade from inventory")
    void testBarterScreenInteraction() {
        GramwasiEntity kisan = new GramwasiEntity(0, 64, 0, VillagerProfession.KISAN);
        BarterScreen screen = new BarterScreen();

        assertFalse(screen.isOpen());
        screen.open(kisan);
        assertTrue(screen.isOpen());
        assertEquals(kisan, screen.getActiveVillager());

        Player player = new Player(0, 64, 0);
        player.getInventory().clear();

        // Kisan trade 1 (index 1): 2 Copper Mudras -> 4 Roti
        screen.selectTrade(1);
        TradeOffer selected = screen.getSelectedTrade();
        assertNotNull(selected);

        // Fail without currency
        assertFalse(screen.performTrade(player));

        // Add 4 Copper Mudras to player inventory
        player.getInventory().addItem(new ItemStack(ItemRegistry.MUDRA_COPPER, 4));

        // Now trade succeeds
        boolean success = screen.performTrade(player);
        assertTrue(success, "Trade should succeed with sufficient copper mudras");

        // Player now has 2 Mudras remaining and 4 Rotis
        int remainingMudras = 0;
        int rotis = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getSlot(i);
            if (!stack.isEmpty()) {
                if (stack.getItem() == ItemRegistry.MUDRA_COPPER) remainingMudras += stack.getCount();
                if (stack.getItem() == ItemRegistry.ROTI) rotis += stack.getCount();
            }
        }
        assertEquals(2, remainingMudras);
        assertEquals(4, rotis);

        screen.close();
        assertFalse(screen.isOpen());
    }
}
