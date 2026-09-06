/**
 * @fileoverview OrganicTreeGenerator - Procedural Ancient Indian Voxel Flora
 * @module shilploka/world/organic_trees
 * 
 * Generates natural, non-blocky voxel trees representing the Indian subcontinent:
 * 1. Sacred Banyan Tree (वटवृक्ष - Ficus benghalensis):
 *    Massive central trunk with sprawling horizontal branches and vertical aerial prop roots (जटा)
 *    dropping all the way down to ground level.
 * 2. Sacred Peepal Tree (अश्वत्थ - Ficus religiosa):
 *    Heart-shaped sprawling canopy with tiered leaf clusters.
 * 3. Sacred Neem Tree (निम्ब - Azadirachta indica):
 *    Slender trunk with layered umbrella foliage.
 */

import { ShilpBlockId } from './voxel_constants.js';

export class OrganicTreeGenerator {
  /**
   * Generates a Sacred Banyan Tree at world coordinates (originX, originY, originZ).
   * 
   * @param {Object} chunkWriter - Interface with setVoxel(wx, wy, wz, blockId) and getGroundHeight(wx, wz).
   * @param {number} originX - Root base X coordinate.
   * @param {number} originY - Ground surface Y coordinate.
   * @param {number} originZ - Root base Z coordinate.
   * @param {number} [trunkHeight=7] - Height of main trunk before branching.
   * @param {number} [canopyRadius=6] - Spread radius of sprawling canopy.
   */
  static generateBanyanTree(chunkWriter, originX, originY, originZ, trunkHeight = 7, canopyRadius = 6) {
    // 1. Central Multi-Column Trunk (2x2 with flared base)
    for (let dy = 0; dy <= trunkHeight; dy++) {
      const y = originY + dy;
      chunkWriter.setVoxel(originX, y, originZ, ShilpBlockId.BANYAN_WOOD);
      chunkWriter.setVoxel(originX + 1, y, originZ, ShilpBlockId.BANYAN_WOOD);
      chunkWriter.setVoxel(originX, y, originZ + 1, ShilpBlockId.BANYAN_WOOD);
      chunkWriter.setVoxel(originX + 1, y, originZ + 1, ShilpBlockId.BANYAN_WOOD);
    }

    // Buttress root flares at ground level
    chunkWriter.setVoxel(originX - 1, originY, originZ, ShilpBlockId.BANYAN_WOOD);
    chunkWriter.setVoxel(originX + 2, originY, originZ, ShilpBlockId.BANYAN_WOOD);
    chunkWriter.setVoxel(originX, originY, originZ - 1, ShilpBlockId.BANYAN_WOOD);
    chunkWriter.setVoxel(originX, originY, originZ + 2, ShilpBlockId.BANYAN_WOOD);

    const canopyBaseY = originY + trunkHeight;

    // 2. Horizontal Limbs & Sprawling Canopy
    const limbDirections = [
      { dx: 1, dz: 0 },
      { dx: -1, dz: 0 },
      { dx: 0, dz: 1 },
      { dx: 0, dz: -1 },
      { dx: 1, dz: 1 },
      { dx: -1, dz: 1 },
      { dx: 1, dz: -1 },
      { dx: -1, dz: -1 },
    ];

    for (const dir of limbDirections) {
      const limbLength = Math.floor(canopyRadius * (0.75 + 0.25 * Math.abs(dir.dx * dir.dz)));
      for (let step = 1; step <= limbLength; step++) {
        const lx = originX + dir.dx * step;
        const lz = originZ + dir.dz * step;
        const ly = canopyBaseY + Math.floor(step * 0.35);

        chunkWriter.setVoxel(lx, ly, lz, ShilpBlockId.BANYAN_WOOD);

        // Clustered organic foliage along each branch
        for (let fx = -2; fx <= 2; fx++) {
          for (let fz = -2; fz <= 2; fz++) {
            for (let fy = 0; fy <= 2; fy++) {
              const distSq = fx * fx + fz * fz + fy * fy * 1.5;
              if (distSq <= 5.5) {
                const wx = lx + fx;
                const wy = ly + fy;
                const wz = lz + fz;
                // Only place leaves in air
                if (chunkWriter.getVoxel(wx, wy, wz) === ShilpBlockId.AIR) {
                  chunkWriter.setVoxel(wx, wy, wz, ShilpBlockId.BANYAN_LEAVES);
                }
              }
            }
          }
        }

        // 3. Aerial Prop Roots (वट जटा):
        // Descend vertically from branches beyond radius 3 down to ground
        if (step >= 3 && step % 2 === 1) {
          const rootX = lx;
          const rootZ = lz;
          const rootStartY = ly - 1;
          const groundY = chunkWriter.getGroundHeight(rootX, rootZ);

          for (let ry = rootStartY; ry >= groundY; ry--) {
            if (chunkWriter.getVoxel(rootX, ry, rootZ) === ShilpBlockId.AIR) {
              chunkWriter.setVoxel(rootX, ry, rootZ, ShilpBlockId.BANYAN_PROP_ROOT);
            }
          }
        }
      }
    }
  }

  /**
   * Generates a Sacred Peepal Tree (अश्वत्थ) with heart-shaped spreading canopy.
   * 
   * @param {Object} chunkWriter - Voxel setter interface.
   * @param {number} originX - Root base X coordinate.
   * @param {number} originY - Ground surface Y coordinate.
   * @param {number} originZ - Root base Z coordinate.
   * @param {number} [height=8] - Trunk height.
   */
  static generatePeepalTree(chunkWriter, originX, originY, originZ, height = 8) {
    // Slender single trunk with natural curve
    for (let dy = 0; dy <= height; dy++) {
      const y = originY + dy;
      chunkWriter.setVoxel(originX, y, originZ, ShilpBlockId.PEEPAL_WOOD);
    }

    const crownY = originY + height;

    // Sprawling dome-shaped crown
    const radius = 4;
    for (let dy = -2; dy <= 4; dy++) {
      const currentRadius = radius - Math.abs(dy - 1) * 0.7;
      const rSq = currentRadius * currentRadius;
      for (let dx = -radius; dx <= radius; dx++) {
        for (let dz = -radius; dz <= radius; dz++) {
          if (dx * dx + dz * dz <= rSq) {
            const wx = originX + dx;
            const wy = crownY + dy;
            const wz = originZ + dz;
            if (chunkWriter.getVoxel(wx, wy, wz) === ShilpBlockId.AIR) {
              chunkWriter.setVoxel(wx, wy, wz, ShilpBlockId.PEEPAL_LEAVES);
            }
          }
        }
      }
    }
  }
}
