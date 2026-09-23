package shilploka.world.save;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * WHY:
 * Central data schema for world configuration, seed state, celestial time, and gamerules.
 * Stored in `saves/<world_folder>/level.json`.
 *
 * WHAT:
 * Jackson-serializable POJO conforming to ShilpLoka DATA_MODELS specification §7.1.
 * Contains format version, game version, world name, seed, timestamps, game mode,
 * celestial day/night ticks, day count, spawn coordinates, weather state, and gamerules.
 *
 * USED FOR:
 * Loaded at world boot by SaveManager and serialized during autosaves and shutdown.
 *
 * WHERE NEXT:
 * Used by Game to seed WorldGenerator, restore DayNightCycle tick counts, and position Player.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldMetadata {

    @JsonProperty("formatVersion")
    private int formatVersion = 1;

    @JsonProperty("gameVersion")
    private String gameVersion = "0.1.0";

    @JsonProperty("worldName")
    private String worldName = "Mera Bharat";

    @JsonProperty("seed")
    private long seed = 19470815L;

    @JsonProperty("createdTimestamp")
    private long createdTimestamp = System.currentTimeMillis() / 1000L;

    @JsonProperty("lastPlayedTimestamp")
    private long lastPlayedTimestamp = System.currentTimeMillis() / 1000L;

    @JsonProperty("gameMode")
    private String gameMode = "creative"; // "survival" or "creative"

    @JsonProperty("difficulty")
    private String difficulty = "saadharan"; // "saral", "saadharan", "kathin"

    @JsonProperty("worldTimeTicks")
    private long worldTimeTicks = 0L;

    @JsonProperty("dayCount")
    private long dayCount = 0L;

    @JsonProperty("weather")
    private WeatherState weather = new WeatherState();

    @JsonProperty("spawnCoordinates")
    private SpawnCoordinates spawnCoordinates = new SpawnCoordinates(8.5, 64.0, 8.5);

    @JsonProperty("gamerules")
    private Map<String, Object> gamerules = new HashMap<>();

    public WorldMetadata() {
        gamerules.put("doDaylightCycle", true);
        gamerules.put("doWeatherCycle", true);
        gamerules.put("keepInventory", false);
        gamerules.put("mobSpawning", true);
    }

    public WorldMetadata(String worldName, long seed, String gameMode) {
        this();
        this.worldName = worldName;
        this.seed = seed;
        this.gameMode = gameMode;
        this.createdTimestamp = System.currentTimeMillis() / 1000L;
        this.lastPlayedTimestamp = this.createdTimestamp;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SpawnCoordinates {
        @JsonProperty("x")
        public double x;
        @JsonProperty("y")
        public double y;
        @JsonProperty("z")
        public double z;

        public SpawnCoordinates() {}

        public SpawnCoordinates(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherState {
        @JsonProperty("currentWeather")
        public String currentWeather = "saaf"; // "saaf" (clear), "barsaat" (rain), "aandhi" (dust storm)
        @JsonProperty("remainingDurationTicks")
        public int remainingDurationTicks = 12000;

        public WeatherState() {}

        public WeatherState(String currentWeather, int remainingDurationTicks) {
            this.currentWeather = currentWeather;
            this.remainingDurationTicks = remainingDurationTicks;
        }
    }

    // Getters and Setters
    public int getFormatVersion() { return formatVersion; }
    public void setFormatVersion(int formatVersion) { this.formatVersion = formatVersion; }

    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public long getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(long createdTimestamp) { this.createdTimestamp = createdTimestamp; }

    public long getLastPlayedTimestamp() { return lastPlayedTimestamp; }
    public void setLastPlayedTimestamp(long lastPlayedTimestamp) { this.lastPlayedTimestamp = lastPlayedTimestamp; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public long getWorldTimeTicks() { return worldTimeTicks; }
    public void setWorldTimeTicks(long worldTimeTicks) { this.worldTimeTicks = worldTimeTicks; }

    public long getDayCount() { return dayCount; }
    public void setDayCount(long dayCount) { this.dayCount = dayCount; }

    public WeatherState getWeather() { return weather; }
    public void setWeather(WeatherState weather) { this.weather = weather; }

    public SpawnCoordinates getSpawnCoordinates() { return spawnCoordinates; }
    public void setSpawnCoordinates(SpawnCoordinates spawnCoordinates) { this.spawnCoordinates = spawnCoordinates; }

    public Map<String, Object> getGamerules() { return gamerules; }
    public void setGamerules(Map<String, Object> gamerules) { this.gamerules = gamerules; }
}
