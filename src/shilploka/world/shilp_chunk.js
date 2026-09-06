/**
 * @fileoverview ShilpChunk - 3D Voxel Storage & Greedy Meshed Mesh Component
 * @module shilploka/world/shilp_chunk
 */

import * as THREE from 'three';
import { ShilpBlockId } from './voxel_constants.js';
import { ShilpGreedyMesher } from './greedy_mesher.js';

export const CHUNK_SIZE_X = 16;
export const CHUNK_SIZE_Y = 64;
export const CHUNK_SIZE_Z = 16;

export class ShilpChunk {
  /**
   * @param {number} chunkX - Chunk grid coordinate X.
   * @param {number} chunkZ - Chunk grid coordinate Z.
   */
  constructor(chunkX, chunkZ) {
    this.chunkX = chunkX;
    this.chunkZ = chunkZ;

    this.worldX = chunkX * CHUNK_SIZE_X;
    this.worldZ = chunkZ * CHUNK_SIZE_Z;

    // Voxel storage: 16 * 64 * 16 = 16,384 bytes
    this.voxels = new Uint8Array(CHUNK_SIZE_X * CHUNK_SIZE_Y * CHUNK_SIZE_Z);

    this.mesh = null;
    this.isDirty = true;
    this.isDisposed = false;

    // Spatial bounding box for Octree queries
    this.aabb = new THREE.Box3(
      new THREE.Vector3(this.worldX, 0, this.worldZ),
      new THREE.Vector3(this.worldX + CHUNK_SIZE_X, CHUNK_SIZE_Y, this.worldZ + CHUNK_SIZE_Z)
    );
  }

  /**
   * Translates 3D local coordinates to 1D array index.
   */
  getIndex(lx, ly, lz) {
    return lx + CHUNK_SIZE_X * (lz + CHUNK_SIZE_Z * ly);
  }

  /**
   * Gets block at local coordinates.
   */
  getBlock(lx, ly, lz) {
    if (lx < 0 || lx >= CHUNK_SIZE_X || ly < 0 || ly >= CHUNK_SIZE_Y || lz < 0 || lz >= CHUNK_SIZE_Z) {
      return ShilpBlockId.AIR;
    }
    return this.voxels[this.getIndex(lx, ly, lz)];
  }

  /**
   * Sets block at local coordinates and marks chunk dirty for rebuild.
   */
  setBlock(lx, ly, lz, blockId) {
    if (lx < 0 || lx >= CHUNK_SIZE_X || ly < 0 || ly >= CHUNK_SIZE_Y || lz < 0 || lz >= CHUNK_SIZE_Z) {
      return;
    }
    this.voxels[this.getIndex(lx, ly, lz)] = blockId;
    this.isDirty = true;
  }

  /**
   * Builds greedy-meshed Three.js Mesh.
   * 
   * @param {THREE.Material} sharedMaterial - Vertex-colored MeshLambertMaterial.
   * @returns {THREE.Mesh|null}
   */
  buildMesh(sharedMaterial) {
    if (this.isDisposed) return null;

    // Dispose old geometry if rebuilding
    if (this.mesh) {
      if (this.mesh.geometry) this.mesh.geometry.dispose();
      this.mesh.geometry = null;
    }

    const geometry = ShilpGreedyMesher.meshChunk(
      this.voxels,
      CHUNK_SIZE_X,
      CHUNK_SIZE_Y,
      CHUNK_SIZE_Z,
      this.worldX,
      this.worldZ
    );

    if (geometry.attributes.position.count === 0) {
      geometry.dispose();
      if (this.mesh) this.mesh.visible = false;
      this.isDirty = false;
      return null;
    }

    if (!this.mesh) {
      this.mesh = new THREE.Mesh(geometry, sharedMaterial);
      this.mesh.matrixAutoUpdate = false;
      this.mesh.updateMatrix();
    } else {
      this.mesh.geometry = geometry;
      this.mesh.visible = true;
    }

    this.isDirty = false;
    return this.mesh;
  }

  /**
   * Cleanly disposes geometry and frees memory.
   */
  dispose() {
    this.isDisposed = true;
    if (this.mesh) {
      if (this.mesh.geometry) this.mesh.geometry.dispose();
      if (this.mesh.parent) this.mesh.parent.remove(this.mesh);
      this.mesh = null;
    }
  }
}
