/**
 * @fileoverview ShilpGreedyMesher - Production Greedy Meshing Algorithm for Voxel Chunk Buffers
 * @module shilploka/world/greedy_mesher
 * 
 * Implements the Mikola Lysenko greedy meshing algorithm:
 * Consolidates coplanar adjacent identical voxel faces into maximal rectangular quads.
 * Reduces raw voxel triangle counts by 75%–90%, minimizing GPU vertex fetch bottlenecks
 * and maintaining locked 60+ FPS even on low-end hardware.
 */

import * as THREE from 'three';
import { ShilpBlockId, SHILP_BLOCK_REGISTRY } from './voxel_constants.js';

export class ShilpGreedyMesher {
  /**
   * Generates an optimized Three.js BufferGeometry from a 3D voxel array.
   * 
   * @param {Uint8Array|Uint16Array} voxels - 1D array of chunk voxel IDs.
   * @param {number} sizeX - Chunk dimension along X (typically 16).
   * @param {number} sizeY - Chunk dimension along Y (typically 64).
   * @param {number} sizeZ - Chunk dimension along Z (typically 16).
   * @param {number} [worldOffsetX=0] - World space X position of chunk.
   * @param {number} [worldOffsetZ=0] - World space Z position of chunk.
   * @returns {THREE.BufferGeometry} Optimized greedy-meshed BufferGeometry.
   */
  static meshChunk(voxels, sizeX, sizeY, sizeZ, worldOffsetX = 0, worldOffsetZ = 0) {
    const dims = [sizeX, sizeY, sizeZ];

    // Helper: 1D index lookup
    const getBlock = (x, y, z) => {
      if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
        return ShilpBlockId.AIR;
      }
      return voxels[x + sizeX * (z + sizeZ * y)];
    };

    const positions = [];
    const normals = [];
    const colors = [];
    const indices = [];
    let vertexCount = 0;

    // Temporary color helper for unpacking hex values
    const tempColor = new THREE.Color();

    // Directional sunlight modulation factors for realistic Vastu shading
    // Axis 0 (X): 0.75, Axis 1 (Y): 1.0 (top) / 0.5 (bottom), Axis 2 (Z): 0.8
    const getShadingFactor = (axis, direction) => {
      if (axis === 1) return direction > 0 ? 1.0 : 0.5; // Top receives full light, bottom half
      if (axis === 0) return 0.75;
      return 0.82; // Z-axis
    };

    // Sweep across all 3 primary coordinate axes (d = 0: X, 1: Y, 2: Z)
    for (let d = 0; d < 3; d++) {
      const u = (d + 1) % 3; // First perpendicular 2D slice coordinate
      const v = (d + 2) % 3; // Second perpendicular 2D slice coordinate

      const x = [0, 0, 0];
      const q = [0, 0, 0];
      q[d] = 1;

      // 2D slice mask stores: [blockId, direction (+1 or -1)]
      const maskSize = dims[u] * dims[v];
      const maskBlock = new Int32Array(maskSize);
      const maskDir = new Int8Array(maskSize);

      // Sweep slice coordinate along dimension d from -1 to dims[d]
      for (x[d] = -1; x[d] < dims[d]; ) {
        let n = 0;

        // Compute exposed face mask for the current slice plane
        for (x[v] = 0; x[v] < dims[v]; x[v]++) {
          for (x[u] = 0; x[u] < dims[u]; x[u]++) {
            const blockA = (x[d] >= 0) ? getBlock(x[0], x[1], x[2]) : ShilpBlockId.AIR;
            const blockB = (x[d] < dims[d] - 1) ? getBlock(x[0] + q[0], x[1] + q[1], x[2] + q[2]) : ShilpBlockId.AIR;

            const solidA = blockA !== ShilpBlockId.AIR && (SHILP_BLOCK_REGISTRY[blockA]?.solid ?? true);
            const solidB = blockB !== ShilpBlockId.AIR && (SHILP_BLOCK_REGISTRY[blockB]?.solid ?? true);

            if (solidA === solidB) {
              maskBlock[n] = 0;
              maskDir[n] = 0;
            } else if (solidA) {
              maskBlock[n] = blockA;
              maskDir[n] = 1; // Facing in positive direction (+d)
            } else {
              maskBlock[n] = blockB;
              maskDir[n] = -1; // Facing in negative direction (-d)
            }
            n++;
          }
        }

        x[d]++;
        n = 0;

        // Generate greedy rectangular quads from the 2D mask
        for (let j = 0; j < dims[v]; j++) {
          for (let i = 0; i < dims[u]; ) {
            const currentBlock = maskBlock[n];
            const currentDir = maskDir[n];

            if (currentBlock !== 0) {
              // 1. Compute quad width along dimension u
              let width = 1;
              while (
                i + width < dims[u] &&
                maskBlock[n + width] === currentBlock &&
                maskDir[n + width] === currentDir
              ) {
                width++;
              }

              // 2. Compute quad height along dimension v
              let height = 1;
              let canExpand = true;
              while (j + height < dims[v]) {
                for (let k = 0; k < width; k++) {
                  const checkIndex = n + k + height * dims[u];
                  if (
                    maskBlock[checkIndex] !== currentBlock ||
                    maskDir[checkIndex] !== currentDir
                  ) {
                    canExpand = false;
                    break;
                  }
                }
                if (!canExpand) break;
                height++;
              }

              // 3. Construct the merged quad vertices in 3D space
              x[u] = i;
              x[v] = j;

              const du = [0, 0, 0];
              du[u] = width;

              const dv = [0, 0, 0];
              dv[v] = height;

              // Four quad corners
              const v0 = [worldOffsetX + x[0], x[1], worldOffsetZ + x[2]];
              const v1 = [worldOffsetX + x[0] + du[0], x[1] + du[1], worldOffsetZ + x[2] + du[2]];
              const v2 = [worldOffsetX + x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], worldOffsetZ + x[2] + du[2] + dv[2]];
              const v3 = [worldOffsetX + x[0] + dv[0], x[1] + dv[1], worldOffsetZ + x[2] + dv[2]];

              // Normal vector
              const norm = [0, 0, 0];
              norm[d] = currentDir;

              // Face color with directional sunlight shading
              const meta = SHILP_BLOCK_REGISTRY[currentBlock] || SHILP_BLOCK_REGISTRY[ShilpBlockId.SINDHU_ALLUVIAL_SOIL];
              let hexColor = meta.colorHex;
              if (d === 1 && currentDir > 0 && meta.topColorHex !== undefined) {
                hexColor = meta.topColorHex;
              }

              tempColor.setHex(hexColor);
              const shade = getShadingFactor(d, currentDir);
              const cr = tempColor.r * shade;
              const cg = tempColor.g * shade;
              const cb = tempColor.b * shade;

              // Emit quad vertices (ordered counter-clockwise based on normal direction)
              if (currentDir > 0) {
                // v0 -> v1 -> v2 -> v3
                positions.push(...v0, ...v1, ...v2, ...v3);
              } else {
                // v0 -> v3 -> v2 -> v1
                positions.push(...v0, ...v3, ...v2, ...v1);
              }

              for (let k = 0; k < 4; k++) {
                normals.push(...norm);
                colors.push(cr, cg, cb);
              }

              // Two triangles per merged quad
              indices.push(
                vertexCount, vertexCount + 1, vertexCount + 2,
                vertexCount, vertexCount + 2, vertexCount + 3
              );
              vertexCount += 4;

              // 4. Zero out consumed cells from mask
              for (let l = 0; l < height; l++) {
                for (let k = 0; k < width; k++) {
                  maskBlock[n + k + l * dims[u]] = 0;
                  maskDir[n + k + l * dims[u]] = 0;
                }
              }

              i += width;
              n += width;
            } else {
              i++;
              n++;
            }
          }
        }
      }
    }

    // Assemble Three.js BufferGeometry
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geometry.setAttribute('normal', new THREE.Float32BufferAttribute(normals, 3));
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));
    geometry.setIndex(indices);

    geometry.computeBoundingBox();
    geometry.computeBoundingSphere();

    return geometry;
  }
}
