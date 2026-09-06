/**
 * @fileoverview ShilpBarterModal - Ancient Indian Barter Exchange UI
 * @module shilploka/ui/shilp_barter_modal
 */

import { BARTER_COMMODITIES, ShilpBarterLedger } from '../economy/barter_ledger.js';

export class ShilpBarterModal {
  /**
   * @param {HTMLElement} rootContainer - Modal mounting root.
   * @param {import('../inventory/shilp_inventory.js').ShilpInventory} inventory - Player inventory.
   * @param {Object} merchantStock - Dhanapati's merchant stock.
   */
  constructor(rootContainer, inventory, merchantStock) {
    this.container = rootContainer;
    this.inventory = inventory;
    this.merchantStock = merchantStock;
    this.isOpen = false;

    this.selectedOffer = 'CARDAMOM';
    this.selectedTarget = 'BRONZE_INGOT';
    this.offerQty = 2;

    this.renderModal();
    this.bindEvents();
  }

  renderModal() {
    this.container.innerHTML = `
      <div class="barter-modal-backdrop">
        <div class="barter-card">
          <div class="barter-header">
            <div class="barter-title">
              <span class="chakra-icon">🏺</span>
              <div>
                <h2>धनपति वस्तु-विनिमय • Ancient Indian Barter Ledger</h2>
                <span class="barter-sub">Direct Commodity Exchange: Spices (Elaichi, Maricha, Kesar) & Kansa Bronze</span>
              </div>
            </div>
            <button id="barter-close-btn" class="learn-close-btn">&times;</button>
          </div>

          <div class="barter-body">
            <!-- Left: Merchant Stock -->
            <div class="barter-column">
              <h3 class="col-title">🏛️ Merchant Stock (धनपति भण्डार)</h3>
              <div id="merchant-stock-list" class="stock-list"></div>
            </div>

            <!-- Center: Trade Configuration & Ledger Exchange -->
            <div class="barter-exchange-box">
              <h3 class="col-title">⚖️ Trade Ledger (विनिमय गणना)</h3>
              
              <div class="trade-field">
                <label>You Offer (आपकी वस्तु):</label>
                <select id="barter-offer-select" class="retro-select">
                  <option value="CARDAMOM">🌿 Cardamom (एला - 2 : 1 Bronze)</option>
                  <option value="PEPPER">⚫ Black Pepper (मरिच - 5 : 1 Bronze)</option>
                  <option value="SAFFRON">🌸 Kashmiri Saffron (केसर - 1 : 2 Bronze)</option>
                  <option value="BRONZE_INGOT">🔶 Kansa Bronze Ingot</option>
                  <option value="LAJWARD_GEM">💎 Badakhshan Lajward (Lapis)</option>
                </select>
                <div class="qty-control">
                  <button id="qty-minus" class="qty-btn">-</button>
                  <span id="qty-display" class="qty-num">${this.offerQty}</span>
                  <button id="qty-plus" class="qty-btn">+</button>
                </div>
              </div>

              <div class="trade-arrow">⬇️ Fair Commodity Value ⬇️</div>

              <div class="trade-field">
                <label>You Receive (प्राप्त वस्तु):</label>
                <select id="barter-target-select" class="retro-select">
                  <option value="BRONZE_INGOT">🔶 Kansa Bronze Ingot (कांस्य)</option>
                  <option value="BRONZE_PICKAXE">⛏️ Kansa Bronze Pickaxe (खनित्र)</option>
                  <option value="CARDAMOM">🌿 Cardamom (एला)</option>
                  <option value="PEPPER">⚫ Black Pepper (मरिच)</option>
                  <option value="SAFFRON">🌸 Kashmiri Saffron (केसर)</option>
                </select>
              </div>

              <div id="exchange-summary" class="exchange-summary"></div>

              <button id="execute-barter-btn" class="barter-action-btn">वस्तु-विनिमय सम्पन्न करें (Execute Barter)</button>
              <div id="barter-toast" class="barter-toast-msg"></div>
            </div>

            <!-- Right: Player Bag -->
            <div class="barter-column">
              <h3 class="col-title">🎒 Player Pouch (खिलाड़ी की थैली)</h3>
              <div id="player-stock-list" class="stock-list"></div>
            </div>
          </div>
        </div>
      </div>
    `;

    this.updateStockViews();
    this.updateExchangeSummary();
  }

  updateStockViews() {
    const merchantList = document.getElementById('merchant-stock-list');
    const playerList = document.getElementById('player-stock-list');
    if (!merchantList || !playerList) return;

    merchantList.innerHTML = Object.entries(this.merchantStock)
      .map(([k, v]) => `<div class="stock-item"><span class="stock-name">${k}:</span> <span class="stock-val">${v}</span></div>`)
      .join('');

    const playerStock = this.inventory.getCommodityStock();
    playerList.innerHTML = Object.entries(playerStock)
      .map(([k, v]) => `<div class="stock-item"><span class="stock-name">${k}:</span> <span class="stock-val">${v}</span></div>`)
      .join('');
  }

  updateExchangeSummary() {
    const summaryElem = document.getElementById('exchange-summary');
    if (!summaryElem) return;

    const { yieldAmount, exchangeRatio } = ShilpBarterLedger.calculateExchange(
      this.selectedOffer,
      this.offerQty,
      this.selectedTarget
    );

    const offerName = BARTER_COMMODITIES[this.selectedOffer]?.name || this.selectedOffer;
    const targetName = BARTER_COMMODITIES[this.selectedTarget]?.name || this.selectedTarget;

    summaryElem.innerHTML = `
      <div class="calc-row">Offered: <strong>${this.offerQty}x ${offerName}</strong></div>
      <div class="calc-row">Yield: <strong style="color: #2ecc71;">${yieldAmount}x ${targetName}</strong></div>
      <div class="calc-sub">Exchange Rate: 1x = ${exchangeRatio.toFixed(2)}x target value</div>
    `;
  }

  bindEvents() {
    const closeBtn = document.getElementById('barter-close-btn');
    if (closeBtn) closeBtn.addEventListener('click', () => this.toggle(false));

    const offerSelect = document.getElementById('barter-offer-select');
    if (offerSelect) {
      offerSelect.addEventListener('change', (e) => {
        this.selectedOffer = e.target.value;
        this.updateExchangeSummary();
      });
    }

    const targetSelect = document.getElementById('barter-target-select');
    if (targetSelect) {
      targetSelect.addEventListener('change', (e) => {
        this.selectedTarget = e.target.value;
        this.updateExchangeSummary();
      });
    }

    const plusBtn = document.getElementById('qty-plus');
    const minusBtn = document.getElementById('qty-minus');
    const qtyDisplay = document.getElementById('qty-display');

    if (plusBtn) {
      plusBtn.addEventListener('click', () => {
        this.offerQty = Math.min(64, this.offerQty + 1);
        if (qtyDisplay) qtyDisplay.textContent = this.offerQty;
        this.updateExchangeSummary();
      });
    }

    if (minusBtn) {
      minusBtn.addEventListener('click', () => {
        this.offerQty = Math.max(1, this.offerQty - 1);
        if (qtyDisplay) qtyDisplay.textContent = this.offerQty;
        this.updateExchangeSummary();
      });
    }

    const tradeBtn = document.getElementById('execute-barter-btn');
    const toast = document.getElementById('barter-toast');

    if (tradeBtn) {
      tradeBtn.addEventListener('click', () => {
        const offerKey = this.selectedOffer.toLowerCase();
        const targetKey = this.selectedTarget.toLowerCase();

        // Check if player has item
        const offerCommodity = BARTER_COMMODITIES[this.selectedOffer];
        const targetCommodity = BARTER_COMMODITIES[this.selectedTarget];
        if (!offerCommodity || !targetCommodity) return;

        const count = this.inventory.countItem(this.selectedOffer);
        const { yieldAmount } = ShilpBarterLedger.calculateExchange(this.selectedOffer, this.offerQty, this.selectedTarget);

        if (count < this.offerQty) {
          if (toast) {
            toast.textContent = `❌ Insufficient ${offerCommodity.name} in your bag!`;
            toast.style.color = '#e74c3c';
          }
          return;
        }

        if (yieldAmount <= 0) {
          if (toast) {
            toast.textContent = `⚠️ Offered quantity too low for 1 unit.`;
            toast.style.color = '#f39c12';
          }
          return;
        }

        // Execute transaction
        this.inventory.consumeItem(this.selectedOffer, this.offerQty);
        this.inventory.addItem(this.selectedTarget, yieldAmount);

        // Mutate merchant stock
        this.merchantStock[offerKey] = (this.merchantStock[offerKey] ?? 0) + this.offerQty;
        this.merchantStock[targetKey] = Math.max(0, (this.merchantStock[targetKey] ?? 0) - yieldAmount);

        this.updateStockViews();
        this.updateExchangeSummary();

        if (toast) {
          toast.textContent = `✓ विनिमय सम्पन्न: Traded ${this.offerQty}x for ${yieldAmount}x ${targetCommodity.name}!`;
          toast.style.color = '#2ecc71';
        }
      });
    }
  }

  toggle(forceState) {
    this.isOpen = (forceState !== undefined) ? forceState : !this.isOpen;
    if (this.isOpen) {
      this.container.classList.remove('hidden');
      this.updateStockViews();
      this.updateExchangeSummary();
      if (document.exitPointerLock) document.exitPointerLock();
    } else {
      this.container.classList.add('hidden');
    }
  }
}
