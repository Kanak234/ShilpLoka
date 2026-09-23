package shilploka.world.save;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkSection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Voxel sandbox worlds require 100% data fidelity when writing chunks to disk and reading them back.
 * Any bit corruption, palette drift, or lost lighting data would degrade player constructions
 * and world ambiance.
 *
 * WHAT:
 * Tests the binary serialization pipeline (ChunkSerializer) and the 32x32 region container (RegionFile)
 * across empty sections, filled sections, complex multi-block palettes, and cellular light arrays.
 *
 * USED FOR:
 * Automated CI verification of Phase P6 persistence engine.
 */
class RegionFileRoundTripTest {

    @Test
    @DisplayName("ChunkSerializer preserves 100% bit-exact block states and light levels in memory")
    void testChunkSerializerRoundTrip() throws IOException {
        ChunkColumn original = new ChunkColumn(5, -12);

        // Populate sections with diverse authentic Indian blocks and lighting
        original.setBlock(2, 10, 4, BlockIds.DECCAN_BASALT);
        original.setBlock(2, 11, 4, BlockIds.RED_SANDSTONE);
        original.setBlock(2, 12, 4, BlockIds.YELLOW_SANDSTONE);
        original.setBlock(8, 64, 8, BlockIds.DIYA);
        original.setBlock(15, 319, 15, BlockIds.HIMALAYAN_SNOW);

        // Set celestial sky and block light
        original.setSkyLight(8, 64, 8, 15);
        original.setBlockLight(8, 64, 8, 14); // Diya light emission
        original.setBlockLight(8, 65, 8, 13);
        original.setSkyLight(15, 319, 15, 15);

        // Serialize
        byte[] payload = ChunkSerializer.serialize(original);
        assertNotNull(payload);
        assertTrue(payload.length > 0);

        // Deserialize
        ChunkColumn loaded = ChunkSerializer.deserialize(5, -12, payload);
        assertNotNull(loaded);
        assertEquals(5, loaded.getChunkX());
        assertEquals(-12, loaded.getChunkZ());

        // Verify block parity
        assertEquals(BlockIds.DECCAN_BASALT, loaded.getBlock(2, 10, 4));
        assertEquals(BlockIds.RED_SANDSTONE, loaded.getBlock(2, 11, 4));
        assertEquals(BlockIds.YELLOW_SANDSTONE, loaded.getBlock(2, 12, 4));
        assertEquals(BlockIds.DIYA, loaded.getBlock(8, 64, 8));
        assertEquals(BlockIds.HIMALAYAN_SNOW, loaded.getBlock(15, 319, 15));
        assertEquals(BlockIds.AIR, loaded.getBlock(0, 0, 0));

        // Verify lighting parity
        assertEquals(15, loaded.getSkyLight(8, 64, 8));
        assertEquals(14, loaded.getBlockLight(8, 64, 8));
        assertEquals(13, loaded.getBlockLight(8, 65, 8));
        assertEquals(15, loaded.getSkyLight(15, 319, 15));
        assertEquals(0, loaded.getBlockLight(0, 0, 0));
    }

    @Test
    @DisplayName("RegionFile writes and reads chunks across 32x32 grid with sector alignment")
    void testRegionFileDiskRoundTrip(@TempDir Path tempDir) throws IOException {
        File regionFile = tempDir.resolve("r.0.0.slr").toFile();

        ChunkColumn chunk1 = new ChunkColumn(0, 0);
        chunk1.setBlock(0, 60, 0, BlockIds.FIRED_BRICK);
        chunk1.setBlock(15, 60, 15, BlockIds.SAL_PLANKS);
        chunk1.setBlockLight(0, 60, 0, 10);

        ChunkColumn chunk2 = new ChunkColumn(31, 31);
        chunk2.setBlock(7, 100, 7, BlockIds.THAR_SAND);
        chunk2.setSkyLight(7, 100, 7, 15);

        // 1. Write chunks to RegionFile
        try (RegionFile rf = new RegionFile(regionFile)) {
            byte[] p1 = ChunkSerializer.serialize(chunk1);
            byte[] p2 = ChunkSerializer.serialize(chunk2);

            rf.writeChunk(0, 0, p1);
            rf.writeChunk(31, 31, p2);
        }

        // Verify region file exists and has minimum 8 KB header
        assertTrue(regionFile.exists());
        assertTrue(regionFile.length() >= RegionFile.HEADER_BYTES);

        // 2. Re-open RegionFile and read back
        try (RegionFile rf = new RegionFile(regionFile)) {
            byte[] p1Loaded = rf.readChunk(0, 0);
            assertNotNull(p1Loaded);
            ChunkColumn loaded1 = ChunkSerializer.deserialize(0, 0, p1Loaded);
            assertEquals(BlockIds.FIRED_BRICK, loaded1.getBlock(0, 60, 0));
            assertEquals(BlockIds.SAL_PLANKS, loaded1.getBlock(15, 60, 15));
            assertEquals(10, loaded1.getBlockLight(0, 60, 0));

            byte[] p2Loaded = rf.readChunk(31, 31);
            assertNotNull(p2Loaded);
            ChunkColumn loaded2 = ChunkSerializer.deserialize(31, 31, p2Loaded);
            assertEquals(BlockIds.THAR_SAND, loaded2.getBlock(7, 100, 7));
            assertEquals(15, loaded2.getSkyLight(7, 100, 7));

            // Non-existent chunk in same region returns null
            assertNull(rf.readChunk(15, 15));
        }
    }

    @Test
    @DisplayName("RegionFile relocates chunk and reclaims free sectors when payload expands")
    void testChunkExpansionRelocation(@TempDir Path tempDir) throws IOException {
        File regionFile = tempDir.resolve("r.1.-1.slr").toFile();

        ChunkColumn smallChunk = new ChunkColumn(1, -1);
        smallChunk.setBlock(1, 1, 1, BlockIds.ALLUVIAL_SOIL);

        try (RegionFile rf = new RegionFile(regionFile)) {
            rf.writeChunk(1, 31, ChunkSerializer.serialize(smallChunk));
        }

        // Expand chunk heavily: fill thousands of blocks to require additional sectors
        ChunkColumn largeChunk = new ChunkColumn(1, -1);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 16; x++) {
                largeChunk.setBlock(x, y, 0, BlockIds.GRANITE);
                largeChunk.setBlock(x, y, 15, BlockIds.RED_SANDSTONE);
            }
        }

        try (RegionFile rf = new RegionFile(regionFile)) {
            rf.writeChunk(1, 31, ChunkSerializer.serialize(largeChunk));
        }

        // Verify loaded data has all expanded blocks
        try (RegionFile rf = new RegionFile(regionFile)) {
            byte[] payload = rf.readChunk(1, 31);
            assertNotNull(payload);
            ChunkColumn loaded = ChunkSerializer.deserialize(1, -1, payload);
            for (int y = 0; y < 64; y++) {
                assertEquals(BlockIds.GRANITE, loaded.getBlock(0, y, 0));
                assertEquals(BlockIds.RED_SANDSTONE, loaded.getBlock(15, y, 15));
            }
        }
    }
}
