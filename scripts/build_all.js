/**
 * @fileoverview Runs every packaging script for ShilpLoka.
 *
 * Produces (see the individual scripts for detail):
 *   1. release/shilploka-itch-web.zip            - web build for itch.io
 *   2. release/shilploka-offline-web-{linux,windows,mac}.*
 *                                                 - the same web game, packaged
 *                                                   to play offline in a browser.
 *                                                   NOT native apps.
 *   3. release/shilploka-android-studio-project.zip
 *                                                 - Android source project. NOT
 *                                                   an installable APK.
 *   Native desktop binaries and an APK are produced ONLY when a working Tauri /
 *   Gradle toolchain is present; otherwise those scripts say so and skip.
 *
 * This header used to list AppImage, deb, exe, msi, dmg and apk. None of those
 * were ever produced without a toolchain, and the "apk" was a renamed zip.
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
  { name: 'Offline web packages (Linux, Windows, macOS)', script: 'scripts/build_desktop.js --target all' },
  { name: 'Android Studio project', script: 'scripts/build_android.js' },
];

for (const step of steps) {
  console.log(`\n▶ [EXEC] ${step.name}...`);
  execSync(`node ${step.script}`, { cwd: ROOT_DIR, stdio: 'inherit' });
}

console.log('\n===========================================================');
console.log('Packages in release/:');
const files = fs.readdirSync(RELEASE_DIR);
for (const file of files) {
  const fullPath = path.join(RELEASE_DIR, file);
  const stats = fs.statSync(fullPath);
  if (stats.isFile()) {
    console.log(`  - ${file.padEnd(35)} (${(stats.size / 1024).toFixed(1)} KB)`);
  }
}
console.log('===========================================================');
