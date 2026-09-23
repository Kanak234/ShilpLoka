package shilploka.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

/**
 * WHY:
 * High-performance 3D spatial audio engine powered by OpenAL Soft.
 * Provides authentic acoustic feedback (block interactions, creature vocals, footsteps, weather),
 * distance rolloff, and seamless headless fallback resilience.
 *
 * WHAT:
 * - Generates high-fidelity procedural 16-bit PCM audio waveforms without external dependencies.
 * - Manages OpenAL sources, buffers, 3D listener position, and per-category volume control.
 * - Gracefully falls back to a silent mock-free no-op mode if no hardware audio device is available.
 */
public class SoundEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoundEngine.class);
    private static final int SAMPLE_RATE = 22050;
    private static final int MAX_SOURCES = 24;

    private long device = 0;
    private long context = 0;
    private boolean initialized = false;

    private final int[] sources = new int[MAX_SOURCES];
    private final Map<SoundEvent, Integer> bufferMap = new EnumMap<>(SoundEvent.class);
    private final Map<SoundCategory, Float> categoryVolumes = new EnumMap<>(SoundCategory.class);

    private final Random random = new Random();

    public SoundEngine() {
        // Initialize default volume levels
        for (SoundCategory cat : SoundCategory.values()) {
            categoryVolumes.put(cat, 1.0f);
        }
        initOpenAL();
    }

    private void initOpenAL() {
        try {
            String defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER);
            device = alcOpenDevice(defaultDeviceName);
            if (device == 0) {
                LOGGER.warn("OpenAL: No audio device detected. Running in silent fallback mode.");
                return;
            }

            ALCCapabilities deviceCaps = ALC.createCapabilities(device);
            context = alcCreateContext(device, (IntBuffer) null);
            if (context == 0) {
                LOGGER.warn("OpenAL: Failed to create AL context. Running in silent fallback mode.");
                alcCloseDevice(device);
                device = 0;
                return;
            }

            alcMakeContextCurrent(context);
            AL.createCapabilities(deviceCaps);

            // Generate source pool
            for (int i = 0; i < MAX_SOURCES; i++) {
                sources[i] = alGenSources();
            }

            // Generate procedural audio buffers for all events
            generateProceduralBuffers();

            initialized = true;
            LOGGER.info("OpenAL SoundEngine initialized successfully on device [{}] with {} sources.",
                    defaultDeviceName != null ? defaultDeviceName : "Default", MAX_SOURCES);
        } catch (Throwable t) {
            LOGGER.warn("OpenAL initialization encountered an exception; operating in silent mode. Cause: {}", t.getMessage());
            initialized = false;
        }
    }

    /**
     * Synthesizes 16-bit PCM mono waveforms for all registered SoundEvents.
     */
    private void generateProceduralBuffers() {
        for (SoundEvent event : SoundEvent.values()) {
            int bufferId = alGenBuffers();
            int numSamples = (int) (SAMPLE_RATE * event.getDuration());
            ShortBuffer pcmBuffer = BufferUtils.createShortBuffer(numSamples);

            float freq = event.getBaseFrequency();
            float noiseFactor = event.getNoiseFactor();
            float decayRate = 3.5f / event.getDuration();

            for (int i = 0; i < numSamples; i++) {
                float t = (float) i / SAMPLE_RATE;
                float envelope = (float) Math.exp(-decayRate * t);

                // Blend pure sinusoidal tone with stochastic noise
                double tone = Math.sin(2.0 * Math.PI * freq * t);
                double noise = (random.nextDouble() * 2.0 - 1.0);
                double sample = ((1.0 - noiseFactor) * tone + noiseFactor * noise) * envelope;

                // Clamp to 16-bit signed integer range
                short pcm = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * 24000)));
                pcmBuffer.put(pcm);
            }
            pcmBuffer.flip();

            alBufferData(bufferId, AL_FORMAT_MONO16, pcmBuffer, SAMPLE_RATE);
            bufferMap.put(event, bufferId);
        }
    }

    /**
     * Plays a 3D positioned sound effect in world space.
     */
    public void playSound(SoundEvent event, double x, double y, double z, float volumeMod, float pitchMod) {
        if (!initialized || event == null) return;

        int source = findFreeSource();
        if (source == 0) return;

        Integer bufferId = bufferMap.get(event);
        if (bufferId == null) return;

        float masterVol = categoryVolumes.getOrDefault(SoundCategory.MASTER, 1.0f);
        float catVol = categoryVolumes.getOrDefault(event.getCategory(), 1.0f);
        float finalVolume = masterVol * catVol * event.getDefaultVolume() * volumeMod;

        // Apply subtle pitch variation (±5%) for realism
        float jitter = 0.95f + random.nextFloat() * 0.10f;
        float finalPitch = pitchMod * jitter;

        alSourceStop(source);
        alSourcei(source, AL_BUFFER, bufferId);
        alSourcef(source, AL_GAIN, Math.max(0.0f, Math.min(1.0f, finalVolume)));
        alSourcef(source, AL_PITCH, Math.max(0.2f, Math.min(2.0f, finalPitch)));
        alSource3f(source, AL_POSITION, (float) x, (float) y, (float) z);
        alSourcef(source, AL_ROLLOFF_FACTOR, 1.0f);
        alSourcef(source, AL_REFERENCE_DISTANCE, 4.0f);
        alSourcef(source, AL_MAX_DISTANCE, 48.0f);
        alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);

        alSourcePlay(source);
    }

    /**
     * Plays a non-positional 2D sound effect (e.g. UI clicks, level up).
     */
    public void playSound2D(SoundEvent event, float volumeMod, float pitchMod) {
        if (!initialized || event == null) return;

        int source = findFreeSource();
        if (source == 0) return;

        Integer bufferId = bufferMap.get(event);
        if (bufferId == null) return;

        float masterVol = categoryVolumes.getOrDefault(SoundCategory.MASTER, 1.0f);
        float catVol = categoryVolumes.getOrDefault(event.getCategory(), 1.0f);
        float finalVolume = masterVol * catVol * event.getDefaultVolume() * volumeMod;

        alSourceStop(source);
        alSourcei(source, AL_BUFFER, bufferId);
        alSourcef(source, AL_GAIN, Math.max(0.0f, Math.min(1.0f, finalVolume)));
        alSourcef(source, AL_PITCH, pitchMod);
        alSource3f(source, AL_POSITION, 0, 0, 0);
        alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);

        alSourcePlay(source);
    }

    /**
     * Updates listener position and view orientation vectors.
     */
    public void setListenerOrientation(double x, double y, double z,
                                       float forwardX, float forwardY, float forwardZ,
                                       float upX, float upY, float upZ) {
        if (!initialized) return;

        alListener3f(AL_POSITION, (float) x, (float) y, (float) z);
        float[] orientation = new float[]{forwardX, forwardY, forwardZ, upX, upY, upZ};
        alListenerfv(AL_ORIENTATION, orientation);
    }

    public void setCategoryVolume(SoundCategory category, float volume) {
        if (category != null) {
            categoryVolumes.put(category, Math.max(0.0f, Math.min(1.0f, volume)));
        }
    }

    public float getCategoryVolume(SoundCategory category) {
        return categoryVolumes.getOrDefault(category, 1.0f);
    }

    private int findFreeSource() {
        for (int src : sources) {
            int state = alGetSourcei(src, AL_SOURCE_STATE);
            if (state != AL_PLAYING) {
                return src;
            }
        }
        // Steal the first source if all active
        return sources[0];
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void cleanup() {
        if (!initialized) return;

        for (int src : sources) {
            alSourceStop(src);
            alDeleteSources(src);
        }
        for (int buf : bufferMap.values()) {
            alDeleteBuffers(buf);
        }
        bufferMap.clear();

        if (context != 0) {
            alcMakeContextCurrent(0);
            alcDestroyContext(context);
            context = 0;
        }
        if (device != 0) {
            alcCloseDevice(device);
            device = 0;
        }
        initialized = false;
        LOGGER.info("SoundEngine resources destroyed cleanly.");
    }
}
