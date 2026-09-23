package shilploka.world.save;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.world.chunk.BitStorage;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkSection;

import java.io.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * WHY:
 * Chunks must be serialized into compact, compressed binary representations for storage
 * in .slr region files on disk. Efficient bit-exact serialization preserves all voxel
 * palette indices and cellular lighting states (Dhoop & Prakash) across game sessions.
 *
 * WHAT:
 * High-performance ZLIB-compressed binary serializer for ChunkColumn instances.
 * - Magic number 0x534C5231 ("SLR1") header
 * - Chunk column coordinates (cx, cz)
 * - 20 vertical sections: palette definitions, bit-packed storage words, and 4096-byte light nibble arrays
 * - Stream compression via java.util.zip.Deflater / Inflater
 *
 * USED FOR:
 * Called by SaveManager to convert ChunkColumn to byte[] payloads and reconstruct them on load.
 *
 * WHERE NEXT:
 * Compressed bytes are stored directly into sector-aligned slots in RegionFile (.slr).
 */
public final class ChunkSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkSerializer.class);

    public static final int MAGIC = 0x534C5231; // "SLR1" (ShilpLoka Region v1)
    public static final byte VERSION = 1;

    private ChunkSerializer() {
        // Static utility class
    }

    /**
     * WHY:
     * Compresses and serializes an in-memory ChunkColumn into a ZLIB-compressed byte array.
     *
     * WHAT:
     * Writes magic number, version, chunk coordinates, section palettes, bit storage words,
     * and light nibble arrays into a DeflaterOutputStream.
     *
     * @param column Active chunk column to serialize.
     * @return ZLIB-compressed binary byte array.
     * @throws IOException If streaming fails.
     */
    public static byte[] serialize(ChunkColumn column) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);

        try (DeflaterOutputStream defOut = new DeflaterOutputStream(baos, deflater);
             DataOutputStream out = new DataOutputStream(defOut)) {

            // 1. Header
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            out.writeInt(column.getChunkX());
            out.writeInt(column.getChunkZ());
            out.writeByte(ChunkColumn.SECTION_COUNT); // 20 sections

            // 2. Vertical sections (0 to 19)
            for (int sy = 0; sy < ChunkColumn.SECTION_COUNT; sy++) {
                ChunkSection section = column.getSection(sy);
                out.writeByte(sy);

                if (section == null) {
                    out.writeBoolean(false); // No blocks
                    out.writeBoolean(false); // No light
                    continue;
                }

                // Check block content
                boolean hasBlocks = !section.isEmpty();
                out.writeBoolean(hasBlocks);

                if (hasBlocks) {
                    out.writeInt(section.getNonAirCount());
                    int paletteSize = section.getPaletteSize();
                    out.writeInt(paletteSize);
                    int[] palette = section.getPalette();
                    for (int p = 0; p < paletteSize; p++) {
                        out.writeInt(palette[p]);
                    }

                    BitStorage storage = section.getStorage();
                    out.writeByte(storage.getBitsPerEntry());
                    out.writeInt(storage.getSize());
                    long[] rawData = storage.getRawData();
                    out.writeInt(rawData.length);
                    for (long word : rawData) {
                        out.writeLong(word);
                    }
                }

                // Check lighting content
                byte[] light = section.getRawLightArray();
                boolean hasLight = false;
                if (light != null) {
                    for (byte b : light) {
                        if (b != 0) {
                            hasLight = true;
                            break;
                        }
                    }
                }

                out.writeBoolean(hasLight);
                if (hasLight) {
                    out.write(light, 0, ChunkSection.TOTAL_BLOCKS);
                }
            }

            defOut.finish();
        } finally {
            deflater.end();
        }

        return baos.toByteArray();
    }

    /**
     * WHY:
     * Reconstructs an in-memory ChunkColumn from a ZLIB-compressed byte array read from disk.
     *
     * WHAT:
     * Inflates payload, verifies magic number and coordinates, reconstructs dynamic palettes,
     * instantiates BitStorage instances, and restores cellular light nibbles.
     *
     * @param expectedCx Expected chunk grid X coordinate.
     * @param expectedCz Expected chunk grid Z coordinate.
     * @param compressed ZLIB-compressed binary byte array.
     * @return Fully populated ChunkColumn, or null if corrupt.
     * @throws IOException If decompression or parsing fails.
     */
    public static ChunkColumn deserialize(int expectedCx, int expectedCz, byte[] compressed) throws IOException {
        if (compressed == null || compressed.length < 9) {
            throw new IOException("Cannot deserialize null or truncated chunk payload");
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        Inflater inflater = new Inflater();

        try (InflaterInputStream infIn = new InflaterInputStream(bais, inflater);
             DataInputStream in = new DataInputStream(infIn)) {

            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Corrupt chunk data: invalid magic number 0x" + Integer.toHexString(magic));
            }

            byte version = in.readByte();
            if (version != VERSION) {
                throw new IOException("Unsupported chunk format version: " + version);
            }

            int cx = in.readInt();
            int cz = in.readInt();

            if (cx != expectedCx || cz != expectedCz) {
                LOGGER.warn("Chunk coordinate mismatch: expected ({}, {}), payload has ({}, {})",
                        expectedCx, expectedCz, cx, cz);
            }

            int sectionCount = in.readByte() & 0xFF;
            ChunkColumn column = new ChunkColumn(cx, cz);

            for (int i = 0; i < sectionCount; i++) {
                int sy = in.readByte() & 0xFF;
                boolean hasBlocks = in.readBoolean();

                int nonAirCount = 0;
                int paletteSize = 1;
                int[] palette;
                BitStorage storage;

                if (hasBlocks) {
                    nonAirCount = in.readInt();
                    paletteSize = in.readInt();
                    int bitsPerEntry = 4;
                    // Read palette entries
                    int[] loadedPalette = new int[paletteSize];
                    for (int p = 0; p < paletteSize; p++) {
                        loadedPalette[p] = in.readInt();
                    }

                    bitsPerEntry = in.readByte() & 0xFF;
                    int storageSize = in.readInt();
                    int dataLen = in.readInt();
                    long[] rawData = new long[dataLen];
                    for (int d = 0; d < dataLen; d++) {
                        rawData[d] = in.readLong();
                    }

                    // Pre-size palette array to prevent premature storage expansion
                    int capacity = Math.max(16, 1 << bitsPerEntry);
                    if (capacity < paletteSize) {
                        capacity = paletteSize;
                    }
                    palette = new int[capacity];
                    System.arraycopy(loadedPalette, 0, palette, 0, paletteSize);

                    storage = new BitStorage(bitsPerEntry, storageSize, rawData);
                } else {
                    palette = new int[16];
                    palette[0] = 0;
                    storage = new BitStorage(4, ChunkSection.TOTAL_BLOCKS);
                }

                boolean hasLight = in.readBoolean();
                byte[] light = new byte[ChunkSection.TOTAL_BLOCKS];
                if (hasLight) {
                    in.readFully(light);
                }

                ChunkSection section = new ChunkSection(sy, nonAirCount, palette, paletteSize, storage, light);
                column.setSection(sy, section);
            }

            column.setDirty(false);
            return column;
        } finally {
            inflater.end();
        }
    }
}
