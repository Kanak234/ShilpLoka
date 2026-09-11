/**
 * @fileoverview Keyboard shortcuts for ShilpLoka's menus and save system.
 * @module shilploka/input/hotkeys
 *
 * WHY this is its own module: the rules for WHEN a shortcut may fire (never
 * while typing, never with Ctrl/Cmd/Alt, never on key auto-repeat) used to be
 * an inline check inside ShilpEngine's constructor, where no test could reach
 * it - the engine needs WebGL. Here it is plain functions of an event, so
 * tests/hotkeys.test.js exercises exactly the code the game runs.
 *
 * WHAT it does: turns a keydown event into an action name ("quickSave",
 * "toggleInventory", ...) and calls the matching handler.
 *
 * USED BY: ShilpEngine, which builds the handler with createHotkeyHandler()
 * and registers it on window 'keydown'. Movement keys (WASD, Space, Shift, F)
 * are NOT here: PranaInput handles those, continuously, every physics tick.
 */

/**
 * Physical key (KeyboardEvent.code) -> action name.
 * WHY event.code, not event.key: code is the key's position, so the
 * shortcuts stay put on non-QWERTY layouts and when Shift or Caps Lock is on.
 * NEXT: the action name is looked up in the handlers table the engine passes
 * to createHotkeyHandler().
 */
export const HOTKEYS = Object.freeze({
  KeyK: 'quickSave',        // write the game to storage now
  KeyL: 'quickLoad',        // go back to the last save
  KeyE: 'toggleInventory',  // open / close the 36-slot inventory panel
  KeyB: 'toggleBarter',
  KeyC: 'toggleCrafting',
  KeyH: 'toggleLearning',
});

/**
 * Is the player typing into something right now?
 * WHY: the barter modal has number fields. Without this check, typing into
 * one would also trigger shortcuts (and a future text field, such as a name,
 * would save or load the game on every "k" or "l").
 *
 * @param {Element|null|undefined} el - Usually document.activeElement.
 * @returns {boolean}
 */
export function isTextEntry(el) {
  if (!el) return false;
  if (el.isContentEditable) return true;
  // Every <input> counts, not only type="text": the same rule the engine used
  // before, and a focused number/range field still receives the keystroke.
  return el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.tagName === 'SELECT';
}

/**
 * Decide which shortcut, if any, a keydown event means.
 *
 * Returns null (do nothing) when:
 *  - a text field has focus (see isTextEntry);
 *  - Ctrl, Cmd/Meta or Alt is held. WHY: those combinations belong to the
 *    browser and OS - Ctrl+L focuses the address bar, Ctrl+K is search, Alt+E
 *    opens a menu. Stealing them would surprise the player. Shift is allowed:
 *    it is crouch, and a crouching player should still be able to save.
 *  - the event is an auto-repeat from a held key. WHY: holding E would
 *    flicker the inventory open/closed; holding K would save 30 times a second.
 *
 * @param {{code?: string, key?: string, repeat?: boolean, ctrlKey?: boolean, metaKey?: boolean, altKey?: boolean}} event
 * @param {Element|null} [activeElement]
 * @returns {null | {action: string, slot?: number}}
 *   slot is set for "selectSlot" (0-based hotbar index).
 */
export function resolveHotkey(event, activeElement = null) {
  if (isTextEntry(activeElement)) return null;
  if (event.ctrlKey || event.metaKey || event.altKey) return null;
  if (event.repeat) return null;

  const action = HOTKEYS[event.code];
  if (action) return { action };

  // Hotbar 1-9. Uses event.key (the character), as the engine always did, so
  // the number row and the numeric keypad both work.
  if (typeof event.key === 'string' && event.key.length === 1 && event.key >= '1' && event.key <= '9') {
    return { action: 'selectSlot', slot: Number(event.key) - 1 };
  }
  return null;
}

/**
 * Build the keydown listener.
 *
 * @param {Object<string, function(number=): void>} handlers - Action name ->
 *   function. "selectSlot" receives the slot index. Missing actions are ignored.
 * @param {function(): (Element|null)} [getActiveElement] - Injected so tests
 *   can say what has focus without a DOM.
 * @returns {function(KeyboardEvent): boolean} true when a shortcut ran.
 *   NEXT: the engine registers this on window 'keydown'.
 */
export function createHotkeyHandler(handlers, getActiveElement = () => globalThis.document?.activeElement ?? null) {
  return (event) => {
    const hit = resolveHotkey(event, getActiveElement());
    if (!hit) return false;
    const fn = handlers[hit.action];
    if (typeof fn !== 'function') return false;
    fn(hit.slot);
    return true;
  };
}
