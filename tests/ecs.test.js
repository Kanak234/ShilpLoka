/**
 * Tests for ShilpECS and Components:
 *   - src/shilploka/ecs/ecs_registry.js
 *   - src/shilploka/ecs/components.js
 */
import * as THREE from 'three';
import { describe, expect, it, vi } from 'vitest';
import { ShilpECS } from '../src/shilploka/ecs/ecs_registry.js';
import {
  BarterNPCComponent,
  CameraRigComponent,
  ComponentMask,
  FaunaComponent,
  HeritageTagComponent,
  KinematicsComponent,
  PlayerInputComponent,
  RenderableComponent,
  TransformComponent,
} from '../src/shilploka/ecs/components.js';

describe('ShilpECS and Components', () => {
  it('creates and destroys entities properly', () => {
    const ecs = new ShilpECS();
    const e1 = ecs.createEntity();
    const e2 = ecs.createEntity();

    expect(ecs.entities.has(e1)).toBe(true);
    expect(ecs.entities.has(e2)).toBe(true);
    expect(ecs.masks.get(e1)).toBe(0);

    const transform = new TransformComponent(1, 2, 3);
    ecs.addComponent(e1, transform);
    expect(ecs.hasComponent(e1, 'Transform')).toBe(true);

    ecs.destroyEntity(e1);
    expect(ecs.entities.has(e1)).toBe(false);
    expect(ecs.hasComponent(e1, 'Transform')).toBe(false);
    expect(ecs.masks.has(e1)).toBe(false);
  });

  it('throws error when adding component to non-existent entity', () => {
    const ecs = new ShilpECS();
    const transform = new TransformComponent();
    expect(() => ecs.addComponent(999, transform)).toThrow(/Cannot add component/);
  });

  it('manages components and bitmask queries accurately', () => {
    const ecs = new ShilpECS();
    const e1 = ecs.createEntity();
    const e2 = ecs.createEntity();

    const t1 = new TransformComponent(0, 0, 0);
    const k1 = new KinematicsComponent();
    const t2 = new TransformComponent(10, 10, 10);

    ecs.addComponent(e1, t1);
    ecs.addComponent(e1, k1);
    ecs.addComponent(e2, t2);

    expect(ecs.getComponent(e1, 'Transform')).toBe(t1);
    expect(ecs.getComponent(e1, 'Kinematics')).toBe(k1);
    expect(ecs.getComponent(e2, 'Kinematics')).toBeNull();
    expect(ecs.getComponent(e2, 'NonExistent')).toBeNull();

    // Query mask for TRANSFORM only -> both e1 and e2
    const transformEntities = ecs.queryMask(ComponentMask.TRANSFORM);
    expect(transformEntities).toContain(e1);
    expect(transformEntities).toContain(e2);

    // Query mask for TRANSFORM and KINEMATICS -> only e1
    const physicsEntities = ecs.queryMask(ComponentMask.TRANSFORM | ComponentMask.KINEMATICS);
    expect(physicsEntities).toEqual([e1]);

    // Remove component
    ecs.removeComponent(e1, 'Kinematics');
    expect(ecs.hasComponent(e1, 'Kinematics')).toBe(false);
    expect(ecs.queryMask(ComponentMask.KINEMATICS)).toEqual([]);
  });

  it('registers and executes systems in updateFixed and updateVariable', () => {
    const ecs = new ShilpECS();
    const e1 = ecs.createEntity();
    ecs.addComponent(e1, new TransformComponent());

    const mockSystem = {
      mask: ComponentMask.TRANSFORM,
      init: vi.fn(),
      updateFixed: vi.fn(),
      updateVariable: vi.fn(),
    };

    ecs.registerSystem(mockSystem);
    expect(mockSystem.init).toHaveBeenCalledWith(ecs);

    ecs.updateFixed(0.016);
    expect(mockSystem.updateFixed).toHaveBeenCalledWith(0.016, [e1], ecs);

    ecs.updateVariable(0.016, 0.5);
    expect(mockSystem.updateVariable).toHaveBeenCalledWith(0.016, 0.5, [e1], ecs);
  });

  it('instantiates all component classes with expected attributes', () => {
    const cam = new THREE.PerspectiveCamera();
    const obj = new THREE.Group();

    const cameraRig = new CameraRigComponent(cam, { yaw: 1.5, pitch: 0.2 });
    expect(cameraRig.name).toBe('CameraRig');
    expect(cameraRig.yaw).toBe(1.5);
    expect(cameraRig.camera).toBe(cam);

    const playerInput = new PlayerInputComponent();
    expect(playerInput.name).toBe('PlayerInput');
    expect(playerInput.moveIntent).toEqual({ x: 0, z: 0 });

    const renderable = new RenderableComponent(obj);
    expect(renderable.name).toBe('Renderable');
    expect(renderable.mesh).toBe(obj);

    const heritage = new HeritageTagComponent('Ashoka Pillar');
    expect(heritage.name).toBe('HeritageTag');
    expect(heritage.indestructible).toBe(true);

    const npc = new BarterNPCComponent('Harappan Merchant', { bronze: 30 });
    expect(npc.name).toBe('BarterNPC');
    expect(npc.inventory.bronzeIngots).toBe(30);

    const fauna = new FaunaComponent('GAJA_ELEPHANT');
    expect(fauna.name).toBe('Fauna');
    expect(fauna.species).toBe('GAJA_ELEPHANT');
  });
});
