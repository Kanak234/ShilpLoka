/**
 * @fileoverview PranaInput - Resilient Input Dispatcher & Action Mapping Engine
 * @module core/prana_input
 * 
 * Manages asynchronous DOM input signals (keyboard strokes, mouse movements,
 * pointer lock lifecycle) and exposes a clean, decoupled polling interface
 * tailored for fixed-timestep simulation loops.
 * 
 * Features:
 * - Action Mapping: Maps physical keys (e.g., 'KeyW', 'ArrowUp') to symbolic
 *   gameplay actions (e.g., 'move_forward', 'jump', 'interact').
 * - Just-Pressed & Just-Released Edge Detection.
 * - Smooth Pointer Lock ('Drishti Lock') tracking with raw delta extraction.
 * - Decoupled state flushing to eliminate input lag.
 */

/**
 * Standard gameplay action names.
 * @readonly
 * @enum {string}
 */
export const VastuAction = {
  MOVE_FORWARD: 'move_forward',
  MOVE_BACKWARD: 'move_backward',
  MOVE_LEFT: 'move_left',
  MOVE_RIGHT: 'move_right',
  JUMP: 'jump',
  CROUCH: 'crouch',
  SPRINT: 'sprint',
  BREAK_BLOCK: 'break_block',
  PLACE_BLOCK: 'place_block',
  INVENTORY_TOGGLE: 'inventory_toggle',
  FLY_TOGGLE: 'fly_toggle',
  QUICK_SAVE: 'quick_save',
  QUICK_LOAD: 'quick_load',
};

/**
 * Default keyboard binding map connecting physical DOM keycodes to actions.
 */
const DEFAULT_KEY_MAP = {
  KeyW: VastuAction.MOVE_FORWARD,
  ArrowUp: VastuAction.MOVE_FORWARD,
  KeyS: VastuAction.MOVE_BACKWARD,
  ArrowDown: VastuAction.MOVE_BACKWARD,
  KeyA: VastuAction.MOVE_LEFT,
  ArrowLeft: VastuAction.MOVE_LEFT,
  KeyD: VastuAction.MOVE_RIGHT,
  ArrowRight: VastuAction.MOVE_RIGHT,
  Space: VastuAction.JUMP,
  ShiftLeft: VastuAction.CROUCH,
  ShiftRight: VastuAction.CROUCH,
  ControlLeft: VastuAction.SPRINT,
  ControlRight: VastuAction.SPRINT,
  KeyE: VastuAction.INVENTORY_TOGGLE,
  KeyF: VastuAction.FLY_TOGGLE,
  KeyK: VastuAction.QUICK_SAVE,
  KeyL: VastuAction.QUICK_LOAD,
};

/**
 * PranaInput collects DOM events and exposes buffered polling queries.
 */
export class PranaInput {
  /**
   * Initializes input maps and mouse delta accumulators.
   * 
   * @param {HTMLElement} [domElement=window] - Target DOM element for event listeners.
   * @param {Object} [customKeyMap=DEFAULT_KEY_MAP] - Optional custom key mappings.
   */
  constructor(domElement = window, customKeyMap = DEFAULT_KEY_MAP) {
    this.target = domElement;
    this.keyMap = { ...customKeyMap };

    /**
     * Set of actions currently held down.
     * @type {Set<string>}
     */
    this.activeActions = new Set();

    /**
     * Set of actions initiated during the current frame interval.
     * @type {Set<string>}
     */
    this.justPressedActions = new Set();

    /**
     * Set of actions released during the current frame interval.
     * @type {Set<string>}
     */
    this.justReleasedActions = new Set();

    /**
     * Accumulated mouse movement deltas since last flush.
     * @type {{x: number, y: number}}
     */
    this.mouseDelta = { x: 0, y: 0 };

    /**
     * Active hotbar selection index (0 through 8).
     * @type {number}
     */
    this.selectedHotbarSlot = 0;

    /**
     * Flag indicating whether Pointer Lock (Drishti) is active.
     * @type {boolean}
     */
    this.isDrishtiLocked = false;

    /**
     * Virtual touch joystick vector [ -1.0 to 1.0 ].
     * @type {{x: number, z: number}}
     */
    this.virtualJoystickVector = { x: 0, z: 0 };

    /**
     * Flag indicating if mobile touch controls are actively steering.
     * @type {boolean}
     */
    this.isTouchMode = false;

    // Bound listeners for clean event detachment
    this.handleKeyDown = this.handleKeyDown.bind(this);
    this.handleKeyUp = this.handleKeyUp.bind(this);
    this.handleMouseMove = this.handleMouseMove.bind(this);
    this.handleMouseDown = this.handleMouseDown.bind(this);
    this.handleMouseUp = this.handleMouseUp.bind(this);
    this.handleWheel = this.handleWheel.bind(this);
    this.handlePointerLockChange = this.handlePointerLockChange.bind(this);

    this.bind_listeners();
  }

  /**
   * Attaches low-level browser event listeners.
   */
  bind_listeners() {
    window.addEventListener('keydown', this.handleKeyDown, false);
    window.addEventListener('keyup', this.handleKeyUp, false);
    window.addEventListener('mousemove', this.handleMouseMove, false);
    window.addEventListener('mousedown', this.handleMouseDown, false);
    window.addEventListener('mouseup', this.handleMouseUp, false);
    window.addEventListener('wheel', this.handleWheel, { passive: true });
    document.addEventListener('pointerlockchange', this.handlePointerLockChange, false);
  }

  /**
   * Detaches browser event listeners for clean shutdown.
   */
  unbind_listeners() {
    window.removeEventListener('keydown', this.handleKeyDown);
    window.removeEventListener('keyup', this.handleKeyUp);
    window.removeEventListener('mousemove', this.handleMouseMove);
    window.removeEventListener('mousedown', this.handleMouseDown);
    window.removeEventListener('mouseup', this.handleMouseUp);
    window.removeEventListener('wheel', this.handleWheel);
    document.removeEventListener('pointerlockchange', this.handlePointerLockChange);
  }

  handleKeyDown(event) {
    // Number keys 1-9 for hotbar selection
    if (event.key >= '1' && event.key <= '9') {
      this.selectedHotbarSlot = parseInt(event.key, 10) - 1;
    }

    const action = this.keyMap[event.code];
    if (action) {
      if (!this.activeActions.has(action)) {
        this.justPressedActions.add(action);
      }
      this.activeActions.add(action);
    }
  }

  handleKeyUp(event) {
    const action = this.keyMap[event.code];
    if (action) {
      this.activeActions.delete(action);
      this.justReleasedActions.add(action);
    }
  }

  handleMouseMove(event) {
    if (document.pointerLockElement) {
      this.mouseDelta.x += event.movementX || 0;
      this.mouseDelta.y += event.movementY || 0;
    }
  }

  handleMouseDown(event) {
    if (!document.pointerLockElement) return;
    if (event.button === 0) {
      this.justPressedActions.add(VastuAction.BREAK_BLOCK);
      this.activeActions.add(VastuAction.BREAK_BLOCK);
    } else if (event.button === 2) {
      this.justPressedActions.add(VastuAction.PLACE_BLOCK);
      this.activeActions.add(VastuAction.PLACE_BLOCK);
    }
  }

  handleMouseUp(event) {
    if (event.button === 0) {
      this.activeActions.delete(VastuAction.BREAK_BLOCK);
      this.justReleasedActions.add(VastuAction.BREAK_BLOCK);
    } else if (event.button === 2) {
      this.activeActions.delete(VastuAction.PLACE_BLOCK);
      this.justReleasedActions.add(VastuAction.PLACE_BLOCK);
    }
  }

  handleWheel(event) {
    if (document.pointerLockElement) {
      if (event.deltaY > 0) {
        this.selectedHotbarSlot = (this.selectedHotbarSlot + 1) % 9;
      } else if (event.deltaY < 0) {
        this.selectedHotbarSlot = (this.selectedHotbarSlot + 8) % 9;
      }
    }
  }

  handlePointerLockChange() {
    this.isDrishtiLocked = !!document.pointerLockElement;
  }

  /**
   * Requests pointer lock ('Drishti Lock') on the specified canvas element.
   * 
   * @param {HTMLElement} canvasElement - Canvas to lock cursor to.
   */
  request_drishti_lock(canvasElement) {
    if (canvasElement && canvasElement.requestPointerLock) {
      canvasElement.requestPointerLock();
    }
  }

  /**
   * Exits pointer lock.
   */
  release_drishti_lock() {
    if (document.exitPointerLock) {
      document.exitPointerLock();
    }
  }

  /**
   * Checks if an action is actively held down.
   * 
   * @param {string} actionName - Name of the action from `VastuAction`.
   * @returns {boolean} True if the action is currently pressed.
   */
  is_action_pressed(actionName) {
    return this.activeActions.has(actionName);
  }

  /**
   * Checks if an action was initiated during this frame.
   * 
   * @param {string} actionName - Name of the action from `VastuAction`.
   * @returns {boolean} True if pressed on this step.
   */
  is_action_just_pressed(actionName) {
    return this.justPressedActions.has(actionName);
  }

  /**
   * Sets continuous 2D directional input from virtual on-screen joystick.
   * 
   * @param {number} x - Normalized X [-1.0, 1.0] (Left/Right)
   * @param {number} z - Normalized Z [-1.0, 1.0] (Forward/Backward)
   */
  set_virtual_joystick(x, z) {
    this.virtualJoystickVector.x = Math.max(-1, Math.min(1, x));
    this.virtualJoystickVector.z = Math.max(-1, Math.min(1, z));
    this.isTouchMode = true;
  }

  /**
   * Directly feeds look rotation delta from mobile touch gestures.
   * 
   * @param {number} dx - Horizontal look delta.
   * @param {number} dy - Vertical look delta.
   */
  add_virtual_look_delta(dx, dy) {
    this.mouseDelta.x += dx;
    this.mouseDelta.y += dy;
    this.isTouchMode = true;
  }

  /**
   * Simulates action press or release from virtual touch buttons.
   * 
   * @param {string} actionName - Action identifier from VastuAction.
   * @param {boolean} isPressed - Whether button is touched or released.
   */
  trigger_virtual_action(actionName, isPressed) {
    this.isTouchMode = true;
    if (isPressed) {
      if (!this.activeActions.has(actionName)) {
        this.justPressedActions.add(actionName);
      }
      this.activeActions.add(actionName);
    } else {
      this.activeActions.delete(actionName);
      this.justReleasedActions.add(actionName);
    }
  }

  /**
   * Calculates normalized 2D movement vector along (X, Z) axes from directional inputs.
   * 
   * @returns {{x: number, z: number}} Normalized planar movement vector.
   */
  get_movement_vector() {
    let x = 0;
    let z = 0;

    if (this.is_action_pressed(VastuAction.MOVE_FORWARD)) z -= 1;
    if (this.is_action_pressed(VastuAction.MOVE_BACKWARD)) z += 1;
    if (this.is_action_pressed(VastuAction.MOVE_LEFT)) x -= 1;
    if (this.is_action_pressed(VastuAction.MOVE_RIGHT)) x += 1;

    // Incorporate virtual joystick input
    if (this.virtualJoystickVector.x !== 0 || this.virtualJoystickVector.z !== 0) {
      x += this.virtualJoystickVector.x;
      z += this.virtualJoystickVector.z;
    }

    const lengthSq = x * x + z * z;
    if (lengthSq > 1.0) {
      const len = Math.sqrt(lengthSq);
      x /= len;
      z /= len;
    } else if (lengthSq > 0 && (this.virtualJoystickVector.x === 0 && this.virtualJoystickVector.z === 0)) {
      const len = Math.sqrt(lengthSq);
      x /= len;
      z /= len;
    }

    return { x, z };
  }

  /**
   * Retrieves accumulated mouse deltas and resets them for the next frame.
   * 
   * @returns {{x: number, y: number}} Mouse movement delta since last call.
   */
  consume_mouse_delta() {
    const delta = { x: this.mouseDelta.x, y: this.mouseDelta.y };
    this.mouseDelta.x = 0;
    this.mouseDelta.y = 0;
    return delta;
  }

  /**
   * Flushes edge-triggered states at the boundary between frames.
   */
  flush_frame_edges() {
    this.justPressedActions.clear();
    this.justReleasedActions.clear();
  }
}
