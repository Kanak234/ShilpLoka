# ADR 006: Data-Driven Content Architecture via JSON Registries

* **Status**: Proposed (Awaiting Kanak approval)
* **Date**: 2026-09-23
* **Deciders**: Lead Engineer, Kanak Prabhakar

## Context & Problem Statement
Hardcoding block properties, recipes, item definitions, biomes, structures, and codex text directly into Java classes makes balancing difficult, limits moddability/extensibility, complicates bilingual localization (English and Hindi), and leads to sprawling codebases. Adding a new decorative stone or crafting recipe should not require recompiling Java code.

## Decision
1. **JSON as the Source of Truth**:
   - All blocks (`assets/data/blocks/*.json`), items (`assets/data/items/*.json`), biomes (`assets/data/biomes/*.json`), recipes (`assets/data/recipes/*.json`), structures (`assets/data/structures/*.json`), loot tables (`assets/data/loot/*.json`), entities (`assets/data/entities/*.json`), and Itihaas Granth codex entries (`assets/data/codex/*.json`) are defined strictly in JSON files.
2. **Schema Validation on Startup**:
   - Every JSON resource is validated upon engine startup using Jackson databind.
   - If a JSON file contains missing fields or syntactic errors, the engine reports the exact file path and line number, preventing silent runtime failures.
3. **Identifier System**:
   - All assets and registry items use namespaced IDs: `shilploka:<resource_name>` (e.g., `shilploka:fired_brick`, `shilploka:wootz_talwar`).
4. **Behavior Binding via Extensible Handler Classes**:
   - Standard blocks (solids, stairs, slabs, pillars) require zero Java code.
   - Interactive blocks (e.g. Kiln, Bhatti, Chaak, Loom, Sanduk) bind to specialized behavior interfaces via a `BlockBehavior` registry mapped by ID.
5. **Localization Separation**:
   - All human-visible display names and descriptions reference translation keys mapped in `assets/lang/en.json` and `assets/lang/hi.json`.

### Consequences
- Rapid iteration: New cultural blocks (Athangudi tiles, jaali panels, red sandstone) can be added or tuned in seconds.
- Clean foundation for future data packs and mod APIs.
