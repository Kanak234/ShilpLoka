# ShilpLoka (शिल्पलोक)

An Ancient-Indian-themed voxel sandbox: build with Harappan baked bricks, trade
spices with Dhanapati the merchant by barter, craft at the Vedic altar, and walk
among banyan and peepal trees around an indestructible Ashoka Sthambha. Built
with [Three.js](https://threejs.org/) and bundled by [Vite](https://vitejs.dev/)
into a single self-contained HTML file.

## Play

```bash
npm ci
npm run dev        # http://localhost:5173
```

Click the game to lock the mouse, then:

| Action | Keys |
|---|---|
| Move | W A S D |
| Jump | Space |
| Crouch | Shift |
| Toggle flying | F |
| Mine a block | Left click |
| Place the held block | Right click |
| Choose hotbar slot | 1 – 9, or click the slot |
| Barter with Dhanapati | B |
| Vedic crafting altar | C |
| Learning Center (guide) | H |

On phones and tablets an on-screen joystick and buttons appear automatically.
To force them on a desktop browser, open `?mobile=true`.

The Ashoka Sthambha and the Great Bath masonry are heritage blocks: they cannot
be mined. Every block you can place — baked brick, soil, sandstone, basalt,
banyan wood and bitumen mortar — can be mined again, and returns to your
inventory.

## Saves

Your world saves itself. There is nothing to press.

- **What is saved:** only the blocks you changed, plus the world seed, your
  position and view direction, your inventory, and the merchant's stock. The
  terrain regenerates exactly from the seed, so an entire built-up world is
  usually a few kilobytes.
- **When:** every 30 seconds, when the tab is hidden (switching apps on a phone),
  and when the page is closed. A "Saved ✓" note appears bottom-left.
- **Where:** the browser's `localStorage`, key `shilploka.save.v1`. A save
  belongs to one browser on one device; clearing site data deletes it.
- **Size:** the HUD warns once a save passes 4 MB, since browsers cap
  `localStorage` at around 5 MB.
- **New World:** the 🌱 button in the toolbar (or on the mobile toolbar)
  deletes the save, after asking, and starts a fresh random world.
- **Private browsing:** where storage is blocked the game still runs, and says
  that saving is unavailable.
- **A save that can't be opened** (damaged, or written by a newer version) is
  never overwritten. Its exact text is copied to
  `shilploka.save.v1.backup-<timestamp>` first and the game says so. If storage
  is too full even for that copy, saving is paused until you choose New World.
- **Format versions:** every save carries `version`. Version 1 is the first
  format; the earlier builds (including the one on itch.io) saved nothing, so
  there is nothing to migrate. Future formats upgrade through
  `SAVE_MIGRATIONS` in `shilp_save.js`.

## Develop

```bash
npm test               # vitest: inventory, save system, world, barter
npm run build          # production build -> dist/index.html (single file)
```

The live game starts at `index.html` → `src/shilploka/shilploka_main.js` →
`src/shilploka/core/shilp_engine.js`. `shilploka.html` is an identical copy of
`index.html`; change both.

## Releases — what is real

| File in `release/` | What it actually is |
|---|---|
| `shilploka-itch-web.zip` | The web game, `index.html` at the zip root, ready for itch.io (project kind: HTML). |
| `shilploka-offline-web-linux.tar.gz`<br>`shilploka-offline-web-windows.zip`<br>`shilploka-offline-web-mac.zip` | The same web game packaged to play **offline in a browser**: `ShilpLoka.html`, a launcher, and a README. **Not native apps** — they open your browser. |
| `shilploka-android-studio-project.zip` | The Android **source project**, to open in Android Studio. **Not an installable APK.** |

```bash
npm run build:web      # itch.io zip
npm run build:linux    # offline web package (also build:win, build:mac)
npm run build:android  # Android Studio project zip
npm run build:all      # all of the above
```

No APK and no native desktop binary are produced unless the toolchain for them
is installed; the scripts say so and skip rather than ship something that only
looks like one. (Earlier versions of these scripts renamed a zip of the Android
source to `.apk`, and wrapped the web page in launchers named as if they were
native builds.)

### Android

`android/` is a hand-written Android app: `MainActivity` hosts a WebView that
loads the game from `app/src/main/assets/public/index.html`. It is **not** a
Capacitor project, so `npx cap sync android` does not apply to it.

`npm run build:android` builds the game and copies it into that assets folder.
To get an APK, open `android/` in Android Studio and use **Build → Build APK(s)**.

**Known gaps — this project has not been built or verified:** `android/` has no
`settings.gradle`, so Gradle does not know the `:app` module exists, and no
`gradle.properties`, so `android.useAndroidX` is unset while the app depends on
`androidx.appcompat`, which the Android Gradle Plugin rejects. Both must be
added before it will build. Alternatively, convert the project to Capacitor
(`npm i -D @capacitor/cli @capacitor/core @capacitor/android`, then
`npx cap add android`, which replaces `android/`); after that the build flow is
`npm run build && npx cap sync android`, then Android Studio.

### Desktop (native)

`src-tauri/` holds a Tauri **1.5** configuration. `build:linux|win|mac` builds
native bundles with it only when all of this is present; otherwise it skips:

- the Tauri CLI, installed (`npm i -D @tauri-apps/cli@^1` or
  `cargo install tauri-cli --version "^1"`);
- on Linux, the WebKitGTK **4.0** development libraries. Tauri 1.x links 4.0,
  and current Ubuntu releases only ship 4.1, so on those systems the realistic
  route is upgrading `src-tauri` to Tauri 2.

## Verification scripts

`npm run verify` and `npm run verify:all` drive the game in a headless Microsoft
Edge (`/usr/bin/microsoft-edge-stable`) against a running `npm run dev`, check
behaviour through `window.__SHILPLOKA__`, and write screenshots into
`docs/screenshots/`. They record screenshots; they do not compare them against
reference images.

## Requires

- Node.js 20+ and npm
- WebGL2 in the browser
- For `verify`: Microsoft Edge at the path above
