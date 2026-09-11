/**
 * Tests for the voxel world: src/shilploka/world/shilp_world.js.
 *
 * These build a REAL ShilpWorld. Three.js geometry, meshes and scenes all
 * work in Node without a GPU - only rendering needs WebGL - so terrain
 * generation, meshing, streaming and block edits are exercised exactly as the
 * game runs them. viewDistance is kept small only to keep the suite fast.
 */
import * as THREE from 'three';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ShilpSaveStore } from '../src/shilploka/core/shilp_save.js';
import { SHILP_ITEMS } from '../src/shilploka/inventory/shilp_inventory.js';
import { ShilpGreedyMesher } from '../src/shilploka/world/greedy_mesher.js';
import {
  CHUNK_SIZE_X,
  CHUNK_SIZE_Y,
  CHUNK_SIZE_Z,
} from '../src/shilploka/world/shilp_chunk.js';
import { ShilpWorld } from '../src/shilploka/world/shilp_world.js';
import {
  SHILP_BLOCK_REGISTRY,
  ShilpBlockId,
  canBreakVoxel,
} from '../src/shilploka/world/voxel_constants.js';

/** Build a small world with a fixed seed so terrain is reproducible. */
function makeWorld(opts = {}) {
  return new ShilpWorld(new THREE.Scene(), { seed: 1234, viewDistance: 1, ...opts });
}

/** Finish all queued chunk loading (the game spreads this over frames). */
function drain(world) {
  for (let i = 0; i < 1000 && world.pendingChunkCount > 0; i++) world.processStreamingQueue(64);
  world.processStreamingQueue(64);   // flush neighbour remeshes too
}

/** Move the streaming centre and load everything around it. */
function streamTo(world, x, z) {
  world.updateStreaming(x, z);
  drain(world);
}

/** Highest solid block in a column, or -1. */
function topSolidY(world, x, z) {
  for (let y = CHUNK_SIZE_Y - 1; y >= 0; y--) {
    if (SHILP_BLOCK_REGISTRY[world.getBlock(x, y, z)]?.solid) return y;
  }
  return -1;
}

describe('ShilpWorld', () => {
  let warnSpy;

  beforeEach(() => {
    // canBreakVoxel() logs a warning whenever a monument is protected. Silence
    // it so the test output stays readable; tests below also assert on it.
    warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    warnSpy.mockRestore();
  });

  describe('getBlock / setBlock with negative coordinates', () => {
    // WHY these three cases: JavaScript's % keeps the sign of the dividend, so
    // -1 % 16 is -1, not 15. Getting the floor-division and wrap-around wrong
    // is the classic voxel bug, and it only shows up west and south of spawn.
    it.each([
      [-1, -1, 15, '-1 is the LAST voxel of chunk -1'],
      [-16, -1, 0, '-16 is the FIRST voxel of chunk -1'],
      [-17, -2, 15, '-17 is the last voxel of chunk -2'],
    ])('world x=%i lands in chunk %i at local %i (%s)', (wx, expectedCx, expectedLx) => {
      const world = makeWorld();
      streamTo(world, wx, wx);
      expect(world.setBlock(wx, 40, wx, ShilpBlockId.HARAPPAN_BAKED_BRICK)).toBe(true);

      const chunk = world.chunks.get(`${expectedCx},${expectedCx}`);
      expect(chunk).toBeDefined();
      expect(chunk.getBlock(expectedLx, 40, expectedLx)).toBe(ShilpBlockId.HARAPPAN_BAKED_BRICK);
      expect(world.getBlock(wx, 40, wx)).toBe(ShilpBlockId.HARAPPAN_BAKED_BRICK);
    });

    it('does not confuse a negative block with its positive mirror', () => {
      const world = makeWorld();
      streamTo(world, 0, 0);
      world.setBlock(-5, 45, -5, ShilpBlockId.CHUNAR_SANDSTONE);
      expect(world.getBlock(-5, 45, -5)).toBe(ShilpBlockId.CHUNAR_SANDSTONE);
      expect(world.getBlock(5, 45, 5)).not.toBe(ShilpBlockId.CHUNAR_SANDSTONE);
    });

    it('treats Y outside the world as air, and refuses to edit there', () => {
      const world = makeWorld();
      expect(world.getBlock(0, -1, 0)).toBe(ShilpBlockId.AIR);
      expect(world.getBlock(0, CHUNK_SIZE_Y, 0)).toBe(ShilpBlockId.AIR);
      expect(world.setBlock(0, -1, 0, ShilpBlockId.HARAPPAN_BAKED_BRICK)).toBe(false);
    });
  });

  describe('edits survive a chunk being unloaded and reloaded', () => {
    // THE streaming guarantee: unloading throws chunk.voxels away, so the edit
    // must come back from the save store's diff when the chunk regenerates.
    it('keeps a broken block broken after its chunk streams out and back in', () => {
      const world = makeWorld();
      streamTo(world, 8, 8);

      const y = topSolidY(world, 5, 5);
      expect(y).toBeGreaterThan(0);
      world.setBlock(5, y, 5, ShilpBlockId.AIR);

      streamTo(world, 5000, 5000);                   // walk far away
      expect(world.chunks.has('0,0')).toBe(false);   // really unloaded

      streamTo(world, 8, 8);                         // and come back
      expect(world.chunks.has('0,0')).toBe(true);
      expect(world.getBlock(5, y, 5)).toBe(ShilpBlockId.AIR);
    });

    it('keeps a placed block after reload, in a negative chunk too', () => {
      const world = makeWorld();
      streamTo(world, -20, -20);
      world.setBlock(-20, 50, -20, ShilpBlockId.HARAPPAN_BAKED_BRICK);

      streamTo(world, 6000, -6000);
      expect(world.chunks.has('-2,-2')).toBe(false);

      streamTo(world, -20, -20);
      expect(world.getBlock(-20, 50, -20)).toBe(ShilpBlockId.HARAPPAN_BAKED_BRICK);
    });

    it('carries edits across worlds through a saved game', () => {
      // Save in one world, load into a brand-new one with the same seed:
      // exactly what a page refresh does.
      const storage = new Map();
      const mem = { getItem: k => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, v), removeItem: k => storage.delete(k) };

      const saveA = new ShilpSaveStore(mem);
      const worldA = makeWorld({ saveStore: saveA });
      streamTo(worldA, 8, 8);
      worldA.setBlock(3, 55, 3, ShilpBlockId.CHUNAR_SANDSTONE);
      saveA.save({ seed: worldA.seed });

      const saveB = new ShilpSaveStore(mem);
      const loaded = saveB.load();
      const worldB = makeWorld({ saveStore: saveB, seed: loaded.seed });
      streamTo(worldB, 8, 8);
      expect(worldB.getBlock(3, 55, 3)).toBe(ShilpBlockId.CHUNAR_SANDSTONE);
    });

    it('full round trip: place + break, walk out of view, walk back, then reload the page', () => {
      // WHY this test: it strings together, in the order a player does them,
      // every step the review asked to verify. The tests above each cover one
      // step; a bug in how they combine (e.g. a re-streamed chunk writing its
      // regenerated terrain back into the diff) would only show up here.
      const storage = new Map();
      const mem = { getItem: k => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, v), removeItem: k => storage.delete(k) };
      const VD = 2;
      const scene = new THREE.Scene();
      const save = new ShilpSaveStore(mem);
      const world = new ShilpWorld(scene, { seed: 1234, viewDistance: VD, saveStore: save });
      streamTo(world, 8, 8);

      // Edits in three chunks, one of them negative: two breaks, two placed bricks.
      const breakA = { x: 5, z: 5 };
      const breakB = { x: -12, z: 3 };
      breakA.y = topSolidY(world, breakA.x, breakA.z);
      breakB.y = topSolidY(world, breakB.x, breakB.z);
      const original = [world.getBlock(breakA.x, breakA.y, breakA.z), world.getBlock(breakB.x, breakB.y, breakB.z)];
      expect(original.every(id => SHILP_BLOCK_REGISTRY[id].solid)).toBe(true);
      world.setBlock(breakA.x, breakA.y, breakA.z, ShilpBlockId.AIR);
      world.setBlock(breakB.x, breakB.y, breakB.z, ShilpBlockId.AIR);
      const placeY = topSolidY(world, 6, 20) + 1;
      world.setBlock(6, placeY, 20, ShilpBlockId.HARAPPAN_BAKED_BRICK);
      world.setBlock(6, placeY + 1, 20, ShilpBlockId.HARAPPAN_BAKED_BRICK);

      const check = w => [
        w.getBlock(breakA.x, breakA.y, breakA.z),
        w.getBlock(breakB.x, breakB.y, breakB.z),
        w.getBlock(6, placeY, 20),
        w.getBlock(6, placeY + 1, 20),
      ];
      const EXPECTED = [ShilpBlockId.AIR, ShilpBlockId.AIR, ShilpBlockId.HARAPPAN_BAKED_BRICK, ShilpBlockId.HARAPPAN_BAKED_BRICK];
      expect(check(world)).toEqual(EXPECTED);
      const editedKeys = ['0,0', '-1,0', '0,1'];
      const editedMeshes = editedKeys.map(k => world.chunks.get(k).mesh);

      // Walk east in 8-block steps (half a chunk per step, like real movement,
      // so the hysteresis path runs) until the edited chunks are beyond VD + 1.
      let x = 8;
      while (editedKeys.some(k => world.chunks.has(k))) {
        x += 8;
        streamTo(world, x, 8);
        expect(x).toBeLessThan(16 * (VD + 4));        // must unload within a few chunks
      }
      // Really gone: voxels dropped and meshes taken out of the scene.
      for (const mesh of editedMeshes) expect(mesh.parent).toBeNull();
      expect(save.editCount).toBe(4);                  // the diff is all that remains

      // Walk back.
      while (x > 8) { x -= 8; streamTo(world, x, 8); }
      expect(editedKeys.every(k => world.chunks.has(k))).toBe(true);
      expect(check(world)).toEqual(EXPECTED);
      // Reloading chunks must not have turned regenerated terrain into edits.
      expect(save.editCount).toBe(4);

      // Page reload: autosave, then a brand-new store and world from storage.
      expect(save.save({ seed: world.seed }).ok).toBe(true);
      const save2 = new ShilpSaveStore(mem);
      const loaded = save2.load();
      const world2 = new ShilpWorld(new THREE.Scene(), { seed: loaded.seed, viewDistance: VD, saveStore: save2 });
      streamTo(world2, 8, 8);
      expect(check(world2)).toEqual(EXPECTED);

      // Control: the same seed WITHOUT the saved diff still has the original
      // terrain, so the checks above are not passing by coincidence.
      const bare = makeWorld({ viewDistance: VD });
      streamTo(bare, 8, 8);
      expect([bare.getBlock(breakA.x, breakA.y, breakA.z), bare.getBlock(breakB.x, breakB.y, breakB.z)]).toEqual(original);
      expect(bare.getBlock(6, placeY, 20)).toBe(ShilpBlockId.AIR);
    });
  });

  describe('quick load (refreshChunks)', () => {
    // The engine's quickLoad() = saveStore.load() + world.refreshChunks(old
    // keys ∪ new keys). This drives that exact sequence on a real world.
    it('reverts edits made after the save and restores the saved ones', () => {
      const storage = new Map();
      const mem = { getItem: k => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, v), removeItem: k => storage.delete(k) };
      const save = new ShilpSaveStore(mem);
      const world = makeWorld({ saveStore: save });
      streamTo(world, 8, 8);

      const kept = { x: 4, z: 4 };
      kept.y = topSolidY(world, kept.x, kept.z);
      world.setBlock(kept.x, kept.y, kept.z, ShilpBlockId.AIR);            // saved edit
      save.save({ seed: world.seed });

      // After the save: undo the saved edit AND make a new one in another chunk.
      world.setBlock(kept.x, kept.y, kept.z, ShilpBlockId.CHUNAR_SANDSTONE);
      const lateY = topSolidY(world, -6, 5) + 1;
      world.setBlock(-6, lateY, 5, ShilpBlockId.HARAPPAN_BAKED_BRICK);    // unsaved edit
      const untouchedMesh = world.chunks.get('0,-1').mesh;

      const touched = new Set(save.diffs.keys());                       // as quickLoad() does
      expect(save.load()).not.toBeNull();
      for (const k of save.diffs.keys()) touched.add(k);
      const rebuilt = world.refreshChunks(touched);

      expect(world.getBlock(kept.x, kept.y, kept.z)).toBe(ShilpBlockId.AIR);        // saved state back
      expect(world.getBlock(-6, lateY, 5)).toBe(ShilpBlockId.AIR);                  // unsaved edit gone
      expect(rebuilt).toBe(2);                                                       // chunks 0,0 and -1,0 only
      expect(world.chunks.get('0,-1').mesh).toBe(untouchedMesh);                    // others left alone
      expect(save.dirty).toBe(false);
    });

    it('ignores keys of chunks that are not loaded (they apply the diff when they stream in)', () => {
      const world = makeWorld();
      expect(world.refreshChunks(['500,500'])).toBe(0);
    });
  });

  describe('streaming', () => {
    it('keeps the loaded set bounded no matter how far the player travels', () => {
      const world = makeWorld({ viewDistance: 2 });
      const limit = (2 * (2 + 1) + 1) ** 2;          // hysteresis radius R + 1
      for (let x = 0; x <= 3000; x += 250) {
        streamTo(world, x, 0);
        expect(world.chunks.size).toBeLessThanOrEqual(limit);
      }
    });

    it('builds no more than the per-frame budget', () => {
      const world = makeWorld({ viewDistance: 3 });
      world.updateStreaming(4000, 4000);             // a whole new area to load
      expect(world.pendingChunkCount).toBeGreaterThan(2);
      expect(world.processStreamingQueue()).toBeLessThanOrEqual(2);
    });

    it('loads nearest chunks first', () => {
      const world = makeWorld({ viewDistance: 3 });
      world.updateStreaming(4000, 4000);
      const cx = Math.floor(4000 / CHUNK_SIZE_X);
      const first = world._loadQueue[0];
      expect([first.cx, first.cz]).toEqual([cx, cx]);   // the player's own chunk
    });

    it('does no streaming work while the player stays in one chunk', () => {
      const world = makeWorld();
      streamTo(world, 2, 2);
      const queue = world._loadQueue;
      world.updateStreaming(9.5, 14.2);               // still chunk (0,0)
      expect(world._loadQueue).toBe(queue);           // untouched: early return
    });
  });

  describe('heritage monuments cannot be broken', () => {
    // The Ashoka Sthambha stands at local (7..8, 25..32, 7..8) of chunk (0,0),
    // on the Harappan terrace at y = 24.
    const PILLAR = { x: 7, y: 25, z: 7 };

    it('generates the Ashoka pillar in the spawn chunk', () => {
      const world = makeWorld();
      expect(world.getBlock(PILLAR.x, PILLAR.y, PILLAR.z)).toBe(ShilpBlockId.ASHOKA_PILLAR_BLOCK);
    });

    it('refuses to break the pillar through the player\'s mining path', () => {
      const world = makeWorld();
      world.targetVoxel = {
        ...PILLAR,
        blockId: ShilpBlockId.ASHOKA_PILLAR_BLOCK,
        meta: SHILP_BLOCK_REGISTRY[ShilpBlockId.ASHOKA_PILLAR_BLOCK],
        normal: new THREE.Vector3(0, 1, 0),
      };
      const res = world.tryBreakTargetVoxel();
      expect(res.success).toBe(false);
      expect(res.wasHeritage).toBe(true);
      expect(world.getBlock(PILLAR.x, PILLAR.y, PILLAR.z)).toBe(ShilpBlockId.ASHOKA_PILLAR_BLOCK);
      expect(warnSpy).toHaveBeenCalled();
    });

    it('does not record a refused break as a player edit', () => {
      const world = makeWorld();
      world.targetVoxel = { ...PILLAR, blockId: ShilpBlockId.ASHOKA_PILLAR_BLOCK,
        meta: SHILP_BLOCK_REGISTRY[ShilpBlockId.ASHOKA_PILLAR_BLOCK], normal: new THREE.Vector3() };
      world.tryBreakTargetVoxel();
      expect(world.saveStore.editCount).toBe(0);
    });

    it.each([
      ['Ashoka Sthambha pillar', ShilpBlockId.ASHOKA_PILLAR_BLOCK],
      ['Great Bath masonry', ShilpBlockId.MOHENJO_GREAT_BATH_BLOCK],
    ])('canBreakVoxel refuses %s', (_name, id) => {
      expect(canBreakVoxel(id)).toBe(false);
    });

    it('the monuments are the ONLY blocks that refuse to break', () => {
      // Guards both directions: a new block marked heritage by mistake, or a
      // monument losing its protection, changes this list and fails here.
      const protectedIds = Object.values(SHILP_BLOCK_REGISTRY)
        .filter(m => m.isHeritage)
        .map(m => m.id)
        .sort((a, b) => a - b);
      expect(protectedIds).toEqual(
        [ShilpBlockId.ASHOKA_PILLAR_BLOCK, ShilpBlockId.MOHENJO_GREAT_BATH_BLOCK].sort((a, b) => a - b)
      );
    });

    it('still lets ordinary blocks be mined', () => {
      const world = makeWorld();
      streamTo(world, 8, 8);
      const y = topSolidY(world, 12, 12);
      const id = world.getBlock(12, y, 12);
      expect(SHILP_BLOCK_REGISTRY[id].isHeritage).toBe(false);
      world.targetVoxel = { x: 12, y, z: 12, blockId: id, meta: SHILP_BLOCK_REGISTRY[id], normal: new THREE.Vector3() };
      expect(world.tryBreakTargetVoxel().success).toBe(true);
      expect(world.getBlock(12, y, 12)).toBe(ShilpBlockId.AIR);
    });
  });

  describe('placeable blocks follow one breaking rule', () => {
    // Review decision: bitumen mortar is a building material, so it must break
    // exactly like the other blocks the player places.
    const placeable = Object.values(SHILP_ITEMS).filter(item => item.isBlock);

    it('bitumen mortar is one of the placeable blocks (so the table below covers it)', () => {
      expect(placeable.map(i => i.blockId)).toContain(ShilpBlockId.BITUMEN_MORTAR);
    });

    it.each(placeable.map(i => [i.id, i.blockId]))(
      '%s: placed through the player path, then mined through the player path',
      (_itemId, blockId) => {
        const world = makeWorld();
        streamTo(world, 8, 8);
        // Aim at the top of a column, as the crosshair does, and place on it.
        const x = 12, z = 3;
        const top = topSolidY(world, x, z);
        const aim = y => ({
          x, y, z,
          blockId: world.getBlock(x, y, z),
          meta: SHILP_BLOCK_REGISTRY[world.getBlock(x, y, z)],
          normal: new THREE.Vector3(0, 1, 0),
        });
        world.targetVoxel = aim(top);
        expect(world.tryPlaceAdjacentVoxel(blockId)).toBe(true);
        expect(world.getBlock(x, top + 1, z)).toBe(blockId);

        // Now mine the block that was just placed.
        world.targetVoxel = aim(top + 1);
        const res = world.tryBreakTargetVoxel();
        expect(res).toMatchObject({ success: true, wasHeritage: false, blockId });
        expect(world.getBlock(x, top + 1, z)).toBe(ShilpBlockId.AIR);
        // The removal is what gets saved, so it stays removed after a reload.
        expect(world.saveStore.getChunkDiff(0, 0).get((x) + 16 * (z + 16 * (top + 1)))).toBe(ShilpBlockId.AIR);
        // The player path only mines what the rule allows.
        expect(canBreakVoxel(blockId)).toBe(true);
      }
    );
  });

  describe('greedy mesher at chunk borders', () => {
    const N = CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z;
    const idx = (x, y, z) => x + CHUNK_SIZE_X * (z + CHUNK_SIZE_Z * y);
    const solidSlab = () => {
      const v = new Uint8Array(N);
      for (let y = 0; y < 4; y++) for (let z = 0; z < 16; z++) for (let x = 0; x < 16; x++) v[idx(x, y, z)] = ShilpBlockId.DECCAN_BASALT;
      return v;
    };
    /** Quads lying exactly on the plane x = 16, the border between the chunks. */
    const borderQuads = geo => {
      const p = geo.attributes.position.array;
      let n = 0;
      for (let i = 0; i < p.length; i += 12) if ([0, 3, 6, 9].every(o => p[i + o] === 16)) n++;
      return n;
    };

    it('draws no face between two solid neighbouring chunks', () => {
      const left = solidSlab();
      const right = solidSlab();
      const world = (wx, wy, wz) => {
        const v = wx < 16 ? left : right;
        const lx = wx < 16 ? wx : wx - 16;
        return lx >= 0 && lx < 16 && wz >= 0 && wz < 16 ? v[idx(lx, wy, wz)] : 0;
      };
      const faces = borderQuads(ShilpGreedyMesher.meshChunk(left, 16, 64, 16, 0, 0, world)) +
                    borderQuads(ShilpGreedyMesher.meshChunk(right, 16, 64, 16, 16, 0, world));
      expect(faces).toBe(0);
    });

    it('draws an exposed border face exactly once (no z-fighting duplicate)', () => {
      const left = solidSlab();
      const right = new Uint8Array(N);                 // open air
      const world = (wx, wy, wz) => {
        const v = wx < 16 ? left : right;
        const lx = wx < 16 ? wx : wx - 16;
        return lx >= 0 && lx < 16 && wz >= 0 && wz < 16 ? v[idx(lx, wy, wz)] : 0;
      };
      const faces = borderQuads(ShilpGreedyMesher.meshChunk(left, 16, 64, 16, 0, 0, world)) +
                    borderQuads(ShilpGreedyMesher.meshChunk(right, 16, 64, 16, 16, 0, world));
      expect(faces).toBe(1);
    });
  });
});
