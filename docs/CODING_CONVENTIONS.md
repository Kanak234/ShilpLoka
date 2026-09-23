# ShilpLoka (Java Edition) — Coding Conventions & Engineering Standards

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Core Engineering Principles

1. **Pedagogical Explanatory Clarity**: Kanak Prabhakar must be able to present, defend, and explain every file in this repository to any audience. Code readability, clear architecture, and thorough documentation take precedence over cryptic cleverness.
2. **Deterministic & Robust**: Game simulation logic must be pure, testable, and completely detached from the render loop.
3. **Zero Allocation in Hot Loops**: Zero garbage collection churn during active 20 TPS simulation and 60 FPS rendering.
4. **No God Classes**: Strict Single Responsibility Principle. Class files should generally remain **under ~400 lines of code**.

---

## 2. Mandatory Inline Commenting Standard (§0.5)

Every Java class, method, and non-trivial logical block must include comments answering the **Four Essential Questions**:
1. **WHY**: The design rationale and motivation behind writing this code.
2. **WHAT**: An exact summary of what the code computes, mutates, or manages.
3. **USED FOR**: Where this component fits into the broader game ecosystem.
4. **WHERE NEXT**: The subsequent step in the execution pipeline (which method is invoked next, or what state is updated).

*Note*: All comments are written in simple English, with authentic Hindi and cultural terms rendered in Roman script (e.g., *"gaon"*, *"karyashala"*, *"lohar"*, *"bhatti"*, *"bhookh"*).

### Example of Mandatory Method Comment:

```java
/**
 * WHY:
 * Computes greedy-meshed quads for a 16³ chunk section to minimize GPU draw calls
 * and memory bandwidth on entry-level hardware (RTX 3050 Laptop / Radeon iGPU).
 *
 * WHAT:
 * Iterates through the 3D voxel array across all 6 directional face axes, merging
 * adjacent coplanar faces that share the identical block texture layer, light level,
 * and ambient occlusion coefficients into single consolidated rectangular quads.
 *
 * USED FOR:
 * Called by ChunkWorkerPool when a dirty chunk section requires regeneration before
 * uploading geometry to the GPU vertex buffer.
 *
 * WHERE NEXT:
 * Formatted vertex attributes are packed into a direct ByteBuffer and enqueued onto
 * the RenderThread mesh upload queue for OpenGL buffer binding.
 *
 * @param section The 16³ chunk section containing palette-compressed block data.
 * @param buffer  Pre-allocated native ByteBuffer from BufferPool receiving vertex data.
 * @return Total number of consolidated quads generated for this section pass.
 */
public int meshSection(ChunkSection section, ByteBuffer buffer) {
    // 1. WHY: Iterate X-axis slices first to build vertical and horizontal run-lengths.
    // 2. WHAT: Scan each 16x16 2D plane and populate the face mask bitset.
    // 3. USED FOR: Identifying exposed boundary faces adjacent to air or transparent blocks.
    // 4. WHERE NEXT: Inner loop processes the 2D mask to find maximum contiguous rectangle bounds.
    ...
}
```

---

## 3. Java 21 Modern Language Feature Guidelines

1. **Records for Immutable Data Carriers**:
   - Use `record` for coordinate vectors, packet payloads, block states, and immutable registry descriptors:
     ```java
     public record BlockPos(int x, int y, int z) {
         public BlockPos add(int dx, int dy, int dz) {
             return new BlockPos(x + dx, y + dy, z + dz);
         }
     }
     ```
2. **Sealed Types for Exhaustive Hierarchies**:
   - Model tool tiers, entity AI states, and recipe types with `sealed interface` or `sealed class`:
     ```java
     public sealed interface ToolTier permits WoodTier, StoneTier, CopperTier, BronzeTier, IronTier, WootzTier, VajraTier {
         int harvestLevel();
         int maxDurability();
         float miningMultiplier();
     }
     ```
3. **Pattern Matching in Switch Expressions**:
   - Replace long `if-else` chains with exhaustive `switch` pattern matching:
     ```java
     float damage = switch (attacker) {
         case Player p when p.isHoldingTalwar() -> p.getTalwarDamage();
         case TigerEntity t -> t.getPounceDamage();
         case DaakuEntity d -> d.getArrowDamage();
         default -> 1.0f;
     };
     ```

---

## 4. Hot Loop Optimization & Memory Rules

To maintain the strict **< 2.0 GB heap ceiling** and eliminate GC freeze spikes:

1. **No Instantiations in `tick()` or `render()`**:
   - Never create `new Vector3f()`, `new Matrix4f()`, `new ArrayList<>()`, or perform string concatenation (`"x=" + x`) in per-frame or per-tick loops.
2. **Reusable Buffers via `BufferPool`**:
   - Always acquire and release direct native NIO buffers through `shilploka.render.gl.BufferPool`.
3. **Primitive Arrays Over Object Wrappers**:
   - Use `int[]`, `byte[]`, and `long[]` rather than `List<Integer>` or `Byte[]` for voxel storage and heightmap calculations.
4. **In-Place Vector Transformations**:
   - Use JOML destination-passing methods:
     ```java
     // CORRECT: In-place multiplication
     viewMatrix.mul(modelMatrix, destinationMatrix);
     
     // PROHIBITED in hot paths: Creates garbage
     Matrix4f result = new Matrix4f(viewMatrix).mul(modelMatrix);
     ```

---

## 5. Naming Conventions

| Component | Convention | Example |
|---|---|---|
| **Packages** | All lowercase, singular | `shilploka.world.chunk` |
| **Classes & Records** | PascalCase | `ChunkColumn`, `BlockPos` |
| **Interfaces** | PascalCase (adjective or noun) | `Renderable`, `Tickable`, `Workstation` |
| **Methods** | camelCase (action verb prefix) | `generateHeightmap()`, `applyDamage()` |
| **Fields & Variables** | camelCase (no `m_` prefix) | `worldSeed`, `lightLevel`, `currentChunk` |
| **Constants** | UPPER_SNAKE_CASE | `CHUNK_SECTION_SIZE`, `MAX_BUILD_HEIGHT` |
| **Resource Identifiers** | Namespaced snake_case | `shilploka:wootz_talwar`, `shilploka:fired_brick` |
| **Test Classes** | ClassName + `Test` | `SaveMigrationTest`, `GreedyMesherTest` |

---

## 6. Error Handling & Safety

1. **No Silent Failures**: Never write empty `catch` blocks. All caught exceptions must be logged via SLF4J (`LOGGER.error("Failed to load chunk at {}, {}: {}", x, z, e.getMessage(), e)`).
2. **Fail Fast with Clear Diagnostics**: In initialization methods (shader compilation, JSON schema loading), validate preconditions immediately. If OpenGL shaders fail to link, throw an informative `ShaderCompilationException` containing the full GPU driver error log.
3. **Crash Recovery Integrity**: If a fatal error occurs during saving, write the diagnostic report to `crash-reports/crash-<date>.txt` and ensure the existing save remains untouched on disk.