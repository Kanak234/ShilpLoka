/**
 * @fileoverview VoxelPhysicsSystem - Fixed 60Hz Swept AABB & 0.6m Auto Step-Up System
 * @module shilploka/ecs/systems/voxel_physics_system
 * 
 * Production ECS system handling:
 * 1. Multi-axis swept AABB collision queries (Y -> X -> Z) against ShilpWorld voxels.
 * 2. 0.6-block auto step-up to smoothly climb Harappan ghat stairs without jumping.
 * 3. Deterministic 60Hz physics with dual-buffered positions for sub-frame alpha lerping.
 * 4. Snappy Vedic gravity and ground contact state management.
 */

import * as THREE from 'three';
import { ComponentMask } from '../components.js';
import { SHILP_BLOCK_REGISTRY, ShilpBlockId } from '../../world/voxel_constants.js';

export class VoxelPhysicsSystem {
  /**
   * @param {import('../../world/shilp_world.js').ShilpWorld} world - Master voxel world.
   */
  constructor(world) {
    this.name = 'VoxelPhysicsSystem';
    this.mask = ComponentMask.TRANSFORM | ComponentMask.KINEMATICS | ComponentMask.PLAYER_INPUT | ComponentMask.CAMERA_RIG;
    this.world = world;

    // Player AABB dimensions
    this.halfWidth = 0.3;  // Width 0.6m
    this.height = 1.8;     // Height 1.8m
    this.halfDepth = 0.3;  // Depth 0.6m
    this.stepHeight = 0.6; // Auto step-up threshold

    this._fwd = new THREE.Vector3();
    this._rt = new THREE.Vector3();
    this._moveDir = new THREE.Vector3();
  }

  /**
   * Fixed 60Hz physics step.
   * 
   * @param {number} delta - Fixed time delta (1/60s).
   * @param {number[]} entities - Matching entity IDs.
   * @param {import('../ecs_registry.js').ShilpECS} ecs - ECS registry.
   */
  updateFixed(delta, entities, ecs) {
    for (let i = 0; i < entities.length; i++) {
      const id = entities[i];
      const transform = ecs.getComponent(id, 'Transform');
      const kinematics = ecs.getComponent(id, 'Kinematics');
      const input = ecs.getComponent(id, 'PlayerInput');
      const cameraRig = ecs.getComponent(id, 'CameraRig');

      // 1. Snapshot previous position for sub-frame visual alpha lerp
      transform.previousPosition.copy(transform.position);

      // 2. Flight Mode Toggle
      if (input.toggleFlight) {
        kinematics.isFlying = !kinematics.isFlying;
        kinematics.velocity.set(0, 0, 0);
        input.toggleFlight = false;
      }

      // 3. Project movement intent along camera yaw
      this._fwd.set(-Math.sin(cameraRig.yaw), 0, -Math.cos(cameraRig.yaw)).normalize();
      this._rt.set(Math.cos(cameraRig.yaw), 0, -Math.sin(cameraRig.yaw)).normalize();

      this._moveDir.set(0, 0, 0);
      if (input.moveIntent.z !== 0) {
        this._moveDir.addScaledVector(this._fwd, -input.moveIntent.z);
      }
      if (input.moveIntent.x !== 0) {
        this._moveDir.addScaledVector(this._rt, input.moveIntent.x);
      }
      if (this._moveDir.lengthSq() > 0) {
        this._moveDir.normalize();
      }

      // 4. Target Speed Evaluation
      let targetSpeed = kinematics.walkSpeed;
      if (kinematics.isFlying) {
        targetSpeed = kinematics.flySpeed;
      } else if (input.isCrouching) {
        targetSpeed = kinematics.crouchSpeed;
      } else if (input.isSprinting) {
        targetSpeed = kinematics.sprintSpeed;
      }

      const desiredVx = this._moveDir.x * targetSpeed;
      const desiredVz = this._moveDir.z * targetSpeed;

      if (kinematics.isFlying) {
        // Vimana Flight Kinematics
        kinematics.velocity.x = desiredVx;
        kinematics.velocity.z = desiredVz;
        if (input.isJumping) {
          kinematics.velocity.y = kinematics.flySpeed * 0.75;
        } else if (input.isCrouching) {
          kinematics.velocity.y = -kinematics.flySpeed * 0.75;
        } else {
          kinematics.velocity.y = 0;
        }
        transform.position.addScaledVector(kinematics.velocity, delta);
        kinematics.isGrounded = false;
      } else {
        // Terrestrial Locomotion & Acceleration
        const accel = kinematics.isGrounded ? 50.0 : 18.0;
        const t = Math.min(1.0, accel * delta);

        if (this._moveDir.lengthSq() > 0) {
          kinematics.velocity.x += (desiredVx - kinematics.velocity.x) * t;
          kinematics.velocity.z += (desiredVz - kinematics.velocity.z) * t;
        } else {
          // Decelerate smoothly when idle
          const dragFactor = kinematics.isGrounded ? kinematics.drag : kinematics.airDrag;
          const decay = Math.exp(-dragFactor * delta);
          kinematics.velocity.x *= decay;
          kinematics.velocity.z *= decay;
        }

        // Jump Impulse
        if (input.isJumping && kinematics.isGrounded) {
          kinematics.velocity.y = kinematics.jumpImpulse;
          kinematics.isGrounded = false;
        }

        // Apply Gravity (snappy Vedic gravity when airborne)
        if (!kinematics.isGrounded) {
          kinematics.velocity.y -= kinematics.gravity * delta;
          if (kinematics.velocity.y < kinematics.terminalVelocity) {
            kinematics.velocity.y = kinematics.terminalVelocity;
          }
        } else {
          kinematics.velocity.y = -0.1; // Gentle floor contact snap
        }

        // 5. Multi-Axis Swept Voxel Collision & 0.6m Auto Step-Up
        this._resolveVoxelCollision(transform, kinematics, delta);
      }

      // Compute planar speed
      kinematics.speed = Math.hypot(kinematics.velocity.x, kinematics.velocity.z);
    }
  }

  /**
   * Swept AABB collision resolution with auto step-up.
   * @private
   */
  _resolveVoxelCollision(transform, kinematics, delta) {
    const pos = transform.position;
    const targetDx = kinematics.velocity.x * delta;
    const targetDy = kinematics.velocity.y * delta;
    const targetDz = kinematics.velocity.z * delta;

    // 1. Resolve Vertical Movement (Y-axis)
    let newY = pos.y + targetDy;
    let hitGround = false;
    let hitCeiling = false;

    if (targetDy < 0) {
      // Moving downwards (falling / landing)
      const minX = Math.floor(pos.x - this.halfWidth);
      const maxX = Math.floor(pos.x + this.halfWidth);
      const minZ = Math.floor(pos.z - this.halfDepth);
      const maxZ = Math.floor(pos.z + this.halfDepth);
      const targetY = Math.floor(newY);

      for (let x = minX; x <= maxX; x++) {
        for (let z = minZ; z <= maxZ; z++) {
          const b = this.world.getBlock(x, targetY, z);
          if (SHILP_BLOCK_REGISTRY[b]?.solid) {
            newY = targetY + 1.0;
            hitGround = true;
            break;
          }
        }
        if (hitGround) break;
      }
    } else if (targetDy > 0) {
      // Moving upwards (jumping)
      const minX = Math.floor(pos.x - this.halfWidth);
      const maxX = Math.floor(pos.x + this.halfWidth);
      const minZ = Math.floor(pos.z - this.halfDepth);
      const maxZ = Math.floor(pos.z + this.halfDepth);
      const headY = Math.floor(newY + this.height);

      for (let x = minX; x <= maxX; x++) {
        for (let z = minZ; z <= maxZ; z++) {
          const b = this.world.getBlock(x, headY, z);
          if (SHILP_BLOCK_REGISTRY[b]?.solid) {
            newY = headY - this.height;
            hitCeiling = true;
            break;
          }
        }
        if (hitCeiling) break;
      }
    }

    pos.y = newY;
    if (hitGround) {
      kinematics.isGrounded = true;
      kinematics.velocity.y = 0;
    } else {
      kinematics.isGrounded = false;
      if (hitCeiling) kinematics.velocity.y = 0;
    }

    // 2. Resolve Horizontal Movement with Auto Step-Up
    const origX = pos.x;
    const origZ = pos.z;

    // Normal movement trial without step-up
    const normalX = this._testHorizontalAxis(pos.x, pos.y, pos.z, targetDx, 0);
    const normalZ = this._testHorizontalAxis(normalX, pos.y, pos.z, 0, targetDz);
    const normalDistSq = (normalX - origX) ** 2 + (normalZ - origZ) ** 2;
    const targetDistSq = targetDx ** 2 + targetDz ** 2;

    let useStepUp = false;
    let stepX = origX;
    let stepY = pos.y;
    let stepZ = origZ;

    // If blocked horizontally and on ground, try 0.6m auto step-up
    if (kinematics.isGrounded && normalDistSq < targetDistSq - 1e-6) {
      const elevatedY = pos.y + this.stepHeight;
      // Test if space above is clear
      if (!this._isCollidingAt(origX, elevatedY, origZ)) {
        stepX = this._testHorizontalAxis(origX, elevatedY, origZ, targetDx, 0);
        stepZ = this._testHorizontalAxis(stepX, elevatedY, origZ, 0, targetDz);

        // Step back down onto obstacle surface
        let groundedStepY = elevatedY;
        while (groundedStepY > pos.y) {
          if (this._isCollidingAt(stepX, groundedStepY - 0.05, stepZ)) {
            break;
          }
          groundedStepY -= 0.05;
        }

        const stepDistSq = (stepX - origX) ** 2 + (stepZ - origZ) ** 2;
        if (stepDistSq > normalDistSq + 1e-4) {
          useStepUp = true;
          stepY = groundedStepY;
        }
      }
    }

    if (useStepUp) {
      pos.x = stepX;
      pos.y = stepY;
      pos.z = stepZ;
      kinematics.isGrounded = true;
      kinematics.didAutoStep = true;
    } else {
      pos.x = normalX;
      pos.z = normalZ;
      kinematics.didAutoStep = false;
    }
  }

  _testHorizontalAxis(px, py, pz, dx, dz) {
    const target = (dx !== 0) ? px + dx : pz + dz;
    const testX = (dx !== 0) ? target : px;
    const testZ = (dz !== 0) ? target : pz;

    if (!this._isCollidingAt(testX, py, testZ)) {
      return target;
    }
    return (dx !== 0) ? px : pz; // Blocked
  }

  _isCollidingAt(px, py, pz) {
    const minX = Math.floor(px - this.halfWidth);
    const maxX = Math.floor(px + this.halfWidth);
    const minY = Math.floor(py);
    const maxY = Math.floor(py + this.height - 0.05);
    const minZ = Math.floor(pz - this.halfDepth);
    const maxZ = Math.floor(pz + this.halfDepth);

    for (let y = minY; y <= maxY; y++) {
      for (let x = minX; x <= maxX; x++) {
        for (let z = minZ; z <= maxZ; z++) {
          const b = this.world.getBlock(x, y, z);
          if (SHILP_BLOCK_REGISTRY[b]?.solid) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
