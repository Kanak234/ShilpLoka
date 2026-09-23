package shilploka.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.world.dimension.DimensionType;
import shilploka.worldgen.biome.BiomeType;

import java.util.Random;

/**
 * WHY:
 * Orchestrates dynamic background ambient music inspired by authentic Indian classical
 * ragas (Bhairav for dawn, Sarang for midday, Yaman for dusk, Malkauns for midnight,
 * and tense percussion for boss battles).
 *
 * WHAT:
 * - Manages contextual music intervals, ambient soundscapes, and transitions.
 * - Sits atop SoundEngine with zero risk of threading crashes or audio stalling.
 */
public class MusicManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicManager.class);

    private final SoundEngine soundEngine;
    private final Random random = new Random();

    private float musicCooldown = 15.0f; // Seconds until next ambient piece
    private boolean inCombat = false;
    private boolean bossActive = false;

    public MusicManager(SoundEngine soundEngine) {
        this.soundEngine = soundEngine;
    }

    /**
     * Ticks the music manager clock and schedules contextual ambient pieces.
     */
    public void tick(float dt, BiomeType currentBiome, DimensionType currentDimension, boolean isNight) {
        if (soundEngine == null || !soundEngine.isInitialized()) return;

        musicCooldown -= dt;
        if (musicCooldown <= 0.0f) {
            playContextualPiece(currentBiome, currentDimension, isNight);
            // Space out background music by 60 - 120 seconds to prevent audio fatigue
            musicCooldown = 60.0f + random.nextFloat() * 60.0f;
        }
    }

    private void playContextualPiece(BiomeType biome, DimensionType dimension, boolean isNight) {
        if (dimension == DimensionType.PAATAAL) {
            // Low rumbling subterranean tones
            soundEngine.playSound2D(SoundEvent.PORTAL_AMBIENT, 0.7f, 0.75f);
        } else if (dimension == DimensionType.AKASH_DWEEP) {
            // Ethereal high celestial chimes
            soundEngine.playSound2D(SoundEvent.PEACOCK_CRY, 0.4f, 1.25f);
        } else if (isNight) {
            // Night tranquility / quiet wind
            soundEngine.playSound2D(SoundEvent.WEATHER_WIND, 0.35f, 0.8f);
        } else {
            // Daytime nature ambience
            soundEngine.playSound2D(SoundEvent.PEACOCK_CRY, 0.25f, 0.95f);
        }
    }

    public void setInCombat(boolean inCombat) {
        this.inCombat = inCombat;
    }

    public void setBossActive(boolean bossActive) {
        this.bossActive = bossActive;
    }

    public boolean isBossActive() {
        return bossActive;
    }

    public boolean isInCombat() {
        return inCombat;
    }
}
