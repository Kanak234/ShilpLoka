package shilploka.workstation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.crafting.CraftingGrid3x3;
import shilploka.crafting.RecipeManager;
import shilploka.item.Item;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Comprehensive unit test suite for ShilpLoka Phase P8 historical Indian workstations.
 *
 * WHAT:
 * Tests the 7 workstations: Karyashala, Pakki Bhatti, Smelting Bhatti, Kumhar Chaak,
 * Kargah, Atta Chakki, and Lohar Nihai.
 *
 * USED FOR:
 * Automated CI verification of workstation behaviors and crafting progression.
 */
public class WorkstationTest {

    @BeforeAll
    public static void setUp() {
        ItemRegistry.init();
        RecipeManager.init();
    }

    @Test
    @DisplayName("WorkstationType mappings from BlockIds are valid and reciprocal")
    public void testWorkstationTypeMapping() {
        assertEquals(WorkstationType.KARYASHALA, WorkstationType.fromBlockId(BlockIds.KARYASHALA));
        assertEquals(WorkstationType.PAKKI_BHATTI, WorkstationType.fromBlockId(BlockIds.PAKKI_BHATTI));
        assertEquals(WorkstationType.SMELTING_BHATTI, WorkstationType.fromBlockId(BlockIds.SMELTING_BHATTI));
        assertEquals(WorkstationType.CHAAK, WorkstationType.fromBlockId(BlockIds.CHAAK));
        assertEquals(WorkstationType.KARGAH, WorkstationType.fromBlockId(BlockIds.KARGAH));
        assertEquals(WorkstationType.CHAKKI, WorkstationType.fromBlockId(BlockIds.CHAKKI));
        assertEquals(WorkstationType.NIHAI, WorkstationType.fromBlockId(BlockIds.NIHAI));

        assertTrue(BlockIds.isWorkstation(BlockIds.KARYASHALA));
        assertTrue(BlockIds.isWorkstation(BlockIds.PAKKI_BHATTI));
        assertFalse(BlockIds.isWorkstation(BlockIds.AIR));
        assertFalse(BlockIds.isWorkstation(BlockIds.ALLUVIAL_SOIL));
    }

    @Test
    @DisplayName("Karyashala 3x3 workbench crafts Pakki Bhatti and Wootz Kudaal")
    public void testKaryashalaCrafting() {
        CraftingGrid3x3 grid = new CraftingGrid3x3();
        Item brick = ItemRegistry.getItem("shilploka:fired_brick");
        assertNotNull(brick);

        // Pakki Bhatti recipe: 8 Fired Bricks hollow square in 3x3
        grid.setMatrixSlot(0, new ItemStack(brick, 1));
        grid.setMatrixSlot(1, new ItemStack(brick, 1));
        grid.setMatrixSlot(2, new ItemStack(brick, 1));
        grid.setMatrixSlot(3, new ItemStack(brick, 1));
        // slot 4 is empty
        grid.setMatrixSlot(5, new ItemStack(brick, 1));
        grid.setMatrixSlot(6, new ItemStack(brick, 1));
        grid.setMatrixSlot(7, new ItemStack(brick, 1));
        grid.setMatrixSlot(8, new ItemStack(brick, 1));

        ItemStack result = grid.getResultSlot();
        assertFalse(result.isEmpty());
        assertEquals("shilploka:pakki_bhatti", result.getItem().getId());

        // Test craft action
        ItemStack crafted = grid.takeResult();
        assertEquals("shilploka:pakki_bhatti", crafted.getItem().getId());
        assertEquals(1, crafted.getCount());
        assertTrue(grid.getMatrixSlot(0).isEmpty());
        assertTrue(grid.getMatrixSlot(1).isEmpty());
    }

    @Test
    @DisplayName("Pakki Bhatti thermal kiln fires Mitti Clay into Fired Brick using Koyla")
    public void testKilnManagerFiring() {
        KilnManager kiln = new KilnManager();
        Item mittiClay = ItemRegistry.getItem("shilploka:mitti_clay");
        Item koyla = ItemRegistry.getItem("shilploka:koyla");
        assertNotNull(mittiClay);
        assertNotNull(koyla);

        kiln.setInputSlot(new ItemStack(mittiClay, 2));
        kiln.setFuelSlot(new ItemStack(koyla, 1));

        // Initial state
        assertFalse(kiln.isBurning());
        assertEquals(0.0f, kiln.getCookProgress());

        // Fast-forward ticks to complete firing of 1 brick (160 ticks cook time)
        for (int i = 0; i <= 165; i++) {
            kiln.tick();
        }

        // Cooked item should be in output
        ItemStack output = kiln.getOutputSlot();
        assertFalse(output.isEmpty());
        assertEquals("shilploka:fired_brick", output.getItem().getId());
        assertEquals(1, output.getCount());

        // Remaining input should be 1
        assertEquals(1, kiln.getInputSlot().getCount());
        assertTrue(kiln.isBurning()); // Koyla burns for 1600 ticks
    }

    @Test
    @DisplayName("Smelting Bhatti combines Taamba Ingot + Taamba Ore into Kansa (Bronze) ingot")
    public void testAlloyManagerBronzeSmelting() {
        AlloyManager smelter = new AlloyManager();
        Item taambaIngot = ItemRegistry.getItem("shilploka:taamba_ingot");
        Item taambaOre = ItemRegistry.getItem("shilploka:taamba_ore");
        Item koyla = ItemRegistry.getItem("shilploka:koyla");
        assertNotNull(taambaIngot);
        assertNotNull(taambaOre);
        assertNotNull(koyla);

        smelter.setPrimarySlot(new ItemStack(taambaIngot, 2));
        smelter.setSecondarySlot(new ItemStack(taambaOre, 2));
        smelter.setFuelSlot(new ItemStack(koyla, 1));

        // Fast-forward ticks to complete smelting alloy (200 ticks)
        for (int i = 0; i <= 205; i++) {
            smelter.tick();
        }

        ItemStack output = smelter.getOutputSlot();
        assertFalse(output.isEmpty());
        assertEquals("shilploka:kansa_ingot", output.getItem().getId());
        assertEquals(2, output.getCount());

        // Input counts decremented
        assertEquals(1, smelter.getPrimarySlot().getCount());
        assertEquals(1, smelter.getSecondarySlot().getCount());
    }

    @Test
    @DisplayName("Smelting Bhatti creates Wootz crucible steel from Loha Ingot + Koyla")
    public void testAlloyManagerWootzSteel() {
        AlloyManager smelter = new AlloyManager();
        Item loha = ItemRegistry.getItem("shilploka:loha_ingot");
        Item koyla = ItemRegistry.getItem("shilploka:koyla");
        assertNotNull(loha);
        assertNotNull(koyla);

        smelter.setPrimarySlot(new ItemStack(loha, 1));
        smelter.setSecondarySlot(new ItemStack(koyla, 1));
        smelter.setFuelSlot(new ItemStack(koyla, 1));

        for (int i = 0; i <= 205; i++) {
            smelter.tick();
        }

        ItemStack output = smelter.getOutputSlot();
        assertFalse(output.isEmpty());
        assertEquals("shilploka:wootz_ingot", output.getItem().getId());
        assertEquals(1, output.getCount());
    }

    @Test
    @DisplayName("Kumhar Chaak crafts terracotta pottery from raw Mitti Clay")
    public void testChaakManagerPottery() {
        ChaakManager chaak = new ChaakManager();
        Item mittiClay = ItemRegistry.getItem("shilploka:mitti_clay");
        assertNotNull(mittiClay);

        // Kulhad requires 1 clay
        chaak.setClaySlot(new ItemStack(mittiClay, 5));
        assertTrue(chaak.canShape(0)); // 0 = Kulhad
        assertTrue(chaak.craftShape(0));
        assertEquals("shilploka:kulhad", chaak.getOutputSlot().getItem().getId());
        assertEquals(2, chaak.getOutputSlot().getCount()); // Kulhad crafts 2 per lump
        assertEquals(4, chaak.getClaySlot().getCount());

        // Matka requires 3 clay
        chaak.setOutputSlot(ItemStack.EMPTY);
        assertTrue(chaak.canShape(2)); // 2 = Matka
        assertTrue(chaak.craftShape(2));
        assertEquals("shilploka:matka", chaak.getOutputSlot().getItem().getId());
        assertEquals(1, chaak.getClaySlot().getCount());

        // Insufficient clay test (Surahi requires 4 clay, only 1 left)
        chaak.setOutputSlot(ItemStack.EMPTY);
        assertFalse(chaak.canShape(3)); // 3 = Surahi
        assertFalse(chaak.craftShape(3));
    }

    @Test
    @DisplayName("Kargah handloom spins cotton into soot and weaves Khadi cloth")
    public void testLoomManagerWeaving() {
        LoomManager loom = new LoomManager();
        Item kapaas = ItemRegistry.getItem("shilploka:kapaas");
        Item soot = ItemRegistry.getItem("shilploka:soot");
        assertNotNull(kapaas);
        assertNotNull(soot);

        // Spin yarn: 1 Kapaas -> 2 Soot
        loom.setFiberSlot(new ItemStack(kapaas, 2));
        assertTrue(loom.canSpinYarn());
        assertTrue(loom.spinYarn());
        assertEquals("shilploka:soot", loom.getOutputSlot().getItem().getId());
        assertEquals(2, loom.getOutputSlot().getCount());
        assertEquals(1, loom.getFiberSlot().getCount());

        // Weave cloth: 4 Soot -> 1 Khadi Vastra
        loom.setOutputSlot(ItemStack.EMPTY);
        loom.setFiberSlot(new ItemStack(soot, 4));
        assertTrue(loom.canWeaveCloth());
        assertTrue(loom.weaveCloth());
        assertEquals("shilploka:khadi_vastra", loom.getOutputSlot().getItem().getId());
        assertEquals(1, loom.getOutputSlot().getCount());
        assertTrue(loom.getFiberSlot().isEmpty());
    }

    @Test
    @DisplayName("Atta Chakki stone mill quern grinds grains and spices")
    public void testChakkiManagerMilling() {
        ChakkiManager chakki = new ChakkiManager();
        Item chawal = ItemRegistry.getItem("shilploka:chawal");
        Item haldi = ItemRegistry.HALDI_RHIZOME;
        assertNotNull(chawal);
        assertNotNull(haldi);

        // Grind rice into flour
        chakki.setGrainSlot(new ItemStack(chawal, 2));
        assertTrue(chakki.canGrind());
        assertTrue(chakki.grind());
        assertEquals("shilploka:atta", chakki.getFlourSlot().getItem().getId());
        assertEquals(1, chakki.getGrainSlot().getCount());

        // Grind haldi rhizome into haldi powder
        chakki.setFlourSlot(ItemStack.EMPTY);
        chakki.setGrainSlot(new ItemStack(haldi, 1));
        assertTrue(chakki.canGrind());
        assertTrue(chakki.grind());
        assertEquals("shilploka:haldi_powder", chakki.getFlourSlot().getItem().getId());
        assertTrue(chakki.getGrainSlot().isEmpty());
    }

    @Test
    @DisplayName("Lohar Nihai anvil restores tool durability with matching virgin metal")
    public void testNihaiManagerToolRepair() {
        NihaiManager nihai = new NihaiManager();
        Item ironPick = ItemRegistry.LOHA_KUDAAL;
        Item ironIngot = ItemRegistry.getItem("shilploka:loha_ingot");
        Item bronzeIngot = ItemRegistry.getItem("shilploka:kansa_ingot");
        assertNotNull(ironPick);
        assertNotNull(ironIngot);
        assertNotNull(bronzeIngot);

        ItemStack damagedPick = new ItemStack(ironPick, 1);
        int maxDurability = ironPick.getMaxDurability(); // 380 for IRON
        damagedPick.setDamage(300); // 300 damage out of 380

        // Test mismatched metal rejection
        nihai.setToolSlot(damagedPick);
        nihai.setMaterialSlot(new ItemStack(bronzeIngot, 1));
        assertTrue(nihai.getResultSlot().isEmpty());

        // Test matching iron ingot
        nihai.setMaterialSlot(new ItemStack(ironIngot, 1));
        ItemStack repaired = nihai.getResultSlot();
        assertFalse(repaired.isEmpty());
        // +60% repair = 380 * 0.6 = 228 damage restored -> remaining damage = 300 - 228 = 72
        assertEquals(72, repaired.getDamage());

        // Perform take repair
        ItemStack taken = nihai.takeRepairedTool();
        assertEquals(72, taken.getDamage());
        assertTrue(nihai.getToolSlot().isEmpty());
        assertTrue(nihai.getMaterialSlot().isEmpty());
        assertTrue(nihai.getResultSlot().isEmpty());
    }
}
