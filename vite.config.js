import { defineConfig } from 'vite';
import { viteSingleFile } from 'vite-plugin-singlefile';

export default defineConfig({
  plugins: [viteSingleFile()],
  build: {
    target: 'esnext',
    assetsInlineLimit: 100000000,
    chunkSizeWarningLimit: 100000000,
    cssCodeSplit: false,
    sourcemap: false,
    rollupOptions: {
      output: {
        format: 'iife',
        // WHAT: the global variable name Vite gives the IIFE bundle.
        // WHY:  it previously carried another game's name, left over from
        //       the project's origin. It now uses ShilpLoka's own name, so
        //       nothing in the shipped build refers to a different product.
        // WHERE: visible only as window.ShilpLoka in the built dist/index.html.
        name: 'ShilpLoka',
        inlineDynamicImports: true,
      },
    },
  },
});
