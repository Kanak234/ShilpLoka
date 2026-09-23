package shilploka.worldgen.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.entity.EntityManager;
import shilploka.entity.living.npc.GramwasiEntity;
import shilploka.entity.living.npc.VillagerProfession;
import shilploka.event.DaakuRaidManager;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates that rural Indian villages (Gaon) generate properly with central Panchayat Chaupal,
 * fresh water well (Kuaan), and artisan workstations, and verifies nocturnal Daaku raid mechanics.
 *
 * WHAT:
 * Tests GaonGenerator blocks and DaakuRaidManager state machine.
 */
class GaonGeneratorTest {

    @Test
    @DisplayName("GaonGenerator builds Panchayat Chaupal, village well, and artisan huts with workstations")
    void testGaonGeneration() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(424242L);

        GaonGenerator.generate(column, 0, 64, 0, rand);

        // 1. Verify Panchayat Chaupal at center
        // Elevated platform with alluvial soil
        assertEquals(BlockIds.ALLUVIAL_SOIL, column.getBlock(5, 65, 5));
        // Sacred Sal tree at center
        assertEquals(BlockIds.SAL_LOG, column.getBlock(7, 66, 7));
        assertEquals(BlockIds.SAL_LEAVES, column.getBlock(7, 70, 7));
        // Wooden seating planks on Chaupal
        assertEquals(BlockIds.SAL_PLANKS, column.getBlock(5, 66, 7));

        // 2. Verify Village Well (Kuaan) with freshwater reservoir at (startX + 1, startZ + 1)
        assertEquals(BlockIds.WATER, column.getBlock(2, 64, 2));

        // 3. Verify Artisan workstations inside huts
        // Kumhar Hut: Chaak potter's wheel and Pakki Bhatti kiln
        assertEquals(BlockIds.CHAAK, column.getBlock(13, 65, 3));
        assertEquals(BlockIds.PAKKI_BHATTI, column.getBlock(12, 65, 3));

        // Lohar Forge: Nihai anvil and Smelting Bhatti
        assertEquals(BlockIds.NIHAI, column.getBlock(3, 65, 13));
        assertEquals(BlockIds.SMELTING_BHATTI, column.getBlock(2, 65, 13));

        // Julaha Workshop: Kargah loom and Chakki stone mill
        assertEquals(BlockIds.KARGAH, column.getBlock(13, 65, 13));
        assertEquals(BlockIds.CHAKKI, column.getBlock(12, 65, 13));
    }

    @Test
    @DisplayName("DaakuRaidManager spawns raiders, updates villagers, and concludes upon victory")
    void testDaakuRaidLifecycle() {
        DaakuRaidManager raidManager = new DaakuRaidManager();
        EntityManager entityManager = new EntityManager();
        List<GramwasiEntity> villagers = new ArrayList<>();
        villagers.add(new GramwasiEntity(100, 64, 100, VillagerProfession.SARPANCH));
        villagers.add(new GramwasiEntity(105, 64, 100, VillagerProfession.KISAN));

        assertEquals(DaakuRaidManager.RaidStatus.INACTIVE, raidManager.getStatus());
        assertFalse(raidManager.isRaidActive());

        // Start raid
        raidManager.startRaid(100, 64, 100, entityManager, villagers, new Random(101L));

        assertTrue(raidManager.isRaidActive());
        assertEquals(DaakuRaidManager.RaidStatus.RAID_ACTIVE, raidManager.getStatus());
        assertTrue(raidManager.getRemainingRaiders() >= 4, "Raid should spawn 4-6 raiders");

        // Villagers should have fled to shelter (SLEEPING schedule)
        for (GramwasiEntity v : villagers) {
            assertEquals(GramwasiEntity.ScheduleState.SLEEPING, v.getScheduleState());
        }

        // Defeat all raiders
        for (var entity : entityManager.getEntities()) {
            entity.damage(100.0f); // Kill all raiders
        }

        // Update raid manager
        raidManager.update();

        assertEquals(DaakuRaidManager.RaidStatus.VICTORY, raidManager.getStatus());
        assertTrue(raidManager.isVillageProtected(), "Village should gain protected status upon repelling raid");
    }
}
