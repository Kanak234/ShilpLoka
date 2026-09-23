package shilploka.world.chunk;

/**
 * WHY:
 * Storing raw 32-bit integer block IDs for 4,096 voxels in a 16³ chunk section consumes
 * 16,384 bytes. Most chunk sections contain only 2 to 8 unique block types (e.g. air,
 * stone, granite). Packing palette indices into 4-bit or 8-bit bitsets reduces memory
 * by 75% to 87.5%, keeping heap usage well under the strict 2.0 GB budget.
 *
 * WHAT:
 * High-performance bit-packed storage array that packs values of width bitsPerEntry (1-16)
 * into a continuous primitive long[] array.
 *
 * USED FOR:
 * Utilized by ChunkSection to store voxel palette indices.
 *
 * WHERE NEXT:
 * ChunkSection calls get() and set() during block lookups and meshing loops.
 */
public class BitStorage {

    private final int bitsPerEntry;
    private final int size;
    private final long maxEntryValue;
    private final long[] data;

    /**
     * WHY:
     * Constructs a bit-packed array for a fixed count of elements with specified bit-width.
     *
     * WHAT:
     * Calculates the required long[] capacity and allocates primitive array memory.
     *
     * USED FOR:
     * Initialized when creating or resizing a ChunkSection palette container.
     */
    public BitStorage(int bitsPerEntry, int size) {
        if (bitsPerEntry < 1 || bitsPerEntry > 32) {
            throw new IllegalArgumentException("bitsPerEntry must be between 1 and 32, received: " + bitsPerEntry);
        }
        this.bitsPerEntry = bitsPerEntry;
        this.size = size;
        this.maxEntryValue = (1L << bitsPerEntry) - 1L;

        int entriesPerLong = 64 / bitsPerEntry;
        int longCount = (size + entriesPerLong - 1) / entriesPerLong;
        this.data = new long[longCount];
    }

    /**
     * WHY:
     * Constructs a BitStorage populated with pre-existing raw long array data.
     *
     * WHAT:
     * Used during region file deserialization to restore bit-packed voxel indices.
     */
    public BitStorage(int bitsPerEntry, int size, long[] initialData) {
        this(bitsPerEntry, size);
        if (initialData != null) {
            System.arraycopy(initialData, 0, this.data, 0, Math.min(this.data.length, initialData.length));
        }
    }

    /**
     * WHY:
     * Retrieves the bit-packed value at the specified linear index.
     *
     * WHAT:
     * Computes the long[] bucket and bit offset, masks the bits, and returns the integer value.
     *
     * USED FOR:
     * Querying block palette indices in getBlock() and during greedy meshing.
     */
    public int get(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + this.size);
        }

        int entriesPerLong = 64 / this.bitsPerEntry;
        int longIndex = index / entriesPerLong;
        int bitOffset = (index % entriesPerLong) * this.bitsPerEntry;

        return (int) ((this.data[longIndex] >>> bitOffset) & this.maxEntryValue);
    }

    /**
     * WHY:
     * Stores a value at the specified linear index without altering adjacent elements.
     *
     * WHAT:
     * Clears the target bitfield in data[longIndex] and bitwise-ORs the new shifted value.
     *
     * USED FOR:
     * Modifying block states when placing or breaking voxels.
     */
    public void set(int index, int value) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + this.size);
        }

        long val = value & this.maxEntryValue;
        int entriesPerLong = 64 / this.bitsPerEntry;
        int longIndex = index / entriesPerLong;
        int bitOffset = (index % entriesPerLong) * this.bitsPerEntry;

        long mask = ~(this.maxEntryValue << bitOffset);
        this.data[longIndex] = (this.data[longIndex] & mask) | (val << bitOffset);
    }

    public int getBitsPerEntry() {
        return bitsPerEntry;
    }

    public int getSize() {
        return size;
    }

    public long[] getRawData() {
        return data;
    }
}
