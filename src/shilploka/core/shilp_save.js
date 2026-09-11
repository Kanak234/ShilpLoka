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
 * Upgrades for saves written by OLDER versions of this schema.
 * Shape: { <fromVersion>: (data) => data-in-the-next-version }.
 *
 * WHY it is empty: version 1 is the first save format ShilpLoka has ever had.
 * The build on main (the one live on itch.io) stores nothing at all - checked
 * by playing it, pressing K, waiting past any autosave interval and firing
 * beforeunload: localStorage, sessionStorage, IndexedDB, cookies and Cache
 * Storage all stayed empty. So there is no older format to convert.
 *
 * HOW to use it: when the shape changes, bump SAVE_VERSION to 2 and add
 * `1: (d) => ({ ...d, version: 2, <new fields> })`. inspect() runs the chain,
 * so a v1 save opened by a v3 build goes 1 -> 2 -> 3 automatically.
 */
export const SAVE_MIGRATIONS = {};

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

    /**
     * Set by load() when a save EXISTS but cannot be used: damaged JSON, or a
     * version this build does not understand (e.g. a save from a newer build).
     * { reason: 'corrupt'|'newer'|'invalid', version, backupKey, writeBlocked }
     * USED BY: the engine, to tell the player exactly what happened.
     * @type {null|{reason:string, version:(number|null), backupKey:(string|null), writeBlocked:boolean}}
     */
    this.loadIssue = null;

    /**
     * True when an unreadable save could NOT be backed up (storage full).
     * save() then refuses to write, because writing would overwrite the only
     * copy of the player's old world.
     */
    this.writeBlocked = false;
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
  /**
   * Parse a stored save and say WHY it is unusable when it is.
   *
   * WHY this exists alongside deserialize(): "null" alone cannot tell "there
   * is no save" from "there is a save I must not destroy". load() needs that
   * difference to protect the data instead of letting the next autosave
   * overwrite it.
   *
   * @param {string|null} json
   * @returns {{data: Object|null, reason: null|'empty'|'corrupt'|'newer'|'invalid', version: number|null}}
   */
  static inspect(json) {
    if (!json) return { data: null, reason: 'empty', version: null };
    let data;
    try {
      data = JSON.parse(json);
    } catch {
      return { data: null, reason: 'corrupt', version: null };
    }
    if (!data || typeof data !== 'object') return { data: null, reason: 'corrupt', version: null };

    const version = Number.isInteger(data.version) ? data.version : null;
    if (version !== null && version > SAVE_VERSION) {
      // Written by a NEWER build. Never guess at its fields.
      return { data: null, reason: 'newer', version };
    }
    // Older schema: upgrade step by step through SAVE_MIGRATIONS.
    let v = version;
    while (v !== null && v < SAVE_VERSION) {
      const step = SAVE_MIGRATIONS[v];
      if (!step) return { data: null, reason: 'invalid', version };
      data = step(data);
      v = data.version;
    }
    if (v !== SAVE_VERSION || !Number.isFinite(data.seed) ||
        (data.chunks && typeof data.chunks !== 'object')) {
      return { data: null, reason: 'invalid', version };
    }
    return { data, reason: null, version };
  }

  static deserialize(json) {
    return ShilpSaveStore.inspect(json).data;
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
    if (this.writeBlocked) {
      // An unreadable save is in the slot and could not be backed up. Writing
      // now would destroy it, so this build does not save until the player
      // chooses New World (which calls clear()).
      return { ok: false, bytes: 0, tooLarge: false, error: 'protecting unreadable save' };
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
    this.loadIssue = null;
    if (!this.storage) return null;
    let json = null;
    try {
      json = this.storage.getItem(this.key);
    } catch {
      return null;
    }
    const { data, reason, version } = ShilpSaveStore.inspect(json);
    if (data) {
      this.loadDiffs(data);
      return data;
    }
    if (reason === 'empty') return null;

    // A save exists but this build cannot use it. It is NOT discarded: the
    // raw text is copied to a timestamped backup key first. WHY: the game is
    // about to start a new world, and its first autosave would otherwise
    // overwrite the player's old world with no way back.
    const backupKey = `${this.key}.backup-${Date.now()}`;
    let backedUp = false;
    try {
      this.storage.setItem(backupKey, json);
      backedUp = true;
    } catch {
      // Storage is full, so even the backup failed. Stop ALL saving instead:
      // losing new progress is recoverable, overwriting the only copy is not.
      this.writeBlocked = true;
    }
    this.loadIssue = { reason, version, backupKey: backedUp ? backupKey : null, writeBlocked: this.writeBlocked };
    return null;
  }

  /**
   * Delete the save and forget all edits.
   * CALLED BY: the "New World" button, after the player confirms.
   */
  clear() {
    this.diffs.clear();
    this.dirty = false;
    // The player confirmed "delete this world and start over", so the
    // protection for an unreadable save no longer applies. Backup keys are
    // left alone: they are the player's data, not the current world.
    this.writeBlocked = false;
    this.loadIssue = null;
    if (!this.storage) return;
    try {
      this.storage.removeItem(this.key);
    } catch {
      /* nothing more we can do; the next save overwrites it anyway */
    }
  }
}
