/**
 * ============================================================================
 * SAVE & LOAD PERSISTENCE SYSTEM
 * ============================================================================
 * Serializes and restores full game state including world seed, player spatial
 * transforms, inventory contents, day-night cycle time, and the modified blocks
 * delta table. Supports LocalStorage persistence, auto-save timers, and JSON
 * file export/import.
 */

const STORAGE_KEY = 'voxel_game_save_v1';

export class SaveSystem {
  constructor(world, player, inventory, dayNightCycle) {
    this.world = world;
    this.player = player;
    this.inventory = inventory;
    this.dayNight = dayNightCycle;
    this.autoSaveTimer = 0;
    this.autoSaveInterval = 45; // Auto-save every 45 seconds

    this.initDOM();
  }

  /**
   * Compiles comprehensive snapshot of game state into a plain JSON-serializable object
   */
  serializeState() {
    // Pack modified blocks map as array of entries
    const modifiedEntries = Array.from(this.world.modifiedBlocks.entries());

    // Pack inventory slots
    const inventoryData = this.inventory.slots.map((slot) => {
      if (!slot) return null;
      return { id: slot.id, count: slot.count };
    });

    return {
      version: 1,
      timestamp: Date.now(),
      seed: this.world.noise.seed,
      time: this.dayNight.time,
      player: {
        x: this.player.position.x,
        y: this.player.position.y,
        z: this.player.position.z,
        yaw: this.player.yaw,
        pitch: this.player.pitch,
        isFlying: this.player.isFlying,
      },
      inventory: {
        activeSlot: this.inventory.activeSlot,
        slots: inventoryData,
      },
      modifiedBlocks: modifiedEntries,
    };
  }

  /**
   * Restores complete game state from a state object
   */
  deserializeState(data) {
    if (!data || data.version !== 1) {
      console.warn('Incompatible or invalid save data');
      return false;
    }

    try {
      // 1. Restore time of day
      if (typeof data.time === 'number') {
        this.dayNight.time = data.time;
      }

      // 2. Restore player state
      if (data.player) {
        this.player.position.set(data.player.x, data.player.y, data.player.z);
        this.player.velocity.set(0, 0, 0);
        this.player.yaw = data.player.yaw || 0;
        this.player.pitch = data.player.pitch || 0;
        this.player.isFlying = !!data.player.isFlying;
      }

      // 3. Restore inventory
      if (data.inventory && Array.isArray(data.inventory.slots)) {
        this.inventory.activeSlot = data.inventory.activeSlot || 0;
        for (let i = 0; i < 36; i++) {
          this.inventory.slots[i] = data.inventory.slots[i] ? { ...data.inventory.slots[i] } : null;
        }
        this.inventory.render();
      }

      // 4. Restore modified blocks
      if (Array.isArray(data.modifiedBlocks)) {
        this.world.modifiedBlocks.clear();
        for (const [key, blockId] of data.modifiedBlocks) {
          this.world.modifiedBlocks.set(key, blockId);
        }

        // Trigger rebuild of all currently active chunks
        for (const chunk of this.world.chunks.values()) {
          this.world.applyModificationsToChunk(chunk);
          chunk.buildMesh(this.world.textureManager);
        }
      }

      this.showToast('World Loaded Successfully!');
      return true;
    } catch (err) {
      console.error('Failed to load save state:', err);
      this.showToast('Error Loading Save!');
      return false;
    }
  }

  /**
   * Saves current state to browser LocalStorage
   */
  saveToLocalStorage() {
    const data = this.serializeState();
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    this.showToast('World Saved to LocalStorage!');
  }

  /**
   * Loads state from browser LocalStorage
   */
  loadFromLocalStorage() {
    const json = localStorage.getItem(STORAGE_KEY);
    if (!json) {
      this.showToast('No Local Save Found');
      return false;
    }
    const data = JSON.parse(json);
    return this.deserializeState(data);
  }

  /**
   * Exports game state as downloadable JSON file
   */
  exportToFile() {
    const state = this.serializeState();
    const blob = new Blob([JSON.stringify(state, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `voxel_world_save_${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    this.showToast('Save Exported to File!');
  }

  /**
   * Imports game state from user uploaded JSON file
   */
  importFromFile(file) {
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target.result);
        this.deserializeState(data);
      } catch (err) {
        this.showToast('Invalid JSON file format!');
      }
    };
    reader.readAsText(file);
  }

  /**
   * Resets world modifications and restarts
   */
  resetWorld() {
    localStorage.removeItem(STORAGE_KEY);
    this.world.modifiedBlocks.clear();
    location.reload();
  }

  showToast(message) {
    const toast = document.getElementById('save-toast');
    if (toast) {
      toast.textContent = message;
      toast.style.opacity = '1';
      clearTimeout(this.toastTimeout);
      this.toastTimeout = setTimeout(() => {
        toast.style.opacity = '0';
      }, 2500);
    }
  }

  update(dt) {
    // Auto-save interval check
    this.autoSaveTimer += dt;
    if (this.autoSaveTimer >= this.autoSaveInterval) {
      this.autoSaveTimer = 0;
      this.saveToLocalStorage();
    }
  }

  initDOM() {
    // Quick-save (K) and Quick-load (L) hotkeys
    window.addEventListener('keydown', (e) => {
      if (e.code === 'KeyK') {
        this.saveToLocalStorage();
      }
      if (e.code === 'KeyL') {
        this.loadFromLocalStorage();
      }
    });

    const saveBtn = document.getElementById('btn-save');
    const loadBtn = document.getElementById('btn-load');
    const exportBtn = document.getElementById('btn-export');
    const importInput = document.getElementById('file-import');
    const resetBtn = document.getElementById('btn-reset');

    if (saveBtn) saveBtn.addEventListener('click', () => this.saveToLocalStorage());
    if (loadBtn) loadBtn.addEventListener('click', () => this.loadFromLocalStorage());
    if (exportBtn) exportBtn.addEventListener('click', () => this.exportToFile());
    if (importInput) {
      importInput.addEventListener('change', (e) => {
        if (e.target.files && e.target.files[0]) {
          this.importFromFile(e.target.files[0]);
        }
      });
    }
    if (resetBtn) resetBtn.addEventListener('click', () => {
      if (confirm('Reset world to seed state and clear all modifications?')) {
        this.resetWorld();
      }
    });
  }
}
