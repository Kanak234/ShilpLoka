/**
 * @fileoverview ShilpOctree - 3D Hierarchical Spatial Partitioning for Zero-Lag Chunk Culling
 * @module shilploka/world/shilp_octree
 * 
 * Divides 3D world space recursively into 8 octants.
 * Executes hierarchical view frustum culling in O(log N) time, eliminating GPU draw
 * calls for chunks outside the player's field of view and preventing GPU fill-rate choke.
 */

import * as THREE from 'three';

export class ShilpOctreeNode {
  /**
   * @param {THREE.Box3} box - Spatial bounding volume of this node.
   * @param {number} [depth=0] - Recursion depth.
   * @param {number} [maxDepth=4] - Max hierarchy depth.
   * @param {number} [maxItems=4] - Max items before subdivision.
   */
  constructor(box, depth = 0, maxDepth = 4, maxItems = 4) {
    this.box = box.clone();
    this.depth = depth;
    this.maxDepth = maxDepth;
    this.maxItems = maxItems;

    this.center = new THREE.Vector3();
    this.box.getCenter(this.center);

    /**
     * Stored elements: Array of { aabb: THREE.Box3, chunk: Object }
     * @type {Array<{ aabb: THREE.Box3, chunk: Object }>}
     */
    this.items = [];

    /**
     * 8 sub-octant children, or null if leaf.
     * @type {ShilpOctreeNode[]|null}
     */
    this.children = null;
  }

  /**
   * Subdivides this node into 8 equal child octants.
   */
  subdivide() {
    this.children = [];
    const min = this.box.min;
    const max = this.box.max;
    const mid = this.center;

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
      this.children.push(new ShilpOctreeNode(subBox, this.depth + 1, this.maxDepth, this.maxItems));
    }
  }

  /**
   * Inserts a chunk entry into the hierarchy.
   * 
   * @param {{ aabb: THREE.Box3, chunk: Object }} entry - Chunk spatial entry.
   * @returns {boolean} True if inserted into this branch.
   */
  insert(entry) {
    if (!this.box.intersectsBox(entry.aabb)) {
      return false;
    }

    if (this.children !== null) {
      let placedInChild = false;
      for (let i = 0; i < 8; i++) {
        if (this.children[i].insert(entry)) {
          placedInChild = true;
          break;
        }
      }
      return placedInChild;
    }

    this.items.push(entry);

    if (this.items.length > this.maxItems && this.depth < this.maxDepth) {
      this.subdivide();
      const remainingItems = [];
      for (const item of this.items) {
        let placed = false;
        for (let i = 0; i < 8; i++) {
          if (this.children[i].insert(item)) {
            placed = true;
            break;
          }
        }
        if (!placed) {
          remainingItems.push(item);
        }
      }
      this.items = remainingItems;
    }

    return true;
  }

  /**
   * Recursively culls chunks against camera frustum.
   * 
   * @param {THREE.Frustum} frustum - Camera view frustum.
   * @param {Set<Object>} visibleSet - Output set of visible chunks.
   * @param {Set<Object>} culledSet - Output set of culled chunks.
   */
  queryFrustum(frustum, visibleSet, culledSet) {
    // 1. Hierarchical Bounding Box Check:
    // If the entire octant node does NOT intersect the camera frustum,
    // cull all chunks in this subtree in O(1)!
    if (!frustum.intersectsBox(this.box)) {
      this._cullSubtree(culledSet);
      return;
    }

    // 2. Leaf node items check
    for (let i = 0; i < this.items.length; i++) {
      const item = this.items[i];
      if (frustum.intersectsBox(item.aabb)) {
        visibleSet.add(item.chunk);
      } else {
        culledSet.add(item.chunk);
      }
    }

    // 3. Recurse into children
    if (this.children !== null) {
      for (let i = 0; i < 8; i++) {
        this.children[i].queryFrustum(frustum, visibleSet, culledSet);
      }
    }
  }

  _cullSubtree(culledSet) {
    for (let i = 0; i < this.items.length; i++) {
      culledSet.add(this.items[i].chunk);
    }
    if (this.children !== null) {
      for (let i = 0; i < 8; i++) {
        this.children[i]._cullSubtree(culledSet);
      }
    }
  }
}

/**
 * Master ShilpOctree Spatial Partitioning Manager.
 */
export class ShilpOctree {
  constructor(boundsRadius = 256) {
    this.boundsRadius = boundsRadius;
    const rootBox = new THREE.Box3(
      new THREE.Vector3(-boundsRadius, 0, -boundsRadius),
      new THREE.Vector3(boundsRadius, 128, boundsRadius)
    );
    this.root = new ShilpOctreeNode(rootBox, 0, 4, 4);

    this.allChunks = new Set();
    this.visibleChunks = new Set();
    this.culledChunks = new Set();
  }

  /**
   * Registers a chunk and its spatial bounding box into the octree.
   * 
   * @param {Object} chunk - Chunk object with .aabb property.
   */
  registerChunk(chunk) {
    if (!chunk.aabb) return;
    this.allChunks.add(chunk);
    this.root.insert({ aabb: chunk.aabb, chunk });
  }

  /**
   * Clears and rebuilds the octree with a list of chunks.
   * 
   * @param {Iterable<Object>} chunks - Active chunk collection.
   */
  rebuild(chunks) {
    const rootBox = new THREE.Box3(
      new THREE.Vector3(-this.boundsRadius, 0, -this.boundsRadius),
      new THREE.Vector3(this.boundsRadius, 128, this.boundsRadius)
    );
    this.root = new ShilpOctreeNode(rootBox, 0, 4, 4);
    this.allChunks.clear();

    for (const chunk of chunks) {
      this.registerChunk(chunk);
    }
  }

  /**
   * Executes frustum culling and toggles chunk mesh visibility.
   * 
   * @param {THREE.Frustum} frustum - Current camera view frustum.
   * @returns {{ total: number, visible: number, culled: number, cullingRatio: number }} Culling metrics.
   */
  updateFrustumCulling(frustum) {
    this.visibleChunks.clear();
    this.culledChunks.clear();

    this.root.queryFrustum(frustum, this.visibleChunks, this.culledChunks);

    // Apply visibility to chunks
    for (const chunk of this.visibleChunks) {
      if (chunk.mesh) chunk.mesh.visible = true;
    }
    for (const chunk of this.culledChunks) {
      if (chunk.mesh) chunk.mesh.visible = false;
    }

    const total = this.allChunks.size;
    const visible = this.visibleChunks.size;
    const culled = this.culledChunks.size;
    const cullingRatio = total > 0 ? (culled / total) * 100 : 0;

    return { total, visible, culled, cullingRatio };
  }
}
