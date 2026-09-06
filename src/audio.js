/**
 * ============================================================================
 * PROCEDURAL AUDIO SYNTHESIZER
 * ============================================================================
 * Generates all sound effects using the Web Audio API with zero external assets.
 * Synthesizes block footsteps, breaking crunches, placement clicks, jump whooshes,
 * and crafting fanfares dynamically with customizable pitch and envelope shaping.
 */

export class SoundManager {
  constructor() {
    this.ctx = null;
    this.muted = false;
    this.masterGain = null;
    this.lastFootstepTime = 0;
  }

  /**
   * Initializes the AudioContext upon first user interaction (pointer lock or click)
   * to satisfy modern browser autoplay policies.
   */
  init() {
    if (this.ctx) return;
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    this.ctx = new AudioCtx();
    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.setValueAtTime(0.4, this.ctx.currentTime);
    this.masterGain.connect(this.ctx.destination);
  }

  ensureContext() {
    if (!this.ctx) {
      this.init();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  toggleMute() {
    this.muted = !this.muted;
    if (this.masterGain && this.ctx) {
      this.masterGain.gain.setValueAtTime(this.muted ? 0 : 0.4, this.ctx.currentTime);
    }
    return this.muted;
  }

  /**
   * Generates a buffer of white noise for percussion and crunch effects.
   */
  createNoiseBuffer(duration = 0.2) {
    if (!this.ctx) return null;
    const bufferSize = Math.floor(this.ctx.sampleRate * duration);
    const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
    const output = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      output[i] = Math.random() * 2 - 1;
    }
    return buffer;
  }

  /**
   * Block footstep sound effect modulated by block sound category
   */
  playFootstep(soundType = 'grass') {
    const now = performance.now();
    if (now - this.lastFootstepTime < 320) return; // Debounce footsteps
    this.lastFootstepTime = now;
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.08);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'bandpass';

    let freq = 800;
    let Q = 1.0;
    if (soundType === 'grass') { freq = 650; Q = 0.8; }
    else if (soundType === 'stone') { freq = 1200; Q = 2.0; }
    else if (soundType === 'wood') { freq = 450; Q = 1.5; }
    else if (soundType === 'sand') { freq = 900; Q = 0.5; }

    filter.frequency.setValueAtTime(freq + (Math.random() - 0.5) * 150, this.ctx.currentTime);
    filter.Q.setValueAtTime(Q, this.ctx.currentTime);

    const gain = this.ctx.createGain();
    const t = this.ctx.currentTime;
    gain.gain.setValueAtTime(0.15, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.08);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain);

    noise.start(t);
    noise.stop(t + 0.08);
  }

  /**
   * Block break sound: crunchy explosive burst with decaying low-pass filter
   */
  playBreak(soundType = 'dirt') {
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const noise = this.ctx.createBufferSource();
    noise.buffer = this.createNoiseBuffer(0.18);

    const filter = this.ctx.createBiquadFilter();
    filter.type = 'lowpass';
    const t = this.ctx.currentTime;
    filter.frequency.setValueAtTime(soundType === 'stone' ? 1800 : 900, t);
    filter.frequency.exponentialRampToValueAtTime(150, t + 0.18);

    const gain = this.ctx.createGain();
    gain.gain.setValueAtTime(0.35, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.18);

    noise.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain);

    noise.start(t);
    noise.stop(t + 0.18);
  }

  /**
   * Block place sound: snappy pop thud
   */
  playPlace() {
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    const t = this.ctx.currentTime;

    osc.type = 'triangle';
    osc.frequency.setValueAtTime(180, t);
    osc.frequency.exponentialRampToValueAtTime(60, t + 0.09);

    gain.gain.setValueAtTime(0.3, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.09);

    osc.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc.stop(t + 0.09);
  }

  /**
   * Jump sound: uplifting frequency whoosh
   */
  playJump() {
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    const t = this.ctx.currentTime;

    osc.type = 'sine';
    osc.frequency.setValueAtTime(150, t);
    osc.frequency.exponentialRampToValueAtTime(320, t + 0.12);

    gain.gain.setValueAtTime(0.2, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.12);

    osc.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc.stop(t + 0.12);
  }

  /**
   * Crafting triumph sound: 3-note arpeggiated chime
   */
  playCraft() {
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const notes = [523.25, 659.25, 783.99]; // C5, E5, G5
    notes.forEach((freq, idx) => {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      const t = this.ctx.currentTime + idx * 0.08;

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, t);

      gain.gain.setValueAtTime(0.25, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.22);

      osc.connect(gain);
      gain.connect(this.masterGain);

      osc.start(t);
      osc.stop(t + 0.22);
    });
  }

  /**
   * UI Click sound for inventory item pick/drop
   */
  playClick() {
    this.ensureContext();
    if (this.muted || !this.ctx) return;

    const osc = this.ctx.createOscillator();
    const gain = this.ctx.createGain();
    const t = this.ctx.currentTime;

    osc.type = 'square';
    osc.frequency.setValueAtTime(900, t);

    gain.gain.setValueAtTime(0.08, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.04);

    osc.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc.stop(t + 0.04);
  }
}
