package shilploka.world.chunk;

import java.util.Arrays;

/**
 * WHY:
 * A 16³ chunk section (4,096 blocks) is the fundamental atomic volume of the world.
 * Chunk sections allow granular frustum culling, localized greedy meshing, and
 * palette-compressed storage, reducing memory consumption by over 75%.
 *
 * WHAT:
 * Encapsulates a 16x16x16 volume of blocks using a dynamic palette and bit-packed
 * BitStorage. Automatically expands from 4-bit to 8-bit storage when more unique
 * block types are introduced, and maintains an exact count of non-air blocks.
 *
 * USED FOR:
 * Grouped vertically (20 sections) inside ChunkColumn to represent world height 0 to 320.
 *
 * WHERE NEXT:
 * Queried by GreedyMesher to build optimized polygon quads for visible sections.
 */
public class ChunkSection {

    public static final int SECTION_SIZE = 16;
    public static final int TOTAL_BLOCKS = SECTION_SIZE * SECTION_SIZE * SECTION_SIZE; // 4096

    private final int sectionY; // Vertical index (0 to 19 for height 0-320)
    private int nonAirCount = 0;

    // Dynamic palette
    private int[] palette;
    private int paletteSize;
    private BitStorage storage;

    // 4-bit Sky Light (upper nibble: 0-15) and 4-bit Block Light (lower nibble: 0-15)
    private final byte[] light;

    /**
     * WHY:
     * Initializes an empty 16³ chunk section at the designated section elevation.
     *
     * WHAT:
     * Pre-allocates a 4-bit BitStorage (16 palette slots) with air (ID 0) at index 0,
     * and a 4,096-byte array storing packed 4-bit Sky and Block light values.
     *
     * USED FOR:
     * Instantiated by ChunkColumn when generating world terrain.
     */
    public ChunkSection(int sectionY) {
        this.sectionY = sectionY;
        this.palette = new int[16];
        this.palette[0] = 0; // Index 0 is always Air
        this.paletteSize = 1;
        this.storage = new BitStorage(4, TOTAL_BLOCKS);
        this.light = new byte[TOTAL_BLOCKS];
    }

    /**
     * WHY:
     * Constructs a ChunkSection with restored persistence data from a .slr region file.
     */
    public ChunkSection(int sectionY, int nonAirCount, int[] palette, int paletteSize, BitStorage storage, byte[] light) {
        this.sectionY = sectionY;
        this.nonAirCount = nonAirCount;
        this.palette = palette;
        this.paletteSize = paletteSize;
        this.storage = storage;
        this.light = (light != null) ? light : new byte[TOTAL_BLOCKS];
    }

    public int[] getPalette() {
        return palette;
    }

    public int getPaletteSize() {
        return paletteSize;
    }

    public BitStorage getStorage() {
        return storage;
    }

    /**
     * Fast retrieval of 4-bit Sky Light level (0-15) from upper nibble.
     */
    public int getSkyLight(int x, int y, int z) {
        return (this.light[getIndex(x, y, z)] >> 4) & 0x0F;
    }

    /**
     * Sets 4-bit Sky Light level (0-15) into upper nibble.
     */
    public void setSkyLight(int x, int y, int z, int value) {
        int index = getIndex(x, y, z);
        this.light[index] = (byte) ((this.light[index] & 0x0F) | ((value & 0x0F) << 4));
    }

    /**
     * Fast retrieval of 4-bit Block Light level (0-15) from lower nibble.
     */
    public int getBlockLight(int x, int y, int z) {
        return this.light[getIndex(x, y, z)] & 0x0F;
    }

    /**
     * Sets 4-bit Block Light level (0-15) into lower nibble.
     */
    public void setBlockLight(int x, int y, int z, int value) {
        int index = getIndex(x, y, z);
        this.light[index] = (byte) ((this.light[index] & 0xF0) | (value & 0x0F));
    }

    /**
     * Returns the raw 4,096-byte light storage array for serialization or bulk processing.
     */
    public byte[] getRawLightArray() {
        return this.light;
    }

    /**
     * WHY:
     * Computes the 1D linear array index for 3D coordinates within a 16³ section.
     *
     * WHAT:
     * Formats index as (y << 8) | (z << 4) | x.
     */
    public static int getIndex(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    /**
     * WHY:
     * Fast retrieval of the global block ID at local section coordinates.
     *
     * WHAT:
     * Queries BitStorage for the palette index, then looks up the global block ID.
     */
    public int getBlock(int x, int y, int z) {
        if (this.nonAirCount == 0) return 0; // Instant return for empty sections

        int index = getIndex(x, y, z);
        int paletteIndex = this.storage.get(index);
        return this.palette[paletteIndex];
    }

    /**
     * WHY:
     * Sets the block state at local coordinates, managing palette expansion and non-air counters.
     *
     * WHAT:
     * Locates or registers the block ID in the palette, updates the BitStorage index,
     * adjusts nonAirCount, and expands the storage bit-width if capacity is exceeded.
     */
    public void setBlock(int x, int y, int z, int blockId) {
        int index = getIndex(x, y, z);
        int oldPaletteIndex = this.storage.get(index);
        int oldBlockId = this.palette[oldPaletteIndex];

        if (oldBlockId == blockId) return;

        // Update non-air voxel count
        if (oldBlockId == 0 && blockId != 0) {
            this.nonAirCount++;
        } else if (oldBlockId != 0 && blockId == 0) {
            this.nonAirCount--;
        }

        int paletteIndex = getOrCreatePaletteIndex(blockId);
        this.storage.set(index, paletteIndex);
    }

    /**
     * WHY:
     * Finds the palette index for a block ID, expanding the palette and bit-storage if needed.
     */
    private int getOrCreatePaletteIndex(int blockId) {
        // Linear scan through local palette
        for (int i = 0; i < this.paletteSize; i++) {
            if (this.palette[i] == blockId) {
                return i;
            }
        }

        // Needs expansion
        if (this.paletteSize >= this.palette.length) {
            expandStorage();
        }

        int newIndex = this.paletteSize++;
        this.palette[newIndex] = blockId;
        return newIndex;
    }

    /**
     * WHY:
     * Expands the bit-width of the BitStorage container when unique block count exceeds capacity.
     *
     * WHAT:
     * Resizes palette array, creates a new BitStorage with larger bit-width (e.g. 4 -> 8 bits),
     * and migrates existing voxel data.
     */
    private void expandStorage() {
        int newBits = (this.storage.getBitsPerEntry() <= 4) ? 8 : 16;
        int newCapacity = 1 << newBits;

        this.palette = Arrays.copyOf(this.palette, newCapacity);
        BitStorage newStorage = new BitStorage(newBits, TOTAL_BLOCKS);

        // Migrate all 4,096 voxel entries
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            newStorage.set(i, this.storage.get(i));
        }

        this.storage = newStorage;
    }

    /**
     * Fast check: returns true if section contains zero solid blocks (pure air).
     * Empty sections require zero polygon meshing and issue zero draw calls.
     */
    public boolean isEmpty() {
        return this.nonAirCount == 0;
    }

    public int getSectionY() {
        return sectionY;
    }

    public int getNonAirCount() {
        return nonAirCount;
    }
}
