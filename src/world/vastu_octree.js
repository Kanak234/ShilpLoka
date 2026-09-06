/**
 * @fileoverview VastuOctree - 3D Hierarchical Spatial Partitioning Engine
 * @module world/vastu_octree
 * 
 * Implements a recursive 8-way spatial tree for:
 * - Sub-millisecond Hierarchical Frustum Culling (eliminating GPU render overhead).
 * - Accelerated Spatial AABB collision queries for physics.
 * - Dynamic node subdivision and bounding volume hierarchy (BVH).
 */

import * as THREE from 'three';

/**
 * Single node in the 3D Vastu Octree hierarchy.
 */
export class VastuOctreeNode {
  /**
   * Constructs an octree node with spatial boundaries.
   * 
   * @param {THREE.Box3} box - Spatial bounds of this octant.
   * @param {number} [depth=0] - Depth level in tree hierarchy.
   * @param {number} [maxDepth=5] - Maximum recursion depth.
   * @param {number} [maxItems=4] - Maximum items in leaf before subdivision.
   */
  constructor(box, depth = 0, maxDepth = 5, maxItems = 4) {
    this.box = box.clone();
    this.depth = depth;
    this.maxDepth = maxDepth;
    this.maxItems = maxItems;

    this.center = new THREE.Vector3();
    this.box.getCenter(this.center);

    this.size = new THREE.Vector3();
    this.box.getSize(this.size);

    /**
     * Array of stored entries in this node: { aabb: THREE.Box3, data: any }
     * @type {Array<{ aabb: THREE.Box3, data: any }>}
     */
    this.items = [];

    /**
     * 8 sub-octant children, or null if leaf.
     * @type {VastuOctreeNode[]|null}
     */
    this.children = null;
  }

  /**
   * Subdivides this node into 8 equal octants along the cardinal center.
   */
  subdivide() {
    this.children = [];
    const min = this.box.min;
    const max = this.box.max;
    const mid = this.center;

    // 8 Sub-Octants:
    // 0: [min.x, mid.x], [min.y, mid.y], [min.z, mid.z]
    // 1: [mid.x, max.x], [min.y, mid.y], [min.z, mid.z]
    // 2: [min.x, mid.x], [mid.y, max.y], [min.z, mid.z]
    // 3: [mid.x, max.x], [mid.y, max.y], [min.z, mid.z]
    // 4: [min.x, mid.x], [min.y, mid.y], [mid.z, max.z]
    // 5: [mid.x, max.x], [min.y, mid.y], [mid.z, max.z]
    // 6: [min.x, mid.x], [mid.y, max.y], [mid.z, max.z]
    // 7: [mid.x, max.x], [mid.y, max.y], [mid.z, max.z]
    for (let i = 0; i < 8; i++) {
      const x0 = (i & 1) ? mid.x : min.x;
      const x1 = (i & 1) ? max.x : mid.x;
      const y0 = (i & 2) ? mid.y : min.y;
      const y1 = (i & 2) ? max.y : mid.y;
      const z0 = (i & 4) ? mid.z : min.z;
      const z1 = (i & 4) ? max.z : mid.z;

      const subBox = new THREE.Box3(
        new THREE.Vector3(x0, y0, z0),
        new THREE.Vector3(x1, y1, z1)
      );
      this.children.push(
        new VastuOctreeNode(subBox, this.depth + 1, this.maxDepth, this.maxItems)
      );
    }
  }

  /**
   * Inserts an item into this node or its children.
   * 
   * @param {{ aabb: THREE.Box3, data: any }} item - Bounded item to insert.
   * @returns {boolean} True if successfully stored.
   */
  insert(item) {
    if (!this.box.intersectsBox(item.aabb)) {
      return false;
    }

    // If leaf node and capacity available, store directly
    if (this.children === null) {
      if (this.items.length < this.maxItems || this.depth >= this.maxDepth) {
        this.items.push(item);
        return true;
      }
      // Capacity exceeded and can subdivide: split into 8 octants
      this.subdivide();

      // Redistribute existing items
      const prevItems = this.items;
      this.items = [];
      for (let i = 0; i < prevItems.length; i++) {
        this._insertIntoChildren(prevItems[i]);
      }
    }

    return this._insertIntoChildren(item);
  }

  /**
   * Inserts item into any intersecting child octants.
   * 
   * @private
   * @param {{ aabb: THREE.Box3, data: any }} item - Bounded item.
   * @returns {boolean}
   */
  _insertIntoChildren(item) {
    let inserted = false;
    for (let i = 0; i < 8; i++) {
      if (this.children[i].insert(item)) {
        inserted = true;
      }
    }
    // If it spans across multiple octants or wasn't absorbed, retain at this branch
    if (!inserted) {
      this.items.push(item);
      inserted = true;
    }
    return inserted;
  }

  /**
   * Hierarchically evaluates camera frustum intersections to cull invisible subtrees.
   * 
   * @param {THREE.Frustum} frustum - Current camera view frustum.
   * @param {Set<any>} visibleSet - Output set of visible item data references.
   * @param {Set<any>} culledSet - Output set of culled item data references.
   */
  queryFrustum(frustum, visibleSet, culledSet) {
    // 1. Hierarchical Branch Culling:
    // If this entire octree volume is outside the frustum, cull EVERYTHING in this branch!
    if (!frustum.intersectsBox(this.box)) {
      this._cullAllSubtree(culledSet);
      return;
    }

    // 2. Test individual items stored at this node level
    for (let i = 0; i < this.items.length; i++) {
      const item = this.items[i];
      if (frustum.intersectsBox(item.aabb)) {
        visibleSet.add(item.data);
      } else {
        culledSet.add(item.data);
      }
    }

    // 3. Recurse down intersecting children
    if (this.children !== null) {
      for (let i = 0; i < 8; i++) {
        this.children[i].queryFrustum(frustum, visibleSet, culledSet);
      }
    }
  }

  /**
   * Recursively culls all items in this branch and its descendants.
   * 
   * @private
   * @param {Set<any>} culledSet - Output set to receive culled references.
   */
  _cullAllSubtree(culledSet) {
    for (let i = 0; i < this.items.length; i++) {
      culledSet.add(this.items[i].data);
    }
    if (this.children !== null) {
      for (let i = 0; i < 8; i++) {
        this.children[i]._cullAllSubtree(culledSet);
      }
    }
  }

  /**
   * Gathers all items intersecting a spatial query AABB.
   * 
   * @param {THREE.Box3} queryBox - Query bounding box.
   * @param {any[]} results - Output array of intersecting item data.
   */
  queryAABB(queryBox, results) {
    if (!this.box.intersectsBox(queryBox)) {
      return;
    }

    for (let i = 0; i < this.items.length; i++) {
      if (this.items[i].aabb.intersectsBox(queryBox)) {
        results.push(this.items[i].data);
      }
    }

    if (this.children !== null) {
      for (let i = 0; i < 8; i++) {
        this.children[i].queryAABB(queryBox, results);
      }
    }
  }
}

/**
 * Root coordinator managing the Vastu Octree spatial hierarchy.
 */
export class VastuOctree {
  /**
   * Initializes the root octree volume.
   * 
   * @param {THREE.Box3} [bounds] - Total world boundaries.
   * @param {number} [maxDepth=4] - Maximum tree depth levels.
   */
  constructor(bounds, maxDepth = 4) {
    const defaultBounds = bounds || new THREE.Box3(
      new THREE.Vector3(-256, -64, -256),
      new THREE.Vector3(256, 128, 256)
    );
    this.bounds = defaultBounds;
    this.maxDepth = maxDepth;
    this.root = new VastuOctreeNode(this.bounds, 0, this.maxDepth, 4);

    /**
     * Map of registered items: key -> { aabb, data }
     * @type {Map<string|number, { aabb: THREE.Box3, data: any }>}
     */
    this.registry = new Map();

    this._visibleSet = new Set();
    this._culledSet = new Set();
  }

  /**
   * Registers or updates an item in the octree.
   * 
   * @param {string|number} id - Unique identifier.
   * @param {THREE.Box3} aabb - 3D Axis-aligned bounding box.
   * @param {any} data - Associated object (e.g. VastuChunk, Mesh).
   */
  insert(id, aabb, data) {
    this.registry.set(id, { aabb: aabb.clone(), data });
    this.root.insert({ aabb, data });
  }

  /**
   * Rebuilds the octree with all registered items to preserve balance.
   */
  rebuild() {
    this.root = new VastuOctreeNode(this.bounds, 0, this.maxDepth, 4);
    for (const item of this.registry.values()) {
      this.root.insert(item);
    }
  }

  /**
   * Removes an item by identifier and rebuilds.
   * 
   * @param {string|number} id - Unique identifier to purge.
   */
  remove(id) {
    if (this.registry.delete(id)) {
      this.rebuild();
    }
  }

  /**
   * Performs frustum culling across all registered items using hierarchical traversal.
   * Automatically toggles `mesh.visible` flag on data objects containing meshes.
   * 
   * @param {THREE.Frustum} frustum - Camera view frustum.
   * @returns {{ visible: number, culled: number, total: number, efficiencyPercent: number }}
   */
  cullFrustum(frustum) {
    this._visibleSet.clear();
    this._culledSet.clear();

    this.root.queryFrustum(frustum, this._visibleSet, this._culledSet);

    // Apply visibility states to data objects
    for (const item of this._visibleSet) {
      if (item && item.mesh) {
        item.mesh.visible = true;
      } else if (item && typeof item.setVisible === 'function') {
        item.setVisible(true);
      }
    }

    for (const item of this._culledSet) {
      // If an item was marked visible via another octant overlap, respect visibility
      if (!this._visibleSet.has(item)) {
        if (item && item.mesh) {
          item.mesh.visible = false;
        } else if (item && typeof item.setVisible === 'function') {
          item.setVisible(false);
        }
      }
    }

    const visibleCount = this._visibleSet.size;
    const culledCount = this._culledSet.size;
    const total = visibleCount + culledCount;
    const efficiency = total > 0 ? (culledCount / total) * 100 : 0;

    return {
      visible: visibleCount,
      culled: culledCount,
      total,
      efficiencyPercent: Math.round(efficiency),
    };
  }

  /**
   * Queries bounding boxes intersecting a given volume.
   * 
   * @param {THREE.Box3} box - Query volume.
   * @returns {any[]} List of intersecting item references.
   */
  queryAABB(box) {
    const results = [];
    this.root.queryAABB(box, results);
    return results;
  }
}
