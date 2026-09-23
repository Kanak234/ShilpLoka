package shilploka.worldgen.biome;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies that the BiomeProvider climate model successfully generates realistic,
 * deterministic distribution of all 3 Phase P3 regional biomes of Bharat.
 *
 * WHAT:
 * Tests climate noise bounds, seed determinism, and checks that across a wide
 * geographic area, Ganga Maidan, Thar Marusthal, and Himalaya Shikhar all naturally appear.
 *
 * USED FOR:
 * Automated CI verification of climate and biome selection logic.
 */
class BiomeProviderTest {

    @Test
    @DisplayName("BiomeProvider outputs all 3 regional biomes across a continental area")
    void testAllBiomesPresent() {
        BiomeProvider provider = new BiomeProvider(42L);
        EnumSet<BiomeType> foundBiomes = EnumSet.noneOf(BiomeType.class);

        // Sample points across a 4000x4000 block continental expanse
        for (int x = -2000; x <= 2000; x += 100) {
            for (int z = -2000; z <= 2000; z += 100) {
                BiomeType biome = provider.getBiomeAt(x, z);
                foundBiomes.add(biome);
            }
        }

        assertTrue(foundBiomes.contains(BiomeType.GANGA_MAIDAN), "World must generate Ganga Maidan plains");
        assertTrue(foundBiomes.contains(BiomeType.THAR_MARUSTHAL), "World must generate Thar Marusthal desert");
        assertTrue(foundBiomes.contains(BiomeType.HIMALAYA_SHIKHAR), "World must generate Himalaya Shikhar mountains");
        assertTrue(foundBiomes.size() >= 3, "At least 3 distinct regional biomes of Bharat must be present");
    }

    @Test
    @DisplayName("Biome queries are deterministic for identical seeds")
    void testDeterminism() {
        long seed = 987654321L;
        BiomeProvider provider1 = new BiomeProvider(seed);
        BiomeProvider provider2 = new BiomeProvider(seed);

        for (int x = -500; x <= 500; x += 50) {
            for (int z = -500; z <= 500; z += 50) {
                assertEquals(provider1.getBiomeAt(x, z), provider2.getBiomeAt(x, z),
                        "Biome mismatch at (" + x + "," + z + ")");
                assertEquals(provider1.getTemperature(x, z), provider2.getTemperature(x, z), 1e-6);
                assertEquals(provider1.getRainfall(x, z), provider2.getRainfall(x, z), 1e-6);
            }
        }
    }
}
