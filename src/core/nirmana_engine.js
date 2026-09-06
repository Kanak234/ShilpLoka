/**
 * @fileoverview NirmanaEngine - Master Architectural Coordinator for Ancient Voxel Sandbox
 * @module core/nirmana_engine
 * 
 * Orchestrates:
 * - Three.js WebGL2 native 1:1 pixel ratio rendering pipeline.
 * - KalaChakra decoupled dual loop (Fixed 60Hz physics + VSync rendering with sub-frame alpha lerp).
 * - VastuOctree 3D hierarchical spatial partitioning and sub-millisecond frustum culling.
 * - Procedural Ancient Indian Voxel World (Sindhu Plains, Thar dunes, riverbeds, and Harappan citadels).
 * - VastuGraph Ancient Trade Routes connecting Harappa, Mohenjo-Daro, Lothal, and Pataliputra.
 * - Multi-axis swept AABB physics with 0.6m auto step-up on procedural terrain.
 * - VastuInventory 36-slot system with interactive hotbar and Vedic Crafting Altar (Nirmana Peetha).
 * - Fast Voxel Traversal DDA raycasting with strict Ancient Indian Monument preservation.
 */

import * as THREE from 'three';
import { KalaChakra } from './kala_chakra.js';
import { PranaInput, VastuAction } from './prana_input.js';
import { VastuGrid } from './vastu_grid.js';
import { VastuPhysics } from '../physics/vastu_physics.js';
import { DrishtiCamera } from '../player/drishti_camera.js';
import { YoddhaController } from '../player/yoddha_controller.js';
import { VastuWorld } from '../world/vastu_world.js';
import { VastuBlockId, VASTU_REGISTRY } from '../world/ancient_blocks.js';
import { AncientBiome } from '../world/prithvi_generator.js';
import { VastuInventory, VASTU_ITEMS, VASTU_RECIPES } from '../inventory/vastu_inventory.js';

/**
 * Master engine coordinator managing rendering, world streaming, octree culling, inventory, and physics.
 */
export class NirmanaEngine {
  /**
   * Initializes WebGL context, procedural world, octree, inventory, and decoupled game loops.
   * 
   * @param {string} [containerId='canvas-container'] - DOM container ID for canvas.
   */
  constructor(containerId = 'canvas-container') {
    this.container = document.getElementById(containerId);
    if (!this.container) {
      throw new Error(`NirmanaEngine: Canvas container '#${containerId}' not found.`);
    }

    // 1. Scene and Camera Rig
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(
      75,
      window.innerWidth / window.innerHeight,
      0.1,
      1000
    );

    // 2. High-Performance WebGL2 Renderer (Native 1.0 DPR to prevent fill rate lag)
    this.renderer = new THREE.WebGLRenderer({
      antialias: false,
      powerPreference: 'high-performance',
      precision: 'mediump',
      stencil: false,
      depth: true,
    });
    this.renderer.setSize(window.innerWidth, window.innerHeight);
    this.renderer.setPixelRatio(1.0);
    this.container.appendChild(this.renderer.domElement);

    // 3. Subsystem Core Instances
    this.input = new PranaInput(window);
    this.timeWheel = new KalaChakra({ physicsHz: 60 });
    this.physics = new VastuPhysics({
      gravity: 26.0,
      terminalVelocity: -40.0,
      jumpForce: 8.6,
      stepHeight: 0.6,
      groundFriction: 12.0,
      airFriction: 1.2,
    });

    // 4. Inventory and Vedic Crafting Altar
    this.inventory = new VastuInventory();
    this.isCraftingOpen = false;

    // 5. Procedural Ancient Voxel World with VastuOctree and VastuGraph
    this.world = new VastuWorld(this.scene, { viewDistance: 2, seed: 108 });
    this.physics.setWorldProvider(this.world);

    // Initial terrain surface spawn elevation
    const spawnX = 7.5;
    const spawnZ = 7.5;
    const groundY = this.world.generator.getHeightAt(spawnX, spawnZ);
    const spawnY = Math.max(groundY + 2.0, 24.0);

    // 6. Player and Camera Controllers
    this.yoddha = new YoddhaController({
      spawn: { x: spawnX, y: spawnY, z: spawnZ },
    });
    this.drishti = new DrishtiCamera(this.camera, {
      sensitivity: 0.0022,
      yaw: 0.0,
      pitch: -0.1,
    });

    // Pre-stream initial chunks around spawn
    this.world.updateStreaming(this.yoddha.position);
    this.world.rebuildDirtyMeshes();

    // 7. Atmosphere & Indus Valley Sunlight
    this.initAtmosphere();

    // 8. Build Hotbar UI and Crafting Modal
    this.initHotbarUI();
    this.initCraftingModal();

    // 9. Wire Decoupled Game Loop Callbacks
    this._physics_process = this._physics_process.bind(this);
    this._process = this._process.bind(this);
    this.timeWheel.registerPhysicsProcess(this._physics_process);
    this.timeWheel.registerProcess(this._process);

    // 10. Window Resize Listener
    this.handleResize = this.handleResize.bind(this);
    window.addEventListener('resize', this.handleResize, false);

    // 11. Drishti Pointer Lock
    this.renderer.domElement.addEventListener('click', () => {
      if (!this.isCraftingOpen) {
        this.input.request_drishti_lock(this.renderer.domElement);
      }
    });

    // Expose engine instance for telemetry and programmatic testing
    window.__NIRMANA__ = this;

    // Start simulation time wheel
    this.timeWheel.start();
    console.log('[NirmanaEngine] Step 4 active: Ancient Cities Graph Network & Crafting Altar operational.');
  }

  /**
   * Configures atmospheric lighting and Indus Valley skybox ambience.
   */
  initAtmosphere() {
    const skyColor = new THREE.Color(0x87ceeb);
    this.scene.background = skyColor;
    this.scene.fog = new THREE.FogExp2(0x87ceeb, 0.009);

    this.ambientLight = new THREE.AmbientLight(0xffffff, 0.9);
    this.scene.add(this.ambientLight);

    this.sunLight = new THREE.DirectionalLight(0xfffae6, 1.1);
    this.sunLight.position.set(120, 200, 80);
    this.scene.add(this.sunLight);
  }

  /**
   * Builds the 9-slot Hotbar HUD element.
   */
  initHotbarUI() {
    this.hotbarContainer = document.getElementById('hotbar');
    if (!this.hotbarContainer) return;

    this.hotbarSlotsElems = [];
    for (let i = 0; i < 9; i++) {
      const slotDiv = document.createElement('div');
      slotDiv.className = 'hotbar-slot';
      slotDiv.innerHTML = `
        <span class="slot-key">${i + 1}</span>
        <span class="slot-icon"></span>
        <span class="slot-count"></span>
      `;
      slotDiv.addEventListener('click', () => {
        this.input.selectedHotbarSlot = i;
        this.inventory.selectHotbarSlot(i);
        this.updateHotbarUI();
      });
      this.hotbarContainer.appendChild(slotDiv);
      this.hotbarSlotsElems.push(slotDiv);
    }
    this.updateHotbarUI();
  }

  /**
   * Refreshes Hotbar slot rendering with active items and count badges.
   */
  updateHotbarUI() {
    if (!this.hotbarSlotsElems) return;

    const activeIdx = this.input.selectedHotbarSlot;
    this.inventory.selectHotbarSlot(activeIdx);

    for (let i = 0; i < 9; i++) {
      const slotElem = this.hotbarSlotsElems[i];
      const slotData = this.inventory.slots[i];
      const iconSpan = slotElem.querySelector('.slot-icon');
      const countSpan = slotElem.querySelector('.slot-count');

      if (i === activeIdx) {
        slotElem.classList.add('slot-active');
      } else {
        slotElem.classList.remove('slot-active');
      }

      if (slotData) {
        const meta = VASTU_ITEMS[slotData.itemId];
        iconSpan.textContent = meta ? meta.icon : '📦';
        countSpan.textContent = slotData.count > 1 ? slotData.count : '';
        slotElem.title = meta ? `${meta.name} (${meta.devanagari})` : slotData.itemId;
      } else {
        iconSpan.textContent = '';
        countSpan.textContent = '';
        slotElem.title = 'Empty Slot';
      }
    }
  }

  /**
   * Configures Vedic Crafting Altar Modal and populates recipes.
   */
  initCraftingModal() {
    this.modalElem = document.getElementById('crafting-modal');
    this.recipesListElem = document.getElementById('recipes-list');
    const closeBtn = document.getElementById('modal-close-btn');

    if (closeBtn) {
      closeBtn.addEventListener('click', () => {
        this.toggleCraftingModal(false);
      });
    }

    this.renderRecipes();
  }

  /**
   * Renders the list of ancient recipes in the crafting modal.
   */
  renderRecipes() {
    if (!this.recipesListElem) return;
    this.recipesListElem.innerHTML = '';

    for (const recipe of VASTU_RECIPES) {
      const card = document.createElement('div');
      card.className = 'recipe-card';

      const canCraft = this.inventory.canCraft(recipe);
      const outMeta = VASTU_ITEMS[recipe.output.itemId];
      const inputsStr = recipe.inputs
        .map((inp) => {
          const m = VASTU_ITEMS[inp.itemId];
          const name = m ? m.name : inp.itemId;
          const icon = m ? m.icon : '';
          return `${icon} ${inp.count}x ${name}`;
        })
        .join(' + ');

      card.innerHTML = `
        <div class="recipe-info">
          <span class="recipe-title">${outMeta ? outMeta.icon : ''} ${recipe.name} (${recipe.devanagari}) [x${recipe.output.count}]</span>
          <span class="recipe-ingredients">Requires: ${inputsStr}</span>
        </div>
        <button class="recipe-btn" ${canCraft ? '' : 'disabled'}>Forge (निर्माण)</button>
      `;

      const btn = card.querySelector('.recipe-btn');
      btn.addEventListener('click', () => {
        if (this.inventory.craft(recipe.id)) {
          this.updateHotbarUI();
          this.renderRecipes();
        }
      });

      this.recipesListElem.appendChild(card);
    }
  }

  /**
   * Toggles the crafting altar modal visibility.
   * 
   * @param {boolean} [forceState] - Optional explicit state.
   */
  toggleCraftingModal(forceState) {
    this.isCraftingOpen = (forceState !== undefined) ? forceState : !this.isCraftingOpen;
    if (this.modalElem) {
      if (this.isCraftingOpen) {
        this.modalElem.classList.remove('hidden');
        this.input.release_drishti_lock();
        this.renderRecipes();
      } else {
        this.modalElem.classList.add('hidden');
      }
    }
  }

  /**
   * Fixed Timestep Simulation Process (Deterministic 60Hz).
   * Executes mouse consumption, player locomotion, block interactions, and AABB physics.
   * 
   * @param {number} delta - Fixed time delta (1/60s = 0.016667s).
   */
  _physics_process(delta) {
    // 1. Consume accumulated mouse movement for camera orientation
    const mouseDelta = this.input.consume_mouse_delta();
    if (this.input.isDrishtiLocked && !this.isCraftingOpen && (mouseDelta.x !== 0 || mouseDelta.y !== 0)) {
      this.drishti.applyMouseDelta(mouseDelta.x, mouseDelta.y);
    }

    // 2. Synchronize active hotbar slot
    this.inventory.selectHotbarSlot(this.input.selectedHotbarSlot);

    // 3. Toggle Crafting Altar with <kbd>E</kbd>
    if (this.input.is_action_just_pressed(VastuAction.INVENTORY_TOGGLE)) {
      this.toggleCraftingModal();
    }

    // 4. Advance player entity state and swept AABB physics against procedural voxels
    this.yoddha.update(this.input, this.drishti, this.physics, delta);

    // 5. Block Interaction: Mine Voxel (Left Mouse Button)
    if (this.input.is_action_just_pressed(VastuAction.BREAK_BLOCK) && !this.isCraftingOpen) {
      if (this.world.targetVoxel) {
        const tv = this.world.targetVoxel;
        const broke = this.world.breakBlock(tv.x, tv.y, tv.z);
        if (broke) {
          // Award mined block item to inventory
          const itemKey = Object.keys(VASTU_ITEMS).find((k) => VASTU_ITEMS[k].blockId === tv.blockId);
          if (itemKey) {
            this.inventory.addItem(itemKey, 1);
            this.updateHotbarUI();
          }
        } else {
          this.triggerMonumentAlert(tv.blockId);
        }
      }
    }

    // 6. Block Interaction: Place Voxel (Right Mouse Button)
    if (this.input.is_action_just_pressed(VastuAction.PLACE_BLOCK) && !this.isCraftingOpen) {
      if (this.world.targetVoxel) {
        const held = this.inventory.getSelectedItem();
        if (held && held.meta && held.meta.isBlock) {
          const tv = this.world.targetVoxel;
          const px = tv.x + tv.normal[0];
          const py = tv.y + tv.normal[1];
          const pz = tv.z + tv.normal[2];

          // Ensure block does not intersect player's body
          const playerBox = new THREE.Box3(
            new THREE.Vector3(this.yoddha.position.x - 0.3, this.yoddha.position.y, this.yoddha.position.z - 0.3),
            new THREE.Vector3(this.yoddha.position.x + 0.3, this.yoddha.position.y + this.yoddha.dimensions.currentHeight, this.yoddha.position.z + 0.3)
          );
          const blockBox = new THREE.Box3(
            new THREE.Vector3(px, py, pz),
            new THREE.Vector3(px + 1, py + 1, pz + 1)
          );

          if (!playerBox.intersectsBox(blockBox)) {
            const placed = this.world.setBlock(px, py, pz, held.meta.blockId);
            if (placed && !this.yoddha.isFlying) {
              this.inventory.consumeSelectedItem(1);
              this.updateHotbarUI();
            }
          }
        }
      }
    }
  }

  /**
   * Variable Frame Rendering Process (VSync Synchronized).
   * Executes chunk streaming, mesh rebuilding, sub-frame camera lerp,
   * Octree frustum culling, DDA raycasting, WebGL rendering, and HUD telemetry.
   * 
   * @param {number} delta - Frame delta time in seconds.
   * @param {number} alpha - Fixed-step interpolation factor [0.0, 1.0).
   */
  _process(delta, alpha) {
    // 1. Stream procedural chunks around player's current position
    this.world.updateStreaming(this.yoddha.position);

    // 2. Rebuild any modified chunk meshes
    this.world.rebuildDirtyMeshes();

    // 3. Sub-frame camera interpolation and dynamic biometric head-bobbing
    this.drishti.update(this.yoddha, delta, alpha);

    // 4. VastuOctree Hierarchical Frustum Culling
    this.world.updateFrustumCulling(this.camera);

    // 5. Fast Voxel Traversal DDA Raycasting for Target Highlighting
    const lookDir = this.drishti.getLookDirection();
    this.world.updateTargetHighlight(this.camera.position, lookDir);

    // 6. Draw WebGL Frame
    this.renderer.render(this.scene, this.camera);

    // 7. Synchronize Vastu Telemetry HUD
    this.updateHUD();

    // 8. Flush frame-specific action edges
    this.input.flush_frame_edges();
  }

  /**
   * Displays temporary UI banner when attempting to break an indestructible heritage monument.
   * 
   * @param {number} blockId - Monument block ID.
   */
  triggerMonumentAlert(blockId) {
    const meta = VASTU_REGISTRY[blockId];
    const alertElem = document.getElementById('monument-alert');
    if (alertElem && meta) {
      alertElem.textContent = `🏛️ संरक्षित प्राचीन धरोहर: ${meta.name} (${meta.devanagari}) cannot be destroyed!`;
      alertElem.style.opacity = '1';
      clearTimeout(this._alertTimeout);
      this._alertTimeout = setTimeout(() => {
        alertElem.style.opacity = '0';
      }, 2500);
    }
  }

  /**
   * Synchronizes performance telemetry, graph trade routes, and inventory status on the HUD.
   */
  updateHUD() {
    const telemetry = this.timeWheel.getTelemetry();
    const fpsElem = document.getElementById('hud-fps');
    const physFpsElem = document.getElementById('hud-physics-fps');
    const coordsElem = document.getElementById('hud-coords');
    const vastuElem = document.getElementById('hud-vastu');
    const groundElem = document.getElementById('hud-grounded');
    const octreeElem = document.getElementById('hud-octree');
    const targetElem = document.getElementById('hud-target');
    const biomeElem = document.getElementById('hud-biome');
    const tradeElem = document.getElementById('hud-trade');
    const heldElem = document.getElementById('hud-held');

    if (fpsElem) fpsElem.textContent = `Render FPS: ${telemetry.renderFps}`;
    if (physFpsElem) physFpsElem.textContent = `Physics: ${telemetry.physicsFps} Hz (Fixed 60Hz)`;

    const pos = this.yoddha.position;

    if (coordsElem) {
      coordsElem.textContent = `Vastu XYZ: ${pos.x.toFixed(2)} / ${pos.y.toFixed(2)} / ${pos.z.toFixed(2)}`;
    }

    if (vastuElem) {
      const facing = VastuGrid.get_facing_vastu_direction(this.drishti.yaw);
      vastuElem.textContent = `Facing: ${facing}`;
    }

    if (groundElem) {
      groundElem.textContent = `Bhoomi (Ground): ${this.yoddha.isGrounded ? 'Contact (Yes)' : 'Airborne (No)'}`;
      groundElem.style.color = this.yoddha.isGrounded ? '#2ecc71' : '#f39c12';
    }

    // Octree Telemetry
    if (octreeElem) {
      const stats = this.world.octreeStats;
      octreeElem.textContent = `Octree Frustum: ${stats.visible}/${stats.total} Chunks (${stats.efficiencyPercent}% Culled)`;
      octreeElem.style.color = stats.culled > 0 ? '#2ecc71' : '#deb887';
    }

    // Target Voxel & Preservation State
    if (targetElem) {
      if (this.world.targetVoxel) {
        const tv = this.world.targetVoxel;
        const meta = VASTU_REGISTRY[tv.blockId];
        const name = meta ? `${meta.name} (${meta.devanagari})` : `Voxel #${tv.blockId}`;
        const preserved = meta && meta.isMonument ? ' [PROTECTED MONUMENT]' : '';
        targetElem.textContent = `Gaze: ${name} at (${tv.x},${tv.y},${tv.z})${preserved}`;
        targetElem.style.color = meta && meta.isMonument ? '#e74c3c' : '#d4af37';
      } else {
        targetElem.textContent = 'Gaze: Sky / Distant Horizon';
        targetElem.style.color = '#888888';
      }
    }

    // Biome & City Detection
    if (biomeElem) {
      const city = this.world.generator.graph.getCityAt(pos.x, pos.z);
      if (city) {
        biomeElem.textContent = `Location: ${city.name} (${city.devanagari}) • ${city.monument}`;
        biomeElem.style.color = '#d4af37';
      } else {
        const b = this.world.generator.getBiomeAt(pos.x, pos.z);
        let bName = 'Sindhu Plains (सिन्धु कछार)';
        if (b === AncientBiome.THAR_DESERT) bName = 'Thar Desert (मरुभूमि)';
        if (b === AncientBiome.HARAPPA_CITADEL) bName = 'Harappa Citadel (हड़प्पा शैल)';
        biomeElem.textContent = `Biome: ${bName}`;
        biomeElem.style.color = '#f7ede2';
      }
    }

    // Trade Route Detection
    if (tradeElem) {
      const road = this.world.generator.graph.getRoadAt(pos.x, pos.z);
      if (road) {
        tradeElem.textContent = `Route: Ancient ${road.routeType} (Paved Highway)`;
        tradeElem.style.color = '#2ecc71';
      } else {
        tradeElem.textContent = 'Route: Harappa ⇄ Mohenjo-Daro ⇄ Lothal ⇄ Pataliputra';
        tradeElem.style.color = '#deb887';
      }
    }

    // Held Active Item
    if (heldElem) {
      const held = this.inventory.getSelectedItem();
      if (held && held.meta) {
        heldElem.textContent = `Held: ${held.meta.name} (${held.meta.devanagari}) [x${held.count}]`;
      } else {
        heldElem.textContent = 'Held: Empty Hands';
      }
    }
  }

  /**
   * Adapts camera aspect ratio and renderer dimensions upon window resize.
   */
  handleResize() {
    this.camera.aspect = window.innerWidth / window.innerHeight;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(window.innerWidth, window.innerHeight);
  }

  /**
   * Cleanly disposes engine resources and terminates loops.
   */
  destroy() {
    this.timeWheel.stop();
    this.input.unbind_listeners();
    window.removeEventListener('resize', this.handleResize);
    this.renderer.dispose();
  }
}
