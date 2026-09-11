/**
 * Guard: nothing that ships or builds may point at a module that was deleted.
 *
 * WHY this exists: the review branch deleted 24 modules (the legacy engine and
 * the old octree). Their deletion was proven safe by searching for every way a
 * file can be reached - <script> tags, static and dynamic imports, fetch/asset
 * URLs and plain string paths. This test re-runs that search on every
 * `npm test`, so a later change cannot quietly reintroduce a dangling reference
 * that would only fail at runtime.
 *
 * WHAT is scanned: source, HTML entries, build/verify scripts and the native
 * wrappers. The tests/ folder and docs are skipped: they may NAME these files
 * in prose (as this one does) without loading them.
 */
import fs from 'node:fs';
import path from 'node:path';
import { describe, expect, it } from 'vitest';

const ROOT = path.resolve(import.meta.dirname, '..');

const DELETED = [
  'src/audio.js', 'src/constants.js', 'src/core/nirmana_engine.js', 'src/daynight.js',
  'src/inventory.js', 'src/inventory/vastu_inventory.js', 'src/main.js', 'src/nirmana_main.js',
  'src/particles.js', 'src/physics.js', 'src/physics/vastu_physics.js',
  'src/player/drishti_camera.js', 'src/player.js', 'src/player/yoddha_controller.js',
  'src/save.js', 'src/shilploka/world/shilp_octree.js', 'src/textures.js',
  'src/world/ancient_blocks.js', 'src/world.js', 'src/world/prithvi_generator.js',
  'src/world/vastu_chunk.js', 'src/world/vastu_graph.js', 'src/world/vastu_octree.js',
  'src/world/vastu_world.js',
];

const SKIP_DIRS = new Set(['node_modules', 'dist', 'release', '.git', 'tests', 'docs', 'target', 'build', '.gradle']);
const SCAN_EXT = /\.(js|mjs|cjs|html|css|json|java|gradle|toml|rs|sh)$/;

function listFiles(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name.startsWith('.') && entry.name !== '.github') continue;
    if (SKIP_DIRS.has(entry.name)) continue;
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) listFiles(full, out);
    else if (SCAN_EXT.test(entry.name)) out.push(full);
  }
  return out;
}

/**
 * Pattern for one deleted module: its full path, its basename with no letter or
 * "_" directly before it (so "world.js" never matches "shilp_world.js"), or its
 * extension-less stem as a relative import specifier ('./world', "../world").
 */
function referencePattern(file) {
  const esc = s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const base = path.basename(file);
  const stem = base.replace(/\.js$/, '');
  return new RegExp(
    `(${esc(file)})|(^|[^A-Za-z0-9_])${esc(base)}|['"\`]\\.{1,2}/([A-Za-z0-9_/]*/)?${esc(stem)}['"\`]`
  );
}

describe('deleted legacy modules', () => {
  const files = listFiles(ROOT);

  it('scans a meaningful set of files', () => {
    // If this ever drops to a handful, the scan itself has broken.
    expect(files.length).toBeGreaterThan(30);
    expect(files.some(f => f.endsWith(path.join('src', 'shilploka', 'shilploka_main.js')))).toBe(true);
  });

  it('really are gone from disk', () => {
    for (const f of DELETED) expect(fs.existsSync(path.join(ROOT, f)), f).toBe(false);
  });

  it.each(DELETED)('%s is not referenced by any shipped or build file', (deleted) => {
    const re = referencePattern(deleted);
    const offenders = [];
    for (const file of files) {
      const lines = fs.readFileSync(file, 'utf8').split('\n');
      lines.forEach((line, i) => {
        if (re.test(line)) offenders.push(`${path.relative(ROOT, file)}:${i + 1}: ${line.trim()}`);
      });
    }
    expect(offenders).toEqual([]);
  });

  it('the pattern catches every way of referencing a module (self-check)', () => {
    // Positive controls: each must match. Negative: the live look-alikes must not.
    const re = referencePattern('src/world.js');
    expect(re.test("import World from '../world';")).toBe(true);
    expect(re.test("import('./world.js')")).toBe(true);
    expect(re.test("fetch('src/world.js')")).toBe(true);
    expect(re.test('<script src="./src/world.js"></script>')).toBe(true);
    expect(re.test("import { ShilpWorld } from './world/shilp_world.js';")).toBe(false);
  });
});
