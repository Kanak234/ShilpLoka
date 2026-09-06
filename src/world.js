/**
 * ============================================================================
 * INFINITE CHUNKED WORLD & VOXEL MESHING ENGINE
 * ============================================================================
 * Manages procedural terrain generation using multi-octave Perlin noise,
 * infinite chunk streaming based on player distance, block storage via
 * typed arrays, delta modification tracking for persistence, and optimized
 * face-culled mesh generation with ambient occlusion and face lighting.
 */

import * as THREE from 'three';
import {
  CHUNK_SIZE_X,
  CHUNK_SIZE_Y,
  CHUNK_SIZE_Z,
  SEA_LEVEL,
  VIEW_DISTANCE,
  BLOCKS,
  REGISTRY,
} from './constants.js';
import { BLOCK_FACES } from './textures.js';

// Face directional offsets and vertex coordinates
// Faces: 0: +X, 1: -X, 2: +Y, 3: -Y, 4: +Z, 5: -Z
const FACES = [
  {
    dir: [1, 0, 0],
    name: 'east',
    corners: [
      [1, 0, 0], [1, 1, 0], [1, 1, 1], [1, 0, 1]
    ],
    normal: [1, 0, 0],
    shade: 0.75,
  },
  {
    dir: [-1, 0, 0],
    name: 'west',
    corners: [
      [0, 0, 1], [0, 1, 1], [0, 1, 0], [0, 0, 0]
    ],
    normal: [-1, 0, 0],
    shade: 0.75,
  },
  {
    dir: [0, 1, 0],
    name: 'top',
    corners: [
      [0, 1, 1], [1, 1, 1], [1, 1, 0], [0, 1, 0]
    ],
    normal: [0, 1, 0],
    shade: 1.0,
  },
  {
    dir: [0, -1, 0],
    name: 'bottom',
    corners: [
      [0, 0, 0], [1, 0, 0], [1, 0, 1], [0, 0, 1]
    ],
    normal: [0, -1, 0],
    shade: 0.5,
  },
  {
    dir: [0, 0, 1],
    name: 'south',
    corners: [
      [1, 0, 1], [1, 1, 1], [0, 1, 1], [0, 0, 1]
    ],
    normal: [0, 0, 1],
    shade: 0.85,
  },
  {
    dir: [0, 0, -1],
    name: 'north',
    corners: [
      [0, 0, 0], [0, 1, 0], [1, 1, 0], [1, 0, 0]
    ],
    normal: [0, 0, -1],
    shade: 0.65,
  },
];

export class Chunk {
  /**
   * Represents a single 16x64x16 voxel column in the world.
   */
  constructor(world, cx, cz) {
    this.world = world;
    this.cx = cx;
    this.cz = cz;
    this.worldX = cx * CHUNK_SIZE_X;
    this.worldZ = cz * CHUNK_SIZE_Z;

    // Flat typed array for optimal memory and cache locality
    this.voxels = new Uint8Array(CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z);
    this.solidMesh = null;
    this.transparentMesh = null;
    this.needsRebuild = true;
    this.isDisposed = false;
  }

  getIndex(x, y, z) {
    return x + z * CHUNK_SIZE_X + y * (CHUNK_SIZE_X * CHUNK_SIZE_Z);
  }

  getBlock(x, y, z) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      return this.world.getBlock(this.worldX + x, y, this.worldZ + z);
    }
    return this.voxels[this.getIndex(x, y, z)];
  }

  setBlock(x, y, z, blockId) {
    if (x < 0 || x >= CHUNK_SIZE_X || y < 0 || y >= CHUNK_SIZE_Y || z < 0 || z >= CHUNK_SIZE_Z) {
      return;
    }
    this.voxels[this.getIndex(x, y, z)] = blockId;
    this.needsRebuild = true;
  }

  /**
   * Generates procedural terrain using multi-octave Perlin noise.
   */
  generateTerrain(noise) {
    for (let x = 0; x < CHUNK_SIZE_X; x++) {
      const wx = this.worldX + x;
      for (let z = 0; z < CHUNK_SIZE_Z; z++) {
        const wz = this.worldZ + z;

        // Base height calculation using multi-octave FBM
        const continental = noise.fbm2D(wx * 0.008, wz * 0.008, 4, 0.5, 2.0);
        const detail = noise.fbm2D(wx * 0.04, wz * 0.04, 3, 0.4, 2.0);
        const heightNorm = (continental * 0.7 + detail * 0.3) * 0.5 + 0.5; // [0, 1]

        // Map normalized height to world blocks (between 14 and 48)
        const surfaceHeight = Math.floor(14 + heightNorm * 32);

        // Bedrock layer at bottom
        this.voxels[this.getIndex(x, 0, z)] = BLOCKS.BEDROCK;

        // Populate strata layers
        for (let y = 1; y < CHUNK_SIZE_Y; y++) {
          const idx = this.getIndex(x, y, z);

          if (y < surfaceHeight - 4) {
            // Deep stone layer with procedural ore distribution
            const oreSample = Math.abs(noise.noise3D(wx * 0.12, y * 0.12, wz * 0.12));
            if (y <= 8 && oreSample > 0.78) {
              this.voxels[idx] = BLOCKS.DIAMOND_ORE;
            } else if (y <= 24 && oreSample > 0.72) {
              this.voxels[idx] = BLOCKS.IRON_ORE;
            } else if (y <= 42 && oreSample > 0.68) {
              this.voxels[idx] = BLOCKS.COAL_ORE;
            } else {
              this.voxels[idx] = BLOCKS.STONE;
            }
          } else if (y < surfaceHeight) {
            // Dirt layer beneath surface
            this.voxels[idx] = BLOCKS.DIRT;
          } else if (y === surfaceHeight) {
            // Surface block: Sand near water level, otherwise lush Grass
            if (surfaceHeight <= SEA_LEVEL + 1) {
              this.voxels[idx] = BLOCKS.SAND;
            } else {
              this.voxels[idx] = BLOCKS.GRASS;
            }
          } else if (y <= SEA_LEVEL) {
            // Water filling depressions below sea level
            this.voxels[idx] = BLOCKS.WATER;
          } else {
            // Air above surface
            this.voxels[idx] = BLOCKS.AIR;
          }
        }

        // Procedural Tree Generation on grass
        if (surfaceHeight > SEA_LEVEL + 1 && surfaceHeight < CHUNK_SIZE_Y - 9) {
          // Deterministic tree placement hash
          const treeHash = Math.abs(Math.sin(wx * 12.9898 + wz * 78.233) * 43758.5453) % 1;
          if (treeHash < 0.018 && x >= 2 && x <= 13 && z >= 2 && z <= 13) {
            this.world.queueTree(wx, surfaceHeight + 1, wz);
          }
        }
      }
    }
  }

  /**
   * Computes subtle 2-bit vertex ambient occlusion (0: dark, 3: bright)
   */
  computeVertexAO(side1, side2, corner) {
    if (side1 && side2) {
      return 0; // Completely enclosed corner
    }
    return 3 - (side1 ? 1 : 0) - (side2 ? 1 : 0) - (corner ? 1 : 0);
  }

  /**
   * Generates optimized face-culled Three.js BufferGeometry for this chunk
   */
  buildMesh(textureManager) {
    if (this.isDisposed) return;

    // Solid buffers
    const solidPositions = [];
    const solidNormals = [];
    const solidUVs = [];
    const solidColors = [];
    const solidIndices = [];

    // Transparent buffers (water, glass)
    const transPositions = [];
    const transNormals = [];
    const transUVs = [];
    const transColors = [];
    const transIndices = [];

    let solidVertexCount = 0;
    let transVertexCount = 0;

    for (let y = 0; y < CHUNK_SIZE_Y; y++) {
      for (let z = 0; z < CHUNK_SIZE_Z; z++) {
        for (let x = 0; x < CHUNK_SIZE_X; x++) {
          const blockId = this.voxels[this.getIndex(x, y, z)];
          if (blockId === BLOCKS.AIR) continue;

          const blockInfo = REGISTRY[blockId];
          const isTransparent = blockInfo?.transparent ?? false;

          // Check all 6 faces
          for (let f = 0; f < 6; f++) {
            const face = FACES[f];
            const nx = x + face.dir[0];
            const ny = y + face.dir[1];
            const nz = z + face.dir[2];

            const neighborId = this.getBlock(nx, ny, nz);
            const neighborInfo = REGISTRY[neighborId];
            const neighborTransparent = neighborInfo?.transparent ?? false;

            // Face Culling rule:
            // Render face if neighbor is transparent, unless neighbor is identical transparent block (e.g. water next to water)
            if (neighborId === blockId && isTransparent && blockId === BLOCKS.WATER) {
              continue;
            }
            if (!neighborTransparent && neighborId !== BLOCKS.AIR) {
              continue; // Neighbor is solid opaque -> cull face!
            }

            // Determine texture tile for this face
            const faceConfig = BLOCK_FACES[blockId] || {};
            let tileKey = faceConfig.side || 'dirt';
            if (face.name === 'top') tileKey = faceConfig.top || tileKey;
            else if (face.name === 'bottom') tileKey = faceConfig.bottom || tileKey;
            else if (face.name === 'north' && faceConfig.north) tileKey = faceConfig.north;

            const uvs = textureManager.getTileUVs(tileKey);

            // Target buffers based on transparency
            const positions = isTransparent ? transPositions : solidPositions;
            const normals = isTransparent ? transNormals : solidNormals;
            const uvArr = isTransparent ? transUVs : solidUVs;
            const colors = isTransparent ? transColors : solidColors;
            const indices = isTransparent ? transIndices : solidIndices;
            let vCount = isTransparent ? transVertexCount : solidVertexCount;

            const wx = this.worldX + x;
            const wy = y;
            const wz = this.worldZ + z;

            // Add 4 vertices for this quad
            for (let i = 0; i < 4; i++) {
              const c = face.corners[i];
              positions.push(wx + c[0], wy + c[1], wz + c[2]);
              normals.push(face.normal[0], face.normal[1], face.normal[2]);

              // Shading calculation based on face orientation
              const baseShade = face.shade;
              colors.push(baseShade, baseShade, baseShade);
            }

            // Quad UVs
            uvArr.push(uvs.uMin, uvs.vMin);
            uvArr.push(uvs.uMin, uvs.vMax);
            uvArr.push(uvs.uMax, uvs.vMax);
            uvArr.push(uvs.uMax, uvs.vMin);

            // Two triangles for the quad (indices)
            indices.push(
              vCount + 0, vCount + 1, vCount + 2,
              vCount + 0, vCount + 2, vCount + 3
            );

            if (isTransparent) {
              transVertexCount += 4;
            } else {
              solidVertexCount += 4;
            }
          }
        }
      }
    }

    // Update Solid Mesh
    this.updateMesh(
      'solidMesh',
      solidPositions,
      solidNormals,
      solidUVs,
      solidColors,
      solidIndices,
      this.world.solidMaterial
    );

    // Update Transparent Mesh
    this.updateMesh(
      'transparentMesh',
      transPositions,
      transNormals,
      transUVs,
      transColors,
      transIndices,
      this.world.transparentMaterial
    );

    this.needsRebuild = false;
  }

  updateMesh(propName, positions, normals, uvs, colors, indices, material) {
    if (positions.length === 0) {
      if (this[propName]) {
        this.world.scene.remove(this[propName]);
        this[propName].geometry.dispose();
        this[propName] = null;
      }
      return;
    }

    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geometry.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
    geometry.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2));
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
    geometry.setIndex(indices);
    geometry.computeBoundingSphere(); // Enables fast Three.js frustum culling

    if (this[propName]) {
      this[propName].geometry.dispose();
      this[propName].geometry = geometry;
    } else {
      this[propName] = new THREE.Mesh(geometry, material);
      this[propName].castShadow = false; // Fast unshadowed voxel rendering
      this[propName].receiveShadow = false;
      this[propName].matrixAutoUpdate = false; // Chunk never moves; freeze transformation matrix
      this[propName].updateMatrix();
      this.world.scene.add(this[propName]);
    }
  }

  dispose() {
    this.isDisposed = true;
    if (this.solidMesh) {
      this.world.scene.remove(this.solidMesh);
      this.solidMesh.geometry.dispose();
      this.solidMesh = null;
    }
    if (this.transparentMesh) {
      this.world.scene.remove(this.transparentMesh);
      this.transparentMesh.geometry.dispose();
      this.transparentMesh = null;
    }
  }
}

export class World {
  /**
   * Manages global chunk map, procedural noise, delta edits, and chunk streaming
   */
  constructor(scene, noise, textureManager) {
    this.scene = scene;
    this.noise = noise;
    this.textureManager = textureManager;

    // Chunk dictionary keyed by "cx,cz"
    this.chunks = new Map();

    // Map of persistent player block modifications: "x,y,z" -> blockId
    this.modifiedBlocks = new Map();

    // Tree generation queue
    this.treeQueue = [];

    // Cache last player chunk coordinates to avoid redundant streaming checks
    this.lastChunkX = null;
    this.lastChunkZ = null;

    // Shared Materials
    this.solidMaterial = new THREE.MeshLambertMaterial({
      map: this.textureManager.texture,
      vertexColors: true,
      transparent: false,
      alphaTest: 0.1, // Allows leaf pixel transparency without alpha sorting glitches
    });

    this.transparentMaterial = new THREE.MeshLambertMaterial({
      map: this.textureManager.texture,
      vertexColors: true,
      transparent: true,
      opacity: 0.82,
      depthWrite: false,
    });
  }

  getChunkKey(cx, cz) {
    return `${cx},${cz}`;
  }

  getChunkAtWorldPos(wx, wz) {
    const cx = Math.floor(wx / CHUNK_SIZE_X);
    const cz = Math.floor(wz / CHUNK_SIZE_Z);
    return this.chunks.get(this.getChunkKey(cx, cz));
  }

  getBlock(x, y, z) {
    if (y < 0 || y >= CHUNK_SIZE_Y) return BLOCKS.AIR;

    // Check modified blocks delta first
    const key = `${x},${y},${z}`;
    if (this.modifiedBlocks.has(key)) {
      return this.modifiedBlocks.get(key);
    }

    const chunk = this.getChunkAtWorldPos(x, z);
    if (!chunk) return BLOCKS.AIR;

    const lx = ((x % CHUNK_SIZE_X) + CHUNK_SIZE_X) % CHUNK_SIZE_X;
    const lz = ((z % CHUNK_SIZE_Z) + CHUNK_SIZE_Z) % CHUNK_SIZE_Z;
    return chunk.voxels[chunk.getIndex(lx, y, lz)];
  }

  setBlock(x, y, z, blockId, markModified = true) {
    if (y < 0 || y >= CHUNK_SIZE_Y) return false;

    if (markModified) {
      this.modifiedBlocks.set(`${x},${y},${z}`, blockId);
    }

    const cx = Math.floor(x / CHUNK_SIZE_X);
    const cz = Math.floor(z / CHUNK_SIZE_Z);
    const chunk = this.chunks.get(this.getChunkKey(cx, cz));

    if (chunk) {
      const lx = ((x % CHUNK_SIZE_X) + CHUNK_SIZE_X) % CHUNK_SIZE_X;
      const lz = ((z % CHUNK_SIZE_Z) + CHUNK_SIZE_Z) % CHUNK_SIZE_Z;
      chunk.setBlock(lx, y, lz, blockId);

      // Rebuild chunk mesh
      chunk.buildMesh(this.textureManager);

      // Also rebuild neighbor chunks if block is on chunk boundary!
      if (lx === 0) this.chunks.get(this.getChunkKey(cx - 1, cz))?.buildMesh(this.textureManager);
      if (lx === CHUNK_SIZE_X - 1) this.chunks.get(this.getChunkKey(cx + 1, cz))?.buildMesh(this.textureManager);
      if (lz === 0) this.chunks.get(this.getChunkKey(cx, cz - 1))?.buildMesh(this.textureManager);
      if (lz === CHUNK_SIZE_Z - 1) this.chunks.get(this.getChunkKey(cx, cz + 1))?.buildMesh(this.textureManager);

      return true;
    }

    return false;
  }

  queueTree(x, y, z) {
    this.treeQueue.push({ x, y, z });
  }

  processTreeQueue() {
    while (this.treeQueue.length > 0) {
      const { x, y, z } = this.treeQueue.pop();
      // Generate standard oak tree
      const height = 5;
      // Trunk
      for (let ty = 0; ty < height; ty++) {
        this.setBlock(x, y + ty, z, BLOCKS.OAK_LOG, false);
      }
      // Foliage canopy
      const leafBottom = y + height - 2;
      const leafTop = y + height + 1;
      for (let ly = leafBottom; ly <= leafTop; ly++) {
        const radius = ly === leafTop ? 1 : 2;
        for (let dx = -radius; dx <= radius; dx++) {
          for (let dz = -radius; dz <= radius; dz++) {
            if (Math.abs(dx) === radius && Math.abs(dz) === radius && Math.random() < 0.4) continue;
            const bx = x + dx;
            const bz = z + dz;
            if (this.getBlock(bx, ly, bz) === BLOCKS.AIR) {
              this.setBlock(bx, ly, bz, BLOCKS.OAK_LEAVES, false);
            }
          }
        }
      }
    }
  }

  /**
   * Applies all persistent modified blocks onto a freshly generated chunk
   */
  applyModificationsToChunk(chunk) {
    for (const [key, blockId] of this.modifiedBlocks.entries()) {
      const [x, y, z] = key.split(',').map(Number);
      const cx = Math.floor(x / CHUNK_SIZE_X);
      const cz = Math.floor(z / CHUNK_SIZE_Z);
      if (cx === chunk.cx && cz === chunk.cz) {
        const lx = ((x % CHUNK_SIZE_X) + CHUNK_SIZE_X) % CHUNK_SIZE_X;
        const lz = ((z % CHUNK_SIZE_Z) + CHUNK_SIZE_Z) % CHUNK_SIZE_Z;
        chunk.setBlock(lx, y, lz, blockId);
      }
    }
  }

  /**
   * Infinite world streaming: dynamically loads chunks around player coordinate
   * and unloads distant chunks beyond render distance.
   */
  updateStreaming(playerX, playerZ, force = false) {
    const pcx = Math.floor(playerX / CHUNK_SIZE_X);
    const pcz = Math.floor(playerZ / CHUNK_SIZE_Z);

    if (!force && pcx === this.lastChunkX && pcz === this.lastChunkZ) {
      return; // Skip streaming logic entirely when player stays inside current chunk
    }
    this.lastChunkX = pcx;
    this.lastChunkZ = pcz;

    const activeKeys = new Set();
    const newChunks = [];

    // Load chunks within VIEW_DISTANCE
    for (let dx = -VIEW_DISTANCE; dx <= VIEW_DISTANCE; dx++) {
      for (let dz = -VIEW_DISTANCE; dz <= VIEW_DISTANCE; dz++) {
        if (dx * dx + dz * dz > (VIEW_DISTANCE + 0.5) ** 2) continue;

        const cx = pcx + dx;
        const cz = pcz + dz;
        const key = this.getChunkKey(cx, cz);
        activeKeys.add(key);

        if (!this.chunks.has(key)) {
          const chunk = new Chunk(this, cx, cz);
          chunk.generateTerrain(this.noise);
          this.chunks.set(key, chunk);
          newChunks.push(chunk);
        }
      }
    }

    // Process trees on newly generated chunks
    if (this.treeQueue.length > 0) {
      this.processTreeQueue();
    }

    // Build meshes for new chunks
    for (const chunk of newChunks) {
      this.applyModificationsToChunk(chunk);
      chunk.buildMesh(this.textureManager);
    }

    // Unload distant chunks to preserve VRAM and rendering performance
    for (const [key, chunk] of this.chunks.entries()) {
      if (!activeKeys.has(key)) {
        chunk.dispose();
        this.chunks.delete(key);
      }
    }
  }
}
