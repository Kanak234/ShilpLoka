package shilploka.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkManager;
import shilploka.world.save.SaveManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates Phase P17: Performance profiling, stress testing chunk generation at high velocity,
 * verifying GC memory ceiling (< 2.0 GB JVM budget), and preventing memory leaks during rapid traversal.
 */
class PerformanceStressTest {

    @Test
    @DisplayName("Rapid procedural chunk generation maintains heap usage well below 1.5 GB budget")
    void testRapidChunkGenerationMemoryCeiling() {
        WorldGenerator generator = new WorldGenerator(987654321L);
        Runtime runtime = Runtime.getRuntime();

        // Warm up GC
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Generate a 10x10 chunk grid (100 full chunk columns = 2,000 16^3 sections)
        for (int cx = -5; cx < 5; cx++) {
            for (int cz = -5; cz < 5; cz++) {
                ChunkColumn col = generator.generateChunkColumn(cx, cz);
                assertNotNull(col);
                // Validate bedrock integrity under rapid generation
                assertEquals(BlockIds.DECCAN_BASALT, col.getBlock(0, 0, 0));
            }
        }

        long activeMemory = runtime.totalMemory() - runtime.freeMemory();
        long deltaMB = (activeMemory - initialMemory) / (1024 * 1024);

        // Enforce strict memory budget: 100 chunks must consume less than 350 MB additional heap
        assertTrue(deltaMB < 350, "100 generated chunk columns exceeded memory budget: " + deltaMB + " MB");
    }

    @Test
    @DisplayName("High-velocity flight simulation across 2,000 blocks unloads distant chunks cleanly")
    void testHighVelocityChunkStreaming() throws IOException {
        Path tempDir = Files.createTempDirectory("shilploka-perf-test");
        try {
            SaveManager saveManager = new SaveManager(tempDir.toFile(), "PerfWorld");
            WorldGenerator generator = new WorldGenerator(55555L);
            ChunkManager chunkManager = new ChunkManager(generator, saveManager);
            shilploka.render.Camera camera = new shilploka.render.Camera(0, 70, 0);

            // Simulate high-speed player trajectory along X-axis (0 to 1000 blocks, step 64 blocks)
            for (int x = 0; x <= 1000; x += 64) {
                camera.getPosition().set((float) x, 70.0f, 0.0f);
                chunkManager.update(camera, 4);
            }

            // Verify loaded chunks count stays constrained to local radius
            int activeChunks = chunkManager.getLoadedChunks().size();
            assertTrue(activeChunks <= 150, "ChunkManager loaded chunk count exceeded upper threshold: " + activeChunks);

            chunkManager.cleanup();
            saveManager.close();
        } finally {
            // Clean up temporary files
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
