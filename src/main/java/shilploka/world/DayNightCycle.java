package shilploka.world;

import org.joml.Vector3f;

/**
 * WHY:
 * Time in ancient Indian culture is cyclical and marked by the celestial movements of Surya
 * (the Sun) and Chandra (the Moon). ShilpLoka's day-night engine drives sunlight intensity,
 * atmospheric color gradients, nocturnal creature spawning conditions, and lunar phases
 * (Shukla and Krishna Paksha).
 *
 * WHAT:
 * Simulates a continuous 24,000-tick celestial day (20 minutes real time). Calculates:
 * - Real-time sun angle and directional celestial vectors
 * - Sunlight factor (1.0 at noon down to 0.15 at night)
 * - Atmospheric sky and fog colors (azure midday, saffron sunset, deep indigo midnight)
 * - 8 traditional lunar phases (Purnima full moon to Amavasya new moon)
 *
 * USED FOR:
 * Updated in Game.update(), passes sunlight factor and fog uniforms to chunk shaders,
 * sets the OpenGL clear color, and outputs time telemetry to the F3 debug screen.
 *
 * WHERE NEXT:
 * The chunk vertex shader attenuates Sky Light using u_SunlightFactor, and future
 * hostile nocturnal entities (Vetaal, Chhaya) check getSunlightFactor() to emerge.
 */
public class DayNightCycle {

    public static final int TICKS_PER_DAY = 24000;
    public static final float TICKS_PER_SECOND = 20.0f; // 24,000 ticks / 1,200 seconds = 20 ticks/sec

    // Celestial Time Milestones (in ticks)
    public static final int DAWN_START = 23000;
    public static final int DAY_START = 1000;
    public static final int NOON = 6000;
    public static final int DUSK_START = 11000;
    public static final int NIGHT_START = 13000;
    public static final int MIDNIGHT = 18000;

    // Atmospheric Color Palettes (R, G, B)
    private static final Vector3f COLOR_DAY_SKY = new Vector3f(0.42f, 0.68f, 0.96f);    // Clear azure
    private static final Vector3f COLOR_TWILIGHT_SKY = new Vector3f(0.92f, 0.44f, 0.18f); // Saffron-vermillion twilight
    private static final Vector3f COLOR_NIGHT_SKY = new Vector3f(0.04f, 0.05f, 0.12f);   // Deep indigo midnight

    private static final Vector3f COLOR_DAY_FOG = new Vector3f(0.65f, 0.78f, 0.95f);
    private static final Vector3f COLOR_TWILIGHT_FOG = new Vector3f(0.85f, 0.48f, 0.28f);
    private static final Vector3f COLOR_NIGHT_FOG = new Vector3f(0.05f, 0.06f, 0.14f);

    private double worldTime; // 0.0 to 24000.0
    private long totalDays = 0;

    private final Vector3f currentSkyColor = new Vector3f();
    private final Vector3f currentFogColor = new Vector3f();

    /**
     * Initializes the cycle at sunrise (tick 0).
     */
    public DayNightCycle() {
        this(0.0);
    }

    /**
     * Initializes the cycle at a specific starting tick.
     */
    public DayNightCycle(double initialTick) {
        this.worldTime = initialTick % TICKS_PER_DAY;
        updateColors();
    }

    /**
     * WHY:
     * Advances celestial time based on delta frame time.
     *
     * @param deltaSeconds Elapsed seconds since previous frame.
     */
    public void update(float deltaSeconds) {
        this.worldTime += deltaSeconds * TICKS_PER_SECOND;
        if (this.worldTime >= TICKS_PER_DAY) {
            long daysPassed = (long) (this.worldTime / TICKS_PER_DAY);
            this.totalDays += daysPassed;
            this.worldTime %= TICKS_PER_DAY;
        }
        updateColors();
    }

    /**
     * Computes the current celestial sunlight multiplier (0.15f at night to 1.0f at noon).
     */
    public float getSunlightFactor() {
        if (worldTime >= DAY_START && worldTime < DUSK_START) {
            return 1.0f; // Full daylight
        } else if (worldTime >= DUSK_START && worldTime < NIGHT_START) {
            // Dusk transition: 1.0 down to 0.15
            float progress = (float) ((worldTime - DUSK_START) / (NIGHT_START - DUSK_START));
            return 1.0f - progress * 0.85f;
        } else if (worldTime >= NIGHT_START && worldTime < DAWN_START) {
            return 0.15f; // Nighttime ambient starlight/moonlight
        } else {
            // Dawn transition: 0.15 up to 1.0
            float progress;
            if (worldTime >= DAWN_START) {
                progress = (float) ((worldTime - DAWN_START) / (TICKS_PER_DAY - DAWN_START + DAY_START));
            } else {
                progress = (float) ((worldTime + (TICKS_PER_DAY - DAWN_START)) / (TICKS_PER_DAY - DAWN_START + DAY_START));
            }
            return 0.15f + progress * 0.85f;
        }
    }

    public boolean isNight() {
        return worldTime >= NIGHT_START && worldTime < DAWN_START;
    }

    /**
     * Updates interpolated atmospheric sky and fog colors based on the celestial phase.
     */
    private void updateColors() {
        float factor = getSunlightFactor();

        if (worldTime >= DAY_START && worldTime < DUSK_START) {
            currentSkyColor.set(COLOR_DAY_SKY);
            currentFogColor.set(COLOR_DAY_FOG);
        } else if (worldTime >= DUSK_START && worldTime < NIGHT_START) {
            // Blend from Saffron Twilight to Night Indigo
            float t = (float) ((worldTime - DUSK_START) / (NIGHT_START - DUSK_START));
            if (t < 0.5f) {
                // Day to Saffron
                currentSkyColor.set(COLOR_DAY_SKY).lerp(COLOR_TWILIGHT_SKY, t * 2.0f);
                currentFogColor.set(COLOR_DAY_FOG).lerp(COLOR_TWILIGHT_FOG, t * 2.0f);
            } else {
                // Saffron to Indigo
                currentSkyColor.set(COLOR_TWILIGHT_SKY).lerp(COLOR_NIGHT_SKY, (t - 0.5f) * 2.0f);
                currentFogColor.set(COLOR_TWILIGHT_FOG).lerp(COLOR_NIGHT_FOG, (t - 0.5f) * 2.0f);
            }
        } else if (worldTime >= NIGHT_START && worldTime < DAWN_START) {
            currentSkyColor.set(COLOR_NIGHT_SKY);
            currentFogColor.set(COLOR_NIGHT_FOG);
        } else {
            // Dawn: Night Indigo to Saffron Twilight to Day
            float t;
            if (worldTime >= DAWN_START) {
                t = (float) ((worldTime - DAWN_START) / (TICKS_PER_DAY - DAWN_START + DAY_START));
            } else {
                t = (float) ((worldTime + (TICKS_PER_DAY - DAWN_START)) / (TICKS_PER_DAY - DAWN_START + DAY_START));
            }

            if (t < 0.5f) {
                currentSkyColor.set(COLOR_NIGHT_SKY).lerp(COLOR_TWILIGHT_SKY, t * 2.0f);
                currentFogColor.set(COLOR_NIGHT_FOG).lerp(COLOR_TWILIGHT_FOG, t * 2.0f);
            } else {
                currentSkyColor.set(COLOR_TWILIGHT_SKY).lerp(COLOR_DAY_SKY, (t - 0.5f) * 2.0f);
                currentFogColor.set(COLOR_TWILIGHT_FOG).lerp(COLOR_DAY_FOG, (t - 0.5f) * 2.0f);
            }
        }
    }

    /**
     * Calculates the celestial angle of the Sun in radians (0 at noon, PI at midnight).
     */
    public float getSunAngleRadians() {
        return (float) (((worldTime - NOON) / TICKS_PER_DAY) * 2.0 * Math.PI);
    }

    /**
     * Returns the active moon phase index (0 to 7) based on the world day count.
     */
    public int getMoonPhase() {
        return (int) (totalDays % 8);
    }

    /**
     * Returns the traditional Sanskrit/Hindi name for the current moon phase.
     */
    public String getMoonPhaseName() {
        return switch (getMoonPhase()) {
            case 0 -> "Shukla Pratipada (Waxing Crescent)";
            case 1 -> "Shukla Ashtami (First Quarter)";
            case 2 -> "Shukla Chaturdashi (Waxing Gibbous)";
            case 3 -> "Purnima (Full Moon / पूर्णिमा)";
            case 4 -> "Krishna Pratipada (Waning Gibbous)";
            case 5 -> "Krishna Ashtami (Third Quarter)";
            case 6 -> "Krishna Chaturdashi (Waning Crescent)";
            default -> "Amavasya (New Moon / अमावस्या)";
        };
    }

    /**
     * Formats the current time as a 24-hour clock string with Indian day period.
     */
    public String getFormattedTime() {
        int totalMinutes = (int) ((worldTime / TICKS_PER_DAY) * 1440.0);
        int hours = (totalMinutes / 60 + 6) % 24; // Tick 0 is 06:00 AM (Sunrise)
        int minutes = totalMinutes % 60;

        String period;
        if (worldTime >= DAWN_START || worldTime < DAY_START) {
            period = "Pratahkaal (Dawn)";
        } else if (worldTime < DUSK_START) {
            period = (hours < 12) ? "Poorvahn (Morning)" : "Madhyahn (Afternoon)";
        } else if (worldTime < NIGHT_START) {
            period = "Sandhya (Dusk)";
        } else {
            period = "Ratri (Night)";
        }

        return String.format("%02d:%02d [%s]", hours, minutes, period);
    }

    public Vector3f getSkyColor() {
        return currentSkyColor;
    }

    public Vector3f getFogColor() {
        return currentFogColor;
    }

    public double getWorldTime() {
        return worldTime;
    }

    public void setWorldTime(double worldTime) {
        this.worldTime = worldTime % TICKS_PER_DAY;
        updateColors();
    }

    public void setTime(double time) {
        setWorldTime(time);
    }

    public long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }

    public long getDayCount() {
        return totalDays;
    }

    public void setDayCount(long days) {
        this.totalDays = days;
    }
}
