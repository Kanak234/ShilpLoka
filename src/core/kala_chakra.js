/**
 * @fileoverview KalaChakra (The Wheel of Time) - Decoupled Dual Game Loop Engine
 * @module core/kala_chakra
 * 
 * Implements a high-performance, decoupled dual game loop inspired by Godot's
 * `_process(delta)` and `_physics_process(delta)` architecture and Glenn Fiedler's
 * "Fix Your Timestep" paradigm.
 * 
 * Architecture:
 * - Fixed Timestep Loop (`_physics_process`): Executes deterministic physics,
 *   collision validation, and spatial updates at exactly 60Hz (16.666ms).
 * - Variable Rendering Loop (`_process`): Synchronizes with screen refresh rates
 *   (60Hz, 120Hz, 144Hz+) using `requestAnimationFrame`, providing an interpolation
 *   factor (alpha) to blend between physical states for buttery-smooth motion.
 * - Prevents the "Spiral of Death" through a clamped accumulator threshold.
 */

/**
 * Represents the execution state of the engine lifecycle.
 * @readonly
 * @enum {string}
 */
export const PranaState = {
  STOPPED: 'STOPPED',
  RUNNING: 'RUNNING',
  PAUSED: 'PAUSED',
};

/**
 * KalaChakra manages time accumulation, fixed updates, and variable rendering.
 */
export class KalaChakra {
  /**
   * Initializes the KalaChakra time wheel with configurable frequency.
   * 
   * @param {Object} [options={}] - Configuration options.
   * @param {number} [options.physicsHz=60] - Target physics ticks per second (default: 60Hz).
   * @param {number} [options.maxAccumulator=0.2] - Max accumulated seconds before dropping ticks.
   */
  constructor(options = {}) {
    /**
     * Target frequency of physics calculations in Hertz.
     * @type {number}
     */
    this.physicsHz = options.physicsHz || 60;

    /**
     * Fixed delta time per physics step in seconds (e.g., 0.016667s for 60Hz).
     * @type {number}
     */
    this.fixedDelta = 1.0 / this.physicsHz;

    /**
     * Maximum time accumulation permitted to prevent the spiral of death during stalls.
     * @type {number}
     */
    this.maxAccumulator = options.maxAccumulator || 0.2;

    /**
     * Unprocessed simulation time accumulator in seconds.
     * @type {number}
     */
    this.accumulator = 0.0;

    /**
     * High-resolution timestamp of the previous frame in milliseconds.
     * @type {number}
     */
    this.lastTimestamp = 0.0;

    /**
     * Current lifecycle state of the time wheel.
     * @type {PranaState}
     */
    this.state = PranaState.STOPPED;

    /**
     * Physics callback executed at fixed intervals.
     * Signature: `(delta: number) => void`
     * @type {Function|null}
     */
    this.onPhysicsProcess = null;

    /**
     * Rendering callback executed every animation frame.
     * Signature: `(delta: number, alpha: number) => void`
     * @type {Function|null}
     */
    this.onProcess = null;

    /**
     * RequestAnimationFrame handle for canceling the loop.
     * @type {number|null}
     */
    this.rafHandle = null;

    // Telemetry and profiling metrics
    this.fps = 60;
    this.physicsFps = 60;
    this.frameCount = 0;
    this.physicsCount = 0;
    this.metricsTimer = 0.0;

    // Bind execution loop to preserve instance context
    this.tick = this.tick.bind(this);
  }

  /**
   * Registers the fixed physics update handler.
   * 
   * @param {function(number): void} callback - The `_physics_process(delta)` handler.
   * @returns {KalaChakra} Current instance for method chaining.
   * @example
   * kalaChakra.registerPhysicsProcess((delta) => {
   *   player.integrate_forces(delta);
   *   collision.validate_bounds();
   * });
   */
  registerPhysicsProcess(callback) {
    if (typeof callback !== 'function') {
      throw new TypeError('KalaChakra.registerPhysicsProcess: callback must be a function.');
    }
    this.onPhysicsProcess = callback;
    return this;
  }

  /**
   * Registers the variable visual rendering handler.
   * 
   * @param {function(number, number): void} callback - The `_process(delta, alpha)` handler.
   * @returns {KalaChakra} Current instance for method chaining.
   * @example
   * kalaChakra.registerProcess((delta, alpha) => {
   *   camera.interpolate_transform(alpha);
   *   renderer.render_scene();
   * });
   */
  registerProcess(callback) {
    if (typeof callback !== 'function') {
      throw new TypeError('KalaChakra.registerProcess: callback must be a function.');
    }
    this.onProcess = callback;
    return this;
  }

  /**
   * Starts the time wheel and begins continuous frame execution.
   */
  start() {
    if (this.state === PranaState.RUNNING) return;
    this.state = PranaState.RUNNING;
    this.lastTimestamp = performance.now();
    this.accumulator = 0.0;
    this.frameCount = 0;
    this.physicsCount = 0;
    this.metricsTimer = 0.0;
    this.rafHandle = requestAnimationFrame(this.tick);
  }

  /**
   * Pauses the loop without resetting accumulated simulation metrics.
   */
  pause() {
    this.state = PranaState.PAUSED;
    if (this.rafHandle) {
      cancelAnimationFrame(this.rafHandle);
      this.rafHandle = null;
    }
  }

  /**
   * Stops the loop entirely and resets temporal metrics.
   */
  stop() {
    this.pause();
    this.state = PranaState.STOPPED;
    this.accumulator = 0.0;
  }

  /**
   * Core frame step executed by `requestAnimationFrame`.
   * Evaluates delta time, processes fixed physics sub-steps, and calls the render update.
   * 
   * @param {DOMHighResTimeStamp} currentTimestamp - High-precision DOM timestamp.
   */
  tick(currentTimestamp) {
    if (this.state !== PranaState.RUNNING) return;

    // Request next frame immediately to ensure smooth vsync cadence
    this.rafHandle = requestAnimationFrame(this.tick);

    // Calculate delta time elapsed since last animation frame (in seconds)
    let frameTime = (currentTimestamp - this.lastTimestamp) / 1000.0;
    this.lastTimestamp = currentTimestamp;

    // Clamp huge frame times (e.g., when browser tab was backgrounded)
    if (frameTime > this.maxAccumulator) {
      frameTime = this.maxAccumulator;
    }

    // Accumulate simulation debt
    this.accumulator += frameTime;

    // --- FIXED TIMESTEP PHYSICS PROCESS ---
    // Drain accumulated time in discrete, deterministic slices of `fixedDelta`
    while (this.accumulator >= this.fixedDelta) {
      if (this.onPhysicsProcess) {
        this.onPhysicsProcess(this.fixedDelta);
      }
      this.accumulator -= this.fixedDelta;
      this.physicsCount++;
    }

    // --- VARIABLE RENDERING PROCESS ---
    // Alpha represents the fractional progress toward the next physics step [0.0, 1.0)
    // Used for sub-frame transform interpolation on 120Hz/144Hz gaming monitors
    const alpha = this.accumulator / this.fixedDelta;

    if (this.onProcess) {
      this.onProcess(frameTime, alpha);
    }
    this.frameCount++;

    // --- REAL-TIME TELEMETRY CALCULATION ---
    this.metricsTimer += frameTime;
    if (this.metricsTimer >= 0.5) {
      this.fps = Math.round(this.frameCount / this.metricsTimer);
      this.physicsFps = Math.round(this.physicsCount / this.metricsTimer);
      this.frameCount = 0;
      this.physicsCount = 0;
      this.metricsTimer = 0.0;
    }
  }

  /**
   * Retrieves high-level temporal performance metrics for debugging and HUD displays.
   * 
   * @returns {{renderFps: number, physicsFps: number, fixedDelta: number, state: PranaState}}
   */
  getTelemetry() {
    return {
      renderFps: this.fps,
      physicsFps: this.physicsFps,
      fixedDelta: this.fixedDelta,
      state: this.state,
    };
  }
}
