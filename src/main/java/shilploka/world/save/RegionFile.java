package shilploka.world.save;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.BitSet;

/**
 * WHY:
 * Storing thousands of individual chunk files on modern operating systems exhausts OS inodes
 * and degrades filesystem performance. ShilpLoka bundles a 32x32 grid of chunk columns
 * (512x512 block footprint) into a single optimized region container (.slr).
 *
 * WHAT:
 * High-performance binary region file container featuring:
 * - 4,096-byte (4 KB) sector-aligned storage
 * - Sector 0 (0..4095): Location / Offset table (1,024 4-byte entries: 3 bytes sector offset, 1 byte sector count)
 * - Sector 1 (4096..8191): Timestamp table (1,024 4-byte Unix epoch seconds entries)
 * - Sectors 2+: Compressed chunk payloads (length prefix, compression byte, Deflater payload, sector padding)
 * - Dynamic BitSet sector allocation with free-list recycling
 * - Synchronous physical disk commits via FileChannel.force(true)
 *
 * USED FOR:
 * Managed by SaveManager to stream chunks in and out from disk.
 *
 * WHERE NEXT:
 * ChunkSerializer passes compressed chunk byte arrays to writeChunk() and receives
 * raw payloads from readChunk().
 */
public class RegionFile implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionFile.class);

    public static final int SECTOR_BYTES = 4096;
    public static final int ENTRIES_PER_REGION = 1024; // 32x32 chunks
    public static final int HEADER_BYTES = SECTOR_BYTES * 2; // 8,192 bytes (Sectors 0 & 1)
    public static final byte COMPRESSION_ZLIB = 1;

    private final File file;
    private RandomAccessFile raf;
    private FileChannel channel;

    private final int[] offsets = new int[ENTRIES_PER_REGION];
    private final int[] timestamps = new int[ENTRIES_PER_REGION];
    private final BitSet sectorFree = new BitSet();

    /**
     * WHY:
     * Opens or initializes a ShilpLoka Region (.slr) file.
     *
     * @param file Target file path (e.g. saves/world/region/r.0.0.slr).
     * @throws IOException If file access fails.
     */
    public RegionFile(File file) throws IOException {
        this.file = file;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        this.raf = new RandomAccessFile(file, "rw");
        this.channel = raf.getChannel();

        initHeaderAndSectors();
    }

    /**
     * Reads header tables and establishes free sector allocations.
     */
    private void initHeaderAndSectors() throws IOException {
        long fileLength = raf.length();

        if (fileLength < HEADER_BYTES) {
            // Allocate initial 8 KB header (Sectors 0 & 1)
            raf.setLength(HEADER_BYTES);
            raf.seek(0);
            byte[] emptyHeader = new byte[HEADER_BYTES];
            raf.write(emptyHeader);
            channel.force(true);

            // Sectors 0 and 1 reserved for headers
            sectorFree.set(0, false);
            sectorFree.set(1, false);
        } else {
            // Read Location Table (Sector 0: 1024 ints)
            raf.seek(0);
            ByteBuffer buf = ByteBuffer.allocate(HEADER_BYTES);
            while (buf.hasRemaining()) {
                if (channel.read(buf) == -1) break;
            }
            buf.flip();

            for (int i = 0; i < ENTRIES_PER_REGION; i++) {
                offsets[i] = buf.getInt();
            }

            // Read Timestamp Table (Sector 1: 1024 ints)
            for (int i = 0; i < ENTRIES_PER_REGION; i++) {
                timestamps[i] = buf.getInt();
            }

            // Determine total sectors currently in file
            int totalSectors = (int) ((fileLength + SECTOR_BYTES - 1) / SECTOR_BYTES);
            // Default all sectors to free
            sectorFree.set(0, totalSectors, true);

            // Reserve Sectors 0 & 1 for headers
            sectorFree.set(0, false);
            sectorFree.set(1, false);

            // Mark occupied sectors from existing chunk entries
            for (int i = 0; i < ENTRIES_PER_REGION; i++) {
                int entry = offsets[i];
                if (entry != 0) {
                    int sectorOffset = entry >>> 8;
                    int sectorCount = entry & 0xFF;
                    for (int s = 0; s < sectorCount; s++) {
                        sectorFree.set(sectorOffset + s, false);
                    }
                }
            }
        }
    }

    /**
     * Translates local chunk coordinates (0..31, 0..31) into table index (0..1023).
     */
    public static int getChunkIndex(int localX, int localZ) {
        return (localX & 31) + (localZ & 31) * 32;
    }

    /**
     * WHY:
     * Reads the compressed binary payload for a chunk column from disk.
     *
     * @param localX Local chunk X coordinate (0 to 31).
     * @param localZ Local chunk Z coordinate (0 to 31).
     * @return Raw compressed payload bytes, or null if ungenerated/unallocated.
     * @throws IOException If disk read fails.
     */
    public synchronized byte[] readChunk(int localX, int localZ) throws IOException {
        int index = getChunkIndex(localX, localZ);
        int entry = offsets[index];

        if (entry == 0) {
            return null; // Chunk not yet generated or saved
        }

        int sectorOffset = entry >>> 8;
        int sectorCount = entry & 0xFF;

        if (sectorOffset < 2) {
            LOGGER.warn("Corrupt sector offset {} in region file {}", sectorOffset, file.getName());
            return null;
        }

        long filePos = (long) sectorOffset * SECTOR_BYTES;
        if (filePos + 5 > raf.length()) {
            LOGGER.warn("Chunk offset exceeds region file bounds in {}", file.getName());
            return null;
        }

        raf.seek(filePos);
        int totalPayloadLen = raf.readInt(); // Total length of payload including compression byte
        byte compressionType = raf.readByte();

        if (totalPayloadLen <= 1 || compressionType != COMPRESSION_ZLIB) {
            LOGGER.warn("Invalid chunk payload length ({}) or compression ({})", totalPayloadLen, compressionType);
            return null;
        }

        byte[] compressedData = new byte[totalPayloadLen - 1];
        raf.readFully(compressedData);
        return compressedData;
    }

    /**
     * WHY:
     * Writes compressed chunk payload to sector-aligned storage and flushes tables to disk.
     *
     * @param localX         Local chunk X coordinate (0 to 31).
     * @param localZ         Local chunk Z coordinate (0 to 31).
     * @param compressedData Deflater-compressed chunk payload.
     * @throws IOException If disk write fails.
     */
    public synchronized void writeChunk(int localX, int localZ, byte[] compressedData) throws IOException {
        int index = getChunkIndex(localX, localZ);
        int totalBytes = 4 + 1 + compressedData.length; // 4 bytes length + 1 byte compression + payload
        int sectorsNeeded = (totalBytes + SECTOR_BYTES - 1) / SECTOR_BYTES;

        int oldEntry = offsets[index];
        int oldSectorOffset = oldEntry >>> 8;
        int oldSectorCount = oldEntry & 0xFF;

        int targetSectorOffset;

        // Check if existing allocation can be reused
        if (oldEntry != 0 && oldSectorCount >= sectorsNeeded) {
            targetSectorOffset = oldSectorOffset;
        } else {
            // Free previously occupied sectors if relocating
            if (oldEntry != 0) {
                for (int s = 0; s < oldSectorCount; s++) {
                    sectorFree.set(oldSectorOffset + s, true);
                }
            }

            // Find or allocate contiguous run of free sectors
            targetSectorOffset = findContiguousFreeSectors(sectorsNeeded);
            for (int s = 0; s < sectorsNeeded; s++) {
                sectorFree.set(targetSectorOffset + s, false);
            }
        }

        // Write chunk payload at target sector position
        long filePos = (long) targetSectorOffset * SECTOR_BYTES;
        raf.seek(filePos);
        raf.writeInt(compressedData.length + 1);
        raf.writeByte(COMPRESSION_ZLIB);
        raf.write(compressedData);

        // Zero-fill sector padding
        int paddingBytes = (sectorsNeeded * SECTOR_BYTES) - totalBytes;
        if (paddingBytes > 0) {
            raf.write(new byte[paddingBytes]);
        }

        // Update Location Table (Sector 0)
        int newEntry = (targetSectorOffset << 8) | (sectorsNeeded & 0xFF);
        offsets[index] = newEntry;
        raf.seek((long) index * 4);
        raf.writeInt(newEntry);

        // Update Timestamp Table (Sector 1)
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        timestamps[index] = timestamp;
        raf.seek(SECTOR_BYTES + (long) index * 4);
        raf.writeInt(timestamp);

        // Force physical disk synchronization
        channel.force(true);
    }

    /**
     * Searches for a contiguous block of free sectors, or expands the file end.
     */
    private int findContiguousFreeSectors(int count) throws IOException {
        int runStart = -1;
        int runLength = 0;

        // Search starting after the 2 header sectors
        for (int i = 2; i < sectorFree.size(); i++) {
            if (sectorFree.get(i)) {
                if (runLength == 0) runStart = i;
                runLength++;
                if (runLength == count) {
                    return runStart;
                }
            } else {
                runLength = 0;
            }
        }

        // If no internal gap fits, append to the end of the file
        int appendSector = (int) ((raf.length() + SECTOR_BYTES - 1) / SECTOR_BYTES);
        if (appendSector < 2) appendSector = 2;
        return appendSector;
    }

    /**
     * Flushes and closes file descriptors.
     */
    @Override
    public synchronized void close() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.force(true);
            channel.close();
        }
        if (raf != null) {
            raf.close();
        }
    }

    public File getFile() {
        return file;
    }
}
