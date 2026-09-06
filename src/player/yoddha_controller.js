/**
 * @fileoverview YoddhaController - Ancient Indian Player Entity Coordinator
 * @module player/yoddha_controller
 * 
 * Features:
 * - Coordinates player spatial kinematics, state transitions, and dimensional scaling.
 * - Bridges PranaInput action events to VastuPhysics and DrishtiCamera.
 * - Supports walking, sprinting, crouching, jumping, and Vimana (creative flight) mode.
 * - Maintains historical position snapshots enabling zero-jitter sub-frame interpolation.
 */

import * as THREE from 'three';
import { VastuAction } from '../core/prana_input.js';

/**
 * Player character entity representing an ancient builder / warrior.
 */
export class YoddhaController {
  /**
   * Initializes player entity parameters and coordinate vectors.
   * 
   * @param {Object} [config={}] - Initial configuration and spawn coordinates.
   */
  constructor(config = {}) {
    // 1. Spatial Kinematics
    const spawn = config.spawn ?? { x: 0, y: 12, z: 12 };
    this.position = new THREE.Vector3(spawn.x, spawn.y, spawn.z);
    this.previousPosition = new THREE.Vector3(spawn.x, spawn.y, spawn.z);
    this.velocity = new THREE.Vector3(0, 0, 0);

    // 2. Physical Dimensions (Vedic Human Stature in Voxel Units)
    this.dimensions = {
      width: 0.6,
      depth: 0.6,
      standingHeight: 1.8,
      crouchHeight: 1.35,
      currentHeight: 1.8,
      standingEyeHeight: 1.62,
      crouchEyeHeight: 1.20,
      currentEyeHeight: 1.62,
    };

    // 3. Locomotion Tuning Values
    this.walkSpeed = config.walkSpeed ?? 4.8;
    this.sprintSpeed = config.sprintSpeed ?? 8.2;
    this.crouchSpeed = config.crouchSpeed ?? 2.2;
    this.flySpeed = config.flySpeed ?? 14.0;
    this.jumpImpulse = config.jumpImpulse ?? 8.6;

    // 4. Locomotion State Flags
    this.isGrounded = false;
    this.isSprinting = false;
    this.isCrouching = false;
    this.isFlying = false;
    this.didAutoStep = false;
    this.speed = 0.0;

    // Scratch vector caches to eliminate runtime heap allocation
    this._moveDir = new THREE.Vector3();
    this._fwd = new THREE.Vector3();
    this._rt = new THREE.Vector3();
  }

  /**
   * Updates state, processes action mappings, and simulates physics step.
   * Executed at deterministic 60Hz via `_physics_process(delta)`.
   * 
   * @param {import('../core/prana_input.js').PranaInput} input - User input interface.
   * @param {import('./drishti_camera.js').DrishtiCamera} camera - Camera controller for yaw orientation.
   * @param {import('../physics/vastu_physics.js').VastuPhysics} physics - Physics simulator.
   * @param {number} delta - Fixed time delta in seconds (1/60s = 0.016667s).
   */
  update(input, camera, physics, delta) {
    // 1. Snapshot previous position for sub-frame alpha interpolation
    this.previousPosition.copy(this.position);

    // 2. Handle Vimana (Creative Flight) Toggle
    if (input.is_action_just_pressed(VastuAction.FLY_TOGGLE)) {
      this.isFlying = !this.isFlying;
      this.velocity.set(0, 0, 0);
    }

    // 3. Locomotion Modifiers (Crouch & Sprint)
    this.isCrouching = input.is_action_pressed(VastuAction.CROUCH) && !this.isFlying;
    this.isSprinting = input.is_action_pressed(VastuAction.SPRINT) && !this.isCrouching && !this.isFlying;

    // Smoothly adapt physical height and eye elevation
    const targetHeight = this.isCrouching ? this.dimensions.crouchHeight : this.dimensions.standingHeight;
    const targetEye = this.isCrouching ? this.dimensions.crouchEyeHeight : this.dimensions.standingEyeHeight;
    const heightLerp = Math.min(1.0, 16.0 * delta);
    this.dimensions.currentHeight += (targetHeight - this.dimensions.currentHeight) * heightLerp;
    this.dimensions.currentEyeHeight += (targetEye - this.dimensions.currentEyeHeight) * heightLerp;

    // 4. Calculate Intended Planar Direction Relative to Camera Yaw
    const moveInput = input.get_movement_vector();
    camera.getForwardVector(this._fwd);
    camera.getRightVector(this._rt);

    this._moveDir.set(0, 0, 0);
    if (moveInput.z !== 0) {
      // Negative Z is forward in our coordinate space
      this._moveDir.addScaledVector(this._fwd, -moveInput.z);
    }
    if (moveInput.x !== 0) {
      this._moveDir.addScaledVector(this._rt, moveInput.x);
    }

    if (this._moveDir.lengthSq() > 0) {
      this._moveDir.normalize();
    }

    // 5. Determine Target Horizontal Velocity
    let targetSpeed = this.walkSpeed;
    if (this.isFlying) {
      targetSpeed = this.flySpeed;
    } else if (this.isCrouching) {
      targetSpeed = this.crouchSpeed;
    } else if (this.isSprinting) {
      targetSpeed = this.sprintSpeed;
    }

    const desiredVx = this._moveDir.x * targetSpeed;
    const desiredVz = this._moveDir.z * targetSpeed;

    if (this.isFlying) {
      // Vimana Flight Kinematics: Direct responsive velocity
      this.velocity.x = desiredVx;
      this.velocity.z = desiredVz;

      if (input.is_action_pressed(VastuAction.JUMP)) {
        this.velocity.y = this.flySpeed * 0.75;
      } else if (input.is_action_pressed(VastuAction.CROUCH)) {
        this.velocity.y = -this.flySpeed * 0.75;
      } else {
        this.velocity.y = 0;
      }
    } else {
      // Terrestrial Locomotion: Acceleration Curve
      const accel = this.isGrounded ? 50.0 : 18.0;
      const t = Math.min(1.0, accel * delta);

      if (this._moveDir.lengthSq() > 0) {
        this.velocity.x += (desiredVx - this.velocity.x) * t;
        this.velocity.z += (desiredVz - this.velocity.z) * t;
      }

      // Jump Impulse Trigger
      if (input.is_action_just_pressed(VastuAction.JUMP) && this.isGrounded) {
        this.velocity.y = this.jumpImpulse;
        this.isGrounded = false;
      }
    }

    // 6. Execute Multi-Axis Swept Physics Simulation
    physics.update(this, delta);

    // 7. Compute Planar Speed for Telemetry and Biometric Head-Bobbing
    this.speed = Math.hypot(this.velocity.x, this.velocity.z);
  }

  /**
   * Sets player position directly (e.g. for respawn or teleport).
   * 
   * @param {number} x - Target X coordinate.
   * @param {number} y - Target Y coordinate.
   * @param {number} z - Target Z coordinate.
   */
  teleport(x, y, z) {
    this.position.set(x, y, z);
    this.previousPosition.set(x, y, z);
    this.velocity.set(0, 0, 0);
  }
}
