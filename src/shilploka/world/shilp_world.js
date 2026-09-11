/**
 * @fileoverview ShilpWorld - Master Voxel World & Subcontinent Biome Coordinator
 * @module shilploka/world/shilp_world
 * 
 * Coordinates:
 * - Multi-octave Perlin procedural generation of Indian subcontinent biomes.
 * - Organic Banyan trees with vertical aerial prop roots and Peepal trees.
 * - Procedural Mohenjo-Daro Great Bath and Ashoka Sthambha pillars (strictly indestructible).
 * - Chunk streaming: nearest-first loading, a per-frame budget, and
 *   unloading with hysteresis (see updateStreaming()).
 * - Per-chunk frustum culling (see cullFrustum()).
 * - DDA raymarching for block interaction and strict heritage preservation.
 */

import * as THREE from 'three';
import { PerlinNoise } from '../../noise.js';
import { ShilpBlockId, SHILP_BLOCK_REGISTRY, canBreakVoxel } from './voxel_constants.js';
import { ShilpChunk, CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z } from './shilp_chunk.js';
import { OrganicTreeGenerator } from './organic_trees.js';
import { ShilpGraph } from './shilp_graph.js';
import { randomSeed, ShilpSaveStore } from '../core/shilp_save.js';

export const SEA_LEVEL = 18;

export class ShilpWorld {
  /**
   * @param {THREE.Scene} scene - Scene instance.
   * @param {Object} [config={}] - Configuration options.
   */
  constructor(scene, config = {}) {
    this.scene = scene;
    // WHY random: the seed used to default to the fixed number 1008, so every
    // player got the identical map. The engine now passes either the saved
    // world's seed (to regenerate the same terrain) or a fresh random one.
    this.seed = config.seed ?? randomSeed();

    /**
     * Save store holding the player's edits (see core/shilp_save.js).
     * USED FOR: re-applying edits whenever a chunk is generated, and recording
     * every edit made through setBlock(). Optional -- tests and tools can build
     * a world without persistence.
     * @type {import('../core/shilp_save.js').ShilpSaveStore|null}
     */
    //
    // WHY never null: once chunks unload, chunk.voxels is thrown away, so the
    // save store's diff map becomes the ONLY copy of the player's edits. A
    // world built without one (a test, a tool) would silently lose every edit
    // the first time a chunk streamed out. An in-memory store with no storage
    // backend keeps edits alive for the session; it just cannot persist them.
    this.saveStore = config.saveStore ?? new ShilpSaveStore(null);
    // Load radius in chunks, passed in by the engine. A radius of R keeps a
    // (2R+1) x (2R+1) square of chunks loaded.
    this.viewDistance = config.viewDistance ?? 4;
    this.noise = new PerlinNoise(this.seed);

    this.graph = new ShilpGraph();
    this.chunks = new Map(); // key: `${cx},${cz}` -> ShilpChunk

    // ── Streaming state ────────────────────────────────────────────────────
    /** Chunk the player was last in. Streaming is recomputed only when it
     *  changes, instead of scanning the whole view square every frame. */
    this._centerKey = null;
    this._centerCX = 0;
    this._centerCZ = 0;

    /** Chunks still to be generated, NEAREST FIRST. Drained a little each
     *  frame by processStreamingQueue(), which is what removes the stutter. */
    this._loadQueue = [];

    /** Chunks whose mesh is stale because a neighbour loaded beside them.
     *  Rebuilt with any per-frame budget left over after loading. */
    this._remeshQueue = new Set();

    /** At most this many chunk meshes are built per frame. WHY 2: generating
     *  and meshing one chunk takes a few milliseconds; two stays well inside a
     *  16.7 ms frame, and at 60 fps that is still 120 chunks per second - far
     *  faster than anyone can walk. */
    this.chunksPerFrame = config.chunksPerFrame ?? 2;


    // Shared high-performance vertex-colored material
    this.material = new THREE.MeshLambertMaterial({
      vertexColors: true,
      reflectivity: 0.2,
    });

    // Frustum and matrices for camera culling
    this.frustum = new THREE.Frustum();
    this.projScreenMatrix = new THREE.Matrix4();

    // Visual selection bounding box for targeted voxels
    this.targetVoxel = null; // { x, y, z, faceNormal, blockId }
    this.selectionBox = this._createSelectionOutline();
    this.scene.add(this.selectionBox);

    // Initial chunk generation. WHY a configurable centre: with saves, the
    // player may resume far from the origin; generating around (0,0) first
    // would build chunks they are nowhere near while their own area is empty.
    const start = config.initialCenter ?? { x: 0, z: 0 };
    this.updateStreaming(start.x, start.z);
    // Build the ground under the player RIGHT NOW. WHY: everything else is
    // spread over later frames, but if the player's own chunk were not there
    // on the first physics step, getBlock() would answer AIR everywhere and
    // the player would fall straight through the world.
    this.processStreamingQueue(9);
  }

  /**
   * Translates chunk coordinates to map key.
   */
  getChunkKey(cx, cz) {
    return `${cx},${cz}`;
  }

  /**
   * Gets block at world voxel coordinates.
   */
  getBlock(wx, wy, wz) {
    if (wy < 0 || wy >= CHUNK_SIZE_Y) return ShilpBlockId.AIR;

    const cx = Math.floor(wx / CHUNK_SIZE_X);
    const cz = Math.floor(wz / CHUNK_SIZE_Z);
    const chunk = this.chunks.get(this.getChunkKey(cx, cz));
    if (!chunk) return ShilpBlockId.AIR;

    const lx = ((wx % CHUNK_SIZE_X) + CHUNK_SIZE_X) % CHUNK_SIZE_X;
    const lz = ((wz % CHUNK_SIZE_Z) + CHUNK_SIZE_Z) % CHUNK_SIZE_Z;
    return chunk.getBlock(lx, wy, lz);
  }

  /**
   * Sets block at world voxel coordinates.
   */
  setBlock(wx, wy, wz, blockId) {
    if (wy < 0 || wy >= CHUNK_SIZE_Y) return false;

    const cx = Math.floor(wx / CHUNK_SIZE_X);
    const cz = Math.floor(wz / CHUNK_SIZE_Z);
    const chunk = this.chunks.get(this.getChunkKey(cx, cz));
    if (!chunk) return false;

    const lx = ((wx % CHUNK_SIZE_X) + CHUNK_SIZE_X) % CHUNK_SIZE_X;
    const lz = ((wz % CHUNK_SIZE_Z) + CHUNK_SIZE_Z) % CHUNK_SIZE_Z;
    chunk.setBlock(lx, wy, lz, blockId);

    // SAVE HOOK. WHY here: setBlock() is the only path player actions take
    // (tryBreakTargetVoxel / tryPlaceAdjacentVoxel). World generation writes
    // to chunks directly and never passes through here, so the save records
    // exactly the player's edits and nothing procedural.
    // NEXT: the edit is written to storage on the next autosave.
    if (this.saveStore) {
      this.saveStore.recordEdit(cx, cz, chunk.getIndex(lx, wy, lz), blockId);
    }

    // Rebuild this chunk's mesh immediately so the edit shows this frame.
    this._meshChunk(chunk);

    // EDGE EDITS. If the edited voxel sits on a chunk border, the chunk on the
    // other side must be rebuilt too, immediately. WHY: faces on a shared
    // border are drawn only by the chunk that owns the solid voxel. Breaking a
    // border block exposes the NEIGHBOUR's face, which that neighbour has not
    // drawn; without this rebuild the player would see a see-through hole.
    if (lx === 0) this._remeshNow(cx - 1, cz);
    if (lx === CHUNK_SIZE_X - 1) this._remeshNow(cx + 1, cz);
    if (lz === 0) this._remeshNow(cx, cz - 1);
    if (lz === CHUNK_SIZE_Z - 1) this._remeshNow(cx, cz + 1);
    return true;
  }

  /**
   * Computes natural terrain surface height at world coordinates (wx, wz).
   */
  getTerrainHeight(wx, wz) {
    // Multi-octave Fractal Brownian Motion (FBM)
    const n1 = this.noise.noise2D(wx * 0.008, wz * 0.008) * 18;
    const n2 = this.noise.noise2D(wx * 0.024, wz * 0.024) * 6;
    const n3 = this.noise.noise2D(wx * 0.06, wz * 0.06) * 2;
    const baseHeight = 24 + Math.floor(n1 + n2 + n3);
    return Math.max(4, Math.min(CHUNK_SIZE_Y - 14, baseHeight));
  }

  /**
   * Generates procedural voxel data for a newly loaded chunk.
   */
  generateChunkData(chunk) {
    const wx0 = chunk.worldX;
    const wz0 = chunk.worldZ;

    for (let lz = 0; lz < CHUNK_SIZE_Z; lz++) {
      for (let lx = 0; lx < CHUNK_SIZE_X; lx++) {
        const wx = wx0 + lx;
        const wz = wz0 + lz;
        const surfaceY = this.getTerrainHeight(wx, wz);

        // Bedrock foundation
        chunk.setBlock(lx, 0, lz, ShilpBlockId.DECCAN_BASALT);

        // Underground strata layers
        for (let y = 1; y <= surfaceY; y++) {
          if (y < surfaceY - 5) {
            // Deep stone & mineral veins
            const oreSample = Math.abs(this.noise.noise3D(wx * 0.1, y * 0.1, wz * 0.1));
            if (y <= 12 && oreSample > 0.72) {
              chunk.setBlock(lx, y, lz, ShilpBlockId.LAJWARD_LAPIS);
            } else if (y <= 24 && oreSample > 0.65) {
              chunk.setBlock(lx, y, lz, ShilpBlockId.KANSA_BRONZE_ORE);
            } else {
              chunk.setBlock(lx, y, lz, ShilpBlockId.DECCAN_BASALT);
            }
          } else if (y < surfaceY) {
            // Sindhu Alluvial Soil
            chunk.setBlock(lx, y, lz, ShilpBlockId.SINDHU_ALLUVIAL_SOIL);
          } else {
            // Surface layer: Check if on Ancient Trade Route
            const road = this.graph.getRoadSurfaceInfo(wx, wz);
            if (road) {
              chunk.setBlock(lx, surfaceY, lz, road.blockId);
            } else if (surfaceY <= SEA_LEVEL + 1) {
              chunk.setBlock(lx, surfaceY, lz, ShilpBlockId.CHUNAR_SANDSTONE);
            } else {
              chunk.setBlock(lx, surfaceY, lz, ShilpBlockId.SINDHU_GRASS);
            }
          }
        }

        // River water filling depressions below sea level
        if (surfaceY < SEA_LEVEL) {
          for (let y = surfaceY + 1; y <= SEA_LEVEL; y++) {
            chunk.setBlock(lx, y, lz, ShilpBlockId.SACRED_WATER);
          }
        }
      }
    }

    // Procedural Monuments in central chunk (0, 0)
    if (chunk.chunkX === 0 && chunk.chunkZ === 0) {
      this._generateHarappanHeritagePlaza(chunk);
    }

    // Procedural Organic Trees
    if (Math.abs(chunk.chunkX) > 0 || Math.abs(chunk.chunkZ) > 0) {
      const treeNoise = Math.abs(this.noise.noise2D(chunk.chunkX * 13.5, chunk.chunkZ * 17.2));
      if (treeNoise > 0.55) {
        const tx = 8;
        const tz = 8;
        const wx = wx0 + tx;
        const wz = wz0 + tz;
        const ty = this.getTerrainHeight(wx, wz);

        if (ty > SEA_LEVEL + 1) {
          const writer = {
            setVoxel: (x, y, z, id) => {
              if (x >= wx0 && x < wx0 + CHUNK_SIZE_X && z >= wz0 && z < wz0 + CHUNK_SIZE_Z) {
                chunk.setBlock(x - wx0, y, z - wz0, id);
              }
            },
            getVoxel: (x, y, z) => {
              if (x >= wx0 && x < wx0 + CHUNK_SIZE_X && z >= wz0 && z < wz0 + CHUNK_SIZE_Z) {
                return chunk.getBlock(x - wx0, y, z - wz0);
              }
              return ShilpBlockId.AIR;
            },
            getGroundHeight: (x, z) => this.getTerrainHeight(x, z),
          };

          if (treeNoise > 0.78) {
            OrganicTreeGenerator.generateBanyanTree(writer, wx, ty, wz, 6, 5);
          } else {
            OrganicTreeGenerator.generatePeepalTree(writer, wx, ty, wz, 7);
          }
        }
      }
    }
  }

  /**
   * Procedural Harappan Citadel & Ashoka Pillar Monument.
   * Marked with isHeritage: true (Indestructible invariant).
   * @private
   */
  _generateHarappanHeritagePlaza(chunk) {
    const groundY = 24;

    // 1. Raised Harappan Citadel Baked Brick Terrace
    for (let lx = 1; lx < 15; lx++) {
      for (let lz = 1; lz < 15; lz++) {
        chunk.setBlock(lx, groundY, lz, ShilpBlockId.HARAPPAN_BAKED_BRICK);
      }
    }

    // 2. Ashoka Sthambha Pillar at center (7, 7) - STRICTLY INDESTRUCTIBLE
    for (let py = 1; py <= 8; py++) {
      chunk.setBlock(7, groundY + py, 7, ShilpBlockId.ASHOKA_PILLAR_BLOCK);
      chunk.setBlock(8, groundY + py, 7, ShilpBlockId.ASHOKA_PILLAR_BLOCK);
      chunk.setBlock(7, groundY + py, 8, ShilpBlockId.ASHOKA_PILLAR_BLOCK);
      chunk.setBlock(8, groundY + py, 8, ShilpBlockId.ASHOKA_PILLAR_BLOCK);
    }

    // 3. Ancient Stepped Ghat (0.5m/1-block risers for auto step-up testing)
    for (let step = 0; step < 4; step++) {
      const stepY = groundY + step;
      const stepZ = 14 - step;
      for (let lx = 4; lx <= 11; lx++) {
        chunk.setBlock(lx, stepY, stepZ, ShilpBlockId.HARAPPAN_BAKED_BRICK);
      }
    }
  }

  // ═════════════════════════════════════════════════════════ CHUNK LIFECYCLE

  /**
   * Build (or rebuild) a chunk's mesh, looking across its borders.
   * USED BY: loading, edits, and neighbour remeshes - the one place a mesh is
   * made, so every mesh is border-aware.
   */
  _meshChunk(chunk) {
    const mesh = chunk.buildMesh(this.material, this._worldBlockReader);
    // buildMesh() only creates the THREE.Mesh the first time; after that it
    // swaps geometry in place. Add it to the scene only when it is new.
    if (mesh && !mesh.parent) this.scene.add(mesh);
    return mesh;
  }

  /**
   * Bound world getBlock, created once and reused. WHY cached: the mesher
   * calls it thousands of times per chunk, and allocating a new closure for
   * every mesh would be pointless garbage.
   */
  get _worldBlockReader() {
    if (!this.__reader) this.__reader = (wx, wy, wz) => this.getBlock(wx, wy, wz);
    return this.__reader;
  }

  /** Rebuild a neighbour's mesh now, if it is loaded. USED BY edge edits. */
  _remeshNow(cx, cz) {
    const neighbour = this.chunks.get(this.getChunkKey(cx, cz));
    if (neighbour) {
      this._remeshQueue.delete(this.getChunkKey(cx, cz));
      this._meshChunk(neighbour);
    }
  }

  /**
   * Create one chunk: generate terrain, re-apply the player's saved edits,
   * build its mesh and register it.
   *
   * WHY the order matters: edits are applied AFTER generation (so they
   * overwrite the procedural blocks) but BEFORE meshing (so the very first
   * mesh already shows the player's buildings, with no flicker).
   *
   * WHY edits survive unloading: this is the single place a chunk comes into
   * existence, and it re-applies the saved diff every time. A chunk that
   * streamed out and back in is regenerated from the seed, then corrected.
   *
   * NEXT: the 4 neighbours are queued for a remesh. They were meshed while
   * this chunk did not exist, so they drew faces along the shared border as
   * if it were open air. Those faces are now buried against solid ground.
   *
   * @returns {ShilpChunk}
   */
  _loadChunk(cx, cz) {
    const chunk = new ShilpChunk(cx, cz);
    this.generateChunkData(chunk);
    this.saveStore.applyToChunk(chunk);
    this.chunks.set(this.getChunkKey(cx, cz), chunk);
    this._meshChunk(chunk);

    for (const [nx, nz] of [[cx - 1, cz], [cx + 1, cz], [cx, cz - 1], [cx, cz + 1]]) {
      const key = this.getChunkKey(nx, nz);
      if (this.chunks.has(key)) this._remeshQueue.add(key);
    }
    return chunk;
  }

  /**
   * Remove a chunk from memory and from the GPU.
   *
   * WHY this is safe for the player's edits: they live in saveStore, not in
   * the chunk. The voxels thrown away here are pure regenerable terrain plus
   * edits that are already recorded; _loadChunk() puts both back.
   */
  _unloadChunk(key) {
    const chunk = this.chunks.get(key);
    if (!chunk) return;
    // dispose() frees the GPU geometry AND removes the mesh from the scene.
    // Without it every chunk ever visited would keep its vertex buffers alive
    // - memory that only grows the longer the player walks.
    chunk.dispose();
    this.chunks.delete(key);
    this._remeshQueue.delete(key);
  }

  /**
   * Rebuild the given chunks from terrain + the save store's CURRENT diff.
   *
   * WHY: quick load swaps the diff for the saved one. A loaded chunk still
   * holds voxels built from the old diff, so it must be regenerated. Chunks
   * that are not loaded need nothing: _loadChunk() applies the new diff when
   * they stream in.
   *
   * WHY only some chunks: a chunk with no edits in either diff is pure
   * terrain from the same seed - identical before and after - so rebuilding
   * it would only cost time. The caller passes the union of both diffs' keys.
   *
   * NEXT: _loadChunk() meshes each one immediately and queues its
   * neighbours for a border remesh.
   *
   * @param {Iterable<string>} keys - "cx,cz" chunk keys.
   * @returns {number} How many loaded chunks were rebuilt.
   */
  refreshChunks(keys) {
    let rebuilt = 0;
    for (const key of keys) {
      const old = this.chunks.get(key);
      if (!old) continue;
      this._unloadChunk(key);
      this._loadChunk(old.chunkX, old.chunkZ);
      rebuilt++;
    }
    // The block under the crosshair may have just changed or vanished.
    this.targetVoxel = null;
    return rebuilt;
  }

  // ═══════════════════════════════════════════════════════════════ STREAMING

  /**
   * Decide which chunks should be loaded, around the player.
   *
   * CALLED every frame by the engine, but does real work only when the player
   * crosses into a different chunk. WHY: the loaded set can only change when
   * the centre chunk changes. Re-scanning the view square every frame, as
   * this did before, spent work each frame to reach the same answer.
   *
   * HYSTERESIS: chunks LOAD within viewDistance but only UNLOAD beyond
   * viewDistance + 1. WHY: with a single radius, a player pacing back and
   * forth across a chunk border would make the far row load, unload, load,
   * unload - regenerating terrain every few steps. The one-chunk gap means a
   * row has to be clearly left behind before it is dropped.
   *
   * NEXT: missing chunks go into the nearest-first queue; processStreamingQueue()
   * builds them a couple at a time.
   *
   * @param {number} playerWorldX
   * @param {number} playerWorldZ
   * @param {boolean} [force=false] - Recompute even without a boundary crossing.
   */
  updateStreaming(playerWorldX, playerWorldZ, force = false) {
    const cx0 = Math.floor(playerWorldX / CHUNK_SIZE_X);
    const cz0 = Math.floor(playerWorldZ / CHUNK_SIZE_Z);
    const centerKey = this.getChunkKey(cx0, cz0);
    if (!force && centerKey === this._centerKey) return;   // same chunk: nothing to do

    this._centerKey = centerKey;
    this._centerCX = cx0;
    this._centerCZ = cz0;

    const loadR = this.viewDistance;
    const unloadR = this.viewDistance + 1;

    // 1. Everything that SHOULD be loaded. `activeKeys` was computed by the old
    //    code and then never used; it now drives unloading below.
    const activeKeys = new Set();
    const missing = [];
    for (let dx = -loadR; dx <= loadR; dx++) {
      for (let dz = -loadR; dz <= loadR; dz++) {
        const key = this.getChunkKey(cx0 + dx, cz0 + dz);
        activeKeys.add(key);
        if (!this.chunks.has(key)) {
          missing.push({ cx: cx0 + dx, cz: cz0 + dz, d2: dx * dx + dz * dz });
        }
      }
    }

    // 2. Nearest first. WHY: the ground under and just ahead of the player
    //    matters most; the far edge is hidden in fog anyway. The queue is
    //    rebuilt from scratch on each crossing, which also drops entries that
    //    fell out of range before they were ever built.
    missing.sort((a, b) => a.d2 - b.d2);
    this._loadQueue = missing;

    // 3. Unload beyond the hysteresis radius. Chebyshev distance (the larger of
    //    |dx| and |dz|) because the loaded area is a square, not a circle.
    for (const [key, chunk] of this.chunks) {
      if (activeKeys.has(key)) continue;
      const dist = Math.max(Math.abs(chunk.chunkX - cx0), Math.abs(chunk.chunkZ - cz0));
      if (dist > unloadR) this._unloadChunk(key);
    }
  }

  /**
   * Build up to `budget` chunks from the queue. CALLED once per frame.
   *
   * WHY a budget: loading used to happen all at once - crossing a border
   * generated and meshed a whole row of chunks in a single frame, which is
   * the stutter the player felt. Spreading it keeps every frame short.
   *
   * Leftover budget goes to neighbour remeshes. Those only remove faces that
   * are buried inside solid terrain, so delaying them is invisible; loading
   * the ground ahead of the player is always more urgent.
   *
   * @param {number} [budget=this.chunksPerFrame]
   * @returns {number} Mesh builds performed this frame.
   */
  processStreamingQueue(budget = this.chunksPerFrame) {
    let done = 0;
    while (done < budget && this._loadQueue.length > 0) {
      const { cx, cz } = this._loadQueue.shift();
      if (this.chunks.has(this.getChunkKey(cx, cz))) continue;  // loaded meanwhile
      this._loadChunk(cx, cz);
      done++;
    }
    for (const key of this._remeshQueue) {
      if (done >= budget) break;
      this._remeshQueue.delete(key);
      const chunk = this.chunks.get(key);
      if (chunk) { this._meshChunk(chunk); done++; }
    }
    return done;
  }

  /** Chunks still waiting to be generated. USED BY the HUD and tests. */
  get pendingChunkCount() {
    return this._loadQueue.length;
  }

  // ═════════════════════════════════════════════════════════════════ CULLING

  /**
   * Hide chunks the camera cannot see.
   *
   * WHY a plain per-chunk test replaced the octree:
   *  1. The octree's root box was fixed at +/-256 blocks around the origin.
   *     A chunk outside it was never inserted, so it was in neither the
   *     visible nor the culled set and its visibility was never changed:
   *     far chunks were always drawn.
   *  2. Worse, a chunk straddling an octant boundary was stored in only the
   *     first child that touched it. When that child was off-screen the chunk
   *     was culled even though part of it was in view. Measured over 96
   *     camera angles on a 9x9 grid, it hid a chunk the camera could see 943
   *     times -- holes flickering at the edges of the screen.
   *  3. The octree only earns its keep with thousands of objects. At view
   *     distance 4 there are about 81 chunks. 81 box-vs-frustum tests cost a
   *     few microseconds, and the octree was being rebuilt in O(N) on every
   *     load anyway.
   *
   * A direct test over the loaded chunks has none of those failure modes and
   * no size limit. Because unloading keeps this.chunks to the chunks near the
   * player, it only ever loops over ACTIVE chunks.
   *
   * @returns {{ total: number, visible: number, culled: number, cullingRatio: number }}
   *   Same shape as before, so the HUD needs no change.
   */
  cullFrustum(camera) {
    this.projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
    this.frustum.setFromProjectionMatrix(this.projScreenMatrix);

    let visible = 0;
    let total = 0;
    for (const chunk of this.chunks.values()) {
      // No mesh, or a mesh whose geometry was released because the chunk
      // became entirely empty (buildMesh sets geometry = null then). Keep it
      // hidden: Three.js throws when asked to render a mesh with no geometry.
      if (!chunk.mesh || !chunk.mesh.geometry) {
        if (chunk.mesh) chunk.mesh.visible = false;
        continue;
      }
      total++;
      const inView = this.frustum.intersectsBox(chunk.aabb);
      chunk.mesh.visible = inView;
      if (inView) visible++;
    }
    const culled = total - visible;
    return { total, visible, culled, cullingRatio: total > 0 ? (culled / total) * 100 : 0 };
  }

  /**
   * Amanatides-Woo Fast Voxel Traversal (DDA raymarching) to find targeted block.
   */
  raycastVoxel(camera, maxDistance = 6.0) {
    const rayOrigin = camera.position;
    const rayDir = new THREE.Vector3(0, 0, -1).applyQuaternion(camera.quaternion).normalize();

    let x = Math.floor(rayOrigin.x);
    let y = Math.floor(rayOrigin.y);
    let z = Math.floor(rayOrigin.z);

    const stepX = Math.sign(rayDir.x);
    const stepY = Math.sign(rayDir.y);
    const stepZ = Math.sign(rayDir.z);

    const tDeltaX = stepX !== 0 ? Math.abs(1 / rayDir.x) : Infinity;
    const tDeltaY = stepY !== 0 ? Math.abs(1 / rayDir.y) : Infinity;
    const tDeltaZ = stepZ !== 0 ? Math.abs(1 / rayDir.z) : Infinity;

    let tMaxX = stepX > 0 ? (x + 1 - rayOrigin.x) * tDeltaX : (rayOrigin.x - x) * tDeltaX;
    let tMaxY = stepY > 0 ? (y + 1 - rayOrigin.y) * tDeltaY : (rayOrigin.y - y) * tDeltaY;
    let tMaxZ = stepZ > 0 ? (z + 1 - rayOrigin.z) * tDeltaZ : (rayOrigin.z - z) * tDeltaZ;

    let distanceTraveled = 0;
    let normalX = 0;
    let normalY = 0;
    let normalZ = 0;

    while (distanceTraveled <= maxDistance) {
      const blockId = this.getBlock(x, y, z);
      const meta = SHILP_BLOCK_REGISTRY[blockId];

      if (meta && meta.solid) {
        this.targetVoxel = {
          x,
          y,
          z,
          blockId,
          normal: new THREE.Vector3(normalX, normalY, normalZ),
          meta,
        };
        this.selectionBox.position.set(x + 0.5, y + 0.5, z + 0.5);
        this.selectionBox.visible = true;
        return this.targetVoxel;
      }

      if (tMaxX < tMaxY) {
        if (tMaxX < tMaxZ) {
          x += stepX;
          distanceTraveled = tMaxX;
          tMaxX += tDeltaX;
          normalX = -stepX;
          normalY = 0;
          normalZ = 0;
        } else {
          z += stepZ;
          distanceTraveled = tMaxZ;
          tMaxZ += tDeltaZ;
          normalX = 0;
          normalY = 0;
          normalZ = -stepZ;
        }
      } else {
        if (tMaxY < tMaxZ) {
          y += stepY;
          distanceTraveled = tMaxY;
          tMaxY += tDeltaY;
          normalX = 0;
          normalY = -stepY;
          normalZ = 0;
        } else {
          z += stepZ;
          distanceTraveled = tMaxZ;
          tMaxZ += tDeltaZ;
          normalX = 0;
          normalY = 0;
          normalZ = -stepZ;
        }
      }
    }

    this.targetVoxel = null;
    this.selectionBox.visible = false;
    return null;
  }

  /**
   * Strictly attempts to break the targeted voxel with low-level invariant validation.
   * 
   * @returns {{ success: boolean, wasHeritage: boolean, blockName: string }}
   */
  tryBreakTargetVoxel() {
    if (!this.targetVoxel) {
      return { success: false, wasHeritage: false, blockName: 'Air', blockId: ShilpBlockId.AIR };
    }

    const { x, y, z, blockId, meta } = this.targetVoxel;

    // Strict validation rule: if block.is_heritage { deny_break() }
    if (!canBreakVoxel(blockId)) {
      return {
        success: false,
        wasHeritage: true,
        blockName: meta.name,
        blockId,
      };
    }

    this.setBlock(x, y, z, ShilpBlockId.AIR);
    return {
      success: true,
      wasHeritage: false,
      blockName: meta.name,
      blockId,
    };
  }

  /**
   * Places a voxel adjacent to the targeted face.
   */
  tryPlaceAdjacentVoxel(blockId) {
    if (!this.targetVoxel) return false;
    const placeX = this.targetVoxel.x + this.targetVoxel.normal.x;
    const placeY = this.targetVoxel.y + this.targetVoxel.normal.y;
    const placeZ = this.targetVoxel.z + this.targetVoxel.normal.z;

    return this.setBlock(placeX, placeY, placeZ, blockId);
  }

  _createSelectionOutline() {
    const geom = new THREE.BoxGeometry(1.002, 1.002, 1.002);
    const edges = new THREE.EdgesGeometry(geom);
    const line = new THREE.LineSegments(
      edges,
      new THREE.LineBasicMaterial({ color: 0xffd700, linewidth: 2 })
    );
    line.visible = false;
    return line;
  }
}
