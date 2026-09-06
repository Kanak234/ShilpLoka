/**
 * @fileoverview VastuPhysics - Multi-Axis Swept AABB Voxel Collision Engine
 * @module physics/vastu_physics
 * 
 * Features:
 * - Deterministic AABB (Axis-Aligned Bounding Box) spatial mathematics.
 * - Multi-axis swept collision resolution (Y -> X -> Z) to prevent clipping and tunneling.
 * - 0.6-block auto step-up for effortless terrain traversal across Ancient Harappan steps.
 * - Ground detection, snappy gravity, terminal velocity clamps, and exponential friction decay.
 * - Decoupled collider provider compatible with both static plinths and procedural voxel Octrees.
 */

import * as THREE from 'three';

/**
 * Classical 3D Axis-Aligned Bounding Box (AABB) for Vastu spatial containment.
 */
export class VastuAABB {
  /**
   * Constructs an AABB with minimum and maximum bounds.
   * 
   * @param {number} [minX=0] - Minimum boundary along X-axis.
   * @param {number} [minY=0] - Minimum boundary along Y-axis.
   * @param {number} [minZ=0] - Minimum boundary along Z-axis.
   * @param {number} [maxX=0] - Maximum boundary along X-axis.
   * @param {number} [maxY=0] - Maximum boundary along Y-axis.
   * @param {number} [maxZ=0] - Maximum boundary along Z-axis.
   */
  constructor(minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0) {
    this.minX = minX;
    this.minY = minY;
    this.minZ = minZ;
    this.maxX = maxX;
    this.maxY = maxY;
    this.maxZ = maxZ;
  }

  /**
   * Sets bounds based on center coordinates and dimension sizes.
   * 
   * @param {number} cx - Center X coordinate.
   * @param {number} cy - Center Y coordinate.
   * @param {number} cz - Center Z coordinate.
   * @param {number} width - Total extent along X.
   * @param {number} height - Total extent along Y.
   * @param {number} depth - Total extent along Z.
   * @returns {VastuAABB} Reference to this AABB for chaining.
   */
  setFromCenterAndSize(cx, cy, cz, width, height, depth) {
    const hw = width / 2;
    const hd = depth / 2;
    this.minX = cx - hw;
    this.maxX = cx + hw;
    this.minY = cy;
    this.maxY = cy + height;
    this.minZ = cz - hd;
    this.maxZ = cz + hd;
    return this;
  }

  /**
   * Checks whether this AABB intersects with another AABB.
   * 
   * @param {VastuAABB} other - Bounding box to test against.
   * @returns {boolean} True if bounds overlap along all three cardinal axes.
   */
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

  /**
   * Duplicates this bounding box.
   * 
   * @returns {VastuAABB} Fresh clone.
   */
  clone() {
    return new VastuAABB(
      this.minX,
      this.minY,
      this.minZ,
      this.maxX,
      this.maxY,
      this.maxZ
    );
  }

  /**
   * Copies bounds from another AABB.
   * 
   * @param {VastuAABB} other - Source bounding box.
   * @returns {VastuAABB} Reference to this AABB.
   */
  copy(other) {
    this.minX = other.minX;
    this.minY = other.minY;
    this.minZ = other.minZ;
    this.maxX = other.maxX;
    this.maxY = other.maxY;
    this.maxZ = other.maxZ;
    return this;
  }

  /**
   * Displaces bounds by a 3D translation offset.
   * 
   * @param {number} dx - Translation along X.
   * @param {number} dy - Translation along Y.
   * @param {number} dz - Translation along Z.
   * @returns {VastuAABB} Reference to this AABB.
   */
  offset(dx, dy, dz) {
    this.minX += dx;
    this.maxX += dx;
    this.minY += dy;
    this.maxY += dy;
    this.minZ += dz;
    this.maxZ += dz;
    return this;
  }

  /**
   * Creates an expanded bounding box enclosing the trajectory of displacement.
   * 
   * @param {number} dx - Displacement along X.
   * @param {number} dy - Displacement along Y.
   * @param {number} dz - Displacement along Z.
   * @returns {VastuAABB} Enclosing bounding box.
   */
  expand(dx, dy, dz) {
    const minX = dx < 0 ? this.minX + dx : this.minX;
    const maxX = dx > 0 ? this.maxX + dx : this.maxX;
    const minY = dy < 0 ? this.minY + dy : this.minY;
    const maxY = dy > 0 ? this.maxY + dy : this.maxY;
    const minZ = dz < 0 ? this.minZ + dz : this.minZ;
    const maxZ = dz > 0 ? this.maxZ + dz : this.maxZ;
    return new VastuAABB(minX, minY, minZ, maxX, maxY, maxZ);
  }

  /**
   * Calculates maximum non-penetrating offset along Y-axis against an obstacle.
   * 
   * @param {VastuAABB} other - Obstacle bounding box.
   * @param {number} dy - Desired displacement along Y.
   * @returns {number} Clamped non-penetrating displacement along Y.
   */
  calculateYOffset(other, dy) {
    // Check overlap in X and Z
    if (this.maxX <= other.minX || this.minX >= other.maxX) return dy;
    if (this.maxZ <= other.minZ || this.minZ >= other.maxZ) return dy;

    if (dy > 0 && other.minY >= this.maxY) {
      const diff = other.minY - this.maxY;
      if (diff < dy) dy = diff;
    } else if (dy < 0 && other.maxY <= this.minY) {
      const diff = other.maxY - this.minY;
      if (diff > dy) dy = diff;
    }
    return dy;
  }

  /**
   * Calculates maximum non-penetrating offset along X-axis against an obstacle.
   * 
   * @param {VastuAABB} other - Obstacle bounding box.
   * @param {number} dx - Desired displacement along X.
   * @returns {number} Clamped non-penetrating displacement along X.
   */
  calculateXOffset(other, dx) {
    // Check overlap in Y and Z
    if (this.maxY <= other.minY || this.minY >= other.maxY) return dx;
    if (this.maxZ <= other.minZ || this.minZ >= other.maxZ) return dx;

    if (dx > 0 && other.minX >= this.maxX) {
      const diff = other.minX - this.maxX;
      if (diff < dx) dx = diff;
    } else if (dx < 0 && other.maxX <= this.minX) {
      const diff = other.maxX - this.minX;
      if (diff > dx) dx = diff;
    }
    return dx;
  }

  /**
   * Calculates maximum non-penetrating offset along Z-axis against an obstacle.
   * 
   * @param {VastuAABB} other - Obstacle bounding box.
   * @param {number} dz - Desired displacement along Z.
   * @returns {number} Clamped non-penetrating displacement along Z.
   */
  calculateZOffset(other, dz) {
    // Check overlap in X and Y
    if (this.maxX <= other.minX || this.minX >= other.maxX) return dz;
    if (this.maxY <= other.minY || this.minY >= other.maxY) return dz;

    if (dz > 0 && other.minZ >= this.maxZ) {
      const diff = other.minZ - this.maxZ;
      if (diff < dz) dz = diff;
    } else if (dz < 0 && other.maxZ <= this.minZ) {
      const diff = other.maxZ - this.minZ;
      if (diff > dz) dz = diff;
    }
    return dz;
  }
}

/**
 * VastuPhysics simulation coordinator implementing swept collision and forces.
 */
export class VastuPhysics {
  /**
   * Initializes physics parameters.
   * 
   * @param {Object} [config={}] - Optional physics configurations.
   */
  constructor(config = {}) {
    this.gravity = config.gravity ?? 26.0; // Snappy Vedic earth gravity (m/s^2)
    this.terminalVelocity = config.terminalVelocity ?? -40.0;
    this.jumpForce = config.jumpForce ?? 8.6; // Clears 1.4m effortlessly
    this.stepHeight = config.stepHeight ?? 0.6; // Auto step-up for 0.5-0.6m stone steps
    this.groundFriction = config.groundFriction ?? 12.0; // Responsive ground damping
    this.airFriction = config.airFriction ?? 1.2; // Fluid air resistance

    /**
     * Static obstacles and architectural plinths in the scene.
     * @type {VastuAABB[]}
     */
    this.staticBoxes = [];

    /**
     * Optional external procedural world provider (e.g., Chunk / Octree manager).
     * @type {Object|null}
     */
    this.worldProvider = null;
  }

  /**
   * Registers a static obstacle bounding box.
   * 
   * @param {VastuAABB} box - Bounding box to add to collision registry.
   */
  addStaticBox(box) {
    this.staticBoxes.push(box);
  }

  /**
   * Removes a static obstacle from collision registry.
   * 
   * @param {VastuAABB} box - Bounding box to remove.
   */
  removeStaticBox(box) {
    const idx = this.staticBoxes.indexOf(box);
    if (idx !== -1) {
      this.staticBoxes.splice(idx, 1);
    }
  }

  /**
   * Clears all registered static obstacles.
   */
  clearStaticBoxes() {
    this.staticBoxes.length = 0;
  }

  /**
   * Attaches a procedural world or octree provider for voxel queries.
   * 
   * @param {{ getIntersectingBoxes: (box: VastuAABB, out: VastuAABB[]) => void }} provider - World provider.
   */
  setWorldProvider(provider) {
    this.worldProvider = provider;
  }

  /**
   * Retrieves all candidate colliders intersecting a broadphase query AABB.
   * 
   * @param {VastuAABB} queryBox - Broadphase query bounding box.
   * @returns {VastuAABB[]} List of intersecting colliders.
   */
  getIntersectingColliders(queryBox) {
    const colliders = [];

    // Query static architectural obstacles
    for (let i = 0; i < this.staticBoxes.length; i++) {
      const b = this.staticBoxes[i];
      if (queryBox.intersects(b)) {
        colliders.push(b);
      }
    }

    // Query procedural world provider if attached
    if (this.worldProvider && typeof this.worldProvider.getIntersectingBoxes === 'function') {
      this.worldProvider.getIntersectingBoxes(queryBox, colliders);
    }

    return colliders;
  }

  /**
   * Advances entity physics by fixed delta time with swept collision resolution.
   * 
   * @param {Object} entity - The moving entity (e.g., YoddhaController).
   * @param {number} delta - Fixed time delta in seconds (1/60s).
   */
  update(entity, delta) {
    if (entity.isFlying) {
      // Vimana (Creative Flight) Mode: Omnidirectional direct displacement with heavy damping
      entity.position.x += entity.velocity.x * delta;
      entity.position.y += entity.velocity.y * delta;
      entity.position.z += entity.velocity.z * delta;

      const flyDamping = Math.exp(-6.0 * delta);
      entity.velocity.x *= flyDamping;
      entity.velocity.y *= flyDamping;
      entity.velocity.z *= flyDamping;
      entity.isGrounded = false;
      entity.didAutoStep = false;
      return;
    }

    // 1. Apply Gravitational Acceleration (Snappy Vedic gravity when airborne, gentle snap when grounded)
    if (!entity.isGrounded) {
      entity.velocity.y -= this.gravity * delta;
      if (entity.velocity.y < this.terminalVelocity) {
        entity.velocity.y = this.terminalVelocity;
      }
    } else {
      entity.velocity.y = -0.1; // Maintain gentle contact without heavy downward penetration
    }


    // Calculate desired displacements for this fixed step
    const targetDx = entity.velocity.x * delta;
    const targetDy = entity.velocity.y * delta;
    const targetDz = entity.velocity.z * delta;

    // Construct entity AABB at current position
    const box = new VastuAABB();
    box.setFromCenterAndSize(
      entity.position.x,
      entity.position.y,
      entity.position.z,
      entity.dimensions.width,
      entity.dimensions.currentHeight,
      entity.dimensions.depth
    );

    // Broadphase query over trajectory extent
    const broadBox = box.expand(targetDx, targetDy, targetDz);
    // Expand upwards slightly to catch obstacles reachable via auto step-up
    if (entity.isGrounded && this.stepHeight > 0) {
      broadBox.maxY += this.stepHeight;
    }
    const colliders = this.getIntersectingColliders(broadBox);

    // 2. Resolve Vertical (Y) Motion First
    let resolvedDy = targetDy;
    for (let i = 0; i < colliders.length; i++) {
      resolvedDy = box.calculateYOffset(colliders[i], resolvedDy);
    }
    box.offset(0, resolvedDy, 0);

    const hitGround = targetDy < 0 && resolvedDy > targetDy;
    const hitCeiling = targetDy > 0 && resolvedDy < targetDy;

    if (hitGround) {
      entity.isGrounded = true;
      entity.velocity.y = 0;
    } else if (hitCeiling) {
      entity.velocity.y = 0;
    }

    // 3. Resolve Horizontal Motion with Auto Step-Up Support
    // Path A: Standard Horizontal Resolution
    const normalBox = box.clone();
    let normalDx = targetDx;
    for (let i = 0; i < colliders.length; i++) {
      normalDx = normalBox.calculateXOffset(colliders[i], normalDx);
    }
    normalBox.offset(normalDx, 0, 0);

    let normalDz = targetDz;
    for (let i = 0; i < colliders.length; i++) {
      normalDz = normalBox.calculateZOffset(colliders[i], normalDz);
    }
    normalBox.offset(0, 0, normalDz);

    const normalDistSq = normalDx * normalDx + normalDz * normalDz;
    const desiredDistSq = targetDx * targetDx + targetDz * targetDz;
    const wasObstructed = normalDistSq < desiredDistSq - 1e-6;

    let useStepUp = false;
    let steppedBox = null;

    // Path B: Step-Up Test (If grounded, obstructed, and stepHeight > 0)
    if (entity.isGrounded && wasObstructed && this.stepHeight > 0) {
      steppedBox = box.clone(); // Box right after vertical movement

      // 3.a. Elevate by stepHeight
      let stepUpDy = this.stepHeight;
      for (let i = 0; i < colliders.length; i++) {
        stepUpDy = steppedBox.calculateYOffset(colliders[i], stepUpDy);
      }
      steppedBox.offset(0, stepUpDy, 0);

      // 3.b. Displace horizontally at elevated height
      let stepDx = targetDx;
      for (let i = 0; i < colliders.length; i++) {
        stepDx = steppedBox.calculateXOffset(colliders[i], stepDx);
      }
      steppedBox.offset(stepDx, 0, 0);

      let stepDz = targetDz;
      for (let i = 0; i < colliders.length; i++) {
        stepDz = steppedBox.calculateZOffset(colliders[i], stepDz);
      }
      steppedBox.offset(0, 0, stepDz);

      // 3.c. Step downward back onto the elevated obstacle surface
      let stepDownDy = -stepUpDy - 0.05;
      for (let i = 0; i < colliders.length; i++) {
        stepDownDy = steppedBox.calculateYOffset(colliders[i], stepDownDy);
      }
      steppedBox.offset(0, stepDownDy, 0);

      const steppedDistSq = stepDx * stepDx + stepDz * stepDz;

      // If stepping up yielded strictly greater horizontal displacement, accept step-up
      if (steppedDistSq > normalDistSq + 1e-6) {
        useStepUp = true;
      }
    }

    if (useStepUp && steppedBox) {
      box.copy(steppedBox);
      entity.isGrounded = true;
      entity.didAutoStep = true;
    } else {
      box.copy(normalBox);
      entity.didAutoStep = false;
      if (normalDx !== targetDx) entity.velocity.x = 0;
      if (normalDz !== targetDz) entity.velocity.z = 0;
    }

    // 4. Update Entity Center and Feet Positions
    entity.position.x = (box.minX + box.maxX) / 2;
    entity.position.y = box.minY; // Feet elevation
    entity.position.z = (box.minZ + box.maxZ) / 2;

    // 5. Downward Ground Probe for Ledge Detection
    if (!hitGround && !useStepUp) {
      const probeBox = box.clone().offset(0, -0.05, 0);
      let foundGround = false;
      for (let i = 0; i < colliders.length; i++) {
        if (probeBox.intersects(colliders[i])) {
          foundGround = true;
          break;
        }
      }
      entity.isGrounded = foundGround && entity.velocity.y <= 0;
    }

    // 6. Apply Ground Friction / Air Drag Momentum Damping
    if (entity.isGrounded) {
      const groundDamping = Math.exp(-this.groundFriction * delta);
      entity.velocity.x *= groundDamping;
      entity.velocity.z *= groundDamping;
    } else {
      const airDamping = Math.exp(-this.airFriction * delta);
      entity.velocity.x *= airDamping;
      entity.velocity.z *= airDamping;
    }

    // 7. Nether/Patala Void Respawn Safety Clamp
    if (entity.position.y < -30.0) {
      entity.position.set(0, 15, 15);
      entity.velocity.set(0, 0, 0);
      entity.isGrounded = false;
    }
  }
}
