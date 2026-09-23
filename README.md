# ShilpLoka (शिल्पलोक) — Java Edition

**A First-Person Voxel Sandbox Celebrating the History, Geography, Crafts & Ecology of Bharat**

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-orange.svg)]()
[![LWJGL 3](https://img.shields.io/badge/Engine-LWJGL%203.4.2%20%7C%20OpenGL%203.3%20Core-blue.svg)]()
[![Originality](https://img.shields.io/badge/Originality-100%25%20Zero--Minecraft-success.svg)]()

> **Created by Kanak Prabhakar**  
> *Proprietary KP Orbit Logo Mark: `assets/branding/logo-mark.svg`*

---

## 1. Overview & Vision

**ShilpLoka (शिल्पलोक — "World of Craft")** is an original, production-grade first-person 3D voxel sandbox game built entirely from scratch in **Java 21 LTS** using **LWJGL 3** and **OpenGL 3.3 Core**. 

Drawing inspiration from the civilizational heritage, architecture, traditional crafts, folklore, and diverse biomes of the Indian subcontinent (Bharat), ShilpLoka provides an end-to-end sandbox experience featuring survival, crafting progression, village trade, historical monument discovery, multi-dimensional ascension, and dynamic Indian classical music.

### Zero-Asset & Zero-Minecraft Policy
ShilpLoka is a 100% original creative work:
* **Zero Minecraft Code, Terms, or Assets**: Strictly verified via automated CI audits (`./scripts/verify.sh`).
* **Cultural Grounding**: Sacred fauna (Cows / *Gau*) are protected and milkable with earthen pottery; temples and stupas are indestructible heritage sanctuaries; all lore is neutrally derived from the Archaeological Survey of India (ASI) and NCERT sources.

---

## 2. Core Features (Phases P0 to P18)

1. **High-Performance Voxel Engine (P1–P5)**:
   - Palette-compressed 16³ chunk section storage with BitStorage (4-bit & 8-bit packing).
   - High-throughput Greedy Meshing (99% face count reduction on flat slabs).
   - Frustum Culling with 6 Gribb-Hartmann extraction planes.
   - Dual cellular automata light engine (15-level Skylight & Blocklight BFS ring queues).
   - Dynamic 24,000-tick day/night celestial dome with 8 lunar phases (Paksha).
   - Cellular fluid mechanics: Water flows, cascades down gravity shafts, and quenches volcanic Lava into Deccan Basalt.

2. **11 Authentic Biomes of Bharat (P3, P10)**:
   - *Ganga Maidan* (Alluvial plains, Sal forests)
   - *Thar Marusthal* (Desert dunes, Khejri trees, sandstone)
   - *Himalaya Shikhar* (Snowy crags, Deodar cedars)
   - *Deccan Pathar* (Columnar basalt traps, regur soil)
   - *Sahyadri Varshavan* (Tropical rainforests, teak trees)
   - *Sundarban* (Tidal mangrove delta, stilt roots)
   - *Poorvottar Baans* (Northeastern bamboo hills)
   - *Rann Namak* (White salt flats)
   - *Samudra Tat* & *Sagar* (Coastal dunes, deep oceanic trenches)
   - *Terai Van* (Lush marshland and tall grasses)

3. **Indo-Centric Workstations & Metallurgy (P7, P8, P13)**:
   - **Karyashala**: 3×3 artisan crafting workbench.
   - **Pakki Bhatti**: High-temperature kiln for Harappan fired bricks and silica glass.
   - **Smelting Bhatti**: Metallurgical forge for copper, iron, gold, ancient Kansa (bronze) alloy, and Wootz crucible steel.
   - **Kumhar Chaak**: Potter's wheel spinning clay into terracotta Kulhad, Handi, Matka, and Surahi.
   - **Kargah (Loom)**: Handloom weaving raw cotton into Khadi cloth.
   - **Atta Chakki**: Stone quern mill grinding grains and spices into flour and Haldi.
   - **Lohar Nihai**: Blacksmith anvil repairing damaged tools with virgin ingots.
   - **Shilp Mandap**: Vedic upgrade altar bestowing Kaushal craft enhancements.

4. **Living Fauna, Hostiles & Village Life (P9, P12)**:
   - **Fauna**: Sacred Gau (cow), Water Buffalo, Colossal Indian Elephant, Peacock, Royal Bengal Tiger, King Cobra.
   - **Folklore Hostiles**: Nocturnal Vetaal spirits and wilderness Daaku bandits.
   - **Gaon Settlements**: Procedurally generated villages with Panchayat Chaupal, deep freshwater well, and 9 villager artisan professions.
   - **Barter Trade**: Interactive 2D trade GUI with currency hierarchy (Kauri shells and Mudras).
   - **Raid Defense**: Nocturnal Daaku bandit raids with villager alarm warnings.

5. **Historical Monuments & Itihaas Granth (P11)**:
   - Procedural era monuments: Harappan Great Bath, Buddhist Stupa, Chola Stepwell, Maratha-Rajput Hill Fort, Ashoka Sthambha.
   - Interactive dual-pane illustrated codex (*Itihaas Granth*) toggled via `G` key.

6. **Cosmic Dimensions & Guardian Boss (P14)**:
   - **Paataal**: Volcanic underworld of subterranean lava oceans, burning Agni rock, and basalt columns.
   - **Guardian Boss (Andhakaar Naag)**: 300 HP multi-stage cosmic serpent guarding Paataal. Enrages below 50% health, doubles velocity, summons minion serpents, and drops the divine **Vajra Core**.
   - **Akash Dweep**: Serene floating celestial islands of ivory Akash Marble and glowing Akash Crystals.
   - **Vajra Tools**: Indestructible endgame tools and weapons forged with Wootz steel and the Vajra Core.

7. **Spatial Audio & Indian Classical Music (P15)**:
   - 3D spatial audio powered by OpenAL Soft with distance rolloff and headless fallback resilience.
   - Procedural 16-bit PCM waveform synthesis for footsteps, block breaking, and creature cries.
   - Dynamic ambient music based on Indian classical ragas (Bhairav, Sarang, Yaman, Malkauns).

8. **Bilingual Localization & Options (P16)**:
   - 100% bilingual support: Switch seamlessly between English and authentic Devanagari Hindi (`hi.json`).
   - Settings Screen (`O` key): Render distance, FOV, audio sliders, GUI scale, directional subtitles, reduced motion.
   - Cultural Festivals: Diwali illumination bonus, Holi colors, Makar Sankranti, and seasonal harvest bonuses.

9. **Persistence & Robustness (P6, P17, P18)**:
   - Versioned `.slr` region file format (32×32 chunk containers, ZLIB compression).
   - Atomic staging writes (`.tmp` $\to$ `sync` $\to$ `ATOMIC_MOVE` with `.old` rollback backups).
   - Automated pre-session backup archives (`backups/`).
   - Memory ceiling strictly maintained under 1.5 GB.

---

## 3. Controls Quick Reference

| Action | Primary Key | Alternate |
|---|---|---|
| Move Forward / Backward | `W` / `S` | Arrow Up / Down |
| Strafe Left / Right | `A` / `D` | Arrow Left / Right |
| Jump / Ascend | `Space` | |
| Sneak / Crouch | `Left Shift` | (Ledge-fall prevention) |
| Sprint | `Left Ctrl` | Double-tap `W` |
| Break Block / Attack | `Left Mouse Button` | |
| Place Block / Interact | `Right Mouse Button` | (Opens workstations, tills soil, rides mounts) |
| Hotbar Selection | Keys `1`–`9` | Mouse Scroll Wheel |
| Inventory & 2x2 Crafting | `E` | |
| Drop Held Item | `Q` | |
| Itihaas Granth (Codex) | `G` | |
| Settings & Options | `O` | |
| Creative Flight Toggle | `F` | |
| Diagnostic Overlay | `F3` | |
| Close Menus / Pause | `Esc` | |

---

## 4. Building, Testing & Verification

Ensure **JDK 21** or later is installed.

```bash
# Build the project and run all 147 automated unit tests:
./gradlew test

# Run the comprehensive 3-step verification pipeline (tests, zero-Minecraft grep, doc integrity):
./scripts/verify.sh

# Run the game interactively:
./gradlew run

# Package portable release distributions (.zip and .tar) into dist/:
./gradlew packageAll
```

---

## 5. Architecture & Documentation Suite

Detailed architectural specifications and context documents are located in `docs/`:
* [docs/PRD.md](file:///home/kanak/Desktop/java%20poj/docs/PRD.md) — Product Requirements Document
* [docs/TRD.md](file:///home/kanak/Desktop/java%20poj/docs/TRD.md) — Technical Requirements Document
* [docs/IMPLEMENTATION_PLAN.md](file:///home/kanak/Desktop/java%20poj/docs/IMPLEMENTATION_PLAN.md) — Phase Implementation Plan (P0 through P18)
* [docs/UI_UX_BRIEF.md](file:///home/kanak/Desktop/java%20poj/docs/UI_UX_BRIEF.md) — UI/UX Design Brief
* [docs/DATA_MODELS.md](file:///home/kanak/Desktop/java%20poj/docs/DATA_MODELS.md) — Data Models & Schemas
* [docs/TESTING_STRATEGY.md](file:///home/kanak/Desktop/java%20poj/docs/TESTING_STRATEGY.md) — Quality Gates & Testing Strategy
* [docs/CODING_CONVENTIONS.md](file:///home/kanak/Desktop/java%20poj/docs/CODING_CONVENTIONS.md) — Architectural Rules & Coding Standards
* [docs/ASSET_LICENSES.md](file:///home/kanak/Desktop/java%20poj/docs/ASSET_LICENSES.md) — Asset Licenses & Origins
* [docs/HISTORY_SOURCES.md](file:///home/kanak/Desktop/java%20poj/docs/HISTORY_SOURCES.md) — Historical & Archaeological Sources (ASI / NCERT)
* [docs/OPEN_QUESTIONS.md](file:///home/kanak/Desktop/java%20poj/docs/OPEN_QUESTIONS.md) — Architecture & Design Decisions
