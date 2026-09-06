/**
 * @fileoverview PrithviGenerator - Vedic Procedural Terrain & Biome Generator
 * @module world/prithvi_generator
 * 
 * Features:
 * - Seedable multi-octave Perlin Fractal Brownian Motion (FBM) for natural landscapes.
 * - Ancient Indian biomes: Sindhu Floodplains, Thar Desert dunes, and Saraswati Riverbeds.
 * - Graph-driven Ancient Trade Network: Paves royal brick highways and caravan trails between cities.
 * - Procedural Ancient Indian Cities:
 *   1. Harappa (Mound of the Granary & Ashoka Pillar) at (0, 0).
 *   2. Mohenjo-Daro (The Great Bath) at (-96, 64).
 *   3. Lothal (Tidal Dockyard) at (-48, -80).
 *   4. Pataliputra (Mauryan 80-Pillared Hall) at (96, -64).
 * - Subterranean mineral veins: Kansa (Bronze Ore) and Badakhshan Lajward (Lapis Lazuli).
 */

import { PerlinNoise } from '../noise.js';
import { VastuBlockId } from './ancient_blocks.js';
import { VastuGraph } from './vastu_graph.js';

export const CHUNK_WIDTH = 16;
export const CHUNK_HEIGHT = 64;
export const CHUNK_DEPTH = 16;
export const SEA_LEVEL = 16;

/**
 * Biome enumeration for the Ancient Indian subcontinent.
 * @readonly
 * @enum {number}
 */
export const AncientBiome = {
  SINDHU_PLAINS: 0,   // Alluvial loam, fertile greens, teak trees
  THAR_DESERT: 1,     // Rolling sand dunes, dry riverbeds, sandstone
  HARAPPA_CITADEL: 2, // Terracotta clay, baked brick outposts, bronze veins
};

/**
 * Procedural world generator constructing ancient voxel terrain.
 */
export class PrithviGenerator {
  /**
   * Initializes noise samplers, world seed, and ancient trade route graph.
   * 
   * @param {number} [seed=108] - Sacred seed for deterministic world generation.
   */
  constructor(seed = 108) {
    this.seed = seed;
    this.elevationNoise = new PerlinNoise(seed);
    this.roughnessNoise = new PerlinNoise(seed + 101);
    this.biomeNoise = new PerlinNoise(seed + 202);
    this.riverNoise = new PerlinNoise(seed + 303);
    this.oreNoise = new PerlinNoise(seed + 404);

    // Ancient Cities & Trade Route Graph
    this.graph = new VastuGraph();
  }

  /**
   * Evaluates the biome at world coordinates (x, z).
   * 
   * @param {number} x - World X coordinate.
   * @param {number} z - World Z coordinate.
   * @returns {number} Biome identifier.
   */
  getBiomeAt(x, z) {
    const b = this.biomeNoise.noise2D(x * 0.005, z * 0.005);
    if (b < -0.2) return AncientBiome.THAR_DESERT;
    if (b > 0.25) return AncientBiome.HARAPPA_CITADEL;
    return AncientBiome.SINDHU_PLAINS;
  }

  /**
   * Computes terrain elevation at world coordinates (x, z).
   * 
   * @param {number} x - World X coordinate.
   * @param {number} z - World Z coordinate.
   * @returns {number} Height in voxel units (integer between 10 and 45).
   */
  getHeightAt(x, z) {
    // 1. Continental Elevation FBM
    const baseElev = this.elevationNoise.fbm2D(x * 0.008, z * 0.008, 4, 0.5, 2.0);
    // 2. Micro-surface roughness
    const rough = this.roughnessNoise.fbm2D(x * 0.03, z * 0.03, 2, 0.4, 2.0);

    let height = 22 + baseElev * 10 + rough * 3;

    // 3. Ancient River Channels (Saraswati & Sindhu waterways)
    const riverVal = Math.abs(this.riverNoise.noise2D(x * 0.006, z * 0.006));
    if (riverVal < 0.12) {
      // Carve river channel down below sea level
      const depthFactor = (1.0 - (riverVal / 0.12));
      height -= depthFactor * 10;
    }

    return Math.max(3, Math.min(CHUNK_HEIGHT - 4, Math.floor(height)));
  }

  /**
   * Generates a complete 3D voxel array for a single 16x64x16 chunk.
   * 
   * @param {number} chunkX - Chunk coordinate along X.
   * @param {number} chunkZ - Chunk coordinate along Z.
   * @returns {Uint8Array} Flat voxel buffer of size CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_DEPTH.
   */
  generateChunkVoxels(chunkX, chunkZ) {
    const voxels = new Uint8Array(CHUNK_WIDTH * CHUNK_HEIGHT * CHUNK_DEPTH);
    const startX = chunkX * CHUNK_WIDTH;
    const startZ = chunkZ * CHUNK_DEPTH;

    // Flat index helper
    const getIndex = (lx, ly, lz) => (ly * CHUNK_WIDTH * CHUNK_DEPTH) + (lz * CHUNK_WIDTH) + lx;

    for (let lx = 0; lx < CHUNK_WIDTH; lx++) {
      const wx = startX + lx;
      for (let lz = 0; lz < CHUNK_DEPTH; lz++) {
        const wz = startZ + lz;

        const surfaceY = this.getHeightAt(wx, wz);
        const biome = this.getBiomeAt(wx, wz);
        const roadInfo = this.graph.getRoadAt(wx, wz);

        // Bedrock floor (Patal-Shila)
        voxels[getIndex(lx, 0, lz)] = VastuBlockId.PATAL_BEDROCK;
        voxels[getIndex(lx, 1, lz)] = VastuBlockId.PATAL_BEDROCK;

        // Subterranean and surface layers
        for (let ly = 2; ly <= surfaceY; ly++) {
          const depthBelowSurface = surfaceY - ly;
          let blockId = VastuBlockId.CHUNAR_SANDSTONE;

          if (depthBelowSurface === 0) {
            // Surface Topsoil or Graph Trade Route
            if (roadInfo && roadInfo.isRoad) {
              blockId = roadInfo.blockId; // Pave ancient highway
            } else if (surfaceY < SEA_LEVEL + 1) {
              blockId = VastuBlockId.RIVER_SAND;
            } else if (biome === AncientBiome.THAR_DESERT) {
              blockId = VastuBlockId.RIVER_SAND;
            } else if (biome === AncientBiome.HARAPPA_CITADEL) {
              blockId = VastuBlockId.TERRACOTTA;
            } else {
              blockId = VastuBlockId.ALLUVIAL_LOAM;
            }
          } else if (depthBelowSurface <= 3) {
            // Sub-surface strata
            if (biome === AncientBiome.HARAPPA_CITADEL) {
              blockId = VastuBlockId.HARAPPAN_BRICK;
            } else if (biome === AncientBiome.THAR_DESERT) {
              blockId = VastuBlockId.CHUNAR_SANDSTONE;
            } else {
              blockId = VastuBlockId.TERRACOTTA;
            }
          } else {
            // Deep stone strata with mineral deposits
            const oreVal = this.oreNoise.noise3D(wx * 0.1, ly * 0.1, wz * 0.1);
            if (oreVal > 0.65 && ly < 20) {
              blockId = VastuBlockId.BRONZE_ORE; // Kansa ore
            } else if (oreVal < -0.7 && ly < 16) {
              blockId = VastuBlockId.LAPIS_LAZULI; // Lajward vein
            } else {
              blockId = VastuBlockId.CHUNAR_SANDSTONE;
            }
          }

          voxels[getIndex(lx, ly, lz)] = blockId;
        }

        // River Basin: Fill below sea level with sacred river water
        if (surfaceY < SEA_LEVEL) {
          for (let wy = surfaceY + 1; wy <= SEA_LEVEL; wy++) {
            voxels[getIndex(lx, wy, lz)] = VastuBlockId.SACRED_WATER;
          }
        }

        // Procedural Sacred Teak Trees (in lush Sindhu Plains, not on roads)
        if (!roadInfo && biome === AncientBiome.SINDHU_PLAINS && surfaceY >= SEA_LEVEL + 1) {
          const treeHash = (wx * 73856093 ^ wz * 19349663) >>> 0;
          if (treeHash % 100 === 0 && lx >= 2 && lx <= CHUNK_WIDTH - 3 && lz >= 2 && lz <= CHUNK_DEPTH - 3) {
            const trunkHeight = 4 + (treeHash % 3);
            for (let ty = 1; ty <= trunkHeight; ty++) {
              if (surfaceY + ty < CHUNK_HEIGHT) {
                voxels[getIndex(lx, surfaceY + ty, lz)] = VastuBlockId.SAAGWAN_LOG;
              }
            }
            const topY = surfaceY + trunkHeight;
            for (let dx = -2; dx <= 2; dx++) {
              for (let dz = -2; dz <= 2; dz++) {
                for (let dy = 0; dy <= 2; dy++) {
                  const foliageY = topY + dy;
                  if (foliageY < CHUNK_HEIGHT && (Math.abs(dx) + Math.abs(dz) + dy <= 4)) {
                    const leafIdx = getIndex(lx + dx, foliageY, lz + dz);
                    if (voxels[leafIdx] === VastuBlockId.AIR) {
                      voxels[leafIdx] = VastuBlockId.SAAGWAN_LEAVES;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Embed Ancient Indian Cities & Heritage Monuments at specific graph nodes
    this.generateAncientCityMonuments(chunkX, chunkZ, voxels);

    return voxels;
  }

  /**
   * Generates city-specific monuments for the four ancient civilization nodes.
   * 
   * @param {number} cx - Chunk X.
   * @param {number} cz - Chunk Z.
   * @param {Uint8Array} voxels - Chunk voxel buffer.
   */
  generateAncientCityMonuments(cx, cz, voxels) {
    const getIndex = (lx, ly, lz) => (ly * CHUNK_WIDTH * CHUNK_DEPTH) + (lz * CHUNK_WIDTH) + lx;

    // 1. Harappa: Great Granary & Ashoka Sthambha Pillar at Chunk (0, 0)
    if (cx === 0 && cz === 0) {
      const baseY = 22;
      for (let x = 4; x <= 11; x++) {
        for (let z = 4; z <= 11; z++) {
          voxels[getIndex(x, baseY, z)] = VastuBlockId.HARAPPA_MONUMENT;
          voxels[getIndex(x, baseY + 1, z)] = VastuBlockId.HARAPPAN_BRICK;
        }
      }
      for (let y = baseY + 2; y <= baseY + 9; y++) {
        voxels[getIndex(7, y, 7)] = VastuBlockId.ASHOKA_MONUMENT;
        voxels[getIndex(8, y, 7)] = VastuBlockId.ASHOKA_MONUMENT;
        voxels[getIndex(7, y, 8)] = VastuBlockId.ASHOKA_MONUMENT;
        voxels[getIndex(8, y, 8)] = VastuBlockId.ASHOKA_MONUMENT;
      }
    }

    // 2. Mohenjo-Daro: The Great Bath at Chunk (-6, 4) -> (-96, 64)
    if (cx === -6 && cz === 4) {
      const baseY = 20;
      // Surrounding courtyard & bitumen waterproof brick pool
      for (let x = 2; x <= 13; x++) {
        for (let z = 2; z <= 13; z++) {
          const isEdge = (x === 2 || x === 13 || z === 2 || z === 13);
          if (isEdge) {
            voxels[getIndex(x, baseY, z)] = VastuBlockId.HARAPPA_MONUMENT;
            voxels[getIndex(x, baseY + 1, z)] = VastuBlockId.HARAPPAN_BRICK;
          } else {
            // Sunken Great Bath pool filled with water
            voxels[getIndex(x, baseY - 1, z)] = VastuBlockId.GREAT_BATH_BITUMEN;
            voxels[getIndex(x, baseY, z)] = VastuBlockId.SACRED_WATER;
          }
        }
      }
    }

    // 3. Lothal: Tidal Dockyard at Chunk (-3, -5) -> (-48, -80)
    if (cx === -3 && cz === -5) {
      const baseY = 16;
      for (let x = 3; x <= 12; x++) {
        for (let z = 3; z <= 12; z++) {
          if (x === 3 || x === 12 || z === 3 || z === 12) {
            voxels[getIndex(x, baseY, z)] = VastuBlockId.HARAPPA_MONUMENT;
            voxels[getIndex(x, baseY + 1, z)] = VastuBlockId.HARAPPAN_BRICK;
          } else {
            voxels[getIndex(x, baseY, z)] = VastuBlockId.SACRED_WATER;
          }
        }
      }
    }

    // 4. Pataliputra: Mauryan Eighty-Pillared Hall at Chunk (6, -4) -> (96, -64)
    if (cx === 6 && cz === -4) {
      const baseY = 24;
      for (let x = 2; x <= 13; x++) {
        for (let z = 2; z <= 13; z++) {
          voxels[getIndex(x, baseY, z)] = VastuBlockId.CHUNAR_SANDSTONE;
          // Pillared columns every 3 blocks
          if (x % 3 === 0 && z % 3 === 0) {
            for (let py = 1; py <= 6; py++) {
              voxels[getIndex(x, baseY + py, z)] = VastuBlockId.ASHOKA_MONUMENT;
            }
          }
        }
      }
    }
  }
}
