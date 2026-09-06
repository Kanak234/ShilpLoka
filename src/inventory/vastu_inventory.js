/**
 * @fileoverview VastuInventory - Ancient Indian Inventory & Crafting Altar System
 * @module inventory/vastu_inventory
 * 
 * Features:
 * - 36-slot inventory matrix (9 hotbar slots + 27 main bag slots).
 * - Full metadata for Ancient Indian materials, bronze tools, and cultural artifacts.
 * - Vedic Crafting Altar (Nirmana Peetha) recipe database with deterministic verification.
 * - Hotbar active item selection (controlled via 1-9 keys or scroll wheel).
 * - Stack management with automatic overflow handling (max stack 64).
 */

import { VastuBlockId } from '../world/ancient_blocks.js';

/**
 * Item registry containing block items, smelted ingots, and bronze age tools.
 */
export const VASTU_ITEMS = {
  HARAPPAN_BRICK: {
    id: 'HARAPPAN_BRICK',
    name: 'Harappan Baked Brick',
    devanagari: 'सिन्धु पक्की ईंट',
    isBlock: true,
    blockId: VastuBlockId.HARAPPAN_BRICK,
    icon: '🧱',
    maxStack: 64,
  },
  TERRACOTTA: {
    id: 'TERRACOTTA',
    name: 'Terracotta Mitti',
    devanagari: 'पक्व-मृदा',
    isBlock: true,
    blockId: VastuBlockId.TERRACOTTA,
    icon: '🏺',
    maxStack: 64,
  },
  CHUNAR_SANDSTONE: {
    id: 'CHUNAR_SANDSTONE',
    name: 'Chunar Sandstone',
    devanagari: 'चुनार बलुआ पत्थर',
    isBlock: true,
    blockId: VastuBlockId.CHUNAR_SANDSTONE,
    icon: '🪨',
    maxStack: 64,
  },
  SAAGWAN_LOG: {
    id: 'SAAGWAN_LOG',
    name: 'Saagwan Teak Log',
    devanagari: 'शागवान काष्ठ',
    isBlock: true,
    blockId: VastuBlockId.SAAGWAN_LOG,
    icon: '🪵',
    maxStack: 64,
  },
  SAAGWAN_PLANKS: {
    id: 'SAAGWAN_PLANKS',
    name: 'Carved Saagwan Planks',
    devanagari: 'शागवान फलक',
    isBlock: true,
    blockId: VastuBlockId.SAAGWAN_PLANKS,
    icon: '🪵',
    maxStack: 64,
  },
  BRONZE_ORE: {
    id: 'BRONZE_ORE',
    name: 'Kansa Bronze Ore',
    devanagari: 'कांस्य धातु',
    isBlock: true,
    blockId: VastuBlockId.BRONZE_ORE,
    icon: '🪨',
    maxStack: 64,
  },
  BRONZE_INGOT: {
    id: 'BRONZE_INGOT',
    name: 'Kansa Bronze Ingot',
    devanagari: 'कांस्य पिण्ड',
    isBlock: false,
    icon: '🥇',
    maxStack: 64,
  },
  LAPIS_LAZULI: {
    id: 'LAPIS_LAZULI',
    name: 'Lajward Lapis Lazuli',
    devanagari: 'लाजवर्द्',
    isBlock: true,
    blockId: VastuBlockId.LAPIS_LAZULI,
    icon: '💎',
    maxStack: 64,
  },
  SHAFT_STICK: {
    id: 'SHAFT_STICK',
    name: 'Wooden Shaft Stick',
    devanagari: 'काष्ठ दण्ड',
    isBlock: false,
    icon: '🥢',
    maxStack: 64,
  },
  BRONZE_PICKAXE: {
    id: 'BRONZE_PICKAXE',
    name: 'Kansa Khanitra (Pickaxe)',
    devanagari: 'कांस्य खनित्र',
    isBlock: false,
    isTool: true,
    icon: '⛏️',
    maxStack: 1,
  },
  BRONZE_SWORD: {
    id: 'BRONZE_SWORD',
    name: 'Kansa Khadga (Sword)',
    devanagari: 'कांस्य खड्ग',
    isBlock: false,
    isTool: true,
    icon: '🗡️',
    maxStack: 1,
  },
  BRONZE_AXE: {
    id: 'BRONZE_AXE',
    name: 'Kansa Kuthara (Axe)',
    devanagari: 'कांस्य कुठार',
    isBlock: false,
    isTool: true,
    icon: '🪓',
    maxStack: 1,
  },
  INDUS_SEAL: {
    id: 'INDUS_SEAL',
    name: 'Indus Valley Seal',
    devanagari: 'सिन्धु मुद्रा शिला',
    isBlock: false,
    icon: '📜',
    maxStack: 16,
  },
};

/**
 * Vedic Crafting Altar (Nirmana Peetha) Recipe Database.
 */
export const VASTU_RECIPES = [
  {
    id: 'CRAFT_PLANKS',
    name: 'Hew Saagwan Planks',
    devanagari: 'शागवान फलक निर्माण',
    inputs: [{ itemId: 'SAAGWAN_LOG', count: 1 }],
    output: { itemId: 'SAAGWAN_PLANKS', count: 4 },
  },
  {
    id: 'CRAFT_STICKS',
    name: 'Carve Wooden Shafts',
    devanagari: 'काष्ठ दण्ड निर्माण',
    inputs: [{ itemId: 'SAAGWAN_PLANKS', count: 2 }],
    output: { itemId: 'SHAFT_STICK', count: 4 },
  },
  {
    id: 'CRAFT_BRICKS',
    name: 'Kiln-Bake Harappan Bricks',
    devanagari: 'सिन्धु ईंट पकाना',
    inputs: [{ itemId: 'TERRACOTTA', count: 4 }],
    output: { itemId: 'HARAPPAN_BRICK', count: 4 },
  },
  {
    id: 'CRAFT_PICKAXE',
    name: 'Forge Kansa Pickaxe',
    devanagari: 'कांस्य खनित्र निर्माण',
    inputs: [
      { itemId: 'BRONZE_INGOT', count: 3 },
      { itemId: 'SHAFT_STICK', count: 2 },
    ],
    output: { itemId: 'BRONZE_PICKAXE', count: 1 },
  },
  {
    id: 'CRAFT_SWORD',
    name: 'Forge Kansa Khadga',
    devanagari: 'कांस्य खड्ग निर्माण',
    inputs: [
      { itemId: 'BRONZE_INGOT', count: 2 },
      { itemId: 'SHAFT_STICK', count: 1 },
    ],
    output: { itemId: 'BRONZE_SWORD', count: 1 },
  },
  {
    id: 'CRAFT_AXE',
    name: 'Forge Kansa Kuthara',
    devanagari: 'कांस्य कुठार निर्माण',
    inputs: [
      { itemId: 'BRONZE_INGOT', count: 3 },
      { itemId: 'SHAFT_STICK', count: 2 },
    ],
    output: { itemId: 'BRONZE_AXE', count: 1 },
  },
  {
    id: 'CRAFT_SEAL',
    name: 'Engrave Indus Valley Seal',
    devanagari: 'सिन्धु मुद्रा उत्कीर्णन',
    inputs: [
      { itemId: 'LAPIS_LAZULI', count: 2 },
      { itemId: 'CHUNAR_SANDSTONE', count: 2 },
    ],
    output: { itemId: 'INDUS_SEAL', count: 1 },
  },
];

/**
 * 36-slot Inventory coordinator with crafting verification.
 */
export class VastuInventory {
  /**
   * Initializes inventory slots and default Ancient Indian starting equipment.
   */
  constructor() {
    this.totalSlots = 36;
    this.hotbarSize = 9;
    this.selectedHotbarIndex = 0;

    /**
     * Array of 36 slots. Each slot is { itemId: string, count: number } or null.
     * @type {Array<{ itemId: string, count: number }|null>}
     */
    this.slots = new Array(this.totalSlots).fill(null);

    // Populate Initial Vedic Builder Kit
    this.slots[0] = { itemId: 'HARAPPAN_BRICK', count: 48 };
    this.slots[1] = { itemId: 'TERRACOTTA', count: 32 };
    this.slots[2] = { itemId: 'CHUNAR_SANDSTONE', count: 32 };
    this.slots[3] = { itemId: 'SAAGWAN_LOG', count: 16 };
    this.slots[4] = { itemId: 'BRONZE_INGOT', count: 12 };
    this.slots[5] = { itemId: 'LAPIS_LAZULI', count: 8 };
    this.slots[6] = { itemId: 'SHAFT_STICK', count: 16 };
    this.slots[7] = { itemId: 'BRONZE_PICKAXE', count: 1 };
    this.slots[8] = { itemId: 'BRONZE_SWORD', count: 1 };
  }

  /**
   * Selects an active hotbar slot (0 through 8).
   * 
   * @param {number} index - Hotbar slot index.
   */
  selectHotbarSlot(index) {
    if (index >= 0 && index < this.hotbarSize) {
      this.selectedHotbarIndex = index;
    }
  }

  /**
   * Gets the item stack currently held in the active hotbar slot.
   * 
   * @returns {{ itemId: string, count: number, meta: Object }|null}
   */
  getSelectedItem() {
    const slot = this.slots[this.selectedHotbarIndex];
    if (!slot) return null;
    return {
      ...slot,
      meta: VASTU_ITEMS[slot.itemId],
    };
  }

  /**
   * Consumes one or more units from the active hotbar slot.
   * 
   * @param {number} [count=1] - Amount to consume.
   * @returns {boolean} True if successfully consumed.
   */
  consumeSelectedItem(count = 1) {
    const slot = this.slots[this.selectedHotbarIndex];
    if (!slot || slot.count < count) return false;

    slot.count -= count;
    if (slot.count <= 0) {
      this.slots[this.selectedHotbarIndex] = null;
    }
    return true;
  }

  /**
   * Counts the total number of items matching itemId across all slots.
   * 
   * @param {string} itemId - Item ID.
   * @returns {number}
   */
  countItem(itemId) {
    let total = 0;
    for (let i = 0; i < this.slots.length; i++) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId) {
        total += slot.count;
      }
    }
    return total;
  }

  /**
   * Adds an item stack into the inventory, grouping into existing stacks or empty slots.
   * 
   * @param {string} itemId - Item ID to add.
   * @param {number} [count=1] - Quantity.
   * @returns {boolean} True if all items were successfully added.
   */
  addItem(itemId, count = 1) {
    const meta = VASTU_ITEMS[itemId];
    if (!meta) return false;

    let remaining = count;

    // 1. Fill existing incomplete stacks
    for (let i = 0; i < this.slots.length && remaining > 0; i++) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId && slot.count < meta.maxStack) {
        const available = meta.maxStack - slot.count;
        const toAdd = Math.min(available, remaining);
        slot.count += toAdd;
        remaining -= toAdd;
      }
    }

    // 2. Occupy empty slots
    for (let i = 0; i < this.slots.length && remaining > 0; i++) {
      if (!this.slots[i]) {
        const toAdd = Math.min(meta.maxStack, remaining);
        this.slots[i] = { itemId, count: toAdd };
        remaining -= toAdd;
      }
    }

    return remaining === 0;
  }

  /**
   * Removes a specified quantity of an item across inventory slots.
   * 
   * @param {string} itemId - Item ID.
   * @param {number} count - Quantity to deduct.
   * @returns {boolean} True if quantity was available and deducted.
   */
  removeItem(itemId, count) {
    if (this.countItem(itemId) < count) return false;

    let remaining = count;
    for (let i = this.slots.length - 1; i >= 0 && remaining > 0; i--) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId) {
        if (slot.count <= remaining) {
          remaining -= slot.count;
          this.slots[i] = null;
        } else {
          slot.count -= remaining;
          remaining = 0;
        }
      }
    }
    return true;
  }

  /**
   * Validates whether a recipe can be crafted with current inventory items.
   * 
   * @param {Object} recipe - Recipe object from VASTU_RECIPES.
   * @returns {boolean}
   */
  canCraft(recipe) {
    for (const input of recipe.inputs) {
      if (this.countItem(input.itemId) < input.count) {
        return false;
      }
    }
    return true;
  }

  /**
   * Crafts a recipe, deducting ingredients and placing the product in inventory.
   * 
   * @param {string} recipeId - Recipe identifier.
   * @returns {boolean} True if crafted successfully.
   */
  craft(recipeId) {
    const recipe = VASTU_RECIPES.find((r) => r.id === recipeId);
    if (!recipe || !this.canCraft(recipe)) return false;

    // Deduct inputs
    for (const input of recipe.inputs) {
      this.removeItem(input.itemId, input.count);
    }

    // Award output
    this.addItem(recipe.output.itemId, recipe.output.count);
    console.log(`[Nirmana Crafting] Crafted: ${recipe.name} (${recipe.devanagari})`);
    return true;
  }
}
