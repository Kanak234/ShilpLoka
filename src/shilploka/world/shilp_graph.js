/**
 * @fileoverview ShilpGraph - Ancient Indian Civilization Graph & Trade Network Engine
 * @module shilploka/world/shilp_graph
 * 
 * Features:
 * - Graph Data Structure with adjacency lists and Euclidean path metrics.
 * - Ancient Indian cities: Harappa, Mohenjo-Daro, Lothal, and Pataliputra.
 * - Dijkstra's Shortest Path Algorithm for ancient barter caravans and trade routes.
 * - Procedural road corridor paving for Royal Highways (राजपथ) and Caravan Trails.
 */

import { ShilpBlockId } from './voxel_constants.js';

export class AncientCityNode {
  /**
   * @param {Object} config - Node configuration.
   */
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.devanagari = config.devanagari;
    this.x = config.x;
    this.z = config.z;
    this.type = config.type; // 'CITADEL', 'PORT', 'CAPITAL'
    this.radius = config.radius ?? 20;
    this.monument = config.monument;
  }

  distanceTo(x, z) {
    return Math.hypot(this.x - x, this.z - z);
  }

  contains(x, z) {
    return this.distanceTo(x, z) <= this.radius;
  }
}

export class AncientTradeEdge {
  /**
   * @param {string} from - Source city ID.
   * @param {string} to - Destination city ID.
   * @param {string} name - Route name.
   * @param {string} devanagari - Route name in Hindi/Sanskrit.
   * @param {number} distance - Length in blocks.
   * @param {'HIGHWAY'|'WATERWAY'|'CARAVAN'} routeType - Route archetype.
   */
  constructor(from, to, name, devanagari, distance, routeType = 'HIGHWAY') {
    this.from = from;
    this.to = to;
    this.name = name;
    this.devanagari = devanagari;
    this.distance = distance;
    this.routeType = routeType;
  }
}

export class ShilpGraph {
  constructor() {
    /**
     * Map of city nodes: cityId -> AncientCityNode
     * @type {Map<string, AncientCityNode>}
     */
    this.cities = new Map();

    /**
     * Adjacency list: cityId -> AncientTradeEdge[]
     * @type {Map<string, AncientTradeEdge[]>}
     */
    this.adjacency = new Map();

    /**
     * Edge list.
     * @type {AncientTradeEdge[]}
     */
    this.edges = [];

    this._initializeAncientSubcontinentNetwork();
  }

  _initializeAncientSubcontinentNetwork() {
    // 1. Ancient Cities (Vertices)
    this.addCity(new AncientCityNode({
      id: 'harappa',
      name: 'Harappa Citadel',
      devanagari: 'हड़प्पा दुर्ग',
      x: 0,
      z: 0,
      type: 'CITADEL',
      radius: 24,
      monument: 'Ashoka Sthambha & Granary Mound',
    }));

    this.addCity(new AncientCityNode({
      id: 'mohenjo_daro',
      name: 'Mohenjo-Daro',
      devanagari: 'मोहनजो-दड़ो',
      x: -96,
      z: 64,
      type: 'CITADEL',
      radius: 26,
      monument: 'The Great Bath (महास्नानागार)',
    }));

    this.addCity(new AncientCityNode({
      id: 'lothal',
      name: 'Lothal Tidal Port',
      devanagari: 'लोथल पत्तन',
      x: -48,
      z: -80,
      type: 'PORT',
      radius: 22,
      monument: 'Tidal Basin & Bead Factory',
    }));

    this.addCity(new AncientCityNode({
      id: 'pataliputra',
      name: 'Pataliputra Imperial Capital',
      devanagari: 'पाटलिपुत्र राजधानी',
      x: 96,
      z: -64,
      type: 'CAPITAL',
      radius: 28,
      monument: 'Mauryan 80-Pillared Hall',
    }));

    // 2. Ancient Trade Corridors (Edges)
    this.connectCities('harappa', 'mohenjo_daro', 'Sindhu Royal Highway', 'सिन्धु राजपथ', 'HIGHWAY');
    this.connectCities('harappa', 'lothal', 'Saraswati Riverway Canal', 'सरस्वती जलमार्ग', 'WATERWAY');
    this.connectCities('harappa', 'pataliputra', 'Uttarapatha Grand Highway', 'उत्तरापथ महामार्ग', 'HIGHWAY');
    this.connectCities('mohenjo_daro', 'lothal', 'Thar Desert Caravan Trail', 'मरुस्थल सार्थवाह पथ', 'CARAVAN');
  }

  addCity(city) {
    this.cities.set(city.id, city);
    if (!this.adjacency.has(city.id)) {
      this.adjacency.set(city.id, []);
    }
  }

  connectCities(cityIdA, cityIdB, name, devanagari, routeType = 'HIGHWAY') {
    const cityA = this.cities.get(cityIdA);
    const cityB = this.cities.get(cityIdB);
    if (!cityA || !cityB) return;

    const dist = Math.round(cityA.distanceTo(cityB.x, cityB.z));
    const edgeAB = new AncientTradeEdge(cityIdA, cityIdB, name, devanagari, dist, routeType);
    const edgeBA = new AncientTradeEdge(cityIdB, cityIdA, name, devanagari, dist, routeType);

    this.adjacency.get(cityIdA).push(edgeAB);
    this.adjacency.get(cityIdB).push(edgeBA);
    this.edges.push(edgeAB);
  }

  /**
   * Dijkstra's Shortest Path Algorithm for ancient trade caravans.
   * 
   * @param {string} startCityId - Starting city ID.
   * @param {string} endCityId - Destination city ID.
   * @returns {{ path: string[], distance: number, edges: AncientTradeEdge[] }|null}
   */
  findShortestRoute(startCityId, endCityId) {
    if (!this.cities.has(startCityId) || !this.cities.has(endCityId)) return null;

    const distances = new Map();
    const previous = new Map();
    const prevEdge = new Map();
    const unvisited = new Set(this.cities.keys());

    for (const cityId of this.cities.keys()) {
      distances.set(cityId, Infinity);
    }
    distances.set(startCityId, 0);

    while (unvisited.size > 0) {
      let currentId = null;
      let smallestDist = Infinity;

      for (const cityId of unvisited) {
        const d = distances.get(cityId);
        if (d < smallestDist) {
          smallestDist = d;
          currentId = cityId;
        }
      }

      if (currentId === null || smallestDist === Infinity) break;
      if (currentId === endCityId) break;

      unvisited.delete(currentId);

      const neighbors = this.adjacency.get(currentId) || [];
      for (const edge of neighbors) {
        if (!unvisited.has(edge.to)) continue;

        const alt = distances.get(currentId) + edge.distance;
        if (alt < distances.get(edge.to)) {
          distances.set(edge.to, alt);
          previous.set(edge.to, currentId);
          prevEdge.set(edge.to, edge);
        }
      }
    }

    if (distances.get(endCityId) === Infinity) return null;

    // Reconstruct path
    const path = [];
    const edges = [];
    let curr = endCityId;
    while (curr) {
      path.unshift(curr);
      const e = prevEdge.get(curr);
      if (e) edges.unshift(e);
      curr = previous.get(curr);
    }

    return {
      path,
      distance: distances.get(endCityId),
      edges,
    };
  }

  /**
   * Evaluates whether world coordinates (wx, wz) lie on a paved trade route.
   * 
   * @param {number} wx - World X coordinate.
   * @param {number} wz - World Z coordinate.
   * @returns {{ isRoad: boolean, isCurb: boolean, edge: AncientTradeEdge, blockId: number }|null}
   */
  getRoadSurfaceInfo(wx, wz) {
    const roadHalfWidth = 1.6;
    const curbWidth = 2.4;

    for (let i = 0; i < this.edges.length; i++) {
      const edge = this.edges[i];
      const cityA = this.cities.get(edge.from);
      const cityB = this.cities.get(edge.to);
      if (!cityA || !cityB) continue;

      const dist = this._distanceToSegment(wx, wz, cityA.x, cityA.z, cityB.x, cityB.z);

      if (dist <= roadHalfWidth) {
        return {
          isRoad: true,
          isCurb: false,
          edge,
          blockId: ShilpBlockId.HARAPPAN_BAKED_BRICK,
        };
      } else if (dist <= curbWidth) {
        return {
          isRoad: true,
          isCurb: true,
          edge,
          blockId: ShilpBlockId.CHUNAR_SANDSTONE,
        };
      }
    }

    return null;
  }

  _distanceToSegment(px, pz, ax, az, bx, bz) {
    const dx = bx - ax;
    const dz = bz - az;
    const lenSq = dx * dx + dz * dz;
    if (lenSq === 0) return Math.hypot(px - ax, pz - az);

    let t = ((px - ax) * dx + (pz - az) * dz) / lenSq;
    t = Math.max(0, Math.min(1, t));

    const projX = ax + t * dx;
    const projZ = az + t * dz;
    return Math.hypot(px - projX, pz - projZ);
  }
}
