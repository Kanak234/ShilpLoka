/**
 * ============================================================================
 * MAIN APPLICATION & GAME LOOP ENTRY POINT
 * ============================================================================
 * Orchestrates Three.js rendering pipeline, initializes all engine subsystems
 * (Procedural World, Physics, First-Person Player, Inventory, Day/Night, Audio,
 * Particles, Persistence), manages event handling, and updates the real-time HUD.
 */

import * as THREE from 'three';
import { PerlinNoise } from './noise.js';
import { TextureManager } from './textures.js';
import { SoundManager } from './audio.js';
import { ParticleManager } from './particles.js';
import { World } from './world.js';
import { PhysicsEngine } from './physics.js';
import { Player } from './player.js';
import { Inventory } from './inventory.js';
import { DayNightCycle } from './daynight.js';
import { SaveSystem } from './save.js';
import { REGISTRY, BLOCKS } from './constants.js';

class GameApp {
  constructor() {
    this.container = document.getElementById('canvas-container');

    // 1. Core Three.js Setup
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(
      75,
      window.innerWidth / window.innerHeight,
      0.1,
      1000
    );

    this.renderer = new THREE.WebGLRenderer({
      antialias: false,
      powerPreference: 'high-performance',
      precision: 'mediump',
      stencil: false,
      depth: true,
    });
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(1.0); // 1:1 pixel ratio eliminates 4K/Retina GPU fill-rate choke
    this.container.appendChild(this.renderer.domElement);

    // 2. Subsystem Instantiation
    this.noise = new PerlinNoise(4242);
    this.textures = new TextureManager();
    this.sound = new SoundManager();
    this.particles = new ParticleManager(this.scene);
    this.world = new World(this.scene, this.noise, this.textures);
    this.physics = new PhysicsEngine(this.world);
    this.player = new Player(
      this.scene,
      this.camera,
      this.world,
      this.physics,
      this.sound,
      this.particles
    );
    this.inventory = new Inventory(this.sound, this.textures);
    this.dayNight = new DayNightCycle(this.scene, 240); // 4 minute full day-night cycle
    this.saveSystem = new SaveSystem(
      this.world,
      this.player,
      this.inventory,
      this.dayNight
    );

    // 3. Connect Event Handlers & Subsystems
    this.player.onOpenCraftingTable = () => {
      this.inventory.openCraftingTable();
    };

    this.setupInteractions();
    this.setupResize();

    // 4. Initial World Generation & Spawn Placement
    this.initSpawn();

    // 5. Game Loop Timing
    this.lastTime = performance.now();
    this.fpsCounter = 0;
    this.fpsTimer = 0;
    this.currentFps = 60;

    // Check for auto-saved game in localStorage
    const hasSave = localStorage.getItem('voxel_game_save_v1');
    if (hasSave) {
      console.log('Restoring previous save from LocalStorage...');
      this.saveSystem.loadFromLocalStorage();
    }

    // Expose for headless verification / testing
    window.__GAME__ = this;

    // Start Main Loop
    this.animate = this.animate.bind(this);
    requestAnimationFrame(this.animate);
  }

  /**
   * Generates initial chunks and places player safely on top of clear open terrain
   */
  initSpawn() {
    // Generate initial chunks around spawn
    this.world.updateStreaming(this.player.position.x, this.player.position.z, true);

    // Search for a clear open grassy hilltop spot without any tree trunk blocking view
    let bestX = 8.5;
    let bestZ = 8.5;
    let bestY = 32;

    for (let radius = 2; radius <= 24; radius += 2) {
      let found = false;
      for (let angle = 0; angle < Math.PI * 2; angle += 0.4) {
        const testX = Math.floor(8 + Math.cos(angle) * radius);
        const testZ = Math.floor(8 + Math.sin(angle) * radius);

        let surfaceY = -1;
        let isTreeNearby = false;

        for (let y = 63; y >= 1; y--) {
          const b = this.world.getBlock(testX, y, testZ);
          if (b === BLOCKS.OAK_LOG || b === BLOCKS.OAK_LEAVES) {
            isTreeNearby = true;
          }
          if (b !== BLOCKS.AIR && b !== BLOCKS.WATER && surfaceY === -1) {
            surfaceY = y;
          }
        }

        // Also check space in front is clear
        const fwdX = testX;
        const fwdZ = testZ - 2;
        const fwdBlock = this.world.getBlock(fwdX, surfaceY + 1, fwdZ);

        if (!isTreeNearby && surfaceY >= 24 && fwdBlock === BLOCKS.AIR) {
          bestX = testX + 0.5;
          bestZ = testZ + 0.5;
          bestY = surfaceY;
          found = true;
          break;
        }
      }
      if (found) break;
    }

    this.player.position.set(bestX, bestY + 1.1, bestZ);
    this.player.pitch = -0.12; // Natural scenic viewing angle
    this.player.yaw = 0.0;
  }

  /**
   * Sets up mouse click and pointer lock events
   */
  setupInteractions() {
    const dom = this.renderer.domElement;

    // Click canvas to acquire pointer lock
    dom.addEventListener('click', () => {
      if (this.inventory.currentMode === 'none') {
        dom.requestPointerLock();
        this.sound.ensureContext();
      }
    });

    // Handle block destruction (Left-Click) and placement (Right-Click)
    window.addEventListener('mousedown', (e) => {
      if (!document.pointerLockElement) return;

      if (e.button === 0) {
        // Left-click: break targeted block
        this.player.breakBlock(this.inventory);
      } else if (e.button === 2) {
        // Right-click: place active block or interact
        this.player.placeBlock(this.inventory);
      }
    });

    // Prevent default context menu during right-click gameplay
    window.addEventListener('contextmenu', (e) => {
      if (document.pointerLockElement) {
        e.preventDefault();
      }
    });
  }

  setupResize() {
    window.addEventListener('resize', () => {
      this.camera.aspect = window.innerWidth / window.innerHeight;
      this.camera.updateProjectionMatrix();
      this.renderer.setSize(window.innerWidth, window.innerHeight);
    });
  }

  /**
   * Updates real-time HUD stats (Coordinates, FPS, Time of Day, Mode, Target)
   */
  updateHUD() {
    const coordsElem = document.getElementById('hud-coords');
    const biomeElem = document.getElementById('hud-chunk');
    const fpsElem = document.getElementById('hud-fps');
    const timeElem = document.getElementById('hud-time');
    const modeElem = document.getElementById('hud-mode');
    const targetElem = document.getElementById('hud-target');

    if (coordsElem) {
      coordsElem.textContent = `XYZ: ${this.player.position.x.toFixed(1)} / ${this.player.position.y.toFixed(1)} / ${this.player.position.z.toFixed(1)}`;
    }
    if (biomeElem) {
      const cx = Math.floor(this.player.position.x / 16);
      const cz = Math.floor(this.player.position.z / 16);
      biomeElem.textContent = `Chunk: [${cx}, ${cz}]`;
    }
    if (fpsElem) {
      fpsElem.textContent = `FPS: ${this.currentFps}`;
    }
    if (timeElem) {
      timeElem.textContent = `Time: ${this.dayNight.getTimeFormatted()}`;
    }
    if (modeElem) {
      modeElem.textContent = `Mode: ${this.player.isFlying ? 'Creative Flight (F)' : 'Survival Walking'}`;
    }
    if (targetElem) {
      if (this.player.targetBlock && this.player.targetBlock.hit) {
        const id = this.player.targetBlock.blockId;
        targetElem.textContent = `Looking at: ${REGISTRY[id]?.name || 'Block'}`;
      } else {
        targetElem.textContent = 'Looking at: Air';
      }
    }
  }

  /**
   * Main game animation frame
   */
  animate() {
    requestAnimationFrame(this.animate);

    const now = performance.now();
    const dt = Math.min((now - this.lastTime) / 1000, 0.1);
    this.lastTime = now;

    // FPS calculation
    this.fpsCounter++;
    this.fpsTimer += dt;
    if (this.fpsTimer >= 0.5) {
      this.currentFps = Math.round(this.fpsCounter / this.fpsTimer);
      this.fpsCounter = 0;
      this.fpsTimer = 0;
    }

    // 1. Update Player Physics & Inputs
    this.player.update(dt, this.inventory);

    // 2. Infinite Chunk Streaming around Player
    this.world.updateStreaming(this.player.position.x, this.player.position.z);

    // 3. Update Day-Night Cycle & Celestial Illumination
    this.dayNight.update(dt, this.player.position);

    // 4. Update Particle Physics
    this.particles.update(dt);

    // 5. Update Auto-Save System
    this.saveSystem.update(dt);

    // 6. Update On-Screen HUD
    this.updateHUD();

    // 7. Render Three.js Scene
    this.renderer.render(this.scene, this.camera);
  }
}

// Bootstrap once DOM content is ready
function bootstrapLegacy() {
  if (window.__GAME__) return;
  new GameApp();
}

if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', bootstrapLegacy);
} else {
  bootstrapLegacy();
}

