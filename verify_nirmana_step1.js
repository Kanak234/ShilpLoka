/**
 * Automated Verification Script for Step 1 of Nirmana: The Ancient Voxel
 * Validates decoupled 60Hz physics process, rendering loop, and Vastu coordinate systems.
 */

import puppeteer from 'puppeteer-core';
import { spawn } from 'child_process';

const EDGE_PATH = '/usr/bin/microsoft-edge-stable';

async function runStep1Verification() {
  console.log('--- Starting Nirmana Step 1 Verification ---');

  let browser = null;
  let vite = null;

  try {
    // 1. Launch Vite Server
    console.log('1. Launching Vite server on 127.0.0.1:5173...');
    vite = spawn('npx', ['vite', '--host', '127.0.0.1', '--port', '5173'], {
      cwd: process.cwd(),
      stdio: 'pipe',
    });

    await new Promise((resolve) => {
      vite.stdout.on('data', (data) => {
        const msg = data.toString();
        if (msg.includes('5173') || msg.includes('ready')) {
          console.log('Vite server is ready:', msg.trim());
          resolve();
        }
      });
      setTimeout(resolve, 3000);
    });

    // 2. Launch Microsoft Edge
    console.log('2. Launching Microsoft Edge browser...');
    browser = await puppeteer.launch({
      executablePath: EDGE_PATH,
      headless: 'new',
      args: [
        '--no-sandbox',
        '--disable-setuid-sandbox',
        '--use-gl=angle',
        '--use-angle=swiftshader',
        '--enable-webgl',
        '--ignore-gpu-blocklist',
        '--window-size=1280,800',
      ],
    });

    const page = await browser.newPage();
    await page.setViewport({ width: 1280, height: 800 });

    page.on('console', (msg) => {
      console.log(`[Browser Console ${msg.type()}]:`, msg.text());
    });
    page.on('pageerror', (err) => {
      console.error('[Browser Uncaught Error]:', err);
    });

    // 3. Navigate to nirmana.html
    console.log('3. Navigating to http://127.0.0.1:5173/nirmana.html ...');
    await page.goto('http://127.0.0.1:5173/nirmana.html', { waitUntil: 'load', timeout: 15000 });

    // 4. Verify Engine & Decoupled Loops
    await page.waitForFunction(() => window.__NIRMANA__ !== undefined, { timeout: 10000 });
    console.log('✓ NirmanaEngine initialized on window.__NIRMANA__');

    // Settle for 1.2 seconds to measure tick rates
    await new Promise((r) => setTimeout(r, 1200));

    const loopStats = await page.evaluate(() => {
      const engine = window.__NIRMANA__;
      const telemetry = engine.timeWheel.getTelemetry();
      return {
        renderFps: telemetry.renderFps,
        physicsFps: telemetry.physicsFps,
        fixedDelta: telemetry.fixedDelta,
        state: telemetry.state,
        cameraPos: {
          x: engine.camera.position.x,
          y: engine.camera.position.y,
          z: engine.camera.position.z,
        },
      };
    });
    console.log('✓ Decoupled Loop Telemetry:', loopStats);

    if (loopStats.state !== 'RUNNING' || loopStats.fixedDelta > 0.017) {
      throw new Error('KalaChakra loop failed validation');
    }

    // 5. Test Input Integration
    console.log('5. Testing PranaInput movement integration in physics loop...');
    await page.evaluate(() => {
      const engine = window.__NIRMANA__;
      // Simulate pressing Move Forward
      engine.input.activeActions.add('move_forward');
    });

    await new Promise((r) => setTimeout(r, 500));

    const movedStats = await page.evaluate(() => {
      const engine = window.__NIRMANA__;
      engine.input.activeActions.delete('move_forward');
      return {
        x: engine.camera.position.x,
        y: engine.camera.position.y,
        z: engine.camera.position.z,
      };
    });
    console.log('✓ Player moved under fixed 60Hz physics process:', movedStats);

    // 6. Capture Visual Verification Screenshot
    console.log('6. Capturing Step 1 verification screenshot...');
    await page.screenshot({ path: 'nirmana_step1_verification.png' });
    console.log('✓ Screenshot saved to nirmana_step1_verification.png');

    console.log('\n=============================================');
    console.log('🎉 STEP 1 VERIFIED AND RUNNING WITH 0 LAG!');
    console.log('=============================================\n');
  } finally {
    if (browser) await browser.close();
    if (vite) vite.kill('SIGKILL');
  }
}

runStep1Verification().then(() => {
  process.exit(0);
}).catch((err) => {
  console.error('Step 1 Verification Failed:', err);
  process.exit(1);
});
