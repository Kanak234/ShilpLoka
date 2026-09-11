/**
 * Tests for the trade network graph engine: src/shilploka/world/shilp_graph.js.
 */
import { describe, expect, it } from 'vitest';
import {
  AncientCityNode,
  AncientTradeEdge,
  ShilpGraph,
} from '../src/shilploka/world/shilp_graph.js';
import { ShilpBlockId } from '../src/shilploka/world/voxel_constants.js';

describe('AncientCityNode', () => {
  it('creates node with correct properties and defaults', () => {
    const node = new AncientCityNode({
      id: 'test_city',
      name: 'Test City',
      devanagari: 'परीक्षण नगर',
      x: 10,
      z: 20,
      type: 'CITADEL',
      monument: 'Stupa',
    });

    expect(node.id).toBe('test_city');
    expect(node.name).toBe('Test City');
    expect(node.devanagari).toBe('परीक्षण नगर');
    expect(node.x).toBe(10);
    expect(node.z).toBe(20);
    expect(node.type).toBe('CITADEL');
    expect(node.radius).toBe(20); // Default radius
    expect(node.monument).toBe('Stupa');
  });

  it('calculates Euclidean distance to coordinates', () => {
    const node = new AncientCityNode({
      id: 'c1',
      name: 'C1',
      devanagari: 'नगर १',
      x: 0,
      z: 0,
      type: 'PORT',
      radius: 15,
    });

    expect(node.distanceTo(3, 4)).toBe(5);
    expect(node.distanceTo(0, 0)).toBe(0);
  });

  it('evaluates whether coordinates lie within radius', () => {
    const node = new AncientCityNode({
      id: 'c1',
      name: 'C1',
      devanagari: 'नगर १',
      x: 10,
      z: 10,
      type: 'PORT',
      radius: 5,
    });

    expect(node.contains(10, 14)).toBe(true);
    expect(node.contains(10, 15)).toBe(true);
    expect(node.contains(10, 16)).toBe(false);
  });
});

describe('AncientTradeEdge', () => {
  it('instantiates edge with given parameters', () => {
    const edge = new AncientTradeEdge(
      'harappa',
      'lothal',
      'Saraswati Riverway Canal',
      'सरस्वती जलमार्ग',
      120,
      'WATERWAY'
    );

    expect(edge.from).toBe('harappa');
    expect(edge.to).toBe('lothal');
    expect(edge.name).toBe('Saraswati Riverway Canal');
    expect(edge.devanagari).toBe('सरस्वती जलमार्ग');
    expect(edge.distance).toBe(120);
    expect(edge.routeType).toBe('WATERWAY');
  });
});

describe('ShilpGraph', () => {
  it('initializes default subcontinent trade network', () => {
    const graph = new ShilpGraph();

    expect(graph.cities.has('harappa')).toBe(true);
    expect(graph.cities.has('mohenjo_daro')).toBe(true);
    expect(graph.cities.has('lothal')).toBe(true);
    expect(graph.cities.has('pataliputra')).toBe(true);

    expect(graph.edges.length).toBeGreaterThanOrEqual(4);
    expect(graph.adjacency.get('harappa').length).toBeGreaterThanOrEqual(3);
  });

  it('handles connecting non-existent cities gracefully', () => {
    const graph = new ShilpGraph();
    const countBefore = graph.edges.length;

    graph.connectCities('invalid_a', 'harappa', 'Route', 'मार्ग');
    graph.connectCities('harappa', 'invalid_b', 'Route', 'मार्ग');
    graph.connectCities('invalid_a', 'invalid_b', 'Route', 'मार्ग');

    expect(graph.edges.length).toBe(countBefore);
  });

  it('finds shortest route using Dijkstra algorithm', () => {
    const graph = new ShilpGraph();

    const route = graph.findShortestRoute('mohenjo_daro', 'pataliputra');
    expect(route).not.toBeNull();
    expect(route.path[0]).toBe('mohenjo_daro');
    expect(route.path[route.path.length - 1]).toBe('pataliputra');
    expect(route.distance).toBeGreaterThan(0);
    expect(route.edges.length).toBe(route.path.length - 1);
  });

  it('returns null for shortest route between non-existent cities', () => {
    const graph = new ShilpGraph();

    expect(graph.findShortestRoute('unknown_city', 'harappa')).toBeNull();
    expect(graph.findShortestRoute('harappa', 'unknown_city')).toBeNull();
    expect(graph.findShortestRoute('unknown_city1', 'unknown_city2')).toBeNull();
  });

  it('returns null if destination is unreachable', () => {
    const graph = new ShilpGraph();
    graph.addCity(
      new AncientCityNode({
        id: 'isolated_oasis',
        name: 'Isolated Oasis',
        devanagari: 'एकांत मरुद्यान',
        x: 1000,
        z: 1000,
        type: 'CITADEL',
      })
    );

    const route = graph.findShortestRoute('harappa', 'isolated_oasis');
    expect(route).toBeNull();
  });

  it('evaluates road surface and curb positions along highways', () => {
    const graph = new ShilpGraph();

    // The road between Harappa (0,0) and Mohenjo-Daro (-96, 64) passes near (-48, 32)
    const roadInfo = graph.getRoadSurfaceInfo(-48, 32);
    expect(roadInfo).not.toBeNull();
    expect(roadInfo.isRoad).toBe(true);
    expect(roadInfo.blockId).toBe(ShilpBlockId.HARAPPAN_BAKED_BRICK);

    // Further off the center line should be curb
    const curbInfo = graph.getRoadSurfaceInfo(-48 + 1.8 * (64 / 115.2), 32 + 1.8 * (96 / 115.2));
    if (curbInfo) {
      expect(curbInfo.isRoad).toBe(true);
    }

    // Far off the road should return null
    const wilderness = graph.getRoadSurfaceInfo(500, 500);
    expect(wilderness).toBeNull();
  });

  it('handles degenerate zero-length segments in distance calculation', () => {
    const graph = new ShilpGraph();
    const dist = graph._distanceToSegment(5, 5, 0, 0, 0, 0);
    expect(dist).toBeCloseTo(Math.hypot(5, 5));
  });
});
