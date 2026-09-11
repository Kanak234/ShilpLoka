/**
 * @fileoverview Desktop packaging for ShilpLoka - honest edition.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHAT WAS WRONG BEFORE
 *   The header promised .deb/.rpm/.AppImage, .msi/.exe and .dmg. None of those
 *   were ever produced. What shipped was dist/index.html plus a launcher:
 *     - Linux:   a shell script running `xdg-open index.html`
 *     - Windows: a .bat running `start index.html`
 *     - macOS:   a ShilpLoka.app whose "binary" was a bash script running
 *                `open index.html` - an app bundle in name only.
 *   The names said "x64" and "universal", terms for native binaries, about an
 *   architecture-independent HTML file. And detection treated "cargo is
 *   installed" as "Tauri works": it would run `npx @tauri-apps/cli build`
 *   (downloading the CLI from the network), fail, print a warning, and quietly
 *   fall back to the web packages.
 *
 * WHAT IT DOES NOW
 *   A. Always builds the OFFLINE WEB packages, named for what they are:
 *        shilploka-offline-web-linux.tar.gz
 *        shilploka-offline-web-windows.zip
 *        shilploka-offline-web-mac.zip
 *      Each holds the whole game as ONE html file (vite-plugin-singlefile
 *      inlines everything, so it runs from file:// with no server), a
 *      convenience launcher, and a README.txt stating plainly that it opens in
 *      a browser and is not a native app.
 *   B. Builds NATIVE binaries with Tauri ONLY when a working toolchain is
 *      actually present (see detectTauri). If that build runs and fails, this
 *      script fails. There is no silent fallback.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const ROOT_DIR = path.resolve(import.meta.dirname, '..');
const DIST_HTML = path.join(ROOT_DIR, 'dist', 'index.html');
const RELEASE_DIR = path.join(ROOT_DIR, 'release');
const STAGE_DIR = path.join(RELEASE_DIR, 'desktop');
const ICON_PNG = path.join(ROOT_DIR, 'src-tauri', 'icons', 'icon.png');

/** Names the old script produced. Deleted so a stale fake can never ship. */
const LEGACY_OUTPUTS = [
  'shilploka-linux-x64.tar.gz',
  'shilploka-windows-x64.zip',
  'shilploka-macos-universal.zip',
];

const argTarget = (() => {
  const i = process.argv.indexOf('--target');
  return i !== -1 && process.argv[i + 1] ? process.argv[i + 1].toLowerCase() : 'all';
})();

/** The same honest text goes in every package. */
const README_TXT = `ShilpLoka (शिल्पलोक) - offline web edition
============================================

WHAT THIS IS
  The web version of ShilpLoka, packaged to play without an internet
  connection. It is NOT a native application: it opens in your web browser.
  The whole game is the single file ShilpLoka.html.

HOW TO PLAY
  Double-click ShilpLoka.html, or use the launcher in this folder.
  Any current Chrome, Edge, Firefox or Safari works. WebGL2 is required.

SAVES
  Your world is saved in the browser's local storage, automatically every
  30 seconds and when you close the tab. Saves belong to the browser you
  play in: switching browsers, or clearing site data, starts a new world.
`;

function stageFresh(name) {
  const dir = path.join(STAGE_DIR, name);
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  // ShilpLoka.html rather than index.html: this folder is opened by a person,
  // not served by a web server, so the name should say what the file is.
  fs.copyFileSync(DIST_HTML, path.join(dir, 'ShilpLoka.html'));
  fs.writeFileSync(path.join(dir, 'README.txt'), README_TXT);
  return dir;
}

function packageLinux() {
  const name = 'shilploka-offline-web-linux';
  const dir = stageFresh(name);
  // Honest launcher: its name says it plays the game; its body opens a file.
  fs.writeFileSync(path.join(dir, 'play-shilploka.sh'), `#!/usr/bin/env bash
# Opens ShilpLoka in your default web browser. This is a browser game, not a
# native binary: this script only opens ShilpLoka.html.
DIR="$(cd "$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
exec xdg-open "$DIR/ShilpLoka.html"
`, { mode: 0o755 });
  fs.copyFileSync(ICON_PNG, path.join(dir, 'icon.png'));
  fs.writeFileSync(path.join(dir, 'shilploka.desktop'), `[Desktop Entry]
Type=Application
Name=ShilpLoka (शिल्पलोक)
Comment=Ancient Indian voxel sandbox (opens in your web browser)
Exec=sh -c 'xdg-open "$(dirname "%k")/ShilpLoka.html"'
Icon=icon.png
Terminal=false
Categories=Game;Simulation;
`);
  const out = path.join(RELEASE_DIR, `${name}.tar.gz`);
  execSync(`tar -czf "${out}" -C "${STAGE_DIR}" ${name}`);
  return out;
}

function packageWindows() {
  const name = 'shilploka-offline-web-windows';
  const dir = stageFresh(name);
  fs.writeFileSync(path.join(dir, 'Play ShilpLoka.bat'),
    '@echo off\r\n' +
    'rem Opens ShilpLoka in your default web browser (a browser game, not an .exe).\r\n' +
    'start "" "%~dp0ShilpLoka.html"\r\n');
  const out = path.join(RELEASE_DIR, `${name}.zip`);
  execSync(`cd "${STAGE_DIR}" && zip -qr "${out}" "${name}"`);
  return out;
}

function packageMac() {
  const name = 'shilploka-offline-web-mac';
  const dir = stageFresh(name);
  // A .command file is macOS's own way to make a script double-clickable, and
  // it is visibly a script. The old ShilpLoka.app was an app bundle whose
  // "binary" was secretly a bash script.
  fs.writeFileSync(path.join(dir, 'Play ShilpLoka.command'), `#!/usr/bin/env bash
# Opens ShilpLoka in your default web browser (a browser game, not a native app).
open "$(dirname "$0")/ShilpLoka.html"
`, { mode: 0o755 });
  const out = path.join(RELEASE_DIR, `${name}.zip`);
  execSync(`cd "${STAGE_DIR}" && zip -qr "${out}" "${name}"`);
  return out;
}

/**
 * Is a Tauri toolchain really usable here?
 * WHY stricter than before: "cargo exists" is not "Tauri works". A real build
 * needs the Tauri CLI installed locally (not fetched by npx at build time) and,
 * on Linux, the WebKitGTK development libraries.
 * @returns {{ok: boolean, reason: string}}
 */
function detectTauri() {
  const run = cmd => {
    try { execSync(cmd, { stdio: 'pipe', shell: '/bin/sh' }); return true; } catch { return false; }
  };
  const localCli = fs.existsSync(path.join(ROOT_DIR, 'node_modules', '.bin', 'tauri'));
  const cargoCli = run('cargo tauri --version');
  if (!localCli && !cargoCli) {
    return { ok: false, reason: 'the Tauri CLI is not installed (neither @tauri-apps/cli nor cargo tauri)' };
  }
  if (process.platform === 'linux') {
    // This project is on Tauri 1.x, which links webkit2gtk-4.0.
    if (!run('pkg-config --exists webkit2gtk-4.0')) {
      return { ok: false, reason: 'webkit2gtk-4.0 development files are missing (Tauri 1.x needs them)' };
    }
  }
  return { ok: true, reason: localCli ? 'local @tauri-apps/cli' : 'cargo tauri' };
}

// ─────────────────────────────────────────────────────────────────── main
console.log('===========================================================');
console.log(`🖥️  SHILPLOKA DESKTOP PACKAGING (target: ${argTarget})`);
console.log('===========================================================');

execSync('npm run build', { cwd: ROOT_DIR, stdio: 'inherit' });
if (!fs.existsSync(DIST_HTML)) {
  console.error('❌ dist/index.html missing after build.');
  process.exit(1);
}
fs.mkdirSync(RELEASE_DIR, { recursive: true });
for (const f of LEGACY_OUTPUTS) {
  const p = path.join(RELEASE_DIR, f);
  if (fs.existsSync(p)) { fs.unlinkSync(p); console.log(`  removed stale ${f}`); }
}

console.log('\nA. Offline web packages (open in a browser; not native apps):');
const built = [];
if (argTarget === 'linux' || argTarget === 'all') built.push(packageLinux());
if (argTarget === 'win' || argTarget === 'all') built.push(packageWindows());
if (argTarget === 'mac' || argTarget === 'all') built.push(packageMac());
for (const p of built) {
  console.log(`  ✓ ${path.relative(ROOT_DIR, p)}  (${(fs.statSync(p).size / 1024).toFixed(1)} KB)`);
}

console.log('\nB. Native binaries (Tauri):');
const tauri = detectTauri();
if (!tauri.ok) {
  console.log(`  Skipped: ${tauri.reason}.`);
  console.log('  NO native binary was built. See README "Desktop" for what it needs.');
} else {
  console.log(`  Toolchain found (${tauri.reason}). Building...`);
  // No try/catch on purpose: a failed native build must fail this script.
  const cli = tauri.reason === 'cargo tauri' ? 'cargo tauri' : path.join(ROOT_DIR, 'node_modules', '.bin', 'tauri');
  execSync(`${cli} build`, { cwd: ROOT_DIR, stdio: 'inherit' });
  console.log('  ✓ Native bundles are in src-tauri/target/release/bundle/');
}
console.log('===========================================================');
