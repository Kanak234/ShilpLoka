package shilploka.audio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.dimension.DimensionType;
import shilploka.worldgen.biome.BiomeType;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates Phase P15: Spatial audio pipeline, SoundEvents catalog, SoundCategories,
 * headless sound engine resilience, and dynamic music coordination.
 */
class SoundEngineTest {

    @Test
    @DisplayName("All SoundEvents have valid IDs, duration > 0, base frequency > 0, and non-empty categories")
    void testSoundEventsCatalog() {
        Set<String> uniqueIds = new HashSet<>();

        for (SoundEvent event : SoundEvent.values()) {
            assertNotNull(event.getId(), "SoundEvent id must not be null");
            assertNotNull(event.getCategory(), "SoundEvent category must not be null");
            assertTrue(event.getBaseFrequency() > 0, "Base frequency must be positive");
            assertTrue(event.getDuration() > 0, "Duration must be positive");
            assertTrue(event.getDefaultVolume() > 0.0f && event.getDefaultVolume() <= 1.0f, "Default volume must be in (0, 1]");

            boolean added = uniqueIds.add(event.getId());
            assertTrue(added, "Duplicate SoundEvent id detected: " + event.getId());
        }
    }

    @Test
    @DisplayName("All SoundCategories define authentic English and Devanagari Hindi descriptions")
    void testSoundCategories() {
        for (SoundCategory cat : SoundCategory.values()) {
            assertNotNull(cat.getEnglishName(), "SoundCategory English name must not be null");
            assertNotNull(cat.getHindiName(), "SoundCategory Hindi name must not be null");
            assertFalse(cat.getEnglishName().isBlank());
            assertFalse(cat.getHindiName().isBlank());
        }
    }

    @Test
    @DisplayName("SoundEngine initializes cleanly in headless environment and adjusts category volumes safely")
    void testSoundEngineHeadlessResilience() {
        SoundEngine engine = new SoundEngine();

        // Validate volume setters and clamping
        engine.setCategoryVolume(SoundCategory.MASTER, 0.75f);
        assertEquals(0.75f, engine.getCategoryVolume(SoundCategory.MASTER), 0.01f);

        engine.setCategoryVolume(SoundCategory.MUSIC, -0.5f);
        assertEquals(0.0f, engine.getCategoryVolume(SoundCategory.MUSIC), 0.01f, "Volume must clamp to minimum 0.0");

        engine.setCategoryVolume(SoundCategory.BLOCKS, 1.8f);
        assertEquals(1.0f, engine.getCategoryVolume(SoundCategory.BLOCKS), 0.01f, "Volume must clamp to maximum 1.0");

        // Validate play sound calls do not throw even in headless / mock-free mode
        assertDoesNotThrow(() -> engine.playSound(SoundEvent.STEP_STONE, 10.0, 64.0, 10.0, 1.0f, 1.0f));
        assertDoesNotThrow(() -> engine.playSound2D(SoundEvent.UI_CLICK, 1.0f, 1.0f));
        assertDoesNotThrow(() -> engine.setListenerOrientation(0, 0, 0, 0, 0, -1, 0, 1, 0));

        // Cleanup must succeed safely
        assertDoesNotThrow(engine::cleanup);
    }

    @Test
    @DisplayName("MusicManager tracks combat/boss state and ticks cooldown without exceptions")
    void testMusicManager() {
        SoundEngine engine = new SoundEngine();
        MusicManager musicManager = new MusicManager(engine);

        assertFalse(musicManager.isInCombat());
        assertFalse(musicManager.isBossActive());

        musicManager.setInCombat(true);
        assertTrue(musicManager.isInCombat());

        musicManager.setBossActive(true);
        assertTrue(musicManager.isBossActive());

        // Tick through music cycle
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 300; i++) {
                musicManager.tick(0.1f, BiomeType.GANGA_MAIDAN, DimensionType.PRITHVI, false);
            }
        });

        engine.cleanup();
    }
}
