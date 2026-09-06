/**
 * @fileoverview ShilpECS - Production Entity Component System Registry
 * @module shilploka/ecs/ecs_registry
 * 
 * Provides an ultra-scalable, cache-friendly, bitmask-filtered ECS architecture
 * designed to support long-term modular feature addition without touching core engine files.
 */

/**
 * High-performance Entity Component System coordinator.
 */
export class ShilpECS {
  constructor() {
    this._nextEntityId = 1;

    /**
     * Set of currently active entity IDs.
     * @type {Set<number>}
     */
    this.entities = new Set();

    /**
     * Map of entity ID to bitmask for O(1) archetype queries.
     * @type {Map<number, number>}
     */
    this.masks = new Map();

    /**
     * Component tables: componentName -> Map<entityId, ComponentInstance>
     * @type {Map<string, Map<number, Object>>}
     */
    this.components = new Map();

    /**
     * Ordered list of registered systems.
     * @type {Array<Object>}
     */
    this.systems = [];
  }

  /**
   * Spawns a new entity and returns its unique numeric ID.
   * 
   * @returns {number} Unique entity ID.
   */
  createEntity() {
    const id = this._nextEntityId++;
    this.entities.add(id);
    this.masks.set(id, 0);
    return id;
  }

  /**
   * Destroys an entity and cleans up all associated components.
   * 
   * @param {number} entityId - Entity ID to purge.
   */
  destroyEntity(entityId) {
    if (!this.entities.has(entityId)) return;

    for (const table of this.components.values()) {
      table.delete(entityId);
    }
    this.masks.delete(entityId);
    this.entities.delete(entityId);
  }

  /**
   * Attaches a component instance to an entity.
   * 
   * @param {number} entityId - Target entity ID.
   * @param {Object} component - Component instance.
   * @returns {ShilpECS} Fluent reference for chaining.
   */
  addComponent(entityId, component) {
    if (!this.entities.has(entityId)) {
      throw new Error(`ShilpECS: Cannot add component '${component.name}' to non-existent entity #${entityId}`);
    }

    if (!this.components.has(component.name)) {
      this.components.set(component.name, new Map());
    }

    this.components.get(component.name).set(entityId, component);

    // Update bitmask for archetype filtering
    if (component.mask !== undefined) {
      const currentMask = this.masks.get(entityId) || 0;
      this.masks.set(entityId, currentMask | component.mask);
    }

    return this;
  }

  /**
   * Retrieves a component instance by name for a given entity.
   * 
   * @param {number} entityId - Entity ID.
   * @param {string} componentName - Name of the component.
   * @returns {Object|null} Component instance or null.
   */
  getComponent(entityId, componentName) {
    const table = this.components.get(componentName);
    if (!table) return null;
    return table.get(entityId) || null;
  }

  /**
   * Checks if an entity possesses a given component.
   * 
   * @param {number} entityId - Entity ID.
   * @param {string} componentName - Component name.
   * @returns {boolean}
   */
  hasComponent(entityId, componentName) {
    const table = this.components.get(componentName);
    return table ? table.has(entityId) : false;
  }

  /**
   * Removes a component from an entity.
   * 
   * @param {number} entityId - Entity ID.
   * @param {string} componentName - Component name.
   */
  removeComponent(entityId, componentName) {
    const table = this.components.get(componentName);
    if (table && table.has(entityId)) {
      const comp = table.get(entityId);
      if (comp.mask !== undefined) {
        const currentMask = this.masks.get(entityId) || 0;
        this.masks.set(entityId, currentMask & ~comp.mask);
      }
      table.delete(entityId);
    }
  }

  /**
   * Queries all entities matching the required bitmask signature.
   * 
   * @param {number} requiredMask - Bitwise OR of required ComponentMask values.
   * @returns {number[]} Array of matching entity IDs.
   */
  queryMask(requiredMask) {
    const results = [];
    for (const [entityId, mask] of this.masks.entries()) {
      if ((mask & requiredMask) === requiredMask) {
        results.push(entityId);
      }
    }
    return results;
  }

  /**
   * Registers an execution system into the ECS pipeline.
   * 
   * @param {Object} system - System implementing updateFixed and/or updateVariable.
   */
  registerSystem(system) {
    if (typeof system.init === 'function') {
      system.init(this);
    }
    this.systems.push(system);
  }

  /**
   * Advances all registered systems during the fixed 60Hz physics process.
   * 
   * @param {number} delta - Fixed time delta in seconds (1/60s).
   */
  updateFixed(delta) {
    for (let i = 0; i < this.systems.length; i++) {
      const sys = this.systems[i];
      if (typeof sys.updateFixed === 'function') {
        const entities = sys.mask ? this.queryMask(sys.mask) : Array.from(this.entities);
        sys.updateFixed(delta, entities, this);
      }
    }
  }

  /**
   * Advances all registered systems during variable refresh rendering.
   * 
   * @param {number} delta - Frame delta time in seconds.
   * @param {number} alpha - Fixed-step interpolation factor [0.0, 1.0).
   */
  updateVariable(delta, alpha) {
    for (let i = 0; i < this.systems.length; i++) {
      const sys = this.systems[i];
      if (typeof sys.updateVariable === 'function') {
        const entities = sys.mask ? this.queryMask(sys.mask) : Array.from(this.entities);
        sys.updateVariable(delta, alpha, entities, this);
      }
    }
  }
}
