/**
 * @fileoverview CameraInterpolationSystem - Sub-frame Visual Interpolation & Head-Bobbing
 * @module shilploka/ecs/systems/camera_system
 */

import * as THREE from 'three';
import { ComponentMask } from '../components.js';

export class CameraInterpolationSystem {
  constructor() {
    this.name = 'CameraInterpolationSystem';
    this.mask = ComponentMask.TRANSFORM | ComponentMask.CAMERA_RIG;
  }

  /**
   * Variable refresh camera interpolation.
   * 
   * @param {number} delta - Frame delta time.
   * @param {number} alpha - Fixed-step interpolation factor [0.0, 1.0).
   * @param {number[]} entities - Matching entity IDs.
   * @param {import('../ecs_registry.js').ShilpECS} ecs - ECS registry.
   */
  updateVariable(delta, alpha, entities, ecs) {
    for (let i = 0; i < entities.length; i++) {
      const id = entities[i];
      const transform = ecs.getComponent(id, 'Transform');
      const cameraRig = ecs.getComponent(id, 'CameraRig');
      const kinematics = ecs.getComponent(id, 'Kinematics');
      const input = ecs.getComponent(id, 'PlayerInput');

      // 1. Sub-Frame Lerp between previous and current physics ticks
      const prev = transform.previousPosition;
      const curr = transform.position;

      const interpX = prev.x + (curr.x - prev.x) * alpha;
      const interpY = prev.y + (curr.y - prev.y) * alpha;
      const interpZ = prev.z + (curr.z - prev.z) * alpha;

      // 2. Dynamic biometric head-bobbing
      let targetBobX = 0;
      let targetBobY = 0;

      const isMoving = kinematics && kinematics.isGrounded && kinematics.speed > 0.2 && !kinematics.isFlying;
      if (isMoving) {
        const isSprinting = input && input.isSprinting;
        const freq = isSprinting ? 14.0 : 9.5;
        cameraRig.bobTimer += delta * freq;
        const amp = isSprinting ? 1.3 : 1.0;
        targetBobY = Math.sin(cameraRig.bobTimer) * 0.045 * amp;
        targetBobX = Math.cos(cameraRig.bobTimer * 0.5) * 0.025 * amp;
      } else {
        cameraRig.bobTimer = 0;
      }

      const smoothing = Math.min(1.0, 12.0 * delta);
      cameraRig.bobOffset.x += (targetBobX - cameraRig.bobOffset.x) * smoothing;
      cameraRig.bobOffset.y += (targetBobY - cameraRig.bobOffset.y) * smoothing;

      // 3. Update Camera 3D Transform
      const eyeElevation = cameraRig.currentEyeHeight;
      cameraRig.camera.position.set(
        interpX + cameraRig.bobOffset.x,
        interpY + eyeElevation + cameraRig.bobOffset.y,
        interpZ
      );

      cameraRig.camera.rotation.order = 'YXZ';
      cameraRig.camera.rotation.y = cameraRig.yaw;
      cameraRig.camera.rotation.x = cameraRig.pitch;
      cameraRig.camera.rotation.z = 0;
    }
  }
}
