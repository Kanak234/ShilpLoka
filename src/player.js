/**
 * ============================================================================
 * FIRST-PERSON PLAYER CONTROLLER & VOXEL RAYCASTER
 * ============================================================================
 * Manages first-person camera orientation via PointerLock API, keyboard movement
 * processing, Fast Voxel Traversal (DDA raymarching) for sub-voxel block targeting,
 * block highlight wireframe, and block place/break interactions.
 */

import * as THREE from 'three';
import { PHYSICS, BLOCKS, REGISTRY } from './constants.js';

export class Player {
  constructor(scene, camera, world, physics, soundManager, particleManager) {
    this.scene = scene;
    this.camera = camera;
    this.world = world;
    this.physics = physics;
    this.sound = soundManager;
    this.particles = particleManager;

    // Spatial State
    this.position = new THREE.Vector3(8.5, 38, 8.5);
    this.velocity = new THREE.Vector3(0, 0, 0);
    this.yaw = 0.0;
    this.pitch = 0.0;

    // Movement & Mode Flags
    this.isFlying = false;
    this.isSprinting = false;
    this.isGrounded = false;
    this.keys = {};

    // Block Raycast Target
    this.targetBlock = null; // { x, y, z, normal: [nx, ny, nz], placePos: [px, py, pz], blockId }

    // Visual selection outline box
    this.selectionBox = this.createSelectionBox();
    this.scene.add(this.selectionBox);

    // Callbacks for inventory interactions
    this.onOpenCraftingTable = null;
    this.onInventoryChange = null;

    // Setup input listeners
    this.setupInputs();
  }

  createSelectionBox() {
    const geom = new THREE.BoxGeometry(1.004, 1.004, 1.004);
    const wireframe = new THREE.WireframeGeometry(geom);
    const line = new THREE.LineSegments(
      wireframe,
      new THREE.LineBasicMaterial({ color: 0x000000, linewidth: 2, transparent: true, opacity: 0.6 })
    );
    line.visible = false;
    return line;
  }

  setupInputs() {
    window.addEventListener('keydown', (e) => {
      this.keys[e.code] = true;

      // Toggle flying mode with 'KeyF'
      if (e.code === 'KeyF') {
        this.isFlying = !this.isFlying;
        this.velocity.set(0, 0, 0);
      }
    });

    window.addEventListener('keyup', (e) => {
      this.keys[e.code] = false;
    });

    // Mouse look with pointer lock
    window.addEventListener('mousemove', (e) => {
      if (document.pointerLockElement) {
        this.yaw -= e.movementX * PHYSICS.MOUSE_SENSITIVITY;
        this.pitch -= e.movementY * PHYSICS.MOUSE_SENSITIVITY;

        // Clamp vertical look angle
        const maxPitch = (Math.PI / 2) - 0.02;
        this.pitch = Math.max(-maxPitch, Math.min(maxPitch, this.pitch));
      }
    });
  }

  /**
   * Fast Voxel Traversal Algorithm (Amanatides & Woo DDA Raycast)
   * Evaluates discrete voxels along ray trajectory without missing thin geometry.
   */
  raycastVoxel(maxDistance = PHYSICS.REACH_DISTANCE) {
    const origin = new THREE.Vector3(
      this.position.x,
      this.position.y + PHYSICS.EYE_HEIGHT,
      this.position.z
    );

    // Compute look direction vector from yaw and pitch
    const dir = new THREE.Vector3(
      -Math.sin(this.yaw) * Math.cos(this.pitch),
      Math.sin(this.pitch),
      -Math.cos(this.yaw) * Math.cos(this.pitch)
    ).normalize();

    let x = Math.floor(origin.x);
    let y = Math.floor(origin.y);
    let z = Math.floor(origin.z);

    const stepX = dir.x > 0 ? 1 : -1;
    const stepY = dir.y > 0 ? 1 : -1;
    const stepZ = dir.z > 0 ? 1 : -1;

    const deltaX = dir.x !== 0 ? Math.abs(1 / dir.x) : Infinity;
    const deltaY = dir.y !== 0 ? Math.abs(1 / dir.y) : Infinity;
    const deltaZ = dir.z !== 0 ? Math.abs(1 / dir.z) : Infinity;

    let maxX = dir.x > 0 ? (x + 1 - origin.x) * deltaX : (origin.x - x) * deltaX;
    let maxY = dir.y > 0 ? (y + 1 - origin.y) * deltaY : (origin.y - y) * deltaY;
    let maxZ = dir.z > 0 ? (z + 1 - origin.z) * deltaZ : (origin.z - z) * deltaZ;

    let normal = [0, 0, 0];
    let distance = 0;

    while (distance < maxDistance) {
      const blockId = this.world.getBlock(x, y, z);
      if (blockId !== BLOCKS.AIR && blockId !== BLOCKS.WATER) {
        return {
          hit: true,
          x, y, z,
          blockId,
          normal,
          placePos: [x + normal[0], y + normal[1], z + normal[2]],
        };
      }

      if (maxX < maxY) {
        if (maxX < maxZ) {
          x += stepX;
          distance = maxX;
          maxX += deltaX;
          normal = [-stepX, 0, 0];
        } else {
          z += stepZ;
          distance = maxZ;
          maxZ += deltaZ;
          normal = [0, 0, -stepZ];
        }
      } else {
        if (maxY < maxZ) {
          y += stepY;
          distance = maxY;
          maxY += deltaY;
          normal = [0, -stepY, 0];
        } else {
          z += stepZ;
          distance = maxZ;
          maxZ += deltaZ;
          normal = [0, 0, -stepZ];
        }
      }
    }

    return { hit: false };
  }

  /**
   * Left-click handler: Breaks targeted block and spawns drops / particle debris
   */
  breakBlock(inventory) {
    if (!this.targetBlock || !this.targetBlock.hit) return;

    const { x, y, z, blockId } = this.targetBlock;
    const info = REGISTRY[blockId];
    if (info && info.indestructible) return; // Cannot break bedrock

    // Spawn 3D debris particles
    this.particles.spawnBreakParticles(x + 0.5, y + 0.5, z + 0.5, blockId);

    // Play crunchy block sound
    this.sound.playBreak(info?.sound || 'dirt');

    // Remove block from world
    this.world.setBlock(x, y, z, BLOCKS.AIR, true);

    // Give dropped item to player inventory
    const dropId = info?.drop ?? blockId;
    if (dropId !== BLOCKS.AIR) {
      inventory.addItem(dropId, 1);
    }
  }

  /**
   * Right-click handler: Places selected hotbar block or interacts with crafting table
   */
  placeBlock(inventory) {
    if (!this.targetBlock || !this.targetBlock.hit) return;

    const { x, y, z, blockId, placePos } = this.targetBlock;

    // Check interaction with Crafting Table
    if (blockId === BLOCKS.CRAFTING_TABLE) {
      if (this.onOpenCraftingTable) {
        this.onOpenCraftingTable();
        return;
      }
    }

    const activeItem = inventory.getActiveItem();
    if (!activeItem || activeItem.count <= 0) return;

    const reg = REGISTRY[activeItem.id];
    if (!reg || !reg.isBlock) return; // Only placeable blocks

    const [px, py, pz] = placePos;

    // Check if new block would intersect the player's bounding box
    const playerAABB = this.physics.getPlayerAABB(this.position);
    const blockAABB = {
      minX: px, minY: py, minZ: pz,
      maxX: px + 1, maxY: py + 1, maxZ: pz + 1
    };

    const overlaps = (
      playerAABB.minX < blockAABB.maxX &&
      playerAABB.maxX > blockAABB.minX &&
      playerAABB.minY < blockAABB.maxY &&
      playerAABB.maxY > blockAABB.minY &&
      playerAABB.minZ < blockAABB.maxZ &&
      playerAABB.maxZ > blockAABB.minZ
    );

    if (overlaps) {
      return; // Prevent suffocating self
    }

    // Place block into voxel world
    const placed = this.world.setBlock(px, py, pz, activeItem.id, true);
    if (placed) {
      this.sound.playPlace();
      inventory.consumeActiveItem(1);
    }
  }

  /**
   * Updates camera orientation, keyboard motion vector, physics simulation,
   * raycast targeting, and block selection box.
   */
  update(dt, inventory) {
    // 1. Process movement inputs
    const forward = new THREE.Vector3(-Math.sin(this.yaw), 0, -Math.cos(this.yaw)).normalize();
    const right = new THREE.Vector3(Math.cos(this.yaw), 0, -Math.sin(this.yaw)).normalize();

    const moveDir = new THREE.Vector3(0, 0, 0);
    if (this.keys['KeyW']) moveDir.add(forward);
    if (this.keys['KeyS']) moveDir.sub(forward);
    if (this.keys['KeyD']) moveDir.add(right);
    if (this.keys['KeyA']) moveDir.sub(right);

    this.isSprinting = !!this.keys['ControlLeft'] || !!this.keys['ControlRight'];
    const currentSpeed = this.isFlying
      ? PHYSICS.FLY_SPEED
      : (this.isSprinting ? PHYSICS.SPRINT_SPEED : PHYSICS.WALK_SPEED);

    if (moveDir.lengthSq() > 0) {
      moveDir.normalize();
      this.velocity.x = moveDir.x * currentSpeed;
      this.velocity.z = moveDir.z * currentSpeed;

      // Footstep sound when walking on ground
      if (this.isGrounded && !this.isFlying) {
        const blockBelow = this.world.getBlock(
          Math.floor(this.position.x),
          Math.floor(this.position.y - 0.2),
          Math.floor(this.position.z)
        );
        const snd = REGISTRY[blockBelow]?.sound || 'grass';
        this.sound.playFootstep(snd);
      }
    }

    if (this.isFlying) {
      if (this.keys['Space']) this.velocity.y = currentSpeed;
      else if (this.keys['ShiftLeft'] || this.keys['KeyC']) this.velocity.y = -currentSpeed;
      else this.velocity.y = 0;
    } else {
      // Jump impulse
      if (this.keys['Space'] && this.isGrounded) {
        this.velocity.y = PHYSICS.JUMP_FORCE;
        this.sound.playJump();
      }
    }

    // 2. Physics & AABB Collision Step
    const physResult = this.physics.update(
      this.position,
      this.velocity,
      moveDir,
      this.isFlying,
      dt
    );
    this.isGrounded = physResult.isGrounded;

    // 3. Sync Three.js Camera Position and Rotation
    this.camera.position.set(
      this.position.x,
      this.position.y + PHYSICS.EYE_HEIGHT,
      this.position.z
    );
    this.camera.rotation.set(0, 0, 0);
    this.camera.rotation.y = this.yaw;
    this.camera.rotation.x = this.pitch;

    // 4. Update Raycast and Target Highlight
    this.targetBlock = this.raycastVoxel();
    if (this.targetBlock.hit) {
      this.selectionBox.visible = true;
      this.selectionBox.position.set(
        this.targetBlock.x + 0.5,
        this.targetBlock.y + 0.5,
        this.targetBlock.z + 0.5
      );
    } else {
      this.selectionBox.visible = false;
    }
  }
}
