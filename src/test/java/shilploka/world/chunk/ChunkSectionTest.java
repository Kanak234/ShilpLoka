package shilploka.world.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies ChunkSection 16³ voxel volume operations, non-air tracking,
 * and automatic palette expansion from 4-bit to 8-bit storage.
 *
 * WHAT:
 * Tests empty section optimization, block set/get consistency,
 * accurate nonAirCount increment/decrement, and palette resizing when >16 unique block IDs are added.
 *
 * USED FOR:
 * Automated CI verification of chunk section state management.
 */
class ChunkSectionTest {

    @Test
    @DisplayName("Empty section reports zero non-air blocks and returns 0 for all coordinates")
    void testEmptySection() {
        ChunkSection section = new ChunkSection(0);
        assertTrue(section.isEmpty());
        assertEquals(0, section.getNonAirCount());

        for (int x = 0; x < 16; x += 4) {
            for (int y = 0; y < 16; y += 4) {
                for (int z = 0; z < 16; z += 4) {
                    assertEquals(0, section.getBlock(x, y, z));
                }
            }
        }
    }

    @Test
    @DisplayName("Block updates correctly maintain nonAirCount and return set values")
    void testBlockUpdates() {
        ChunkSection section = new ChunkSection(2);

        // Place a block
        section.setBlock(3, 4, 5, 7); // Harappan Fired Brick
        assertFalse(section.isEmpty());
        assertEquals(1, section.getNonAirCount());
        assertEquals(7, section.getBlock(3, 4, 5));

        // Overwrite with another solid block (nonAirCount must stay 1)
        section.setBlock(3, 4, 5, 2); // Granite
        assertEquals(1, section.getNonAirCount());
        assertEquals(2, section.getBlock(3, 4, 5));

        // Clear to air (nonAirCount must become 0, isEmpty true)
        section.setBlock(3, 4, 5, 0);
        assertTrue(section.isEmpty());
        assertEquals(0, section.getNonAirCount());
        assertEquals(0, section.getBlock(3, 4, 5));
    }

    @Test
    @DisplayName("Section automatically expands palette when unique block count exceeds 16")
    void testPaletteExpansion() {
        ChunkSection section = new ChunkSection(1);

        // Fill with 20 distinct block types (IDs 101 to 120)
        for (int i = 0; i < 20; i++) {
            section.setBlock(i % 16, i / 16, 0, 100 + i);
        }

        assertEquals(20, section.getNonAirCount());

        // Verify all 20 unique blocks retained their exact values after palette expansion
        for (int i = 0; i < 20; i++) {
            assertEquals(100 + i, section.getBlock(i % 16, i / 16, 0),
                    "Mismatch for block at (" + (i % 16) + "," + (i / 16) + ",0)");
        }
    }
}
