/**
 * Tests for VastuGrid: src/core/vastu_grid.js
 */
import { describe, expect, it } from 'vitest';
import {
  VastuDirection,
  VastuGrid,
  VASTU_CHUNK_SIZE_X,
  VASTU_CHUNK_SIZE_Y,
  VASTU_CHUNK_SIZE_Z,
} from '../src/core/vastu_grid.js';

describe('VastuGrid', () => {
  it('defines correct cardinal directions and vectors', () => {
    expect(VastuDirection.PURVA.vector).toEqual([1, 0, 0]);
    expect(VastuDirection.PASHCHIMA.vector).toEqual([-1, 0, 0]);
    expect(VastuDirection.UTTARA.vector).toEqual([0, 0, -1]);
    expect(VastuDirection.DAKSHINA.vector).toEqual([0, 0, 1]);
    expect(VastuDirection.URDHVA.vector).toEqual([0, 1, 0]);
    expect(VastuDirection.ADHAH.vector).toEqual([0, -1, 0]);
  });

  it('transforms world coordinates to chunk coordinates', () => {
    expect(VastuGrid.world_to_chunk(0, 0)).toEqual({ cx: 0, cz: 0 });
    expect(VastuGrid.world_to_chunk(16, 32)).toEqual({ cx: 1, cz: 2 });
    expect(VastuGrid.world_to_chunk(-1, -1)).toEqual({ cx: -1, cz: -1 });
    expect(VastuGrid.world_to_chunk(-16, -16)).toEqual({ cx: -1, cz: -1 });
    expect(VastuGrid.world_to_chunk(-17, -17)).toEqual({ cx: -2, cz: -2 });
  });

  it('transforms world coordinates to local voxel coordinates within chunk bounds', () => {
    expect(VastuGrid.world_to_local_voxel(0, 10, 0)).toEqual({ lx: 0, ly: 10, lz: 0 });
    expect(VastuGrid.world_to_local_voxel(15, 63, 15)).toEqual({ lx: 15, ly: 63, lz: 15 });
    expect(VastuGrid.world_to_local_voxel(16, 0, 16)).toEqual({ lx: 0, ly: 0, lz: 0 });
    expect(VastuGrid.world_to_local_voxel(-1, 5, -1)).toEqual({ lx: 15, ly: 5, lz: 15 });
    expect(VastuGrid.world_to_local_voxel(0, -10, 0)).toEqual({ lx: 0, ly: 0, lz: 0 });
    expect(VastuGrid.world_to_local_voxel(0, 100, 0)).toEqual({ lx: 0, ly: 63, lz: 0 });
  });

  it('computes 1D voxel buffer index correctly', () => {
    const idx0 = VastuGrid.get_voxel_index(0, 0, 0);
    expect(idx0).toBe(0);

    const idxMax = VastuGrid.get_voxel_index(15, 63, 15);
    const expectedMax = 15 + (15 * 16) + (63 * 16 * 16);
    expect(idxMax).toBe(expectedMax);
  });

  it('generates chunk hash keys', () => {
    expect(VastuGrid.get_chunk_key(3, -5)).toBe('3,-5');
    expect(VastuGrid.get_chunk_key(0, 0)).toBe('0,0');
  });

  it('maps yaw angles to facing Vastu directions', () => {
    expect(VastuGrid.get_facing_vastu_direction(0)).toBe(VastuDirection.UTTARA.name);
    expect(VastuGrid.get_facing_vastu_direction(Math.PI / 2)).toBe(VastuDirection.PASHCHIMA.name);
    expect(VastuGrid.get_facing_vastu_direction(Math.PI)).toBe(VastuDirection.DAKSHINA.name);
    expect(VastuGrid.get_facing_vastu_direction((3 * Math.PI) / 2)).toBe(VastuDirection.PURVA.name);
  });

  it('computes 3D Euclidean distance', () => {
    expect(VastuGrid.distance(0, 0, 0, 3, 4, 0)).toBe(5);
    expect(VastuGrid.distance(1, 2, 3, 1, 2, 3)).toBe(0);
    expect(VastuGrid.distance(0, 0, 0, 1, 2, 2)).toBe(3);
  });
});
