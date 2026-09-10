/**
 * @fileoverview VirtualJoystick - Mobile Device Detection & Virtual Touch Control System
 * @module shilploka/input/virtual_joystick
 * 
 * Features:
 * - Robust mobile device detection (User Agent, MaxTouchPoints, TouchEvent API & Query Overrides).
 * - Transparent on-screen floating virtual joystick for omnidirectional movement.
 * - Touch action buttons (Jump, Crouch, Place Block, Mine Block, Barter, Crafting).
 * - Touchpad camera look rotation across the right screen quadrant.
 * - Zero external dependencies, direct coupling into PranaInput and ShilpEngine.
 */

import { VastuAction } from '../../core/prana_input.js';

/**
 * Checks if the client environment is a mobile phone, tablet, or touch-first device.
 * Supports URL param '?mobile=true' for developer testing on desktop browsers.
 * 
 * @returns {boolean} True if running on mobile device or touch environment.
 */
export function isMobileDevice() {
  if (typeof window === 'undefined') return false;

  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get('mobile') === 'true' || urlParams.get('touch') === '1') {
    return true;
  }

  const hasTouchEvents = 'ontouchstart' in window || (navigator.maxTouchPoints && navigator.maxTouchPoints > 0);
  const mobileUARegex = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini|Mobile|mobile|CriOS/i;
  const isMobileUA = mobileUARegex.test(navigator.userAgent || navigator.vendor || window.opera);

  return hasTouchEvents && isMobileUA;
}

export class VirtualTouchControls {
  /**
   * @param {import('../../core/prana_input.js').PranaInput} pranaInput - Master PranaInput instance.
   * @param {import('../core/shilp_engine.js').ShilpEngine} engine - ShilpEngine master instance.
   */
  constructor(pranaInput, engine) {
    this.input = pranaInput;
    this.engine = engine;

    this.root = null;
    this.joystickBase = null;
    this.joystickThumb = null;

    // Joystick State
    this.joystickTouchId = null;
    this.basePos = { x: 0, y: 0 };
    this.maxRadius = 45; // Max knob displacement in px

    // Camera Touchpad State
    this.lookTouchId = null;
    this.lastLookPos = { x: 0, y: 0 };

    this.init();
  }

  /**
   * Initializes and mounts virtual touch controls into document body.
   */
  init() {
    // Prevent duplicate injection
    if (document.getElementById('mobile-controls-root')) return;

    this.root = document.createElement('div');
    this.root.id = 'mobile-controls-root';
    this.root.className = 'mobile-controls-layer';

    this.root.innerHTML = `
      <!-- Left Screen: Virtual Joystick -->
      <div id="joystick-touch-zone" class="joystick-zone">
        <div id="joystick-base" class="joystick-base">
          <div id="joystick-thumb" class="joystick-thumb"></div>
        </div>
      </div>

      <!-- Right Screen: Touchpad Camera Zone & Action Buttons -->
      <div id="camera-touch-zone" class="camera-touch-zone"></div>

      <div class="mobile-action-buttons">
        <div class="action-btn-row">
          <button id="touch-btn-place" class="mobile-btn btn-place" title="Place Held Block">🧱</button>
          <button id="touch-btn-mine" class="mobile-btn btn-mine" title="Mine Block">⛏️</button>
        </div>
        <div class="action-btn-row">
          <button id="touch-btn-crouch" class="mobile-btn btn-crouch" title="Crouch">⬇️</button>
          <button id="touch-btn-jump" class="mobile-btn btn-jump" title="Jump">🦘</button>
        </div>
      </div>

      <!-- Quick Mobile Header Bar for Barter, Crafting & Knowledge -->
      <div class="mobile-quick-toolbar">
        <button id="touch-btn-barter" class="mobile-mini-btn">🏺 विनिमय (Barter)</button>
        <button id="touch-btn-craft" class="mobile-mini-btn">☸ निर्माण (Craft)</button>
        <!-- New World on touch devices. WHY a second button: shilploka.css
             hides the whole desktop toolbar at <=920px and on any coarse
             pointer, so without this a phone player could never start over. -->
        <button id="touch-btn-new-world" class="mobile-mini-btn">🌱 नया लोक</button>
      </div>
    `;

    document.body.appendChild(this.root);

    this.joystickZone = document.getElementById('joystick-touch-zone');
    this.joystickBase = document.getElementById('joystick-base');
    this.joystickThumb = document.getElementById('joystick-thumb');
    this.cameraZone = document.getElementById('camera-touch-zone');

    this.bindJoystickEvents();
    this.bindCameraEvents();
    this.bindActionButtons();

    console.log('[VirtualTouchControls] Mobile virtual touch controls successfully mounted and hooked to PranaInput.');
  }

  bindJoystickEvents() {
    if (!this.joystickZone) return;

    const onTouchStart = (e) => {
      e.preventDefault();
      if (this.joystickTouchId !== null) return;

      const touch = e.changedTouches[0];
      this.joystickTouchId = touch.identifier;

      const rect = this.joystickZone.getBoundingClientRect();
      const clientX = touch.clientX;
      const clientY = touch.clientY;

      // Position base dynamically around touch point
      this.basePos = { x: clientX - rect.left, y: clientY - rect.top };
      this.joystickBase.style.left = `${this.basePos.x}px`;
      this.joystickBase.style.top = `${this.basePos.y}px`;
      this.joystickBase.classList.add('active');
      this.joystickThumb.style.transform = `translate(0px, 0px)`;
    };

    const onTouchMove = (e) => {
      e.preventDefault();
      if (this.joystickTouchId === null) return;

      for (let i = 0; i < e.changedTouches.length; i++) {
        const touch = e.changedTouches[i];
        if (touch.identifier === this.joystickTouchId) {
          const rect = this.joystickZone.getBoundingClientRect();
          const currX = touch.clientX - rect.left;
          const currY = touch.clientY - rect.top;

          const dx = currX - this.basePos.x;
          const dy = currY - this.basePos.y;
          const dist = Math.hypot(dx, dy);

          const angle = Math.atan2(dy, dx);
          const clampedDist = Math.min(dist, this.maxRadius);

          const thumbX = Math.cos(angle) * clampedDist;
          const thumbY = Math.sin(angle) * clampedDist;

          this.joystickThumb.style.transform = `translate(${thumbX}px, ${thumbY}px)`;

          // Normalize vector to [-1.0, 1.0]
          // In ShilpLoka coordinate space: Forward is -Z, Backward is +Z
          const normX = thumbX / this.maxRadius;
          const normZ = thumbY / this.maxRadius;

          this.input.set_virtual_joystick(normX, normZ);
          break;
        }
      }
    };

    const onTouchEnd = (e) => {
      e.preventDefault();
      for (let i = 0; i < e.changedTouches.length; i++) {
        if (e.changedTouches[i].identifier === this.joystickTouchId) {
          this.joystickTouchId = null;
          this.joystickThumb.style.transform = `translate(0px, 0px)`;
          this.joystickBase.classList.remove('active');
          this.input.set_virtual_joystick(0, 0);
          break;
        }
      }
    };

    this.joystickZone.addEventListener('touchstart', onTouchStart, { passive: false });
    this.joystickZone.addEventListener('touchmove', onTouchMove, { passive: false });
    this.joystickZone.addEventListener('touchend', onTouchEnd, { passive: false });
    this.joystickZone.addEventListener('touchcancel', onTouchEnd, { passive: false });
  }

  bindCameraEvents() {
    if (!this.cameraZone) return;

    const onTouchStart = (e) => {
      if (this.lookTouchId !== null) return;
      const touch = e.changedTouches[0];
      this.lookTouchId = touch.identifier;
      this.lastLookPos = { x: touch.clientX, y: touch.clientY };
    };

    const onTouchMove = (e) => {
      if (this.lookTouchId === null) return;
      for (let i = 0; i < e.changedTouches.length; i++) {
        const touch = e.changedTouches[i];
        if (touch.identifier === this.lookTouchId) {
          const dx = touch.clientX - this.lastLookPos.x;
          const dy = touch.clientY - this.lastLookPos.y;

          this.lastLookPos = { x: touch.clientX, y: touch.clientY };

          // Feed into PranaInput virtual look rotation
          this.input.add_virtual_look_delta(dx * 1.5, dy * 1.5);
          break;
        }
      }
    };

    const onTouchEnd = (e) => {
      for (let i = 0; i < e.changedTouches.length; i++) {
        if (e.changedTouches[i].identifier === this.lookTouchId) {
          this.lookTouchId = null;
          break;
        }
      }
    };

    this.cameraZone.addEventListener('touchstart', onTouchStart, { passive: true });
    this.cameraZone.addEventListener('touchmove', onTouchMove, { passive: true });
    this.cameraZone.addEventListener('touchend', onTouchEnd, { passive: true });
    this.cameraZone.addEventListener('touchcancel', onTouchEnd, { passive: true });
  }

  bindActionButtons() {
    const attachButton = (elementId, actionName) => {
      const btn = document.getElementById(elementId);
      if (!btn) return;

      const handlePress = (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.input.trigger_virtual_action(actionName, true);
        btn.classList.add('pressed');
      };

      const handleRelease = (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.input.trigger_virtual_action(actionName, false);
        btn.classList.remove('pressed');
      };

      btn.addEventListener('touchstart', handlePress, { passive: false });
      btn.addEventListener('touchend', handleRelease, { passive: false });
      btn.addEventListener('touchcancel', handleRelease, { passive: false });
      btn.addEventListener('mousedown', handlePress);
      btn.addEventListener('mouseup', handleRelease);
    };

    // Jump & Crouch
    attachButton('touch-btn-jump', VastuAction.JUMP);
    attachButton('touch-btn-crouch', VastuAction.CROUCH);

    // Place & Mine
    const placeBtn = document.getElementById('touch-btn-place');
    if (placeBtn) {
      placeBtn.addEventListener('touchstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (this.engine) this.engine.tryPlaceActiveVoxel();
      }, { passive: false });
    }

    const mineBtn = document.getElementById('touch-btn-mine');
    if (mineBtn) {
      mineBtn.addEventListener('touchstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (this.engine) this.engine.tryMineTargetVoxel();
      }, { passive: false });
    }

    // Modal Toggles
    const barterBtn = document.getElementById('touch-btn-barter');
    if (barterBtn) {
      barterBtn.addEventListener('touchstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (this.engine?.barterModal) this.engine.barterModal.toggle();
      }, { passive: false });
    }

    const craftBtn = document.getElementById('touch-btn-craft');
    if (craftBtn) {
      craftBtn.addEventListener('touchstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (this.engine?.craftingModal) this.engine.craftingModal.toggle();
      }, { passive: false });
    }

    // New World (touch). Delegates to the engine, which shows the confirm
    // dialog, stops autosave, clears the save and reloads -- the exact same
    // path as the desktop button, so the two can never behave differently.
    const newWorldBtn = document.getElementById('touch-btn-new-world');
    if (newWorldBtn) {
      newWorldBtn.addEventListener('touchstart', (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (this.engine?.startNewWorld) this.engine.startNewWorld();
      }, { passive: false });
    }
  }

  destroy() {
    if (this.root && this.root.parentNode) {
      this.root.parentNode.removeChild(this.root);
    }
  }
}
