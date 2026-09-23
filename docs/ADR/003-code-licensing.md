# ADR 003: Software & Asset Licensing Strategy

* **Status**: Proposed (Awaiting Kanak confirmation on Open Question Q5)
* **Date**: 2026-09-23
* **Deciders**: Kanak Prabhakar, Lead Engineer

## Context & Problem Statement
ShilpLoka is being developed as an original single-player voxel sandbox intended for commercial/public release on itch.io (https://kanak234.itch.io/shilploka). We must establish a clear legal licensing framework for the source code, game assets, and third-party libraries.

## Considered Licensing Options
1. **Option A: Proprietary Commercial (All Rights Reserved)**
   - Game source code and game lore/assets remain the proprietary intellectual property of Kanak Prabhakar.
   - Allows direct commercial distribution and monetization on itch.io or Steam.
2. **Option B: MIT License (Permissive Open Source)**
   - Allows public forks, educational inspection, and portfolio demonstration.
   - Permits Kanak to distribute compiled packages commercially while keeping source open.
3. **Option C: GPLv3 (Copyleft Open Source)**
   - Requires any distribution of the binaries to provide complete source code under GPL. Creates friction with commercial platforms and app store models.

## Proposal & Recommendation
- **Game Engine & Core Code**: **Option A (Proprietary)** OR **Option B (MIT)** per Kanak's preference in Open Question Q5. (Currently proposed: Proprietary core with open architectural documentation).
- **Procedural Tools (`tools/texture-gen`, `tools/codex-builder`)**: Released under **MIT / CC0** for utility reuse.
- **Third-Party Libraries**:
  - LWJGL 3: Apache License 2.0 (Permissive)
  - JOML: MIT License (Permissive)
  - SLF4J / Logback: MIT / EPL 1.0 (Permissive)
  - Jackson Databind: Apache License 2.0 (Permissive)
  - Noto Sans Fonts: SIL Open Font License 1.1 (OFL)
  - JUnit 5: Eclipse Public License 2.0 (EPL)
- **Branding Assets**:
  - The KP Orbit Logo (`assets/branding/logo-mark.svg`) is strictly proprietary and copyrighted by Kanak Prabhakar. No external redistribution or modification permitted.

## Enforcement
- An automated license audit task checks every asset against `docs/ASSET_LICENSES.md` before release packaging.
