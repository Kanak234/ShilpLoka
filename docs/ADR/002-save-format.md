# ADR 002: Versioned Region Save Format & Atomic Persistence

* **Status**: Proposed (Awaiting Kanak approval)
* **Date**: 2026-09-23
* **Deciders**: Lead Engineer, Kanak Prabhakar

## Context & Problem Statement
Voxel worlds modify thousands of blocks dynamically. If saving is unoptimized or interrupted by power failure/crash (e.g. `kill -9`), worlds can suffer catastrophic corruption. We need a robust, compact, versioned file format that allows continuous autosaving every 5 minutes and upon world exit without stutter or data loss.

## Decision Drivers
1. **Zero Data Loss on Crash**: Abrupt process termination must never leave a corrupted world file.
2. **Compact Disk Usage**: Worlds must store modified chunks efficiently using standard Deflater compression (`java.util.zip`) without external native library dependencies.
3. **Chunk Grouping (Regions)**: Individual files per chunk cause excessive OS inode usage. Chunks must be grouped into multi-chunk region containers.
4. **Format Versioning & Migrations**: Format changes in future updates must be seamlessly migratable via backward-compatibility hooks.

## Decision Outcome
We adopt a **Versioned Region-Based Architecture**:
1. **Directory Structure**:
   - Linux: `~/.local/share/ShilpLoka/saves/<world_name>/`
   - Windows: `%APPDATA%\ShilpLoka\saves\<world_name>\`
   - Files: `level.json`, `region/r.<rx>.<rz>.slr`, `playerdata/<uuid>.json`
2. **`level.json`**:
   - Stores metadata: `saveVersion`, `worldName`, `seed`, `gameMode`, `difficulty`, `timeOfDay`, `dayCount`, `spawnX/Y/Z`, `gameRules`.
3. **Region Container (`.slr` — ShilpLoka Region)**:
   - Each region spans **32×32 chunks** (1,024 chunk columns).
   - **Header**: 4 KB offset table (1,024 entries × 4 bytes: 3 bytes sector offset + 1 byte sector count) + 4 KB timestamp table.
   - **Payload**: Chunk payloads compressed with standard `java.util.zip.Deflater` (ZLIB).
4. **Atomic Rename Write**:
   - Modified regions and `level.json` are written to temporary staging files (e.g., `level.json.tmp`) and flushed to disk (`FileDescriptor.sync()`).
   - The OS atomic rename operation (`Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`) commits the changes. If interrupted, the previous valid save remains intact.
5. **Pre-Session Backup**:
   - Whenever an existing world is loaded, a compressed archive `backups/<world_name>-<timestamp>.zip` of `level.json` and altered regions is automatically generated.

### Consequences
- Resilient to unexpected crashes and power failures.
- Zero native compression dependencies.
- Clear migration pipeline via `SaveMigrationManager`.
