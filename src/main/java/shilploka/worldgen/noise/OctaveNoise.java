package shilploka.worldgen.noise;

/**
 * WHY:
 * A single frequency of noise produces monotonous, artificial hills or valleys.
 * Natural topography (Himalayan crags, Gangetic floodplains, Thar sand dunes) is fractal:
 * large continental swells are modulated by medium-scale hills, which in turn are modulated
 * by small-scale surface pebbles and ripples.
 *
 * WHAT:
 * Implements Fractal Brownian Motion (fBm) by summing multiple successive octaves of
 * OpenSimplex2 noise with frequency scaling (lacunarity) and amplitude attenuation (persistence).
 *
 * USED FOR:
 * Generating multi-octave continental terrain heights, biome climate fields, and cave carvers.
 *
 * WHERE NEXT:
 * Queried by BiomeProvider and WorldGenerator during chunk terrain synthesis.
 */
public class OctaveNoise {

    private final OpenSimplex2[] octaves;
    private final double persistence;
    private final double lacunarity;
    private final double maxPossibleAmplitude;

    /**
     * WHY:
     * Constructs a fractal noise generator with a specified number of octaves.
     *
     * WHAT:
     * Seeds each successive octave with a deterministic pseudo-random offset.
     *
     * @param seed        Base 64-bit world seed.
     * @param numOctaves  Number of frequency layers (typically 3 to 6).
     * @param persistence Amplitude decay factor per octave (typically 0.5).
     * @param lacunarity  Frequency growth factor per octave (typically 2.0).
     */
    public OctaveNoise(long seed, int numOctaves, double persistence, double lacunarity) {
        if (numOctaves <= 0) {
            throw new IllegalArgumentException("numOctaves must be at least 1, received: " + numOctaves);
        }
        this.octaves = new OpenSimplex2[numOctaves];
        this.persistence = persistence;
        this.lacunarity = lacunarity;

        double maxAmp = 0.0;
        double amp = 1.0;

        for (int i = 0; i < numOctaves; i++) {
            this.octaves[i] = new OpenSimplex2(seed + i * 49999L);
            maxAmp += amp;
            amp *= persistence;
        }

        this.maxPossibleAmplitude = maxAmp;
    }

    /**
     * WHY:
     * Evaluates multi-octave 2D fractal noise at world coordinates (x, z).
     *
     * WHAT:
     * Accumulates weighted octave samples and normalizes by maxPossibleAmplitude.
     *
     * @return Normalized value in range [-1.0, 1.0].
     */
    public double noise2(double x, double z) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;

        for (OpenSimplex2 octave : this.octaves) {
            total += octave.noise2(x * frequency, z * frequency) * amplitude;
            frequency *= this.lacunarity;
            amplitude *= this.persistence;
        }

        return total / this.maxPossibleAmplitude;
    }

    /**
     * WHY:
     * Evaluates multi-octave 3D fractal noise at coordinates (x, y, z).
     *
     * WHAT:
     * Accumulates weighted 3D noise samples for subterranean cave network carving.
     *
     * @return Normalized value in range [-1.0, 1.0].
     */
    public double noise3(double x, double y, double z) {
        double total = 0.0;
        double frequency = 1.0;
        double amplitude = 1.0;

        for (OpenSimplex2 octave : this.octaves) {
            total += octave.noise3(x * frequency, y * frequency, z * frequency) * amplitude;
            frequency *= this.lacunarity;
            amplitude *= this.persistence;
        }

        return total / this.maxPossibleAmplitude;
    }
}
