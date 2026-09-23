# ShilpLoka (Java Edition) — Technical Requirements Document (TRD)

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. System Architecture & Technology Stack

### 1.1 Core Stack Specifications

| Layer / Subsystem | Technology | Version | Rationale & Justification |
|---|---|---|---|
| **Runtime Language** | **Java (LTS)** | **Java 21** | Modern language features: Records for immutable data carriers, Sealed Types for exhaustive hierarchies, Pattern Matching for switch expressions, Virtual Threads for IO tasks. |
| **Build Automation** | **Gradle (Kotlin DSL)** | **8.5+** | Type-safe Kotlin build scripts (`build.gradle.kts`), robust dependency resolution, integration with `jlink`/`jpackage`. |
| **Windowing & Input** | **LWJGL 3 (GLFW)** | **3.4.2** | Low-latency window management, native input polling (keyboard, mouse, gamepad), high-DPI handling. |
| **Graphics API** | **OpenGL Core Profile** | **3.3 Core** | Ubiquitous hardware support across modern Linux and Windows PCs; direct access to modern shader pipelines, VAOs, VBOs, and 2D Texture Arrays. |
| **Spatial Audio API** | **LWJGL 3 (OpenAL Soft)** | **3.4.2** | Hardware-accelerated 3D positional audio, distance attenuation, Doppler shift, and environmental reverberation. |
| **Asset Decoding** | **LWJGL 3 (STB)** | **3.4.2** | High-performance C-native decoders for PNG images (`stb_image`), TrueType fonts (`stb_truetype`), and Vorbis audio (`stb_vorbis`). |
| **Mathematics Engine** | **JOML (Java OpenGL Math)** | **1.10.9** | Zero-allocation vector, matrix, and quaternion math optimized for SIMD and high-performance game loops. |
| **Logging Framework** | **SLF4J + Logback** | **2.0.x / 1.5.x** | Fast asynchronous file-based rolling logging (`logs/shilploka.log`) with friendly console formatting. |
| **Data Serialization** | **Jackson Databind** | **2.15.x** | Fast JSON parsing for data-driven block, item, biome, recipe, and structure registries with precise error line-number reporting. |
| **Compression** | **java.util.zip (Deflater)** | Built-in | Fast ZLIB compression for region chunks without third-party native dependencies. |
| **Testing Framework** | **JUnit 5 (Jupiter)** | **5.10.x** | Unit testing, world generation determinism suites, and save/load round-trip validation. |

### 1.2 Architectural Module Decomposition

The codebase resides entirely under `src/main/java/shilploka/` structured into clean, single-responsibility domain packages:

```
shilploka/
├── app/          # Bootstrap entry point, CLI arguments, JVM runtime validation, crash handling
├── core/         # Engine loop, fixed-step time coordination, window creation, input routing
├── render/       # OpenGL shader manager, 2D texture array, sky dome, particle system, GUI engine
├── world/        # Chunk column, 16³ section storage, palette compression, light engine, fluid ticking
├── worldgen/     # OpenSimplex2 procedural noise, biomes, heightmaps, cave carvers, ore veins, structures
├── block/        # Data-driven block registry, block states, properties, hardness, sound types
├── item/         # Item registry, tool tiers, weapon stats, food nutrition, inventory container logic
├── crafting/     # 2x2/3x3 recipe evaluators, kiln/bhatti smelting, chaak pottery, loom weaving
├── entity/       # Entity base classes, AABB physics, collision response, A* pathfinding, AI behaviors
├── player/       # Player entity, survival statistics (Health, Bhookh), interaction, game modes
├── village/      # Gaon generation, Panchayat square, villager schedules, trading barter logic, raids
├── dimension/    # Dimensional gateways (Bhoolok, Paataal, Akash Dweep), portal frames, dimension state
├── audio/        # OpenAL sound sources, spatial listener, sound effects, biome ambient music manager
├── ui/           # Game screen state machine, HUD renderer, widget library, Itihaas Granth codex UI
├── lang/         # Bilingual localization engine (English en.json and Devanagari Hindi hi.json)
├── command/      # In-game console parser, tab-completion, administrative game commands
└── data/         # JSON resource loaders, schema validators, syntax error reporters
```

---

## 2. Concurrency Model & Game Loop

### 2.1 Fixed-Step Simulation vs. Variable-Step Rendering

To ensure deterministic physics and rock-solid gameplay across different refresh rates, ShilpLoka decouples the simulation tick rate from the display frame rate:

$$\text{Fixed Simulation Step: } \Delta t = \frac{1}{20} \text{ s} = 50 \text{ ms}$$

```
                ┌──────────────────────────────────────────────┐
                │             Main (Render) Thread             │
                │  - GLFW Poll Events                          │
                │  - Camera & Input Interpolation              │
                │  - Greedy Mesh GPU Uploads                   │
                │  - Multi-Pass OpenGL Rendering               │
                │  - Buffer Swap (VSync / Framerate Limiter)   │
                └──────────────────────▲───────────────────────┘
                                       │ (State Snapshots & Meshes)
   ┌───────────────────────────────────┼───────────────────────────────────┐
   │                                   │                                   │
   ▼                                   ▼                                   ▼
┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
│  Simulation Thread   │    │  Worker Thread Pool  │    │     I/O Thread       │
│  (Integrated Server) │    │  (ForkJoinPool)      │    │  - Region Reads      │
│  - 20 TPS Fixed Loop │    │  - Simplex Noise Gen │    │  - Region Writes     │
│  - Physics & AABB    │    │  - Structure Stamps  │    │  - Atomic Renames    │
│  - Fluid Spreading   │    │  - Greedy Meshing    │    │  - Pre-Save Backups  │
│  - Entity AI & Path  │    │  - Light Propagation │    │                      │
└──────────────────────┘    └──────────────────────┘    └──────────────────────┘
```

1. **Main Thread (Render & Window)**:
   - Exclusively owns the GLFW window handle and the OpenGL 3.3 Core rendering context.
   - **Absolute Rule**: *Only the main thread touches OpenGL*. All vertex uploads, texture updates, and draw calls are marshaled to this thread.
   - Calculates fractional tick interpolation factor $\alpha \in [0.0, 1.0)$ to render smooth entity movements between discrete 20 TPS physics ticks:
     $$\mathbf{P}_{\text{render}} = (1 - \alpha)\mathbf{P}_{t-1} + \alpha\mathbf{P}_{t}$$
2. **Simulation Tick Thread (Integrated Server)**:
   - Executes the fixed 20 TPS world tick.
   - Processes cellular automata fluid updates (water, lava), gravity blocks (sand, gravel), crop growth cycles, mob AI navigation, and player stat depletion (Health, Bhookh).
   - Generates state snapshots pushed to thread-safe lock-free queues for the render thread.
3. **Worker Thread Pool (`ForkJoinPool`)**:
   - Asynchronously calculates OpenSimplex2 3D noise values, carves procedural caves and aquifers, stamps historical structure templates, and constructs raw vertex buffers via greedy meshing.
4. **Asynchronous I/O Thread**:
   - Manages background disk streaming for region chunk files and player records, preventing disk latency spikes from interrupting frame delivery.

---

## 3. Voxel Rendering Pipeline & Optimization

### 3.1 Section Geometry & Greedy Meshing
- Each Chunk Column ($16 \times 320 \times 16$) is partitioned into twenty **$16^3$ Section Cubes**.
- Sections containing entirely air or completely submerged in solid underground rock are culled prior to mesh generation.
- **Greedy Meshing Algorithm**:
  - Scans each section axis-by-axis across 6 face orientations ($+X, -X, +Y, -Y, +Z, -Z$).
  - Merges adjacent coplanar faces sharing identical texture layers, lighting coefficients, and ambient occlusion values into single consolidated quads.
  - **Result**: Reduces quad and vertex count by **65% to 85%**, drastically cutting vertex-buffer memory and draw-call overhead.

### 3.2 2D Texture Array (`GL_TEXTURE_2D_ARRAY`)
- Traditional voxel texture atlases experience mipmap bleeding at polygon edges when distant camera angles sample adjacent texture pixels.
- ShilpLoka loads all 16×16 block textures into layers of a single `GL_TEXTURE_2D_ARRAY` with mipmaps.
- Because texture layers are separated along the array's $W$ (slice) coordinate, **edge bleeding is mathematically impossible**.
- Fragment shaders sample textures via `texture(u_TextureArray, vec3(v_UV, v_LayerIndex))`.

### 3.3 Multi-Pass Rendering Architecture
Rendering is partitioned into three distinct passes to ensure optimal depth testing and transparency blending:
1. **Pass 1 — Opaque Geometry**:
   - Solid stone, soils, bricks, wood planks, metals.
   - `glEnable(GL_DEPTH_TEST)`, `glDepthMask(true)`, `glEnable(GL_CULL_FACE)`.
   - Rendered strictly front-to-back using a depth pre-pass heuristic.
2. **Pass 2 — Cutout Geometry**:
   - Tree foliage, crops, wild grasses, flowers, jaali latticework panels.
   - `glEnable(GL_DEPTH_TEST)`, `glDepthMask(true)`.
   - Alpha-to-coverage or explicit shader discard: `if (texColor.a < 0.5) discard;`.
3. **Pass 3 — Translucent Geometry**:
   - Water, ice, stained glass.
   - `glEnable(GL_BLEND)`, `glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)`, `glDepthMask(false)`.
   - Sorted back-to-front relative to camera distance.

### 3.4 Smooth Lighting & Ambient Occlusion (AO)
- **Light Simulation**: Propagates 4-bit Sky Light (0–15) and 4-bit Block Light (0–15) packed into a single byte.
- **Vertex AO**: Evaluates the 4 corner neighbors of each voxel face to assign an ambient occlusion coefficient (0, 1, 2, or 3) per vertex, yielding realistic corner shadowing.
- Toggleable in Video Settings: Fast (flat lighting) vs. Fancy (smooth vertex interpolation + AO).

### 3.5 Dynamic Sky & Atmospheric Shader
- Procedural atmospheric dome computed in vertex and fragment shaders using Rayleigh and Mie scattering approximations.
- Celestial objects (Sun and Moon) rendered as billboards with proper astronomical inclination and lunar phase tracking.
- Volumetric cloud layer at $Y = 192$ drifting horizontally according to wind direction vectors.

---

## 4. Memory Architecture & Performance Ceiling

### 4.1 Palette-Compressed Block Storage
Storing raw 32-bit block integers for a 320-height world would consume excessive heap:
$$16 \times 16 \times 320 \times 4 \text{ bytes} \approx 327 \text{ KB per chunk}$$
At render distance 8 ($17 \times 17 = 289$ chunks), raw block storage would exceed 94 MB just for geometry, before caching and meshing.

**ShilpLoka Palette Compression**:
- Each $16^3$ section maintains a local dynamic palette of unique block states (typically 2 to 16 unique blocks per section).
- Block indices are packed into compact bit-arrays:
  - $\le 16$ unique blocks: **4 bits per block** (2,048 bytes per section).
  - $\le 256$ unique blocks: **8 bits per block** (4,096 bytes per section).
- **Result**: Over **75% reduction** in heap usage for chunk block data.

### 4.2 Zero-Allocation Hot Loops
To prevent Garbage Collector pause spikes exceeding 5 ms:
1. **No Instantiations in Render/Tick Paths**:
   - Prohibit `new Vector3f()`, `new String()`, or `new ArrayList<>()` inside `tick()` or `render()`.
2. **BufferPool Allocation**:
   - Reusable `ByteBuffer`, `FloatBuffer`, and `IntBuffer` allocations managed through a pre-allocated `BufferPool`.
3. **JOML In-Place Math**:
   - Perform all vector and matrix transformations using destination-passing in-place methods:
     `transform.mul(projection, view);`

---

## 5. Persistence & Region Save File Format

### 5.1 Storage Hierarchy
Saves are stored in OS-standard application data locations:
- **Linux**: `~/.local/share/ShilpLoka/saves/<world_folder>/`
- **Windows**: `%APPDATA%\ShilpLoka\saves\<world_folder>\`

```
<world_folder>/
├── level.json                # Global world metadata, seed, time, gamerules
├── level.json.old            # Automatic rollback copy
├── playerdata/
│   └── player.json           # Player inventory, coordinates, health, bhookh
└── region/
    ├── r.0.0.slr             # ShilpLoka Region file (32x32 chunk columns)
    ├── r.-1.0.slr
    └── ...
```

### 5.2 Region File (`.slr`) Binary Specification
Each region file bundles a $32 \times 32$ grid of chunk columns (1,024 chunks total):

```
+-----------------------------------------------------------------------+
| Location Table (4096 bytes: 1024 entries x 4 bytes)                  |
| [3 bytes Sector Offset | 1 byte Sector Count (4KB sectors)]           |
+-----------------------------------------------------------------------+
| Timestamp Table (4096 bytes: 1024 entries x 4 bytes)                  |
| [4 bytes Epoch Seconds per chunk]                                     |
+-----------------------------------------------------------------------+
| Sector Payload Data (4096-byte aligned sectors)                       |
|   +---------------------------------------------------------------+   |
|   | Chunk 1: [4 bytes length | 1 byte compression (ZLIB=1)]       |   |
|   |          [Deflater-compressed JSON/Binary Payload]            |   |
|   +---------------------------------------------------------------+   |
|   | Padding to 4096-byte sector boundary                          |   |
+-----------------------------------------------------------------------+
```

### 5.3 Crash Resilience & Atomic Writes
1. **Periodic Autosave**: Fires every 5 minutes and upon opening the Pause Menu.
2. **Atomic Staging**:
   - Chunks and metadata are written to `.tmp` files.
   - `FileDescriptor.sync()` flushes all OS buffers to physical disk platters.
   - `Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)` guarantees that an ungraceful crash (e.g. `kill -9` or power cut) leaves either the old or new save file intact—never a corrupt, half-written file.
3. **Session Backup**: Opening a world automatically creates a zip archive `backups/<world_name>-<date>.zip` of `level.json` and modified regions.

---

## 6. Asset Management & Procedural Fallbacks

1. **Asset Licenses & Audit**:
   - Every asset file is cataloged in `docs/ASSET_LICENSES.md`.
   - Strict rule: Only project-original, CC0, or open-licensed fonts (SIL OFL 1.1) are bundled.
2. **Procedural Texture Generator (`tools/texture-gen`)**:
   - A standalone build-time generator written in Java/Python that synthesizes crisp 16×16 pixel-art textures using Perlin color modulation, border embossing, and natural pigment palettes (terracotta, sandstone, basalt, lapis, copper).
   - Guarantees the game is 100% visually functional and playable without waiting for external artists.
3. **Audio Architecture**:
   - OpenAL Soft sound engine.
   - `MusicManager` dynamically selects calm ambient tracks according to current biome and elevation.
   - If audio tracks are unavailable, the engine gracefully executes with silence without throwing exceptions.

---

## 7. Diagnostics, Robustness & Crash Handler

1. **Hardware Validation at Startup**:
   - Verifies OpenGL 3.3 Core Profile support upon GLFW window creation.
   - If unsupported (e.g. legacy GPU driver), displays an OS-native dialog (`JOptionPane` or GLFW modal) explaining GPU requirements before graceful exit.
2. **Global Unhandled Exception Handler**:
   - Captures any unhandled exceptions on main, simulation, or worker threads.
   - Generates a timestamped diagnostic crash dump: `crash-reports/crash-<timestamp>.txt`.
   - Dumps: Complete stack trace, active thread states, OS platform, GPU vendor and renderer string, OpenGL version, loaded chunk count, player coordinates, and JVM memory stats.
3. **JSON Load-Time Validation**:
   - All JSON files loaded through `Jackson` report exact filename and line numbers upon syntax error, failing early with explicit log messages rather than cryptic runtime `NullPointerExceptions`.

---

## 8. Build, Testing & Release Packaging

1. **Build Automation**:
   - `./gradlew test`: Executes JUnit 5 pure-logic suites.
   - `./gradlew check`: Executes Checkstyle coding convention linting.
   - `./gradlew run`: Launches desktop game directly with development JVM parameters (`-Xmx2g`).
2. **Distribution Packaging**:
   - `./gradlew packageAll`: Uses `jlink` to generate a trimmed, stripped Java 21 runtime, and `jpackage` to produce:
     - **Linux**: Native `.deb` package and standalone portable `.tar.gz`.
     - **Windows**: Native `.msi` installer and standalone portable `.zip`.
   - End users install and launch ShilpLoka in one click with zero Java prerequisite.