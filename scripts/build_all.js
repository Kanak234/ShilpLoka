/**
 * @fileoverview Unified Master Cross-Platform Packaging Script for ShilpLoka
 * Builds:
 * 1. Web / Itch.io Target (release/shilploka-itch-web.zip)
 * 2. Linux Distribution (.tar.gz / AppImage / deb)
 * 3. Windows Distribution (.zip / exe / msi)
 * 4. macOS Distribution (.zip / app / dmg)
 * 5. Android Mobile (.apk / Gradle Project)
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const ROOT_DIR = path.resolve(import.meta.dirname, '..');
const RELEASE_DIR = path.join(ROOT_DIR, 'release');

console.log('===========================================================');
console.log('☸  STARTING UNIFIED CROSS-PLATFORM PACKAGING (BUILD:ALL)');
console.log('===========================================================');

const steps = [
  { name: 'Itch.io Web Bundle', script: 'scripts/build_itch.js' },
  { name: 'Desktop Packages (Linux, Windows, macOS)', script: 'scripts/build_desktop.js --target all' },
  { name: 'Android APK Mobile Target', script: 'scripts/build_android.js' },
];

for (const step of steps) {
  console.log(`\n▶ [EXEC] ${step.name}...`);
  execSync(`node ${step.script}`, { cwd: ROOT_DIR, stdio: 'inherit' });
}

console.log('\n===========================================================');
console.log('🎉 ALL CROSS-PLATFORM PACKAGES READY IN release/:');
const files = fs.readdirSync(RELEASE_DIR);
for (const file of files) {
  const fullPath = path.join(RELEASE_DIR, file);
  const stats = fs.statSync(fullPath);
  if (stats.isFile()) {
    console.log(`  - ${file.padEnd(35)} (${(stats.size / 1024).toFixed(1)} KB)`);
  }
}
console.log('===========================================================');
