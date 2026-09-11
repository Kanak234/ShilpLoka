/**
 * Tests for the save system: src/shilploka/core/shilp_save.js.
 *
 * The store takes its storage backend as a constructor argument, so these
 * tests hand it a plain in-memory object instead of the browser's
 * localStorage. Everything else is the real code.
 */
import { beforeEach, describe, expect, it } from 'vitest';

import {
  SAVE_MIGRATIONS,
  SAVE_VERSION,
  SAVE_WARN_BYTES,
  ShilpSaveStore,
  chunkKey,
  randomSeed,
} from '../src/shilploka/core/shilp_save.js';

/** Minimal localStorage stand-in: the three methods the store uses. */
function memoryStorage() {
  const data = new Map();
  return {
    getItem: k => (data.has(k) ? data.get(k) : null),
    setItem: (k, v) => { data.set(k, String(v)); },
    removeItem: k => { data.delete(k); },
    raw: data,
  };
}

/** A realistic game-state snapshot, the shape the engine passes to save(). */
const STATE = {
  seed: 424242,
  player: { x: 11.5, y: 30, z: -7.25, yaw: 1.25, pitch: -0.3 },
  inventory: {
    activeSlotIndex: 4,
    slots: [{ itemId: 'HARAPPAN_BAKED_BRICK', count: 48 }, null, { itemId: 'SAFFRON', count: 5 }],
  },
  merchantStock: { cardamom: 40, pepper: 3, saffron: 15, bronze: 25 },
};

describe('ShilpSaveStore', () => {
  let storage;
  let store;

  beforeEach(() => {
    storage = memoryStorage();
    store = new ShilpSaveStore(storage);
  });

  describe('serialize / deserialize round trip', () => {
    // THE core guarantee: what goes in comes back out, field for field.
    it('restores seed, player, inventory, merchant and every edit', () => {
      store.recordEdit(0, 0, 1234, 0);          // a broken block
      store.recordEdit(-1, -3, 42, 3);          // a placed block in a negative chunk

      expect(store.save(STATE).ok).toBe(true);

      const fresh = new ShilpSaveStore(storage);
      const data = fresh.load();

      expect(data.version).toBe(SAVE_VERSION);
      expect(data.seed).toBe(STATE.seed);
      expect(data.player).toEqual(STATE.player);
      expect(data.inventory).toEqual(STATE.inventory);
      expect(data.merchantStock).toEqual(STATE.merchantStock);
      expect(fresh.getChunkDiff(0, 0).get(1234)).toBe(0);
      expect(fresh.getChunkDiff(-1, -3).get(42)).toBe(3);
      expect(fresh.editCount).toBe(2);
    });

    it('stores edits as per-chunk "cx,cz" -> {voxelIndex: blockId}', () => {
      store.recordEdit(2, -5, 7, 9);
      const json = JSON.parse(store.serialize(STATE));
      expect(json.chunks).toEqual({ '2,-5': { 7: 9 } });
    });

    it('keeps only the latest value when one voxel is edited twice', () => {
      store.recordEdit(0, 0, 10, 3);   // place brick
      store.recordEdit(0, 0, 10, 0);   // then break it again
      expect(store.getChunkDiff(0, 0).get(10)).toBe(0);
      expect(store.editCount).toBe(1);
    });
  });

  describe('rejecting bad saves', () => {
    // A corrupt save must yield a fresh world, never a crash or a blank screen.
    it.each([
      ['not JSON at all', '{ this is not json'],
      ['a different schema version', JSON.stringify({ version: 99, seed: 1, chunks: {} })],
      ['a missing seed', JSON.stringify({ version: SAVE_VERSION, chunks: {} })],
      ['null', null],
    ])('returns null for %s', (_label, raw) => {
      expect(ShilpSaveStore.deserialize(raw)).toBeNull();
    });

    it('skips an out-of-range voxel index instead of throwing', () => {
      const chunk = { chunkX: 0, chunkZ: 0, voxels: new Uint8Array(16), isDirty: false };
      store.recordEdit(0, 0, 3, 7);
      store.recordEdit(0, 0, 999999, 7);           // outside the array
      expect(() => store.applyToChunk(chunk)).not.toThrow();
      expect(chunk.voxels[3]).toBe(7);
    });
  });

  describe('applyToChunk', () => {
    it('writes saved edits over freshly generated voxels', () => {
      const chunk = { chunkX: 4, chunkZ: -2, voxels: new Uint8Array(64).fill(5), isDirty: false };
      store.recordEdit(4, -2, 10, 0);
      store.recordEdit(4, -2, 11, 3);
      expect(store.applyToChunk(chunk)).toBe(2);
      expect(chunk.voxels[10]).toBe(0);
      expect(chunk.voxels[11]).toBe(3);
      expect(chunk.voxels[12]).toBe(5);             // untouched voxels keep terrain
      expect(chunk.isDirty).toBe(true);             // so it gets re-meshed
    });

    it('leaves a chunk with no edits alone', () => {
      const chunk = { chunkX: 9, chunkZ: 9, voxels: new Uint8Array(8).fill(5), isDirty: false };
      expect(store.applyToChunk(chunk)).toBe(0);
      expect(chunk.isDirty).toBe(false);
    });
  });

  describe('storage behaviour', () => {
    it('reports a failed write instead of throwing (e.g. quota exceeded)', () => {
      storage.setItem = () => { const e = new Error('full'); e.name = 'QuotaExceededError'; throw e; };
      const res = store.save(STATE);
      expect(res.ok).toBe(false);
      expect(res.error).toBe('QuotaExceededError');
    });

    it('flags saves near the browser limit so the HUD can warn', () => {
      // Enough edits to push the JSON past the warning threshold.
      for (let i = 0; i < 16384; i++) store.recordEdit(i % 400, Math.floor(i / 400), i, 3);
      const res = store.save(STATE);
      expect(res.bytes).toBeGreaterThan(0);
      expect(res.tooLarge).toBe(res.bytes > SAVE_WARN_BYTES);
    });

    it('still records edits in memory when no storage exists', () => {
      // Private browsing: nothing persists, but edits must survive chunk
      // unloading within the session, so the in-memory diff still works.
      const noStorage = new ShilpSaveStore(null);
      noStorage.recordEdit(0, 0, 5, 3);
      expect(noStorage.available).toBe(false);
      expect(noStorage.getChunkDiff(0, 0).get(5)).toBe(3);
      expect(noStorage.save(STATE).ok).toBe(false);
      expect(noStorage.load()).toBeNull();
    });

    it('clear() deletes the save and all edits (New World)', () => {
      store.recordEdit(0, 0, 1, 1);
      store.save(STATE);
      store.clear();
      expect(store.editCount).toBe(0);
      expect(new ShilpSaveStore(storage).load()).toBeNull();
    });
  });

  describe('protecting saves this build cannot read', () => {
    // WHY this block exists: before this fix, load() returned null for an
    // unreadable save, the game started a new world, and its first autosave
    // OVERWROTE the old save for good. Each test below would fail on that code.
    const NEWER = JSON.stringify({ version: SAVE_VERSION + 1, seed: 7, chunks: { '0,0': { 1: 3 } }, extra: 'v2 field' });
    const CORRUPT = '{"version":1,"seed":7,"chunks":{"0,0":{"1":3';   // truncated mid-write

    it.each([
      ['a save from a newer version', NEWER, 'newer', SAVE_VERSION + 1],
      ['a corrupt (truncated) save', CORRUPT, 'corrupt', null],
      ['a save with no version field', JSON.stringify({ seed: 7, chunks: {} }), 'invalid', null],
    ])('%s is backed up byte-for-byte before a new world starts', (_label, raw, reason, version) => {
      storage.setItem(store.key, raw);

      expect(store.load()).toBeNull();                       // cannot be opened...
      expect(store.loadIssue).toMatchObject({ reason, version, writeBlocked: false });
      const backupKey = store.loadIssue.backupKey;
      expect(backupKey).toMatch(/^shilploka\.save\.v1\.backup-\d+$/);
      expect(storage.getItem(backupKey)).toBe(raw);          // ...but kept exactly

      // The new world's first autosave now goes ahead; the backup survives it.
      expect(store.save(STATE).ok).toBe(true);
      expect(storage.getItem(backupKey)).toBe(raw);
    });

    it('refuses to save when even the backup cannot be written (storage full)', () => {
      storage.setItem(store.key, NEWER);
      const realSetItem = storage.setItem;
      storage.setItem = () => { const e = new Error('full'); e.name = 'QuotaExceededError'; throw e; };

      expect(store.load()).toBeNull();
      expect(store.loadIssue).toMatchObject({ reason: 'newer', backupKey: null, writeBlocked: true });

      // Even once space frees up, this session must not write over the only copy.
      storage.setItem = realSetItem;
      const res = store.save(STATE);
      expect(res.ok).toBe(false);
      expect(res.error).toBe('protecting unreadable save');
      expect(storage.getItem(store.key)).toBe(NEWER);
    });

    it('New World (clear) is explicit consent: it lifts the block but keeps backups', () => {
      storage.setItem(store.key, CORRUPT);
      store.load();
      const backupKey = store.loadIssue.backupKey;
      store.writeBlocked = true;                             // as if the backup had failed

      store.clear();
      expect(store.writeBlocked).toBe(false);
      expect(store.loadIssue).toBeNull();
      expect(store.save(STATE).ok).toBe(true);
      expect(storage.getItem(backupKey)).toBe(CORRUPT);
    });

    it('a readable save reports no issue and makes no backup', () => {
      store.save(STATE);
      const before = storage.raw.size;
      expect(new ShilpSaveStore(storage).load()).not.toBeNull();
      expect(storage.raw.size).toBe(before);
    });

    it('runs registered migrations in order (the upgrade path for a future v2)', () => {
      // No migration exists today: v1 is the first format (the build on main
      // saves nothing). This proves the mechanism works for when one is added.
      SAVE_MIGRATIONS[SAVE_VERSION - 1] = d => ({ ...d, version: SAVE_VERSION, seed: d.worldSeed });
      try {
        const old = JSON.stringify({ version: SAVE_VERSION - 1, worldSeed: 99, chunks: {} });
        const { data, reason } = ShilpSaveStore.inspect(old);
        expect(reason).toBeNull();
        expect(data).toMatchObject({ version: SAVE_VERSION, seed: 99 });
      } finally {
        delete SAVE_MIGRATIONS[SAVE_VERSION - 1];
      }
    });
  });

  describe('helpers', () => {
    it('chunkKey matches the world\'s "cx,cz" format, including negatives', () => {
      expect(chunkKey(-3, 12)).toBe('-3,12');
    });

    it('randomSeed produces varied positive integers', () => {
      const seeds = new Set(Array.from({ length: 20 }, randomSeed));
      expect(seeds.size).toBeGreaterThan(15);
      for (const s of seeds) {
        expect(Number.isInteger(s)).toBe(true);
        expect(s).toBeGreaterThan(0);
      }
    });
  });
});
