package shilploka.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.audio.SoundCategory;
import shilploka.audio.SoundEngine;
import shilploka.event.FestivalManager;
import shilploka.event.FestivalType;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * WHY:
 * Validates Phase P16: Full Hindi & English localization, SettingsScreen options navigation,
 * accessibility controls, and cultural festival mechanics.
 */
class LocalizationSettingsTest {

    @BeforeEach
    void resetState() {
        Localization.setLanguage("en");
    }

    @Test
    @DisplayName("Localization loads English strings and returns valid values with fallback support")
    void testLocalizationEnglish() {
        assertEquals("en", Localization.getLanguage());
        assertFalse(Localization.isHindi());

        assertEquals("ShilpLoka", Localization.get("game.title"));
        assertEquals("Singleplayer", Localization.get("menu.singleplayer"));
        assertEquals("Settings & Options", Localization.get("settings.title"));

        // Fallback for missing key
        String missingKey = "some.nonexistent.key.123";
        assertEquals(missingKey, Localization.get(missingKey));
    }

    @Test
    @DisplayName("Localization switches to Devanagari Hindi and returns authentic Indian strings")
    void testLocalizationHindi() {
        Localization.setLanguage("hi");
        assertEquals("hi", Localization.getLanguage());
        assertTrue(Localization.isHindi());

        assertEquals("शिल्पलोक", Localization.get("game.title"));
        assertTrue(Localization.get("menu.singleplayer").contains("एकल खिलाड़ी"));
        assertEquals("सेटिंग्स एवं विकल्प", Localization.get("settings.title"));
        assertEquals("कमल स्वास्थ्य", Localization.get("hud.health"));
        assertEquals("रोटी भूख", Localization.get("hud.hunger"));
    }

    @Test
    @DisplayName("FestivalManager updates active festival and computes crop/illumination bonuses")
    void testFestivalManager() {
        FestivalManager manager = new FestivalManager();
        assertEquals(FestivalType.NONE, manager.getActiveFestival());
        assertEquals(1.0f, manager.getCropGrowthMultiplier());
        assertFalse(manager.isDiwaliActive());

        manager.setActiveFestival(FestivalType.DIWALI);
        assertEquals(FestivalType.DIWALI, manager.getActiveFestival());
        assertTrue(manager.isDiwaliActive());
        assertTrue(manager.getActiveFestival().hasGoldenNightIllumination());

        manager.setActiveFestival(FestivalType.HARVEST);
        assertEquals(1.5f, manager.getCropGrowthMultiplier(), 0.01f);
    }

    @Test
    @DisplayName("SettingsScreen navigates options, adjusts parameters with clamps, and updates audio/festivals")
    void testSettingsScreenNavigation() {
        SoundEngine soundEngine = new SoundEngine();
        FestivalManager festivalManager = new FestivalManager();
        SettingsScreen screen = new SettingsScreen(soundEngine, festivalManager);

        assertFalse(screen.isOpen());
        screen.toggle();
        assertTrue(screen.isOpen());

        // Row 0: Render distance (default 6)
        assertEquals(0, screen.getSelectedRow());
        assertEquals(6, screen.getRenderDistance());

        // Press Right to increase render distance (+2 -> 8)
        screen.handleKey(GLFW_KEY_RIGHT, GLFW_PRESS);
        assertEquals(8, screen.getRenderDistance());

        // Press Down to move to FOV (Row 1)
        screen.handleKey(GLFW_KEY_DOWN, GLFW_PRESS);
        assertEquals(1, screen.getSelectedRow());
        assertEquals(75.0f, screen.getFov(), 0.01f);

        // Press Left to decrease FOV (-5 -> 70)
        screen.handleKey(GLFW_KEY_LEFT, GLFW_PRESS);
        assertEquals(70.0f, screen.getFov(), 0.01f);

        // Navigate to Master Volume (Row 2)
        screen.handleKey(GLFW_KEY_DOWN, GLFW_PRESS);
        assertEquals(2, screen.getSelectedRow());
        screen.handleKey(GLFW_KEY_LEFT, GLFW_PRESS); // 1.0 -> 0.9
        assertEquals(0.9f, screen.getMasterVolume(), 0.01f);
        assertEquals(0.9f, soundEngine.getCategoryVolume(SoundCategory.MASTER), 0.01f);

        // Press Escape to close
        screen.handleKey(GLFW_KEY_ESCAPE, GLFW_PRESS);
        assertFalse(screen.isOpen());

        soundEngine.cleanup();
    }
}
