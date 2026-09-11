/**
 * @fileoverview Android packaging for ShilpLoka - honest edition.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHAT WAS WRONG BEFORE
 *   This script copied the Android *source project* zip and renamed the copy
 *   to `shilploka-mobile.apk`. The two files were byte-identical: a zip of Java
 *   source and Gradle files, with no compiled code (no classes.dex) and no
 *   manifest at the root. Android refuses to install it. It then printed
 *   "✓ Android Mobile Package Created".
 *
 *   It was also dishonest when Gradle WAS present: a failed Gradle build was
 *   caught and only printed a warning, and even a successful one was ignored in
 *   favour of the renamed zip.
 *
 * WHAT IT DOES NOW
 *   1. Builds the game (vite) and copies dist/index.html into the Android
 *      project's assets, where MainActivity's WebView loads it from.
 *   2. Packages the Android project as a zip named for what it is: a project
 *      to open in Android Studio, not an installable app.
 *   3. Builds a real APK ONLY if a Gradle toolchain is present. If that build
 *      fails, this script fails. If it succeeds, the APK Gradle actually
 *      produced is copied to release/. Without a toolchain, it says so and
 *      produces no .apk at all.
 *
 * THE ANDROID PROJECT ITSELF
 *   android/ is a hand-written WebView app, not a Capacitor project, so
 *   `npx cap sync android` does not apply to it. It is also missing
 *   settings.gradle and gradle.properties, which Gradle needs to build it.
 *   See the README "Android" section for the options.
 * ─────────────────────────────────────────────────────────────────────────────
 */

import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const ROOT_DIR = path.resolve(import.meta.dirname, '..');
const DIST_DIR = path.join(ROOT_DIR, 'dist');
const ANDROID_DIR = path.join(ROOT_DIR, 'android');
const ASSETS_PUBLIC = path.join(ANDROID_DIR, 'app', 'src', 'main', 'assets', 'public');
const RELEASE_DIR = path.join(ROOT_DIR, 'release');

/** Name chosen so nobody mistakes it for something installable. */
const PROJECT_ZIP = path.join(RELEASE_DIR, 'shilploka-android-studio-project.zip');

/** Stale outputs from the old script that must never be shipped again. */
const LEGACY_OUTPUTS = ['shilploka-mobile.apk', 'shilploka-android-project.zip'];

console.log('===========================================================');
console.log('📱 SHILPLOKA ANDROID PACKAGING');
console.log('===========================================================');

// 1. Build the game and place it where the WebView looks for it.
console.log('1. Building the game (vite build)...');
execSync('npm run build', { cwd: ROOT_DIR, stdio: 'inherit' });

fs.mkdirSync(ASSETS_PUBLIC, { recursive: true });
fs.copyFileSync(path.join(DIST_DIR, 'index.html'), path.join(ASSETS_PUBLIC, 'index.html'));
// MainActivity loads file:///android_asset/public/index.html.
console.log('✓ Copied dist/index.html into android/app/src/main/assets/public/');

fs.mkdirSync(RELEASE_DIR, { recursive: true });
for (const name of LEGACY_OUTPUTS) {
  const p = path.join(RELEASE_DIR, name);
  if (fs.existsSync(p)) {
    fs.unlinkSync(p);
    console.log(`  removed stale ${name}`);
  }
}

// 2. Package the project for Android Studio. Build output and IDE state are
//    excluded: they are machine-specific and regenerated on the next build.
console.log('2. Packaging the Android Studio project...');
if (fs.existsSync(PROJECT_ZIP)) fs.unlinkSync(PROJECT_ZIP);
execSync(
  `zip -qr "${PROJECT_ZIP}" android/ -x "*.git*" "android/.gradle/*" "android/build/*" ` +
  `"android/app/build/*" "android/.idea/*" "android/local.properties"`,
  { cwd: ROOT_DIR, stdio: 'inherit' }
);
console.log(`✓ ${path.relative(ROOT_DIR, PROJECT_ZIP)}`);
console.log('  This is SOURCE: open it in Android Studio to build an APK. It is not installable.');

// 3. A real APK, only if a real toolchain is here.
//    WHY both checks: a Gradle install alone cannot build this project -
//    Gradle also needs settings.gradle to know the :app module exists.
const gradlew = path.join(ANDROID_DIR, 'gradlew');
const hasSettings = ['settings.gradle', 'settings.gradle.kts']
  .some(f => fs.existsSync(path.join(ANDROID_DIR, f)));
let gradleCmd = null;
if (fs.existsSync(gradlew)) {
  gradleCmd = './gradlew';
} else {
  try {
    execSync('command -v gradle', { stdio: 'pipe', shell: '/bin/sh' });
    gradleCmd = 'gradle';
  } catch { /* no system gradle */ }
}

console.log('3. Native APK...');
if (!gradleCmd || !hasSettings) {
  const why = !gradleCmd ? 'no Gradle / gradlew found' : 'android/settings.gradle is missing';
  console.log(`  Skipped: ${why}. NO .apk was produced.`);
  console.log('  To get an APK, open the project in Android Studio (see README).');
} else {
  // No try/catch: if Gradle fails, this script must fail too. Catching the
  // error and reporting success is exactly the old behaviour being removed.
  execSync(`${gradleCmd} assembleDebug`, { cwd: ANDROID_DIR, stdio: 'inherit' });

  // Copy the file Gradle really made -- never a renamed stand-in.
  const apkDir = path.join(ANDROID_DIR, 'app', 'build', 'outputs', 'apk', 'debug');
  const built = fs.existsSync(apkDir) ? fs.readdirSync(apkDir).filter(f => f.endsWith('.apk')) : [];
  if (built.length === 0) {
    console.error('❌ Gradle reported success but produced no APK. Failing.');
    process.exit(1);
  }
  const out = path.join(RELEASE_DIR, 'shilploka-debug.apk');
  fs.copyFileSync(path.join(apkDir, built[0]), out);
  console.log(`✓ Real APK built by Gradle: ${path.relative(ROOT_DIR, out)} (debug-signed)`);
}
console.log('===========================================================');
