/**
 * @fileoverview Keeps the old `shilploka.html` address working, generated from
 * index.html instead of kept as a second hand-edited copy.
 *
 * WHY THIS EXISTS
 *   The repo used to carry shilploka.html as a byte-identical copy of
 *   index.html ("change both", said the README). Two copies of the entry page
 *   drift the moment someone edits one. index.html is now the only source.
 *   But some things still ask for /shilploka.html:
 *     - verify_shilploka_step1/2/3.js and verify_mobile_controls.js open
 *       http://127.0.0.1:5173/shilploka.html on the dev server;
 *     - that address has been public on GitHub Pages, which serves main.
 *   So the name is kept, and generated - never edited by hand.
 *
 * WHAT IT DOES
 *   - Dev server (`npm run dev`): /shilploka.html is answered with index.html,
 *     query string kept (?mobile=true matters to verify_mobile_controls.js).
 *     No file is involved; Vite transforms index.html as usual.
 *   - Build (`npm run build`): emits dist/shilploka.html as a ~0.5 KB page
 *     that forwards to index.html, carrying over ?query and #hash.
 *     WHY a forwarder, not a copy of the 640 KB game: build_itch.js zips all
 *     of dist/, so a copy would double the upload for an alias nobody needs
 *     there; build_desktop.js and build_android.js take only index.html.
 *
 * USED BY: vite.config.js (plugins list).
 * NEXT: tests/html_entry.test.js builds and serves the project to check both.
 */

/** The retired file name, kept as an address only. */
export const LEGACY_ENTRY = 'shilploka.html';

/**
 * The forwarding page written to dist/shilploka.html.
 * location.replace keeps the back button clean (no bounce back to the alias);
 * the <meta refresh> and the link are fallbacks for script-blocked browsers.
 */
export const LEGACY_ENTRY_HTML = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<title>ShilpLoka | शिल्पलोक</title>
<meta http-equiv="refresh" content="0; url=index.html" />
<script>location.replace('index.html' + location.search + location.hash);</script>
</head>
<body><p>ShilpLoka has moved to <a href="index.html">index.html</a>.</p></body>
</html>
`;

/** @returns {import('vite').Plugin} */
export function legacyEntryAlias() {
  return {
    name: 'shilploka-legacy-entry',

    // Dev: rewrite the request before Vite's own HTML handling sees it.
    // Registered directly (not in a returned function) so it runs FIRST.
    configureServer(server) {
      server.middlewares.use((req, _res, next) => {
        const [path, query = ''] = (req.url ?? '').split('?');
        if (path === `/${LEGACY_ENTRY}`) req.url = `/index.html${query ? `?${query}` : ''}`;
        next();
      });
    },

    // Build: add the forwarder next to the real, single-file index.html.
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: LEGACY_ENTRY, source: LEGACY_ENTRY_HTML });
    },
  };
}
