package shilploka.world.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Save file durability and corruption resilience are essential to prevent world loss.
 * ShilpLoka must support atomic file commits, rollback backups (.old), automated zip archives,
 * and seamless recovery from unexpected process crashes.
 *
 * WHAT:
 * Tests SaveManager atomic writes, .old fallback recovery upon file corruption,
 * pre-session zip archive generation, chunk caching, and saved world directory discovery.
 *
 * USED FOR:
 * Automated CI verification of Phase P6 persistence engine.
 */
class SaveManagerTest {

    @Test
    @DisplayName("SaveManager writes and loads WorldMetadata atomically")
    void testWorldMetadataAtomicSaveAndLoad(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "TestBharat");

        WorldMetadata meta = new WorldMetadata("TestBharat", 987654321L, "survival");
        meta.setWorldTimeTicks(12000L);
        meta.setDayCount(4);
        meta.setDifficulty("kathin");

        sm.saveWorldMetadata(meta);

        // Load back
        WorldMetadata loaded = sm.loadWorldMetadata();
        assertNotNull(loaded);
        assertEquals("TestBharat", loaded.getWorldName());
        assertEquals(987654321L, loaded.getSeed());
        assertEquals("survival", loaded.getGameMode());
        assertEquals("kathin", loaded.getDifficulty());
        assertEquals(12000L, loaded.getWorldTimeTicks());
        assertEquals(4, loaded.getDayCount());

        sm.close();
    }

    @Test
    @DisplayName("SaveManager recovers from corrupt level.json by rolling back to level.json.old")
    void testCorruptLevelJsonRollback(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "RollbackWorld");

        // 1. Initial valid save
        WorldMetadata v1 = new WorldMetadata("RollbackWorld", 11111L, "creative");
        v1.setDayCount(1);
        sm.saveWorldMetadata(v1);

        // 2. Second valid save (creates level.json.old containing v1)
        WorldMetadata v2 = new WorldMetadata("RollbackWorld", 11111L, "creative");
        v2.setDayCount(2);
        sm.saveWorldMetadata(v2);

        File levelFile = new File(sm.getWorldDir(), "level.json");
        File oldFile = new File(sm.getWorldDir(), "level.json.old");
        assertTrue(levelFile.exists());
        assertTrue(oldFile.exists());

        // 3. Simulate sudden crash corruption (corrupting level.json with invalid truncated JSON)
        try (FileOutputStream fos = new FileOutputStream(levelFile)) {
            fos.write("{ \"formatVersion\": 1, \"corrupt_partial_json\": true ...".getBytes());
        }

        // 4. Attempt load: must seamlessly fall back to level.json.old without throwing exception
        WorldMetadata recovered = sm.loadWorldMetadata();
        assertNotNull(recovered, "Expected recovered metadata from .old rollback copy");
        assertEquals("RollbackWorld", recovered.getWorldName());
        assertEquals(11111L, recovered.getSeed());

        sm.close();
    }

    @Test
    @DisplayName("SaveManager writes and loads PlayerData atomically")
    void testPlayerDataSaveAndLoad(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "PlayerWorld");

        PlayerData player = new PlayerData(128.5, 72.0, -64.2, 45.0f, -15.0f, false, 3);
        player.getSurvivalStats().health = 18.0f;
        player.getSurvivalStats().bhookh = 16;
        player.getInventory().add(new PlayerData.InventoryItem(0, "shilploka:fired_brick", 64, 0));

        sm.savePlayerData(player);

        PlayerData loaded = sm.loadPlayerData();
        assertNotNull(loaded);
        assertEquals(128.5, loaded.getPosition()[0], 0.001);
        assertEquals(72.0, loaded.getPosition()[1], 0.001);
        assertEquals(-64.2, loaded.getPosition()[2], 0.001);
        assertEquals(45.0f, loaded.getRotation()[0], 0.001);
        assertEquals(-15.0f, loaded.getRotation()[1], 0.001);
        assertFalse(loaded.isFlying());
        assertEquals(3, loaded.getSelectedHotbarSlot());
        assertEquals(18.0f, loaded.getSurvivalStats().health, 0.001);
        assertEquals(16, loaded.getSurvivalStats().bhookh);
        assertEquals(1, loaded.getInventory().size());
        assertEquals("shilploka:fired_brick", loaded.getInventory().get(0).item);

        sm.close();
    }

    @Test
    @DisplayName("SaveManager creates pre-session backup zip archive with all world contents")
    void testCreateBackupArchive(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "BackupTestWorld");

        // Save metadata and a chunk
        WorldMetadata meta = new WorldMetadata("BackupTestWorld", 54321L, "creative");
        sm.saveWorldMetadata(meta);

        ChunkColumn col = new ChunkColumn(2, 3);
        col.setBlock(4, 50, 6, BlockIds.RED_SANDSTONE);
        sm.saveChunk(col);

        // Create backup
        File backupZip = sm.createBackup();
        assertNotNull(backupZip);
        assertTrue(backupZip.exists());
        assertTrue(backupZip.length() > 0);

        // Verify zip contains level.json and region/r.0.0.slr
        boolean foundLevel = false;
        boolean foundRegion = false;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupZip.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("level.json")) {
                    foundLevel = true;
                } else if (entry.getName().startsWith("region/")) {
                    foundRegion = true;
                }
                zis.closeEntry();
            }
        }

        assertTrue(foundLevel, "Backup zip must include level.json");
        assertTrue(foundRegion, "Backup zip must include region files");

        sm.close();
    }

    @Test
    @DisplayName("SaveManager discovers and lists all saved worlds sorted by last played")
    void testListSavedWorlds(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();

        // Create two worlds
        SaveManager sm1 = SaveManager.createNewWorld(savesDir, "World Alpha", 101L, "creative");
        sm1.close();

        // Small delay to ensure timestamp difference
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}

        SaveManager sm2 = SaveManager.createNewWorld(savesDir, "World Beta", 202L, "survival");
        sm2.close();

        List<WorldMetadata> list = SaveManager.listSavedWorlds(savesDir);
        assertEquals(2, list.size());
        assertEquals("World Beta", list.get(0).getWorldName(), "Most recently played world must be first");
        assertEquals("World Alpha", list.get(1).getWorldName());
    }

    @Test
    @DisplayName("SaveManager survives abrupt process crash leaving uncommitted .tmp files without data corruption")
    void testCrashKillRecoveryWithUncommittedTmpFiles(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "CrashResilienceWorld");

        // 1. Initial successful commit
        WorldMetadata committedMeta = new WorldMetadata("CrashResilienceWorld", 99999L, "creative");
        committedMeta.setDayCount(5);
        sm.saveWorldMetadata(committedMeta);

        PlayerData committedPlayer = new PlayerData(10.0, 65.0, 10.0, 0.0f, 0.0f, true, 0);
        sm.savePlayerData(committedPlayer);

        // 2. Simulate abrupt process kill (e.g. kill -9 or power loss) midway through next write
        // leaving half-written .tmp garbage files
        File tempLevelFile = new File(sm.getWorldDir(), "level.json.tmp");
        try (FileOutputStream fos = new FileOutputStream(tempLevelFile)) {
            fos.write("INCOMPLETE_CHUNK_BUFFER_CRASH_DUMP_GARBAGE".getBytes());
        }

        File tempPlayerFile = new File(new File(sm.getWorldDir(), "playerdata"), "player.json.tmp");
        try (FileOutputStream fos = new FileOutputStream(tempPlayerFile)) {
            fos.write("TRUNCATED_JSON_STREAM_KILL_9".getBytes());
        }

        // 3. Re-open SaveManager on next boot: uncommitted .tmp files are ignored,
        // while valid committed data is intact!
        SaveManager rebootSm = new SaveManager(savesDir, "CrashResilienceWorld");
        WorldMetadata recoveredMeta = rebootSm.loadWorldMetadata();
        assertNotNull(recoveredMeta);
        assertEquals("CrashResilienceWorld", recoveredMeta.getWorldName());
        assertEquals(99999L, recoveredMeta.getSeed());
        assertEquals(5, recoveredMeta.getDayCount());

        PlayerData recoveredPlayer = rebootSm.loadPlayerData();
        assertNotNull(recoveredPlayer);
        assertEquals(10.0, recoveredPlayer.getPosition()[0], 0.001);
        assertEquals(65.0, recoveredPlayer.getPosition()[1], 0.001);
        assertTrue(recoveredPlayer.isFlying());

        rebootSm.close();
        sm.close();
    }

    @Test
    @DisplayName("SaveManager saves and loads multiple chunks across different region coordinates")
    void testSaveManagerChunkOperations(@TempDir Path tempDir) throws IOException {
        File savesDir = tempDir.resolve("saves").toFile();
        SaveManager sm = new SaveManager(savesDir, "MultiRegionWorld");

        // Chunk in Region (0, 0)
        ChunkColumn c1 = new ChunkColumn(5, 7);
        c1.setBlock(2, 64, 3, BlockIds.SAL_PLANKS);
        sm.saveChunk(c1);

        // Chunk in Region (-1, 2)
        ChunkColumn c2 = new ChunkColumn(-10, 70);
        c2.setBlock(14, 20, 14, BlockIds.HIRA_ORE);
        sm.saveChunk(c2);

        // Load chunks back
        ChunkColumn loaded1 = sm.loadChunk(5, 7);
        assertNotNull(loaded1);
        assertEquals(BlockIds.SAL_PLANKS, loaded1.getBlock(2, 64, 3));

        ChunkColumn loaded2 = sm.loadChunk(-10, 70);
        assertNotNull(loaded2);
        assertEquals(BlockIds.HIRA_ORE, loaded2.getBlock(14, 20, 14));

        // Unsaved chunk returns null
        assertNull(sm.loadChunk(99, 99));

        sm.close();
    }
}
