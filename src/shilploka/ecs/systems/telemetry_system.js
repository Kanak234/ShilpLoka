/**
 * @fileoverview TelemetrySystem - Real-time ECS & Engine Performance HUD Coordinator
 * @module shilploka/ecs/systems/telemetry_system
 */

import { VastuGrid } from '../../../core/vastu_grid.js';
import { SHILP_ITEMS } from '../../inventory/shilp_inventory.js';

export class TelemetrySystem {
  /**
   * @param {import('../ecs_registry.js').ShilpECS} ecs - ECS registry reference.
   * @param {import('../core/kala_loop.js').KalaLoop} kalaLoop - Dual loop reference.
   * @param {number} playerEntityId - Player entity ID.
   * @param {Object} [engine=null] - Master engine reference.
   */
  constructor(ecs, kalaLoop, playerEntityId, engine = null) {
    this.name = 'TelemetrySystem';
    this.ecs = ecs;
    this.timeWheel = kalaLoop;
    this.playerId = playerEntityId;
    this.engine = engine;

    this.fpsElem = document.getElementById('hud-fps');
    this.physElem = document.getElementById('hud-physics-fps');
    this.coordsElem = document.getElementById('hud-coords');
    this.vastuElem = document.getElementById('hud-vastu');
    this.tradeElem = document.getElementById('hud-trade');
    this.heldElem = document.getElementById('hud-held');
    this.ecsElem = document.getElementById('hud-ecs');
    this.speedElem = document.getElementById('hud-speed');
    this.groundElem = document.getElementById('hud-grounded');
    this.modeElem = document.getElementById('hud-mode');
    this.octreeElem = document.getElementById('hud-octree');
    this.targetElem = document.getElementById('hud-target');
  }


  /**
   * Variable refresh telemetry update.
   */
  updateVariable() {
    const telemetry = this.timeWheel.getTelemetry();

    if (this.fpsElem) this.fpsElem.textContent = `Render FPS: ${telemetry.renderFps}`;
    if (this.physElem) this.physElem.textContent = `Physics: ${telemetry.physicsFps} Hz (Fixed 60Hz)`;

    const transform = this.ecs.getComponent(this.playerId, 'Transform');
    const kinematics = this.ecs.getComponent(this.playerId, 'Kinematics');
    const cameraRig = this.ecs.getComponent(this.playerId, 'CameraRig');
    const input = this.ecs.getComponent(this.playerId, 'PlayerInput');

    if (transform && this.coordsElem) {
      const pos = transform.position;
      this.coordsElem.textContent = `Vastu XYZ: ${pos.x.toFixed(2)} / ${pos.y.toFixed(2)} / ${pos.z.toFixed(2)}`;
    }

    if (cameraRig && this.vastuElem) {
      const facing = VastuGrid.get_facing_vastu_direction(cameraRig.yaw);
      this.vastuElem.textContent = `Facing: ${facing}`;
    }

    if (this.ecsElem) {
      const totalEntities = this.ecs.entities.size;
      let totalComponents = 0;
      for (const table of this.ecs.components.values()) {
        totalComponents += table.size;
      }
      this.ecsElem.textContent = `ECS: ${totalEntities} Entities • ${totalComponents} Components`;
    }

    if (this.engine) {
      if (this.heldElem && this.engine.inventory) {
        const activeSlot = this.engine.inventory.getActiveSlot();
        if (activeSlot) {
          const meta = SHILP_ITEMS[activeSlot.itemId];
          this.heldElem.textContent = `Held: ${meta?.name || activeSlot.itemId} (x${activeSlot.count})`;
        } else {
          this.heldElem.textContent = 'Held: Khali Haath (Empty Hand)';
        }
      }

      if (this.tradeElem && this.engine.world && this.engine.world.graph && transform) {
        const wx = transform.position.x;
        const wz = transform.position.z;
        const graph = this.engine.world.graph;

        let currentCity = null;
        for (const city of graph.cities.values()) {
          if (city.contains(wx, wz)) {
            currentCity = city;
            break;
          }
        }

        if (currentCity) {
          this.tradeElem.textContent = `City: ${currentCity.name} (${currentCity.devanagari})`;
          this.tradeElem.style.color = '#2ecc71';
        } else {
          const roadInfo = graph.getRoadSurfaceInfo(wx, wz);
          if (roadInfo) {
            this.tradeElem.textContent = `Highway: ${roadInfo.edge.name} (${roadInfo.edge.devanagari})`;
            this.tradeElem.style.color = '#f1c40f';
          } else {
            this.tradeElem.textContent = `Route: Harappa-Lothal Trade Basin Corridor`;
            this.tradeElem.style.color = '#deb887';
          }
        }
      }

      if (this.octreeElem && this.engine.cullingMetrics) {
        const m = this.engine.cullingMetrics;
        this.octreeElem.textContent = `Octree Frustum: ${m.visible}/${m.total} Chunks (${m.cullingRatio.toFixed(1)}% Culled)`;
      }

      if (this.targetElem && this.engine.world) {
        const target = this.engine.world.targetVoxel;
        if (target && target.meta) {
          this.targetElem.textContent = `Gaze: ${target.meta.name}`;
          this.targetElem.style.color = target.meta.isHeritage ? '#e74c3c' : '#f1c40f';
        } else {
          this.targetElem.textContent = 'Gaze: Sky / Distant Horizon';
          this.targetElem.style.color = '#deb887';
        }
      }
    }


    if (kinematics) {
      if (this.speedElem) {
        this.speedElem.textContent = `Planar Speed: ${kinematics.speed.toFixed(2)} m/s`;
      }
      if (this.groundElem) {
        this.groundElem.textContent = `Bhoomi (Ground): ${kinematics.isGrounded ? 'Contact (Yes)' : 'Airborne (No)'}`;
        this.groundElem.style.color = kinematics.isGrounded ? '#2ecc71' : '#f39c12';
      }
      if (this.modeElem) {
        if (kinematics.isFlying) {
          this.modeElem.textContent = 'Gati (Mode): Vimana (Flight)';
          this.modeElem.style.color = '#3498db';
        } else if (input && input.isCrouching) {
          this.modeElem.textContent = 'Gati (Mode): Vinamra (Crouch)';
          this.modeElem.style.color = '#deb887';
        } else if (input && input.isSprinting) {
          this.modeElem.textContent = 'Gati (Mode): Vega (Sprint)';
          this.modeElem.style.color = '#e67e22';
        } else {
          this.modeElem.textContent = 'Gati (Mode): Padayatra (Walk)';
          this.modeElem.style.color = '#f7ede2';
        }
      }
    }
  }
}
