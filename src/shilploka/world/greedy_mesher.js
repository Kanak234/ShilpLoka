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
   * @param {((wx:number, wy:number, wz:number) => number)|null} [getWorldBlock=null]
   *   Reads a block from the WORLD, used to look across this chunk's X/Z
   *   borders into its neighbours. Pass null to treat the border as air (the
   *   old behaviour, kept for callers that mesh a chunk in isolation).
   * @returns {THREE.BufferGeometry} Optimized greedy-meshed BufferGeometry.
   */
  static meshChunk(voxels, sizeX, sizeY, sizeZ, worldOffsetX = 0, worldOffsetZ = 0, getWorldBlock = null) {
    const dims = [sizeX, sizeY, sizeZ];

    /**
     * Block lookup that can see past this chunk's edges.
     *
     * WHY: the mesher used to treat everything outside the chunk as AIR. At
     * every X/Z border that meant a solid block next to a solid neighbour
     * still got a face -- a wall buried inside the terrain that the GPU drew
     * but the player could never see. Every border of every chunk paid for it.
     *
     * NOW:
     *  - inside the chunk        -> read the local voxel array (fast path)
     *  - outside on Y            -> AIR. Below y=0 and above the chunk top is
     *                               genuinely the edge of the world.
     *  - outside on X or Z       -> ask the world (the neighbouring chunk).
     *                               If that neighbour is not loaded the world
     *                               answers AIR, so the loaded edge still shows
     *                               a face instead of a see-through hole.
     */
    const getBlock = (x, y, z) => {
      if (y < 0 || y >= sizeY) return ShilpBlockId.AIR;
      if (x >= 0 && x < sizeX && z >= 0 && z < sizeZ) {
        return voxels[x + sizeX * (z + sizeZ * y)];
      }
      return getWorldBlock ? getWorldBlock(worldOffsetX + x, y, worldOffsetZ + z) : ShilpBlockId.AIR;
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
            // A is the voxel at this slice, B the next one along axis d. On
            // the first slice (x[d] = -1) A sits in the neighbouring chunk; on
            // the last slice B does. getBlock() handles both.
            const blockA = getBlock(x[0], x[1], x[2]);
            const blockB = getBlock(x[0] + q[0], x[1] + q[1], x[2] + q[2]);

            const solidA = blockA !== ShilpBlockId.AIR && (SHILP_BLOCK_REGISTRY[blockA]?.solid ?? true);
            const solidB = blockB !== ShilpBlockId.AIR && (SHILP_BLOCK_REGISTRY[blockB]?.solid ?? true);

            // FACE OWNERSHIP. Once the mesher can see across borders, the face
            // on a shared border is visible to BOTH chunks, and both would emit
            // it - two identical coplanar quads that z-fight and flicker.
            // Rule: a face is drawn only by the chunk that owns its SOLID voxel.
            //   first slice: if the solid side is A, A is the neighbour's -> skip
            //   last slice:  if the solid side is B, B is the neighbour's -> skip
            // Each border face is then emitted exactly once, by its owner.
            const aIsOutside = x[d] < 0;
            const bIsOutside = x[d] >= dims[d] - 1;
            const faceOwnedByNeighbour = (solidA && !solidB && aIsOutside) ||
                                         (solidB && !solidA && bIsOutside);

            if (solidA === solidB || faceOwnedByNeighbour) {
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
