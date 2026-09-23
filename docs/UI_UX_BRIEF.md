# ShilpLoka (Java Edition) — UI/UX Design Brief

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Visual Direction & Design System

### 1.1 Color Palette
The aesthetic of ShilpLoka is rooted in the natural minerals, earthen crafts, hand-dyed textiles, and architectural landmarks of the Indian subcontinent. The interface is warm, elegant, inviting, and distinctly distinguished from generic voxel sandbox aesthetics.

```
TERRACOTTA     SAFFRON        SANDSTONE      PEACOCK TEAL   INDIGO         DECCAN BASALT
#E37A57        #FF7A18        #F4E3B2        #0088A8        #2C3E50        #1E1E24
Primary UI     Highlights     Panels/Cards   Accent/Water   Borders/Header Shadow/Text
```

| Token | Hex Value | Semantic Usage |
|---|---|---|
| `color-primary-terracotta` | `#E37A57` / `#C85A32` | Button backgrounds, active tab markers, primary focus accents. |
| `color-accent-saffron` | `#FF7A18` / `#E65100` | Notifications, selection halos, critical warning alerts, XP orb glow. |
| `color-surface-sandstone` | `#F4E3B2` / `#E8D5A3` | Inventory slots, dialog background card, parchment scroll surfaces. |
| `color-accent-peacock` | `#0088A8` / `#005B73` | Mana/air bubbles, water status, celestial codex entries, active links. |
| `color-deep-indigo` | `#2C3E50` / `#1A252F` | Window outlines, panel borders, typography contrast layers. |
| `color-dark-basalt` | `#1E1E24` | HUD backdrop tint, tooltip containers, darkened pause overlays. |
| `color-haldi-yellow` | `#FFB84C` | Golden coin currency, hunger roti icons, tooltip titles. |
| `color-madder-red` | `#A82424` | Health lotus icons, critical damage indicators, danger alerts. |

### 1.2 Motif, Borders & Textures
- **Jaali Latticework & Rangoli Accents**: Panel corners, dialog dividers, and frame margins feature subtle geometric jaali and rangoli filigree.
- **Parchment & Hand-Made Paper**: Tooltips, codex pages, and trading panels utilize a subtle handmade paper texture.
- **Pixel-Art Coherence**: All item sprites, slot brackets, and HUD glyphs are drawn in crisp $16 \times 16$ pixel art, scaling uniformly with screen resolution.

### 1.3 Typography System
1. **Body & UI Text**: Bundled open-licensed fonts:
   - **Noto Sans** (Latin scripts) — SIL Open Font License 1.1.
   - **Noto Sans Devanagari** (Devanagari Hindi) — SIL Open Font License 1.1.
2. **HUD & Retro Gauges**: Bundled pixel-proportional monospace font for numerical meters (hotbar quantities, FPS counters, coordinates).
3. **No Hardcoded Strings**: Every displayed label, tooltip, dialogue snippet, and button text is resolved dynamically from `assets/lang/en.json` or `assets/lang/hi.json`.

---

## 2. Information Architecture & Screen Flow

```
                      ┌──────────────────────┐
                      │    Splash Screen     │ (2 seconds, KP Logo)
                      └──────────┬───────────┘
                                 │
                                 ▼
                      ┌──────────────────────┐
                      │     Title Screen     │ ◄──────────────────────────────┐
                      └──────────┬───────────┘                                │
         ┌───────────────────────┼───────────────────────┐                    │
         ▼                       ▼                       ▼                    │
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐           │
│ Singleplayer UI │     │ Settings Screen │     │  Credits Screen │           │
└────────┬────────┘     └─────────────────┘     └─────────────────┘           │
         │ (Select / Create)                                                  │
         ▼                                                                    │
┌─────────────────┐                                                           │
│ Loading Screen  │ (Progress Bar + Rotating Itihaas Fact)                    │
└────────┬────────┘                                                           │
         │ (World Ready)                                                      │
         ▼                                                                    │
┌────────────────────────────────────────────────────────────────────────┐    │
│                           In-Game HUD                                  │    │
│  - Lotus Health        - Roti Hunger        - Compass (Uttar/Dakshin)  │    │
│  - Hotbar (1-9)        - Armor Dhaal        - Air Bubbles              │    │
└───────────────────────────────────┬────────────────────────────────────┘    │
                                    │                                         │
       ┌────────────────────────────┼────────────────────────────┐            │
       ▼                            ▼                            ▼            │
┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐     │
│ Inventory &     │        │ Workstations    │        │ Pause Menu      │─────┘
│ Crafting (E)    │        │ (Kiln/Bhatti/   │        │ (Esc)           │
│ + Recipe Book   │        │  Chaak/Loom)    │        └─────────────────┘
└─────────────────┘        └─────────────────┘
```

---

## 3. Screen Specifications & State Matrix

Every screen must explicitly support five foundational states:
1. **Loading**: Progress spinner or animated bar while computing background work.
2. **Empty**: Helpful prompt and call-to-action when no data exists.
3. **Error**: User-friendly explanation with remediation steps and error logging.
4. **Success**: Confirmation feedback or visual state transition.
5. **Disabled**: Visually dimmed (50% opacity, greyed border) accompanied by an explanatory hover tooltip detailing why the action is currently unavailable.

---

### 3.1 Splash Screen
- **Visuals**: Centered KP orbit logo mark from `assets/branding/logo-mark.svg` rasterized at 256px.
- **Text**: "ShilpLoka (शिल्पलोक)" in Devanagari and Latin script, with subtitle *"A Bharat-Inspired Voxel Sandbox by Kanak Prabhakar"*.
- **Duration**: Displays for 2.0 seconds or skips instantly upon any mouse click / key press.

### 3.2 Title Screen
- **Background**: Sweeping procedural panoramic camera view drifting over dynamic terrain (Himalaya peaks or Thar dunes).
- **Navigation Column (Centered)**:
  - **Singleplayer (Ekal-Khela)**: Primary action button.
  - **Multiplayer (Bahu-Khela)**: Disabled in v1. Tooltip: *"LAN & Online multiplayer planned for v2"*.
  - **Itihaas Granth (History Codex)**: Accesses codex overview and unlocked knowledge.
  - **Settings (Vivyavastha)**: Opens audio, video, controls, and accessibility options.
  - **Credits (Aabhaar)**: Creators, contributors, and open-source acknowledgments.
  - **Quit (Bahaar)**: Gracefully terminates the application.
- **Branding**: Small KP orbit logo at bottom-right corner at 70% opacity with text *"by Kanak Prabhakar"*.

### 3.3 Singleplayer World Selection Screen
- **Layout**: Scrollable vertical list of existing world save folders.
- **World Card Details**: World Name, Seed number (truncated), Game Mode badge (Survival/Creative/Kathin), Difficulty, Last Played date/time, File Size.
- **Action Buttons**: Play Selected World, Create New World, Backup World, Delete World.
- **States**:
  - *Empty State*: "No worlds discovered yet. Click 'Create New World' to begin your journey in Bharat!"
  - *Error State*: "Corrupt save file detected in folder [name]. Automatic backup generated. Click to repair."
  - *Disabled State*: 'Play' and 'Delete' buttons disabled when no world is selected.

### 3.4 Create World Screen
- **Input Fields**:
  - **World Name**: Text field (default: *"Mera ShilpLoka"*).
  - **Seed**: Text field populated with random 64-bit integer, editable by player.
  - **Game Mode Selector**: Carousel toggle between Survival (*Astitva*), Creative (*Srijan*), and Kathin (*Hardcore*).
  - **Difficulty Selector**: Carousel toggle between Shaanti, Aasaan, Saadharan, and Kathin.
  - **World Generator Type**: Saadharan (Default Bharat topography), Samatal (Flat testing surface), Dweep (Archipelago islands).
  - **Cheats / Commands**: Toggle On/Off (On by default in Creative).
- **Buttons**: Create World (Primary terracotta) and Cancel (Neutral sandstone).

### 3.5 Loading Screen
- **Visuals**: Terracotta-tinted progress bar tracking world generation steps:
  1. *Generating noise terrain heightmaps...*
  2. *Carving caves and underground aquifers...*
  3. *Distributing mineral ore veins...*
  4. *Spawning flora, forests, and villages...*
  5. *Building initial chunk render meshes...*
- **Historical Vignette**: A rotating historical card from the **Itihaas Granth** displaying an interesting historical fact (e.g., *"Did you know? Harappan master builders used standardized 1:2:4 fired bricks and natural bitumen mortar to construct the Great Bath at Mohenjo-daro around 2600 BCE."*).

---

## 4. In-Game HUD Architecture

```
[Lotus Hearts] [Armor Dhaals]        [Compass: UTTAR (N) - 034°]        [Air Bubbles]
 ♥ ♥ ♥ ♥ ♥ ♥ ♥ ♥ ♥ ♥  🛡 🛡 🛡 🛡                                       ○ ○ ○ ○ ○ ○ ○ ○ ○ ○
[Roti Hunger]
 🫓 🫓 🫓 🫓 🫓 🫓 🫓 🫓 🫓 🫓
                                            + (Crosshair)


                                  [Anubhav XP Bar: Level 14]
                             ════════════════════════════════════
                             [ 1 ][ 2 ][ 3 ][ 4 ][ 5 ][ 6 ][ 7 ][ 8 ][ 9 ]
                                            (Hotbar)
```

### 4.1 Health Meter (Lotus Icons)
- Displays 10 Indian **Lotus (*Kamal*)** icons:
  - **Full Blooming Pink Lotus**: 2 Health Points (HP).
  - **Half-Open Lotus Bud**: 1 HP.
  - **Wilted / Grey Outline**: 0 HP.
  - **Poison Status (Cobra Bite)**: Lotuses tint to dark toxic violet.

### 4.2 Hunger Meter (Roti Icons)
- Displays 10 Indian **Roti / Flatbread** icons:
  - **Golden Baked Roti**: 2 Bhookh Points.
  - **Half Roti**: 1 Bhookh Point.
  - **Empty Brass Plate Outline**: 0 Bhookh Points.
  - Depletion shakes icons; starvation at 0 Bhookh deals recurring damage.

### 4.3 Compass Ribbon (Top Center)
- A sleek horizontal ribbon indicating current heading in Hindi and English:
  - **Uttar (North / 0°)**
  - **Purab (East / 90°)**
  - **Dakshin (South / 180°)**
  - **Paschim (West / 270°)**
- Includes numeric bearing readout (e.g. `Purab 094°`).

### 4.4 Hotbar (Bottom Center)
- 9 slots displaying selected item sprites, stack counts (up to 64), and durability wear bars (green $\to$ yellow $\to$ red).
- Active slot highlighted with an ornate terracotta/gold bracket frame.

### 4.5 Subtitle & Closed Caption System
- Optional on-screen directional captions for hearing-impaired players:
  - *"Water flowing Purab"*, *"Bengal Tiger growling Dakshin"*, *"Vetaal screeching above"*.

---

## 5. Workstation, Inventory & Trading Interfaces

### 5.1 Player Inventory & Personal Crafting (`E` Key)
- **Top Left**: 2×2 personal crafting grid with result slot.
- **Top Right**: Player 3D mannequin rendering currently equipped cotton/leather/copper/iron/wootz/vajra armor pieces.
- **Bottom**: 27 storage inventory slots + 9 hotbar slots.
- **Recipe Book Icon**: Toggles expandable recipe lookup drawer with instant text search and craftable filter.

### 5.2 Karyashala (3×3 Workbench)
- Standard $3 \times 3$ crafting grid.
- Allows craftable recipe auto-fill when materials are available in player inventory.

### 5.3 Specialized Workstation UIs
1. **Kiln (Bhatti)**: Input slot (raw clay/sand/limestone) + fuel slot (koyla/wood) + flame progress gauge + output slot (fired brick/terracotta/glass).
2. **Alloy Bhatti (Smelter)**: Dual metal input slots (e.g. Copper + Tin) + high-heat fuel slot + alloy output slot (Bronze ingot).
3. **Chaak (Potter's Wheel)**: Rotating clay lump preview; player selects pottery pattern (matka, diya, jar) to shape.
4. **Loom (Kargah)**: Fiber slot (cotton/coir) + dye slot (indigo/haldi/manjistha) + output cloth slot.
5. **Chakki (Mill)**: Grain input hopper + rotating wheel handle + flour output bin.
6. **Nihai (Anvil)**: Target damaged tool slot + material repair ingot slot + name rename text field + experience cost badge.

### 5.4 Barter Trading UI (Gaon Merchants & Dhanapati)
- Merchant portrait and dialogue greeting on left.
- Two-column trade list:
  - Column 1: Items demanded by merchant (e.g., 16 Kauri Shells or 4 Wheat).
  - Arrow: Barter exchange indicator.
  - Column 2: Items provided to player (e.g., 1 Fired Brick or 1 Copper Ingot).
- Quick-trade action allows shift-clicking to exchange full stacks.

### 5.5 Shilp Mandap (Kaushal Upgrades)
- Central pedestal holds tool/weapon.
- Secondary slot accepts **Rajavarta (Lapis)** offering.
- Displays 3 selectable **Kaushal** disciplines (Tez, Majboot, Tej-Dhaar) with required Anubhav (XP) level.

---

## 6. Itihaas Granth (History Codex) Interface (`G` Key)

- **Layout**: Two-pane book/codex design reminiscent of an illuminated Indian palm-leaf manuscript (*Grantha*).
- **Left Pane**: Era and Category Filter (Sindhu-Saraswati, Maurya, Gupta, Chola, Vijayanagara, Rajput/Maratha, Indo-Islamic, Colonial). Unlocked entries are highlighted; undiscovered entries display as *"Unknown Ancient Mystery — explore ruins to reveal"*.
- **Right Pane**:
  - Era Badge and Title.
  - 3D interactive spinning voxel model of the structure or artifact.
  - Factually accurate, neutral 2–4 sentence historical narrative (toggleable between English and Devanagari Hindi).
  - Associated blocks and discovered locations.

---

## 7. Accessibility & System Configuration

1. **GUI Scale**: Auto, 1×, 2×, 3×, 4× (supports displays from 720p laptops up to 4K monitors).
2. **Field of View (FOV)**: Slider from 60° (normal) to 110° (wide).
3. **Colorblind-Safe HUD**: Toggles alternate high-contrast symbols and colors for health, hunger, and status indicators.
4. **Reduce Motion**: Disables camera view bobbing, screen shake on damage, and fast FOV warping during sprinting.
5. **Input Adaptability**:
   - Rebindable keyboard and mouse mapping.
   - Hold vs. Toggle options for Sneak and Sprint.
   - Mouse look sensitivity slider and invert-Y toggle.