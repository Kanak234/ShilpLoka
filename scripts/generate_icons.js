/**
 * @fileoverview Icon Generation Utility for Desktop & Mobile Packaging
 * Generates PNG icon variants for Tauri, Capacitor, and Desktop bundles.
 */

import puppeteer from 'puppeteer-core';
import fs from 'fs';
import path from 'path';
import { packAll } from './pack_icons.js';

const EDGE_PATH = '/usr/bin/microsoft-edge-stable';
const ROOT_DIR = path.resolve(import.meta.dirname, '..');
const ICONS_DIR = path.join(ROOT_DIR, 'src-tauri', 'icons');
const ANDROID_RES = path.join(ROOT_DIR, 'android', 'app', 'src', 'main', 'res');

if (!fs.existsSync(ICONS_DIR)) {
  fs.mkdirSync(ICONS_DIR, { recursive: true });
}

const SVG_ICON = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#2a1d17"/>
      <stop offset="100%" stop-color="#140f0c"/>
    </linearGradient>
    <linearGradient id="gold" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f1c40f"/>
      <stop offset="100%" stop-color="#b8860b"/>
    </linearGradient>
    <linearGradient id="brick" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#d35400"/>
      <stop offset="100%" stop-color="#962d00"/>
    </linearGradient>
  </defs>
  <!-- Background Rounded Shield -->
  <rect x="16" y="16" width="480" height="480" rx="96" fill="url(#bg)" stroke="#d4af37" stroke-width="14"/>
  <!-- Terracotta Foundation Plinth -->
  <polygon points="120,380 392,380 432,420 80,420" fill="url(#brick)" stroke="#d4af37" stroke-width="6"/>
  <!-- Ashoka Chakra Spokes & Wheel -->
  <circle cx="256" cy="230" r="130" fill="none" stroke="url(#gold)" stroke-width="16"/>
  <circle cx="256" cy="230" r="40" fill="#2a1d17" stroke="url(#gold)" stroke-width="12"/>
  <circle cx="256" cy="230" r="14" fill="#d4af37"/>
  <!-- 16 Sacred Rays / Spokes -->
  <g stroke="url(#gold)" stroke-width="8" stroke-linecap="round">
    <line x1="256" y1="100" x2="256" y2="190"/>
    <line x1="256" y1="270" x2="256" y2="360"/>
    <line x1="126" y1="230" x2="216" y2="230"/>
    <line x1="296" y1="230" x2="386" y2="230"/>
    <line x1="164" y1="138" x2="228" y2="202"/>
    <line x1="284" y1="258" x2="348" y2="322"/>
    <line x1="348" y1="138" x2="284" y2="202"/>
    <line x1="228" y1="258" x2="164" y2="322"/>
  </g>
</svg>
`;

async function generateIcons() {
  console.log('Generating crisp game icons...');
  const browser = await puppeteer.launch({
    executablePath: EDGE_PATH,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  const html = `<!DOCTYPE html><html><body style="margin:0;padding:0;background:transparent;">${SVG_ICON}</body></html>`;
  await page.setContent(html);

  const sizes = [
    { name: '32x32.png', width: 32, height: 32 },
    { name: '128x128.png', width: 128, height: 128 },
    { name: '128x128@2x.png', width: 256, height: 256 },
    { name: 'icon.png', width: 512, height: 512 },
  ];

  for (const s of sizes) {
    await page.setViewport({ width: s.width, height: s.height });
    const outPath = path.join(ICONS_DIR, s.name);
    await page.screenshot({ path: outPath, omitBackground: true });
    console.log(`✓ Generated icon: ${outPath} (${s.width}x${s.height})`);
  }

  // Build REAL .ico / .icns containers around the PNGs just rendered.
  // WHY: this used to copy a PNG and rename it to .ico / .icns. The files
  // were still PNGs (`file` said so), so Windows and macOS could not use them,
  // and Tauri - which validates the formats named in tauri.conf.json - would
  // have rejected them in any native build. See scripts/pack_icons.js.
  packAll(ICONS_DIR);

  await browser.close();
  console.log('All desktop & mobile icons successfully generated.');
}

generateIcons().catch(err => {
  console.error('Icon generation failed:', err);
  process.exit(1);
});
