/**
 * Tests for KalaChakra: src/core/kala_chakra.js
 */
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { KalaChakra, PranaState } from '../src/core/kala_chakra.js';

describe('KalaChakra', () => {
  let chakra;

  beforeEach(() => {
    global.requestAnimationFrame = vi.fn(() => 123);
    global.cancelAnimationFrame = vi.fn();
    chakra = new KalaChakra({ physicsHz: 60, maxAccumulator: 0.2 });
  });

  afterEach(() => {
    chakra.stop();
  });

  it('initializes with correct defaults', () => {
    expect(chakra.physicsHz).toBe(60);
    expect(chakra.fixedDelta).toBeCloseTo(1 / 60);
    expect(chakra.maxAccumulator).toBe(0.2);
    expect(chakra.state).toBe(PranaState.STOPPED);
    expect(chakra.accumulator).toBe(0);
  });

  it('registers physics and process callbacks', () => {
    const physicsFn = vi.fn();
    const renderFn = vi.fn();

    expect(chakra.registerPhysicsProcess(physicsFn)).toBe(chakra);
    expect(chakra.registerProcess(renderFn)).toBe(chakra);

    expect(() => chakra.registerPhysicsProcess('not a function')).toThrow(TypeError);
    expect(() => chakra.registerProcess('not a function')).toThrow(TypeError);
  });

  it('executes tick and drains accumulator in fixed steps', () => {
    const physicsSteps = [];
    chakra.registerPhysicsProcess((delta) => {
      physicsSteps.push(delta);
    });

    const renderCalls = [];
    chakra.registerProcess((delta, alpha) => {
      renderCalls.push({ delta, alpha });
    });

    chakra.state = PranaState.RUNNING;
    chakra.lastTimestamp = 1000;

    // Simulate 35ms passing (~2 physics steps at 16.66ms)
    chakra.tick(1035);

    expect(physicsSteps.length).toBe(2);
    expect(renderCalls.length).toBe(1);
    expect(renderCalls[0].delta).toBeCloseTo(0.035);
  });

  it('clamps huge frame times to maxAccumulator', () => {
    let accumulated = 0;
    chakra.registerPhysicsProcess((delta) => {
      accumulated += delta;
    });

    chakra.state = PranaState.RUNNING;
    chakra.lastTimestamp = 1000;

    // Simulate 5000ms pause (e.g. background tab)
    chakra.tick(6000);

    // Accumulated should not exceed maxAccumulator (0.2s)
    expect(accumulated).toBeLessThanOrEqual(0.2 + 0.02);
  });

  it('provides telemetry metrics', () => {
    const telemetry = chakra.getTelemetry();
    expect(telemetry).toHaveProperty('renderFps');
    expect(telemetry).toHaveProperty('physicsFps');
    expect(telemetry).toHaveProperty('fixedDelta');
    expect(telemetry).toHaveProperty('state');
  });

  it('controls lifecycle states (start, pause, stop)', () => {
    chakra.start();
    expect(chakra.state).toBe(PranaState.RUNNING);

    chakra.pause();
    expect(chakra.state).toBe(PranaState.PAUSED);
    expect(global.cancelAnimationFrame).toHaveBeenCalledWith(123);

    chakra.stop();
    expect(chakra.state).toBe(PranaState.STOPPED);
  });
});
