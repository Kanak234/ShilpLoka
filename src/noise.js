/**
 * ============================================================================
 * PERLIN & SIMPLEX NOISE GENERATOR
 * ============================================================================
 * Implements seedable coherent procedural noise algorithms from scratch.
 * Includes classic Perlin noise, gradient evaluation with quintic fade curves,
 * and Fractal Brownian Motion (FBM) with multiple octaves for realistic terrain.
 */

export class PerlinNoise {
  /**
   * Initializes permutation table deterministically using a pseudorandom number generator (PRNG)
   * seeded by the given integer seed.
   * @param {number} seed - Integer seed for repeatable world generation
   */
  constructor(seed = 1337) {
    this.seed = seed;
    this.perm = new Uint8Array(512);
    this.grad3 = [
      [1, 1, 0], [-1, 1, 0], [1, -1, 0], [-1, -1, 0],
      [1, 0, 1], [-1, 0, 1], [1, 0, -1], [-1, 0, -1],
      [0, 1, 1], [0, -1, 1], [0, 1, -1], [0, -1, -1],
    ];

    // Seedable linear congruential generator for shuffling
    const p = new Uint8Array(256);
    for (let i = 0; i < 256; i++) {
      p[i] = i;
    }

    let s = (seed ^ 0x6a09e667) >>> 0;
    const lcg = () => {
      s = (Math.imul(1664525, s) + 1013904223) >>> 0;
      return s / 4294967296;
    };

    // Fisher-Yates shuffle
    for (let i = 255; i > 0; i--) {
      const j = Math.floor(lcg() * (i + 1));
      const tmp = p[i];
      p[i] = p[j];
      p[j] = tmp;
    }

    // Duplicate permutation array to avoid modulo operations
    for (let i = 0; i < 512; i++) {
      this.perm[i] = p[i & 255];
    }
  }

  /**
   * Ken Perlin's quintic fade curve: 6t^5 - 15t^4 + 10t^3
   * Has zero 1st and 2nd derivatives at t=0 and t=1, eliminating grid artifacts.
   */
  fade(t) {
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
  }

  /**
   * Linear interpolation between a and b by factor t.
   */
  lerp(t, a, b) {
    return a + t * (b - a);
  }

  /**
   * Dot product between gradient vector and distance vector.
   */
  grad2D(hash, x, y) {
    const h = hash & 7;
    const u = h < 4 ? x : y;
    const v = h < 4 ? y : x;
    return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
  }

  /**
   * Calculates 2D Perlin noise at coordinate (x, y).
   * @param {number} x - X coordinate
   * @param {number} y - Y coordinate
   * @returns {number} Value in range [-1, 1]
   */
  noise2D(x, y) {
    // Find unit grid cell containing point
    const X = Math.floor(x) & 255;
    const Y = Math.floor(y) & 255;

    // Relative coordinates within cell
    const xf = x - Math.floor(x);
    const yf = y - Math.floor(y);

    // Compute fade curves
    const u = this.fade(xf);
    const v = this.fade(yf);

    // Hash coordinates of 4 corners of unit cell
    const aa = this.perm[this.perm[X] + Y];
    const ab = this.perm[this.perm[X] + Y + 1];
    const ba = this.perm[this.perm[X + 1] + Y];
    const bb = this.perm[this.perm[X + 1] + Y + 1];

    // Blend contributions from 4 corners
    const x1 = this.lerp(u, this.grad2D(aa, xf, yf), this.grad2D(ba, xf - 1, yf));
    const x2 = this.lerp(u, this.grad2D(ab, xf, yf - 1), this.grad2D(bb, xf - 1, yf - 1));

    return this.lerp(v, x1, x2);
  }

  /**
   * Calculates 3D Perlin noise at coordinate (x, y, z).
   * Used for 3D cave systems and overhangs.
   */
  noise3D(x, y, z) {
    const X = Math.floor(x) & 255;
    const Y = Math.floor(y) & 255;
    const Z = Math.floor(z) & 255;

    const xf = x - Math.floor(x);
    const yf = y - Math.floor(y);
    const zf = z - Math.floor(z);

    const u = this.fade(xf);
    const v = this.fade(yf);
    const w = this.fade(zf);

    const A = this.perm[X] + Y;
    const AA = this.perm[A] + Z;
    const AB = this.perm[A + 1] + Z;
    const B = this.perm[X + 1] + Y;
    const BA = this.perm[B] + Z;
    const BB = this.perm[B + 1] + Z;

    const grad = (hash, gx, gy, gz) => {
      const g = this.grad3[hash % 12];
      return g[0] * gx + g[1] * gy + g[2] * gz;
    };

    return this.lerp(
      w,
      this.lerp(
        v,
        this.lerp(u, grad(this.perm[AA], xf, yf, zf), grad(this.perm[BA], xf - 1, yf, zf)),
        this.lerp(u, grad(this.perm[AB], xf, yf - 1, zf), grad(this.perm[BB], xf - 1, yf - 1, zf))
      ),
      this.lerp(
        v,
        this.lerp(u, grad(this.perm[AA + 1], xf, yf, zf - 1), grad(this.perm[BA + 1], xf - 1, yf, zf - 1)),
        this.lerp(u, grad(this.perm[AB + 1], xf, yf - 1, zf - 1), grad(this.perm[BB + 1], xf - 1, yf - 1, zf - 1))
      )
    );
  }

  /**
   * Multi-octave Fractal Brownian Motion (FBM) for natural terrain height variation.
   * Sums multiple octaves with decreasing amplitude and increasing frequency.
   * @param {number} x - X coordinate
   * @param {number} y - Y coordinate
   * @param {number} octaves - Number of noise octaves
   * @param {number} persistence - Amplitude multiplier per octave (default 0.5)
   * @param {number} lacunarity - Frequency multiplier per octave (default 2.0)
   * @returns {number} Normalized value in approx [-1, 1]
   */
  fbm2D(x, y, octaves = 4, persistence = 0.5, lacunarity = 2.0) {
    let total = 0.0;
    let frequency = 1.0;
    let amplitude = 1.0;
    let maxValue = 0.0;

    for (let i = 0; i < octaves; i++) {
      total += this.noise2D(x * frequency, y * frequency) * amplitude;
      maxValue += amplitude;
      amplitude *= persistence;
      frequency *= lacunarity;
    }

    return total / maxValue;
  }
}
