package shilploka.world.weather;

import org.joml.Vector3f;
import shilploka.worldgen.biome.BiomeProvider;
import shilploka.worldgen.biome.BiomeType;

import java.util.Random;

/**
 * WHY:
 * Orchestrates dynamic regional weather events (Barsaat monsoon, Thar Aandhi dust storms,
 * Himalayan Himpaat snow) and traditional 6-season (Shat-Ritu) progression.
 *
 * WHAT:
 * Modulates sky illumination, atmospheric fog density and coloration, precipitation particles,
 * and environmental crop hydration rates.
 *
 * USED FOR:
 * Updated in Game loop and queried during shader uniform uploads and debug overlay rendering.
 */
public class WeatherManager {

    private WeatherType currentWeather = WeatherType.CLEAR;
    private SeasonType currentSeason = SeasonType.VASANTA;
    private int weatherDurationTicks = 2400; // 2 minutes default duration
    private final Random random = new Random();

    private final Vector3f weatherFogColor = new Vector3f();

    public void update(float deltaTime, int playerX, int playerZ, int dayCount, BiomeProvider biomeProvider) {
        // Update active season based on total elapsed days
        this.currentSeason = SeasonType.fromDay(dayCount);

        // Update weather timer
        int deltaTicks = (int) (deltaTime * 20.0f);
        weatherDurationTicks -= Math.max(1, deltaTicks);

        if (weatherDurationTicks <= 0) {
            // Pick next weather event based on regional biome & season
            BiomeType biome = (biomeProvider != null) ? biomeProvider.getBiomeAt(playerX, playerZ) : BiomeType.GANGA_MAIDAN;
            transitionWeather(biome);
        }
    }

    private void transitionWeather(BiomeType biome) {
        // Duration: 2 to 5 minutes (2400 to 6000 ticks)
        this.weatherDurationTicks = 2400 + random.nextInt(3600);

        if (biome == BiomeType.THAR_MARUSTHAL || biome == BiomeType.RANN_NAMAK) {
            // Arid desert biomes: clear skies or howling Aandhi dust storms
            this.currentWeather = (random.nextFloat() < 0.35f) ? WeatherType.AANDHI : WeatherType.CLEAR;
        } else if (biome == BiomeType.HIMALAYA_SHIKHAR) {
            // High altitude alpine: clear cold skies or Himpaat snowfall
            this.currentWeather = (random.nextFloat() < 0.45f) ? WeatherType.HIMPAAT : WeatherType.CLEAR;
        } else if (biome == BiomeType.SAHYADRI_VARSHAVAN || biome == BiomeType.SUNDARBAN) {
            // Rainforest and delta wetlands: frequent Barsaat monsoon rains
            this.currentWeather = (random.nextFloat() < 0.60f) ? WeatherType.BARSAAT : WeatherType.CLEAR;
        } else {
            // Continental plains & forests: monsoon dependent on season
            if (currentSeason == SeasonType.VARSHA) {
                this.currentWeather = (random.nextFloat() < 0.70f) ? WeatherType.BARSAAT : WeatherType.CLEAR;
            } else if (currentSeason == SeasonType.SHISHIRA) {
                this.currentWeather = (random.nextFloat() < 0.20f) ? WeatherType.HIMPAAT : WeatherType.CLEAR;
            } else {
                this.currentWeather = (random.nextFloat() < 0.20f) ? WeatherType.BARSAAT : WeatherType.CLEAR;
            }
        }
    }

    public Vector3f getModifiedFogColor(Vector3f baseFog) {
        weatherFogColor.set(baseFog);
        switch (currentWeather) {
            case BARSAAT -> {
                // Dark slate-grey rain storm fog
                weatherFogColor.mul(0.55f, 0.60f, 0.70f);
            }
            case AANDHI -> {
                // Amber-ochre desert dust haze
                weatherFogColor.set(0.82f, 0.62f, 0.32f);
            }
            case HIMPAAT -> {
                // Blinding cold white snow flurry fog
                weatherFogColor.set(0.90f, 0.93f, 0.98f);
            }
            case CLEAR -> {}
        }
        return weatherFogColor;
    }

    public float getFogDistanceMultiplier() {
        return switch (currentWeather) {
            case AANDHI -> 0.40f;  // Severe visibility reduction in dust storm
            case BARSAAT -> 0.65f; // Reduced visibility in torrential rain
            case HIMPAAT -> 0.75f; // Moderate snowfall reduction
            case CLEAR -> 1.0f;
        };
    }

    public float getSunlightFactor(float baseSunlight) {
        return baseSunlight * currentWeather.getSunlightMultiplier();
    }

    public void setWeather(WeatherType weather, int durationTicks) {
        this.currentWeather = (weather != null) ? weather : WeatherType.CLEAR;
        this.weatherDurationTicks = durationTicks;
    }

    public WeatherType getCurrentWeather() { return currentWeather; }
    public SeasonType getCurrentSeason() { return currentSeason; }
    public int getWeatherDurationTicks() { return weatherDurationTicks; }
}
