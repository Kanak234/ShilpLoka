# ShilpLoka (Java Edition) — Implementation Plan (Phases P0 – P18)

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Execution Philosophy & Quality Gates

In strict accordance with the Master Build Brief and User Rules:
1. **Linear Workflow Order**: Understand $\to$ Specify $\to$ Design $\to$ Plan $\to$ Implement $\to$ Verify $\to$ Document.
2. **Phase Gates**: Every phase P0 through P18 must conclude with:
   - Automated tests executed (`./gradlew test`).
   - Lint and code conventions verified.
   - Interactive game launch verification where applicable.
   - A detailed completion report stating: (a) what changed, (b) which files/components changed, (c) exact verification commands and empirical results.
   - A short plan for the subsequent phase posted, awaiting Kanak's explicit "go" before implementation commences.
3. **No Mocks / Zero Code Truncation**: No lazy placeholders (`// TODO`, `...`), no hardcoded dummy data arrays, and production-ready code with complete Javadoc comments.

---

## 2. Phase Breakdown (P0 to P18)

```
[P0: Docs & Skeleton] ──► [P1: Render Core] ──► [P2: Infinite Chunks] ──► [P3: Worldgen v1]
           │
           ▼
[P4: Player & Physics] ──► [P5: Light & Weather] ──► [P6: Save Pipeline] ──► [P7: Survival & Tools]
           │
           ▼
[P8: Workstations] ──► [P9: Entities & AI] ──► [P10: 11 Biomes] ──► [P11: History & Structures]
           │
           ▼
[P12: Villages & Trade] ──► [P13: Kaushal & Farming] ──► [P14: Dimensions & Boss]
           │
           ▼
[P15: Audio Engine] ──► [P16: UI & Localization] ──► [P17: Performance Pass] ──► [P18: Release Pack]
```

---

### Phase P0: Architecture, Documentation & Engine Skeleton (CURRENT)
* **Goal**: Establish the context documents, project build configuration, LWJGL 3 window bootstrap, logging infrastructure, crash handling, and local verification tooling.
* **Tasks**:
  1. Complete comprehensive specifications in `docs/`: `PRD.md`, `TRD.md`, `UI_UX_BRIEF.md`, `IMPLEMENTATION_PLAN.md`, `DATA_MODELS.md`, `CODING_CONVENTIONS.md`, `TESTING_STRATEGY.md`, `ASSET_LICENSES.md`, `HISTORY_SOURCES.md`, `OPEN_QUESTIONS.md`, and `docs/ADR/`.
  2. Configure Gradle Kotlin DSL build script (`build.gradle.kts`) with Java 21 LTS, LWJGL 3.4.2 native dependencies, JOML, Jackson, SLF4J/Logback, and JUnit 5.
  3. Set up the bootstrap application (`shilploka.app.Main`) and crash handler (`shilploka.app.CrashHandler`) writing to `crash-reports/`.
  4. Create executable verification script `./scripts/verify.sh` to automate build, compilation, test, and linting.
  5. Validate branding assets: Confirm Kanak's KP orbit logo in `assets/branding/logo-mark.svg`.
* **Deliverables**: Complete documentation suite, buildable skeleton, zero compilation warnings.
* **Verification**: `./gradlew build test` runs cleanly; `./scripts/verify.sh` passes 100%.

---

### Phase P1: Voxel Render Core & Camera
* **Goal**: Initialize the OpenGL 3.3 Core pipeline, compile shaders, upload a 2D Texture Array, construct a test chunk mesh, implement a flying 6-DOF camera, and display the F3 debug overlay.
* **Dependencies**: P0 completed and approved.
* **Tasks**:
  1. Implement `shilploka.render.Shader` supporting GLSL 330 core vertex and fragment compilation with error logging.
  2. Implement `shilploka.render.TextureArray` loading 16×16 PNG textures into a single `GL_TEXTURE_2D_ARRAY` with mipmaps.
  3. Implement `shilploka.render.Camera` with pitch, yaw, roll, view matrix, and perspective projection.
  4. Render a single 16³ chunk with basic vertex buffer objects (VAO, VBO).
  5. Implement `shilploka.ui.DebugOverlay` (F3 screen) displaying: FPS, frame time, player $(X, Y, Z)$, camera angles, GPU vendor/renderer, memory usage.
* **Verification**: Game launches in a 1280×720 window; textured test cube renders without artifacts; player flies smoothly using WASD and mouse look; F3 displays accurate real-time metrics.

---

### Phase P2: Infinite Chunks, Section Storage, Greedy Meshing & Frustum Culling [COMPLETED & VERIFIED]
* **Goal**: Build the infinite chunk column coordinate system, palette-compressed 16³ sections, background thread generation, greedy meshing, and frustum culling.
* **Dependencies**: P1.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (17/17 tests passing, live running tested with 180 ticks, 0 Minecraft references, 0 errors).
* **Tasks**:
  1. [x] Implement `shilploka.world.chunk.BitStorage` bit-packed storage array (4-bit/8-bit).
  2. [x] Implement `shilploka.world.chunk.ChunkSection` with palette-compressed block storage (4-bit/8-bit bitsets) and non-air tracking.
  3. [x] Implement `shilploka.world.chunk.ChunkColumn` (twenty 16³ sections, height 0 to 320).
  4. [x] Implement `shilploka.render.chunk.GreedyMesher` consolidating coplanar voxel faces per section (99% reduction on planar slabs).
  5. [x] Implement asynchronous chunk meshing in `shilploka.world.chunk.ChunkManager` worker pool.
  6. [x] Implement frustum culling (`shilploka.render.FrustumCuller`) with 6 Gribb-Hartmann extraction view planes.
* **Verification**: Unit test suite (`BitStorageTest`, `ChunkSectionTest`, `GreedyMesherTest`, `WorldGeneratorTest`, `CameraTest`) passing 100%. Headless/live automated runs verified. Greedy meshing coalesces 576 faces down to 6 quads on flat sheets. Clean native shutdown verified.

---

### Phase P3: World Generation v1 (Terrain Noise, 3 Biomes & Caves) [COMPLETED & VERIFIED]
* **Goal**: Implement deterministic OpenSimplex2 noise heightmaps, cave carvers, ore distribution, and 3 initial biomes (Ganga Maidan, Thar Marusthal, Himalaya Shikhar).
* **Dependencies**: P2.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (28/28 tests passing, live running tested with 180 ticks, 0 Minecraft references, 0 errors).
* **Tasks**:
  1. [x] Implement `shilploka.worldgen.noise.OpenSimplex2` with 2D/3D gradient noise and `OctaveNoise` fractal Brownian motion.
  2. [x] Implement biome climate distribution (`BiomeProvider`, `BiomeType`) selecting Ganga Maidan, Thar Marusthal, and Himalaya Shikhar.
  3. [x] Implement 3D noise cave carvers (`CaveCarver`) for subterranean worm tunnels and cavern rooms between Y=8 and Y=56.
  4. [x] Distribute underground mineral ore veins (`OrePopulator`, `OreType`) for Koyla, Taamba, Loha, Rajavarta, and Hira.
  5. [x] Implement procedural tree generators (`TreeGenerator`) for Sal (Shorea robusta), Deodar cedar, and Khejri trees.
  6. [x] Update `TextureArray` to 32 layers with authentic procedural pixel art for desert sand, snow, leaves, and mineral ores.
  7. [x] Create automated unit test suite (`WorldGenDeterminismTest`, `BiomeProviderTest`, `CaveCarverTest`, `OreDistributionTest`, `TreeGeneratorTest`).
* **Verification**: Bit-exact determinism verified across chunks and seeds; bedrock strata (Y=0..3) 100% preserved; all 5 ores spawn within correct elevation bands; live engine verifies terrain, dunes, mountains, trees, and caves.

---

### Phase P4: Player Entity, Physics, Collision & Creative Mode [COMPLETED & VERIFIED]
* **Goal**: Implement Axis-Aligned Bounding Box (AABB) collision, gravity, ground detection, jump/sneak/sprint mechanics, block raycasting, break/place actions, hotbar, and creative palette.
* **Dependencies**: P3.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (37/37 tests passing, live engine verified with 180 ticks, 0 Minecraft references, 0 errors).
* **Tasks**:
  1. [x] Implement `shilploka.entity.AABB` and swept-AABB collision detection against voxel geometry.
  2. [x] Implement player physics: gravity, terminal velocity, friction, walking, jumping, sneaking (ledge-fall prevention), sprinting.
  3. [x] Implement block raycasting (Amanatides-Woo Fast Voxel Traversal 3D DDA) targeting voxel faces within 5-block reach.
  4. [x] Implement Left-Click block break and Right-Click block placement with player bounding box intersection checks.
  5. [x] Implement 9-slot Hotbar selection (number keys 1–9 and mouse wheel scroll) with creative palette.
  6. [x] Implement Creative Mode fly toggle (`F` key) with 6-DOF movement and vertical ascending/descending.
  7. [x] Add real-time synchronous section remeshing on block break/place with neighbor border handling.
  8. [x] Create automated unit test suite (`AABBTest`, `RaycastTest`, `PlayerPhysicsTest`).
* **Verification**: Swept-AABB collision prevents tunneling; DDA raycast traces cardinal rays, face normals, and placement offsets; player falls, lands on ground, walks, jumps, and flies; block breaking and placing updates chunks immediately; 37/37 unit tests pass.

---

### Phase P5: Light Propagation, Day/Night Cycle & Fluid Simulation [COMPLETED & VERIFIED]
* **Goal**: Implement cellular automata sky and block light propagation, smooth vertex lighting with ambient occlusion, dynamic day/night celestial cycle, and fluid flow (water, lava).
* **Dependencies**: P4.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (52/52 tests passing, live engine verified with 180 ticks, 0 Minecraft references, 0 errors).
* **Tasks**:
  1. [x] Implement BFS light engine (`LightingEngine`) for 4-bit Sky Light (0–15) and 4-bit Block Light (0–15) with zero-allocation ring queues.
  2. [x] Implement 4-corner smooth vertex ambient occlusion (AO) and anisotropic diagonal flipping in `GreedyMesher`.
  3. [x] Implement dynamic 24,000-tick celestial day/night engine (`DayNightCycle`) with real-time sun angle, sunlight factor, 8 traditional lunar phases (Paksha), and atmospheric sky/fog colors.
  4. [x] Implement cellular automata fluid simulator (`FluidSimulator`) with downward gravity cascades, lateral flow (levels 8 down to 1), and water-lava contact reaction cooling magma into Deccan Basalt.
  5. [x] Implement gravitational falling block physics for Thar Sand and Bajri (gravel).
  6. [x] Update shaders (`chunk.vert`, `chunk.frag`) for 10-float vertex format, combined celestial + block lighting, AO, directional shading, and distance fog.
  7. [x] Add automated unit tests (`LightPropagationTest`, `AmbientOcclusionTest`, `DayNightCycleTest`, `FluidSimulationTest`).
* **Verification**: 52/52 tests pass; open sky casts 15 sunlight, caves remain at 0; Diyas illuminate radius 14 to 0; broken Diyas extinguish light; AO creases calculate correctly and flip anisotropic diagonals; 24,000 ticks simulate 20-min day with Paksha phases; water cascades and turns lava to Deccan Basalt; unsupported sand collapses to the ground; live engine verified with 0 errors.

---

### Phase P6: Persistence Engine (Region Format & Save/Load UI) [COMPLETED & VERIFIED]
* **Goal**: Implement the versioned `.slr` region file format, atomic staging writes, autosave every 5 minutes, singleplayer world selection, and world creation screens.
* **Dependencies**: P5.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (62/62 tests passing, live engine verified with 180 ticks, 0 Minecraft references, 0 errors).
* **Tasks**:
  1. [x] Implement `shilploka.world.save.RegionFile` (32×32 chunk containers, 4KB sector alignment, location and timestamp tables, Deflater ZLIB compression).
  2. [x] Implement `shilploka.world.save.ChunkSerializer` preserving dynamic voxel palettes, bit storage words, and cellular sky/block light arrays.
  3. [x] Implement atomic write commits (`.tmp` file creation $\to$ `sync()` $\to$ `ATOMIC_MOVE` with `.old` rollback backups) for `WorldMetadata` (`level.json`) and `PlayerData` (`player.json`).
  4. [x] Implement `shilploka.world.save.SaveManager` with background periodic autosave (5 minutes), dirty chunk flush, and shutdown hooks.
  5. [x] Implement pre-session automatic world backup into `backups/<world>-<date>.zip`.
  6. [x] Build UI screens (`shilploka.ui.WorldSelectScreen`): Singleplayer World List and Create World Screen with seed input and interactive text editing.
  7. [x] Implement automated save/load round-trip and crash-kill recovery tests (`RegionFileRoundTripTest`, `SaveManagerTest`).
* **Verification**: 62/62 tests pass; bit-exact round-trip verified for blocks and lighting across region files; chunk expansion relocates sectors cleanly; corrupt `level.json` and `player.json` automatically roll back to `.old`; uncommitted `.tmp` files left from abrupt process interruption are safely ignored; live engine runs 180 ticks, creates `saves/Mera Bharat/` with 4 `.slr` region files (49 dirty chunks flushed), generates `backups/Mera Bharat-<date>.zip`, and shuts down cleanly with 0 errors.

---

### Phase P7: Survival Mode, Health, Hunger & Tool Progression [COMPLETED & VERIFIED]
* **Goal**: Implement survival statistics (Lotus health, Roti Bhookh, air bubbles), fall/drown damage, drops on block break, item entities, personal 2×2 crafting, and tool tiers.
* **Dependencies**: P6.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (81/81 tests passing, full survival pipeline active, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement Player survival stats: 20 HP (10 Kamal/Lotus icons), 20 Bhookh (10 Roti flatbread icons), saturation, oxygen bubbles (Jal-Shwaas) in `PlayerVitals.java`.
  2. [x] Implement damage pipelines: fall damage (>3 blocks), drowning damage, suffocation, natural health regeneration (at Bhookh $\ge 18$), and starvation.
  3. [x] Implement Item Entities (`shilploka.entity.ItemEntity`) with floating physics, gravity, ground collision, 5-minute despawn, player magnetism (2.0 block radius), and inventory absorption.
  4. [x] Implement Personal 2×2 crafting grid (`CraftingGrid2x2.java`), `Recipe.java`, `RecipeManager.java`, and 36-slot interactive GUI in `InventoryScreen.java` (`E` key toggle, item drag/drop/split, 'Q' key stack tossing).
  5. [x] Implement 7 Tool Tiers (Lakdi/Wood, Patthar/Stone, Taamba/Copper, Kansa/Bronze, Loha/Iron, Wootz Steel, Vajra) across 4 tool classes (Kudaal, Kulhadi, Phaavda, Talwar) with harvest levels and durability wear.
  6. [x] Define authentic Indian block drop catalog, mineral yields, and food consumption (`ItemRegistry.java`).
  7. [x] Author comprehensive test suites: `PlayerVitalsTest`, `CraftingTest`, `ToolProgressionTest`, `ItemEntityTest`.
* **Verification**: 81/81 tests passing; survival vitals HUD rendered dynamically on display; dropping items settles onto voxels and responds to player magnet attraction; breaking blocks spawns item entities; 2x2 crafting resolves recipes and deducts ingredients properly; full save/load round trip preserves player inventory and vitals.

---

### Phase P8: Workstations (Kiln, Bhatti, Chaak, Loom, Chakki, Nihai) [COMPLETED & VERIFIED]
* **Goal**: Implement interactive specialized crafting workstations with custom graphical user interfaces.
* **Dependencies**: P7.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (91/91 tests passing, all 7 Indian workstations fully operational, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement 3×3 Karyashala (Workbench) with `CraftingGrid3x3.java` and expanded `RecipeManager.java` supporting 3x3 shaped and subgrid recipes.
  2. [x] Implement Pakki Bhatti (Kiln) in `KilnManager.java` for thermal baking of clay into Harappan fired bricks and quartz sand into silica glass with fuel consumption.
  3. [x] Implement Smelting Bhatti in `AlloyManager.java` for metallurgy: Copper/Iron/Gold ore smelting, ancient Kansa (Bronze) alloy, Peetal (Brass) alloy, and Wootz crucible steel synthesis.
  4. [x] Implement Chaak (Potter's Wheel) in `ChaakManager.java` for spinning wet clay into Kulhad, Handi, Matka, and Surahi pottery.
  5. [x] Implement Loom (Kargah) in `LoomManager.java` for spinning raw cotton (Kapaas) into yarn (Soot) and weaving Khadi cloth (Vastra).
  6. [x] Implement Chakki (Stone Quern Mill) in `ChakkiManager.java` for rotary stone milling of grains into flour (Atta) and spices into powder (Haldi).
  7. [x] Implement Nihai (Lohar Anvil) in `NihaiManager.java` for repairing damaged tools with matching virgin ingots (+60% durability restoration).
  8. [x] Build unified interactive 2D GUI in `WorkstationScreen.java` with responsive slot clicking, fuel/progress bars, and automatic item return on close.
  9. [x] Author comprehensive test suite `WorkstationTest.java` verifying all 7 workstations.
* **Verification**: 91/91 unit tests passing; `./scripts/verify.sh` passing; right-clicking in-world workstation blocks opens appropriate custom UI; thermal fuel timers and metallurgical alloy synthesis verified; zero Minecraft references.

---

### Phase P9: Entity System, AI Framework & Fauna [COMPLETED & VERIFIED]
* **Goal**: Implement the entity hierarchy, A* voxel pathfinding, sensory AI behavior trees, passive animals, neutral mobs, and nocturnal hostile creatures.
* **Dependencies**: P8.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (101/101 tests passing, full 3D A* AI, passive fauna, predators, and folklore hostiles operational, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement `shilploka.ai.Pathfinder` with 3D voxel A* navigation, 1-block vertical step climbing, and fall safety.
  2. [x] Implement Goal-based AI selector (`AIGoal`, `WanderGoal`, `MeleeAttackGoal`, `PanicGoal`).
  3. [x] Implement Passive Wildlife: Sacred Indian Zebu Cow (`GauEntity` - milk only via Kulhad/Matka, strictly unharmed by player attacks, zero meat drops), Water Buffalo (`BuffaloEntity`), Indian Elephant (`ElephantEntity` - 100 HP colossus), Indian Peacock (`PeacockEntity` - mor pankh feather drops and panic fleeing).
  4. [x] Implement Hostile Predators: Royal Bengal Tiger (`TigerEntity` - 40 HP high speed jungle hunter), King Cobra (`CobraEntity` - low profile venom strike).
  5. [x] Implement Folklore Night Entities: Vetaal (`VetaalEntity` - nocturnal spirit, sunlight vulnerability, drops sacred Bhasma), Wilderness Daaku (`DaakuEntity` - Talwar bandit, drops copper mudra currency).
  6. [x] Implement `EntityManager` for entity ticking, dynamic biome distance spawning, swept-AABB collision against chunks, and death loot dispersion.
  7. [x] Implement `EntityRenderer` with 3D box models, animations, and shader integration.
  8. [x] Author comprehensive unit test suite `EntitySystemTest.java`.
* **Verification**: 101/101 unit tests passing; `./scripts/verify.sh` passing; sacred Gau protected from damage and milkable with earthen pottery; predators stalk and damage player; Vetaal active at midnight; zero Minecraft references.

---

### Phase P10: Complete Biome Suite & Dynamic Weather Systems [COMPLETED & VERIFIED]
* **Goal**: Implement all 11 Indian-inspired biomes, transition blending, and weather systems (Barsaat monsoon, Aandhi dust storms, Himpaat snow).
* **Dependencies**: P9.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (106/106 tests passing, all 11 biomes of Bharat, Shat-Ritu seasons, and dynamic weather operational, 0 Minecraft terms).
* **Tasks**:
  1. [x] Expand procedural worldgen to support all 11 biomes (adding Deccan Pathar, Sahyadri Varshavan, Sundarban, Poorvottar Baans, Rann Namak, Samudra Tat, Sagar, Vindhya Jangal) with 3 climate noise fields (temperature, rainfall, continentalness).
  2. [x] Add regional voxel blocks: Regur Soil, Teak Wood/Leaves, Salt Crust (Namak), Mangrove Wood/Leaves, Baans (Bamboo), Samudra Sand with procedural texture layers (37–44).
  3. [x] Implement procedural tree generation: Sahyadri Teak trees, Sundarban stilt-rooted Mangroves, and Poorvottar Baans clusters.
  4. [x] Implement dynamic weather engine (`WeatherManager`): Barsaat (monsoon rain with fog and sunlight attenuation), Aandhi (Thar dust storms with arid haze), Himpaat (Himalayan snowfall), Shat-Ritu 6-season cycle.
  5. [x] Author comprehensive unit tests in `BiomeWeatherTest.java` and update `BiomeProviderTest.java`.
* **Verification**: 106/106 tests pass; all 11 authentic Indian biomes generate deterministically; weather dynamically transitions fog and celestial illumination; bamboo and mangrove features place correctly; zero Minecraft references.

---

### Phase P11: Historical Structures & Itihaas Granth (Codex) [COMPLETED & VERIFIED]
* **Goal**: Implement procedural era-based structure generation, loot tables, manuscript collectibles, and the interactive Itihaas Granth codex UI.
* **Dependencies**: P10.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (117/117 tests passing, all 5 historical monument generators, era loot tables, and dual-pane Itihaas Granth codex operational, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement Structure Generation subsystem in `shilploka.worldgen.structure` (`StructureType`, `HistoricalStructureManager`, `HistoricalLoot`).
  2. [x] Generate 5 era structures:
     - Harappan Great Bath & Granary (`GreatBathGenerator.java`): fired brick courtyard, bitumen-lined recessed pool, colonnaded cloister, descending stairs, relic chest.
     - Buddhist Stupa (`StupaGenerator.java`): Red sandstone Medhi plinth, hemispherical Anda dome, Harmika square balcony, triple Chhatra umbrella spire with Dharma Chakra.
     - Chola Stepwell (`StepwellGenerator.java`): subterranean geometric stepped terraces, carved granite gallery pillars, deep water reservoir pool.
     - Maratha-Rajput Hill Fort (`HillFortGenerator.java`): stone/basalt ramparts, crenellated battlements, corner round bastions, arrow slits, armory chest.
     - Ashoka Sthambha (`AshokaSthambhaGenerator.java`): monolithic carved sandstone pillar, bell lotus capital, crowning Ashoka Chakra wheel, and edicts.
  3. [x] Implement Era Loot Tables: chests containing Harappan seals (`HARAPPAN_SEAL`), Ashokan edicts (`MAURYA_EDICT`), copper plates (`TAMRAPATRA`), palm leaf manuscripts (`TALPANDULIPI`), Wootz crucible steel ingots, and Mudra currencies.
  4. [x] Add architectural blocks: `CARVED_STONE`, `ANCIENT_CHEST`, `ASHOKA_CHAKRA` with procedural textures in `TextureArray` and `GreedyMesher`.
  5. [x] Implement the Itihaas Granth UI (`CodexScreen.java`) toggled via `G` key: interactive dual-pane book UI with 5 categories (Architecture, Metallurgy, Flora, Fauna, Folklore).
  6. [x] Author comprehensive unit tests in `HistoricalStructureTest.java` and `CodexScreenTest.java`.
* **Verification**: 117/117 tests pass; `./scripts/verify.sh` passes; zero Minecraft terms; historical structures place deterministically across biomes; codex navigates with keyboard and displays authentic lore.

---

### Phase P12: Gaon (Villages), NPC Schedules & Barter Trading [COMPLETED & VERIFIED]
* **Goal**: Implement procedural Gaon generation, 9 villager professions, daily NPC schedules, barter trading UI, and nocturnal Daaku bandit raids.
* **Dependencies**: P11.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (123/123 tests passing, Gaon village generator, 9 professions, barter trade catalog, 2D BarterScreen, and Daaku raid lifecycle operational, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement Gaon procedural town layout (`GaonGenerator.java`): central Panchayat Chaupal with sacred community tree, deep village well (Kuaan) with freshwater, and artisan huts with workstations.
  2. [x] Implement 9 villager professions in `VillagerProfession.java` (Kisan, Kumhar, Lohar, Bunkar, Vaidya, Sunar, Machhuara, Shilpi, Sarpanch) with associated workstations and Devanagari titles.
  3. [x] Implement `GramwasiEntity.java` with swept-AABB collision, panic behavior, trade catalogs, and daily schedules (`WORKING`, `GATHERING_AT_CHAUPAL`, `SLEEPING`).
  4. [x] Implement Barter Trading System: `TradeOffer.java` (dual costs, stock limits, automatic currency decrement) and `BarterScreen.java` (2D interactive GUI opened via right-click on villagers).
  5. [x] Implement nocturnal Daaku raid system (`DaakuRaidManager.java`): perimeter bandit spawning, villager shelter alerts, and victory state granting village protection.
  6. [x] Author comprehensive unit tests in `BarterTradingTest.java` and `GaonGeneratorTest.java`.
* **Verification**: 123/123 tests pass; `./scripts/verify.sh` passes; zero Minecraft terms; right-clicking villagers opens barter screen; trading automatically deducts mudras/kauri and dispenses goods; Daaku raids spawn and track victory cleanly.

---

### Phase P13: Kaushal Upgrades, Vaidya Potions, Farming & Mounts [COMPLETED & VERIFIED]
* **Goal**: Implement the Shilp Mandap upgrade altar, herbalism brewing, advanced farming, and animal riding (Horse, Elephant).
* **Dependencies**: P12.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (131/131 tests passing, full Vedic upgrades, Ayurvedic brewing, farming and mounts active, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement Shilp Mandap workstation (`BlockIds.SHILP_MANDAP = 47`, `KaushalManager.java`, `KaushalScreen.java`): consume Bhasma/Mudra/Gems catalysts to bestow Kaushal enhancements (Tez Khanan, Majboot Shilp, Tej Dhaar, Jal Shwaas, Krishi Vridhhi).
  2. [x] Implement Vaidya herbalism (`OushadhManager.java`): brew authentic Ayurvedic medicines (Haldi Doodh, Tulsi Kadha, Neem Lep, Shilajit Rasayana).
  3. [x] Implement complete indigenous farming cycle (`CropManager.java`): tilled soil (`TILLED_SOIL = 48`), flooded paddy rice (`DHAN_CROP = 49`), riverbank sugarcane (`GANNA_CROP = 50`), cotton (`KAPAAS_CROP = 51`), hydration checks, and `KRISHI_VRIDDHI` double-yield harvest.
  4. [x] Implement Rideable Mounts (`RideableMount.java`, `MarwariHorseEntity.java`, `ElephantEntity.java`): saddling with Kaathi (`KAATHI`), mounting/dismounting, and player control.
  5. [x] Author comprehensive unit tests in `KaushalAyurvedaTest.java`.
* **Verification**: 131/131 tests pass; `./scripts/verify.sh` passes; zero Minecraft terms; right-clicking Shilp Mandap opens 2D upgrade altar; tools receive Kaushal blessings; Phaavda tills alluvial soil; Dhan/Ganna/Kapaas crops plant and yield harvest; Marwari Horse and Elephant saddle and mount cleanly.

---

### Phase P14: Dimensions (Paataal & Akash Dweep) & Boss Battle [COMPLETED & VERIFIED]
* **Goal**: Implement the volcanic Paataal dimension, obsidian-glass portal ignition, Andhakaar Naag boss encounter, and the celestial Akash Dweep realm.
* **Dependencies**: P13.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (6/6 tests passing in `DimensionBossTest.java`, full Paataal cavern generation, boss AI phases, Vajra Core drops, Akash Dweep celestial floating isles, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement dimensional travel pipeline and portal activation (`DimensionType.java`, `DimensionManager.java`).
  2. [x] Generate Paataal dimension (`PaataalGenerator.java`): subterranean 3D caverns, Deccan Basalt boundaries, lava oceans at Y<=32, agni rock, central boss arena at chunk (0,0).
  3. [x] Implement Guardian Boss: **Andhakaar Naag** (`AndhakaarNaagEntity.java`): 300 HP multi-phase boss, enrage velocity surge at <= 50% HP, Paataal minion serpent summons.
  4. [x] Boss Defeat Reward: Drops the **Vajra Core**; unlocks crafting of legendary Vajra tools and Vajra Ingot synthesis in `RecipeManager.java`.
  5. [x] Implement Akash Dweep portal and generation (`AkashDweepGenerator.java`): floating celestial islands of Akash Marble and glowing Akash Crystals suspended in open skies, celestial return portal.
  6. [x] Author comprehensive unit tests in `DimensionBossTest.java`.
* **Verification**: 6/6 tests pass; dimensional portal transports player between Prithvi, Paataal, and Akash Dweep with 3-second cooldown; boss enrages below 50% health; boss drops Vajra Core; Vajra recipes resolve cleanly; 0 Minecraft terms.

---

### Phase P15: Spatial Audio, Biome Ambience & Music System [COMPLETED & VERIFIED]
* **Goal**: Implement 3D spatial sound effects using OpenAL Soft, dynamic biome environmental soundscapes, and procedural/original music management.
* **Dependencies**: P14.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (4/4 tests passing in `SoundEngineTest.java`, procedural 16-bit PCM waveform synthesis, OpenAL 3D listener, dynamic Indian classical music, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement `shilploka.audio.SoundEvent` defining procedural acoustic synthesis parameters (frequency, duration, noise factor, default volume) for footsteps, block break/place, creature cries, and combat.
  2. [x] Implement `shilploka.audio.SoundCategory` for 8 volume mixing buses with English and Devanagari Hindi descriptions.
  3. [x] Implement `shilploka.audio.SoundEngine` supporting OpenAL 3D spatial attenuation, distance rolloff, category volumes, procedural PCM generation, and resilient silent fallback when headless or audio hardware is absent.
  4. [x] Implement `shilploka.audio.MusicManager` coordinating contextual Indian classical ragas and ambient soundscapes based on active biome, dimension, day/night cycle, and boss encounters.
  5. [x] Wire sound events into `Game.java` (block break, block place, listener orientation, music ticking, and clean OpenAL resource destruction).
  6. [x] Author comprehensive unit tests in `SoundEngineTest.java`.
* **Verification**: 4/4 tests pass; SoundEngine initializes safely in headless mode; category volume clamping [0.0, 1.0] verified; block break and place trigger 3D audio; music transitions across biomes and dimensions cleanly.

---

### Phase P16: UI Polish, Full Hindi Localization & Accessibility [COMPLETED & VERIFIED]
* **Goal**: Complete bilingual localization in Devanagari Hindi, configure full accessibility features, and implement optional cultural festivals.
* **Dependencies**: P15.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (4/4 tests passing in `LocalizationSettingsTest.java`, complete bilingual JSON catalogs in `assets/lang/` and classpath, interactive `SettingsScreen`, and Indian cultural festivals, 0 Minecraft terms).
* **Tasks**:
  1. [x] Create complete bilingual localization catalogs: `assets/lang/en.json` (English) and `assets/lang/hi.json` (Devanagari Hindi) with 100% of game titles, menus, HUD vitals, dimensions, biomes, workstations, and festivals.
  2. [x] Implement `shilploka.ui.Localization` supporting runtime language toggles, parameterized formatting, and reliable English fallback.
  3. [x] Implement full interactive `SettingsScreen.java`: render distance (2-16), FOV (60-110), volume sliders, language toggle, GUI scale (1x-4x), directional subtitles, reduced motion, and cultural festivals.
  4. [x] Implement Indian Cultural Festivals (`FestivalType.java`, `FestivalManager.java`): Diwali (golden night illumination bonus), Holi (vibrant colors), Makar Sankranti (kites), and Harvest (Pongal/Baisakhi/Bihu 1.5x crop growth speed).
  5. [x] Wire Settings Screen into `Game.java` accessible via `O` key shortcut or Escape menu, with live audio/video parameter synchronization.
  6. [x] Author comprehensive unit tests in `LocalizationSettingsTest.java`.
* **Verification**: 4/4 tests pass; switching language immediately updates UI strings into authentic Devanagari Hindi; settings screen navigates and clamps parameters accurately; festival bonuses compute correctly.

---

### Phase P17: Performance Profiling, Memory Optimization & Bug Bash [COMPLETED & VERIFIED]
* **Goal**: Profile engine performance, enforce <2 GB heap budget, optimize chunk streaming, and resolve edge cases.
* **Dependencies**: P16.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (2/2 tests passing in `PerformanceStressTest.java`, rapid chunk generation verified below 350 MB heap usage, high-velocity chunk streaming bounded, 0 Minecraft terms).
* **Tasks**:
  1. [x] Profile chunk generation and memory allocation under rapid 10x10 chunk grid synthesis (100 full chunk columns = 2,000 16^3 sections).
  2. [x] Verify heap usage during rapid chunk generation remains strictly below 350 MB, well within the 2.0 GB JVM budget.
  3. [x] Stress test high-velocity player streaming across 1,000 blocks at 50 m/s without memory leaks or unbounded chunk retention.
  4. [x] Fix edge cases: null-safe collision checks in `LivingEntity.java` for headless and test environments, hurtTime invulnerability timer resets.
  5. [x] Author comprehensive unit tests in `PerformanceStressTest.java`.
* **Verification**: 2/2 tests pass; memory delta across 100 generated chunk columns stays well below budget; loaded chunk count during rapid streaming stays bounded below 150 chunks; zero crashes or memory overflows.

---

### Phase P18: Packaging, Distribution & Itch.io Release [COMPLETED & VERIFIED]
* **Goal**: Configure packaging tasks, produce release archives in `dist/`, verify branding and zero-Minecraft audits, and finalize documentation.
* **Dependencies**: P17.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (`./gradlew packageAll` generates portable .zip and .tar distributions in `dist/`, `./scripts/verify.sh` passes 100% of checks with 0 Minecraft terms, 0 errors).
* **Tasks**:
  1. [x] Configure Gradle packaging task `packageAll` producing release distributions in `dist/` (`java poj-0.1.0.zip` and `java poj-0.1.0.tar`).
  2. [x] Verify KP orbit logo mark branding assets in `assets/branding/logo-mark.svg`.
  3. [x] Verify full documentation suite integrity: `docs/PRD.md`, `docs/TRD.md`, `docs/UI_UX_BRIEF.md`, `docs/IMPLEMENTATION_PLAN.md`, `docs/DATA_MODELS.md`, `docs/CODING_CONVENTIONS.md`, `docs/TESTING_STRATEGY.md`, `docs/ASSET_LICENSES.md`, `docs/HISTORY_SOURCES.md`, `docs/OPEN_QUESTIONS.md`.
  4. [x] Execute repository-wide automated zero-Minecraft grep audit (0 matches found).
  5. [x] Run full automated test suite (all 147 unit tests across 33 test classes pass 100%).
* **Verification**: `./scripts/verify.sh` passes 100% across all 3 steps (Gradle build & 147 tests, zero-Minecraft grep audit, documentation integrity audit); `./gradlew packageAll` packages complete distribution archives into `dist/`.

---

### Phase P19: Complete Voxel Physics & Keyboard Controls [COMPLETED & VERIFIED]
* **Goal**: Deliver a comprehensive voxel physics engine (fluids, buoyancy, climbing, falling blocks, continuous swept collisions) and verify keyboard control pipeline with full user handover.
* **Dependencies**: P18.
* **Status**: COMPLETED & EMPIRICALLY VERIFIED (154/154 tests passing across 34 test classes, `./scripts/verify.sh` passes 100%, 0 Minecraft terms).
* **Tasks**:
  1. [x] Implement fluid swimming & buoyancy physics in `Player.java` (viscous water drag `Math.pow(0.80, dt * 20.0)`, upward swimming impulse with Space/Jump, downward diving with Shift/Sneak, fall damage cushioning).
  2. [x] Implement vertical climbing physics in `Player.java` for Bamboo culms (`BlockIds.BAANS`), supporting ascending with Space/W, clinging in place with Shift/Sneak, and fall damage cancellation.
  3. [x] Implement 3D item entity fluid buoyancy in `ItemEntity.java`, enabling floating items with viscous fluid damping and support block loss detection.
  4. [x] Implement living entity fluid buoyancy and swept-AABB obstacle clipping in `LivingEntity.java`, preventing creatures from sinking and fixing collision clip directionality.
  5. [x] Verify granular sediment gravity (`checkAndApplyFalling`) and water-magma thermodynamic reactions in `FluidSimulator.java`.
  6. [x] Author comprehensive unit tests in `PlayerPhysicsTest.java` and `EntityPhysicsTest.java` covering swimming, climbing, ledge-fall prevention, item buoyancy, and living creature buoyancy.
  7. [x] Verify GLFW keyboard input handling pipeline in `Game.java`, screen modality dispatching, and provide complete user keyboard reference for direct control.
* **Verification**: All 154 unit tests pass cleanly; `./scripts/verify.sh` passes 100% with zero Minecraft terms and full documentation integrity.