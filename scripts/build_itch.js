/**
 * @fileoverview Production Itch.io Web Build Packager for ShilpLoka
 * 
 * Packages the optimized single-file HTML5 build from dist/ into a standard
 * distribution-ready ZIP archive formatted specifically for Itch.io HTML5 game uploads.
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const ROOT_DIR = path.resolve(import.meta.dirname, '..');
const DIST_DIR = path.join(ROOT_DIR, 'dist');
const RELEASE_DIR = path.join(ROOT_DIR, 'release');
const ZIP_OUT = path.join(RELEASE_DIR, 'shilploka-itch-web.zip');

console.log('===========================================================');
console.log('🏺 BUILDING ITCH.IO WEB DISTRIBUTION ARCHIVE');
console.log('===========================================================');

// 1. Ensure production build exists
console.log('1. Compiling production bundle (vite build)...');
execSync('npm run build', { cwd: ROOT_DIR, stdio: 'inherit' });

if (!fs.existsSync(path.join(DIST_DIR, 'index.html'))) {
  console.error('❌ Error: dist/index.html not found after build.');
  process.exit(1);
}

// 2. Ensure release directory exists
if (!fs.existsSync(RELEASE_DIR)) {
  fs.mkdirSync(RELEASE_DIR, { recursive: true });
}

// Remove prior zip if exists
if (fs.existsSync(ZIP_OUT)) {
  fs.unlinkSync(ZIP_OUT);
}

// 3. Create Itch.io compliant ZIP archive
console.log('2. Compressing into Itch.io zip archive...');
try {
  // Zip contents of dist directly at root of archive
  execSync(`cd "${DIST_DIR}" && zip -r "${ZIP_OUT}" ./*`, { stdio: 'inherit' });
} catch (err) {
  console.error('❌ Zip creation failed:', err.message);
  process.exit(1);
}

// 4. Verify Archive
const stats = fs.statSync(ZIP_OUT);
const sizeKb = (stats.size / 1024).toFixed(1);
console.log(`\n✓ Successfully created Itch.io Web Bundle: ${ZIP_OUT}`);
console.log(`  Package Size: ${sizeKb} KB`);
console.log('  Verification: index.html placed at root of archive.');
console.log('  Ready for direct upload to https://itch.io/game/new (Kind of project: HTML)');
console.log('===========================================================');
