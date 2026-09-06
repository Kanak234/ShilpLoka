/**
 * @fileoverview Ancient Indian Voxel Materials & Registry
 * @module world/ancient_blocks
 * 
 * Features:
 * - Vedic and Indus Valley cultural material domain definitions.
 * - Rigorous physical properties: solidity, opacity, hardness, and drop tables.
 * - Indestructible Monument validation flags enforcing heritage protection
 *   for Harappan citadels, Great Baths, and Ashokan pillars.
 */

/**
 * Unique numeric identifiers for Ancient Indian block types.
 * @readonly
 * @enum {number}
 */
export const VastuBlockId = {
  AIR: 0,
  ALLUVIAL_LOAM: 1,       // Fertile Sindhu soil / Mitti (मृत्तिका)
  TERRACOTTA: 2,          // Baked Terracotta clay brick (पक्व-मृदा)
  HARAPPAN_BRICK: 3,      // Archetypal Harappan Red Brick (4:2:1 ratio) (सिन्धु ईंट)
  CHUNAR_SANDSTONE: 4,    // Golden Buff Chunar Sandstone (चुनार बलुआ पत्थर)
  RIVER_SAND: 5,          // Saraswati & Sindhu riverbed sand (सिकता)
  SACRED_WATER: 6,        // Sindhu Jala / River water (सिन्धु जल)
  BRONZE_ORE: 7,          // Kansa / Copper-Tin ore vein (कांस्य धातु)
  LAPIS_LAZULI: 8,        // Lajward royal blue mineral from Badakhshan (लाजवर्द्)
  SAAGWAN_LOG: 9,         // Sacred Teak tree trunk (शागवान काष्ठ)
  SAAGWAN_LEAVES: 10,     // Sacred Teak tree foliage (शागवान पत्र)
  SAAGWAN_PLANKS: 11,     // Hand-hewn wooden architectural planks (शागवान फलक)
  GREAT_BATH_BITUMEN: 12, // Bitumen & gypsum waterproof brick lining (सिन्धु स्नानागार ईंट)
  PATAL_BEDROCK: 13,      // Indestructible primordial subterranean bedrock (पाताल शिला)
  ASHOKA_MONUMENT: 14,    // Indestructible Ashoka Sthambha Pillar block (अशोक स्तम्भ)
  HARAPPA_MONUMENT: 15,   // Indestructible Mohenjo-Daro / Harappa Citadel Monument (हड़प्पा स्मारक)
};

/**
 * Complete metadata registry defining rendering, physics, and preservation properties.
 * @type {Record<number, {
 *   id: number,
 *   name: string,
 *   devanagari: string,
 *   solid: boolean,
 *   transparent: boolean,
 *   isMonument: boolean,
 *   colorHex: number,
 *   topColorHex?: number,
 *   bottomColorHex?: number,
 *   sound: string,
 *   hardness: number
 * }>}
 */
export const VASTU_REGISTRY = {
  [VastuBlockId.AIR]: {
    id: VastuBlockId.AIR,
    name: 'Air',
    devanagari: 'आकाश',
    solid: false,
    transparent: true,
    isMonument: false,
    colorHex: 0x000000,
    sound: 'none',
    hardness: 0,
  },
  [VastuBlockId.ALLUVIAL_LOAM]: {
    id: VastuBlockId.ALLUVIAL_LOAM,
    name: 'Alluvial Loam',
    devanagari: 'मृत्तिका',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0x4a7c29, // Lush Sindhu vegetation top
    topColorHex: 0x4a7c29,
    bottomColorHex: 0x6e4726,
    sound: 'earth',
    hardness: 1.0,
  },
  [VastuBlockId.TERRACOTTA]: {
    id: VastuBlockId.TERRACOTTA,
    name: 'Terracotta Mitti',
    devanagari: 'पक्व-मृदा',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0xba5d3f,
    sound: 'stone',
    hardness: 2.0,
  },
  [VastuBlockId.HARAPPAN_BRICK]: {
    id: VastuBlockId.HARAPPAN_BRICK,
    name: 'Harappan Baked Brick',
    devanagari: 'सिन्धु ईंट',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0xa84a2c,
    sound: 'stone',
    hardness: 2.5,
  },
  [VastuBlockId.CHUNAR_SANDSTONE]: {
    id: VastuBlockId.CHUNAR_SANDSTONE,
    name: 'Chunar Sandstone',
    devanagari: 'चुनार बलुआ पत्थर',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0xdeb887,
    sound: 'stone',
    hardness: 3.0,
  },
  [VastuBlockId.RIVER_SAND]: {
    id: VastuBlockId.RIVER_SAND,
    name: 'Saraswati River Sand',
    devanagari: 'सिकता',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0xd9c58b,
    sound: 'sand',
    hardness: 0.8,
  },
  [VastuBlockId.SACRED_WATER]: {
    id: VastuBlockId.SACRED_WATER,
    name: 'Sindhu Sacred Water',
    devanagari: 'सिन्धु जल',
    solid: false,
    transparent: true,
    isMonument: false,
    colorHex: 0x2b6cb0,
    sound: 'water',
    hardness: 100.0,
  },
  [VastuBlockId.BRONZE_ORE]: {
    id: VastuBlockId.BRONZE_ORE,
    name: 'Kansa Bronze Ore',
    devanagari: 'कांस्य धातु',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0xcd7f32,
    sound: 'stone',
    hardness: 3.5,
  },
  [VastuBlockId.LAPIS_LAZULI]: {
    id: VastuBlockId.LAPIS_LAZULI,
    name: 'Lajward Lapis Lazuli',
    devanagari: 'लाजवर्द्',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0x1f4788,
    sound: 'stone',
    hardness: 4.0,
  },
  [VastuBlockId.SAAGWAN_LOG]: {
    id: VastuBlockId.SAAGWAN_LOG,
    name: 'Saagwan Teak Log',
    devanagari: 'शागवान काष्ठ',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0x5a3818,
    sound: 'wood',
    hardness: 2.0,
  },
  [VastuBlockId.SAAGWAN_LEAVES]: {
    id: VastuBlockId.SAAGWAN_LEAVES,
    name: 'Saagwan Leaves',
    devanagari: 'शागवान पत्र',
    solid: true,
    transparent: true,
    isMonument: false,
    colorHex: 0x2d682a,
    sound: 'grass',
    hardness: 0.3,
  },
  [VastuBlockId.SAAGWAN_PLANKS]: {
    id: VastuBlockId.SAAGWAN_PLANKS,
    name: 'Carved Saagwan Planks',
    devanagari: 'शागवान फलक',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0x8b5a2b,
    sound: 'wood',
    hardness: 1.8,
  },
  [VastuBlockId.GREAT_BATH_BITUMEN]: {
    id: VastuBlockId.GREAT_BATH_BITUMEN,
    name: 'Great Bath Bitumen Brick',
    devanagari: 'स्नानागार ईंट',
    solid: true,
    transparent: false,
    isMonument: false,
    colorHex: 0x3d302a,
    sound: 'stone',
    hardness: 3.0,
  },
  [VastuBlockId.PATAL_BEDROCK]: {
    id: VastuBlockId.PATAL_BEDROCK,
    name: 'Patal-Shila Bedrock',
    devanagari: 'पाताल शिला',
    solid: true,
    transparent: false,
    isMonument: true,
    colorHex: 0x1a1614,
    sound: 'stone',
    hardness: 9999.0,
  },
  [VastuBlockId.ASHOKA_MONUMENT]: {
    id: VastuBlockId.ASHOKA_MONUMENT,
    name: 'Ashoka Sthambha Pillar',
    devanagari: 'अशोक स्तम्भ',
    solid: true,
    transparent: false,
    isMonument: true, // Strict validation: Cannot be destroyed!
    colorHex: 0xc89d66,
    sound: 'stone',
    hardness: 9999.0,
  },
  [VastuBlockId.HARAPPA_MONUMENT]: {
    id: VastuBlockId.HARAPPA_MONUMENT,
    name: 'Ancient Citadel Monument',
    devanagari: 'हड़प्पा स्मारक',
    solid: true,
    transparent: false,
    isMonument: true, // Strict validation: Cannot be destroyed!
    colorHex: 0x8b3a1a,
    sound: 'stone',
    hardness: 9999.0,
  },
};

/**
 * Strict validation rule enforcing indestructibility of Ancient Indian monuments.
 * 
 * @param {number} blockId - Numeric block identifier.
 * @returns {boolean} True if the block is permitted to be mined or altered.
 */
export function canBreakVoxel(blockId) {
  const meta = VASTU_REGISTRY[blockId];
  if (!meta) return false;
  // Strict heritage preservation: Under no circumstances can ancient monuments be destroyed
  if (meta.isMonument === true) {
    console.warn(`[Nirmana Preservation] Denied attempt to destroy Ancient Heritage: ${meta.name} (${meta.devanagari})`);
    return false;
  }
  return meta.solid;
}
