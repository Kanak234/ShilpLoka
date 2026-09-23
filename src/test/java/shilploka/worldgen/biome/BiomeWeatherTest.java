package shilploka.worldgen.biome;

import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.item.ItemRegistry;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.weather.SeasonType;
import shilploka.world.weather.WeatherManager;
import shilploka.world.weather.WeatherType;
import shilploka.worldgen.feature.TreeGenerator;

import java.util.EnumSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Comprehensive unit test suite for ShilpLoka Phase P10:
 * Complete Biome Suite (11 Indian Biomes) & Dynamic Weather Systems.
 *
 * WHAT:
 * Tests the 11-biome geography, 6 Shat-Ritu seasons, WeatherManager transitions
 * (Barsaat, Aandhi, Himpaat), and regional flora generation (Teak, Mangrove, Bamboo).
 */
public class BiomeWeatherTest {

    @BeforeAll
    public static void setUp() {
        ItemRegistry.init();
    }

    @Test
    @DisplayName("All 11 authentic Indian biomes are registered with valid strata parameters")
    public void testAll11BiomesExist() {
        assertEquals(11, BiomeType.values().length, "Engine must define exactly 11 authentic Indian biomes");

        for (BiomeType biome : BiomeType.values()) {
            assertNotNull(biome.getId());
            assertNotNull(biome.getDisplayName());
            assertTrue(biome.getTopBlock() > 0, "Biome must have valid solid top block: " + biome.name());
            assertTrue(biome.getUnderBlock() > 0, "Biome must have valid under block: " + biome.name());
            assertTrue(biome.getBaseHeight() >= 45, "Base height must be above bedrock: " + biome.name());
        }

        // Verify specific key biomes
        assertNotNull(BiomeType.valueOf("DECCAN_PATHAR"));
        assertNotNull(BiomeType.valueOf("SAHYADRI_VARSHAVAN"));
        assertNotNull(BiomeType.valueOf("SUNDARBAN"));
        assertNotNull(BiomeType.valueOf("POORVOTTAR_BAANS"));
        assertNotNull(BiomeType.valueOf("RANN_NAMAK"));
        assertNotNull(BiomeType.valueOf("SAMUDRA_TAT"));
        assertNotNull(BiomeType.valueOf("SAGAR"));
        assertNotNull(BiomeType.valueOf("VINDHYA_JANGAL"));
    }

    @Test
    @DisplayName("BiomeProvider outputs all 11 biomes across a continental subcontinent area")
    public void test11BiomeDistribution() {
        BiomeProvider provider = new BiomeProvider(19470815L);
        EnumSet<BiomeType> observedBiomes = EnumSet.noneOf(BiomeType.class);

        // Sample a 4000x4000 block continental survey
        for (int x = -2000; x <= 2000; x += 100) {
            for (int z = -2000; z <= 2000; z += 100) {
                BiomeType b = provider.getBiomeAt(x, z);
                observedBiomes.add(b);
            }
        }

        // Ensure all 11 regional biomes are sampled
        for (BiomeType b : BiomeType.values()) {
            assertTrue(observedBiomes.contains(b), "Subcontinent survey must naturally generate: " + b.name());
        }
        assertEquals(11, observedBiomes.size());
    }

    @Test
    @DisplayName("Shat-Ritu 6-season cycle advances accurately every 4 in-game days")
    public void testSeasonProgression() {
        assertEquals(SeasonType.VASANTA, SeasonType.fromDay(0));  // Days 0-3: Spring (Vasanta)
        assertEquals(SeasonType.VASANTA, SeasonType.fromDay(3));
        assertEquals(SeasonType.GRISHMA, SeasonType.fromDay(4));  // Days 4-7: Summer (Grishma)
        assertEquals(SeasonType.VARSHA, SeasonType.fromDay(8));   // Days 8-11: Monsoon (Varsha)
        assertEquals(SeasonType.SHARAD, SeasonType.fromDay(12));  // Days 12-15: Autumn (Sharad)
        assertEquals(SeasonType.HEMANTA, SeasonType.fromDay(16)); // Days 16-19: Pre-winter (Hemanta)
        assertEquals(SeasonType.SHISHIRA, SeasonType.fromDay(20));// Days 20-23: Deep winter (Shishira)
        assertEquals(SeasonType.VASANTA, SeasonType.fromDay(24)); // Day 24: Wraps around to Vasanta
    }

    @Test
    @DisplayName("WeatherManager modulates fog color, fog distance, and sunlight accurately")
    public void testWeatherAtmospherics() {
        WeatherManager wm = new WeatherManager();
        Vector3f baseFog = new Vector3f(0.6f, 0.75f, 0.95f);

        // Clear weather
        wm.setWeather(WeatherType.CLEAR, 2400);
        assertEquals(1.0f, wm.getFogDistanceMultiplier());
        assertEquals(1.0f, wm.getSunlightFactor(1.0f));

        // Barsaat Monsoon
        wm.setWeather(WeatherType.BARSAAT, 2400);
        assertEquals(0.65f, wm.getFogDistanceMultiplier());
        assertEquals(0.45f, wm.getSunlightFactor(1.0f), 0.01f);
        Vector3f rainFog = wm.getModifiedFogColor(baseFog);
        assertTrue(rainFog.y < baseFog.y, "Rain storm fog should be darker than clear sky fog");

        // Thar Aandhi Dust Storm
        wm.setWeather(WeatherType.AANDHI, 2400);
        assertEquals(0.40f, wm.getFogDistanceMultiplier(), "Aandhi dust storms severely reduce visibility");
        Vector3f dustFog = wm.getModifiedFogColor(baseFog);
        assertTrue(dustFog.x > 0.8f && dustFog.z < 0.4f, "Aandhi fog should be warm amber ochre");

        // Himalayan Himpaat Snow
        wm.setWeather(WeatherType.HIMPAAT, 2400);
        assertEquals(0.75f, wm.getFogDistanceMultiplier());
        Vector3f snowFog = wm.getModifiedFogColor(baseFog);
        assertTrue(snowFog.x > 0.85f && snowFog.z > 0.90f, "Himpaat snow fog should be bright white");
    }

    @Test
    @DisplayName("TreeGenerator generates authentic Teak, Mangrove, and Bamboo features")
    public void testNewBiomeTrees() {
        ChunkColumn column = new ChunkColumn(0, 0);
        Random rand = new Random(42L);

        // 1. Generate Sahyadri Teak Tree
        TreeGenerator.generateTree(column, 4, 64, 4, BiomeType.SAHYADRI_VARSHAVAN, rand);
        assertEquals(BlockIds.TEAK_LOG, column.getBlock(4, 65, 4), "Teak trunk should be TEAK_LOG");

        // 2. Generate Sundarban Mangrove Tree
        TreeGenerator.generateTree(column, 8, 62, 8, BiomeType.SUNDARBAN, rand);
        assertEquals(BlockIds.MANGROVE_LOG, column.getBlock(8, 63, 8), "Mangrove trunk should be MANGROVE_LOG");

        // 3. Generate Poorvottar Bamboo Grove
        TreeGenerator.generateTree(column, 12, 70, 12, BiomeType.POORVOTTAR_BAANS, rand);
        boolean hasBamboo = false;
        for (int y = 71; y <= 78; y++) {
            if (column.getBlock(12, y, 12) == BlockIds.BAANS) {
                hasBamboo = true;
                break;
            }
        }
        assertTrue(hasBamboo, "Bamboo grove should generate BAANS blocks");
    }
}
