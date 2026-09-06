/**
 * @fileoverview InputDispatchSystem - Bridges PranaInput actions to ECS entities
 * @module shilploka/ecs/systems/input_system
 */

import { ComponentMask } from '../components.js';
import { VastuAction } from '../../../core/prana_input.js';

export class InputDispatchSystem {
  /**
   * @param {import('../../core/prana_input.js').PranaInput} pranaInput - Master input buffer.
   */
  constructor(pranaInput) {
    this.name = 'InputDispatchSystem';
    this.input = pranaInput;
    this.mask = ComponentMask.PLAYER_INPUT | ComponentMask.CAMERA_RIG;
  }

  /**
   * Fixed 60Hz input polling.
   * 
   * @param {number} delta - Time delta.
   * @param {number[]} entities - Matching entity IDs.
   * @param {import('../ecs_registry.js').ShilpECS} ecs - ECS registry.
   */
  updateFixed(delta, entities, ecs) {
    // 1. Consume mouse movement delta for camera orientation
    const mouseDelta = this.input.consume_mouse_delta();

    for (let i = 0; i < entities.length; i++) {
      const id = entities[i];
      const playerInput = ecs.getComponent(id, 'PlayerInput');
      const cameraRig = ecs.getComponent(id, 'CameraRig');

      // Update Camera Look Angles if pointer lock is active or touch rotation occurs
      if ((this.input.isDrishtiLocked || this.input.isTouchMode) && (mouseDelta.x !== 0 || mouseDelta.y !== 0)) {
        cameraRig.yaw -= mouseDelta.x * cameraRig.sensitivity;
        cameraRig.pitch -= mouseDelta.y * cameraRig.sensitivity;

        // Clamp vertical pitch to prevent gimbal flip
        if (cameraRig.pitch > cameraRig.maxPitch) cameraRig.pitch = cameraRig.maxPitch;
        if (cameraRig.pitch < cameraRig.minPitch) cameraRig.pitch = cameraRig.minPitch;
      }

      // Update Locomotion Intentions
      const moveVec = this.input.get_movement_vector();
      playerInput.moveIntent.x = moveVec.x;
      playerInput.moveIntent.z = moveVec.z;

      playerInput.isJumping = this.input.is_action_pressed(VastuAction.JUMP);
      playerInput.isSprinting = this.input.is_action_pressed(VastuAction.SPRINT);
      playerInput.isCrouching = this.input.is_action_pressed(VastuAction.CROUCH);

      if (this.input.is_action_just_pressed(VastuAction.FLY_TOGGLE)) {
        playerInput.toggleFlight = true;
      }
    }
  }
}
