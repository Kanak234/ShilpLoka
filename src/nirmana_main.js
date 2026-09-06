/**
 * @fileoverview Main Bootstrap Entry Point for Nirmana: The Ancient Voxel
 * @module nirmana_main
 */

import { NirmanaEngine } from './core/nirmana_engine.js';

function bootstrapNirmana() {
  if (window.__NIRMANA_ENGINE__) return;
  console.log('[Nirmana] Bootstrapping Ancient Indian Voxel Sandbox...');
  const engine = new NirmanaEngine('canvas-container');
  window.__NIRMANA_ENGINE__ = engine;
}

if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', bootstrapNirmana);
} else {
  bootstrapNirmana();
}

