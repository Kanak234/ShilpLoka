/**
 * @fileoverview MovementKinematicsSystem - Fixed 60Hz Kinematic Simulation System
 * @module shilploka/ecs/systems/movement_system
 * 
 * Advances physical entities deterministically using fixed-timestep kinematics,
 * projected orientation, snappy Vedic gravity, and momentum damping.
 */

import * as THREE from 'three';
import { ComponentMask } from '../components.js';

export class MovementKinematicsSystem {
  constructor() {
    this.name = 'MovementKinematicsSystem';
    this.mask = ComponentMask.TRANSFORM | ComponentMask.KINEMATICS | ComponentMask.PLAYER_INPUT | ComponentMask.CAMERA_RIG;

    this._fwd = new THREE.Vector3();
    this._rt = new THREE.Vector3();
    this._moveDir = new THREE.Vector3();
  }

  /**
   * Fixed 60Hz physics update.
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

      // 1. Snapshot previous position for sub-frame alpha interpolation
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
        // Creative Flight Kinematics
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
        // Terrestrial Locomotion & Snappy Gravity
        const accel = kinematics.isGrounded ? 50.0 : 18.0;
        const t = Math.min(1.0, accel * delta);

        if (this._moveDir.lengthSq() > 0) {
          kinematics.velocity.x += (desiredVx - kinematics.velocity.x) * t;
          kinematics.velocity.z += (desiredVz - kinematics.velocity.z) * t;
        } else {
          // Decelerate smoothly when no directional keys are held
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
        }

        // Advance Position
        transform.position.addScaledVector(kinematics.velocity, delta);

        // Ground Foundation Plane Collision (Y = 1.0)
        if (transform.position.y <= 1.0) {
          transform.position.y = 1.0;
          kinematics.velocity.y = 0;
          kinematics.isGrounded = true;
        }
      }


      // Planar speed
      kinematics.speed = Math.hypot(kinematics.velocity.x, kinematics.velocity.z);
    }
  }
}
