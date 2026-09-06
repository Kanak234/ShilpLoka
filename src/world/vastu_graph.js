/**
 * @fileoverview VastuGraph - Ancient Indian Trade Network & City Graph Engine
 * @module world/vastu_graph
 * 
 * Features:
 * - Graph Data Structure with adjacency lists and Euclidean path metrics.
 * - Ancient Indian cities: Mohenjo-Daro, Harappa, Lothal, and Pataliputra.
 * - Dijkstra's Shortest Path Algorithm for ancient trade caravan navigation.
 * - Organic trade route waypoint generation following terrain contours.
 * - Procedural road surface query for automatic paving of Royal Highways and River Channels.
 */

import { VastuBlockId } from './ancient_blocks.js';

/**
 * Node representing an Ancient Indian city or settlement in the graph.
 */
export class AncientCityNode {
  /**
   * Constructs an ancient city node.
   * 
   * @param {Object} config - City configuration.
   * @param {string} config.id - Unique identifier (e.g. 'harappa').
   * @param {string} config.name - English display name.
   * @param {string} config.devanagari - Hindi/Sanskrit name.
   * @param {number} config.x - World X coordinate.
   * @param {number} config.z - World Z coordinate.
   * @param {string} config.type - City archetype ('CITADEL', 'PORT', 'CAPITAL').
   * @param {number} [config.radius=20] - Perimeter radius in blocks.
   * @param {string} config.monument - Primary heritage monument.
   */
  constructor(config) {
    this.id = config.id;
    this.name = config.name;
    this.devanagari = config.devanagari;
    this.x = config.x;
    this.z = config.z;
    this.type = config.type;
    this.radius = config.radius ?? 20;
    this.monument = config.monument;
  }

  /**
   * Calculates planar distance from this city to world coordinates (x, z).
   * 
   * @param {number} x - Target X.
   * @param {number} z - Target Z.
   * @returns {number} Distance in blocks.
   */
  distanceTo(x, z) {
    const dx = this.x - x;
    const dz = this.z - z;
    return Math.hypot(dx, dz);
  }

  /**
   * Checks if coordinates lie within the city perimeter.
   * 
   * @param {number} x - Target X.
   * @param {number} z - Target Z.
   * @returns {boolean}
   */
  contains(x, z) {
    return this.distanceTo(x, z) <= this.radius;
  }
}

/**
 * Graph edge representing an Ancient trade route or river canal connecting two cities.
 */
export class TradeRouteEdge {
  /**
   * Constructs a trade route edge.
   * 
   * @param {Object} config - Route configuration.
   * @param {string} config.sourceId - Origin city ID.
   * @param {string} config.targetId - Destination city ID.
   * @param {string} config.routeType - 'ROYAL_HIGHWAY', 'CARAVAN_TRAIL', or 'RIVER_CANAL'.
   * @param {Array<{x: number, z: number}>} config.waypoints - Discrete path points.
   * @param {number} config.width - Road width in blocks.
   * @param {number} config.roadBlockId - VastuBlockId for paving.
   * @param {number} config.distance - Cumulative route distance in meters.
   */
  constructor(config) {
    this.sourceId = config.sourceId;
    this.targetId = config.targetId;
    this.routeType = config.routeType;
    this.waypoints = config.waypoints;
    this.width = config.width;
    this.roadBlockId = config.roadBlockId;
    this.distance = config.distance;
  }

  /**
   * Checks if world coordinates (wx, wz) lie within this trade route corridor.
   * 
   * @param {number} wx - World X.
   * @param {number} wz - World Z.
   * @returns {boolean} True if within road footprint.
   */
  intersectsPoint(wx, wz) {
    const halfWidth = this.width / 2;
    const halfWidthSq = halfWidth * halfWidth;

    for (let i = 0; i < this.waypoints.length - 1; i++) {
      const p1 = this.waypoints[i];
      const p2 = this.waypoints[i + 1];

      // Distance from point (wx, wz) to line segment p1-p2
      const dx = p2.x - p1.x;
      const dz = p2.z - p1.z;
      const lengthSq = dx * dx + dz * dz;
      if (lengthSq === 0) continue;

      let t = ((wx - p1.x) * dx + (wz - p1.z) * dz) / lengthSq;
      t = Math.max(0, Math.min(1, t));

      const projX = p1.x + t * dx;
      const projZ = p1.z + t * dz;

      const distSq = (wx - projX) * (wx - projX) + (wz - projZ) * (wz - projZ);
      if (distSq <= halfWidthSq) {
        return true;
      }
    }
    return false;
  }
}

/**
 * Master Graph Data Structure coordinating the Ancient Indian Subcontinent network.
 */
export class VastuGraph {
  /**
   * Initializes graph storage and populates archetypal ancient cities.
   */
  constructor() {
    /**
     * Map of city ID to AncientCityNode.
     * @type {Map<string, AncientCityNode>}
     */
    this.cities = new Map();

    /**
     * Adjacency list: cityId -> Array<{ targetId: string, edge: TradeRouteEdge }>
     * @type {Map<string, Array<{ targetId: string, edge: TradeRouteEdge }>>}
     */
    this.adjacency = new Map();

    /**
     * Complete list of all trade route edges.
     * @type {TradeRouteEdge[]}
     */
    this.edges = [];

    // Initialize the Four Archetypal Ancient Indian Cities
    this._initAncientCities();
    this._initTradeRoutes();
  }

  /**
   * Registers Ancient Indian civilization centers.
   * @private
   */
  _initAncientCities() {
    const cities = [
      new AncientCityNode({
        id: 'harappa',
        name: 'Harappa',
        devanagari: 'हड़प्पा',
        x: 0,
        z: 0,
        type: 'CITADEL',
        radius: 24,
        monument: 'The Great Granary & Citadel Mound (अन्न-भण्डार)',
      }),
      new AncientCityNode({
        id: 'mohenjo_daro',
        name: 'Mohenjo-Daro',
        devanagari: 'मोहनजोदड़ो',
        x: -96,
        z: 64,
        type: 'CITADEL',
        radius: 28,
        monument: 'The Great Bath (विशाल स्नानागार)',
      }),
      new AncientCityNode({
        id: 'lothal',
        name: 'Lothal',
        devanagari: 'लोथल',
        x: -48,
        z: -80,
        type: 'PORT',
        radius: 20,
        monument: 'World First Tidal Dockyard (जहाजी गोदी)',
      }),
      new AncientCityNode({
        id: 'pataliputra',
        name: 'Pataliputra',
        devanagari: 'पाटलिपुत्र',
        x: 96,
        z: -64,
        type: 'CAPITAL',
        radius: 30,
        monument: 'Eighty-Pillared Mauryan Hall (अशोक स्तम्भ मण्डप)',
      }),
    ];

    for (const city of cities) {
      this.addCity(city);
    }
  }

  /**
   * Connects ancient cities with curved procedural trade routes.
   * @private
   */
  _initTradeRoutes() {
    // 1. Royal Highway: Harappa <--> Mohenjo-Daro along Sindhu
    this.connectCities('harappa', 'mohenjo_daro', {
      routeType: 'ROYAL_HIGHWAY',
      width: 4.0,
      roadBlockId: VastuBlockId.HARAPPAN_BRICK,
      segments: 12,
      sinCurvature: 14.0,
    });

    // 2. Maritime Caravan Trail: Harappa <--> Lothal
    this.connectCities('harappa', 'lothal', {
      routeType: 'CARAVAN_TRAIL',
      width: 3.5,
      roadBlockId: VastuBlockId.TERRACOTTA,
      segments: 10,
      sinCurvature: -12.0,
    });

    // 3. Imperial East-West Grand Trunk Highway: Harappa <--> Pataliputra
    this.connectCities('harappa', 'pataliputra', {
      routeType: 'ROYAL_HIGHWAY',
      width: 4.5,
      roadBlockId: VastuBlockId.CHUNAR_SANDSTONE,
      segments: 14,
      sinCurvature: 18.0,
    });

    // 4. Southern River Trade Route: Lothal <--> Mohenjo-Daro
    this.connectCities('lothal', 'mohenjo_daro', {
      routeType: 'CARAVAN_TRAIL',
      width: 3.0,
      roadBlockId: VastuBlockId.RIVER_SAND,
      segments: 8,
      sinCurvature: 8.0,
    });
  }

  /**
   * Adds an ancient city node to the graph.
   * 
   * @param {AncientCityNode} node - City node.
   */
  addCity(node) {
    this.cities.set(node.id, node);
    if (!this.adjacency.has(node.id)) {
      this.adjacency.set(node.id, []);
    }
  }

  /**
   * Connects two cities with an organic curved trade route edge.
   * 
   * @param {string} id1 - Origin city ID.
   * @param {string} id2 - Destination city ID.
   * @param {Object} options - Edge options.
   */
  connectCities(id1, id2, options = {}) {
    const city1 = this.cities.get(id1);
    const city2 = this.cities.get(id2);
    if (!city1 || !city2) return;

    const segments = options.segments ?? 10;
    const curvature = options.sinCurvature ?? 10.0;
    const waypoints = [];

    // Generate organic curved path with harmonic sine offsets
    let cumulativeDist = 0;
    for (let i = 0; i <= segments; i++) {
      const t = i / segments;
      // Linear interpolation
      const lx = city1.x + t * (city2.x - city1.x);
      const lz = city1.z + t * (city2.z - city1.z);

      // Perpendicular normal offset for organic curve
      const dx = city2.x - city1.x;
      const dz = city2.z - city1.z;
      const normalX = -dz / Math.hypot(dx, dz);
      const normalZ = dx / Math.hypot(dx, dz);

      // Arc curvature (sin(pi * t) peaks at midpoint t=0.5)
      const arc = Math.sin(Math.PI * t) * curvature;

      const wx = lx + normalX * arc;
      const wz = lz + normalZ * arc;
      waypoints.push({ x: wx, z: wz });

      if (i > 0) {
        const prev = waypoints[i - 1];
        cumulativeDist += Math.hypot(wx - prev.x, wz - prev.z);
      }
    }

    const edge = new TradeRouteEdge({
      sourceId: id1,
      targetId: id2,
      routeType: options.routeType ?? 'ROYAL_HIGHWAY',
      waypoints,
      width: options.width ?? 3.5,
      roadBlockId: options.roadBlockId ?? VastuBlockId.HARAPPAN_BRICK,
      distance: cumulativeDist,
    });

    this.edges.push(edge);
    this.adjacency.get(id1).push({ targetId: id2, edge });
    this.adjacency.get(id2).push({ targetId: id1, edge });
  }

  /**
   * Computes the shortest trade route between two cities using Dijkstra's Algorithm.
   * 
   * @param {string} startCityId - Origin city ID.
   * @param {string} endCityId - Destination city ID.
   * @returns {{ path: string[], distance: number, edges: TradeRouteEdge[] }|null}
   */
  findShortestRoute(startCityId, endCityId) {
    if (!this.cities.has(startCityId) || !this.cities.has(endCityId)) {
      return null;
    }

    const distances = new Map();
    const previous = new Map();
    const visited = new Set();
    const queue = [];

    for (const cityId of this.cities.keys()) {
      distances.set(cityId, Infinity);
      previous.set(cityId, null);
    }
    distances.set(startCityId, 0);
    queue.push({ id: startCityId, dist: 0 });

    while (queue.length > 0) {
      // Extract node with minimum distance
      queue.sort((a, b) => a.dist - b.dist);
      const { id: currentId } = queue.shift();

      if (visited.has(currentId)) continue;
      visited.add(currentId);

      if (currentId === endCityId) break;

      const neighbors = this.adjacency.get(currentId) || [];
      for (const { targetId, edge } of neighbors) {
        if (visited.has(targetId)) continue;

        const alt = distances.get(currentId) + edge.distance;
        if (alt < distances.get(targetId)) {
          distances.set(targetId, alt);
          previous.set(targetId, { cityId: currentId, edge });
          queue.push({ id: targetId, dist: alt });
        }
      }
    }

    if (distances.get(endCityId) === Infinity) {
      return null;
    }

    // Backtrack path
    const path = [];
    const routeEdges = [];
    let curr = endCityId;

    while (curr !== null) {
      path.unshift(curr);
      const prev = previous.get(curr);
      if (prev) {
        routeEdges.unshift(prev.edge);
        curr = prev.cityId;
      } else {
        curr = null;
      }
    }

    return {
      path,
      distance: Math.round(distances.get(endCityId)),
      edges: routeEdges,
    };
  }

  /**
   * Spatial query determining if world coordinates (wx, wz) lie on any ancient trade route.
   * 
   * @param {number} wx - World X coordinate.
   * @param {number} wz - World Z coordinate.
   * @returns {{ isRoad: boolean, blockId: number, routeType: string }|null}
   */
  getRoadAt(wx, wz) {
    for (let i = 0; i < this.edges.length; i++) {
      const edge = this.edges[i];
      if (edge.intersectsPoint(wx, wz)) {
        return {
          isRoad: true,
          blockId: edge.roadBlockId,
          routeType: edge.routeType,
        };
      }
    }
    return null;
  }

  /**
   * Spatial query determining if world coordinates (wx, wz) fall within an ancient city.
   * 
   * @param {number} wx - World X coordinate.
   * @param {number} wz - World Z coordinate.
   * @returns {AncientCityNode|null} City node reference, or null.
   */
  getCityAt(wx, wz) {
    for (const city of this.cities.values()) {
      if (city.contains(wx, wz)) {
        return city;
      }
    }
    return null;
  }
}
