/**
 * @fileoverview ShilpWorld - Master Voxel World & Subcontinent Biome Coordinator
 * @module shilploka/world/shilp_world
 * 
 * Coordinates:
 * - Multi-octave Perlin procedural generation of Indian subcontinent biomes.
 * - Organic Banyan trees with vertical aerial prop roots and Peepal trees.
 * - Procedural Mohenjo-Daro Great Bath and Ashoka Sthambha pillars (strictly indestructible).
 * - Dynamic chunk loading and streaming.
 * - Hierarchical 3D ShilpOctree frustum culling.
 * - DDA raymarching for block interaction and strict heritage preservation.
 */

import * as THREE from 'three';
import { PerlinNoise } from '../../noise.js';
import { ShilpBlockId, SHILP_BLOCK_REGISTRY, canBreakVoxel } from './voxel_constants.js';
import { ShilpChunk, CHUNK_SIZE_X, CHUNK_SIZE_Y, CHUNK_SIZE_Z } from './shilp_chunk.js';
import { ShilpOctree } from './shilp_octree.js';
import { OrganicTreeGenerator } from './organic_trees.js';
import { ShilpGraph } from './shilp_graph.js';
import { randomSeed } from '../core/shilp_save.js';

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
    this.saveStore = config.saveStore ?? null;
    this.viewDistance = config.viewDistance ?? 2; // Radius in chunks (5x5 grid = 25 chunks)
    this.noise = new PerlinNoise(this.seed);

    this.graph = new ShilpGraph();
    this.octree = new ShilpOctree(256);
    this.chunks = new Map(); // key: `${cx},${cz}` -> ShilpChunk


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
    this.updateStreaming(start.x, start.z, true);
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

    // Rebuild chunk mesh immediately
    chunk.buildMesh(this.material);
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

  /**
   * Create one chunk: generate terrain, re-apply the player's saved edits,
   * build its mesh and register it.
   *
   * WHY the order matters: edits are applied AFTER generation (so they
   * overwrite the procedural blocks) but BEFORE meshing (so the very first
   * mesh already shows the player's buildings, with no flicker).
   *
   * USED BY: chunk streaming. It is the single place a chunk comes into
   * existence, which is why edits survive a chunk being unloaded and loaded
   * again - they are re-applied here every time.
   *
   * @returns {ShilpChunk}
   */
  _loadChunk(cx, cz) {
    const chunk = new ShilpChunk(cx, cz);
    this.generateChunkData(chunk);
    if (this.saveStore) this.saveStore.applyToChunk(chunk);
    const mesh = chunk.buildMesh(this.material);
    if (mesh) {
      this.scene.add(mesh);
    }
    this.chunks.set(this.getChunkKey(cx, cz), chunk);
    this.octree.registerChunk(chunk);
    return chunk;
  }

  /**
   * Updates streaming around player position.
   */
  updateStreaming(playerWorldX, playerWorldZ, forceRebuild = false) {
    const centerChunkX = Math.floor(playerWorldX / CHUNK_SIZE_X);
    const centerChunkZ = Math.floor(playerWorldZ / CHUNK_SIZE_Z);

    const radius = this.viewDistance;
    const activeKeys = new Set();
    let didLoadNewChunks = false;

    for (let dx = -radius; dx <= radius; dx++) {
      for (let dz = -radius; dz <= radius; dz++) {
        const cx = centerChunkX + dx;
        const cz = centerChunkZ + dz;
        const key = this.getChunkKey(cx, cz);
        activeKeys.add(key);

        if (!this.chunks.has(key)) {
          this._loadChunk(cx, cz);
          didLoadNewChunks = true;
        } else if (forceRebuild) {
          const chunk = this.chunks.get(key);
          const mesh = chunk.buildMesh(this.material);
          if (mesh && !mesh.parent) {
            this.scene.add(mesh);
          }
        }
      }
    }

    if (didLoadNewChunks) {
      this.octree.rebuild(this.chunks.values());
    }
  }

  /**
   * Executes Octree frustum culling.
   */
  cullFrustum(camera) {
    this.projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse);
    this.frustum.setFromProjectionMatrix(this.projScreenMatrix);
    return this.octree.updateFrustumCulling(this.frustum);
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
