package shilploka.audio;

/**
 * WHY:
 * Defines authentic acoustic and ambient sound events across the Bharatiya world of ShilpLoka.
 *
 * WHAT:
 * Sound events categorized by source (blocks, creatures, weather, combat, portals, and UI)
 * with procedural waveform synthesis parameters.
 */
public enum SoundEvent {
    // Footsteps
    STEP_STONE(SoundCategory.BLOCKS, "step.stone", 380.0f, 0.08f, 0.6f, 0.4f),
    STEP_WOOD(SoundCategory.BLOCKS, "step.wood", 220.0f, 0.09f, 0.4f, 0.5f),
    STEP_SAND(SoundCategory.BLOCKS, "step.sand", 160.0f, 0.12f, 0.8f, 0.3f),
    STEP_GRASS(SoundCategory.BLOCKS, "step.grass", 260.0f, 0.10f, 0.7f, 0.4f),
    STEP_WATER(SoundCategory.BLOCKS, "step.water", 180.0f, 0.15f, 0.5f, 0.7f),

    // Block interactions
    BLOCK_BREAK(SoundCategory.BLOCKS, "block.break", 200.0f, 0.14f, 0.7f, 0.6f),
    BLOCK_PLACE(SoundCategory.BLOCKS, "block.place", 320.0f, 0.10f, 0.3f, 0.5f),

    // Combat & Vitals
    PLAYER_HURT(SoundCategory.PLAYERS, "player.hurt", 180.0f, 0.20f, 0.2f, 0.8f),
    PLAYER_DIE(SoundCategory.PLAYERS, "player.die", 120.0f, 0.60f, 0.4f, 0.9f),
    COMBAT_HIT(SoundCategory.HOSTILE, "combat.hit", 440.0f, 0.12f, 0.5f, 0.7f),

    // Wildlife & Creatures
    COBRA_HISS(SoundCategory.HOSTILE, "creature.cobra_hiss", 2200.0f, 0.35f, 0.9f, 0.5f),
    TIGER_ROAR(SoundCategory.HOSTILE, "creature.tiger_roar", 95.0f, 0.65f, 0.4f, 0.9f),
    PEACOCK_CRY(SoundCategory.NEUTRAL, "creature.peacock_cry", 880.0f, 0.40f, 0.1f, 0.7f),
    ELEPHANT_TRUMPET(SoundCategory.NEUTRAL, "creature.elephant_trumpet", 140.0f, 0.80f, 0.3f, 1.0f),
    VETAAL_SHRIEK(SoundCategory.HOSTILE, "creature.vetaal_shriek", 1400.0f, 0.50f, 0.6f, 0.8f),
    BOSS_ROAR(SoundCategory.HOSTILE, "boss.andhakaar_roar", 65.0f, 1.20f, 0.5f, 1.0f),

    // Weather & Atmosphere
    WEATHER_RAIN(SoundCategory.WEATHER, "weather.rain", 500.0f, 0.50f, 0.95f, 0.4f),
    WEATHER_WIND(SoundCategory.WEATHER, "weather.wind", 110.0f, 0.60f, 0.85f, 0.5f),

    // Dimensional Portals & Rituals
    PORTAL_AMBIENT(SoundCategory.AMBIENT, "portal.ambient", 85.0f, 0.70f, 0.3f, 0.6f),
    PORTal_TRAVEL(SoundCategory.PLAYERS, "portal.travel", 280.0f, 0.80f, 0.5f, 0.9f),

    // UI & System
    UI_CLICK(SoundCategory.MASTER, "ui.click", 800.0f, 0.04f, 0.1f, 0.5f);

    private final SoundCategory category;
    private final String id;
    private final float baseFrequency;
    private final float duration;
    private final float noiseFactor;
    private final float defaultVolume;

    SoundEvent(SoundCategory category, String id, float baseFrequency, float duration, float noiseFactor, float defaultVolume) {
        this.category = category;
        this.id = id;
        this.baseFrequency = baseFrequency;
        this.duration = duration;
        this.noiseFactor = noiseFactor;
        this.defaultVolume = defaultVolume;
    }

    public SoundCategory getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public float getBaseFrequency() {
        return baseFrequency;
    }

    public float getDuration() {
        return duration;
    }

    public float getNoiseFactor() {
        return noiseFactor;
    }

    public float getDefaultVolume() {
        return defaultVolume;
    }
}
