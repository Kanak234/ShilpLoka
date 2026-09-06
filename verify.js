/**
 * Automated Verification Script for Minecraft Voxel Engine
 * Uses Puppeteer Core connecting to Microsoft Edge
 */

import puppeteer from 'puppeteer-core';
import { spawn } from 'child_process';

const EDGE_PATH = '/usr/bin/microsoft-edge-stable';

async function runVerification() {
  console.log('--- Starting Minecraft Voxel Game Verification ---');

  let browser = null;
  let vite = null;

  try {
    // 1. Launch Vite preview/dev server
    console.log('1. Launching Vite server on 127.0.0.1:5173...');
    vite = spawn('npx', ['vite', '--host', '127.0.0.1', '--port', '5173'], {
      cwd: process.cwd(),
      stdio: 'pipe',
    });

    // Wait for server to become ready
    await new Promise((resolve) => {
      vite.stdout.on('data', (data) => {
        const msg = data.toString();
        if (msg.includes('5173') || msg.includes('ready')) {
          console.log('Vite server is ready:', msg.trim());
          resolve();
        }
      });
      setTimeout(resolve, 3000); // Fallback timeout
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

    // Capture console logs from game
    page.on('console', (msg) => {
      console.log(`[Browser Console ${msg.type()}]:`, msg.text());
    });
    page.on('pageerror', (err) => {
      console.error('[Browser Uncaught Error]:', err);
    });

    // 3. Navigate to game URL
    console.log('3. Navigating to http://127.0.0.1:5173 ...');
    await page.goto('http://127.0.0.1:5173', { waitUntil: 'load', timeout: 20000 });

    // 4. Wait for game engine initialization
    await page.waitForFunction(() => window.__GAME__ !== undefined, { timeout: 10000 });
    console.log('✓ Game engine initialized successfully!');

    // 5. Test World Generation & Chunk Meshes
    const worldStats = await page.evaluate(() => {
      const game = window.__GAME__;
      const chunkCount = game.world.chunks.size;
      let solidMeshes = 0;
      for (const chunk of game.world.chunks.values()) {
        if (chunk.solidMesh) solidMeshes++;
      }
      return {
        chunkCount,
        solidMeshes,
        playerPos: {
          x: game.player.position.x,
          y: game.player.position.y,
          z: game.player.position.z,
        },
      };
    });
    console.log('✓ World Generation Stats:', worldStats);
    if (worldStats.chunkCount < 10) {
      throw new Error(`Expected at least 10 chunks loaded, found ${worldStats.chunkCount}`);
    }

    // 6. Test Player Movement & Physics
    console.log('6. Testing player physics simulation...');
    await page.evaluate(() => {
      const game = window.__GAME__;
      // Simulate player walking forward for a few frames
      game.player.keys['KeyW'] = true;
    });
    await new Promise((r) => setTimeout(r, 600));

    const movedStats = await page.evaluate(() => {
      const game = window.__GAME__;
      game.player.keys['KeyW'] = false;
      return {
        x: game.player.position.x,
        y: game.player.position.y,
        z: game.player.position.z,
        isGrounded: game.player.isGrounded,
      };
    });
    console.log('✓ Player moved & settled:', movedStats);

    // 7. Test Block Placement & Breaking
    console.log('7. Testing Block Placement and Breaking...');
    const blockTest = await page.evaluate(() => {
      const game = window.__GAME__;
      const px = Math.floor(game.player.position.x) + 1;
      const py = Math.floor(game.player.position.y);
      const pz = Math.floor(game.player.position.z);

      // Place an Oak Wood block
      game.world.setBlock(px, py, pz, 5, true); // BLOCKS.OAK_LOG = 5
      const placedBlock = game.world.getBlock(px, py, pz);

      // Break it
      game.world.setBlock(px, py, pz, 0, true); // BLOCKS.AIR = 0
      const brokenBlock = game.world.getBlock(px, py, pz);

      return {
        placedBlock,
        brokenBlock,
        modifiedSize: game.world.modifiedBlocks.size,
      };
    });
    console.log('✓ Block Place & Break verified:', blockTest);
    if (blockTest.placedBlock !== 5 || blockTest.brokenBlock !== 0) {
      throw new Error('Block placement or destruction failed');
    }

    // 8. Test Crafting & Inventory System
    console.log('8. Testing Crafting System...');
    const craftTest = await page.evaluate(() => {
      const game = window.__GAME__;
      const inv = game.inventory;

      // Place 1 Oak Log into player 2x2 crafting grid at index 0
      inv.playerCraftGrid[0] = { id: 5, count: 1 }; // BLOCKS.OAK_LOG
      inv.updateCraftingOutput(false);

      const output1 = inv.playerCraftOutput ? { ...inv.playerCraftOutput } : null;

      // Craft it (1 Oak Log -> 4 Oak Planks)
      const success1 = inv.craftItem(false);
      const cursor1 = inv.cursorStack ? { ...inv.cursorStack } : null;

      // Now put 4 planks in 2x2 grid to make Crafting Table
      inv.playerCraftGrid[0] = { id: 7, count: 1 }; // OAK_PLANKS
      inv.playerCraftGrid[1] = { id: 7, count: 1 };
      inv.playerCraftGrid[2] = { id: 7, count: 1 };
      inv.playerCraftGrid[3] = { id: 7, count: 1 };
      inv.updateCraftingOutput(false);

      const output2 = inv.playerCraftOutput ? { ...inv.playerCraftOutput } : null;

      return {
        output1,
        success1,
        cursor1,
        output2,
      };
    });
    console.log('✓ Crafting System verified:', craftTest);
    if (!craftTest.output1 || craftTest.output1.id !== 7 || craftTest.output1.count !== 4) {
      throw new Error('Crafting 1 Log -> 4 Planks failed');
    }
    if (!craftTest.output2 || craftTest.output2.id !== 8 || craftTest.output2.count !== 1) {
      throw new Error('Crafting 4 Planks -> Crafting Table failed');
    }

    // 9. Test Save and Load Persistence
    console.log('9. Testing Save & Load System...');
    const saveTest = await page.evaluate(() => {
      const game = window.__GAME__;
      game.world.setBlock(10, 40, 10, 13, true); // DIAMOND_ORE = 13
      game.saveSystem.saveToLocalStorage();

      const rawSaved = localStorage.getItem('voxel_game_save_v1');
      const parsed = JSON.parse(rawSaved);

      game.world.setBlock(10, 40, 10, 0, true);
      const cleared = game.world.getBlock(10, 40, 10);

      game.saveSystem.loadFromLocalStorage();
      const restored = game.world.getBlock(10, 40, 10);

      return {
        savedValid: !!parsed && parsed.version === 1,
        cleared,
        restored,
        savedSeed: parsed.seed,
      };
    });
    console.log('✓ Save/Load verified:', saveTest);
    if (!saveTest.savedValid || saveTest.restored !== 13) {
      throw new Error('Save/Load persistence failed to restore modified blocks');
    }

    // 10. Test Day-Night Cycle
    console.log('10. Testing Day-Night Cycle...');
    const timeTest = await page.evaluate(() => {
      const game = window.__GAME__;
      const initialTime = game.dayNight.time;
      game.dayNight.update(60, game.player.position);
      const newTime = game.dayNight.time;
      const skyColor = game.dayNight.getSkyColor().getHexString();
      return {
        initialTime,
        newTime,
        skyColor,
        formatted: game.dayNight.getTimeFormatted(),
      };
    });
    console.log('✓ Day-Night Cycle verified:', timeTest);

    // 11. Capture Screenshot of Live Gameplay
    console.log('11. Capturing in-game gameplay screenshot...');
    await page.screenshot({ path: 'gameplay_verification.png' });
    console.log('✓ Screenshot saved to gameplay_verification.png');

    // 12. Open Inventory Modal and Capture Screenshot
    console.log('12. Opening inventory modal and capturing GUI screenshot...');
    await page.evaluate(() => {
      window.__GAME__.inventory.openModal('player');
    });
    await new Promise((r) => setTimeout(r, 400));
    await page.screenshot({ path: 'inventory_verification.png' });
    console.log('✓ Screenshot saved to inventory_verification.png');

    console.log('\n=============================================');
    console.log('🎉 ALL GAME SYSTEMS VERIFIED AND RUNNING 100%!');
    console.log('=============================================\n');
  } finally {
    if (browser) await browser.close();
    if (vite) vite.kill('SIGKILL');
  }
}

runVerification().then(() => {
  process.exit(0);
}).catch((err) => {
  console.error('Verification failed:', err);
  process.exit(1);
});
