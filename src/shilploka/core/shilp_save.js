/**
 * @fileoverview ShilpSave - persistent world saves for ShilpLoka.
 * @module shilploka/core/shilp_save
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS EXISTS
 *   Before this module the game had no persistence at all: every refresh threw
 *   away everything the player had built, and the world was always the same
 *   because the seed was hardcoded to 1008.
 *
 * THE KEY DESIGN DECISION: SAVE EDITS, NOT THE WORLD
 *   The terrain is procedural - the same seed always regenerates exactly the
 *   same blocks. So there is no need to store the world itself, only what the
 *   player CHANGED. A save is therefore:
 *
 *       seed  +  a list of edits  +  player / inventory / merchant state
 *
 *   A full chunk is 16 x 64 x 16 = 16,384 voxels. Storing whole chunks would
 *   fill browser storage after a few hundred of them. Storing only edits keeps
 *   a heavily built world to kilobytes.
 *
 * HOW DATA FLOWS
 *
 *   player breaks/places a block
 *        │  ShilpWorld.setBlock()  ──►  saveStore.recordEdit(cx, cz, index, id)
 *        ▼
 *   in-memory diff map  { "cx,cz" -> Map(voxelIndex -> blockId) }
 *        │  autosave timer / tab hidden / page closing
 *        ▼
 *   saveStore.save(state) ──► JSON ──► localStorage["shilploka.save.v1"]
 *
 *   on the next start:
 *   localStorage ──► saveStore.load() ──► seed goes to ShilpWorld,
 *        player/inventory/merchant go back into the engine, and each chunk
 *        gets its edits re-applied by applyToChunk() the moment it is
 *        generated -- which is also what makes edits survive chunk unloading.
 *
 * WHY THE STORAGE BACKEND IS INJECTABLE
 *   The constructor accepts any object with getItem/setItem/removeItem. In the
 *   browser that is window.localStorage; in the vitest suite it is a plain
 *   in-memory stand-in. That is the only reason this file can be unit-tested
 *   without a browser.
 * ─────────────────────────────────────────────────────────────────────────────
 */

/**
 * Schema version written into every save.
 * USED FOR: refusing (rather than mis-reading) a save written by a future or
 * incompatible version. Bump it whenever the shape below changes, and add a
 * migration in deserialize().
 */
export const SAVE_VERSION = 1;

/**
 * localStorage key. The version is part of the key so an incompatible future
 * save can never be read by accident, and an old one is simply ignored.
 */
export const SAVE_KEY = 'shilploka.save.v1';

/**
 * Above this size the HUD warns the player.
 * WHY 4 MB: browsers cap localStorage at roughly 5 MB per site. Warning at 4
 * leaves headroom, so the player hears about it before a save actually fails.
 */
export const SAVE_WARN_BYTES = 4 * 1024 * 1024;

/**
 * Produce a random world seed.
 * USED FOR: "New World" and first launch. Previously every world was seed
 * 1008, so every player got the identical map.
 * The range stays well inside the 32-bit integers the Perlin noise expects.
 */
export function randomSeed() {
  return Math.floor(Math.random() * 2_000_000_000) + 1;
}

/**
 * Chunk-map key. Must match ShilpWorld.getChunkKey() exactly, because the
 * world looks chunks up by the same string. Negative coordinates are fine:
 * "-3,-1" is just a string.
 */
export function chunkKey(cx, cz) {
  return `${cx},${cz}`;
}

export class ShilpSaveStore {
  /**
   * @param {{getItem:Function,setItem:Function,removeItem:Function}|null} [storage]
   *   Where the save lives. Defaults to localStorage when it exists. Can be null
   *   (e.g. private browsing with storage disabled) - the game then still runs,
   *   it just cannot persist.
   * @param {string} [key=SAVE_KEY] - Storage key; overridden only in tests.
   */
  constructor(storage = ShilpSaveStore.defaultStorage(), key = SAVE_KEY) {
    this.storage = storage;
    this.key = key;

    /**
     * The edit diff. "cx,cz" -> Map(voxelIndex -> blockId).
     * WHY a Map of Maps: lookups by chunk happen every time a chunk loads, and
     * per-voxel writes happen on every edit; both are O(1) this way.
     * @type {Map<string, Map<number, number>>}
     */
    this.diffs = new Map();

    /**
     * True when there are edits not yet written to storage.
     * USED FOR: skipping pointless autosaves when nothing changed.
     */
    this.dirty = false;
  }

  /**
   * Resolve localStorage defensively.
   * WHY the try/catch: in some browsers (Safari private mode, sandboxed
   * iframes, file:// pages with storage blocked) merely READING
   * window.localStorage throws. A save system must never be the reason the
   * game fails to start.
   */
  static defaultStorage() {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const probe = '__shilploka_probe__';
        window.localStorage.setItem(probe, '1');
        window.localStorage.removeItem(probe);
        return window.localStorage;
      }
    } catch {
      /* storage blocked -- fall through to "no persistence" */
    }
    return null;
  }

  /** Whether saves can actually be written on this device. */
  get available() {
    return this.storage !== null;
  }

  // ───────────────────────────────────────────────────────── recording edits

  /**
   * Record one player edit.
   * CALLED BY: ShilpWorld.setBlock(), which is the only path player actions
   * take (world generation writes to chunks directly and is never recorded).
   * NEXT: the entry is written to storage on the next save().
   *
   * @param {number} cx - Chunk X.
   * @param {number} cz - Chunk Z.
   * @param {number} voxelIndex - Index into ShilpChunk.voxels (chunk.getIndex).
   * @param {number} blockId - The block now at that voxel (AIR when broken).
   */
  recordEdit(cx, cz, voxelIndex, blockId) {
    const key = chunkKey(cx, cz);
    let chunkDiff = this.diffs.get(key);
    if (!chunkDiff) {
      chunkDiff = new Map();
      this.diffs.set(key, chunkDiff);
    }
    // Always store the LATEST value. Placing then breaking the same voxel just
    // overwrites the entry with AIR, which is exactly the right end state.
    chunkDiff.set(voxelIndex, blockId);
    this.dirty = true;
  }

  /** @returns {Map<number,number>|undefined} Edits for one chunk. */
  getChunkDiff(cx, cz) {
    return this.diffs.get(chunkKey(cx, cz));
  }

  /**
   * Re-apply a chunk's saved edits on top of freshly generated terrain.
   * CALLED BY: ShilpWorld, immediately after generateChunkData() and before
   * the chunk is meshed - so the first mesh the player ever sees already
   * includes their buildings.
   * WHY this makes edits survive unloading: when a chunk streams out, its
   * voxels are thrown away; when it streams back in, it is regenerated from
   * the seed and this puts the edits back. The diff store is the single source
   * of truth for "what the player changed".
   *
   * Writes to chunk.voxels directly rather than chunk.setBlock(), because
   * setBlock would mark the chunk dirty once per voxel for no benefit.
   *
   * @param {import('../world/shilp_chunk.js').ShilpChunk} chunk
   * @returns {number} How many edits were applied.
   */
  applyToChunk(chunk) {
    const chunkDiff = this.diffs.get(chunkKey(chunk.chunkX, chunk.chunkZ));
    if (!chunkDiff) return 0;
    const voxels = chunk.voxels;
    for (const [index, blockId] of chunkDiff) {
      // Guard against a corrupted or hand-edited save pointing outside the
      // chunk array; one bad entry must not throw and abort the whole load.
      if (index >= 0 && index < voxels.length) voxels[index] = blockId;
    }
    chunk.isDirty = true;
    return chunkDiff.size;
  }

  /** Total edited voxels across all chunks. USED FOR: the save-status HUD. */
  get editCount() {
    let n = 0;
    for (const d of this.diffs.values()) n += d.size;
    return n;
  }

  // ─────────────────────────────────────────────────────── (de)serialisation

  /**
   * Turn the full game state into the JSON string that gets stored.
   *
   * Shape (version 1):
   *   {
   *     version: 1,
   *     savedAt: <ms timestamp>,
   *     seed: <number>,
   *     player: { x, y, z, yaw, pitch },
   *     inventory: { activeSlotIndex, slots: [ {itemId,count} | null, ... ] },
   *     merchantStock: { cardamom, pepper, saffron, bronze },
   *     chunks: { "cx,cz": { "<voxelIndex>": blockId, ... }, ... }
   *   }
   *
   * `chunks` is a plain object of plain objects because JSON cannot represent
   * a Map. deserialize() converts it back.
   *
   * @param {Object} state - Everything except the diffs, which this store owns.
   * @returns {string}
   */
  serialize(state) {
    const chunks = {};
    for (const [key, chunkDiff] of this.diffs) {
      if (chunkDiff.size === 0) continue;
      chunks[key] = Object.fromEntries(chunkDiff);
    }
    return JSON.stringify({
      version: SAVE_VERSION,
      savedAt: Date.now(),
      seed: state.seed,
      player: state.player ?? null,
      inventory: state.inventory ?? null,
      merchantStock: state.merchantStock ?? null,
      chunks,
    });
  }

  /**
   * Parse and validate a stored save. Returns null for anything unusable.
   * WHY it never throws: a corrupted save must drop the player into a fresh
   * world, not a blank screen. The caller treats null as "no save".
   *
   * @param {string|null} json
   * @returns {Object|null}
   */
  static deserialize(json) {
    if (!json) return null;
    let data;
    try {
      data = JSON.parse(json);
    } catch {
      return null;
    }
    if (!data || typeof data !== 'object') return null;
    // A save from a different schema version is not guessed at.
    if (data.version !== SAVE_VERSION) return null;
    if (!Number.isFinite(data.seed)) return null;
    if (data.chunks && typeof data.chunks !== 'object') return null;
    return data;
  }

  /**
   * Load the diff map out of a deserialised save.
   * NEXT: ShilpWorld calls applyToChunk() as each chunk is generated.
   */
  loadDiffs(data) {
    this.diffs.clear();
    for (const [key, entries] of Object.entries(data?.chunks ?? {})) {
      const chunkDiff = new Map();
      for (const [index, blockId] of Object.entries(entries)) {
        const i = Number(index);
        const id = Number(blockId);
        if (Number.isInteger(i) && Number.isInteger(id)) chunkDiff.set(i, id);
      }
      if (chunkDiff.size) this.diffs.set(key, chunkDiff);
    }
    this.dirty = false;
  }

  // ─────────────────────────────────────────────────────────── storage I/O

  /**
   * Write the game to storage.
   * CALLED BY: the engine's 30 s autosave, the tab going hidden, and the page
   * closing.
   *
   * @param {Object} state - See serialize().
   * @returns {{ok: boolean, bytes: number, tooLarge: boolean, error?: string}}
   *   `tooLarge` means the save worked but is near the browser limit; the HUD
   *   shows a warning. `ok: false` means it did not save at all.
   */
  save(state) {
    if (!this.storage) {
      return { ok: false, bytes: 0, tooLarge: false, error: 'storage unavailable' };
    }
    const json = this.serialize(state);
    // localStorage accounts in UTF-16 code units in most browsers, so each
    // character costs 2 bytes against the quota. Estimating conservatively
    // means the warning fires early rather than late.
    const bytes = json.length * 2;
    try {
      this.storage.setItem(this.key, json);
    } catch (err) {
      // QuotaExceededError lands here. The previous save is left untouched,
      // which is the safest outcome: the player loses recent edits, not
      // everything.
      return { ok: false, bytes, tooLarge: true, error: err?.name || String(err) };
    }
    this.dirty = false;
    return { ok: true, bytes, tooLarge: bytes > SAVE_WARN_BYTES };
  }

  /**
   * Read the save from storage and load its diffs.
   * CALLED BY: the engine at startup, BEFORE the world is created, because
   * the world needs the saved seed to regenerate the same terrain.
   *
   * @returns {Object|null} The save, or null when there is none / it is bad.
   */
  load() {
    if (!this.storage) return null;
    let json = null;
    try {
      json = this.storage.getItem(this.key);
    } catch {
      return null;
    }
    const data = ShilpSaveStore.deserialize(json);
    if (data) this.loadDiffs(data);
    return data;
  }

  /**
   * Delete the save and forget all edits.
   * CALLED BY: the "New World" button, after the player confirms.
   */
  clear() {
    this.diffs.clear();
    this.dirty = false;
    if (!this.storage) return;
    try {
      this.storage.removeItem(this.key);
    } catch {
      /* nothing more we can do; the next save overwrites it anyway */
    }
  }
}
