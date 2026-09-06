/**
 * ============================================================================
 * PLAYER PHYSICS & AABB VOXEL COLLISION ENGINE
 * ============================================================================
 * Implements axis-aligned bounding box (AABB) collision detection and swept
 * multi-axis resolution against the discrete voxel grid. Handles gravity,
 * momentum damping, ground detection, jump impulses, and 0.5-block step-up mechanics.
 */

import { PHYSICS, REGISTRY, BLOCKS } from './constants.js';

export class AABB {
  constructor(minX, minY, minZ, maxX, maxY, maxZ) {
    this.minX = minX;
    this.minY = minY;
    this.minZ = minZ;
    this.maxX = maxX;
    this.maxY = maxY;
    this.maxZ = maxZ;
  }

  intersects(other) {
    return (
      this.minX < other.maxX &&
      this.maxX > other.minX &&
      this.minY < other.maxY &&
      this.maxY > other.minY &&
      this.minZ < other.maxZ &&
      this.maxZ > other.minZ
    );
  }
}

export class PhysicsEngine {
  constructor(world) {
    this.world = world;
  }

  /**
   * Constructs the player's current bounding box given feet position
   */
  getPlayerAABB(pos) {
    const halfWidth = PHYSICS.PLAYER_WIDTH / 2;
    return new AABB(
      pos.x - halfWidth,
      pos.y,
      pos.z - halfWidth,
      pos.x + halfWidth,
      pos.y + PHYSICS.PLAYER_HEIGHT,
      pos.z + halfWidth
    );
  }

  /**
   * Checks if a block at (bx, by, bz) is solid for collision
   */
  isSolid(bx, by, bz) {
    const id = this.world.getBlock(bx, by, bz);
    if (id === BLOCKS.AIR) return false;
    const info = REGISTRY[id];
    return info ? info.solid : false;
  }

  /**
   * Retrieves all solid block bounding boxes within the vicinity of the given AABB
   */
  getSurroundingBlockAABBs(aabb) {
    const boxes = [];
    const minX = Math.floor(aabb.minX);
    const maxX = Math.floor(aabb.maxX);
    const minY = Math.floor(aabb.minY);
    const maxY = Math.floor(aabb.maxY);
    const minZ = Math.floor(aabb.minZ);
    const maxZ = Math.floor(aabb.maxZ);

    for (let x = minX; x <= maxX; x++) {
      for (let y = minY; y <= maxY; y++) {
        for (let z = minZ; z <= maxZ; z++) {
          if (this.isSolid(x, y, z)) {
            boxes.push(new AABB(x, y, z, x + 1, y + 1, z + 1));
          }
        }
      }
    }
    return boxes;
  }

  /**
   * Advances player position and velocity over delta time dt with rigorous
   * axis-separated collision resolution.
   */
  update(pos, vel, input, isFlying, dt) {
    // Clamp delta time to avoid large tunnel clipping on frame drops
    const delta = Math.min(dt, 0.05);
    const halfWidth = PHYSICS.PLAYER_WIDTH / 2;

    if (isFlying) {
      // Creative flight physics: direct omni-directional velocity and heavy damping
      pos.x += vel.x * delta;
      pos.y += vel.y * delta;
      pos.z += vel.z * delta;

      const flyDamp = Math.pow(0.001, delta);
      vel.x *= flyDamp;
      vel.y *= flyDamp;
      vel.z *= flyDamp;
      return { isGrounded: false };
    }

    // Survival Mode Physics: Apply gravity
    vel.y += PHYSICS.GRAVITY * delta;
    if (vel.y < PHYSICS.TERMINAL_VELOCITY) {
      vel.y = PHYSICS.TERMINAL_VELOCITY;
    }

    // --- 1. RESOLVE VERTICAL (Y) MOVEMENT ---
    pos.y += vel.y * delta;
    let isGrounded = false;
    let playerBox = this.getPlayerAABB(pos);
    let blockBoxes = this.getSurroundingBlockAABBs(playerBox);

    for (const box of blockBoxes) {
      if (playerBox.intersects(box)) {
        if (vel.y < 0) {
          // Landing on a solid block below
          pos.y = box.maxY;
          vel.y = 0;
          isGrounded = true;
        } else if (vel.y > 0) {
          // Head hit ceiling block above
          pos.y = box.minY - PHYSICS.PLAYER_HEIGHT;
          vel.y = 0;
        }
        playerBox = this.getPlayerAABB(pos);
      }
    }

    // --- 2. RESOLVE HORIZONTAL (X) MOVEMENT ---
    const oldX = pos.x;
    pos.x += vel.x * delta;
    playerBox = this.getPlayerAABB(pos);
    blockBoxes = this.getSurroundingBlockAABBs(playerBox);

    let collidedX = false;
    for (const box of blockBoxes) {
      if (playerBox.intersects(box)) {
        collidedX = true;
        if (vel.x > 0) {
          pos.x = box.minX - halfWidth - 0.0001;
        } else if (vel.x < 0) {
          pos.x = box.maxX + halfWidth + 0.0001;
        }
        vel.x = 0;
        playerBox = this.getPlayerAABB(pos);
      }
    }

    // Auto-step up mechanic for smooth 0.5-0.6 block terrain traversal
    if (collidedX && isGrounded) {
      // Attempt stepping up by 0.6 blocks
      const stepY = 0.6;
      const testPos = { x: oldX + vel.x * delta, y: pos.y + stepY, z: pos.z };
      const testBox = this.getPlayerAABB(testPos);
      const testBlocks = this.getSurroundingBlockAABBs(testBox);
      let blocked = false;
      for (const b of testBlocks) {
        if (testBox.intersects(b)) {
          blocked = true;
          break;
        }
      }
      if (!blocked) {
        pos.x = testPos.x;
        pos.y = testPos.y;
      }
    }

    // --- 3. RESOLVE HORIZONTAL (Z) MOVEMENT ---
    const oldZ = pos.z;
    pos.z += vel.z * delta;
    playerBox = this.getPlayerAABB(pos);
    blockBoxes = this.getSurroundingBlockAABBs(playerBox);

    let collidedZ = false;
    for (const box of blockBoxes) {
      if (playerBox.intersects(box)) {
        collidedZ = true;
        if (vel.z > 0) {
          pos.z = box.minZ - halfWidth - 0.0001;
        } else if (vel.z < 0) {
          pos.z = box.maxZ + halfWidth + 0.0001;
        }
        vel.z = 0;
        playerBox = this.getPlayerAABB(pos);
      }
    }

    // Auto-step up for Z collision
    if (collidedZ && isGrounded) {
      const stepY = 0.6;
      const testPos = { x: pos.x, y: pos.y + stepY, z: oldZ + vel.z * delta };
      const testBox = this.getPlayerAABB(testPos);
      const testBlocks = this.getSurroundingBlockAABBs(testBox);
      let blocked = false;
      for (const b of testBlocks) {
        if (testBox.intersects(b)) {
          blocked = true;
          break;
        }
      }
      if (!blocked) {
        pos.z = testPos.z;
        pos.y = testPos.y;
      }
    }

    // Horizontal momentum damping / friction
    const damping = isGrounded ? Math.pow(0.00001, delta) : Math.pow(0.05, delta);
    vel.x *= damping;
    vel.z *= damping;

    // Void floor fallback (respawn if fell out of world)
    if (pos.y < -10) {
      pos.x = 8;
      pos.y = 45;
      pos.z = 8;
      vel.x = 0;
      vel.y = 0;
      vel.z = 0;
    }

    return { isGrounded };
  }
}
