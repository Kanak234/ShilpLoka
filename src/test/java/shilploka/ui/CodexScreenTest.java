package shilploka.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

/**
 * WHY:
 * Validates that the Itihaas Granth (Codex Screen) functions smoothly,
 * contains all 5 comprehensive lore chapters, and navigates correctly without UI crashes.
 *
 * WHAT:
 * Tests codex state, toggling, entries, categories, and key handling.
 */
class CodexScreenTest {

    @Test
    @DisplayName("CodexScreen initializes closed and contains 5 cultural categories")
    void testInitialization() {
        CodexScreen codex = new CodexScreen();
        assertFalse(codex.isOpen(), "Codex should start closed");
        assertEquals(5, codex.getCategories().size(), "Codex must have 5 major chapters");
        assertFalse(codex.getAllEntries().isEmpty(), "Codex entries must be loaded");
    }

    @Test
    @DisplayName("CodexScreen toggles open and closed state")
    void testToggle() {
        CodexScreen codex = new CodexScreen();
        codex.toggle();
        assertTrue(codex.isOpen());
        codex.toggle();
        assertFalse(codex.isOpen());
    }

    @Test
    @DisplayName("CodexScreen navigates categories and entries via key presses")
    void testNavigation() {
        CodexScreen codex = new CodexScreen();
        codex.setOpen(true);

        CodexScreen.CodexEntry initialEntry = codex.getActiveEntry();
        assertNotNull(initialEntry, "Initial entry must not be null");

        // Next entry (DOWN / S key)
        codex.handleKeyPress(GLFW_KEY_DOWN);
        CodexScreen.CodexEntry nextEntry = codex.getActiveEntry();
        assertNotNull(nextEntry);
        assertNotEquals(initialEntry.id(), nextEntry.id(), "Entry index should advance");

        // Next category (RIGHT / D key)
        codex.handleKeyPress(GLFW_KEY_RIGHT);
        CodexScreen.CodexEntry catEntry = codex.getActiveEntry();
        assertNotNull(catEntry);
        assertNotEquals(initialEntry.category(), catEntry.category(), "Category should advance");

        // Escape closes
        codex.handleKeyPress(GLFW_KEY_ESCAPE);
        assertFalse(codex.isOpen(), "ESCAPE key should close the codex");
    }

    @Test
    @DisplayName("All codex entries contain non-empty title, era, and lore lines")
    void testEntriesIntegrity() {
        CodexScreen codex = new CodexScreen();
        for (CodexScreen.CodexEntry entry : codex.getAllEntries()) {
            assertNotNull(entry.id(), "Entry id cannot be null");
            assertNotNull(entry.title(), "Entry title cannot be null");
            assertNotNull(entry.era(), "Entry era cannot be null");
            assertFalse(entry.lines().isEmpty(), "Entry must contain informative lines");
        }
    }
}
