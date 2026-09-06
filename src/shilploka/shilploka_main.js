/**
 * @fileoverview ShilpLoka Entry Point
 * @module shilploka/shilploka_main
 */

import { ShilpEngine } from './core/shilp_engine.js';

function bootstrap() {
  if (window.__SHILPLOKA__) return; // Prevent duplicate instantiation
  console.log('[ShilpLoka] Bootstrapping production voxel sandbox...');
  const engine = new ShilpEngine('canvas-container');
  window.__SHILPLOKA__ = engine;
}

if (document.readyState === 'loading') {
  window.addEventListener('DOMContentLoaded', bootstrap);
} else {
  bootstrap();
}

