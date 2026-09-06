/**
 * @fileoverview ShilpCraftingModal - Vedic Crafting Altar (निर्माण पीठ) UI
 * @module shilploka/ui/shilp_crafting_modal
 */

import { VEDIC_RECIPES, SHILP_ITEMS } from '../inventory/shilp_inventory.js';

export class ShilpCraftingModal {
  /**
   * @param {HTMLElement} rootContainer - Modal container element.
   * @param {import('../inventory/shilp_inventory.js').ShilpInventory} inventory - Inventory instance.
   */
  constructor(rootContainer, inventory) {
    this.container = rootContainer;
    this.inventory = inventory;
    this.isOpen = false;

    this.renderModal();
    this.bindEvents();
  }

  renderModal() {
    this.container.innerHTML = `
      <div class="crafting-modal-backdrop">
        <div class="crafting-card">
          <div class="crafting-header">
            <div class="crafting-title">
              <span class="chakra-icon">☸</span>
              <div>
                <h2>वैदिक निर्माण पीठ • Vedic Crafting Altar</h2>
                <span class="crafting-sub">Transform Indus Timber, Minerals & Bronze into Sacred Architecture & Tools</span>
              </div>
            </div>
            <button id="crafting-close-btn" class="learn-close-btn">&times;</button>
          </div>

          <div class="crafting-body">
            <div id="crafting-recipes-list" class="recipes-grid"></div>
          </div>
        </div>
      </div>
    `;

    this.renderRecipes();
  }

  renderRecipes() {
    const listElem = document.getElementById('crafting-recipes-list');
    if (!listElem) return;

    listElem.innerHTML = '';

    for (const recipe of VEDIC_RECIPES) {
      const resultMeta = SHILP_ITEMS[recipe.result.itemId];
      const canCraft = recipe.ingredients.every(
        ing => this.inventory.countItem(ing.itemId) >= ing.count
      );

      const ingredientsHtml = recipe.ingredients.map(ing => {
        const ingMeta = SHILP_ITEMS[ing.itemId];
        const hasCount = this.inventory.countItem(ing.itemId);
        const color = hasCount >= ing.count ? '#2ecc71' : '#e74c3c';
        return `
          <div class="ing-item">
            <span>${ingMeta?.icon || '📦'} ${ingMeta?.name || ing.itemId}:</span>
            <span style="color: ${color}; font-weight: bold;">${hasCount}/${ing.count}</span>
          </div>
        `;
      }).join('');

      const card = document.createElement('div');
      card.className = `recipe-card ${canCraft ? 'craftable' : 'locked'}`;
      card.innerHTML = `
        <div class="recipe-card-header">
          <span class="recipe-result-icon">${resultMeta?.icon || '📦'}</span>
          <div>
            <h4>${recipe.name}</h4>
            <span class="recipe-yield">Yields: ${recipe.result.count}x ${resultMeta?.name}</span>
          </div>
        </div>
        <p class="recipe-desc">${recipe.description}</p>
        <div class="recipe-ingredients-box">
          <div class="ing-title">Required Materials:</div>
          ${ingredientsHtml}
        </div>
        <button class="craft-btn ${canCraft ? 'btn-active' : 'btn-disabled'}" data-recipe="${recipe.id}">
          ${canCraft ? '☸ निर्माण करें (Craft)' : 'अपर्याप्त सामग्री (Missing Materials)'}
        </button>
      `;

      const craftBtn = card.querySelector('.craft-btn');
      if (craftBtn && canCraft) {
        craftBtn.addEventListener('click', () => {
          const res = this.inventory.craftRecipe(recipe.id);
          this.renderRecipes();
          const alertBanner = document.getElementById('monument-alert');
          if (alertBanner) {
            alertBanner.textContent = res.message;
            alertBanner.classList.remove('hidden');
            setTimeout(() => alertBanner.classList.add('hidden'), 2500);
          }
        });
      }

      listElem.appendChild(card);
    }
  }

  bindEvents() {
    const closeBtn = document.getElementById('crafting-close-btn');
    if (closeBtn) closeBtn.addEventListener('click', () => this.toggle(false));
  }

  toggle(forceState) {
    this.isOpen = (forceState !== undefined) ? forceState : !this.isOpen;
    if (this.isOpen) {
      this.container.classList.remove('hidden');
      this.renderRecipes();
      if (document.exitPointerLock) document.exitPointerLock();
    } else {
      this.container.classList.add('hidden');
    }
  }
}
