/**
 * @fileoverview ShilpInventory - 36-Slot Inventory Matrix & Vedic Crafting Altar
 * @module shilploka/inventory/shilp_inventory
 * 
 * Features:
 * - 36 inventory slots (9 hotbar slots + 27 main pouch slots).
 * - Full metadata for Harappan materials, ancient spices, bronze ingots, and tools.
 * - Vedic Crafting Altar (निर्माण पीठ - Nirmana Peetha) recipe database.
 * - Active hotbar selection (1-9).
 * - Seamless integration with ShilpBarterLedger for commodity trading.
 */

import { ShilpBlockId } from '../world/voxel_constants.js';

export const SHILP_ITEMS = {
  HARAPPAN_BAKED_BRICK: {
    id: 'HARAPPAN_BAKED_BRICK',
    name: 'Harappan Baked Brick (पक्व इष्टिका)',
    isBlock: true,
    blockId: ShilpBlockId.HARAPPAN_BAKED_BRICK,
    icon: '🧱',
    maxStack: 64,
  },
  SINDHU_ALLUVIAL_SOIL: {
    id: 'SINDHU_ALLUVIAL_SOIL',
    name: 'Sindhu Loam (सिन्धु कछार मृदा)',
    isBlock: true,
    blockId: ShilpBlockId.SINDHU_ALLUVIAL_SOIL,
    icon: '🪴',
    maxStack: 64,
  },
  CHUNAR_SANDSTONE: {
    id: 'CHUNAR_SANDSTONE',
    name: 'Chunar Sandstone (चुनार पाषाण)',
    isBlock: true,
    blockId: ShilpBlockId.CHUNAR_SANDSTONE,
    icon: '🪨',
    maxStack: 64,
  },
  DECCAN_BASALT: {
    id: 'DECCAN_BASALT',
    name: 'Deccan Basalt (दक्कन श्याम शैल)',
    isBlock: true,
    blockId: ShilpBlockId.DECCAN_BASALT,
    icon: '⬛',
    maxStack: 64,
  },
  BANYAN_WOOD: {
    id: 'BANYAN_WOOD',
    name: 'Banyan Log (वटवृक्ष काष्ठ)',
    isBlock: true,
    blockId: ShilpBlockId.BANYAN_WOOD,
    icon: '🪵',
    maxStack: 64,
  },
  BANYAN_PLANKS: {
    id: 'BANYAN_PLANKS',
    name: 'Carved Banyan Planks (वट फलक)',
    isBlock: false,
    icon: '🪵',
    maxStack: 64,
  },
  WOODEN_SHAFT: {
    id: 'WOODEN_SHAFT',
    name: 'Turned Wooden Shaft (काष्ठ दण्ड)',
    isBlock: false,
    icon: '🥢',
    maxStack: 64,
  },
  BRONZE_INGOT: {
    id: 'BRONZE_INGOT',
    name: 'Kansa Bronze Ingot (कांस्य पिण्ड)',
    isBlock: false,
    icon: '🔶',
    maxStack: 64,
  },
  LAJWARD_GEM: {
    id: 'LAJWARD_GEM',
    name: 'Badakhshan Lajward (लाजवर्द - Lapis)',
    isBlock: false,
    icon: '💎',
    maxStack: 64,
  },
  CARDAMOM: {
    id: 'CARDAMOM',
    name: 'Malabar Cardamom (एला / इलायची)',
    isBlock: false,
    icon: '🌿',
    maxStack: 64,
  },
  PEPPER: {
    id: 'PEPPER',
    name: 'Black Pepper (मरिच / काला सोना)',
    isBlock: false,
    icon: '⚫',
    maxStack: 64,
  },
  SAFFRON: {
    id: 'SAFFRON',
    name: 'Kashmiri Saffron (कुंकुम / केसर)',
    isBlock: false,
    icon: '🌸',
    maxStack: 64,
  },
  BRONZE_PICKAXE: {
    id: 'BRONZE_PICKAXE',
    name: 'Kansa Bronze Pickaxe (कांस्य खनित्र)',
    isBlock: false,
    icon: '⛏️',
    maxStack: 1,
  },
  BITUMEN_MORTAR: {
    id: 'BITUMEN_MORTAR',
    name: 'Waterproof Bitumen (शिलाजतु संधानक)',
    isBlock: true,
    blockId: ShilpBlockId.BITUMEN_MORTAR,
    icon: '🖤',
    maxStack: 64,
  },
};

/**
 * Vedic Crafting Altar Recipe Registry
 */
export const VEDIC_RECIPES = [
  {
    id: 'craft_banyan_planks',
    name: 'Hew Banyan Planks (वट फलक निर्माण)',
    description: 'Hew raw sacred Banyan logs into polished building timber planks.',
    ingredients: [{ itemId: 'BANYAN_WOOD', count: 1 }],
    result: { itemId: 'BANYAN_PLANKS', count: 4 },
  },
  {
    id: 'craft_wooden_shafts',
    name: 'Carve Wooden Shafts (काष्ठ दण्ड निर्माण)',
    description: 'Split timber planks into balanced shafts for bronze tool crafting.',
    ingredients: [{ itemId: 'BANYAN_PLANKS', count: 2 }],
    result: { itemId: 'WOODEN_SHAFT', count: 4 },
  },
  {
    id: 'craft_bronze_pickaxe',
    name: 'Forge Kansa Pickaxe (कांस्य खनित्र निर्माण)',
    description: 'Cast an ancient Harappan bronze pickaxe head fastened onto teak shafts.',
    ingredients: [
      { itemId: 'BRONZE_INGOT', count: 3 },
      { itemId: 'WOODEN_SHAFT', count: 2 },
    ],
    result: { itemId: 'BRONZE_PICKAXE', count: 1 },
  },
  {
    id: 'craft_waterproof_masonry',
    name: 'Mohenjo Waterproof Masonry (सिन्धु जलसह इष्टिका)',
    description: 'Coat baked bricks with natural bitumen asphalt for impervious canal lining.',
    ingredients: [
      { itemId: 'HARAPPAN_BAKED_BRICK', count: 4 },
      { itemId: 'BITUMEN_MORTAR', count: 1 },
    ],
    result: { itemId: 'HARAPPAN_BAKED_BRICK', count: 4 },
  },
];

export class ShilpInventory {
  constructor() {
    this.totalSlots = 36;
    this.hotbarSlotsCount = 9;
    this.activeSlotIndex = 0;

    /**
     * Slot array: each entry is { itemId: string, count: number } or null
     * @type {Array<{ itemId: string, count: number }|null>}
     */
    this.slots = new Array(this.totalSlots).fill(null);

    this._initializeStartingPouch();
  }

  _initializeStartingPouch() {
    // Starting Harappan toolkit and trade commodities
    this.setSlot(0, 'HARAPPAN_BAKED_BRICK', 48);
    this.setSlot(1, 'CHUNAR_SANDSTONE', 32);
    this.setSlot(2, 'BANYAN_WOOD', 16);
    this.setSlot(3, 'BRONZE_INGOT', 12);
    this.setSlot(4, 'CARDAMOM', 24); // Spices for barter
    this.setSlot(5, 'PEPPER', 40);
    this.setSlot(6, 'SAFFRON', 8);
    this.setSlot(7, 'BITUMEN_MORTAR', 16);
    this.setSlot(8, 'BRONZE_PICKAXE', 1);
  }

  setSlot(index, itemId, count) {
    if (index < 0 || index >= this.totalSlots) return;
    if (!itemId || count <= 0) {
      this.slots[index] = null;
      return;
    }
    const meta = SHILP_ITEMS[itemId];
    if (!meta) return;
    this.slots[index] = { itemId, count: Math.min(count, meta.maxStack) };
  }

  getSlot(index) {
    if (index < 0 || index >= this.totalSlots) return null;
    return this.slots[index];
  }

  getActiveSlot() {
    return this.slots[this.activeSlotIndex];
  }

  /**
   * Move the stack in slot `from` onto slot `to`.
   *
   * WHY this exists: the inventory always had 36 slots, but only the 9-slot
   * hotbar was ever shown, so items that overflowed into slots 10-36 (mining,
   * barter, crafting) could never be seen or used. The inventory panel (E)
   * uses this to bring them into the hotbar.
   *
   * WHAT it does:
   *  - different items (or an empty target): the two slots swap;
   *  - the same item: `to` fills up to the item's maxStack, and whatever
   *    does not fit stays in `from` - so no item is ever created or lost.
   *
   * USED BY: ShilpInventoryPanel. NEXT: the panel re-renders itself and the
   * engine's hotbar, and the next autosave stores the new slot layout.
   *
   * @param {number} from - Slot index 0-35.
   * @param {number} to - Slot index 0-35.
   * @returns {boolean} false when an index is out of range or they are equal.
   */
  moveSlot(from, to) {
    const valid = i => Number.isInteger(i) && i >= 0 && i < this.totalSlots;
    if (!valid(from) || !valid(to) || from === to) return false;
    const a = this.slots[from];
    const b = this.slots[to];
    if (a && b && a.itemId === b.itemId) {
      const room = SHILP_ITEMS[a.itemId].maxStack - b.count;
      const moved = Math.min(room, a.count);
      b.count += moved;
      a.count -= moved;
      if (a.count === 0) this.slots[from] = null;
    } else {
      this.slots[from] = b;
      this.slots[to] = a;
    }
    return true;
  }

  setActiveSlot(index) {
    if (index >= 0 && index < this.hotbarSlotsCount) {
      this.activeSlotIndex = index;
    }
  }

  /**
   * Counts total items matching itemId across all slots.
   */
  countItem(itemId) {
    let total = 0;
    for (let i = 0; i < this.totalSlots; i++) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId) {
        total += slot.count;
      }
    }
    return total;
  }

  /**
   * Adds an item with automatic stacking and overflow.
   */
  addItem(itemId, amount = 1) {
    const meta = SHILP_ITEMS[itemId];
    if (!meta || amount <= 0) return 0;

    let remaining = amount;

    // 1. Stack into existing matching non-full slots
    for (let i = 0; i < this.totalSlots; i++) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId && slot.count < meta.maxStack) {
        const space = meta.maxStack - slot.count;
        const add = Math.min(space, remaining);
        slot.count += add;
        remaining -= add;
        if (remaining <= 0) return amount;
      }
    }

    // 2. Fill first empty slot
    for (let i = 0; i < this.totalSlots; i++) {
      if (this.slots[i] === null) {
        const add = Math.min(meta.maxStack, remaining);
        this.slots[i] = { itemId, count: add };
        remaining -= add;
        if (remaining <= 0) return amount;
      }
    }

    return amount - remaining; // Added count
  }

  /**
   * Consumes a given amount of an item.
   */
  consumeItem(itemId, amount = 1) {
    if (this.countItem(itemId) < amount) return false;

    let remaining = amount;
    for (let i = 0; i < this.totalSlots; i++) {
      const slot = this.slots[i];
      if (slot && slot.itemId === itemId) {
        if (slot.count > remaining) {
          slot.count -= remaining;
          remaining = 0;
          break;
        } else {
          remaining -= slot.count;
          this.slots[i] = null;
          if (remaining === 0) break;
        }
      }
    }
    return true;
  }

  /**
   * Executes a Vedic Crafting Altar recipe.
   * 
   * @param {string} recipeId - Recipe ID.
   * @returns {{ success: boolean, message: string }}
   */
  craftRecipe(recipeId) {
    const recipe = VEDIC_RECIPES.find(r => r.id === recipeId);
    if (!recipe) return { success: false, message: 'Unknown recipe.' };

    // Verify all ingredients are present
    for (const ing of recipe.ingredients) {
      if (this.countItem(ing.itemId) < ing.count) {
        return {
          success: false,
          message: `Missing ingredients: Need ${ing.count}x ${SHILP_ITEMS[ing.itemId]?.name}.`,
        };
      }
    }

    // Deduct ingredients
    for (const ing of recipe.ingredients) {
      this.consumeItem(ing.itemId, ing.count);
    }

    // Grant crafted output
    this.addItem(recipe.result.itemId, recipe.result.count);

    return {
      success: true,
      message: `सफलतापूर्वक निर्मित: Crafted ${recipe.result.count}x ${SHILP_ITEMS[recipe.result.itemId]?.name}.`,
    };
  }

  /**
   * Helper snapshot for barter commodity trading.
   */
  getCommodityStock() {
    return {
      cardamom: this.countItem('CARDAMOM'),
      pepper: this.countItem('PEPPER'),
      saffron: this.countItem('SAFFRON'),
      bronzeIngots: this.countItem('BRONZE_INGOT'),
      lapis: this.countItem('LAJWARD_GEM'),
    };
  }
}
