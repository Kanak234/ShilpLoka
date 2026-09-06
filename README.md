# ShilpLoka (शिल्पलोक)

An Ancient-Indian-themed voxel sandbox game — a browser-based block builder
built with [Three.js](https://threejs.org/) and bundled by [Vite](https://vitejs.dev/).

## Run in development

```bash
npm install
npm run dev        # Vite dev server on http://localhost:5173
```

## Build

```bash
npm run build          # production web build (dist/)
npm run build:web      # itch.io web package
npm run build:linux    # desktop build (also :win, :mac)
npm run build:android  # Android build
npm run build:all      # every target
```

## Verify

```bash
npm run verify         # step-3 verification
npm run verify:all     # all verification steps
```

The `*_verification.png` files in the repo are the reference screenshots those
verification scripts compare against.

## Requires

- Node.js and npm
- Dependencies (installed by `npm install`): `three`, `vite`, `puppeteer-core`
  (the last drives the headless verification screenshots).

## Entry points

Several HTML entry points exist for different builds: `index.html`,
`shilploka.html`, `nirmana.html`, `standalone.html`, `dev.html`.

## Status

Playable web build. The desktop/Android build scripts (`build:linux`,
`build:win`, `build:mac`, `build:android`) depend on platform toolchains that
must be present locally; they are not exercised by the web build.
