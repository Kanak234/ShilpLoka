# ShilpLoka (Java Edition) — Testing Strategy & Verification Plan

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Quality Assurance Philosophy

To ensure ShilpLoka achieves commercial-grade stability for release on itch.io:
1. **Separation of Concerns**: Pure simulation logic (noise generation, light propagation, recipe evaluation, inventory containers, save format serialization) is decoupled from OpenGL and tested in headless automated JUnit 5 suites.
2. **Automated Continuous Verification**: `./gradlew test` and `./scripts/verify.sh` must execute without failure before every commit and before advancing past any phase gate.
3. **No Unverified Success Claims**: Completion of any phase requires running the corresponding build and reporting empirical terminal outputs.

---

## 2. Test Pyramid & Automation Levels

```
                     ┌───────────────────────────────┐
                     │   Manual Playthrough Audit    │  Full loop (Day 1 -> Vajra)
                     └───────────────▲───────────────┘
                                     │
                     ┌───────────────┴───────────────┐
                     │   Integration / Headless UI   │  Headless GL, Round-trip
                     └───────────────▲───────────────┘
                                     │
                     ┌───────────────┴───────────────┐
                     │  Determinism & Stress Tests   │  Seed reproducibility,
                     │                               │  Crash kill-9 resilience
                     └───────────────▲───────────────┘
                                     │
      ┌──────────────────────────────┴──────────────────────────────┐
      │                  Unit Test Suites (JUnit 5)                 │
      │  - OpenSimplex2 Noise         - Palette Section Storage     │
      │  - Breadth-First Light Prop   - Greedy Meshing Math         │
      │  - JSON Schema Registries     - Recipe Matching Logic       │
      │  - Region File Binary Layout  - Zero Minecraft Grep Audit   │
      └─────────────────────────────────────────────────────────────┘
```

---

## 3. Specific Test Suites

### 3.1 World Generation Determinism Test
- **Objective**: Guarantee that identical seeds produce bit-exact identical terrain across multiple independent runs.
- **Suite**: `shilploka.worldgen.WorldGenDeterminismTest`
- **Execution**:
  ```java
  @Test
  void testWorldGenSeedDeterminism() {
      long testSeed = 9482019485710293L;
      WorldGenerator genA = new WorldGenerator(testSeed);
      WorldGenerator genB = new WorldGenerator(testSeed);
      
      for (int cx = -2; cx <= 2; cx++) {
          for (int cz = -2; cz <= 2; cz++) {
              ChunkColumn chunkA = genA.generateChunk(cx, cz);
              ChunkColumn chunkB = genB.generateChunk(cx, cz);
              assertArrayEquals(chunkA.exportBlockIds(), chunkB.exportBlockIds(),
                  "Chunk (" + cx + "," + cz + ") must match exactly between runs.");
          }
      }
  }
  ```

### 3.2 Save / Load Round-Trip Integrity Test
- **Objective**: Verify that world saves preserve every modified block, container inventory, player coordinate, and entity record.
- **Suite**: `shilploka.world.save.SaveRoundTripTest`
- **Execution**:
  1. Generate a test world in a temporary directory.
  2. Mutate 1,000 random block coordinates across 4 chunks (placing Fired Bricks, Basalt, Diyas).
  3. Populate player inventory with high-tier items (Wootz Talwar, Bronze Ingot, Roti).
  4. Execute `saveManager.saveAll()`.
  5. Completely discard the in-memory world and reload from disk.
  6. Assert all 1,000 blocks and all inventory slots match expected states.

### 3.3 Crash Resilience & `kill -9` Simulation Test
- **Objective**: Verify that an abrupt system kill during active disk writes cannot corrupt the world save.
- **Suite**: `shilploka.world.save.CrashResilienceTest`
- **Execution**:
  1. Initiate an asynchronous write operation on a Region container.
  2. Simulate thread interruption / abort prior to atomic rename completion.
  3. Verify that the previous valid `.slr` file is intact and readable.
  4. Verify that no partial `.tmp` file is recognized as valid world data.

### 3.4 Automated Grep Audit (Zero Minecraft Assets/Names)
- **Objective**: Enforce Master Brief §0 rule: zero Minecraft names, asset identifiers, mob names, or namespaces.
- **Suite**: Executed via Gradle task `./gradlew verifyGrepChecklist` and `./scripts/verify.sh`.
- **Target Patterns**:
  - `minecraft:`
  - `creeper`, `enderman`, `ender_dragon`, `ghast`, `wither`
  - `nether`, `the_end`, `redstone`, `herobrine`
- **Acceptance Criterion**: Grep must return **ZERO matches** across `src/` and `assets/`.

### 3.5 Recipe & Data Registry Validation Test
- **Objective**: Validate that all JSON files under `assets/data/` load cleanly without null pointers or missing dependencies.
- **Suite**: `shilploka.data.DataRegistryValidationTest`
- **Execution**:
  - Validates every block has a valid hardness and render pass.
  - Validates every item recipe references existing registered item IDs.
  - Validates every biome surface rule references valid registered blocks.

---

## 4. Performance & Profiling Benchmarks

1. **Memory Ceiling Audit**:
   - Run Java Flight Recorder (JFR) during rapid camera flight through 1,000 chunks:
     ```bash
     java -XX:StartFlightRecording=filename=profile.jfr,duration=60s -jar build/libs/shilploka.jar
     ```
   - Analyze heap allocation in JDK Mission Control. Heap usage must stay **strictly under 2.0 GB** at render distance 8.
2. **Frame Rate Audit**:
   - Frame time graph displayed in F3 debug overlay must record 99th percentile frame times $\le 16.6 \text{ ms}$ (sustained 60 FPS) on reference hardware (RTX 3050 Laptop).