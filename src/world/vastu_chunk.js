/**
 * @fileoverview VastuChunk - High-Performance Face-Culled Voxel Chunk Engine
 * @module world/vastu_chunk
 * 
 * Features:
 * - Flat 16x64x16 typed array storage (Uint8Array) for cache locality.
 * - Dynamic 6-way face culling with boundary seam resolution.
 * - Directional sunlight shading baked into vertex colors (Top: 1.0, Bottom: 0.5, Sides: 0.75-0.85).
 * - Consolidated single draw-call BufferGeometry with frozen matrices (`matrixAutoUpdate = false`).
 * - Separate translucent water pass for flowing Sindhu river channels.
 */

import * as THREE from 'three';
import { VastuBlockId, VASTU_REGISTRY } from './ancient_blocks.js';
import { CHUNK_WIDTH, CHUNK_HEIGHT, CHUNK_DEPTH } from './prithvi_generator.js';

// Directional Face Normals, Vertex Offsets, and Shading Constants
// 0: +Y (Top), 1: -Y (Bottom), 2: +X (East), 3: -X (West), 4: +Z (South), 5: -Z (North)
const FACE_DEFINITIONS = [
  {
    name: 'top',
    dir: [0, 1, 0],
    normal: [0, 1, 0],
    shade: 1.0,
    corners: [
      [0, 1, 1], [1, 1, 1], [1, 1, 0],
      [0, 1, 1], [1, 1, 0], [0, 1, 0],
    ],
  },
  {
    name: 'bottom',
    dir: [0, -1, 0],
    normal: [0, -1, 0],
    shade: 0.5,
    corners: [
      [0, 0, 0], [1, 0, 0], [1, 0, 1],
      [0, 0, 0], [1, 0, 1], [0, 0, 1],
    ],
  },
  {
    name: 'east',
    dir: [1, 0, 0],
    normal: [1, 0, 0],
    shade: 0.85,
    corners: [
      [1, 0, 0], [1, 1, 0], [1, 1, 1],
      [1, 0, 0], [1, 1, 1], [1, 0, 1],
    ],
  },
  {
    name: 'west',
    dir: [-1, 0, 0],
    normal: [-1, 0, 0],
    shade: 0.75,
    corners: [
      [0, 0, 1], [0, 1, 1], [0, 1, 0],
      [0, 0, 1], [0, 1, 0], [0, 0, 0],
    ],
  },
  {
    name: 'south',
    dir: [0, 0, 1],
    normal: [0, 0, 1],
    shade: 0.80,
    corners: [
      [1, 0, 1], [1, 1, 1], [0, 1, 1],
      [1, 0, 1], [0, 1, 1], [0, 0, 1],
    ],
  },
  {
    name: 'north',
    dir: [0, 0, -1],
    normal: [0, 0, -1],
    shade: 0.70,
    corners: [
      [0, 0, 0], [0, 1, 0], [1, 1, 0],
      [0, 0, 0], [1, 1, 0], [1, 0, 0],
    ],
  },
];

// Shared high-performance materials
const OPAQUE_VOXEL_MATERIAL = new THREE.MeshLambertMaterial({
  vertexColors: true,
  side: THREE.FrontSide,
});

const WATER_VOXEL_MATERIAL = new THREE.MeshLambertMaterial({
  color: 0x2b6cb0,
  transparent: true,
  opacity: 0.65,
  side: THREE.DoubleSide,
  depthWrite: false,
});

/**
 * Single 16x64x16 Voxel Chunk entity.
 */
export class VastuChunk {
  /**
   * Initializes chunk coordinates and memory buffers.
   * 
   * @param {Object} world - Master VastuWorld reference.
   * @param {number} cx - Chunk coordinate along X.
   * @param {number} cz - Chunk coordinate along Z.
   */
  constructor(world, cx, cz) {
    this.world = world;
    this.cx = cx;
    this.cz = cz;
    this.worldX = cx * CHUNK_WIDTH;
    this.worldZ = cz * CHUNK_DEPTH;

    /**
     * Flat voxel buffer.
     * @type {Uint8Array}
     */
    this.voxels = new Uint8Array(CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_DEPTH);

    /**
     * Consolidated Three.js mesh for opaque voxels.
     * @type {THREE.Mesh|null}
     */
    this.mesh = null;

    /**
     * Translucent water surface mesh.
     * @type {THREE.Mesh|null}
     */
    this.waterMesh = null;

    /**
     * AABB bounding volume for Octree spatial indexing and frustum culling.
     * @type {THREE.Box3}
     */
    this.bounds = new THREE.Box3(
      new THREE.Vector3(this.worldX, 0, this.worldZ),
      new THREE.Vector3(this.worldX + CHUNK_WIDTH, CHUNK_HEIGHT, this.worldZ + CHUNK_DEPTH)
    );

    this.isDirty = true;
    this.isDisposed = false;
  }

  /**
   * Bitwise flat index calculation for voxel coordinates (x, y, z).
   * 
   * @param {number} lx - Local X [0..15].
   * @param {number} ly - Local Y [0..63].
   * @param {number} lz - Local Z [0..15].
   * @returns {number} Flat buffer index.
   */
  getIndex(lx, ly, lz) {
    return (ly * CHUNK_WIDTH * CHUNK_DEPTH) + (lz * CHUNK_WIDTH) + lx;
  }

  /**
   * Gets local voxel ID, or queries world if out of local bounds.
   * 
   * @param {number} lx - Local X.
   * @param {number} ly - Local Y.
   * @param {number} lz - Local Z.
   * @returns {number} VastuBlockId.
   */
  getBlock(lx, ly, lz) {
    if (ly < 0 || ly >= CHUNK_HEIGHT) return VastuBlockId.AIR;

    if (lx >= 0 && lx < CHUNK_WIDTH && lz >= 0 && lz < CHUNK_DEPTH) {
      return this.voxels[this.getIndex(lx, ly, lz)];
    }

    // Boundary query against neighboring chunks
    return this.world.getBlock(this.worldX + lx, ly, this.worldZ + lz);
  }

  /**
   * Sets local voxel ID.
   * 
   * @param {number} lx - Local X [0..15].
   * @param {number} ly - Local Y [0..63].
   * @param {number} lz - Local Z [0..15].
   * @param {number} blockId - VastuBlockId to write.
   */
  setBlock(lx, ly, lz, blockId) {
    if (lx < 0 || lx >= CHUNK_WIDTH || ly < 0 || ly >= CHUNK_HEIGHT || lz < 0 || lz >= CHUNK_DEPTH) {
      return;
    }
    this.voxels[this.getIndex(lx, ly, lz)] = blockId;
    this.isDirty = true;
  }

  /**
   * Toggles visibility for both opaque and water meshes (used by VastuOctree).
   * 
   * @param {boolean} visible - Visibility flag.
   */
  setVisible(visible) {
    if (this.mesh) this.mesh.visible = visible;
    if (this.waterMesh) this.waterMesh.visible = visible;
  }

  /**
   * Generates optimized face-culled BufferGeometry for this chunk.
   */
  buildMesh() {
    if (this.isDisposed) return;

    // Buffer accumulation arrays
    const positions = [];
    const normals = [];
    const colors = [];

    const waterPositions = [];
    const waterNormals = [];

    // Helper: Unpack Hex Color to RGB with shading modulation
    const colorScratch = new THREE.Color();

    for (let ly = 0; ly < CHUNK_HEIGHT; ly++) {
      for (let lz = 0; lz < CHUNK_DEPTH; lz++) {
        for (let lx = 0; lx < CHUNK_WIDTH; lx++) {
          const blockId = this.voxels[this.getIndex(lx, ly, lz)];
          if (blockId === VastuBlockId.AIR) continue;

          const meta = VASTU_REGISTRY[blockId];
          if (!meta) continue;

          const isWater = (blockId === VastuBlockId.SACRED_WATER);

          // Test all 6 cardinal directions
          for (let f = 0; f < 6; f++) {
            const face = FACE_DEFINITIONS[f];
            const nx = lx + face.dir[0];
            const ny = ly + face.dir[1];
            const nz = lz + face.dir[2];

            const neighborId = this.getBlock(nx, ny, nz);
            const neighborMeta = VASTU_REGISTRY[neighborId];

            // Face Culling Logic:
            // 1. Water only renders top faces or faces against non-water air
            if (isWater) {
              if (neighborId === VastuBlockId.AIR) {
                for (let c = 0; c < 6; c++) {
                  const corner = face.corners[c];
                  waterPositions.push(this.worldX + lx + corner[0], ly + corner[1], this.worldZ + lz + corner[2]);
                  waterNormals.push(face.normal[0], face.normal[1], face.normal[2]);
                }
              }
              continue;
            }

            // 2. Solid Blocks: emit face if neighbor is air or transparent
            const emitFace = (neighborId === VastuBlockId.AIR) || (neighborMeta && neighborMeta.transparent && neighborId !== blockId);
            if (!emitFace) continue;

            // Determine face color with directional sunlight shading
            let baseHex = meta.colorHex;
            if (f === 0 && meta.topColorHex !== undefined) {
              baseHex = meta.topColorHex;
            } else if (f === 1 && meta.bottomColorHex !== undefined) {
              baseHex = meta.bottomColorHex;
            }

            colorScratch.setHex(baseHex);
            const r = colorScratch.r * face.shade;
            const g = colorScratch.g * face.shade;
            const b = colorScratch.b * face.shade;

            // Emit 2 triangles (6 vertices)
            for (let c = 0; c < 6; c++) {
              const corner = face.corners[c];
              positions.push(this.worldX + lx + corner[0], ly + corner[1], this.worldZ + lz + corner[2]);
              normals.push(face.normal[0], face.normal[1], face.normal[2]);
              colors.push(r, g, b);
            }
          }
        }
      }
    }

    // 1. Build Opaque Mesh
    if (positions.length > 0) {
      const geom = new THREE.BufferGeometry();
      geom.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
      geom.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
      geom.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
      geom.computeBoundingSphere();

      if (!this.mesh) {
        this.mesh = new THREE.Mesh(geom, OPAQUE_VOXEL_MATERIAL);
        this.mesh.matrixAutoUpdate = false;
        this.mesh.updateMatrix();
        this.world.scene.add(this.mesh);
      } else {
        this.mesh.geometry.dispose();
        this.mesh.geometry = geom;
      }
    } else if (this.mesh) {
      this.world.scene.remove(this.mesh);
      this.mesh.geometry.dispose();
      this.mesh = null;
    }

    // 2. Build Water Mesh
    if (waterPositions.length > 0) {
      const waterGeom = new THREE.BufferGeometry();
      waterGeom.setAttribute('position', new THREE.Float32BufferAttribute(waterPositions, 3));
      waterGeom.setAttribute('normal', new THREE.Float32BufferAttribute(waterNormals, 3));
      waterGeom.computeBoundingSphere();

      if (!this.waterMesh) {
        this.waterMesh = new THREE.Mesh(waterGeom, WATER_VOXEL_MATERIAL);
        this.waterMesh.matrixAutoUpdate = false;
        this.waterMesh.updateMatrix();
        this.world.scene.add(this.waterMesh);
      } else {
        this.waterMesh.geometry.dispose();
        this.waterMesh.geometry = waterGeom;
      }
    } else if (this.waterMesh) {
      this.world.scene.remove(this.waterMesh);
      this.waterMesh.geometry.dispose();
      this.waterMesh = null;
    }

    this.isDirty = false;
  }

  /**
   * Cleanly disposes geometry, materials, and removes meshes from Three.js scene.
   */
  dispose() {
    this.isDisposed = true;
    if (this.mesh) {
      this.world.scene.remove(this.mesh);
      this.mesh.geometry.dispose();
      this.mesh = null;
    }
    if (this.waterMesh) {
      this.world.scene.remove(this.waterMesh);
      this.waterMesh.geometry.dispose();
      this.waterMesh = null;
    }
  }
}
