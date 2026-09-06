/**
 * @fileoverview VastuGrid - Ancient Indian Spatial Mathematics & Coordinate Architecture
 * @module core/vastu_grid
 * 
 * Implements the spatial geometry system of Nirmana, anchored in classical Vastu
 * Shastra grid archetypes (Peetha, Manduka, Paramasayika grids).
 * 
 * Provides:
 * - Cardinal & Intercardinal Vastu Orientations (Purva, Pashchima, Uttara, Dakshina).
 * - High-speed bitwise coordinate transformations (World Space <-> Chunk Space <-> Voxel Space).
 * - Spatial bounding, distance metrics, and hash key generation.
 */

/**
 * Cardinal directions of Vastu Shastra mapped to 3D Cartesian vectors.
 * @readonly
 * @enum {Object}
 */
export const VastuDirection = {
  PURVA: { name: 'Purva (East)', vector: [1, 0, 0] },
  PASHCHIMA: { name: 'Pashchima (West)', vector: [-1, 0, 0] },
  UTTARA: { name: 'Uttara (North)', vector: [0, 0, -1] },
  DAKSHINA: { name: 'Dakshina (South)', vector: [0, 0, 1] },
  URDHVA: { name: 'Urdhva (Up / Zenith)', vector: [0, 1, 0] },
  ADHAH: { name: 'Adhah (Down / Nadir)', vector: [0, -1, 0] },
};

/**
 * Global Grid Dimensions
 */
export const VASTU_CHUNK_SIZE_X = 16;
export const VASTU_CHUNK_SIZE_Y = 64;
export const VASTU_CHUNK_SIZE_Z = 16;
export const VASTU_TOTAL_VOXELS_PER_CHUNK = VASTU_CHUNK_SIZE_X * VASTU_CHUNK_SIZE_Y * VASTU_CHUNK_SIZE_Z;

/**
 * Spatial coordinate transformations and spatial hashing routines.
 */
export class VastuGrid {
  /**
   * Converts world space coordinates to discrete chunk column coordinates.
   * 
   * @param {number} worldX - Global X position.
   * @param {number} worldZ - Global Z position.
   * @returns {{cx: number, cz: number}} Chunk grid coordinates.
   */
  static world_to_chunk(worldX, worldZ) {
    return {
      cx: Math.floor(worldX / VASTU_CHUNK_SIZE_X),
      cz: Math.floor(worldZ / VASTU_CHUNK_SIZE_Z),
    };
  }

  /**
   * Converts world space coordinates to local chunk voxel coordinates (0..15, 0..63, 0..15).
   * 
   * @param {number} worldX - Global X position.
   * @param {number} worldY - Global Y position.
   * @param {number} worldZ - Global Z position.
   * @returns {{lx: number, ly: number, lz: number}} Local voxel coordinates within chunk.
   */
  static world_to_local_voxel(worldX, worldY, worldZ) {
    const lx = ((Math.floor(worldX) % VASTU_CHUNK_SIZE_X) + VASTU_CHUNK_SIZE_X) % VASTU_CHUNK_SIZE_X;
    const ly = Math.max(0, Math.min(VASTU_CHUNK_SIZE_Y - 1, Math.floor(worldY)));
    const lz = ((Math.floor(worldZ) % VASTU_CHUNK_SIZE_Z) + VASTU_CHUNK_SIZE_Z) % VASTU_CHUNK_SIZE_Z;
    return { lx, ly, lz };
  }

  /**
   * Computes the 1D flat array index for a local voxel coordinate inside a chunk.
   * Format: X + Z * 16 + Y * 256.
   * 
   * @param {number} lx - Local X (0..15).
   * @param {number} ly - Local Y (0..63).
   * @param {number} lz - Local Z (0..15).
   * @returns {number} Flat buffer offset index.
   */
  static get_voxel_index(lx, ly, lz) {
    return lx + (lz * VASTU_CHUNK_SIZE_X) + (ly * VASTU_CHUNK_SIZE_X * VASTU_CHUNK_SIZE_Z);
  }

  /**
   * Generates a spatial hash key string for chunk map lookups.
   * 
   * @param {number} cx - Chunk X coordinate.
   * @param {number} cz - Chunk Z coordinate.
   * @returns {string} Unique chunk hash key (e.g., "4,-2").
   */
  static get_chunk_key(cx, cz) {
    return `${cx},${cz}`;
  }

  /**
   * Determines the primary Vastu cardinal orientation for a given yaw angle in radians.
   * 
   * @param {number} yawRadians - Player look yaw in radians.
   * @returns {string} Named Vastu direction (Purva, Pashchima, Uttara, Dakshina).
   */
  static get_facing_vastu_direction(yawRadians) {
    // Normalize angle to [0, 2*PI)
    let angle = (yawRadians % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
    // Sectors: North (0 rad / 2PI), East (1.5PI / -0.5PI), South (PI), West (0.5PI)
    if (angle >= Math.PI * 1.75 || angle < Math.PI * 0.25) {
      return VastuDirection.UTTARA.name;
    } else if (angle >= Math.PI * 0.25 && angle < Math.PI * 0.75) {
      return VastuDirection.PASHCHIMA.name;
    } else if (angle >= Math.PI * 0.75 && angle < Math.PI * 1.25) {
      return VastuDirection.DAKSHINA.name;
    } else {
      return VastuDirection.PURVA.name;
    }
  }

  /**
   * Computes Euclidean distance between two 3D points.
   * 
   * @param {number} x1
   * @param {number} y1
   * @param {number} z1
   * @param {number} x2
   * @param {number} y2
   * @param {number} z2
   * @returns {number} Distance in blocks.
   */
  static distance(x1, y1, z1, x2, y2, z2) {
    const dx = x2 - x1;
    const dy = y2 - y1;
    const dz = z2 - z1;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }
}
