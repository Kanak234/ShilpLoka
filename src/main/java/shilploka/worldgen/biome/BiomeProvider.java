package shilploka.worldgen.biome;

import shilploka.worldgen.noise.OctaveNoise;

/**
 * WHY:
 * World topography and ecosystems must transition organically across the subcontinent of Bharat.
 * Northern latitudes yield freezing Himalayan elevations, western reaches transition into arid
 * Thar dunes and salt flats, southern and western coasts host dense rainforests and beaches,
 * and central-eastern river basins form fertile Gangetic plains and mangrove deltas.
 *
 * WHAT:
 * Evaluates three independent low-frequency 2D Fractal Brownian Motion noise fields:
 * 1. Temperature field (Cold vs Tropical)
 * 2. Rainfall / Moisture field (Arid vs Monsoon Deluge)
 * 3. Continental / Ocean field (Deep sea, coast, inland, highland plateau)
 *
 * Classifies world coordinates into one of the 11 distinct regional biomes.
 */
public class BiomeProvider {

    private final OctaveNoise temperatureNoise;
    private final OctaveNoise rainfallNoise;
    private final OctaveNoise continentalNoise;

    // Macro-scale climate frequency (continents stretch over hundreds of blocks)
    private static final double CLIMATE_FREQUENCY = 0.0015;
    private static final double OCEAN_FREQUENCY = 0.0010;

    public BiomeProvider(long seed) {
        this.temperatureNoise = new OctaveNoise(seed + 101L, 3, 0.5, 2.0);
        this.rainfallNoise = new OctaveNoise(seed + 202L, 3, 0.5, 2.0);
        this.continentalNoise = new OctaveNoise(seed + 303L, 3, 0.5, 2.0);
    }

    public double getTemperature(int worldX, int worldZ) {
        return this.temperatureNoise.noise2(worldX * CLIMATE_FREQUENCY, worldZ * CLIMATE_FREQUENCY);
    }

    public double getRainfall(int worldX, int worldZ) {
        return this.rainfallNoise.noise2(worldX * CLIMATE_FREQUENCY, worldZ * CLIMATE_FREQUENCY);
    }

    public double getContinentalness(int worldX, int worldZ) {
        return this.continentalNoise.noise2(worldX * OCEAN_FREQUENCY, worldZ * OCEAN_FREQUENCY);
    }

    /**
     * Resolves the primary BiomeType for a specific block column among all 11 authentic biomes.
     */
    public BiomeType getBiomeAt(int worldX, int worldZ) {
        double temp = getTemperature(worldX, worldZ);
        double rain = getRainfall(worldX, worldZ);
        double conti = getContinentalness(worldX, worldZ);

        // 1. Oceanic & Coastal Transition Zones
        if (conti < -0.45) {
            return BiomeType.SAGAR; // Deep Indian Ocean
        } else if (conti < -0.30) {
            if (rain < -0.20) {
                return BiomeType.RANN_NAMAK; // Salt flats of Kutch
            } else if (rain > 0.35) {
                return BiomeType.SUNDARBAN; // Mangrove delta wetlands
            } else {
                return BiomeType.SAMUDRA_TAT; // Sandy ocean beach
            }
        }

        // 2. Continental Mountain & Desert Extremes
        if (temp < -0.30) {
            return BiomeType.HIMALAYA_SHIKHAR; // Snowy alpine crags
        } else if (temp > 0.25 && rain < -0.25) {
            return BiomeType.THAR_MARUSTHAL; // Arid golden desert
        }

        // 3. Dense Monsoon Rainforests & Bamboo Highlands
        if (rain > 0.45) {
            if (temp > 0.10) {
                return BiomeType.SAHYADRI_VARSHAVAN; // Western Ghats tropical rainforest
            } else {
                return BiomeType.POORVOTTAR_BAANS; // Northeastern bamboo hills
            }
        }

        // 4. Plateaus, Plains & Dry Deciduous Jungles
        if (temp > 0.10 && rain < 0.05) {
            return BiomeType.DECCAN_PATHAR; // Deccan basalt plateau
        } else if (rain > -0.05) {
            return BiomeType.GANGA_MAIDAN; // Indo-Gangetic alluvial plains
        } else {
            return BiomeType.VINDHYA_JANGAL; // Central Vindhyan plateau
        }
    }
}
