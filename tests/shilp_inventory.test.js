/**
 * Tests for the LIVE inventory: src/shilploka/inventory/shilp_inventory.js.
 *
 * WHY this file replaced tests/inventory.test.js: the old file tested
 * src/inventory.js, a module from the legacy engine that the running game never
 * imported. It passed while telling us nothing about the inventory players
 * actually use, and once the legacy engine was removed it could not even load.
 *
 * These tests use the real ShilpInventory class. It has no DOM dependency, so
 * nothing is mocked.
 */
import { beforeEach, describe, expect, it } from 'vitest';

import { ShilpInventory, SHILP_ITEMS, VEDIC_RECIPES } from '../src/shilploka/inventory/shilp_inventory.js';

describe('ShilpInventory', () => {
  let inv;

  beforeEach(() => {
    inv = new ShilpInventory();
  });

  describe('starting pouch', () => {
    // WHY: the first hotbar is what a new player sees. If it silently came up
    // empty, placing and crafting would be impossible from the first second.
    it('begins with the Harappan toolkit in the hotbar', () => {
      expect(inv.getSlot(0)).toEqual({ itemId: 'HARAPPAN_BAKED_BRICK', count: 48 });
      expect(inv.getSlot(8)).toEqual({ itemId: 'BRONZE_PICKAXE', count: 1 });
      expect(inv.countItem('CARDAMOM')).toBe(24);
    });

    it('has 36 slots, the first 9 of them the hotbar', () => {
      expect(inv.slots).toHaveLength(36);
      expect(inv.hotbarSlotsCount).toBe(9);
    });
  });

  describe('addItem', () => {
    it('tops up an existing stack before using a new slot', () => {
      const before = inv.getSlot(0).count;                  // 48 bricks
      expect(inv.addItem('HARAPPAN_BAKED_BRICK', 10)).toBe(10);
      expect(inv.getSlot(0).count).toBe(before + 10);
    });

    it('overflows into the next empty slot once a stack is full', () => {
      // 48 bricks + 20 = 68, over the 64 max stack: 64 stay, 4 spill over.
      inv.addItem('HARAPPAN_BAKED_BRICK', 20);
      expect(inv.getSlot(0).count).toBe(SHILP_ITEMS.HARAPPAN_BAKED_BRICK.maxStack);
      expect(inv.countItem('HARAPPAN_BAKED_BRICK')).toBe(68);
    });

    it('never builds a stack larger than maxStack', () => {
      // A pickaxe has maxStack 1, so three of them need three slots.
      inv.addItem('BRONZE_PICKAXE', 2);
      for (const slot of inv.slots) {
        if (slot) expect(slot.count).toBeLessThanOrEqual(SHILP_ITEMS[slot.itemId].maxStack);
      }
      expect(inv.countItem('BRONZE_PICKAXE')).toBe(3);
    });

    it('reports how much actually fit when the inventory is full', () => {
      for (let i = 0; i < inv.totalSlots; i++) inv.setSlot(i, 'BRONZE_PICKAXE', 1);
      expect(inv.addItem('SAFFRON', 5)).toBe(0);
    });

    it('rejects unknown items and non-positive amounts', () => {
      expect(inv.addItem('NOT_A_REAL_ITEM', 5)).toBe(0);
      expect(inv.addItem('SAFFRON', 0)).toBe(0);
      expect(inv.addItem('SAFFRON', -3)).toBe(0);
    });
  });

  describe('consumeItem', () => {
    it('removes items and empties a slot that reaches zero', () => {
      expect(inv.consumeItem('SAFFRON', 8)).toBe(true);     // the whole stack
      expect(inv.countItem('SAFFRON')).toBe(0);
      expect(inv.slots.some(s => s?.itemId === 'SAFFRON')).toBe(false);
    });

    it('takes from several stacks when one is not enough', () => {
      inv.addItem('HARAPPAN_BAKED_BRICK', 30);              // 78 across two stacks
      expect(inv.consumeItem('HARAPPAN_BAKED_BRICK', 70)).toBe(true);
      expect(inv.countItem('HARAPPAN_BAKED_BRICK')).toBe(8);
    });

    // WHY this matters: without the up-front check, a failed consume would
    // still take whatever partial amount existed - the player loses items and
    // gets nothing for it.
    it('takes nothing at all when there is not enough', () => {
      expect(inv.consumeItem('SAFFRON', 999)).toBe(false);
      expect(inv.countItem('SAFFRON')).toBe(8);
    });
  });

  describe('setSlot / setActiveSlot', () => {
    it('clamps a stack to the item\'s maxStack', () => {
      inv.setSlot(20, 'BRONZE_PICKAXE', 50);
      expect(inv.getSlot(20).count).toBe(1);
    });

    // The save system restores slots through setSlot, so this is what stops a
    // corrupted or hand-edited save from injecting items the game does not know.
    it('ignores unknown item ids', () => {
      inv.setSlot(20, 'FORGED_ITEM', 5);
      expect(inv.getSlot(20)).toBeNull();
    });

    it('clears a slot when given no item or a zero count', () => {
      inv.setSlot(0, null, 0);
      expect(inv.getSlot(0)).toBeNull();
    });

    it('only lets the active slot move within the hotbar', () => {
      inv.setActiveSlot(5);
      expect(inv.activeSlotIndex).toBe(5);
      inv.setActiveSlot(20);                                 // not a hotbar slot
      expect(inv.activeSlotIndex).toBe(5);
    });
  });

  describe('craftRecipe', () => {
    it('consumes the ingredients and grants the result', () => {
      const recipe = VEDIC_RECIPES.find(r => r.id === 'craft_banyan_planks');
      const woodBefore = inv.countItem('BANYAN_WOOD');
      const res = inv.craftRecipe(recipe.id);
      expect(res.success).toBe(true);
      expect(inv.countItem('BANYAN_WOOD')).toBe(woodBefore - 1);
      expect(inv.countItem('BANYAN_PLANKS')).toBe(4);
    });

    it('refuses, and changes nothing, when an ingredient is missing', () => {
      inv.consumeItem('BANYAN_WOOD', inv.countItem('BANYAN_WOOD'));
      const snapshot = JSON.stringify(inv.slots);
      expect(inv.craftRecipe('craft_banyan_planks').success).toBe(false);
      expect(JSON.stringify(inv.slots)).toBe(snapshot);
    });

    it('rejects an unknown recipe', () => {
      expect(inv.craftRecipe('no_such_recipe').success).toBe(false);
    });
  });
});
