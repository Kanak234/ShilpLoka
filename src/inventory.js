/**
 * ============================================================================
 * INVENTORY & CRAFTING ENGINE
 * ============================================================================
 * Manages the 36-slot player storage (9 hotbar + 27 main inventory), the 2x2
 * player crafting grid, and the 3x3 Crafting Table grid. Implements exact
 * Minecraft stack manipulation (pick full stack, split stack in half, place
 * single item, shift-click transfer), recipe pattern matching, and UI updates.
 */

import { BLOCKS, ITEMS, REGISTRY, RECIPES } from './constants.js';

export const MAX_STACK_SIZE = 64;

export class Inventory {
  constructor(soundManager, textureManager) {
    this.sound = soundManager;
    this.textures = textureManager;

    // 0..8: Hotbar slots
    // 9..35: Main inventory slots (3 rows x 9 columns)
    this.slots = new Array(36).fill(null).map(() => null);

    // Active hotbar slot index (0..8)
    this.activeSlot = 0;

    // Crafting Grids:
    // 2x2 for player inventory (length 4)
    this.playerCraftGrid = new Array(4).fill(null);
    this.playerCraftOutput = null;

    // 3x3 for Crafting Table (length 9)
    this.tableCraftGrid = new Array(9).fill(null);
    this.tableCraftOutput = null;

    // Floating item held by mouse cursor during inventory management
    this.cursorStack = null;

    // Current open UI mode: 'none' | 'player' | 'crafting_table'
    this.currentMode = 'none';

    // Populate initial starter supplies for immediate gameplay testing
    this.initStarterItems();

    // DOM UI references
    this.initDOM();
    this.render();
  }

  /**
   * Gives starting essentials (wooden log, dirt, torches, cobblestone)
   */
  initStarterItems() {
    this.slots[0] = { id: BLOCKS.OAK_LOG, count: 16 };
    this.slots[1] = { id: BLOCKS.DIRT, count: 32 };
    this.slots[2] = { id: BLOCKS.COBBLESTONE, count: 16 };
    this.slots[3] = { id: BLOCKS.TORCH, count: 8 };
    this.slots[4] = { id: BLOCKS.SAND, count: 12 };
  }

  getActiveItem() {
    return this.slots[this.activeSlot];
  }

  consumeActiveItem(amount = 1) {
    const item = this.slots[this.activeSlot];
    if (!item) return;
    item.count -= amount;
    if (item.count <= 0) {
      this.slots[this.activeSlot] = null;
    }
    this.render();
  }

  /**
   * Adds an item to the first compatible slot or empty slot in inventory
   */
  addItem(id, count = 1) {
    if (id === BLOCKS.AIR || count <= 0) return 0;
    let remaining = count;

    // 1. First attempt to add to existing non-full stacks
    for (let i = 0; i < this.slots.length; i++) {
      const slot = this.slots[i];
      if (slot && slot.id === id && slot.count < MAX_STACK_SIZE) {
        const canAdd = Math.min(remaining, MAX_STACK_SIZE - slot.count);
        slot.count += canAdd;
        remaining -= canAdd;
        if (remaining <= 0) break;
      }
    }

    // 2. If remaining, put into first empty slot
    if (remaining > 0) {
      for (let i = 0; i < this.slots.length; i++) {
        if (!this.slots[i]) {
          const canAdd = Math.min(remaining, MAX_STACK_SIZE);
          this.slots[i] = { id, count: canAdd };
          remaining -= canAdd;
          if (remaining <= 0) break;
        }
      }
    }

    this.render();
    return count - remaining; // Total added
  }

  setActiveSlot(index) {
    if (index >= 0 && index < 9) {
      this.activeSlot = index;
      this.render();
    }
  }

  /**
   * Evaluates crafting recipes on a given grid (2x2 or 3x3)
   */
  evaluateCrafting(grid, size) {
    // 1. Find bounding box of non-empty items in grid
    let minRow = size, maxRow = -1;
    let minCol = size, maxCol = -1;
    let totalItems = 0;

    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        const item = grid[r * size + c];
        if (item && item.count > 0) {
          totalItems++;
          if (r < minRow) minRow = r;
          if (r > maxRow) maxRow = r;
          if (c < minCol) minCol = c;
          if (c > maxCol) maxCol = c;
        }
      }
    }

    if (totalItems === 0) {
      return null;
    }

    const patternW = maxCol - minCol + 1;
    const patternH = maxRow - minRow + 1;

    // Extract compact normalized pattern
    const pattern = [];
    for (let r = minRow; r <= maxRow; r++) {
      for (let c = minCol; c <= maxCol; c++) {
        const item = grid[r * size + c];
        pattern.push(item && item.count > 0 ? item.id : 0);
      }
    }

    // Match against RECIPES
    for (const recipe of RECIPES) {
      if (recipe.width === patternW && recipe.height === patternH) {
        let match = true;
        for (let i = 0; i < pattern.length; i++) {
          if (recipe.pattern[i] !== pattern[i]) {
            match = false;
            break;
          }
        }
        if (match) {
          return { ...recipe.result };
        }
      }
    }

    return null;
  }

  /**
   * Consumes 1 ingredient from each occupied cell in the crafting grid
   */
  craftItem(isTable = false) {
    const grid = isTable ? this.tableCraftGrid : this.playerCraftGrid;
    const size = isTable ? 3 : 2;
    const output = isTable ? this.tableCraftOutput : this.playerCraftOutput;

    if (!output) return false;

    // Check if cursor can receive crafted item
    if (this.cursorStack) {
      if (this.cursorStack.id !== output.id || this.cursorStack.count + output.count > MAX_STACK_SIZE) {
        return false; // Can't merge or stack is full
      }
      this.cursorStack.count += output.count;
    } else {
      this.cursorStack = { id: output.id, count: output.count };
    }

    // Decrement ingredients
    for (let i = 0; i < grid.length; i++) {
      if (grid[i] && grid[i].count > 0) {
        grid[i].count -= 1;
        if (grid[i].count <= 0) {
          grid[i] = null;
        }
      }
    }

    this.sound.playCraft();

    // Re-evaluate crafting grid
    this.updateCraftingOutput(isTable);
    this.render();
    return true;
  }

  updateCraftingOutput(isTable = false) {
    if (isTable) {
      this.tableCraftOutput = this.evaluateCrafting(this.tableCraftGrid, 3);
    } else {
      this.playerCraftOutput = this.evaluateCrafting(this.playerCraftGrid, 2);
    }
  }

  /**
   * Slot click handler implementing full Minecraft inventory interactions
   */
  handleSlotClick(slotType, index, isRightClick, isShiftClick) {
    this.sound.playClick();

    // Target container reference
    let container = null;
    let isOutput = false;
    let isTable = false;

    if (slotType === 'inventory' || slotType === 'hotbar') {
      container = this.slots;
    } else if (slotType === 'playerCraft') {
      container = this.playerCraftGrid;
    } else if (slotType === 'tableCraft') {
      container = this.tableCraftGrid;
      isTable = true;
    } else if (slotType === 'playerOutput') {
      isOutput = true;
      isTable = false;
    } else if (slotType === 'tableOutput') {
      isOutput = true;
      isTable = true;
    }

    // Crafting Output clicked
    if (isOutput) {
      this.craftItem(isTable);
      return;
    }

    const slotItem = container[index];

    // 1. Shift-click quick move between Hotbar (0..8) and Main Inventory (9..35)
    if (isShiftClick && (slotType === 'inventory' || slotType === 'hotbar')) {
      if (!slotItem) return;
      if (index < 9) {
        // Move from hotbar to main inventory
        for (let i = 9; i < 36; i++) {
          if (!this.slots[i]) {
            this.slots[i] = slotItem;
            this.slots[index] = null;
            break;
          }
        }
      } else {
        // Move from main inventory to hotbar
        for (let i = 0; i < 9; i++) {
          if (!this.slots[i]) {
            this.slots[i] = slotItem;
            this.slots[index] = null;
            break;
          }
        }
      }
      this.render();
      return;
    }

    // 2. Right-click actions
    if (isRightClick) {
      if (!this.cursorStack && slotItem) {
        // Pick up half stack
        const take = Math.ceil(slotItem.count / 2);
        this.cursorStack = { id: slotItem.id, count: take };
        slotItem.count -= take;
        if (slotItem.count <= 0) {
          container[index] = null;
        }
      } else if (this.cursorStack) {
        // Drop single item into slot
        if (!slotItem) {
          container[index] = { id: this.cursorStack.id, count: 1 };
          this.cursorStack.count -= 1;
        } else if (slotItem.id === this.cursorStack.id && slotItem.count < MAX_STACK_SIZE) {
          slotItem.count += 1;
          this.cursorStack.count -= 1;
        }
        if (this.cursorStack.count <= 0) {
          this.cursorStack = null;
        }
      }
    } else {
      // 3. Left-click actions
      if (!this.cursorStack && slotItem) {
        // Pick up entire stack
        this.cursorStack = slotItem;
        container[index] = null;
      } else if (this.cursorStack && !slotItem) {
        // Place entire held stack into empty slot
        container[index] = this.cursorStack;
        this.cursorStack = null;
      } else if (this.cursorStack && slotItem) {
        if (slotItem.id === this.cursorStack.id) {
          // Merge stacks
          const canAdd = Math.min(this.cursorStack.count, MAX_STACK_SIZE - slotItem.count);
          slotItem.count += canAdd;
          this.cursorStack.count -= canAdd;
          if (this.cursorStack.count <= 0) {
            this.cursorStack = null;
          }
        } else {
          // Swap stacks
          const temp = container[index];
          container[index] = this.cursorStack;
          this.cursorStack = temp;
        }
      }
    }

    // Update crafting output if crafting grid was modified
    if (slotType === 'playerCraft') {
      this.updateCraftingOutput(false);
    } else if (slotType === 'tableCraft') {
      this.updateCraftingOutput(true);
    }

    this.render();
  }

  /**
   * Returns any items left in crafting grids back into player inventory on close
   */
  returnCraftingItems() {
    for (let i = 0; i < this.playerCraftGrid.length; i++) {
      if (this.playerCraftGrid[i]) {
        this.addItem(this.playerCraftGrid[i].id, this.playerCraftGrid[i].count);
        this.playerCraftGrid[i] = null;
      }
    }
    for (let i = 0; i < this.tableCraftGrid.length; i++) {
      if (this.tableCraftGrid[i]) {
        this.addItem(this.tableCraftGrid[i].id, this.tableCraftGrid[i].count);
        this.tableCraftGrid[i] = null;
      }
    }
    if (this.cursorStack) {
      this.addItem(this.cursorStack.id, this.cursorStack.count);
      this.cursorStack = null;
    }
    this.updateCraftingOutput(false);
    this.updateCraftingOutput(true);
    this.render();
  }

  toggleInventory() {
    if (this.currentMode === 'player') {
      this.closeModal();
    } else {
      this.openModal('player');
    }
  }

  openCraftingTable() {
    this.openModal('crafting_table');
  }

  openModal(mode) {
    this.currentMode = mode;
    document.exitPointerLock?.();
    const modal = document.getElementById('inventory-modal');
    const playerView = document.getElementById('player-craft-view');
    const tableView = document.getElementById('table-craft-view');

    if (modal) modal.style.display = 'flex';
    if (playerView) playerView.style.display = mode === 'player' ? 'flex' : 'none';
    if (tableView) tableView.style.display = mode === 'crafting_table' ? 'flex' : 'none';

    this.render();
  }

  closeModal() {
    this.currentMode = 'none';
    this.returnCraftingItems();
    const modal = document.getElementById('inventory-modal');
    if (modal) modal.style.display = 'none';
  }

  initDOM() {
    // Number keys 1-9 for hotbar selection
    window.addEventListener('keydown', (e) => {
      if (e.key >= '1' && e.key <= '9') {
        const slotIdx = parseInt(e.key, 10) - 1;
        this.setActiveSlot(slotIdx);
      }
      // Toggle inventory with 'KeyE'
      if (e.code === 'KeyE') {
        if (this.currentMode !== 'none') {
          this.closeModal();
        } else {
          this.openModal('player');
        }
      }
      // Close on Escape
      if (e.code === 'Escape' && this.currentMode !== 'none') {
        this.closeModal();
      }
    });

    // Mouse wheel for scrolling hotbar slots
    window.addEventListener('wheel', (e) => {
      if (document.pointerLockElement) {
        if (e.deltaY > 0) {
          this.setActiveSlot((this.activeSlot + 1) % 9);
        } else if (e.deltaY < 0) {
          this.setActiveSlot((this.activeSlot + 8) % 9);
        }
      }
    });

    // Floating cursor follower for held stack
    window.addEventListener('mousemove', (e) => {
      const cursorElem = document.getElementById('cursor-held-item');
      if (cursorElem) {
        if (this.cursorStack && this.currentMode !== 'none') {
          cursorElem.style.display = 'flex';
          cursorElem.style.left = `${e.clientX + 10}px`;
          cursorElem.style.top = `${e.clientY + 10}px`;
          const icon = this.textures.getItemIconUrl(this.cursorStack.id);
          cursorElem.querySelector('img').src = icon;
          cursorElem.querySelector('.item-count').textContent =
            this.cursorStack.count > 1 ? this.cursorStack.count : '';
        } else {
          cursorElem.style.display = 'none';
        }
      }
    });

    // Modal close button
    const closeBtn = document.getElementById('close-inventory-btn');
    if (closeBtn) {
      closeBtn.addEventListener('click', () => this.closeModal());
    }

    // Build static DOM slots
    this.buildHotbarDOM();
    this.buildInventoryGridDOM();
    this.buildCraftingDOM();
  }

  buildHotbarDOM() {
    const container = document.getElementById('hotbar-slots');
    if (!container) return;
    container.innerHTML = '';
    for (let i = 0; i < 9; i++) {
      const slot = document.createElement('div');
      slot.className = `slot hotbar-slot ${i === this.activeSlot ? 'active' : ''}`;
      slot.dataset.index = i;
      slot.innerHTML = `<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="" class="item-icon" /><span class="item-count"></span>`;
      slot.addEventListener('click', (e) => {
        this.setActiveSlot(i);
        if (this.currentMode !== 'none') {
          this.handleSlotClick('hotbar', i, e.button === 2, e.shiftKey);
        }
      });
      container.appendChild(slot);
    }
  }

  buildInventoryGridDOM() {
    const mainGrid = document.getElementById('main-inventory-grid');
    const modalHotbar = document.getElementById('modal-hotbar-grid');
    if (mainGrid) {
      mainGrid.innerHTML = '';
      for (let i = 9; i < 36; i++) {
        const slot = document.createElement('div');
        slot.className = 'slot inv-slot';
        slot.dataset.index = i;
        slot.innerHTML = `<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="" class="item-icon" /><span class="item-count"></span>`;
        slot.addEventListener('mousedown', (e) => {
          e.preventDefault();
          this.handleSlotClick('inventory', i, e.button === 2, e.shiftKey);
        });
        slot.addEventListener('contextmenu', (e) => e.preventDefault());
        mainGrid.appendChild(slot);
      }
    }
    if (modalHotbar) {
      modalHotbar.innerHTML = '';
      for (let i = 0; i < 9; i++) {
        const slot = document.createElement('div');
        slot.className = `slot inv-slot ${i === this.activeSlot ? 'active' : ''}`;
        slot.dataset.index = i;
        slot.innerHTML = `<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="" class="item-icon" /><span class="item-count"></span>`;
        slot.addEventListener('mousedown', (e) => {
          e.preventDefault();
          this.handleSlotClick('hotbar', i, e.button === 2, e.shiftKey);
        });
        slot.addEventListener('contextmenu', (e) => e.preventDefault());
        modalHotbar.appendChild(slot);
      }
    }
  }

  buildCraftingDOM() {
    // 2x2 Player Crafting Grid
    const pGrid = document.getElementById('player-craft-2x2');
    if (pGrid) {
      pGrid.innerHTML = '';
      for (let i = 0; i < 4; i++) {
        const slot = document.createElement('div');
        slot.className = 'slot craft-slot';
        slot.dataset.index = i;
        slot.innerHTML = `<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="" class="item-icon" /><span class="item-count"></span>`;
        slot.addEventListener('mousedown', (e) => {
          e.preventDefault();
          this.handleSlotClick('playerCraft', i, e.button === 2, e.shiftKey);
        });
        slot.addEventListener('contextmenu', (e) => e.preventDefault());
        pGrid.appendChild(slot);
      }
    }

    // 2x2 Output Slot
    const pOut = document.getElementById('player-craft-output');
    if (pOut) {
      pOut.addEventListener('mousedown', (e) => {
        e.preventDefault();
        this.handleSlotClick('playerOutput', 0, false, false);
      });
      pOut.addEventListener('contextmenu', (e) => e.preventDefault());
    }

    // 3x3 Table Crafting Grid
    const tGrid = document.getElementById('table-craft-3x3');
    if (tGrid) {
      tGrid.innerHTML = '';
      for (let i = 0; i < 9; i++) {
        const slot = document.createElement('div');
        slot.className = 'slot craft-slot';
        slot.dataset.index = i;
        slot.innerHTML = `<img src="data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7" alt="" class="item-icon" /><span class="item-count"></span>`;
        slot.addEventListener('mousedown', (e) => {
          e.preventDefault();
          this.handleSlotClick('tableCraft', i, e.button === 2, e.shiftKey);
        });
        slot.addEventListener('contextmenu', (e) => e.preventDefault());
        tGrid.appendChild(slot);
      }
    }

    // 3x3 Table Output Slot
    const tOut = document.getElementById('table-craft-output');
    if (tOut) {
      tOut.addEventListener('mousedown', (e) => {
        e.preventDefault();
        this.handleSlotClick('tableOutput', 0, false, false);
      });
      tOut.addEventListener('contextmenu', (e) => e.preventDefault());
    }
  }

  updateSlotElement(slotElem, item) {
    if (!slotElem) return;
    const img = slotElem.querySelector('.item-icon');
    const count = slotElem.querySelector('.item-count');

    if (item && item.count > 0) {
      const url = this.textures.getItemIconUrl(item.id);
      img.src = url;
      img.style.display = 'block';
      count.textContent = item.count > 1 ? item.count : '';
      count.style.display = item.count > 1 ? 'block' : 'none';
      slotElem.title = REGISTRY[item.id]?.name || 'Item';
    } else {
      img.src = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';
      img.style.display = 'none';
      count.textContent = '';
      count.style.display = 'none';
      slotElem.title = '';
    }
  }

  /**
   * Syncs all UI slot representations with the current inventory memory state
   */
  render() {
    // 1. Bottom screen Hotbar
    const hotbarSlots = document.querySelectorAll('#hotbar-slots .slot');
    hotbarSlots.forEach((slot, i) => {
      slot.classList.toggle('active', i === this.activeSlot);
      this.updateSlotElement(slot, this.slots[i]);
    });

    // 2. Modal Hotbar
    const modalHotbar = document.querySelectorAll('#modal-hotbar-grid .slot');
    modalHotbar.forEach((slot, i) => {
      slot.classList.toggle('active', i === this.activeSlot);
      this.updateSlotElement(slot, this.slots[i]);
    });

    // 3. Modal Main Inventory
    const mainSlots = document.querySelectorAll('#main-inventory-grid .slot');
    mainSlots.forEach((slot, i) => {
      this.updateSlotElement(slot, this.slots[9 + i]);
    });

    // 4. Player 2x2 Crafting Grid & Output
    const pCraftSlots = document.querySelectorAll('#player-craft-2x2 .slot');
    pCraftSlots.forEach((slot, i) => {
      this.updateSlotElement(slot, this.playerCraftGrid[i]);
    });
    const pOutSlot = document.getElementById('player-craft-output');
    this.updateSlotElement(pOutSlot, this.playerCraftOutput);

    // 5. Table 3x3 Crafting Grid & Output
    const tCraftSlots = document.querySelectorAll('#table-craft-3x3 .slot');
    tCraftSlots.forEach((slot, i) => {
      this.updateSlotElement(slot, this.tableCraftGrid[i]);
    });
    const tOutSlot = document.getElementById('table-craft-output');
    this.updateSlotElement(tOutSlot, this.tableCraftOutput);
  }
}
