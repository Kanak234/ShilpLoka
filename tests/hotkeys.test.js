/**
 * Tests for the keyboard shortcuts: src/shilploka/input/hotkeys.js.
 *
 * The engine registers exactly createHotkeyHandler(...) on window 'keydown',
 * so these tests drive the same function with plain event objects. Focus is
 * injected (getActiveElement) instead of needing a browser DOM.
 */
import { describe, expect, it, vi } from 'vitest';

import { HOTKEYS, createHotkeyHandler, isTextEntry, resolveHotkey } from '../src/shilploka/input/hotkeys.js';

/** A keydown event with only the fields the resolver reads. */
const key = (code, extra = {}) => ({ code, key: code.replace(/^Key/, '').toLowerCase(), ...extra });

/** A focusable element stand-in. */
const el = (tagName, extra = {}) => ({ tagName, isContentEditable: false, ...extra });

describe('hotkeys', () => {
  describe('the review\'s three keys', () => {
    it.each([
      ['KeyK', 'quickSave'],
      ['KeyL', 'quickLoad'],
      ['KeyE', 'toggleInventory'],
    ])('%s -> %s', (code, action) => {
      expect(resolveHotkey(key(code))).toEqual({ action });
    });

    it('each calls its handler exactly once per press', () => {
      const handlers = { quickSave: vi.fn(), quickLoad: vi.fn(), toggleInventory: vi.fn() };
      const onKey = createHotkeyHandler(handlers, () => null);
      onKey(key('KeyK'));
      onKey(key('KeyL'));
      onKey(key('KeyE'));
      expect(handlers.quickSave).toHaveBeenCalledTimes(1);
      expect(handlers.quickLoad).toHaveBeenCalledTimes(1);
      expect(handlers.toggleInventory).toHaveBeenCalledTimes(1);
    });
  });

  describe('never while typing', () => {
    // The barter modal has <input type="number"> fields; any future text box
    // must not save or load the game on every "k" or "l".
    it.each([
      ['a text input', el('INPUT', { type: 'text' })],
      ['a number input (barter quantity)', el('INPUT', { type: 'number' })],
      ['a textarea', el('TEXTAREA')],
      ['a select', el('SELECT')],
      ['a contenteditable element', el('DIV', { isContentEditable: true })],
    ])('ignores K, L and E when %s has focus', (_label, focused) => {
      const handlers = { quickSave: vi.fn(), quickLoad: vi.fn(), toggleInventory: vi.fn() };
      const onKey = createHotkeyHandler(handlers, () => focused);
      for (const code of ['KeyK', 'KeyL', 'KeyE']) expect(onKey(key(code))).toBe(false);
      expect(handlers.quickSave).not.toHaveBeenCalled();
      expect(handlers.quickLoad).not.toHaveBeenCalled();
      expect(handlers.toggleInventory).not.toHaveBeenCalled();
    });

    it('still works when a button or the page has focus', () => {
      expect(isTextEntry(el('BUTTON'))).toBe(false);
      expect(isTextEntry(el('CANVAS'))).toBe(false);
      expect(isTextEntry(null)).toBe(false);
      expect(resolveHotkey(key('KeyK'), el('BUTTON'))).toEqual({ action: 'quickSave' });
    });
  });

  describe('leaves browser and OS shortcuts alone', () => {
    it.each([
      ['Ctrl+L (address bar)', { ctrlKey: true }],
      ['Cmd+K', { metaKey: true }],
      ['Alt+E (menu)', { altKey: true }],
    ])('%s does nothing in the game', (_label, mods) => {
      for (const code of ['KeyK', 'KeyL', 'KeyE']) expect(resolveHotkey(key(code, mods))).toBeNull();
    });

    it('Shift is allowed (it is crouch; a crouching player can still save)', () => {
      expect(resolveHotkey(key('KeyK', { shiftKey: true, key: 'K' }))).toEqual({ action: 'quickSave' });
    });
  });

  it('ignores auto-repeat, so holding E does not flicker the inventory', () => {
    const toggleInventory = vi.fn();
    const onKey = createHotkeyHandler({ toggleInventory }, () => null);
    onKey(key('KeyE'));
    for (let i = 0; i < 10; i++) onKey(key('KeyE', { repeat: true }));
    expect(toggleInventory).toHaveBeenCalledTimes(1);
  });

  it('uses the physical key: works with Caps Lock and non-QWERTY layouts', () => {
    // Caps Lock: key is "K" but code is still KeyK.
    expect(resolveHotkey({ code: 'KeyK', key: 'K' })).toEqual({ action: 'quickSave' });
  });

  describe('existing shortcuts keep working through the new handler', () => {
    it.each([
      ['KeyB', 'toggleBarter'],
      ['KeyC', 'toggleCrafting'],
      ['KeyH', 'toggleLearning'],
    ])('%s -> %s', (code, action) => {
      expect(resolveHotkey(key(code))).toEqual({ action });
    });

    it('1-9 select hotbar slots 0-8, from the number row or the keypad', () => {
      expect(resolveHotkey({ code: 'Digit1', key: '1' })).toEqual({ action: 'selectSlot', slot: 0 });
      expect(resolveHotkey({ code: 'Numpad9', key: '9' })).toEqual({ action: 'selectSlot', slot: 8 });
      expect(resolveHotkey({ code: 'Digit0', key: '0' })).toBeNull();
    });

    it('passes the slot index to the selectSlot handler', () => {
      const selectSlot = vi.fn();
      createHotkeyHandler({ selectSlot }, () => null)({ code: 'Digit4', key: '4' });
      expect(selectSlot).toHaveBeenCalledWith(3);
    });
  });

  it('does not claim movement keys (PranaInput owns WASD, Space, Shift, F)', () => {
    for (const code of ['KeyW', 'KeyA', 'KeyS', 'KeyD', 'Space', 'ShiftLeft', 'KeyF']) {
      expect(HOTKEYS[code]).toBeUndefined();
      expect(resolveHotkey(key(code))).toBeNull();
    }
  });

  it('a missing handler is ignored rather than throwing', () => {
    expect(createHotkeyHandler({}, () => null)(key('KeyE'))).toBe(false);
  });
});
