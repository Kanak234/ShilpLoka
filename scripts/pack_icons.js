/**
 * @fileoverview Build REAL .ico and .icns files from the generated PNG icons.
 *
 * WHY THIS EXISTS
 *   generate_icons.js used to "make" icon.ico and icon.icns by copying a PNG and
 *   renaming it. `file` reported both as "PNG image data". Windows and macOS do
 *   not read a renamed PNG as an icon, and Tauri validates the formats listed in
 *   src-tauri/tauri.conf.json - so those fake files would also have broken any
 *   real native desktop build.
 *
 * WHAT IT DOES
 *   Both formats are small containers that can hold PNG images directly:
 *     .ico  - Windows icon. A 6-byte header, one 16-byte directory entry per
 *             image, then the image data. PNG payloads are valid since Vista.
 *     .icns - macOS icon. 'icns' + total length, then typed entries. The types
 *             icp5 / ic07 / ic08 / ic09 hold PNG data at 32/128/256/512 px.
 *   This script writes those headers around the existing PNGs. No image
 *   re-encoding and no extra dependency.
 *
 * USED BY: generate_icons.js (after it renders the PNGs), and runnable alone:
 *   node scripts/pack_icons.js
 *
 * NEXT: src-tauri/tauri.conf.json lists icons/icon.ico and icons/icon.icns;
 *   a Tauri build reads these files.
 */

import fs from 'node:fs';
import path from 'node:path';

const ICONS_DIR = path.resolve(import.meta.dirname, '..', 'src-tauri', 'icons');

/**
 * Read a PNG's real pixel size from its IHDR chunk.
 * WHY not trust the filename: "128x128@2x.png" is actually 256x256. Sizes in
 * an icon's directory must match the pixels, or the OS picks the wrong image.
 */
function pngSize(buf) {
  const PNG_SIGNATURE = '89504e470d0a1a0a';
  if (buf.subarray(0, 8).toString('hex') !== PNG_SIGNATURE) {
    throw new Error('not a PNG file');
  }
  // Signature (8) + chunk length (4) + "IHDR" (4), then width and height.
  return { width: buf.readUInt32BE(16), height: buf.readUInt32BE(20) };
}

/**
 * Pack PNGs into a Windows .ico.
 * @param {Buffer[]} pngs - Each at most 256 px (the format's limit).
 */
export function packIco(pngs) {
  const count = pngs.length;
  const header = Buffer.alloc(6);
  header.writeUInt16LE(0, 0);        // reserved
  header.writeUInt16LE(1, 2);        // type 1 = icon (2 would be cursor)
  header.writeUInt16LE(count, 4);    // number of images

  const entries = [];
  let offset = 6 + 16 * count;       // image data starts after all entries
  for (const png of pngs) {
    const { width, height } = pngSize(png);
    if (width > 256 || height > 256) throw new Error(`.ico images max out at 256 px (got ${width})`);
    const e = Buffer.alloc(16);
    e.writeUInt8(width === 256 ? 0 : width, 0);    // 0 means 256 in this format
    e.writeUInt8(height === 256 ? 0 : height, 1);
    e.writeUInt8(0, 2);                            // palette size: none
    e.writeUInt8(0, 3);                            // reserved
    e.writeUInt16LE(1, 4);                         // colour planes
    e.writeUInt16LE(32, 6);                        // bits per pixel (RGBA)
    e.writeUInt32LE(png.length, 8);                // bytes of image data
    e.writeUInt32LE(offset, 12);                   // where that data starts
    entries.push(e);
    offset += png.length;
  }
  return Buffer.concat([header, ...entries, ...pngs]);
}

/** ICNS entry type for each PNG size macOS understands. */
const ICNS_TYPE_BY_SIZE = { 32: 'icp5', 64: 'icp6', 128: 'ic07', 256: 'ic08', 512: 'ic09' };

/**
 * Pack PNGs into a macOS .icns. Sizes without an ICNS type are skipped.
 * @param {Buffer[]} pngs
 */
export function packIcns(pngs) {
  const chunks = [];
  for (const png of pngs) {
    const { width } = pngSize(png);
    const type = ICNS_TYPE_BY_SIZE[width];
    if (!type) continue;
    const head = Buffer.alloc(8);
    head.write(type, 0, 'ascii');
    head.writeUInt32BE(png.length + 8, 4);   // length INCLUDES this 8-byte header
    chunks.push(head, png);
  }
  const body = Buffer.concat(chunks);
  const head = Buffer.alloc(8);
  head.write('icns', 0, 'ascii');
  head.writeUInt32BE(body.length + 8, 4);    // total file length, header included
  return Buffer.concat([head, body]);
}

/** Rebuild icon.ico and icon.icns from the PNGs in src-tauri/icons. */
export function packAll(dir = ICONS_DIR) {
  const read = n => fs.readFileSync(path.join(dir, n));
  const p32 = read('32x32.png');
  const p128 = read('128x128.png');
  const p256 = read('128x128@2x.png');   // really 256x256
  const p512 = read('icon.png');         // really 512x512

  fs.writeFileSync(path.join(dir, 'icon.ico'), packIco([p32, p128, p256]));
  fs.writeFileSync(path.join(dir, 'icon.icns'), packIcns([p32, p128, p256, p512]));
  console.log(`✓ Packed real icon.ico and icon.icns in ${dir}`);
}

// Run directly: `node scripts/pack_icons.js`
if (import.meta.url === `file://${process.argv[1]}`) {
  packAll();
}
