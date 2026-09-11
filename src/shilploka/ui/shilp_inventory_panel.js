/**
 * @fileoverview ShilpInventoryPanel - the Potli (पोटली, the traveller's pouch):
 * all 36 inventory slots, opened with E.
 * @module shilploka/ui/shilp_inventory_panel
 *
 * WHY this exists: the inventory has always held 36 slots, but the game only
 * ever drew the 9-slot hotbar. Whatever overflowed into slots 10-36 - blocks
 * from mining, spices from barter, crafted goods - was invisible and could
 * not be used. The E key was mapped (VastuAction.INVENTORY_TOGGLE) with
 * nothing listening to it.
 *
 * WHAT it does: shows the hotbar row (1-9) and the 27 pouch slots. Click a
 * slot to pick it up, then click another to move it there (swap, or merge the
 * same item - see ShilpInventory.moveSlot). Click the picked slot again to
 * put it back.
 *
 * USED BY: ShilpEngine (E key, and the mobile 🎒 button). It follows the same
 * container + toggle() pattern as the barter and crafting modals.
 * NEXT: after a move, onChange() lets the engine redraw the hotbar; the new
 * slot layout is written by the next autosave.
 */

import { SHILP_ITEMS } from '../inventory/shilp_inventory.js';

export class ShilpInventoryPanel {
  /**
   * @param {HTMLElement} rootContainer - The #inventory-modal element.
   * @param {import('../inventory/shilp_inventory.js').ShilpInventory} inventory
   * @param {function(): void} [onChange] - Called after every move.
   */
  constructor(rootContainer, inventory, onChange = () => {}) {
    this.container = rootContainer;
    this.inventory = inventory;
    this.onChange = onChange;
    this.isOpen = false;
    /** Slot index picked up by the first click, or null. */
    this.picked = null;

    this.container.innerHTML = `
      <div class="inventory-card" role="dialog" aria-label="Inventory">
        <div class="crafting-header">
          <div class="crafting-title">
            <span class="chakra-icon">🎒</span>
            <div>
              <h2>पोटली • Inventory</h2>
              <span class="crafting-sub">Click a slot, then another, to move it. Same items stack together.</span>
            </div>
          </div>
          <button class="learn-close-btn inventory-close-btn" type="button" aria-label="Close">&times;</button>
        </div>
        <div class="inventory-body">
          <div class="inventory-label">Hotbar (1–9)</div>
          <div class="inventory-grid" data-row="hotbar"></div>
          <div class="inventory-label">Pouch</div>
          <div class="inventory-grid" data-row="pouch"></div>
          <div class="inventory-held" aria-live="polite"></div>
        </div>
      </div>
    `;
    this.hotbarGrid = this.container.querySelector('[data-row="hotbar"]');
    this.pouchGrid = this.container.querySelector('[data-row="pouch"]');
    this.heldLine = this.container.querySelector('.inventory-held');

    this.container.querySelector('.inventory-close-btn')
      .addEventListener('click', () => this.toggle(false));

    // One delegated listener for all 36 slots, instead of 36 listeners
    // re-created on every render.
    this.container.addEventListener('click', (e) => {
      const slotElem = e.target.closest('[data-slot]');
      if (slotElem) this._clickSlot(Number(slotElem.dataset.slot));
    });
  }

  /**
   * First click picks a slot up (only if it holds something); the second
   * click moves it. Clicking the picked slot again cancels.
   */
  _clickSlot(index) {
    if (this.picked === null) {
      if (this.inventory.getSlot(index)) this.picked = index;
    } else {
      if (index !== this.picked) {
        this.inventory.moveSlot(this.picked, index);
        this.onChange();
      }
      this.picked = null;
    }
    this.render();
  }

  /** Draw all 36 slots from the inventory's current state. */
  render() {
    const drawRow = (grid, from, to) => {
      grid.innerHTML = '';
      for (let i = from; i < to; i++) {
        const slot = this.inventory.getSlot(i);
        const meta = slot ? SHILP_ITEMS[slot.itemId] : null;
        const el = document.createElement('button');
        el.type = 'button';
        el.dataset.slot = String(i);
        el.className = 'hotbar-slot inventory-slot' +
          (i === this.inventory.activeSlotIndex ? ' active' : '') +
          (i === this.picked ? ' picked' : '');
        el.title = meta ? `${meta.name} (${slot.count})` : `Empty slot ${i + 1}`;
        el.innerHTML =
          (i < this.inventory.hotbarSlotsCount ? `<span class="slot-number">${i + 1}</span>` : '') +
          (meta ? `<span class="slot-icon">${meta.icon || '📦'}</span>` : '') +
          (meta && slot.count > 1 ? `<span class="slot-count">${slot.count}</span>` : '');
        grid.appendChild(el);
      }
    };
    drawRow(this.hotbarGrid, 0, this.inventory.hotbarSlotsCount);
    drawRow(this.pouchGrid, this.inventory.hotbarSlotsCount, this.inventory.totalSlots);

    const picked = this.picked !== null ? this.inventory.getSlot(this.picked) : null;
    this.heldLine.textContent = picked
      ? `Moving: ${SHILP_ITEMS[picked.itemId].name} ×${picked.count} — click where to put it`
      : '';
  }

  /**
   * Open or close. Same contract as the barter and crafting modals.
   * @param {boolean} [forceState]
   */
  toggle(forceState) {
    this.isOpen = forceState !== undefined ? forceState : !this.isOpen;
    this.picked = null;
    if (this.isOpen) {
      this.render();
      this.container.classList.remove('hidden');
      // Free the mouse so the slots can be clicked (as the other modals do).
      if (document.exitPointerLock) document.exitPointerLock();
    } else {
      this.container.classList.add('hidden');
    }
  }
}
