/**
 * @fileoverview VastuWorld - Master Ancient Voxel World Coordinator & Octree Manager
 * @module world/vastu_world
 * 
 * Features:
 * - Dynamic chunk streaming with infinite world coordinates.
 * - Hierarchical VastuOctree integration for sub-millisecond frustum culling.
 * - Solid AABB collider generation directly feeding VastuPhysics.
 * - Fast Voxel Traversal Algorithm (Amanatides-Woo DDA raymarching) for block interaction.
 * - Strict Ancient Indian Monument preservation validation.
 * - Visual target highlight wireframe with Vedic gold aesthetics.
 */

import * as THREE from 'three';
import { VastuBlockId, VASTU_REGISTRY, canBreakVoxel } from './ancient_blocks.js';
import { PrithviGenerator, CHUNK_WIDTH, CHUNK_HEIGHT, CHUNK_DEPTH } from './prithvi_generator.js';
import { VastuChunk } from './vastu_chunk.js';
import { VastuOctree } from './vastu_octree.js';
import { VastuAABB } from '../physics/vastu_physics.js';

export class VastuWorld {
  /**
   * Initializes world manager, procedural generator, and spatial octree.
   * 
   * @param {THREE.Scene} scene - Three.js scene instance.
   * @param {Object} [config={}] - Optional world parameters.
   */
  constructor(scene, config = {}) {
    this.scene = scene;
    this.seed = config.seed ?? 108;
    this.viewDistance = config.viewDistance ?? 2; // 2 chunks radius (5x5 grid = 25 chunks)

    this.generator = new PrithviGenerator(this.seed);
    this.octree = new VastuOctree();

    /**
     * Map of loaded chunks: `${cx},${cz}` -> VastuChunk
     * @type {Map<string, VastuChunk>}
     */
    this.chunks = new Map();

    /**
     * Set of dirty chunks queued for mesh rebuild.
     * @type {Set<VastuChunk>}
     */
    this.dirtyChunks = new Set();

    this.lastPlayerChunkX = NaN;
    this.lastPlayerChunkZ = NaN;

    // Camera Frustum and Matrices for Octree Culling
    this.frustum = new THREE.Frustum();
    this.projScreenMatrix = new THREE.Matrix4();

    // Voxel selection outline
    this.selectionMesh = this._createSelectionBox();
    this.scene.add(this.selectionMesh);

    /**
     * Currently highlighted target voxel.
     * @type {{ x: number, y: number, z: number, normal: number[], blockId: number }|null}
     */
    this.targetVoxel = null;

    /**
     * Telemetry statistics regarding octree efficiency.
     */
    this.octreeStats = { visible: 0, culled: 0, total: 0, efficiencyPercent: 0 };
  }

  /**
   * Generates selection outline wireframe.
   * 
   * @private
   * @returns {THREE.LineSegments}
   */
  _createSelectionBox() {
    const geom = new THREE.BoxGeometry(1.004, 1.004, 1.004);
    const wire = new THREE.WireframeGeometry(geom);
    const line = new THREE.LineSegments(
      wire,
      new THREE.LineBasicMaterial({ color: 0xd4af37, linewidth: 2, transparent: true, opacity: 0.8 })
    );
    line.visible = false;
    return line;
  }

  /**
   * Translates world coordinate to chunk coordinate.
   * 
   * @param {number} coord - World X or Z coordinate.
   * @returns {number}
   */
  worldToChunkCoord(coord) {
    return Math.floor(coord / CHUNK_WIDTH);
  }

  /**
   * Retrieves or instantiates a chunk at (cx, cz).
   * 
   * @param {number} cx - Chunk X.
   * @param {number} cz - Chunk Z.
   * @returns {VastuChunk}
   */
  getChunk(cx, cz) {
    const key = `${cx},${cz}`;
    return this.chunks.get(key) || null;
  }

  /**
   * Loads or creates a chunk at (cx, cz), generating its procedural voxels.
   * 
   * @param {number} cx - Chunk X.
   * @param {number} cz - Chunk Z.
   * @returns {VastuChunk}
   */
  loadChunk(cx, cz) {
    const key = `${cx},${cz}`;
    let chunk = this.chunks.get(key);
    if (chunk) return chunk;

    chunk = new VastuChunk(this, cx, cz);
    chunk.voxels = this.generator.generateChunkVoxels(cx, cz);
    this.chunks.set(key, chunk);

    // Register chunk in VastuOctree
    this.octree.insert(key, chunk.bounds, chunk);
    this.dirtyChunks.add(chunk);
    return chunk;
  }

  /**
   * Unloads and disposes a chunk at (cx, cz).
   * 
   * @param {number} cx - Chunk X.
   * @param {number} cz - Chunk Z.
   */
  unloadChunk(cx, cz) {
    const key = `${cx},${cz}`;
    const chunk = this.chunks.get(key);
    if (chunk) {
      this.dirtyChunks.delete(chunk);
      this.octree.remove(key);
      chunk.dispose();
      this.chunks.delete(key);
    }
  }

  /**
   * Synchronizes loaded chunks around the player's current position.
   * 
   * @param {THREE.Vector3} playerPos - Player's current world position.
   */
  updateStreaming(playerPos) {
    const pcx = this.worldToChunkCoord(playerPos.x);
    const pcz = this.worldToChunkCoord(playerPos.z);

    if (pcx === this.lastPlayerChunkX && pcz === this.lastPlayerChunkZ) {
      return;
    }

    this.lastPlayerChunkX = pcx;
    this.lastPlayerChunkZ = pcz;

    const r = this.viewDistance;
    const neededKeys = new Set();

    // 1. Stream in Chunks in View Distance
    for (let dx = -r; dx <= r; dx++) {
      for (let dz = -r; dz <= r; dz++) {
        const cx = pcx + dx;
        const cz = pcz + dz;
        const key = `${cx},${cz}`;
        neededKeys.add(key);

        if (!this.chunks.has(key)) {
          this.loadChunk(cx, cz);
        }
      }
    }

    // 2. Unload Chunks outside View Distance
    for (const [key, chunk] of this.chunks.entries()) {
      if (!neededKeys.has(key)) {
        this.unloadChunk(chunk.cx, chunk.cz);
      }
    }

    // 3. Rebuild octree spatial balance
    this.octree.rebuild();
  }

  /**
   * Rebuilds any dirty chunk meshes in queue.
   */
  rebuildDirtyMeshes() {
    if (this.dirtyChunks.size === 0) return;

    for (const chunk of this.dirtyChunks) {
      chunk.buildMesh();
    }
    this.dirtyChunks.clear();
  }

  /**
   * Reads block identifier at integer world coordinates (x, y, z).
   * 
   * @param {number} x - World X.
   * @param {number} y - World Y.
   * @param {number} z - World Z.
   * @returns {number} VastuBlockId.
   */
  getBlock(x, y, z) {
    if (y < 0 || y >= CHUNK_HEIGHT) return VastuBlockId.AIR;

    const cx = this.worldToChunkCoord(x);
    const cz = this.worldToChunkCoord(z);
    const chunk = this.getChunk(cx, cz);
    if (!chunk) return VastuBlockId.AIR;

    const lx = ((x % CHUNK_WIDTH) + CHUNK_WIDTH) % CHUNK_WIDTH;
    const lz = ((z % CHUNK_DEPTH) + CHUNK_DEPTH) % CHUNK_DEPTH;
    return chunk.voxels[chunk.getIndex(lx, y, lz)];
  }

  /**
   * Writes block identifier at integer world coordinates (x, y, z).
   * 
   * @param {number} x - World X.
   * @param {number} y - World Y.
   * @param {number} z - World Z.
   * @param {number} blockId - Block ID to write.
   * @returns {boolean} True if successfully written.
   */
  setBlock(x, y, z, blockId) {
    if (y < 0 || y >= CHUNK_HEIGHT) return false;

    const cx = this.worldToChunkCoord(x);
    const cz = this.worldToChunkCoord(z);
    const chunk = this.getChunk(cx, cz);
    if (!chunk) return false;

    const lx = ((x % CHUNK_WIDTH) + CHUNK_WIDTH) % CHUNK_WIDTH;
    const lz = ((z % CHUNK_DEPTH) + CHUNK_DEPTH) % CHUNK_DEPTH;

    chunk.setBlock(lx, y, lz, blockId);
    this.dirtyChunks.add(chunk);

    // If voxel is on chunk boundary, mark adjacent chunk dirty too
    if (lx === 0) {
      const neighbor = this.getChunk(cx - 1, cz);
      if (neighbor) this.dirtyChunks.add(neighbor);
    } else if (lx === CHUNK_WIDTH - 1) {
      const neighbor = this.getChunk(cx + 1, cz);
      if (neighbor) this.dirtyChunks.add(neighbor);
    }
    if (lz === 0) {
      const neighbor = this.getChunk(cx, cz - 1);
      if (neighbor) this.dirtyChunks.add(neighbor);
    } else if (lz === CHUNK_DEPTH - 1) {
      const neighbor = this.getChunk(cx, cz + 1);
      if (neighbor) this.dirtyChunks.add(neighbor);
    }

    return true;
  }

  /**
   * Attempts to break a block, strictly enforcing ancient monument preservation.
   * 
   * @param {number} x - World X.
   * @param {number} y - World Y.
   * @param {number} z - World Z.
   * @returns {boolean} True if broken, false if denied.
   */
  breakBlock(x, y, z) {
    const id = this.getBlock(x, y, z);
    if (id === VastuBlockId.AIR) return false;

    if (!canBreakVoxel(id)) {
      return false; // Indestructible heritage monument
    }

    this.setBlock(x, y, z, VastuBlockId.AIR);
    return true;
  }

  /**
   * VastuPhysics Collider Provider.
   * Extracts solid voxel AABBs intersecting an expanded candidate box.
   * 
   * @param {import('../physics/vastu_physics.js').VastuAABB} queryBox - Moving entity query AABB.
   * @param {import('../physics/vastu_physics.js').VastuAABB[]} outputColliders - Array to append solid AABBs to.
   */
  getIntersectingBoxes(queryBox, outputColliders) {
    const minX = Math.floor(queryBox.minX);
    const maxX = Math.ceil(queryBox.maxX);
    const minY = Math.max(0, Math.floor(queryBox.minY));
    const maxY = Math.min(CHUNK_HEIGHT - 1, Math.ceil(queryBox.maxY));
    const minZ = Math.floor(queryBox.minZ);
    const maxZ = Math.ceil(queryBox.maxZ);

    for (let x = minX; x < maxX; x++) {
      for (let y = minY; y < maxY; y++) {
        for (let z = minZ; z < maxZ; z++) {
          const id = this.getBlock(x, y, z);
          if (id === VastuBlockId.AIR) continue;
          const meta = VASTU_REGISTRY[id];
          if (meta && meta.solid) {
            outputColliders.push(new VastuAABB(x, y, z, x + 1, y + 1, z + 1));
          }
        }
      }
    }
  }

  /**
   * Fast Voxel Traversal Algorithm (Amanatides-Woo DDA Raymarcher).
   * Accurately determines targeted voxel and hit face normal.
   * 
   * @param {THREE.Vector3} origin - Ray origin (player eye position).
   * @param {THREE.Vector3} direction - Normalized ray direction.
   * @param {number} [maxDistance=6.0] - Reach limit in voxel units.
   * @returns {{ x: number, y: number, z: number, normal: number[], blockId: number }|null}
   */
  raycastVoxel(origin, direction, maxDistance = 6.0) {
    let x = Math.floor(origin.x);
    let y = Math.floor(origin.y);
    let z = Math.floor(origin.z);

    const stepX = direction.x > 0 ? 1 : direction.x < 0 ? -1 : 0;
    const stepY = direction.y > 0 ? 1 : direction.y < 0 ? -1 : 0;
    const stepZ = direction.z > 0 ? 1 : direction.z < 0 ? -1 : 0;

    const tDeltaX = direction.x !== 0 ? Math.abs(1 / direction.x) : Infinity;
    const tDeltaY = direction.y !== 0 ? Math.abs(1 / direction.y) : Infinity;
    const tDeltaZ = direction.z !== 0 ? Math.abs(1 / direction.z) : Infinity;

    const fracX = origin.x - x;
    const fracY = origin.y - y;
    const fracZ = origin.z - z;

    let tMaxX = direction.x > 0 ? (1 - fracX) * tDeltaX : fracX * tDeltaX;
    let tMaxY = direction.y > 0 ? (1 - fracY) * tDeltaY : fracY * tDeltaY;
    let tMaxZ = direction.z > 0 ? (1 - fracZ) * tDeltaZ : fracZ * tDeltaZ;

    let normal = [0, 1, 0];
    let distance = 0;

    while (distance <= maxDistance) {
      const blockId = this.getBlock(x, y, z);
      if (blockId !== VastuBlockId.AIR && blockId !== VastuBlockId.SACRED_WATER) {
        return { x, y, z, normal, blockId };
      }

      if (tMaxX < tMaxY) {
        if (tMaxX < tMaxZ) {
          x += stepX;
          distance = tMaxX;
          tMaxX += tDeltaX;
          normal = [-stepX, 0, 0];
        } else {
          z += stepZ;
          distance = tMaxZ;
          tMaxZ += tDeltaZ;
          normal = [0, 0, -stepZ];
        }
      } else {
        if (tMaxY < tMaxZ) {
          y += stepY;
          distance = tMaxY;
          tMaxY += tDeltaY;
          normal = [0, -stepY, 0];
        } else {
          z += stepZ;
          distance = tMaxZ;
          tMaxZ += tDeltaZ;
          normal = [0, 0, -stepZ];
        }
      }
    }

    return null;
  }

  /**
   * Executes Octree Frustum Culling against the active camera.
   * Executed once per frame during `_process(delta, alpha)`.
   * 
   * @param {THREE.Camera} camera - Active perspective camera.
   */
  updateFrustumCulling(camera) {
    this.projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
    this.frustum.setFromProjectionMatrix(this.projScreenMatrix);
    this.octreeStats = this.octree.cullFrustum(this.frustum);
  }

  /**
   * Updates block target wireframe highlight based on camera gaze.
   * 
   * @param {THREE.Vector3} eyePos - Camera 3D position.
   * @param {THREE.Vector3} lookDir - Normalized camera gaze vector.
   */
  updateTargetHighlight(eyePos, lookDir) {
    this.targetVoxel = this.raycastVoxel(eyePos, lookDir, 6.0);
    if (this.targetVoxel) {
      this.selectionMesh.position.set(
        this.targetVoxel.x + 0.5,
        this.targetVoxel.y + 0.5,
        this.targetVoxel.z + 0.5
      );
      this.selectionMesh.visible = true;
    } else {
      this.selectionMesh.visible = false;
    }
  }
}
