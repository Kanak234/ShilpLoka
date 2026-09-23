# ShilpLoka (Java Edition) — Product Requirements Document (PRD)

* **Document Version**: 1.0.0 (Phase P0 Baseline)
* **Author**: Lead Game Engineer (Pair-Programming with Kanak Prabhakar)
* **Status**: PROPOSED — Awaiting Kanak's Final Approval

---

## 1. Product Overview & Vision

### 1.1 Executive Summary
**ShilpLoka (शिल्पलोक — "World of Craft")** is an original, first-person voxel sandbox game blending survival, creative building, exploration, trade, and crafting within a world inspired by the rich history, geography, arts, crafts, and ecology of the Indian subcontinent (Bharat). 

Originally created by Kanak Prabhakar as an ancient-Indian-themed web sandbox ([itch.io release](https://kanak234.itch.io/shilploka)), this project represents its complete, ground-up rewrite in **Java 21** using a dedicated, high-performance voxel engine built on **LWJGL 3** and **OpenGL 3.3 Core**.

### 1.2 Core Identity & Originality Rule
While sharing the voxel sandbox genre with titles like Minecraft, **ShilpLoka is a 100% original creative work**. 
- **Zero Asset / Lore Copying**: Under no circumstances does ShilpLoka copy Minecraft assets, entity designs, identifiers, names, sound effects, or code.
- **Terminology**: No "Creeper", "Enderman", "Nether", "End", "Redstone", "Villager", "Steve", or "Emerald".
- **Unique Cultural Foundations**: Every creature, block type, workstation, tool tier, architectural template, soundscape, and piece of lore is grounded in Indian history, folklore, and geography.

### 1.3 Cultural Tone & Sensitivity Guidelines
ShilpLoka celebrates India's civilization, architecture, and ecology with absolute respect and academic rigor:
1. **Deities are Sacred Sanctuaries, Never Enemies**: Deities and sacred figures are **never** depicted as enemies, hostile entities, combat targets, loot sources, or killable NPCs. Religious sites (ancient temples, stupas, monasteries) generate as peaceful, indestructible architectural heritage landmarks.
2. **Sacred Fauna**: Cows (*Gau*) are peaceful and sacred; they are **never** a meat/food source. Cows provide milk only.
3. **Historical Accuracy & Neutrality**: All codex and architectural content is derived from respected archaeological and educational sources (Archaeological Survey of India, NCERT). No present-day political disputes, sectarian narratives, or community stereotypes are permitted.
4. **Regional Diversity**: The game showcases all geographical and cultural zones of the subcontinent: the Himalayan peaks, Gangetic alluvial plains, Thar desert, Deccan basalt plateau, Sahyadri rain forests, Sundarban delta mangroves, northeastern bamboo hills, and coastal maritime zones.

---

## 2. Target Platforms & Hardware Budget

### 2.1 Operating Systems
- **Linux (Primary Tier 1)**: Native development and testing on Kubuntu / Ubuntu LTS. Packaged as `.deb` and standalone portable `.tar.gz`.
- **Windows (Tier 1)**: Windows 10/11 64-bit. Packaged as `.msi` installer and portable `.zip`.
- **macOS**: Explicit non-goal for v1 (deferred to v2).

### 2.2 Performance Targets
- **Reference Hardware**: NVIDIA GeForce RTX 3050 Laptop GPU (4 GB VRAM) / 16 GB System RAM.
  - Target: **Solid 60 FPS** at Render Distance **8 chunks** (16³ sections) with Fancy graphics, Smooth Lighting, and Ambient Occlusion.
- **Minimum Hardware**: Integrated AMD Radeon Graphics / Intel Iris Xe with 8 GB System RAM.
  - Target: **Solid 30 FPS** at Render Distance **4 chunks** with Fast graphics.
- **Memory Ceiling**: JVM heap utilization must stay strictly **under 2.0 GB** during active exploration at render distance 8.

---

## 3. Product Goals & Non-Goals

### 3.1 Primary Goals (v1 Release)
1. **Complete Sandbox Gameplay Loop**: Gather natural materials, build functional shelters, mine authentic ores, smelt alloys in high-temperature forges, craft tools through distinct technological tiers, survive day and night cycles, farm indigenous crops, and trade with regional villages.
2. **Living History Codex ("Itihaas Granth")**: Discovery-driven historical codex unlocked by exploring procedurally generated structural ruins spanning 8 major historical eras.
3. **End-to-End Progression**: Progress from primitive wooden and stone tools through Harappan bronze and medieval wootz steel to end-game legendary Vajra gear by confronting the guardian boss in the Paataal dimension.
4. **Production-Grade Java Engine**: Zero memory leaks, palette-compressed chunk storage, greedy meshing, robust crash recovery, and crash-proof atomic save serialization.
5. **Self-Contained Distribution**: Bundled trimmed JRE via `jlink` and `jpackage` so end users require no pre-installed Java environment.

### 3.2 Non-Goals (v1 Release)
- **Online Multiplayer**: v1 is strictly single-player. However, internal client-server architecture is maintained so LAN/multiplayer can be integrated in v2 without rewrites.
- **Modding API**: Public modding API deferred; internal content registries are 100% data-driven via JSON.
- **Mobile & Consoles**: Mobile touches/joysticks from the web prototype are not part of the v1 desktop release.
- **Microtransactions, Telemetry, Accounts**: No external telemetry, tracking, cloud accounts, or internet calls. The game functions 100% offline.

---

## 4. Core Gameplay Mechanics

### 4.1 Game Modes & Difficulties

#### Game Modes
1. **Survival (Astitva)**: Health (lotus hearts), hunger (*Bhookh*), resource harvesting, fall damage, hostile night creatures, and progressive crafting workstations.
2. **Creative (Srijan)**: Unrestricted flight, invulnerability, infinite block palette, instantaneous block breaking, and environmental control commands.
3. **Kathin (Hardcore)**: Single-life survival. Permadeath locks the world into spectator/delete mode upon death.

#### Difficulty Levels
- **Shaanti (Peaceful)**: No hostile creatures spawn. Health regenerates naturally; hunger does not drain below 1 lotus.
- **Aasaan (Easy)**: Hostile creatures deal reduced damage; hunger drains slowly.
- **Saadharan (Normal)**: Standard combat damage, standard hunger depletion, full mob spawning.
- **Kathin (Hard)**: Hostile creatures deal heavy damage, status effects last longer, starvation can deplete health to zero.

---

### 4.2 World Geometry & Environment

- **Coordinate System**: Cartesian coordinates $(X, Y, Z)$ where $Y$ represents elevation (0 to 320 blocks).
- **Chunk Geometry**: 
  - A Chunk Column measures $16 \times 16$ blocks horizontally and 320 blocks vertically.
  - Divided into twenty **16³ Section Cubes** ($16 \times 16 \times 16$).
  - Palette-compressed block storage (16-bit IDs referencing a dynamic section palette).
- **Seeded Procedural Generation**:
  - Deterministic generation using a 64-bit integer seed.
  - Identical seed guarantees identical terrain contours, biome boundaries, ore veins, and structural placements.
- **Astronomical & Day/Night Cycle**:
  - Full day/night cycle lasts **20 real-time minutes** (10 min day, 1.5 min dusk, 7 min night, 1.5 min dawn).
  - Celestial dome displays procedural sun, moon with 8 orbital phases, dynamic cloud layers, and seasonal star constellations.
- **Dynamic Weather Systems**:
  - **Barsaat (Monsoon)**: Seasonal torrential rain, heavy cloud cover, amplified waterfall flow, crop growth speed bonus. Heaviest in Sahyadri and Poorvottar hills.
  - **Toofan (Thunderstorm)**: Lightning strikes, darkened skies, elevated hostile spawning.
  - **Aandhi (Thar Dust Storm)**: Reduced visibility, ambient particulate fog, howling desert wind.
  - **Himpaat (Himalayan Snow)**: Dynamic snow layer accumulation on exposed surfaces at high altitudes.
- **Cellular Automata Fluids & Physics**:
  - Realistic spreading for Water and Lava (fluid levels 1–8).
  - Gravity simulation for loose blocks (sand, gravel, salt).

---

### 4.3 Biomes of Bharat (11 Distinct Environments)

| Biome Identifier | Native Name | Surface & Terrain | Unique Flora & Fauna | Structures & Geological Features |
|---|---|---|---|---|
| `shilploka:himalaya_shikhar` | **Himalaya Shikhar** | Deep snow, ice, granite peaks, high crags | Deodar pine, alpine shrubs; Snow leopards, yaks | Glaciers, high mountain monasteries, hot springs |
| `shilploka:terai_van` | **Terai Van** | Lush grass, deep loam, dense marshy lowlands | Sal trees, tall elephant grass; Bengal tigers, Indian elephants | River channels, elephant grazing grounds, hunter shelters |
| `shilploka:ganga_maidan` | **Ganga Maidan** | Rich alluvial soil, expansive plains, river bends | Banyan, mango, neem, sugarcane, wheat; Cows, river dolphins | Farming gaon villages, ghat riverbanks, Harappan ruins |
| `shilploka:thar_marusthal` | **Thar Marusthal** | Red and yellow sand dunes, sandstone outcrops | Khejri trees, thorny scrub; Camels, desert foxes | Rajput hill forts, deep stepwells (*baolis*), sandstone havelis |
| `shilploka:rann_namak` | **Rann Namak** | Brilliant white salt crust, cracked alkali flats | Halophyte shrubs; Greater flamingos, wild asses | Natural salt harvesting pans, mirage effects |
| `shilploka:deccan_pathar` | **Deccan Pathar** | Dark basalt steps (*traps*), black cotton soil | Teak, acacia, cotton shrubs; Indian wolves, blackbucks | Rock-cut Buddhist/Hindu cave shrines, boulder citadels |
| `shilploka:sahyadri_varshavan` | **Sahyadri Varshavan**| Laterite stone, mossy crags, high plateaus | Sheesham, ferns, wild spices; Great hornbills, macaques | Living waterfalls, dense monsoon canopies, Maratha forts |
| `shilploka:sundarban` | **Sundarban** | Deep tidal mud, brackish water channels | Mangrove trees with aerial roots; Saltwater crocodiles, tigers | Mud villages, fishing platforms, tidal estuaries |
| `shilploka:poorvottar_baans` | **Poorvottar Baans** | Steep emerald hills, mist-shrouded valleys | Giant bamboo groves, orchids; Red pandas, hornbills | Living root bridges across gorges, stilt bamboo huts |
| `shilploka:samudra_tat` | **Samudra Tat** | Fine coastal sand, seashell margins, estuaries | Coconut palms, coastal shrubs; Crabs, coastal fish | Fishing hamlets, coastal shrines, wooden catamaran docks |
| `shilploka:sagar` | **Sagar** | Deep open ocean, submarine trenches, kelp | Coral reefs, sea turtles, diverse fish | Ancient shipwrecked spice dhows, sunken stone anchors |

---

### 4.4 Material & Block Classifications

#### 1. Stones & Minerals
- **Granite**: High hardness, found throughout crags and deep underground.
- **Red Sandstone**: Medium hardness, primary building stone for imperial forts and monuments.
- **Yellow Jaisalmer Sandstone**: Warm desert building stone.
- **Makrana White Marble**: High-grade ornamental stone for palaces and monuments.
- **Deccan Basalt**: Dark columnar volcanic stone composing the Deccan plateau.
- **Laterite**: Porous, iron-rich reddish rock cut into durable building blocks.
- **Slate & Khondalite**: Regional metamorphic stones for roofing and temple carvings.

#### 2. Soils & Sediments
- **Alluvial Soil**: Highly fertile Gangetic river silt; supports highest crop yields.
- **Black Cotton Soil (*Regur*)**: Moisture-retentive basaltic soil ideal for cotton farming.
- **Red Soil**: High iron content, found in Deccan margins.
- **Desert Sand (Red & Yellow)**: Unstable falling sediment.
- **Mud & Wet Clay**: Harvested from riverbeds and mangroves for pottery and bricks.
- **Salt Block**: Harvested from the Rann; culinary and preservation use.

#### 3. Woods & Arboreal Ecosystems
Each wood species provides dedicated **Log, Stripped Log, Planks, Stairs, Slab, Door, Trapdoor, Fence, and Gate**:
- **Sal (*Shorea robusta*)**: Heavy, termite-resistant structural timber.
- **Teak (*Sagwan*)**: Premium golden-brown weather-resistant hardwood.
- **Sheesham (*Indian Rosewood*)**: Dark, dense ornamental wood.
- **Neem**: Medicinal wood; leaves ward off pests.
- **Deodar (*Cedar of the Gods*)**: Fragrant alpine softwood.
- **Banyan (*Vatavriksha*)**: Massive sprawling tree with hanging aerial prop roots.
- **Peepal (*Sacred Fig*)**: Distinctive spade-shaped foliage; non-harvestable heritage variants in villages.
- **Mango (*Aam*)**: Yields timber and edible seasonal mango fruit.
- **Coconut Palm**: Coastal tropical wood; yields edible coconuts and coir fiber.
- **Bamboo**: Fast-growing segmented culms for lightweight scaffolding, fences, and bridges.

#### 4. Crafted, Cultural & Architectural Blocks
- **Terracotta & Fired Harappan Brick**: Standardized dimensional bricks (1:2:4 ratio).
- **Bitumen Mortar**: Waterproof Harappan asphalt sealant.
- **Lime Plaster (*Chuna*)**: Whitewash and smooth wall coating.
- **Jaali (Latticework Panels)**: Perforated stone screens permitting light and airflow.
- **Chhatri Roofs**: Domed cupolas on slender pillars for fortress watchpoints.
- **Jharokha Windows**: Overhanging stone balcony frames.
- **Athangudi Tiles**: Hand-pressed floral and geometric floor tiles.
- **Carved Granite Pillars**: Ornate temple and mandapa support pillars.
- **Diya (Clay Lamp)**: Emits warm level 14 block light when filled with mustard oil.
- **Mashaal (Torch)**: Resin and bamboo torch (light level 12).
- **Brass Lantern**: Windproof lantern (light level 15).

#### 5. Ores & Metallurgy
- **Koyla (Coal)**: Fuel source found at all elevations.
- **Taamba (Copper)**: Early-game metal for cookware, tools, and alloys.
- **Ranga (Tin)**: Essential alloying component for Bronze.
- **Jasta (Zinc)**: Essential alloying component for Brass.
- **Loha (Iron)**: Workhorse structural and tool metal.
- **Sona (Gold)**: Decorative, ceremonial, and currency metal.
- **Chaandi (Silver)**: Coins, jewelry, and ornate mirrors.
- **Abhrak (Mica)**: High-heat insulation and decorative shimmer.
- **Rajavarta (Lapis Lazuli)**: Deep blue mineral used for pigments and Kaushal upgrades.
- **Hira (Diamond)**: Deep volcanic diamond deposits (Golconda-inspired).

---

### 4.5 Technological Progression & Tool Tiers

Progression follows historical metallurgy and craft mastery:

$$\text{Wood} \longrightarrow \text{Stone} \longrightarrow \text{Copper} \longrightarrow \text{Bronze} \longrightarrow \text{Iron} \longrightarrow \textbf{Wootz Steel} \longrightarrow \textbf{Vajra}$$

| Tier | Required Workstation | Durability | Harvest Level | Mining Speed | Attack Damage | Rationale & Historical Context |
|---|---|---|---|---|---|---|
| **Wood** | Personal 2×2 / Hands | 60 | 0 | 2.0× | +1 | Hand-whittled sal and bamboo tools. |
| **Stone** | Karyashala (Workbench) | 132 | 1 | 4.0× | +2 | Flaked granite and river cobble. |
| **Copper** | Kiln / Crucible | 250 | 1 | 5.5× | +3 | Native copper hammered into basic blades. |
| **Bronze** | Bhatti (Smelter) | 420 | 2 | 6.5× | +4.5 | Harappan alloy: 88% Copper + 12% Tin. Highly durable. |
| **Iron** | Bhatti (Smelter) | 750 | 2 | 7.5× | +6 | Bloomery-smelted wrought iron. |
| **Wootz Steel** | High-Heat Forge + Crucible | 1,800 | 3 | 9.5× | +8 | Ancient crucible steel (high carbon, Damascus banding). |
| **Vajra** | Paataal Altar | 3,200 | 4 | 12.0× | +11 | End-game legendary mineral dropped by Andhakaar Naag. |

#### Complete Tool Arsenal
- **Kudaal (Pickaxe)**: Mines stone, masonry, and ores.
- **Kulhadi (Axe)**: Fells trees and processes timber.
- **Phaavda (Shovel)**: Excavates soils, sand, mud, and gravel.
- **Khurpi (Hoe)**: Tills soil and creates irrigation channels.
- **Talwar (Sword)**: Fast slashing melee weapon.
- **Dhaal (Shield)**: Blocks incoming melee attacks and arrows.
- **Dhanush & Baan (Bow & Arrows)**: Ranged combat weapon.
- **Bhala (Spear)**: Medium-range thrusting weapon with knockback.
- **Machhli Danda (Fishing Rod)**: Casts line into river and ocean bodies.

---

### 4.6 Workstations & Crafting Ecology

Crafting is data-driven via JSON recipes and requires dedicated stations:

```
[Personal 2x2 Crafting] 
       │
       ▼
[Karyashala (3x3 Workbench)] ───► Basic Tools, Furniture, Chests
       │
       ├───► [Kiln] ─────────────► Fired Bricks, Terracotta, Glass, Chuna Plaster
       ├───► [Bhatti (Forge)] ────► Bronze (Cu+Sn), Brass (Cu+Zn), Iron Smelting
       │         └───► [Crucible] ──► Wootz Crucible Steel (Iron + Charcoal)
       ├───► [Chaak (Wheel)] ────► Clay Matkas (Water Storage), Diyas, Jars
       ├───► [Loom (Kargah)] ────► Cotton Cloth, Dyed Banners (Indigo, Haldi)
       ├───► [Chakki (Mill)] ────► Grain to Flour, Spices to Pigments
       └───► [Nihai (Anvil)] ────► Tool Repair, Renaming, Quenching
```

1. **Karyashala (3×3 Workbench)**: Assembled from 4 wooden planks. Enables full $3 \times 3$ shaped and shapeless recipe grid.
2. **Kiln (Pakki Bhatti)**: Built from fired bricks and clay. Bakes raw clay items into durable terracotta, melts sand into clear glass, and burns limestone into lime plaster.
3. **Bhatti (Smelting Forge)**: Built from basalt and granite. Reaches alloying temperatures to smelt copper, tin, and zinc into **Bronze** and **Brass**, and blooms iron ore.
4. **Crucible (*Kuthai*)**: High-temperature sealed terracotta vessel used in the Bhatti to melt iron blooms with charcoal and leaves, producing **Wootz steel ingots**.
5. **Chaak (Potter's Wheel)**: Spun to shape wet clay into water *matkas* (retaining fresh water), culinary bowls, grain silos, and ornate flower vases.
6. **Loom (*Kargah*)**: Weaves harvested cotton fibers into cloth, ropes, sails, and decorative tapestries dyed with natural indigo, turmeric (*haldi*), and madder (*manjistha*).
7. **Chakki (Stone Grindstone)**: Manually rotated stone disk that grinds harvested wheat, rice, and bajra into fine flour, and crushes herbs into healing pastes.
8. **Nihai (Anvil)**: Forged iron block used to hammer damaged tools back to full durability using matching material ingots and an experience cost.
9. **Kuan (Well) & Rahat (Water Wheel)**: Endless water source blocks enabling automated irrigation trenches for nearby crops.

---

### 4.7 Agriculture, Food & Survival

#### Crops & Agronomy
- **Gehun (Wheat)**: Basic staple crop; milled in Chakki for Roti dough.
- **Dhaan (Paddy Rice)**: Requires flooded alluvial soil (1 block water depth); produces white rice.
- **Bajra (Pearl Millet)**: Drought-tolerant grain grown in Thar and Deccan soils.
- **Kapas (Cotton)**: Yields raw cotton fibers for the loom.
- **Ganna (Sugarcane)**: Waterway crop processed into *Gud* (jaggery sugar blocks).
- **Dal (Lentils)**: High-nutrition pulse crop.
- **Haldi (Turmeric)**: Crop used for healing salves and vibrant yellow textile dye.
- **Mirch (Chillies)**: Spice crop adding seasoning to dishes.
- **Chai (Tea Bush)**: High-altitude hill crop grown exclusively in Himalayan and Poorvottar slopes.

#### Culinary & Nutrition System
Players balance Health (10 lotus icons) and Hunger (*Bhookh*, 10 roti icons). Low hunger halts health regeneration; complete exhaustion causes starvation damage:
- **Roti**: Flat unleavened bread baked on a hot tawa/furnace (Restores 5 Bhookh).
- **Dal-Chawal**: Steamed rice and spiced lentil stew (Restores 8 Bhookh + 6 Saturation).
- **Khichdi**: Nourishing comfort food cooked in a matka (Restores 9 Bhookh + 10 Saturation).
- **Sabzi**: Cooked vegetable melange (Restores 6 Bhookh).
- **Doodh & Paneer**: Fresh milk obtained from cows (removes poison effects); curdled into high-protein paneer blocks.
- **Tropical Fruits**: Wild mangoes, bananas, jackfruit, and coconuts harvested from trees.

---

### 4.8 Fauna & Creature Catalog

All creatures have original AI behavioral routines, sounds, and bounding boxes.

#### 1. Passive Wildlife
- **Gau (Indian Zebu Cow)**: Sacred creature with dorsal hump. Cannot be killed; milking with a metal bucket yields fresh milk.
- **Bhains (Water Buffalo)**: Found in rivers and paddy fields; yields buffalo milk.
- **Bakri & Bhed (Goat & Sheep)**: Mountain and plain grazers yielding soft wool and milk.
- **Oont (Camel)**: Native to Thar Marusthal. Can be saddled and ridden across sand without speed penalties.
- **Haathi (Indian Elephant)**: Majestic forest herbivore. Tameable with sugarcane; can be equipped with a riding howdah and carry dual storage chests.
- **Ghoda (Horse)**: Fast overland mount found in plains.
- **Mor (Peacock)**: Displays iridescent plumage; drops vibrant feathers for arrow fletching and decoration.
- **Tota (Parrot)**: Colorful canopy bird; can be tamed with seeds to perch on the player's shoulder.
- **Bandar (Monkey)**: Mischievous canopy resident that snatches dropped food items and throws fruit.
- **Yak**: Heavy-coated pack animal found in Himalayan altitudes; provides thick wool.
- **Hiran (Spotted Deer)**: Shy herbivore in Terai and deciduous forests.
- **Susu (Gangetic River Dolphin)**: Rare aquatic mammal navigating major river waterways.
- **Hornbill & Red Panda**: Forest and bamboo hill fauna.

#### 2. Neutral Wildlife
- **Macaque Troop**: Forest monkeys that become aggressive only if the player attacks an infant or invades their food tree.
- **Madhumakhi (Bees)**: Forest pollinators that produce wild honey in tree hives; attack with venom if smoke is not used.
- **Jungli Suar (Wild Boar)**: Forest wanderer that charges if approached too closely.
- **Bhalu (Sloth Bear)**: Solitary creature in rocky ravines that defends its territory.

#### 3. Hostile Predators
- **Baagh (Royal Bengal Tiger)**: Stealth apex predator of Terai and Sundarban. Stalks through tall grass and pounces from behind.
- **Tendua (Indian Leopard)**: Climbs trees in rocky terrain and drops onto unwary travelers.
- **Naag (Indian Cobra)**: Hisses defensively when approached; striking causes lethal venom poison.
- **Magarmachh (Mugger Crocodile)**: Lurks submerged in rivers and swamps; executes sudden ambush lunges.
- **Bhediya (Wolf Pack)**: Roams Deccan plateaus and desert margins in nocturnal hunting packs.

#### 4. Hostile Folklore Entities (Original, Non-Religious)
- **Vetaal**: Nocturnal humanoid apparition hanging inverted from ancient banyan branches. Drops down with piercing shrieks to ambush travelers.
- **Pishach**: Emaciated, hunched cave dweller stalking dark subterranean depths. Basic relentless melee attack.
- **Chhaya**: Pure shadow entity appearing exclusively in Light Level 0. Has no physical mass; phase-touches the player to drain hunger (*Bhookh*) and stamina.
- **Daaku**: Cunning bandit outlaws residing in fortified ravine hideouts (Chambal style). Wield bows, talwars, and clubs.
- **Mayavi Machhar**: Swarms of swamp mosquitoes in the Sundarbans at dusk; inflict weakness.

#### 5. Guardian Boss
- **Andhakaar Naag (Serpent of the Abyss)**:
  - A colossal, multi-crested shadow serpent dwelling inside the molten core of the **Paataal** dimension.
  - Commands shadow breath attacks, summons subterranean tremors, and whips magma waves.
  - Defeating the guardian yields the legendary **Vajra Core**, used to forge end-game Vajra tools and unlock the portal to **Akash Dweep**.

---

### 4.9 Gaon (Villages), NPCs & Barter Economy

Procedurally generated rural settlements (*Gaon*) generate across plains, deserts, coastal shores, and hilltops, featuring regional architecture:
- **Village Layout**: Centered around a sprawling ancient Banyan tree, a communal well, and a shaded **Panchayat** pavilion.
- **Professions & Wares**:
  1. **Kisan (Farmer)**: Trades grains, seeds, and fruits for kauri shells.
  2. **Kumhar (Potter)**: Sells water matkas, glazed terracotta, and decorative pots.
  3. **Lohar (Blacksmith)**: Trades iron billets, tool heads, and wootz forging tips.
  4. **Julaha (Weaver)**: Sells cotton bolts, dyed banners, and coir rope.
  5. **Vaidya (Herbalist)**: Dispenses healing salves, antivenoms, and herbal teas.
  6. **Sunar (Goldsmith)**: Exchanges coins and sells precious ornaments.
  7. **Machhuara (Fisher)**: Sells rare river fish, nautilus shells, and fishing nets.
  8. **Shilpi (Stone Carver)**: Trades carved pillars, jaali screens, and Athangudi tiles.
  9. **Granthpal (Librarian)**: Sells ancient manuscript codex pages and Kaushal scrolls.
  10. **Dhanapati (Master Merchant)**: Rare wandering merchant (carrying forward the beloved character from the original web game) offering exotic foreign goods and rare seeds.
- **Autonomous NPC Behaviors**:
  - Daily schedules: Tend crops/workstations by day, gather at the Panchayat in late afternoon, sleep indoors on *charpais* by night.
  - Weather reactivity: Villagers seek indoor shelter during heavy monsoon rain (*Barsaat*).
  - Defend & Evacuate: Villagers run indoors and ring the Panchayat bell when **Daaku raids** occur at night.

#### Currency Hierarchy
- **Kauri Shells**: 64 Kauri = 1 Copper Mudra
- **Taamba Mudra (Copper Coin)**: 16 Copper = 1 Silver Coin
- **Chaandi Mudra (Silver Coin)**: 8 Silver = 1 Gold Coin
- **Sona Mudra (Gold Coin)**: Highest-tier trade currency

---

### 4.10 Kaushal System (Skill Upgrades)

Replacing conventional enchantment systems, the **Kaushal (शिल्प कौशल)** upgrade system represents refined artisanal technique and weapon conditioning at the **Shilp Mandap** workstation.

- **Mechanics**: The player spends earned experience points (**Anubhav**) and **Rajavarta (Lapis)** to apply permanent craft enhancements onto tools, weapons, and armor.
- **Key Kaushal Disciplines**:
  - **Tez (Speed)**: Elevates tool excavation velocity.
  - **Majboot (Durability)**: Reduces probability of tool wear per usage.
  - **Tej-Dhaar (Sharpness)**: Increases talwar and bhala slashing damage.
  - **Door-Drishti (Far-Sight)**: Enhances bow range and arrow velocity.
  - **Jal-Shwaas (Water-Breath)**: Extends underwater air bubble reserves.
  - **Agni-Nirodh (Fire Resistance)**: Mitigates lava and burn damage.
  - **Bhagya (Fortune)**: Increases ore drop yields per vein.

---

### 4.11 Dimensions of Existence

1. **Bhoolok (The Terrestrial Realm)**:
   - The primary overworld containing all 11 biomes, villages, historical monuments, caves, and oceans. World height: 0 to 320.
2. **Paataal (The Subterranean Core)**:
   - Entered through a rectangular portal frame constructed from **Deccan Basalt** and sealed with molten obsidian glass (*Kaala Kaanch*).
   - Cavernous subterranean realm composed of roaring lava oceans, volcanic basalt columns, brimstone flats, and fire fungi.
   - Home of the **Andhakaar Naag** boss.
3. **Akash Dweep (The Celestial Sky Isles)**:
   - Reached late-game via an Akashic portal activated with the **Vajra Core**.
   - Floating paradise islands drifting above endless clouds.
   - Serene, peaceful dimension containing rare luminescent flora, celestial springs, and pure wootz/vajra veins.

---

### 4.12 Itihaas Granth (The Living History Codex)

An in-game, illustrated historical encyclopedia dynamically populated as the player discovers procedural ruins, interacts with era artifacts, and harvests historical materials.

- **Codex Structure**:
  - Organized by 8 historical periods (Sindhu-Saraswati, Maurya, Gupta, Chola, Vijayanagara, Rajput/Maratha, Indo-Islamic, Colonial-era).
  - Each unlocked entry presents **2 to 4 concise, factually accurate, neutral sentences** derived from ASI and NCERT sources.
  - Displays linked 3D voxel models of the corresponding structures and items.
  - Full bilingual presentation: Toggle between English and Devanagari Hindi at any time.
- **Era Loot**: Ruined structure chests contain authentic cultural artifacts (Harappan seals, copper punch-marked coins, palm-leaf manuscripts). Reading manuscripts immediately registers entries in the Itihaas Granth.

---

### 4.13 Cultural Festivals & Dynamic Seasonal Events

Optional date-driven events reflecting the Indian cultural calendar (toggleable in Settings; disabled by default in Kathin mode):
- **Diwali (Deepavali)**: Clay diyas in villages illuminate automatically with golden light; festive fireworks items crafted with sulfur and color powders.
- **Holi**: Natural color powder pouches (*Gulal*) crafted from turmeric, indigo, and flower petals to dye wool, cloth, and decorative blocks.
- **Makar Sankranti**: Paper kites (*Patang*) flyable in open skies with wind tracking.
- **Harvest Festivals (Onam / Pongal / Bihu / Baisakhi)**: 25% accelerated crop growth speed and feast recipes.

---

## 5. Controls, Commands & Interface

### 5.1 Default Controls (Fully Rebindable)
- **Movement**: `W`, `A`, `S`, `D`
- **Jump / Swim Up**: `Space`
- **Sneak / Crouch**: `Left Shift` (prevents stepping off ledges)
- **Sprint**: `Left Ctrl` (or double-tap `W`)
- **Look**: Mouse cursor (First-person orientation)
- **Primary Action (Break / Attack)**: `Left Mouse Button (LMB)`
- **Secondary Action (Place / Use / Interact)**: `Right Mouse Button (RMB)`
- **Hotbar Slots**: Keys `1` through `9`, or Mouse Scroll Wheel
- **Inventory & Crafting**: `E`
- **Drop Held Item**: `Q` (Ctrl+Q drops entire stack)
- **Perspective Toggle**: `F` (Cycles 1st-person $\to$ 3rd-person back $\to$ 3rd-person front)
- **Itihaas Granth (Codex)**: `G`
- **Chat & Command Bar**: `T`
- **Quick Command Prompt**: `/`
- **Debug Diagnostic HUD**: `F3`
- **Screenshot**: `F2` (saves timestamped PNG into `screenshots/`)
- **Fullscreen Toggle**: `F11`
- **Pause Menu / Back**: `Esc`

### 5.2 Command System (Creative & Cheats-Enabled Worlds)
- `/time set <day|night|noon|midnight|<ticks>>`
- `/weather set <clear|barsaat|toofan|aandhi|himpaat>`
- `/gamemode <survival|creative|kathin>`
- `/difficulty <shaanti|aasaan|saadharan|kathin>`
- `/tp <x> <y> <z>` or `/tp <entity>`
- `/give <item_id> [count]`
- `/seed` (outputs current world generation seed)
- `/locate <structure_id|biome_id>`
- `/kill [entity]`
- `/spawn <entity_id>`
- `/gamerule <rule_name> <value>`

---

## 6. Branding & Watermark Specification

In strict compliance with Master Brief §6:
1. **Logo File of Record**: Kanak Prabhakar's proprietary KP orbit logo mark is located at `assets/branding/logo-mark.svg`.
2. **Integrity Rule**: Never redraw, recolor, distort, or approximate the logo.
3. **Build-Time Rasterization**: Converted via build tooling to anti-aliased PNGs at 64, 128, 256, and 512 px.
4. **Mandatory Placements**:
   - **Splash Screen**: Centered display for 2.0 seconds with "ShilpLoka" title and copyright note.
   - **Title Screen**: Displayed at bottom-right corner at 70% opacity with attribution text: *"by Kanak Prabhakar"*.
   - **Credits Screen**: Prominently featured on the principal creator title card.
   - **Application Icon**: Secondary badge on desktop window icon and taskbar.
   - **Documentation & Installers**: Included in `README.md`, package banners, and `.deb`/`.msi` installers.
   - *HUD Exclusion*: Never rendered over the active gameplay HUD.

---

## 7. Acceptance Criteria (v1 Release Checklist)

A build of ShilpLoka (Java Edition) is certified "Complete and Production-Ready" when:
1. **Full Playthrough Verification**:
   - A player can spawn in a fresh world, survive the first night, mine copper and iron, forge Wootz steel, discover a Gaon village and trade with villagers, discover at least 5 distinct historical era ruins, open the Paataal portal, defeat the Andhakaar Naag boss, and access Akash Dweep without encountering crashes, severe lag, or memory overflow.
2. **Determinism Certification**:
   - Automated unit tests verify that identical world seeds produce bit-exact identical chunk blocks, biomes, and structure positions.
3. **Save/Load Integrity**:
   - Automated round-trip tests confirm that saving a modified world, terminating the engine abruptly (`kill -9`), and reloading preserves 100% of block changes, player inventory, and entity states without file corruption.
4. **Hardware Performance**:
   - Sustains 60 FPS at render distance 8 on an RTX 3050 Laptop GPU, with JVM heap strictly under 2.0 GB.
   - Sustains 30 FPS at render distance 4 on integrated AMD Radeon graphics.
5. **Localization Complete**:
   - 100% of in-game strings translated into Devanagari Hindi in `assets/lang/hi.json`. Zero raw unlocalized keys visible in UI.
6. **Asset & Legal Audit**:
   - Zero Minecraft names, assets, or textures exist in the repository (enforced via automated grep check in CI).
   - 100% of textures, sounds, and fonts are documented in `docs/ASSET_LICENSES.md`.
7. **Packaging & Distribution**:
   - A single command `./gradlew packageAll` produces clean Linux (`.deb`, `.tar.gz`) and Windows (`.msi`, `.zip`) installers with embedded trimmed Java runtimes.