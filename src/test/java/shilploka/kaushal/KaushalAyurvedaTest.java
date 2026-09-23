package shilploka.kaushal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.agriculture.CropManager;
import shilploka.ayurveda.OushadhManager;
import shilploka.entity.Player;
import shilploka.entity.living.fauna.ElephantEntity;
import shilploka.entity.living.mount.MarwariHorseEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase P13 Unit Test Suite:
 * - Kaushal Vedic Tool Upgrades (KaushalManager, KaushalUpgradeType)
 * - Ayurvedic Medicine Concoction (OushadhManager)
 * - Indigenous Agriculture & Irrigation (CropManager)
 * - Authentic Indian Mounts & Saddling (MarwariHorseEntity, ElephantEntity)
 */
public class KaushalAyurvedaTest {

    @Test
    @DisplayName("KaushalManager provides valid upgrades for Talwar and Kudaal")
    void testKaushalAvailableUpgrades() {
        KaushalManager km = new KaushalManager();

        // 1. Tool slot empty -> no upgrades
        assertTrue(km.getAvailableUpgrades().isEmpty());

        // 2. Talwar -> TEJ_DHAAR, MAJBOOT_SHILP, JAL_SHWAAS
        km.setToolSlot(new ItemStack(ItemRegistry.WOOTZ_TALWAR, 1));
        List<KaushalUpgradeType> talwarUpgrades = km.getAvailableUpgrades();
        assertTrue(talwarUpgrades.contains(KaushalUpgradeType.TEJ_DHAAR));
        assertTrue(talwarUpgrades.contains(KaushalUpgradeType.MAJBOOT_SHILP));
        assertTrue(talwarUpgrades.contains(KaushalUpgradeType.JAL_SHWAAS));

        // 3. Kudaal -> TEZ_KHANAN, MAJBOOT_SHILP, JAL_SHWAAS
        km.setToolSlot(new ItemStack(ItemRegistry.WOOTZ_KUDAAL, 1));
        List<KaushalUpgradeType> kudaalUpgrades = km.getAvailableUpgrades();
        assertTrue(kudaalUpgrades.contains(KaushalUpgradeType.TEZ_KHANAN));
        assertTrue(kudaalUpgrades.contains(KaushalUpgradeType.MAJBOOT_SHILP));
        assertTrue(kudaalUpgrades.contains(KaushalUpgradeType.JAL_SHWAAS));
    }

    @Test
    @DisplayName("KaushalManager successfully bestows Vedic upgrades consuming catalyst")
    void testKaushalApplyUpgrade() {
        KaushalManager km = new KaushalManager();
        ItemStack sword = new ItemStack(ItemRegistry.WOOTZ_TALWAR, 1);
        ItemStack catalyst = new ItemStack(ItemRegistry.BHASMA, 5);

        km.setToolSlot(sword);
        km.setCatalystSlot(catalyst);

        int cost = KaushalUpgradeType.TEJ_DHAAR.getLapisCost();
        boolean success = km.applyUpgrade(KaushalUpgradeType.TEJ_DHAAR);

        assertTrue(success);
        assertEquals(5 - cost, catalyst.getCount());
        assertFalse(km.getOutputSlot().isEmpty());
        assertEquals(KaushalUpgradeType.TEJ_DHAAR, km.getOutputSlot().getKaushal());
        assertTrue(km.getToolSlot().isEmpty());
    }

    @Test
    @DisplayName("KaushalManager rejects upgrades when catalyst is insufficient")
    void testKaushalInsufficientCatalyst() {
        KaushalManager km = new KaushalManager();
        ItemStack sword = new ItemStack(ItemRegistry.WOOTZ_TALWAR, 1);
        ItemStack lowCatalyst = new ItemStack(ItemRegistry.BHASMA, 1); // Cost is 2 or 3

        km.setToolSlot(sword);
        km.setCatalystSlot(lowCatalyst);

        boolean success = km.applyUpgrade(KaushalUpgradeType.TEJ_DHAAR);
        assertFalse(success);
        assertEquals(1, lowCatalyst.getCount());
        assertTrue(km.getOutputSlot().isEmpty());
    }

    @Test
    @DisplayName("OushadhManager brews authentic Ayurvedic medicines")
    void testOushadhBrewing() {
        // 1. Haldi Doodh (Milk + Turmeric)
        ItemStack haldiDoodh = OushadhManager.brew(
                new ItemStack(ItemRegistry.DOODH, 1),
                new ItemStack(ItemRegistry.HALDI_POWDER, 1)
        );
        assertEquals(ItemRegistry.HALDI_DOODH.getId(), haldiDoodh.getItem().getId());

        // 2. Tulsi Kadha (Turmeric / herb + Kulhad)
        ItemStack tulsiKadha = OushadhManager.brew(
                new ItemStack(ItemRegistry.HALDI_POWDER, 1),
                new ItemStack(ItemRegistry.KULHAD, 1)
        );
        assertEquals(ItemRegistry.TULSI_KADHA.getId(), tulsiKadha.getItem().getId());

        // 3. Neem Lep (Clay + Turmeric)
        ItemStack neemLep = OushadhManager.brew(
                new ItemStack(ItemRegistry.MITTI_CLAY, 1),
                new ItemStack(ItemRegistry.HALDI_POWDER, 1)
        );
        assertEquals(ItemRegistry.NEEM_LEP.getId(), neemLep.getItem().getId());
        assertEquals(2, neemLep.getCount());

        // 4. Shilajit Rasayana (Bhasma + Milk)
        ItemStack shilajit = OushadhManager.brew(
                new ItemStack(ItemRegistry.BHASMA, 1),
                new ItemStack(ItemRegistry.DOODH, 1)
        );
        assertEquals(ItemRegistry.SHILAJIT_RASAYANA.getId(), shilajit.getItem().getId());

        // 5. Invalid ingredients -> EMPTY
        ItemStack invalid = OushadhManager.brew(
                new ItemStack(ItemRegistry.KOYLA, 1),
                new ItemStack(ItemRegistry.ROTI, 1)
        );
        assertTrue(invalid.isEmpty());
    }

    @Test
    @DisplayName("CropManager validates soil tilling and seed sowing")
    void testCropManagerTillingAndSowing() {
        assertTrue(CropManager.canTill(BlockIds.ALLUVIAL_SOIL));
        assertFalse(CropManager.canTill(BlockIds.GRANITE));
        assertFalse(CropManager.canTill(BlockIds.THAR_SAND));

        // Planting Dhan on tilled soil
        int dhanBlock = CropManager.getPlantableCropBlock(new ItemStack(ItemRegistry.CHAWAL, 1), BlockIds.TILLED_SOIL);
        assertEquals(BlockIds.DHAN_CROP, dhanBlock);

        // Planting Ganna
        int gannaBlock = CropManager.getPlantableCropBlock(new ItemStack(ItemRegistry.GANNA, 1), BlockIds.TILLED_SOIL);
        assertEquals(BlockIds.GANNA_CROP, gannaBlock);

        // Planting Kapaas
        int kapaasBlock = CropManager.getPlantableCropBlock(new ItemStack(ItemRegistry.KAPAAS, 1), BlockIds.TILLED_SOIL);
        assertEquals(BlockIds.KAPAAS_CROP, kapaasBlock);

        // Invalid item cannot be planted
        int invalidBlock = CropManager.getPlantableCropBlock(new ItemStack(ItemRegistry.KOYLA, 1), BlockIds.TILLED_SOIL);
        assertEquals(BlockIds.AIR, invalidBlock);
    }

    @Test
    @DisplayName("CropManager applies Krishi Vridhhi harvest multiplier")
    void testCropHarvestMultiplier() {
        // Standard harvest without Kaushal
        List<ItemStack> standardDrops = CropManager.getCropHarvest(BlockIds.DHAN_CROP, null);
        assertFalse(standardDrops.isEmpty());
        assertEquals(3, standardDrops.get(0).getCount());

        // Harvest with KRISHI_VRIDDHI Kaushal tool
        ItemStack blessedTool = new ItemStack(ItemRegistry.WOOTZ_KUDAAL, 1);
        blessedTool.setKaushal(KaushalUpgradeType.KRISHI_VRIDDHI);
        List<ItemStack> blessedDrops = CropManager.getCropHarvest(BlockIds.DHAN_CROP, blessedTool);
        assertFalse(blessedDrops.isEmpty());
        assertEquals(6, blessedDrops.get(0).getCount(), "Blessed tool must double crop yield");
    }

    @Test
    @DisplayName("MarwariHorseEntity supports saddling and riding")
    void testMarwariHorseMount() {
        MarwariHorseEntity horse = new MarwariHorseEntity(10, 64, 10);
        Player player = new Player(10, 64, 10);

        assertEquals(35.0f, horse.getMaxHealth());
        assertFalse(horse.isSaddled());
        assertFalse(horse.isMounted());

        // 1. Cannot mount unsaddled horse
        ItemStack emptyHand = ItemStack.EMPTY;
        boolean mountedUnsad = horse.interact(player, emptyHand);
        assertFalse(mountedUnsad);
        assertFalse(horse.isMounted());

        // 2. Saddling with Kaathi
        ItemStack saddle = new ItemStack(ItemRegistry.KAATHI, 1);
        boolean saddled = horse.interact(player, saddle);
        assertTrue(saddled);
        assertTrue(horse.isSaddled());
        assertTrue(saddle.isEmpty());

        // 3. Mount saddled horse
        boolean mounted = horse.interact(player, emptyHand);
        assertTrue(mounted);
        assertTrue(horse.isMounted());
        assertEquals(player, horse.getRider());

        // 4. Dismount
        boolean dismounted = horse.interact(player, emptyHand);
        assertTrue(dismounted);
        assertFalse(horse.isMounted());
        assertNull(horse.getRider());
    }

    @Test
    @DisplayName("ElephantEntity implements RideableMount with majestic health")
    void testElephantRideableMount() {
        ElephantEntity elephant = new ElephantEntity(20, 64, 20);
        Player player = new Player(20, 64, 20);

        assertEquals(100.0f, elephant.getMaxHealth());
        assertFalse(elephant.isSaddled());

        ItemStack saddle = new ItemStack(ItemRegistry.KAATHI, 1);
        assertTrue(elephant.interact(player, saddle));
        assertTrue(elephant.isSaddled());

        assertTrue(elephant.interact(player, ItemStack.EMPTY));
        assertTrue(elephant.isMounted());
        assertEquals(player, elephant.getRider());
    }
}
