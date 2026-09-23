package shilploka.worldgen.dimension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.crafting.Recipe;
import shilploka.crafting.RecipeManager;
import shilploka.entity.EntityManager;
import shilploka.entity.Player;
import shilploka.entity.living.boss.AndhakaarNaagEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.dimension.DimensionManager;
import shilploka.world.dimension.DimensionType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates Phase P14: Multi-dimensional realms (Paataal volcanic underworld, Akash Dweep celestial islands),
 * portal gateways, Andhakaar Naag boss encounter dynamics, and Vajra tool progression.
 */
class DimensionBossTest {

    @BeforeAll
    static void init() {
        ItemRegistry.init();
        RecipeManager.init();
    }

    @Test
    @DisplayName("DimensionType correctly reflects Bharatiya cosmological realms and specifications")
    void testDimensionTypeProperties() {
        assertEquals(0, DimensionType.PRITHVI.getId());
        assertEquals(1, DimensionType.PAATAAL.getId());
        assertEquals(2, DimensionType.AKASH_DWEEP.getId());

        assertTrue(DimensionType.PRITHVI.hasSkyLight());
        assertFalse(DimensionType.PAATAAL.hasSkyLight(), "Paataal subterranean cavern has no direct skylight");
        assertTrue(DimensionType.AKASH_DWEEP.hasSkyLight(), "Akash Dweep open celestial skies have bright skylight");

        assertEquals(320, DimensionType.PRITHVI.getBuildHeight());
        assertEquals(128, DimensionType.PAATAAL.getBuildHeight());
        assertEquals(256, DimensionType.AKASH_DWEEP.getBuildHeight());

        assertSame(DimensionType.PAATAAL, DimensionType.fromId(1));
        assertSame(DimensionType.AKASH_DWEEP, DimensionType.fromId(2));
    }

    @Test
    @DisplayName("PaataalGenerator creates subterranean basalt caverns, lava oceans, and boss altar at (0,0)")
    void testPaataalGenerator() {
        PaataalGenerator gen = new PaataalGenerator(12345L);

        // Test non-boss chunk
        ChunkColumn col = new ChunkColumn(3, 4);
        gen.generateColumn(col);

        // Bedrock boundaries
        assertEquals(BlockIds.DECCAN_BASALT, col.getBlock(0, 0, 0));
        assertEquals(BlockIds.DECCAN_BASALT, col.getBlock(5, 1, 5));
        assertEquals(BlockIds.DECCAN_BASALT, col.getBlock(10, 127, 10));

        // Subterranean lava ocean check: any open cavern cavity at Y <= 32 should be lava
        boolean hasLavaOrAgni = false;
        for (int y = 5; y <= 32; y++) {
            short block = (short) col.getBlock(8, y, 8);
            if (block == BlockIds.LAVA || block == BlockIds.PAATAAL_AGNI_ROCK || block == BlockIds.PAATAAL_LAVA_GEL) {
                hasLavaOrAgni = true;
                break;
            }
        }
        assertTrue(hasLavaOrAgni, "Paataal subterranean strata must contain lava or agni rock");

        // Test central chunk (0, 0) boss altar
        ChunkColumn originCol = new ChunkColumn(0, 0);
        gen.generateColumn(originCol);

        // Platform center at (7, 40, 7) should be Deccan Basalt, and (7, 41, 7) should have Dimensional Portal
        assertEquals(BlockIds.DECCAN_BASALT, originCol.getBlock(7, 40, 7));
        assertEquals(BlockIds.DIMENSIONAL_PORTAL, originCol.getBlock(7, 41, 7));
        assertEquals(BlockIds.DIMENSIONAL_PORTAL, originCol.getBlock(8, 41, 8));
    }

    @Test
    @DisplayName("AkashDweepGenerator generates celestial floating islands of Akash Marble and return portal")
    void testAkashDweepGenerator() {
        AkashDweepGenerator gen = new AkashDweepGenerator(54321L);
        ChunkColumn col = new ChunkColumn(0, 0);
        gen.generateColumn(col);

        // Beneath floating islands (e.g. Y=10) should be open void/air
        assertEquals(BlockIds.AIR, col.getBlock(0, 10, 0));
        assertEquals(BlockIds.AIR, col.getBlock(7, 20, 7));

        // Central shrine at (7, 95, 7) should be Akash Marble with Dimensional Portal at (7, 96, 7)
        assertEquals(BlockIds.AKASH_MARBLE, col.getBlock(7, 95, 7));
        assertEquals(BlockIds.DIMENSIONAL_PORTAL, col.getBlock(7, 96, 7));
        assertEquals(BlockIds.DIMENSIONAL_PORTAL, col.getBlock(8, 96, 8));
        assertEquals(BlockIds.AKASH_CRYSTAL, col.getBlock(5, 96, 5));
    }

    @Test
    @DisplayName("DimensionManager coordinates realm transitions, arrival coordinates, and cooldown")
    void testDimensionManager() {
        DimensionManager dm = new DimensionManager(9999L);
        assertEquals(DimensionType.PRITHVI, dm.getCurrentDimension());

        Player player = new Player(10.0, 65.0, 10.0);

        // Teleport to Paataal
        dm.teleportPlayer(player, DimensionType.PAATAAL);
        assertEquals(DimensionType.PAATAAL, dm.getCurrentDimension());
        assertEquals(7.5, player.getPosition().x, 0.01);
        assertEquals(43.0, player.getPosition().y, 0.01);
        assertEquals(7.5, player.getPosition().z, 0.01);

        // Teleport to Akash Dweep
        dm.teleportPlayer(player, DimensionType.AKASH_DWEEP);
        assertEquals(DimensionType.AKASH_DWEEP, dm.getCurrentDimension());
        assertEquals(7.5, player.getPosition().x, 0.01);
        assertEquals(98.0, player.getPosition().y, 0.01);
        assertEquals(7.5, player.getPosition().z, 0.01);

        // Teleport back to Prithvi
        dm.teleportPlayer(player, DimensionType.PRITHVI);
        assertEquals(DimensionType.PRITHVI, dm.getCurrentDimension());
        assertEquals(0.5, player.getPosition().x, 0.01);
        assertEquals(70.0, player.getPosition().y, 0.01);
        assertEquals(0.5, player.getPosition().z, 0.01);
    }

    @Test
    @DisplayName("Andhakaar Naag boss exhibits phase transitions, enrage mode, and drops Vajra Core")
    void testAndhakaarNaagBoss() {
        Player player = new Player(7.5, 43.0, 7.5);
        AndhakaarNaagEntity boss = new AndhakaarNaagEntity(7.5, 43.0, 15.0, null, player);

        assertEquals(300.0f, boss.getMaxHealth());
        assertEquals(300.0f, boss.getHealth());
        assertFalse(boss.isEnraged());
        assertEquals(1.0f, boss.getHealthFraction(), 0.01f);
        assertTrue(boss.getEntityName().contains("Andhakaar Naag"));

        // Simulate damage down to 40% health (enraged threshold <= 50%)
        boss.damage(160.0f);
        assertEquals(140.0f, boss.getHealth());
        assertTrue(boss.isEnraged(), "Boss should enter enraged phase at <= 50% health");
        assertTrue(boss.getEntityName().contains("Enraged"));
        assertTrue(boss.getHindiName().contains("क्रोधित"));

        // Tick boss to trigger enraged velocity update
        boss.tick(0.05f, null, player);
        assertEquals(5.4f, boss.getMoveSpeed(), 0.01f);

        // Kill boss and check death drops
        boss.resetHurtTime();
        boss.damage(200.0f);
        assertTrue(boss.isDead());
        List<ItemStack> drops = boss.getDeathDrops();
        assertNotNull(drops);
        assertFalse(drops.isEmpty());

        boolean hasVajraCore = drops.stream()
                .anyMatch(stack -> stack.getItem().getId().equals("shilploka:vajra_core") && stack.getCount() >= 1);
        assertTrue(hasVajraCore, "Defeating Andhakaar Naag must yield the sacred Vajra Core");
    }

    @Test
    @DisplayName("Vajra Core allows forging Vajra Ingots and legendary Vajra tools in RecipeManager")
    void testVajraCraftingProgression() {
        // Vajra Ingot shapeless recipe: vajra_core + wootz_ingot + bhasma
        ItemStack[] ingotGrid = new ItemStack[4];
        ingotGrid[0] = new ItemStack(ItemRegistry.VAJRA_CORE, 1);
        ingotGrid[1] = new ItemStack(ItemRegistry.WOOTZ_INGOT, 1);
        ingotGrid[2] = new ItemStack(ItemRegistry.BHASMA, 1);
        ingotGrid[3] = null;

        Recipe ingotRecipe = RecipeManager.findMatchingRecipe(ingotGrid);
        assertNotNull(ingotRecipe, "Vajra Ingot shapeless recipe must resolve");
        assertEquals("shilploka:vajra_ingot", ingotRecipe.getOutput().getItem().getId());

        // Vajra Kudaal 3x3 recipe: 3 Hira/Vajra + 2 sticks
        ItemStack[] kudaalGrid = new ItemStack[9];
        kudaalGrid[0] = new ItemStack(ItemRegistry.HIRA, 1);
        kudaalGrid[1] = new ItemStack(ItemRegistry.HIRA, 1);
        kudaalGrid[2] = new ItemStack(ItemRegistry.HIRA, 1);
        kudaalGrid[3] = null;
        kudaalGrid[4] = new ItemStack(ItemRegistry.LAKDI_CHHAD, 1);
        kudaalGrid[5] = null;
        kudaalGrid[6] = null;
        kudaalGrid[7] = new ItemStack(ItemRegistry.LAKDI_CHHAD, 1);
        kudaalGrid[8] = null;

        Recipe kudaalRecipe = RecipeManager.findMatchingRecipe(kudaalGrid);
        assertNotNull(kudaalRecipe, "Vajra Kudaal recipe must resolve in 3x3 grid");
        assertEquals(ItemRegistry.VAJRA_KUDAAL.getId(), kudaalRecipe.getOutput().getItem().getId());
    }
}
