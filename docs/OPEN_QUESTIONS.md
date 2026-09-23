# ShilpLoka (Java Edition) — Open Questions & Decision Log

This document tracks all open questions, architectural choices, and design ambiguities for ShilpLoka (Java Edition). In accordance with the Project Context First methodology, any proposed decision not yet explicitly approved is marked **PROPOSED — needs approval**.

---

## 1. Primary Open Questions (from Master Brief §14)

### Q1: Original ShilpLoka Feature & Lore Continuity
* **Status**: AWAITING KANAK APPROVAL
* **Context**: We inspected the original ShilpLoka codebase at `/home/kanak/repos_workdir/ShilpLoka` (Three.js/Vite implementation). It features:
  - **Dhanapati the Merchant**: Barter-based trading system.
  - **Heritage Blocks**: Ashoka Sthambha (indestructible pillar) and Great Bath masonry.
  - **Specific Materials**: Bitumen mortar, Harappan baked bricks, alluvial soil, yellow/red sandstone, Deccan basalt, banyan and peepal logs.
  - **Vedic Crafting Altar**: Early workstation prototype.
* **Proposal**: 
  - Retain Dhanapati as the primary wandering merchant / chief gaon trader.
  - Retain Ashoka Sthambha and Great Bath masonry as indestructible heritage blocks in Sindhu-Saraswati and Maurya era structures.
  - Incorporate bitumen mortar as a Harappan waterproof building material (used in Great Bath generation).
* **Question for Kanak**: Do you approve carrying these specific blocks, NPCs, and mechanics over into the Java Edition registries?

---

### Q2: Core Technology Stack Confirmation
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §7.1 specifies Java 21 (LTS) + LWJGL 3 (GLFW, OpenGL 3.3 Core, OpenAL, STB) + JOML + Jackson + SLF4J/Logback, with no third-party game engine (no libGDX or jMonkeyEngine).
* **Proposal**: Approved technical direction. We implement a custom voxel engine providing fine-grained control over chunk section storage, palette compression, greedy meshing, and spatial light propagation. Documented in [ADR 001](file:///home/kanak/Desktop/java%20poj/docs/ADR/001-stack-selection.md).
* **Question for Kanak**: Confirm adherence to pure LWJGL 3 + Java 21 custom engine architecture.

---

### Q3: Art Pipeline & Texture Workflow
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §7.5 / §10.5 requires a procedural pixel-texture generator in `tools/texture-gen` so the game is 100% playable before hand-crafted pixel art exists. Later, handmade 16×16 art can replace procedural textures using identical file names.
* **Proposal**: 
  - Phase P0/P1 will build a Java/Python tool producing 16×16 PNG textures with palette-constrained noise, border shading, and Indian architectural color schemes (terracotta, sandstone, marble, sal wood, lapis).
  - All procedural textures will be registered as CC0 in `docs/ASSET_LICENSES.md`.
* **Question for Kanak**: Who will provide final handcrafted 16×16 pixel art textures later, and will you supply any initial art sets, or should the procedural tool cover the entire registry for now?

---

### Q4: Audio & Music Source
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §7.5 specifies calm ambient tracks with an Indian classical/folk instrument feel (bansuri, sitar, tabla, santoor). All audio must be CC0 or original.
* **Proposal**:
  - Implement full OpenAL spatial sound engine and `MusicManager`.
  - For launch/P0, bundle CC0 Indian classical/ambient recordings or maintain silence fallback if none are approved.
* **Question for Kanak**: Do you have existing original audio tracks from the itch.io release to include, or should we curate vetted CC0 ambient tracks under `docs/ASSET_LICENSES.md`?

---

### Q5: Project & Source Code License
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Need to determine the licensing model for the Java codebase:
  - Option A: Proprietary (All rights reserved by Kanak Prabhakar) — standard for commercial itch.io releases.
  - Option B: MIT License — open-source, permissive.
  - Option C: GPLv3 — open-source copyleft.
* **Proposal**: Proprietary for the core game code and assets, with MIT for open-source utility tools (like procedural texture generator). Documented in [ADR 003](file:///home/kanak/Desktop/java%20poj/docs/ADR/003-license.md).
* **Question for Kanak**: Which license model do you choose for ShilpLoka (Java Edition)?

---

### Q6: itch.io Publishing Strategy
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Kanak currently has https://kanak234.itch.io/shilploka (hosting the original Three.js web release).
* **Proposal**:
  - Release the Java Edition as "ShilpLoka 2.0 (Java Desktop Edition)" on the existing itch.io project page as downloadable packages (.deb, .tar.gz, .zip, .msi), while archiving/keeping the web version playable or redirecting.
* **Question for Kanak**: Should the Java Edition be an update/add-on release on the existing page, or a new distinct page?

---

### Q7: Mob Names, Folklore Creatures & Boss Design Approval
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §2.7 defines original folklore creatures (never deities, never copied from Minecraft):
  - Passive: Cow (milk only), water buffalo, goat, sheep, camel, elephant, horse, peacock, parrot, monkey, yak, deer, river dolphin, fish, flamingo, hornbill, red panda.
  - Neutral: Macaque troop, bees, wild boar, bear.
  - Hostile animals: Tiger, leopard, cobra, crocodile, wolf pack.
  - Folklore hostiles: Vetaal (drops from banyans), Pishach (cave stalker), Chhaya (zero-light hunger drainer), Daaku (ravine bandits), Mayavi Machhar (swamp swarms).
  - Boss: **Andhakaar Naag** (giant shadow serpent in Paataal dimension).
* **Proposal**: Adopt this exact list with Indian cultural sensitivity rules (deities are never mobs or enemies; cows are sacred/milk-only; religious structures are peaceful sanctuaries).
* **Question for Kanak**: Do you approve this complete mob and boss roster?

---

### Q8: UI Localization & Language Strategy
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §5 requires English and Hindi (Devanagari) at launch via `assets/lang/en.json` and `assets/lang/hi.json`. No hardcoded strings. Brief §0.5 requires all code comments and developer docs to use simple English with Hindi terms in Roman script (e.g., "gaon", "lohar").
* **Proposal**: 
  - All in-game UI rendered with Noto Sans Devanagari / Noto Sans Latin.
  - Keys structured hierarchically (e.g., `ui.screen.create_world.title`, `block.shilploka.red_sandstone.name`).
  - Codebase comments stay strictly in English / Roman script.
* **Question for Kanak**: Confirm this dual-script strategy.

---

### Q9: Kanak's KP Orbit Logo Asset Confirmation
* **Status**: AWAITING KANAK APPROVAL
* **Context**: Brief §6 states that Kanak's KP orbit logo is supplied as `assets/branding/logo-mark.svg`. During workspace inspection, we located this exact SVG file at `/home/kanak/Desktop/New Folder/coodinfkanakkyt/brand/logo-mark.svg` (containing the `<title>Kanak Prabhakar logo mark</title>` and the gradient orbit paths).
* **Proposal**: Copy this exact file to `/home/kanak/Desktop/java poj/assets/branding/logo-mark.svg` and register it in `docs/ASSET_LICENSES.md` as Kanak's proprietary branding.
* **Question for Kanak**: Do you authorize copying this file to `assets/branding/logo-mark.svg`?

---

## 2. Decision Log

| Date | ID | Topic | Decision | Status |
|---|---|---|---|---|
| 2026-09-23 | DEC-001 | Game Stack | Java 21 + LWJGL 3 + JOML + Custom Engine | PROPOSED — needs approval |
| 2026-09-23 | DEC-002 | Save Persistence | Versioned Region binary + atomic rename | PROPOSED — needs approval |
| 2026-09-23 | DEC-003 | Licensing | Proprietary game, CC0 procedural assets | PROPOSED — needs approval |
| 2026-09-23 | DEC-004 | Meshing & Texture | Greedy meshing + GL_TEXTURE_2D_ARRAY | PROPOSED — needs approval |
| 2026-09-23 | DEC-005 | Tick Loop Split | Integrated client/server (20 TPS fixed tick) | PROPOSED — needs approval |
| 2026-09-23 | DEC-006 | Data Registries | 100% JSON-driven registry loaders | PROPOSED — needs approval |
| 2026-09-23 | DEC-007 | Git Branch | Java Edition pushed to `Kanak234/ShilpLoka` on branch `java-edition` | APPROVED |
