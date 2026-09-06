/**
 * @fileoverview DrishtiCamera - Sub-Frame Interpolated First-Person Camera System
 * @module player/drishti_camera
 * 
 * Features:
 * - Decoupled Yaw and Pitch rotation matrix with Euler 'YXZ' order preventing gimbal lock.
 * - Vertical pitch clamping strictly bounded to [-88.8°, +88.8°] ([-1.55, 1.55] rad).
 * - Micro-stutter-free sub-frame visual interpolation using fixed-step alpha factor.
 * - Dynamic biometric head-bobbing tied to Yoddha movement cadence and sprint gait.
 * - Directional vector query helpers for player movement projection and DDA raycasting.
 */

import * as THREE from 'three';

/**
 * First-person perspective camera controller adhering to Ancient Voxel aesthetics.
 */
export class DrishtiCamera {
  /**
   * Initializes the Drishti camera rig.
   * 
   * @param {THREE.PerspectiveCamera} camera - Target Three.js perspective camera.
   * @param {Object} [config={}] - Optional camera configurations.
   */
  constructor(camera, config = {}) {
    this.camera = camera;
    this.camera.rotation.order = 'YXZ';

    /**
     * Horizontal yaw rotation in radians (around world Y axis).
     * @type {number}
     */
    this.yaw = config.yaw ?? 0.0;

    /**
     * Vertical pitch rotation in radians (around local X axis).
     * @type {number}
     */
    this.pitch = config.pitch ?? 0.0;

    /**
     * Mouse look angular sensitivity.
     * @type {number}
     */
    this.sensitivity = config.sensitivity ?? 0.0022;

    /**
     * Hard pitch limit in radians (~88.8 degrees).
     * @type {number}
     */
    this.maxPitch = Math.PI / 2 - 0.02;

    /**
     * Elapsed cadence timer for head-bobbing oscillations.
     * @type {number}
     */
    this.bobTimer = 0.0;

    /**
     * Filtered head-bob displacement vector.
     * @type {THREE.Vector2}
     */
    this.bobOffset = new THREE.Vector2(0, 0);

    // Reusable vector allocations to prevent garbage collection spikes in render loop
    this._forward = new THREE.Vector3();
    this._right = new THREE.Vector3();
    this._look = new THREE.Vector3();
  }

  /**
   * Applies raw mouse movement deltas to camera yaw and pitch.
   * 
   * @param {number} deltaX - Horizontal mouse delta in pixels.
   * @param {number} deltaY - Vertical mouse delta in pixels.
   */
  applyMouseDelta(deltaX, deltaY) {
    this.yaw -= deltaX * this.sensitivity;
    this.pitch -= deltaY * this.sensitivity;

    // Strict vertical clamping
    if (this.pitch > this.maxPitch) this.pitch = this.maxPitch;
    if (this.pitch < -this.maxPitch) this.pitch = -this.maxPitch;
  }

  /**
   * Computes the normalized horizontal forward vector on the (X, Z) plane.
   * 
   * @param {THREE.Vector3} [target=new THREE.Vector3()] - Target vector to store result.
   * @returns {THREE.Vector3} Normalized horizontal forward vector.
   */
  getForwardVector(target = this._forward) {
    target.set(-Math.sin(this.yaw), 0, -Math.cos(this.yaw));
    return target.normalize();
  }

  /**
   * Computes the normalized horizontal right vector on the (X, Z) plane.
   * 
   * @param {THREE.Vector3} [target=new THREE.Vector3()] - Target vector to store result.
   * @returns {THREE.Vector3} Normalized horizontal right vector.
   */
  getRightVector(target = this._right) {
    target.set(Math.cos(this.yaw), 0, -Math.sin(this.yaw));
    return target.normalize();
  }

  /**
   * Computes the 3D gaze direction vector for voxel raycasting and targeting.
   * 
   * @param {THREE.Vector3} [target=new THREE.Vector3()] - Target vector to store result.
   * @returns {THREE.Vector3} Normalized 3D look direction vector.
   */
  getLookDirection(target = this._look) {
    const cosPitch = Math.cos(this.pitch);
    target.set(
      -Math.sin(this.yaw) * cosPitch,
      Math.sin(this.pitch),
      -Math.cos(this.yaw) * cosPitch
    );
    return target.normalize();
  }

  /**
   * Synchronizes visual camera transform with sub-frame interpolation and head-bobbing.
   * Executed during variable refresh `_process(delta, alpha)`.
   * 
   * @param {Object} entity - Yoddha player entity instance.
   * @param {number} delta - Frame delta time in seconds.
   * @param {number} alpha - Fixed-step interpolation factor [0.0, 1.0).
   */
  update(entity, delta, alpha) {
    // 1. Sub-frame linear interpolation (lerp) between physics snapshots
    const prev = entity.previousPosition;
    const curr = entity.position;

    const interpX = prev.x + (curr.x - prev.x) * alpha;
    const interpY = prev.y + (curr.y - prev.y) * alpha;
    const interpZ = prev.z + (curr.z - prev.z) * alpha;

    // 2. Compute dynamic head bobbing
    const eyeElevation = entity.dimensions.currentEyeHeight;
    let targetBobX = 0;
    let targetBobY = 0;

    if (entity.isGrounded && entity.speed > 0.2 && !entity.isFlying) {
      const frequency = entity.isSprinting ? 14.0 : 9.5;
      this.bobTimer += delta * frequency;

      const ampMultiplier = entity.isSprinting ? 1.3 : 1.0;
      targetBobY = Math.sin(this.bobTimer) * 0.045 * ampMultiplier;
      targetBobX = Math.cos(this.bobTimer * 0.5) * 0.025 * ampMultiplier;
    } else {
      // Settle timer smoothly when stopped or airborne
      this.bobTimer = 0;
    }

    // Smoothly filter bobbing transitions
    const bobSmoothing = Math.min(1.0, 12.0 * delta);
    this.bobOffset.x += (targetBobX - this.bobOffset.x) * bobSmoothing;
    this.bobOffset.y += (targetBobY - this.bobOffset.y) * bobSmoothing;

    // 3. Update Camera 3D Position
    this.camera.position.set(
      interpX + this.bobOffset.x,
      interpY + eyeElevation + this.bobOffset.y,
      interpZ
    );

    // 4. Update Camera Rotation (Yaw on Y, Pitch on X)
    this.camera.rotation.y = this.yaw;
    this.camera.rotation.x = this.pitch;
    this.camera.rotation.z = 0;
  }
}
