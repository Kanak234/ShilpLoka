/**
 * Tests for scripts/vite_legacy_entry.js.
 */
import { describe, expect, it, vi } from 'vitest';
import {
  LEGACY_ENTRY,
  LEGACY_ENTRY_HTML,
  legacyEntryAlias,
} from '../scripts/vite_legacy_entry.js';

describe('vite_legacy_entry', () => {
  it('exports expected constants', () => {
    expect(LEGACY_ENTRY).toBe('shilploka.html');
    expect(LEGACY_ENTRY_HTML).toContain('index.html');
    expect(LEGACY_ENTRY_HTML).toContain('ShilpLoka');
  });

  it('provides plugin with configureServer and generateBundle', () => {
    const plugin = legacyEntryAlias();
    expect(plugin.name).toBe('shilploka-legacy-entry');
    expect(typeof plugin.configureServer).toBe('function');
    expect(typeof plugin.generateBundle).toBe('function');
  });

  it('rewrites requests to shilploka.html in configureServer middleware', () => {
    const plugin = legacyEntryAlias();
    let middlewareFn = null;
    const mockServer = {
      middlewares: {
        use: vi.fn((fn) => {
          middlewareFn = fn;
        }),
      },
    };

    plugin.configureServer(mockServer);
    expect(mockServer.middlewares.use).toHaveBeenCalled();

    const req1 = { url: '/shilploka.html' };
    const next1 = vi.fn();
    middlewareFn(req1, {}, next1);
    expect(req1.url).toBe('/index.html');
    expect(next1).toHaveBeenCalled();

    const req2 = { url: '/shilploka.html?mobile=true' };
    const next2 = vi.fn();
    middlewareFn(req2, {}, next2);
    expect(req2.url).toBe('/index.html?mobile=true');
    expect(next2).toHaveBeenCalled();

    const req3 = { url: '/other.html' };
    const next3 = vi.fn();
    middlewareFn(req3, {}, next3);
    expect(req3.url).toBe('/other.html');
    expect(next3).toHaveBeenCalled();
  });

  it('emits legacy entry asset in generateBundle', () => {
    const plugin = legacyEntryAlias();
    const context = {
      emitFile: vi.fn(),
    };

    plugin.generateBundle.call(context);
    expect(context.emitFile).toHaveBeenCalledWith({
      type: 'asset',
      fileName: 'shilploka.html',
      source: LEGACY_ENTRY_HTML,
    });
  });
});
