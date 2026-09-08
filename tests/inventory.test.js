import { beforeEach, describe, expect, it } from 'vitest';

import { BLOCKS } from '../src/constants.js';
import { Inventory, MAX_STACK_SIZE } from '../src/inventory.js';

/**
 * Inventory's constructor wires up DOM elements, which do not exist under a
 * plain test runner. These tests exercise the storage logic on its own by
 * building the instance from the prototype and giving it exactly the fields
 * that logic touches. addItem, consumeActiveItem and setActiveSlot are the
 * real implementations -- nothing is stubbed except render(), which only
 * repaints the UI.
 */
function makeInventory() {
  const inv = Object.create(Inventory.prototype);
  inv.slots = new Array(36).fill(null);
  inv.activeSlot = 0;
  inv.render = () => {};
  return inv;
}

describe('Inventory.addItem', () => {
  let inv;
  beforeEach(() => {
    inv = makeInventory();
  });

  it('places a new item in the first empty slot', () => {
    expect(inv.addItem(BLOCKS.DIRT, 5)).toBe(5);
    expect(inv.slots[0]).toEqual({ id: BLOCKS.DIRT, count: 5 });
  });

  it('tops up an existing stack before opening a new slot', () => {
    inv.addItem(BLOCKS.DIRT, 5);
    inv.addItem(BLOCKS.DIRT, 3);

    expect(inv.slots[0].count).toBe(8);
    expect(inv.slots[1]).toBeNull();
  });

  it('never exceeds MAX_STACK_SIZE in one slot', () => {
    inv.addItem(BLOCKS.DIRT, MAX_STACK_SIZE + 10);

    expect(inv.slots[0].count).toBe(MAX_STACK_SIZE);
    expect(inv.slots[1].count).toBe(10);
  });

  it('spills across as many slots as the amount needs', () => {
    inv.addItem(BLOCKS.DIRT, MAX_STACK_SIZE * 2 + 1);

    expect(inv.slots[0].count).toBe(MAX_STACK_SIZE);
    expect(inv.slots[1].count).toBe(MAX_STACK_SIZE);
    expect(inv.slots[2].count).toBe(1);
  });

  it('keeps different item ids in separate stacks', () => {
    inv.addItem(BLOCKS.DIRT, 4);
    inv.addItem(BLOCKS.COBBLESTONE, 4);

    expect(inv.slots[0].id).toBe(BLOCKS.DIRT);
    expect(inv.slots[1].id).toBe(BLOCKS.COBBLESTONE);
  });

  it('returns how many it actually stored when the bag is full', () => {
    for (let i = 0; i < 36; i++) {
      inv.slots[i] = { id: BLOCKS.DIRT, count: MAX_STACK_SIZE };
    }

    expect(inv.addItem(BLOCKS.DIRT, 10)).toBe(0);
  });

  it('fills the last gap and reports the shortfall', () => {
    for (let i = 0; i < 36; i++) {
      inv.slots[i] = { id: BLOCKS.DIRT, count: MAX_STACK_SIZE };
    }
    inv.slots[35] = { id: BLOCKS.DIRT, count: MAX_STACK_SIZE - 3 };

    expect(inv.addItem(BLOCKS.DIRT, 10)).toBe(3);
    expect(inv.slots[35].count).toBe(MAX_STACK_SIZE);
  });

  it('refuses air and non-positive counts', () => {
    expect(inv.addItem(BLOCKS.AIR, 5)).toBe(0);
    expect(inv.addItem(BLOCKS.DIRT, 0)).toBe(0);
    expect(inv.addItem(BLOCKS.DIRT, -3)).toBe(0);
    expect(inv.slots.every((s) => s === null)).toBe(true);
  });

  it('defaults to adding one', () => {
    inv.addItem(BLOCKS.DIRT);
    expect(inv.slots[0].count).toBe(1);
  });
});

describe('Inventory.consumeActiveItem', () => {
  let inv;
  beforeEach(() => {
    inv = makeInventory();
  });

  it('decrements the active stack', () => {
    inv.addItem(BLOCKS.DIRT, 5);
    inv.consumeActiveItem();

    expect(inv.slots[0].count).toBe(4);
  });

  it('empties the slot once the stack runs out', () => {
    inv.addItem(BLOCKS.DIRT, 1);
    inv.consumeActiveItem();

    expect(inv.slots[0]).toBeNull();
  });

  it('clears the slot when asked for more than is there', () => {
    inv.addItem(BLOCKS.DIRT, 2);
    inv.consumeActiveItem(5);

    expect(inv.slots[0]).toBeNull();
  });

  it('does nothing on an empty slot', () => {
    expect(() => inv.consumeActiveItem()).not.toThrow();
    expect(inv.slots[0]).toBeNull();
  });

  it('consumes from whichever slot is active', () => {
    inv.addItem(BLOCKS.DIRT, 4);
    inv.addItem(BLOCKS.COBBLESTONE, 4);
    inv.activeSlot = 1;
    inv.consumeActiveItem(2);

    expect(inv.slots[0].count).toBe(4);
    expect(inv.slots[1].count).toBe(2);
  });
});

describe('Inventory.setActiveSlot', () => {
  let inv;
  beforeEach(() => {
    inv = makeInventory();
  });

  it('accepts every hotbar index', () => {
    for (let i = 0; i < 9; i++) {
      inv.setActiveSlot(i);
      expect(inv.activeSlot).toBe(i);
    }
  });

  it('ignores indices outside the hotbar', () => {
    inv.setActiveSlot(3);
    inv.setActiveSlot(9);
    inv.setActiveSlot(-1);
    inv.setActiveSlot(35);

    expect(inv.activeSlot).toBe(3);
  });
});

describe('Inventory.getActiveItem', () => {
  it('returns the stack under the active slot, or null', () => {
    const inv = makeInventory();
    expect(inv.getActiveItem()).toBeNull();

    inv.addItem(BLOCKS.DIRT, 2);
    expect(inv.getActiveItem()).toEqual({ id: BLOCKS.DIRT, count: 2 });
  });
});
