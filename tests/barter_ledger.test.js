/**
 * Tests for the barter economy: src/shilploka/economy/barter_ledger.js.
 *
 * Vastu-vinimaya (वस्तु-विनिमय): goods are exchanged by relative value, with no
 * currency. Each commodity has a baseValueUnits; a trade converts the value
 * offered into whole units of the commodity wanted.
 */
import { describe, expect, it } from 'vitest';

import { BARTER_COMMODITIES, ShilpBarterLedger } from '../src/shilploka/economy/barter_ledger.js';

describe('ShilpBarterLedger.calculateExchange', () => {
  it('converts by relative value: 2 cardamom (2 x 10) buys 1 bronze (20)', () => {
    const r = ShilpBarterLedger.calculateExchange('CARDAMOM', 2, 'BRONZE_INGOT');
    expect(r.yieldAmount).toBe(1);
    expect(r.remainderValue).toBe(0);
  });

  it('rounds down and reports the value left over', () => {
    // 7 pepper x 4 = 28 units; bronze costs 20 -> 1 bronze, 8 units unspent.
    const r = ShilpBarterLedger.calculateExchange('PEPPER', 7, 'BRONZE_INGOT');
    expect(r.yieldAmount).toBe(1);
    expect(r.remainderValue).toBe(8);
  });

  it('a valuable good buys several of a cheap one: 1 saffron = 10 pepper', () => {
    expect(ShilpBarterLedger.calculateExchange('SAFFRON', 1, 'PEPPER').yieldAmount).toBe(10);
  });

  it('yields nothing for unknown commodities or a non-positive offer', () => {
    expect(ShilpBarterLedger.calculateExchange('GOLD_COIN', 5, 'PEPPER').yieldAmount).toBe(0);
    expect(ShilpBarterLedger.calculateExchange('PEPPER', 0, 'SAFFRON').yieldAmount).toBe(0);
  });

  it('never creates value out of thin air (yield x price <= value offered)', () => {
    const ids = Object.keys(BARTER_COMMODITIES);
    for (const offer of ids) {
      for (const target of ids) {
        for (const amount of [1, 3, 10, 37]) {
          const { yieldAmount } = ShilpBarterLedger.calculateExchange(offer, amount, target);
          const offered = amount * BARTER_COMMODITIES[offer].baseValueUnits;
          const received = yieldAmount * BARTER_COMMODITIES[target].baseValueUnits;
          expect(received).toBeLessThanOrEqual(offered);
        }
      }
    }
  });
});

describe('ShilpBarterLedger.executeTransaction', () => {
  // Stock objects use the same short keys the engine gives Dhanapati.
  const fresh = () => ({
    merchant: { cardamom: 40, pepper: 80, saffron: 15, bronze: 25 },
    player: { cardamom: 24, pepper: 40, saffron: 8, bronze: 12 },
  });

  it('completes a basic trade and moves goods both ways', () => {
    const { merchant, player } = fresh();
    const res = ShilpBarterLedger.executeTransaction(merchant, player, 'cardamom', 4, 'bronze');

    expect(res.success).toBe(true);
    // Player: -4 cardamom, +2 bronze.   Merchant: +4 cardamom, -2 bronze.
    expect(player).toMatchObject({ cardamom: 20, bronze: 14 });
    expect(merchant).toMatchObject({ cardamom: 44, bronze: 23 });
    expect(res.receipt.received.amount).toBe(2);
  });

  it('conserves every good: nothing is created or destroyed by a trade', () => {
    const { merchant, player } = fresh();
    const total = k => merchant[k] + player[k];
    const before = { cardamom: total('cardamom'), bronze: total('bronze') };
    ShilpBarterLedger.executeTransaction(merchant, player, 'cardamom', 6, 'bronze');
    expect(total('cardamom')).toBe(before.cardamom);
    expect(total('bronze')).toBe(before.bronze);
  });

  // The affordability check, isolated. The merchant CAN supply the 15 bronze
  // this would yield, so only the player's own shortfall (24 cardamom, 30
  // offered) can refuse it. An earlier version of this test offered 999, which
  // the merchant-stock check also rejects -- so it passed even with the
  // affordability check deleted, and guarded nothing. Without the check this
  // trade would drive the player's cardamom to -6.
  it('refuses a trade the player cannot afford, even if the merchant could supply it', () => {
    const { merchant, player } = fresh();
    expect(ShilpBarterLedger.calculateExchange('CARDAMOM', 30, 'BRONZE_INGOT').yieldAmount)
      .toBeLessThanOrEqual(merchant.bronze);          // merchant is not the limit
    const snap = JSON.stringify({ merchant, player });
    const res = ShilpBarterLedger.executeTransaction(merchant, player, 'cardamom', 30, 'bronze');
    expect(res.success).toBe(false);
    expect(player.cardamom).toBe(24);                 // never negative
    expect(JSON.stringify({ merchant, player })).toBe(snap);
  });

  // For each refusal the key property is ATOMICITY: a failed trade must leave
  // both stocks exactly as they were, never half-applied.
  it.each([
    ['the offer is worth less than one unit', 'pepper', 1, 'saffron'],
    ['the commodity does not exist', 'gold', 5, 'bronze'],
  ])('refuses and changes nothing when %s', (_why, offer, amount, target) => {
    const { merchant, player } = fresh();
    const snap = JSON.stringify({ merchant, player });
    const res = ShilpBarterLedger.executeTransaction(merchant, player, offer, amount, target);
    expect(res.success).toBe(false);
    expect(res.message).toBeTruthy();
    expect(JSON.stringify({ merchant, player })).toBe(snap);
  });

  it('refuses when the merchant has run out', () => {
    const { merchant, player } = fresh();
    merchant.saffron = 0;
    const snap = JSON.stringify({ merchant, player });
    const res = ShilpBarterLedger.executeTransaction(merchant, player, 'bronze', 4, 'saffron');
    expect(res.success).toBe(false);
    expect(JSON.stringify({ merchant, player })).toBe(snap);
  });
});
