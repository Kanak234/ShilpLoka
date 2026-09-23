# ShilpLoka (Java Edition) — Data Models & Schemas

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Data-Driven Architecture Overview

In ShilpLoka, all game content is configured via **strict, validated JSON data schemas**. 
- Adding a new block, item, recipe, biome, structure, or codex entry requires **zero Java code modifications** (except for novel interactive block entities).
- All identifiers follow namespaced lowercase formatting: `shilploka:<name>`.
- Schema parsing is handled by Jackson Databind with line-number error reporting.

---

## 2. Block Registry Specification

### 2.1 Block Schema (`assets/data/blocks/<block_name>.json`)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "shilploka:fired_brick",
  "name": "block.shilploka.fired_brick.name",
  "materialType": "stone",
  "hardness": 2.5,
  "explosionResistance": 6.0,
  "lightEmission": 0,
  "lightFilter": 15,
  "isCollidable": true,
  "isOpaque": true,
  "renderPass": "opaque",
  "requiresTool": true,
  "harvestLevel": 1,
  "preferredTool": "kudaal",
  "soundGroup": "brick",
  "textures": {
    "all": "blocks/fired_brick"
  },
  "dropItem": {
    "item": "shilploka:fired_brick",
    "minCount": 1,
    "maxCount": 1
  }
}
```

For directional or multi-faced blocks (e.g., logs, stairs, grass blocks):
```json
{
  "textures": {
    "top": "blocks/sal_log_top",
    "bottom": "blocks/sal_log_top",
    "side": "blocks/sal_log_side"
  }
}
```

### 2.2 Complete Block Catalog

| Category | Block Identifiers | Properties & Harvest Levels |
|---|---|---|
| **Stones & Minerals** | `shilploka:granite`<br>`shilploka:red_sandstone`<br>`shilploka:yellow_sandstone`<br>`shilploka:makrana_marble`<br>`shilploka:deccan_basalt`<br>`shilploka:laterite`<br>`shilploka:slate`<br>`shilploka:khondalite` | Hardness: 2.0 to 5.0.<br>Requires Pickaxe (Kudaal) Level 1+.<br>Explosion resistance up to 8.0. |
| **Soils & Sediments** | `shilploka:alluvial_soil`<br>`shilploka:black_cotton_soil`<br>`shilploka:red_soil`<br>`shilploka:desert_sand`<br>`shilploka:mud`<br>`shilploka:salt_block`<br>`shilploka:gravel` | Hardness: 0.5 to 1.0.<br>Requires Shovel (Phaavda).<br>Desert sand, gravel, and salt subject to gravity. |
| **Woods (10 Species)**<br>*(Each has log, stripped, planks, stairs, slab, door, fence, gate)* | `shilploka:sal_*`<br>`shilploka:teak_*`<br>`shilploka:sheesham_*`<br>`shilploka:neem_*`<br>`shilploka:deodar_*`<br>`shilploka:banyan_*`<br>`shilploka:peepal_*`<br>`shilploka:mango_*`<br>`shilploka:coconut_palm_*`<br>`shilploka:bamboo_*` | Hardness: 1.5 to 3.0.<br>Preferred tool: Kulhadi (Axe).<br>Flammable.<br>Planks craft into stairs, slabs, and furniture. |
| **Crafted & Architectural** | `shilploka:terracotta`<br>`shilploka:fired_brick`<br>`shilploka:lime_plaster`<br>`shilploka:jaali_panel`<br>`shilploka:chhatri_roof`<br>`shilploka:jharokha_window`<br>`shilploka:athangudi_tiles`<br>`shilploka:carved_granite_pillar`<br>`shilploka:diya`<br>`shilploka:mashaal`<br>`shilploka:brass_lantern` | Decorative and illumination blocks.<br>`diya`: Light Level 14.<br>`mashaal`: Light Level 12.<br>`brass_lantern`: Light Level 15.<br>`jaali_panel`: Cutout render pass. |
| **Ores** | `shilploka:koyla_ore` (Coal)<br>`shilploka:taamba_ore` (Copper)<br>`shilploka:ranga_ore` (Tin)<br>`shilploka:jasta_ore` (Zinc)<br>`shilploka:loha_ore` (Iron)<br>`shilploka:sona_ore` (Gold)<br>`shilploka:chaandi_ore` (Silver)<br>`shilploka:abhrak_ore` (Mica)<br>`shilploka:rajavarta_ore` (Lapis)<br>`shilploka:hira_ore` (Diamond) | Koyla, Taamba: Level 1 (Stone).<br>Loha, Ranga, Jasta: Level 2 (Bronze/Iron).<br>Sona, Rajavarta: Level 2.<br>Hira: Level 3 (Wootz Steel). |
| **Workstations & Functional** | `shilploka:karyashala`<br>`shilploka:kiln`<br>`shilploka:bhatti`<br>`shilploka:chaak`<br>`shilploka:loom`<br>`shilploka:nihai`<br>`shilploka:chakki`<br>`shilploka:sanduk`<br>`shilploka:charpai`<br>`shilploka:kuan` | Interactive block entities with custom GUI containers. |
| **Heritage Blocks** | `shilploka:ashoka_sthambha`<br>`shilploka:great_bath_masonry`<br>`shilploka:bitumen_mortar` | Indestructible cultural monument blocks found in procedural historical ruins. |

---

## 3. Item & Tool Progression Registry

### 3.1 Item Schema (`assets/data/items/<item_name>.json`)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "shilploka:wootz_talwar",
  "name": "item.shilploka.wootz_talwar.name",
  "category": "combat",
  "maxStackSize": 1,
  "icon": "items/wootz_talwar",
  "itemType": "weapon",
  "toolProperties": {
    "toolClass": "talwar",
    "tier": "wootz",
    "harvestLevel": 3,
    "miningSpeed": 9.5,
    "attackDamage": 8.0,
    "attackSpeed": 1.6,
    "maxDurability": 1800,
    "repairItem": "shilploka:wootz_ingot"
  }
}
```

### 3.2 Tool Tiers & Progression Hierarchy

$$\text{Wood} \xrightarrow{\text{Stone Pick}} \text{Stone} \xrightarrow{\text{Bronze Smelting}} \text{Bronze} \xrightarrow{\text{Iron Forge}} \text{Iron} \xrightarrow{\text{Crucible Steel}} \textbf{Wootz} \xrightarrow{\text{Paataal Boss}} \textbf{Vajra}$$

| Tier | Mining Level | Durability | Base Attack Damage | Mining Multiplier | Required Station |
|---|---|---|---|---|---|
| **Wood (Kaasht)** | 0 | 60 | +1.0 | 2.0× | Personal 2×2 |
| **Stone (Patthar)** | 1 | 132 | +2.0 | 4.0× | Karyashala (Workbench) |
| **Copper (Taamba)** | 1 | 250 | +3.0 | 5.5× | Pakki Bhatti (Kiln) |
| **Bronze (Kansa)** | 2 | 420 | +4.5 | 6.5× | Bhatti (Smelter: Cu+Sn) |
| **Iron (Loha)** | 2 | 750 | +6.0 | 7.5× | Bhatti (Smelter) |
| **Wootz Steel (Ukku)** | 3 | 1,800 | +8.0 | 9.5× | High-Heat Forge + Crucible |
| **Vajra (Divine Core)**| 4 | 3,200 | +11.0 | 12.0× | Paataal Boss Drop |

### 3.3 Food & Agronomy Schema
```json
{
  "id": "shilploka:dal_chawal",
  "name": "item.shilploka.dal_chawal.name",
  "category": "food",
  "maxStackSize": 16,
  "icon": "items/dal_chawal",
  "foodProperties": {
    "nutrition": 8,
    "saturation": 6.0,
    "alwaysEdible": false,
    "curePoison": false,
    "eatingTimeTicks": 32
  }
}
```

---

## 4. Biome Registry Specification

### 4.1 Biome Schema (`assets/data/biomes/<biome_name>.json`)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "shilploka:thar_marusthal",
  "name": "biome.shilploka.thar_marusthal.name",
  "climate": {
    "temperature": 0.95,
    "rainfall": 0.05
  },
  "visuals": {
    "skyColor": "#E6CC99",
    "fogColor": "#D9B380",
    "waterColor": "#738C8C",
    "grassColor": "#B5A642",
    "foliageColor": "#8C8C46"
  },
  "surfaceRule": {
    "topBlock": "shilploka:desert_sand",
    "topDepth": 4,
    "underBlock": "shilploka:yellow_sandstone",
    "bedrockY": 0
  },
  "flora": [
    { "feature": "khejri_tree", "density": 0.02 },
    { "feature": "desert_scrub", "density": 0.08 }
  ],
  "spawns": {
    "passive": [
      { "entity": "shilploka:camel", "weight": 10, "minGroup": 2, "maxGroup": 4 }
    ],
    "hostile": [
      { "entity": "shilploka:daaku", "weight": 6, "minGroup": 1, "maxGroup": 3 }
    ]
  },
  "structures": [
    { "structure": "shilploka:rajput_hill_fort", "weight": 2 },
    { "structure": "shilploka:stepwell", "weight": 5 }
  ],
  "weatherWeights": {
    "clear": 0.90,
    "barsaat": 0.02,
    "toofan": 0.01,
    "aandhi": 0.07,
    "himpaat": 0.00
  }
}
```

---

## 5. Recipe Registry Specification

### 5.1 Shaped Recipe (`assets/data/recipes/bronze_talwar.json`)

```json
{
  "type": "shilploka:crafting_shaped",
  "station": "shilploka:karyashala",
  "pattern": [
    " B ",
    " B ",
    " S "
  ],
  "key": {
    "B": { "item": "shilploka:bronze_ingot" },
    "S": { "item": "shilploka:sal_stick" }
  },
  "result": {
    "item": "shilploka:bronze_talwar",
    "count": 1
  }
}
```

### 5.2 Smelting / Alloy Recipe (`assets/data/recipes/bronze_ingot_alloy.json`)

```json
{
  "type": "shilploka:smelting_alloy",
  "station": "shilploka:bhatti",
  "inputs": [
    { "item": "shilploka:taamba_ingot", "count": 2 },
    { "item": "shilploka:ranga_ingot", "count": 1 }
  ],
  "fuelCost": 200,
  "smeltTimeTicks": 160,
  "experience": 0.7,
  "result": {
    "item": "shilploka:bronze_ingot",
    "count": 3
  }
}
```

---

## 6. Entity Data Specification

### 6.1 Entity Schema (`assets/data/entities/<entity_name>.json`)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "shilploka:royal_bengal_tiger",
  "name": "entity.shilploka.royal_bengal_tiger.name",
  "category": "hostile_predator",
  "boundingDimensions": {
    "width": 1.2,
    "height": 1.1,
    "depth": 2.2
  },
  "attributes": {
    "maxHealth": 35.0,
    "movementSpeed": 0.32,
    "attackDamage": 8.0,
    "followRange": 24.0,
    "knockbackResistance": 0.4
  },
  "aiGoals": [
    { "type": "pounce_attack", "priority": 1, "cooldownTicks": 60 },
    { "type": "stalk_target", "priority": 2, "stealthSpeed": 0.16 },
    { "type": "target_player", "priority": 3 },
    { "type": "target_deer", "priority": 4 },
    { "type": "wander_avoid_water", "priority": 5 }
  ],
  "lootTable": "shilploka:entities/tiger_drops"
}
```

---

## 7. World Save Format Specification

### 7.1 `level.json` Schema

```json
{
  "formatVersion": 1,
  "gameVersion": "0.1.0",
  "worldName": "Mera Bharat",
  "seed": 8392184719283719,
  "createdTimestamp": 1727045000,
  "lastPlayedTimestamp": 1727048600,
  "gameMode": "survival",
  "difficulty": "saadharan",
  "worldTimeTicks": 24600,
  "dayCount": 2,
  "weather": {
    "currentWeather": "barsaat",
    "remainingDurationTicks": 4200
  },
  "spawnCoordinates": {
    "x": 64.5,
    "y": 68.0,
    "z": 128.5
  },
  "gamerules": {
    "doDaylightCycle": true,
    "doWeatherCycle": true,
    "keepInventory": false,
    "mobSpawning": true
  }
}
```

### 7.2 `player.json` Schema (`playerdata/player.json`)

```json
{
  "uuid": "4bb4e1d4-f8e5-484f-9f7f-f641b2d7017f",
  "position": [64.2, 68.0, 128.8],
  "rotation": [-90.0, 0.0],
  "dimension": "shilploka:bhoolok",
  "survivalStats": {
    "health": 20.0,
    "bhookh": 18,
    "saturation": 5.0,
    "exhaustion": 1.2,
    "airBubbles": 300,
    "anubhavLevel": 14,
    "anubhavPoints": 340
  },
  "inventory": [
    { "slot": 0, "item": "shilploka:wootz_talwar", "count": 1, "damage": 24 },
    { "slot": 1, "item": "shilploka:stone_kudaal", "count": 1, "damage": 80 },
    { "slot": 2, "item": "shilploka:roti", "count": 12, "damage": 0 }
  ]
}
```

---

## 8. Itihaas Granth (History Codex) Schema

### 8.1 Codex Entry Schema (`assets/data/codex/<entry_name>.json`)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "id": "shilploka:harappan_great_bath",
  "era": "Sindhu-Saraswati",
  "unlockTrigger": "discover_structure:shilploka:harappan_great_bath",
  "title": {
    "en": "The Great Bath of Mohenjo-daro",
    "hi": "मोहनजोदड़ो का विशाल स्नानागार"
  },
  "narrative": {
    "en": "Constructed circa 2600 BCE, the Great Bath is among the earliest public water tanks in world history. Harappan master builders achieved complete waterproofing using fitted fired bricks laid in gypsum mortar with a central damp-proof layer of natural bitumen.",
    "hi": "लगभग 2600 ईसा पूर्व निर्मित, यह विशाल स्नानागार विश्व इतिहास के प्राचीनतम सार्वजनिक जल कुंडों में से एक है। हड़प्पा के शिल्पकारों ने पकी ईंटों, जिप्सम गारे और प्राकृतिक बिटुमेन (डामर) की परत का उपयोग करके इसे पूर्णतः जलरोधी बनाया था।"
  },
  "sourceCitation": "Archaeological Survey of India (Excavations at Mohenjo-daro)",
  "associatedBlocks": [
    "shilploka:fired_brick",
    "shilploka:bitumen_mortar",
    "shilploka:great_bath_masonry"
  ],
  "associatedItems": [
    "shilploka:copper_seal",
    "shilploka:terracotta_pot"
  ],
  "previewModel": "models/structures/great_bath_mini.vox"
}
```