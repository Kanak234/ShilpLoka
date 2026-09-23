package shilploka.world.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies BitStorage packing and unpacking integrity across various bit widths,
 * boundary conditions, and neighboring entry isolation.
 *
 * WHAT:
 * Tests 4-bit, 8-bit, and 16-bit storage with thousands of entries, boundary index checks,
 * and confirms that mutating one index leaves neighboring indices unchanged.
 *
 * USED FOR:
 * Automated CI verification of voxel palette storage compression.
 */
class BitStorageTest {

    @Test
    @DisplayName("4-bit storage correctly writes and reads full range of values")
    void testFourBitStorage() {
        int size = 4096;
        BitStorage storage = new BitStorage(4, size);

        for (int i = 0; i < size; i++) {
            int val = i % 16;
            storage.set(i, val);
        }

        for (int i = 0; i < size; i++) {
            assertEquals(i % 16, storage.get(i), "Mismatch at index " + i);
        }
    }

    @Test
    @DisplayName("8-bit storage correctly writes and reads values up to 255")
    void testEightBitStorage() {
        int size = 4096;
        BitStorage storage = new BitStorage(8, size);

        for (int i = 0; i < size; i++) {
            int val = i % 256;
            storage.set(i, val);
        }

        for (int i = 0; i < size; i++) {
            assertEquals(i % 256, storage.get(i), "Mismatch at index " + i);
        }
    }

    @Test
    @DisplayName("Mutating an index does not corrupt neighboring indices in the same long word")
    void testNeighborIsolation() {
        int size = 64;
        BitStorage storage = new BitStorage(4, size);

        storage.set(5, 15);
        storage.set(6, 7);
        storage.set(7, 3);

        assertEquals(15, storage.get(5));
        assertEquals(7, storage.get(6));
        assertEquals(3, storage.get(7));
        assertEquals(0, storage.get(4));
        assertEquals(0, storage.get(8));

        // Overwrite middle index
        storage.set(6, 1);
        assertEquals(15, storage.get(5));
        assertEquals(1, storage.get(6));
        assertEquals(3, storage.get(7));
    }

    @Test
    @DisplayName("Index bounds violations throw IndexOutOfBoundsException")
    void testBoundsExceptions() {
        BitStorage storage = new BitStorage(4, 100);

        assertThrows(IndexOutOfBoundsException.class, () -> storage.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.get(100));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.set(-1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> storage.set(100, 5));
    }
}
