/**
 * @fileoverview ShilpLoka ECS Components - Production Schemas & Bitmask Flags
 * @module shilploka/ecs/components
 * 
 * Scalable component definitions adhering to clean-room Entity Component System (ECS) principles.
 * Each component is a pure data holder with no behavior logic.
 */

import * as THREE from 'three';

/**
 * Bitmask flags for O(1) archetype queries.
 * @readonly
 * @enum {number}
 */
export const ComponentMask = {
  TRANSFORM: 1 << 0,
  KINEMATICS: 1 << 1,
  PLAYER_INPUT: 1 << 2,
  CAMERA_RIG: 1 << 3,
  RENDERABLE: 1 << 4,
  HERITAGE_TAG: 1 << 5,
  BARTER_NPC: 1 << 6,
  FAUNA: 1 << 7,
};

/**
 * 3D Spatial Transform Component.
 */
export class TransformComponent {
  /**
   * @param {number} [x=0] - Initial X.
   * @param {number} [y=0] - Initial Y.
   * @param {number} [z=0] - Initial Z.
   */
  constructor(x = 0, y = 0, z = 0) {
    this.name = 'Transform';
    this.mask = ComponentMask.TRANSFORM;
    this.position = new THREE.Vector3(x, y, z);
    this.previousPosition = new THREE.Vector3(x, y, z);
    this.rotation = new THREE.Euler(0, 0, 0, 'YXZ');
    this.scale = new THREE.Vector3(1, 1, 1);
  }
}

/**
 * Kinematics & Physics Properties Component.
 */
export class KinematicsComponent {
  /**
   * @param {Object} [config={}] - Kinematic options.
   */
  constructor(config = {}) {
    this.name = 'Kinematics';
    this.mask = ComponentMask.KINEMATICS;
    this.velocity = new THREE.Vector3(0, 0, 0);
    this.acceleration = new THREE.Vector3(0, 0, 0);
    this.mass = config.mass ?? 75.0; // kg
    this.drag = config.drag ?? 12.0; // Ground friction coefficient
    this.airDrag = config.airDrag ?? 1.2;
    this.gravity = config.gravity ?? 26.0; // m/s^2
    this.terminalVelocity = config.terminalVelocity ?? -40.0;
    this.walkSpeed = config.walkSpeed ?? 4.8;
    this.sprintSpeed = config.sprintSpeed ?? 8.2;
    this.crouchSpeed = config.crouchSpeed ?? 2.2;
    this.flySpeed = config.flySpeed ?? 14.0;
    this.jumpImpulse = config.jumpImpulse ?? 8.6;
    this.isGrounded = false;
    this.isFlying = false;
    this.didAutoStep = false;
    this.speed = 0.0;
  }
}

/**
 * Player Locomotion Intent & Input Component.
 */
export class PlayerInputComponent {
  constructor() {
    this.name = 'PlayerInput';
    this.mask = ComponentMask.PLAYER_INPUT;
    this.moveIntent = { x: 0, z: 0 };
    this.isJumping = false;
    this.isSprinting = false;
    this.isCrouching = false;
    this.isMining = false;
    this.isBuilding = false;
    this.toggleFlight = false;
  }
}

/**
 * Perspective First-Person Camera Rig Component.
 */
export class CameraRigComponent {
  /**
   * @param {THREE.PerspectiveCamera} camera - Target camera.
   * @param {Object} [config={}] - Configuration options.
   */
  constructor(camera, config = {}) {
    this.name = 'CameraRig';
    this.mask = ComponentMask.CAMERA_RIG;
    this.camera = camera;
    this.yaw = config.yaw ?? 0.0;
    this.pitch = config.pitch ?? 0.0;
    this.sensitivity = config.sensitivity ?? 0.0022;
    this.minPitch = -Math.PI / 2 + 0.02;
    this.maxPitch = Math.PI / 2 - 0.02;
    this.standingEyeHeight = 1.62;
    this.crouchEyeHeight = 1.20;
    this.currentEyeHeight = 1.62;
    this.bobTimer = 0.0;
    this.bobOffset = new THREE.Vector2(0, 0);
  }
}

/**
 * Visual Scene Object Component.
 */
export class RenderableComponent {
  /**
   * @param {THREE.Object3D} object3D - Three.js mesh or group.
   */
  constructor(object3D) {
    this.name = 'Renderable';
    this.mask = ComponentMask.RENDERABLE;
    this.mesh = object3D;
    this.visible = true;
  }
}

/**
 * Ancient Heritage Monument Indestructibility Tag.
 */
export class HeritageTagComponent {
  /**
   * @param {string} monumentName - Name of ancient monument.
   * @param {string} archetype - Architectural archetype.
   */
  constructor(monumentName, archetype = 'HARAPPAN_HERITAGE') {
    this.name = 'HeritageTag';
    this.mask = ComponentMask.HERITAGE_TAG;
    this.monumentName = monumentName;
    this.archetype = archetype;
    this.isHeritage = true;
    this.indestructible = true; // Strict validation rule
  }
}

/**
 * Barter NPC System Component for trading spices & bronze.
 */
export class BarterNPCComponent {
  /**
   * @param {string} name - Merchant name.
   * @param {Object} initialStock - Starting trade inventory.
   */
  constructor(name, initialStock = {}) {
    this.name = 'BarterNPC';
    this.mask = ComponentMask.BARTER_NPC;
    this.merchantName = name;
    this.inventory = {
      cardamom: initialStock.cardamom ?? 20, // Elaichi
      pepper: initialStock.pepper ?? 50,     // Maricha
      saffron: initialStock.saffron ?? 10,   // Kesar
      bronzeIngots: initialStock.bronze ?? 15,
    };
    this.tradeRates = {
      cardamomToBronze: 2, // 2 Cardamom = 1 Bronze
      pepperToBronze: 5,   // 5 Pepper = 1 Bronze
      saffronToBronze: 0.5 // 1 Saffron = 2 Bronze
    };
  }
}

/**
 * Native Indian Subcontinent Fauna Component.
 */
export class FaunaComponent {
  /**
   * @param {'GAJA_ELEPHANT'|'ZEBU_CATTLE'|'VYAGHRA_TIGER'} species - Species type.
   */
  constructor(species) {
    this.name = 'Fauna';
    this.mask = ComponentMask.FAUNA;
    this.species = species;
    this.state = 'IDLE';
    this.energy = 100.0;
    this.wanderTimer = 0.0;
    this.targetPosition = new THREE.Vector3();
  }
}
