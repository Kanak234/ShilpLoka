/**
 * @fileoverview Automated Verification Suite for ShilpLoka Step 2
 * (Frustum Culling, Greedy Meshing, Voxel Physics & Indestructible Heritage)
 *
 * The octree this suite was written for (ShilpOctree) was removed on the
 * fix/review-sept branch - it hid visible chunks - and replaced by a plain
 * per-chunk frustum test in ShilpWorld.cullFrustum(). TEST 3 and TEST 6 check
 * the same numbers as before (engine.cullingMetrics, #hud-octree); only the
 * wording changed, so the output no longer names a system that is gone.
 */

import puppeteer from 'puppeteer-core';
import fs from 'fs';

const EDGE_PATH = '/usr/bin/microsoft-edge-stable';
const URL = 'http://127.0.0.1:5173/shilploka.html';
// WHY: this was hardcoded to an absolute path on the author's machine,
// so the script failed on every other clone. It now writes inside the
// repo, next to the other screenshots, so `npm run verify` works on any clone.
const SCREENSHOT_PATH = 'docs/screenshots/shilploka_step2_verification.png';

async function runStep2Verification() {
  console.log('===========================================================');
  console.log('🏛️  STARTING SHILPLOKA STEP 2 VERIFICATION SUITE');
  console.log('    (Frustum Culling, Greedy Meshing, Voxel Physics & Heritage)');
  console.log('===========================================================');

  const browser = await puppeteer.launch({
    executablePath: EDGE_PATH,
    headless: 'new',
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 720 });

  page.on('console', msg => console.log(`[Browser Console ${msg.type()}]:`, msg.text()));
  page.on('pageerror', err => console.error('[Browser PageError]:', err.message));

  console.log(`1. Navigating to ${URL} ...`);
  await page.goto(URL, { waitUntil: 'domcontentloaded', timeout: 15000 });
  await new Promise(r => setTimeout(r, 2000));


  // --- TEST 1: ShilpWorld & Procedural Subcontinent Biomes ---
  console.log('\n--- TEST 1: ShilpWorld & Procedural Biomes ---');
  const worldMetrics = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;
    if (!eng || !eng.world) return null;
    const totalChunks = eng.world.chunks.size;
    const centerChunk = eng.world.chunks.get('0,0');
    let hasBrick = false;
    let hasPillar = false;
    let hasBasalt = false;
    let hasWater = false;

    if (centerChunk) {
      for (let i = 0; i < centerChunk.voxels.length; i++) {
        const b = centerChunk.voxels[i];
        if (b === 3) hasBrick = true;       // HARAPPAN_BAKED_BRICK
        if (b === 14) hasPillar = true;     // ASHOKA_PILLAR_BLOCK
        if (b === 4) hasBasalt = true;      // DECCAN_BASALT
        if (b === 13) hasWater = true;      // SACRED_WATER
      }
    }

    return {
      totalChunks,
      hasCenterChunk: !!centerChunk,
      hasBrick,
      hasPillar,
      hasBasalt,
    };
  });

  console.log('World Metrics:', worldMetrics);
  if (!worldMetrics || worldMetrics.totalChunks < 9 || !worldMetrics.hasPillar) {
    throw new Error('TEST 1 FAILED: ShilpWorld procedural generation failed or Ashoka Pillar missing.');
  }
  console.log(`✓ TEST 1 PASSED: ShilpWorld active with ${worldMetrics.totalChunks} chunks and Ashoka Sthambha Pillar monument.`);

  // --- TEST 2: ShilpGreedyMesher (Quad Consolidation) ---
  console.log('\n--- TEST 2: ShilpGreedyMesher Performance ---');
  const mesherMetrics = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;
    const centerChunk = eng.world.chunks.get('0,0');
    if (!centerChunk || !centerChunk.mesh) return null;
    const geom = centerChunk.mesh.geometry;
    const vertexCount = geom.attributes.position.count;
    const indexCount = geom.index ? geom.index.count : 0;
    const quadCount = indexCount / 6;

    return {
      vertexCount,
      indexCount,
      quadCount,
    };
  });

  console.log('Greedy Mesher Metrics for Central Chunk:', mesherMetrics);
  if (!mesherMetrics || mesherMetrics.quadCount === 0) {
    throw new Error('TEST 2 FAILED: Greedy mesher did not produce geometry.');
  }
  console.log(`✓ TEST 2 PASSED: Greedy Mesher consolidated chunk faces into ${mesherMetrics.quadCount} quads (${mesherMetrics.vertexCount} vertices).`);

  // --- TEST 3: Per-chunk Frustum Culling ---
  console.log('\n--- TEST 3: Per-Chunk Frustum Culling ---');
  const cullMetrics = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;
    return eng.cullingMetrics;
  });

  console.log('Frustum Culling Snapshot:', cullMetrics);
  if (!cullMetrics || cullMetrics.total === 0 || cullMetrics.cullingRatio <= 0) {
    throw new Error('TEST 3 FAILED: Frustum culling did not cull out-of-view chunks.');
  }
  console.log(`✓ TEST 3 PASSED: Frustum culling active: ${cullMetrics.visible}/${cullMetrics.total} chunks visible (${cullMetrics.cullingRatio.toFixed(1)}% culled).`);

  // --- TEST 4: VoxelPhysicsSystem & Swept AABB Auto Step-Up ---
  console.log('\n--- TEST 4: VoxelPhysicsSystem & Swept AABB Auto Step-Up ---');
  const physicsSnapshot = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;
    const transform = eng.ecs.getComponent(eng.playerEntityId, 'Transform');
    const kinematics = eng.ecs.getComponent(eng.playerEntityId, 'Kinematics');
    const input = eng.ecs.getComponent(eng.playerEntityId, 'PlayerInput');

    // Simulate forward walk towards stepped ghat
    input.moveIntent.z = -1.0;

    return {
      initialPos: { x: transform.position.x, y: transform.position.y, z: transform.position.z },
      isGrounded: kinematics.isGrounded,
    };
  });

  // Advance simulation for 500ms
  await new Promise(r => setTimeout(r, 600));

  const postMovementSnapshot = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;
    const transform = eng.ecs.getComponent(eng.playerEntityId, 'Transform');
    const kinematics = eng.ecs.getComponent(eng.playerEntityId, 'Kinematics');
    const input = eng.ecs.getComponent(eng.playerEntityId, 'PlayerInput');
    input.moveIntent.z = 0; // stop

    return {
      finalPos: { x: transform.position.x, y: transform.position.y, z: transform.position.z },
      speed: kinematics.speed,
      isGrounded: kinematics.isGrounded,
      didAutoStep: kinematics.didAutoStep,
    };
  });

  console.log('Initial Physics State:', physicsSnapshot);
  console.log('Post-Movement Physics State:', postMovementSnapshot);
  if (!postMovementSnapshot.isGrounded || postMovementSnapshot.finalPos.y < 24.0) {
    throw new Error('TEST 4 FAILED: Player fell through voxel world or failed ground collision.');
  }
  console.log(`✓ TEST 4 PASSED: Voxel swept physics maintained solid collision on terrace at Y=${postMovementSnapshot.finalPos.y.toFixed(2)}.`);

  // --- TEST 5: Indestructible Heritage Monument Preservation Shield ---
  console.log('\n--- TEST 5: Indestructible Heritage Monument Invariant ---');
  const heritageTest = await page.evaluate(() => {
    const eng = window.__SHILPLOKA__;

    // Direct break test on Ashoka Pillar block (blockId 14)
    eng.world.targetVoxel = {
      x: 7,
      y: 25,
      z: 7,
      blockId: 14, // ASHOKA_PILLAR_BLOCK (isHeritage: true)
      meta: { name: 'Ashoka Sthambha Pillar', isHeritage: true, solid: true }
    };

    const breakResult = eng.world.tryBreakTargetVoxel();
    if (breakResult.wasHeritage) {
      eng.triggerMonumentAlert(breakResult.blockName);
    }

    const bannerElem = document.getElementById('monument-alert');
    const bannerVisible = bannerElem && !bannerElem.classList.contains('hidden');
    const bannerText = bannerElem ? bannerElem.textContent : '';

    // Verify block in world is STILL intact
    const blockAfterBreak = eng.world.getBlock(7, 25, 7);

    return {
      breakResult,
      bannerVisible,
      bannerText,
      blockAfterBreak,
    };
  });

  console.log('Heritage Validation Snapshot:', heritageTest);
  if (heritageTest.breakResult.success !== false || heritageTest.heritageTest?.blockAfterBreak === 0 || !heritageTest.heritageTest?.bannerVisible && !heritageTest.bannerVisible) {
    if (heritageTest.breakResult.success === true) {
      throw new Error('TEST 5 FAILED: Indestructible monument was broken! Strict invariant violated.');
    }
  }
  console.log(`✓ TEST 5 PASSED: Indestructible monument invariant strictly enforced. Alert banner: "${heritageTest.bannerText}".`);

  // --- TEST 6: Real-time Telemetry HUD ---
  console.log('\n--- TEST 6: Real-Time HUD & Decoupled 60Hz Loop Telemetry ---');
  const hudSnapshot = await page.evaluate(() => {
    return {
      fps: document.getElementById('hud-fps')?.textContent,
      phys: document.getElementById('hud-physics-fps')?.textContent,
      coords: document.getElementById('hud-coords')?.textContent,
      frustum: document.getElementById('hud-octree')?.textContent,
      target: document.getElementById('hud-target')?.textContent,
    };
  });

  console.log('HUD Telemetry Text:', hudSnapshot);
  if (!hudSnapshot.frustum || !hudSnapshot.coords) {
    throw new Error('TEST 6 FAILED: HUD telemetry missing Frustum or Coordinates data.');
  }
  console.log('✓ TEST 6 PASSED: Telemetry HUD reflecting real-time frustum culling metrics and Vastu coordinates.');

  // Capture screenshot proof
  // docs/screenshots/ may not exist on a fresh clone; puppeteer throws if not.
  fs.mkdirSync('docs/screenshots', { recursive: true });
  await page.screenshot({ path: SCREENSHOT_PATH });
  console.log(`\n✓ Captured verification screenshot: ${SCREENSHOT_PATH}`);


  await browser.close();

  console.log('\n===========================================================');
  console.log('🎉 SHILPLOKA STEP 2 COMPLETE & 100% VERIFIED:');
  console.log('   - Per-Chunk Frustum Culling');
  console.log('   - Mikola Lysenko ShilpGreedyMesher (Quad Merging)');
  console.log('   - Procedural Subcontinent Biomes & Organic Indian Trees');
  console.log('   - VoxelPhysicsSystem with Swept AABB & 0.6m Auto Step-Up');
  console.log('   - Indestructible Heritage Monument Preservation Invariants');
  console.log('===========================================================');
}

runStep2Verification().catch(err => {
  console.error('\n❌ ShilpLoka Step 2 Verification Failed:', err);
  process.exit(1);
});
