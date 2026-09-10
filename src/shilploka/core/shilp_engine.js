/**
 * @fileoverview ShilpEngine - Master Cross-Platform Engine Coordinator for ShilpLoka
 * @module shilploka/core/shilp_engine
 * 
 * Architecture:
 * - Scalable Entity Component System (ShilpECS) orchestrating all game logic.
 * - Decoupled Glenn Fiedler dual game loop (KalaChakra) with locked 60Hz physics.
 * - Native 1:1 DPR WebGL2 rendering pipeline with Three.js.
 * - Clean cross-platform Tauri desktop packaging hooks.
 * - Pure clean-room implementation: all code and assets are original.
 */

import * as THREE from 'three';
import { KalaChakra } from '../../core/kala_chakra.js';
import { PranaInput } from '../../core/prana_input.js';
import { ShilpECS } from '../ecs/ecs_registry.js';
import {
  TransformComponent,
  KinematicsComponent,
  PlayerInputComponent,
  CameraRigComponent,
  RenderableComponent,
  HeritageTagComponent,
  BarterNPCComponent,
  FaunaComponent,
} from '../ecs/components.js';
import { InputDispatchSystem } from '../ecs/systems/input_system.js';
import { MovementKinematicsSystem } from '../ecs/systems/movement_system.js';
import { VoxelPhysicsSystem } from '../ecs/systems/voxel_physics_system.js';
import { CameraInterpolationSystem } from '../ecs/systems/camera_system.js';
import { TelemetrySystem } from '../ecs/systems/telemetry_system.js';
import { ShilpLearningCenter } from '../ui/shilp_learning_center.js';
import { ShilpBarterModal } from '../ui/shilp_barter_modal.js';
import { ShilpCraftingModal } from '../ui/shilp_crafting_modal.js';
import { ShilpInventory, SHILP_ITEMS } from '../inventory/shilp_inventory.js';
import { ShilpWorld } from '../world/shilp_world.js';
import { ShilpBlockId } from '../world/voxel_constants.js';
import { isMobileDevice, VirtualTouchControls } from '../input/virtual_joystick.js';
import { ShilpSaveStore, randomSeed } from './shilp_save.js';


export class ShilpEngine {
  /**
   * Initializes WebGL2 renderer, ECS architecture, and starts simulation.
   * 
   * @param {string} [containerId='canvas-container'] - DOM element ID for canvas mounting.
   */
  constructor(containerId = 'canvas-container') {
    this.container = document.getElementById(containerId);
    if (!this.container) {
      throw new Error(`ShilpEngine: Container '#${containerId}' not found.`);
    }

    // 1. Three.js Scene and Camera
    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(
      75,
      window.innerWidth / window.innerHeight,
      0.1,
      1000
    );

    // 2. High-Performance WebGL2 Renderer (Native 1.0 DPR to prevent high-DPI GPU lag)
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
    this.ecs = new ShilpECS();
    this.inventory = new ShilpInventory();
    this.merchantStock = null;
    this.merchantId = null;

    // 3b. SAVE SYSTEM - must run BEFORE the world is created.
    // WHY the ordering: the world regenerates terrain from its seed, so it has
    // to be built with the SAVED seed to reproduce the same land the player's
    // edits were made on. The save store also has to exist first so each chunk
    // can have its edits re-applied the moment it is generated.
    // NEXT: the loaded save is used again in step 6b to restore the player,
    // inventory and merchant, once those objects exist.
    this.saveStore = new ShilpSaveStore();
    this.loadedSave = this.saveStore.load();
    const worldSeed = this.loadedSave?.seed ?? randomSeed();

    // 4. Procedural Voxel World (streaming, greedy meshing & Ancient Indian biomes)
    this.world = new ShilpWorld(this.scene, {
      viewDistance: 2,
      seed: worldSeed,
      saveStore: this.saveStore,
      // Resume streaming where the player actually is, not at the origin.
      initialCenter: this.loadedSave?.player
        ? { x: this.loadedSave.player.x, z: this.loadedSave.player.z }
        : { x: 0, z: 0 },
    });
    this.cullingMetrics = null;

    // 5. Initial Indus Valley Atmosphere & Lighting
    this.initAtmosphere();

    // 6. Spawn Foundational Entities
    this.spawnPlayerEntity();
    this.spawnDemonstrationHeritageMonuments();
    this.spawnDemonstrationFaunaAndNPCs();

    // 6b. Restore saved player / inventory / merchant state.
    // WHY here: these objects only exist after step 6 spawned them.
    this._restoreFromSave(this.loadedSave);

    // 7. Register ECS Systems
    this.registerSystems();

    // 8. Wire Decoupled Dual Loop Callbacks
    this._physics_process = this._physics_process.bind(this);
    this._process = this._process.bind(this);
    this.timeWheel.registerPhysicsProcess(this._physics_process);
    this.timeWheel.registerProcess(this._process);

    // 9. Window Resize Listener
    this.handleResize = this.handleResize.bind(this);
    window.addEventListener('resize', this.handleResize, false);

    // 10. Drishti Pointer Lock
    this.renderer.domElement.addEventListener('click', () => {
      this.input.request_drishti_lock(this.renderer.domElement);
    });

    // 11. Desi Learning Center Modal ("The Teacher")
    const learningModal = document.getElementById('learning-modal');
    if (learningModal) {
      this.learningCenter = new ShilpLearningCenter(learningModal);
      const toggleBtn = document.getElementById('learning-toggle-btn');
      if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
          this.learningCenter.toggle();
        });
      }
    }

    // 12. Barter Trading Modal (Dhanapati the Merchant)
    const barterModal = document.getElementById('barter-modal');
    if (barterModal) {
      this.barterModal = new ShilpBarterModal(barterModal, this.inventory, this.merchantStock);
      const barterToggleBtn = document.getElementById('barter-toggle-btn');
      if (barterToggleBtn) {
        barterToggleBtn.addEventListener('click', () => {
          this.barterModal.toggle();
        });
      }
    }

    // 13. Vedic Crafting Altar Modal (Nirmana Peetha)
    const craftingModal = document.getElementById('crafting-modal');
    if (craftingModal) {
      this.craftingModal = new ShilpCraftingModal(craftingModal, this.inventory);
      const craftingToggleBtn = document.getElementById('crafting-toggle-btn');
      if (craftingToggleBtn) {
        craftingToggleBtn.addEventListener('click', () => {
          this.craftingModal.toggle();
        });
      }
    }

    // 14. Hotbar Rendering & Keyboard Selection
    this.renderHotbar();
    this.handleKeyDown = (e) => {
      if (['INPUT', 'SELECT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;

      if (e.code === 'KeyB') {
        if (this.barterModal) this.barterModal.toggle();
      } else if (e.code === 'KeyC') {
        if (this.craftingModal) this.craftingModal.toggle();
      } else if (e.code === 'KeyH') {
        if (this.learningCenter) this.learningCenter.toggle();
      } else if (e.key >= '1' && e.key <= '9') {
        const slotIdx = parseInt(e.key, 10) - 1;
        this.inventory.setActiveSlot(slotIdx);
        this.renderHotbar();
      }
    };
    window.addEventListener('keydown', this.handleKeyDown, false);

    // 15. Voxel Interaction & Indestructible Monument Preservation Shield
    this.monumentAlert = document.getElementById('monument-alert');
    this.alertTimer = null;

    this.renderer.domElement.addEventListener('mousedown', (e) => {
      if (!this.input.isDrishtiLocked && !this.input.isTouchMode) return;

      if (e.button === 0) {
        this.tryMineTargetVoxel();
      } else if (e.button === 2) {
        this.tryPlaceActiveVoxel();
      }
    });

    this.renderer.domElement.addEventListener('contextmenu', (e) => e.preventDefault());

    // 16. Mobile Detection & Virtual Touch Controls Auto-Injection
    this.touchControls = null;
    if (isMobileDevice()) {
      this.enableTouchControls();
    }

    // 17. Autosave, New World button and the save toast.
    this._initSaveSystem();

    // Expose engine instance for telemetry and programmatic testing
    window.__SHILPLOKA__ = this;

    // Start simulation loop
    this.timeWheel.start();
    console.log('[ShilpEngine] ShilpLoka Step 3 initialized: Graph Cities, Barter & Crafting active.');
  }

  /**
   * Attempts to mine the targeted voxel with indestructible monument preservation validation.
   * 
   * @returns {{ success: boolean, wasHeritage: boolean, blockName: string, blockId: number }}
   */
  tryMineTargetVoxel() {
    const result = this.world.tryBreakTargetVoxel();
    if (result.wasHeritage) {
      this.triggerMonumentAlert(result.blockName);
    } else if (result.success && result.blockId) {
      const itemKey = Object.keys(SHILP_ITEMS).find(k => SHILP_ITEMS[k].blockId === result.blockId);
      if (itemKey) {
        this.inventory.addItem(itemKey, 1);
        this.renderHotbar();
      }
    }
    return result;
  }

  /**
   * Places the currently selected hotbar block in the world.
   * 
   * @returns {boolean} True if successfully placed.
   */
  tryPlaceActiveVoxel() {
    const activeSlot = this.inventory.getActiveSlot();
    if (activeSlot) {
      const itemMeta = SHILP_ITEMS[activeSlot.itemId];
      if (itemMeta && itemMeta.isBlock && itemMeta.blockId) {
        const placed = this.world.tryPlaceAdjacentVoxel(itemMeta.blockId);
        if (placed) {
          this.inventory.consumeItem(activeSlot.itemId, 1);
          this.renderHotbar();
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Manually enables on-screen virtual touch controls (joystick + action buttons).
   */
  enableTouchControls() {
    if (!this.touchControls) {
      this.touchControls = new VirtualTouchControls(this.input, this);
    }
  }

  /**
   * Displays the indestructible heritage protection shield banner.
   * 
   * @param {string} monumentName - Name of protected monument.
   */
  triggerMonumentAlert(monumentName) {
    if (!this.monumentAlert) return;
    this.monumentAlert.textContent = `☸ अक्षत धरोहर: ${monumentName} cannot be destroyed! (Preservation Invariant Enforced)`;
    this.monumentAlert.classList.remove('hidden');
    if (this.alertTimer) clearTimeout(this.alertTimer);
    this.alertTimer = setTimeout(() => {
      this.monumentAlert.classList.add('hidden');
    }, 2800);
  }


  /**
   * Configures base atmospheric lighting and Indus Valley skybox backdrop.
   */
  initAtmosphere() {
    const skyColor = new THREE.Color(0x87ceeb);
    this.scene.background = skyColor;
    this.scene.fog = new THREE.FogExp2(0x87ceeb, 0.008);

    this.ambientLight = new THREE.AmbientLight(0xffffff, 0.9);
    this.scene.add(this.ambientLight);

    this.sunLight = new THREE.DirectionalLight(0xfffae6, 1.15);
    this.sunLight.position.set(100, 200, 100);
    this.scene.add(this.sunLight);
  }

  /**
   * Spawns the primary player entity inside the ECS registry.
   */
  spawnPlayerEntity() {
    this.playerEntityId = this.ecs.createEntity();

    // Spawns elevated on top of Harappan Citadel terrace (Y=26.0)
    this.ecs.addComponent(this.playerEntityId, new TransformComponent(4.0, 26.0, 4.0));
    this.ecs.addComponent(this.playerEntityId, new KinematicsComponent());
    this.ecs.addComponent(this.playerEntityId, new PlayerInputComponent());
    this.ecs.addComponent(this.playerEntityId, new CameraRigComponent(this.camera, {
      yaw: 0.0,
      pitch: -0.1,
      sensitivity: 0.0022,
    }));
  }


  /**
   * Spawns Ancient Indian Harappan monument entities protected by HeritageTagComponent.
   */
  spawnDemonstrationHeritageMonuments() {
    // 1. Terracotta Stepped Citadel Courtyard Foundation (Y=0 to 1)
    const foundationMesh = new THREE.Mesh(
      new THREE.BoxGeometry(40, 1, 40),
      new THREE.MeshLambertMaterial({ color: 0x8b4513 })
    );
    foundationMesh.position.set(0, 0.5, 0);
    this.scene.add(foundationMesh);

    const foundationId = this.ecs.createEntity();
    this.ecs.addComponent(foundationId, new TransformComponent(0, 0.5, 0));
    this.ecs.addComponent(foundationId, new RenderableComponent(foundationMesh));
    this.ecs.addComponent(foundationId, new HeritageTagComponent('Harappan Foundation Plaza'));

    // 2. Central Ashoka Sthambha Pillar Base & Column
    const pillarGroup = new THREE.Group();
    const baseMesh = new THREE.Mesh(
      new THREE.BoxGeometry(4, 1.5, 4),
      new THREE.MeshLambertMaterial({ color: 0xdeb887 })
    );
    baseMesh.position.set(0, 1.75, 0);
    pillarGroup.add(baseMesh);

    const shaftMesh = new THREE.Mesh(
      new THREE.CylinderGeometry(0.7, 0.9, 7, 16),
      new THREE.MeshLambertMaterial({ color: 0xc89d66 })
    );
    shaftMesh.position.set(0, 6.0, 0);
    pillarGroup.add(shaftMesh);

    this.scene.add(pillarGroup);

    const pillarEntityId = this.ecs.createEntity();
    this.ecs.addComponent(pillarEntityId, new TransformComponent(0, 0, 0));
    this.ecs.addComponent(pillarEntityId, new RenderableComponent(pillarGroup));
    this.ecs.addComponent(pillarEntityId, new HeritageTagComponent('Ashoka Sthambha Pillar', 'MAURYAN_PILLAR'));

    // 3. Ancient Stepped Ghat Steps (0.5m risers for auto step-up testing)
    const stepsGroup = new THREE.Group();
    const terracottaMat = new THREE.MeshLambertMaterial({ color: 0xba5d3f });
    for (let i = 0; i < 4; i++) {
      const stepMesh = new THREE.Mesh(
        new THREE.BoxGeometry(8, 0.5 * (i + 1), 1.5),
        terracottaMat
      );
      stepMesh.position.set(0, 1.0 + (0.5 * (i + 1)) / 2, -4.0 - i * 1.5);
      stepsGroup.add(stepMesh);
    }
    this.scene.add(stepsGroup);

    const stepsEntityId = this.ecs.createEntity();
    this.ecs.addComponent(stepsEntityId, new TransformComponent(0, 0, 0));
    this.ecs.addComponent(stepsEntityId, new RenderableComponent(stepsGroup));
    this.ecs.addComponent(stepsEntityId, new HeritageTagComponent('Harappan Stepped Ghat'));
  }

  /**
   * Spawns demonstration Barter Merchant NPC and Native Fauna entities into ECS.
   */
  spawnDemonstrationFaunaAndNPCs() {
    // 1. Barter NPC Merchant: Dhanapati (धनपति)
    const merchantGroup = new THREE.Group();
    const body = new THREE.Mesh(
      new THREE.BoxGeometry(0.7, 1.4, 0.5),
      new THREE.MeshLambertMaterial({ color: 0xd4af37 })
    );
    body.position.set(0, 1.7, 0);
    merchantGroup.add(body);
    merchantGroup.position.set(5.0, 0, 3.0);
    this.scene.add(merchantGroup);

    const merchantStock = {
      cardamom: 40,
      pepper: 80,
      saffron: 15,
      bronze: 25,
    };
    this.merchantStock = merchantStock;

    const merchantId = this.ecs.createEntity();
    this.merchantId = merchantId;
    this.ecs.addComponent(merchantId, new TransformComponent(5.0, 1.0, 3.0));
    this.ecs.addComponent(merchantId, new RenderableComponent(merchantGroup));
    this.ecs.addComponent(merchantId, new BarterNPCComponent('Dhanapati the Merchant (धनपति)', merchantStock));

    // 2. Native Subcontinent Fauna: Sacred Gaja / Elephant Entity
    const elephantId = this.ecs.createEntity();
    this.ecs.addComponent(elephantId, new TransformComponent(-8.0, 1.0, -2.0));
    this.ecs.addComponent(elephantId, new FaunaComponent('GAJA_ELEPHANT'));

    // 3. Native Subcontinent Fauna: Nandi / Zebu Bull Entity
    const zebuId = this.ecs.createEntity();
    this.ecs.addComponent(zebuId, new TransformComponent(-6.0, 1.0, 5.0));
    this.ecs.addComponent(zebuId, new FaunaComponent('ZEBU_CATTLE'));
  }

  /**
   * Registers foundational ECS execution systems.
   */
  registerSystems() {
    this.ecs.registerSystem(new InputDispatchSystem(this.input));
    this.ecs.registerSystem(new VoxelPhysicsSystem(this.world));
    this.ecs.registerSystem(new CameraInterpolationSystem());
    this.ecs.registerSystem(new TelemetrySystem(this.ecs, this.timeWheel, this.playerEntityId, this));
  }

  /**
   * Fixed Timestep Simulation Process (Deterministic 60Hz).
   * 
   * @param {number} delta - Fixed time delta (1/60s).
   */
  _physics_process(delta) {
    this.ecs.updateFixed(delta);
  }

  /**
   * Variable Frame Rendering Process (VSync Synchronized).
   * 
   * @param {number} delta - Frame delta time in seconds.
   * @param {number} alpha - Fixed-step interpolation factor [0.0, 1.0).
   */
  _process(delta, alpha) {
    // 1. Chunk streaming around the player.
    //    updateStreaming() is cheap to call every frame: it returns at once
    //    unless the player has crossed into a new chunk.
    //    processStreamingQueue() then builds at most two chunks, so walking
    //    into new terrain never stalls a frame.
    const transform = this.ecs.getComponent(this.playerEntityId, 'Transform');
    if (transform) {
      this.world.updateStreaming(transform.position.x, transform.position.z);
    }
    this.world.processStreamingQueue();

    // 2. Frustum culling: hide chunks outside the camera's view.
    this.cullingMetrics = this.world.cullFrustum(this.camera);

    // 3. Fast Amanatides-Woo Voxel Traversal for block targeting
    this.world.raycastVoxel(this.camera);

    // 4. Advance ECS variable update systems (Camera lerp, Telemetry HUD)
    this.ecs.updateVariable(delta, alpha);

    // 5. Render Scene
    this.renderer.render(this.scene, this.camera);
    this.input.flush_frame_edges();
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
   * Renders the 9-slot ancient hotbar UI.
   */
  renderHotbar() {
    const hotbarElem = document.getElementById('hotbar');
    if (!hotbarElem) return;
    hotbarElem.innerHTML = '';

    for (let i = 0; i < this.inventory.hotbarSlotsCount; i++) {
      const slot = this.inventory.getSlot(i);
      const meta = slot ? SHILP_ITEMS[slot.itemId] : null;

      const slotElem = document.createElement('div');
      slotElem.className = `hotbar-slot ${i === this.inventory.activeSlotIndex ? 'active' : ''}`;
      slotElem.setAttribute('title', meta ? `${meta.name} (${slot.count})` : `Slot ${i + 1}`);

      const numBadge = document.createElement('span');
      numBadge.className = 'slot-number';
      numBadge.textContent = i + 1;
      slotElem.appendChild(numBadge);

      if (slot && meta) {
        const iconElem = document.createElement('span');
        iconElem.className = 'slot-icon';
        iconElem.textContent = meta.icon || '📦';
        slotElem.appendChild(iconElem);

        if (slot.count > 1) {
          const countElem = document.createElement('span');
          countElem.className = 'slot-count';
          countElem.textContent = slot.count;
          slotElem.appendChild(countElem);
        }
      }

      slotElem.addEventListener('click', () => {
        this.inventory.setActiveSlot(i);
        this.renderHotbar();
      });

      hotbarElem.appendChild(slotElem);
    }
  }

  // ═══════════════════════════════════════════════════════════ SAVE SYSTEM

  /**
   * Gather everything that goes into a save (except the block edits, which
   * the save store already holds).
   * USED BY: saveGame(). NEXT: ShilpSaveStore.serialize() turns it into JSON.
   */
  _collectSaveState() {
    const transform = this.ecs.getComponent(this.playerEntityId, 'Transform');
    const rig = this.ecs.getComponent(this.playerEntityId, 'CameraRig');
    return {
      seed: this.world.seed,
      player: transform
        ? {
            x: transform.position.x,
            y: transform.position.y,
            z: transform.position.z,
            yaw: rig?.yaw ?? 0,
            pitch: rig?.pitch ?? 0,
          }
        : null,
      inventory: {
        activeSlotIndex: this.inventory.activeSlotIndex,
        // Copy the slots so later inventory changes cannot mutate the snapshot.
        slots: this.inventory.slots.map(s => (s ? { itemId: s.itemId, count: s.count } : null)),
      },
      // Shallow copy for the same reason.
      merchantStock: this.merchantStock ? { ...this.merchantStock } : null,
    };
  }

  /**
   * Put a loaded save back into the running game.
   * CALLED ONCE at startup, after the player, inventory and merchant exist.
   *
   * @param {Object|null} save - Output of ShilpSaveStore.load(); null = new world.
   */
  _restoreFromSave(save) {
    if (!save) return;

    // Player position AND previousPosition. WHY both: the camera interpolates
    // between the two. Restoring only `position` would make the view visibly
    // glide from the spawn point to the saved spot over the first frames.
    const transform = this.ecs.getComponent(this.playerEntityId, 'Transform');
    if (transform && save.player) {
      transform.position.set(save.player.x, save.player.y, save.player.z);
      transform.previousPosition.copy(transform.position);
      // Drop any momentum from the spawn frame.
      const kin = this.ecs.getComponent(this.playerEntityId, 'Kinematics');
      if (kin) kin.velocity.set(0, 0, 0);
    }
    const rig = this.ecs.getComponent(this.playerEntityId, 'CameraRig');
    if (rig && save.player) {
      rig.yaw = save.player.yaw ?? rig.yaw;
      rig.pitch = save.player.pitch ?? rig.pitch;
    }

    // Inventory: rebuilt through setSlot() so every saved stack is re-validated
    // against the item registry and max stack sizes (a corrupted or edited save
    // cannot inject an unknown item or an oversized stack).
    if (save.inventory?.slots) {
      for (let i = 0; i < this.inventory.totalSlots; i++) {
        const slot = save.inventory.slots[i];
        this.inventory.setSlot(i, slot?.itemId ?? null, slot?.count ?? 0);
      }
      this.inventory.setActiveSlot(save.inventory.activeSlotIndex ?? 0);
    }

    // Merchant stock: mutated IN PLACE, never replaced. WHY: the same object is
    // shared by reference with the barter modal and the NPC's ECS component.
    // Assigning a new object here would leave the modal trading from a stale
    // copy and the barter changes would silently stop being saved.
    if (save.merchantStock && this.merchantStock) {
      Object.assign(this.merchantStock, save.merchantStock);
    }
  }

  /**
   * Write the game to storage and tell the player how it went.
   * CALLED BY: the 30 s autosave timer, the tab going hidden, page unload.
   *
   * @param {{quiet?: boolean}} [opts] - quiet: no "Saved" toast. Used on
   *   unload, where the page is closing and there is no one to show it to.
   * @returns {{ok:boolean, bytes:number, tooLarge:boolean}}
   */
  saveGame({ quiet = false } = {}) {
    const result = this.saveStore.save(this._collectSaveState());
    if (quiet) return result;

    if (!result.ok) {
      this._showSaveToast('⚠ Could not save — browser storage is full or blocked', true);
    } else if (result.tooLarge) {
      const mb = (result.bytes / (1024 * 1024)).toFixed(1);
      this._showSaveToast(`⚠ Saved, but the world is large (${mb} MB) — near the browser limit`, true);
    } else {
      this._showSaveToast('Saved ✓');
    }
    return result;
  }

  /**
   * Autosave triggers, New World button, and toast element lookup.
   * CALLED ONCE from the constructor.
   */
  _initSaveSystem() {
    this.saveToast = document.getElementById('save-toast');
    this.saveToastTimer = null;

    if (!this.saveStore.available) {
      // Private browsing or storage disabled: say so once instead of
      // pretending to save every 30 seconds.
      this._showSaveToast('⚠ Saving is unavailable in this browser mode', true);
      return;
    }

    // (a) Every 30 s. WHY 30: frequent enough that a crash loses little work,
    // rare enough that the JSON.stringify cost is never felt.
    this.autosaveInterval = setInterval(() => this.saveGame(), 30_000);

    // (b) When the tab is hidden. WHY: on mobile, switching apps or locking
    // the screen often KILLS the page without ever firing beforeunload.
    // visibilitychange is the last event such a page reliably gets.
    this.handleVisibility = () => {
      if (document.visibilityState === 'hidden') this.saveGame({ quiet: true });
    };
    document.addEventListener('visibilitychange', this.handleVisibility);

    // (c) On close / refresh on desktop.
    this.handleBeforeUnload = () => this.saveGame({ quiet: true });
    window.addEventListener('beforeunload', this.handleBeforeUnload);

    // New World: destructive, so it always asks first.
    const newWorldBtn = document.getElementById('new-world-btn');
    if (newWorldBtn) {
      newWorldBtn.addEventListener('click', () => this.startNewWorld());
    }
  }

  /**
   * Discard the saved world and reload into a fresh random seed.
   * WHY a page reload rather than rebuilding in place: the world, every chunk
   * mesh, the ECS entities and the streaming queues would all need tearing down. A
   * reload does that completely and cannot leave half-disposed state behind.
   */
  startNewWorld() {
    const ok = window.confirm(
      'Start a new world?\n\nYour current world and everything you have built ' +
      'in it will be permanently deleted.'
    );
    if (!ok) return;

    // Stop the autosave and unload handlers first, or they would immediately
    // write the OLD world back into storage during the reload.
    clearInterval(this.autosaveInterval);
    window.removeEventListener('beforeunload', this.handleBeforeUnload);
    document.removeEventListener('visibilitychange', this.handleVisibility);

    this.saveStore.clear();
    window.location.reload();
  }

  /**
   * Show the save toast briefly.
   * @param {string} text
   * @param {boolean} [warn=false] - Terracotta warning style.
   */
  _showSaveToast(text, warn = false) {
    if (!this.saveToast) return;
    this.saveToast.textContent = text;
    this.saveToast.classList.toggle('warn', warn);
    this.saveToast.classList.remove('hidden');
    if (this.saveToastTimer) clearTimeout(this.saveToastTimer);
    // Warnings stay up longer: they need reading, "Saved ✓" only a glance.
    this.saveToastTimer = setTimeout(
      () => this.saveToast.classList.add('hidden'),
      warn ? 6000 : 1600
    );
  }

  /**
   * Cleanly disposes engine resources, ECS tables, and terminates loops.
   */
  destroy() {
    this.timeWheel.stop();
    // Stop autosaving; leaving these registered would keep writing to storage
    // from a destroyed engine.
    if (this.autosaveInterval) clearInterval(this.autosaveInterval);
    if (this.handleVisibility) document.removeEventListener('visibilitychange', this.handleVisibility);
    if (this.handleBeforeUnload) window.removeEventListener('beforeunload', this.handleBeforeUnload);
    this.input.unbind_listeners();
    window.removeEventListener('resize', this.handleResize);
    if (this.handleKeyDown) window.removeEventListener('keydown', this.handleKeyDown);
    this.renderer.dispose();
  }
}
