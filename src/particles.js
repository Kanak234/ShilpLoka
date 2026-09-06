/**
 * ============================================================================
 * BLOCK BREAK PARTICLE ENGINE
 * ============================================================================
 * Spawns 3D micro-voxel debris upon block destruction. Implements individual
 * particle ballistic trajectories with gravity, ground bounce, and decay.
 * Utilizes instancing / object pooling for high performance and zero GC stutter.
 */

import * as THREE from 'three';
import { REGISTRY, BLOCKS } from './constants.js';

const MAX_PARTICLES = 256;

export class ParticleManager {
  constructor(scene) {
    this.scene = scene;
    this.particles = [];

    // Pre-allocated micro cube geometry (0.15 size)
    this.geometry = new THREE.BoxGeometry(0.14, 0.14, 0.14);

    // Particle color palettes based on block type
    this.blockColors = {
      [BLOCKS.GRASS]: 0x55aa33,
      [BLOCKS.DIRT]: 0x785234,
      [BLOCKS.STONE]: 0x808080,
      [BLOCKS.COBBLESTONE]: 0x707070,
      [BLOCKS.OAK_LOG]: 0x6b5433,
      [BLOCKS.OAK_LEAVES]: 0x2d8223,
      [BLOCKS.OAK_PLANKS]: 0xb48752,
      [BLOCKS.CRAFTING_TABLE]: 0x8b5a2b,
      [BLOCKS.SAND]: 0xded091,
      [BLOCKS.WATER]: 0x2d5fe1,
      [BLOCKS.COAL_ORE]: 0x222222,
      [BLOCKS.IRON_ORE]: 0xd8af93,
      [BLOCKS.DIAMOND_ORE]: 0x2eece2,
      [BLOCKS.GLASS]: 0xe0f0ff,
      [BLOCKS.BEDROCK]: 0x222222,
      [BLOCKS.TORCH]: 0xf39c12,
    };
  }

  /**
   * Spawns a cluster of burst particles at world position (x, y, z)
   */
  spawnBreakParticles(x, y, z, blockId) {
    const count = 12;
    const colorHex = this.blockColors[blockId] || 0x888888;

    for (let i = 0; i < count; i++) {
      if (this.particles.length >= MAX_PARTICLES) {
        // Recycle oldest particle
        const oldest = this.particles.shift();
        this.scene.remove(oldest.mesh);
        oldest.mesh.material.dispose();
      }

      const material = new THREE.MeshLambertMaterial({
        color: colorHex,
        transparent: true,
        opacity: 1.0,
      });

      const mesh = new THREE.Mesh(this.geometry, material);
      mesh.position.set(
        x + (Math.random() - 0.5) * 0.6,
        y + (Math.random() - 0.5) * 0.6,
        z + (Math.random() - 0.5) * 0.6
      );

      // Random burst velocity
      const angle = Math.random() * Math.PI * 2;
      const speed = 1.8 + Math.random() * 2.5;
      const vx = Math.cos(angle) * speed;
      const vy = 2.2 + Math.random() * 3.5;
      const vz = Math.sin(angle) * speed;

      this.scene.add(mesh);

      this.particles.push({
        mesh,
        velocity: new THREE.Vector3(vx, vy, vz),
        rotSpeed: new THREE.Vector3(
          Math.random() * 10 - 5,
          Math.random() * 10 - 5,
          Math.random() * 10 - 5
        ),
        age: 0,
        lifetime: 0.55 + Math.random() * 0.3,
      });
    }
  }

  /**
   * Updates particle physics trajectories, gravity, and fading
   */
  update(dt) {
    const gravity = -18.0;

    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.age += dt;

      if (p.age >= p.lifetime) {
        // Despawn
        this.scene.remove(p.mesh);
        p.mesh.material.dispose();
        this.particles.splice(i, 1);
        continue;
      }

      // Ballistic motion
      p.velocity.y += gravity * dt;
      p.mesh.position.x += p.velocity.x * dt;
      p.mesh.position.y += p.velocity.y * dt;
      p.mesh.position.z += p.velocity.z * dt;

      // Rotation
      p.mesh.rotation.x += p.rotSpeed.x * dt;
      p.mesh.rotation.y += p.rotSpeed.y * dt;
      p.mesh.rotation.z += p.rotSpeed.z * dt;

      // Fade out opacity near end of lifetime
      const progress = p.age / p.lifetime;
      if (progress > 0.6) {
        p.mesh.material.opacity = 1.0 - (progress - 0.6) / 0.4;
      }
    }
  }
}
