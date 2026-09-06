/**
 * @fileoverview ShilpBarterLedger - Ancient Indian Subcontinent Barter Economics Engine
 * @module shilploka/economy/barter_ledger
 * 
 * Historical commodity exchange ledger without fiat paper currency:
 * 1. Spices: Elaichi (Cardamom), Maricha (Black Pepper - Black Gold), Kesar (Kashmiri Saffron).
 * 2. Metals & Gems: Kansa (Bronze Ingots - Copper/Tin alloy), Badakhshan Lajward (Lapis Lazuli).
 * 3. Tools: Kansa Khanitra (Bronze Pickaxe).
 */

export const BARTER_COMMODITIES = {
  CARDAMOM: {
    id: 'CARDAMOM',
    name: 'Cardamom (एला / इलायची)',
    category: 'SPICE',
    baseValueUnits: 10, // 2 Cardamom = 1 Bronze
    icon: '🌿',
  },
  PEPPER: {
    id: 'PEPPER',
    name: 'Black Pepper (मरिच / काला सोना)',
    category: 'SPICE',
    baseValueUnits: 4,  // 5 Pepper = 1 Bronze
    icon: '⚫',
  },
  SAFFRON: {
    id: 'SAFFRON',
    name: 'Kashmiri Saffron (कुंकुम / केसर)',
    category: 'SPICE',
    baseValueUnits: 40, // 1 Saffron = 2 Bronze
    icon: '🌸',
  },
  BRONZE_INGOT: {
    id: 'BRONZE_INGOT',
    name: 'Kansa Ingot (कांस्य पिण्ड)',
    category: 'METAL',
    baseValueUnits: 20, // Standard Bronze Age unit of account
    icon: '🔶',
  },
  LAJWARD_GEM: {
    id: 'LAJWARD_GEM',
    name: 'Badakhshan Lajward (लाजवर्द - Lapis Lazuli)',
    category: 'GEM',
    baseValueUnits: 60, // 1 Lapis = 3 Bronze
    icon: '💎',
  },
  BRONZE_PICKAXE: {
    id: 'BRONZE_PICKAXE',
    name: 'Kansa Bronze Pickaxe (कांस्य खनित्र)',
    category: 'TOOL',
    baseValueUnits: 80, // 4 Bronze Ingots
    icon: '⛏️',
  },
};

export class ShilpBarterLedger {
  /**
   * Calculates how many units of targetCommodity are yielded by offerAmount of offerCommodity.
   * 
   * @param {string} offerId - Key from BARTER_COMMODITIES.
   * @param {number} offerAmount - Units offered.
   * @param {string} targetId - Desired commodity key.
   * @returns {{ yieldAmount: number, remainderValue: number, exchangeRatio: number }}
   */
  static calculateExchange(offerId, offerAmount, targetId) {
    const offerMeta = BARTER_COMMODITIES[offerId];
    const targetMeta = BARTER_COMMODITIES[targetId];
    if (!offerMeta || !targetMeta || offerAmount <= 0) {
      return { yieldAmount: 0, remainderValue: 0, exchangeRatio: 0 };
    }

    const totalOfferedValue = offerAmount * offerMeta.baseValueUnits;
    const yieldAmount = Math.floor(totalOfferedValue / targetMeta.baseValueUnits);
    const remainderValue = totalOfferedValue % targetMeta.baseValueUnits;
    const exchangeRatio = offerMeta.baseValueUnits / targetMeta.baseValueUnits;

    return { yieldAmount, remainderValue, exchangeRatio };
  }

  /**
   * Executes an atomic barter trade between a merchant and player.
   * 
   * @param {Object} merchantStock - Map or object of merchant stock: { cardamom, pepper, saffron, bronze, ... }
   * @param {Object} playerStock - Map or object of player stock.
   * @param {string} offerKey - Property name in stocks.
   * @param {number} offerAmount - Units offered by player.
   * @param {string} targetKey - Property name in stocks desired.
   * @returns {{ success: boolean, message: string, receipt: Object|null }}
   */
  static executeTransaction(merchantStock, playerStock, offerKey, offerAmount, targetKey) {
    const offerCommodity = Object.values(BARTER_COMMODITIES).find(
      c => c.id.toLowerCase().includes(offerKey.toLowerCase()) || offerKey.toLowerCase().includes(c.id.toLowerCase())
    );
    const targetCommodity = Object.values(BARTER_COMMODITIES).find(
      c => c.id.toLowerCase().includes(targetKey.toLowerCase()) || targetKey.toLowerCase().includes(c.id.toLowerCase())
    );

    if (!offerCommodity || !targetCommodity) {
      return { success: false, message: 'Invalid commodity specification.', receipt: null };
    }

    if ((playerStock[offerKey] ?? 0) < offerAmount) {
      return {
        success: false,
        message: `Insufficient ${offerCommodity.name}. Player has ${playerStock[offerKey] ?? 0}, needed ${offerAmount}.`,
        receipt: null,
      };
    }

    const { yieldAmount } = this.calculateExchange(offerCommodity.id, offerAmount, targetCommodity.id);
    if (yieldAmount <= 0) {
      return {
        success: false,
        message: `Offered quantity of ${offerCommodity.name} is insufficient to barter for 1 unit of ${targetCommodity.name}.`,
        receipt: null,
      };
    }

    if ((merchantStock[targetKey] ?? 0) < yieldAmount) {
      return {
        success: false,
        message: `Merchant has insufficient ${targetCommodity.name}. In stock: ${merchantStock[targetKey] ?? 0}, requested: ${yieldAmount}.`,
        receipt: null,
      };
    }

    // Atomic Balance Mutation
    playerStock[offerKey] -= offerAmount;
    merchantStock[offerKey] = (merchantStock[offerKey] ?? 0) + offerAmount;

    merchantStock[targetKey] -= yieldAmount;
    playerStock[targetKey] = (playerStock[targetKey] ?? 0) + yieldAmount;

    const receipt = {
      timestamp: Date.now(),
      offered: { name: offerCommodity.name, amount: offerAmount },
      received: { name: targetCommodity.name, amount: yieldAmount },
      settledRatio: `${offerAmount} ${offerCommodity.name} ➔ ${yieldAmount} ${targetCommodity.name}`,
    };

    return {
      success: true,
      message: `वस्तु-विनिमय सम्पन्न: Exchanged ${offerAmount} ${offerCommodity.name} for ${yieldAmount} ${targetCommodity.name}.`,
      receipt,
    };
  }
}
