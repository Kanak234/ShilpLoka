package shilploka.worldgen.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.biome.BiomeType;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies that the historical monuments of Bharat generate accurately with authentic
 * materials, intact structural components, and era-appropriate relic loot.
 *
 * WHAT:
 * Tests Great Bath, Buddhist Stupa, Chola Stepwell, Hill Fort, Ashoka Sthambha,
 * biome associations, and artifact loot tables.
 */
class HistoricalStructureTest {

    @Test
    @DisplayName("GreatBathGenerator generates fired brick courtyard with central water pool and relic chest")
    void testGreatBathGenerator() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(12345L);

        GreatBathGenerator.generate(column, 1, 64, 1, rand);

        // Verify fired brick platform
        assertEquals(BlockIds.FIRED_BRICK, column.getBlock(1, 64, 1));
        // Verify water pool in central sunken basin
        assertEquals(BlockIds.WATER, column.getBlock(5, 64, 5));
        // Verify ancient relic chest placed
        assertEquals(BlockIds.ANCIENT_CHEST, column.getBlock(2, 65, 2));
    }

    @Test
    @DisplayName("StupaGenerator generates sandstone Anda dome and crowning Ashoka Chakra")
    void testStupaGenerator() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(54321L);

        StupaGenerator.generate(column, 1, 64, 1, rand);

        // Verify base red sandstone plinth
        assertEquals(BlockIds.RED_SANDSTONE, column.getBlock(1, 64, 1));
        // Verify central relic chamber contains ancient chest
        assertEquals(BlockIds.ANCIENT_CHEST, column.getBlock(7, 65, 7));
        // Verify crowning Dharma Chakra atop triple umbrella spire (baseY + 10 = 74)
        assertEquals(BlockIds.ASHOKA_CHAKRA, column.getBlock(7, 74, 7));
    }

    @Test
    @DisplayName("StepwellGenerator excavates subterranean geometric terraces down into pristine water reservoir")
    void testStepwellGenerator() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(99999L);

        StepwellGenerator.generate(column, 2, 70, 2, rand);

        // Verify retaining wall at surface
        assertEquals(BlockIds.YELLOW_SANDSTONE, column.getBlock(2, 71, 2));
        // Verify deep water reservoir at bottom (70 - 10 = 60)
        assertEquals(BlockIds.WATER, column.getBlock(6, 61, 6));
        // Verify subterranean relic chest
        assertEquals(BlockIds.ANCIENT_CHEST, column.getBlock(4, 62, 4));
    }

    @Test
    @DisplayName("HillFortGenerator creates fortified basalt and sandstone ramparts with armory chest")
    void testHillFortGenerator() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(11111L);

        HillFortGenerator.generate(column, 1, 80, 1, rand);

        // Verify stone foundation
        assertEquals(BlockIds.STONE, column.getBlock(1, 80, 1));
        // Verify ramparts and battlements
        assertEquals(BlockIds.CARVED_STONE, column.getBlock(5, 85, 1));
        // Verify armory chest inside courtyard
        assertEquals(BlockIds.ANCIENT_CHEST, column.getBlock(7, 82, 7));
    }

    @Test
    @DisplayName("AshokaSthambhaGenerator erects carved monolithic pillar with crowning Dharma Chakra")
    void testAshokaSthambhaGenerator() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(77777L);

        AshokaSthambhaGenerator.generate(column, 5, 64, 5, rand);

        // Verify carved base
        assertEquals(BlockIds.RED_SANDSTONE, column.getBlock(5, 64, 5));
        // Verify monolithic shaft
        assertEquals(BlockIds.CARVED_STONE, column.getBlock(8, 68, 8));
        // Verify crowning Ashoka Chakra (64 + 10 = 74)
        assertEquals(BlockIds.ASHOKA_CHAKRA, column.getBlock(8, 74, 8));
        // Verify edict chest at base
        assertEquals(BlockIds.ANCIENT_CHEST, column.getBlock(8, 66, 9));
    }

    @Test
    @DisplayName("HistoricalStructureManager associates authentic structure types with biomes")
    void testBiomeStructureMapping() {
        HistoricalStructureManager manager = new HistoricalStructureManager(42L);
        Random rand = new Random(42L);

        assertEquals(StructureType.CHOLA_STEPWELL, manager.selectStructureForBiome(BiomeType.THAR_MARUSTHAL, rand));
        assertEquals(StructureType.CHOLA_STEPWELL, manager.selectStructureForBiome(BiomeType.RANN_NAMAK, rand));
        assertEquals(StructureType.HILL_FORT, manager.selectStructureForBiome(BiomeType.HIMALAYA_SHIKHAR, rand));
        assertEquals(StructureType.HILL_FORT, manager.selectStructureForBiome(BiomeType.VINDHYA_JANGAL, rand));
    }

    @Test
    @DisplayName("HistoricalLoot generates era-authentic artifacts for each structure type")
    void testLootTables() {
        Random rand = new Random(1337L);

        // Harappan Bath: Harappan Seal
        List<ItemStack> harappaLoot = HistoricalLoot.generateLoot(StructureType.HARAPPAN_BATH, rand);
        assertTrue(harappaLoot.stream().anyMatch(s -> s.getItem() == ItemRegistry.HARAPPAN_SEAL));

        // Buddhist Stupa: Maurya Edict or Talpandulipi
        List<ItemStack> stupaLoot = HistoricalLoot.generateLoot(StructureType.BUDDHIST_STUPA, rand);
        assertTrue(stupaLoot.stream().anyMatch(s -> s.getItem() == ItemRegistry.MAURYA_EDICT));

        // Stepwell: Tamrapatra copper plate grant
        List<ItemStack> stepwellLoot = HistoricalLoot.generateLoot(StructureType.CHOLA_STEPWELL, rand);
        assertTrue(stepwellLoot.stream().anyMatch(s -> s.getItem() == ItemRegistry.TAMRAPATRA));

        // Hill Fort: Wootz crucible steel ingot
        List<ItemStack> fortLoot = HistoricalLoot.generateLoot(StructureType.HILL_FORT, rand);
        assertTrue(fortLoot.stream().anyMatch(s -> s.getItem() == ItemRegistry.WOOTZ_INGOT));
    }
}
