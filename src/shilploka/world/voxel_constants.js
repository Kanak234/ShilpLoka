/**
 * @fileoverview ShilpLoka Voxel Constants & Ancient Indian Material Registry
 * @module shilploka/world/voxel_constants
 * 
 * Scalable clean-room block definitions rooted in the Ancient Indian subcontinent
 * civilization (Harappa, Mohenjo-Daro, Lothal, Pataliputra).
 * Includes strict low-level invariant validation for indestructible heritage monuments.
 */

export const ShilpBlockId = {
  AIR: 0,
  SINDHU_ALLUVIAL_SOIL: 1,
  SINDHU_GRASS: 2,
  HARAPPAN_BAKED_BRICK: 3,
  DECCAN_BASALT: 4,
  CHUNAR_SANDSTONE: 5,
  KANSA_BRONZE_ORE: 6,
  LAJWARD_LAPIS: 7,
  BANYAN_WOOD: 8,
  BANYAN_LEAVES: 9,
  BANYAN_PROP_ROOT: 10,
  PEEPAL_WOOD: 11,
  PEEPAL_LEAVES: 12,
  SACRED_WATER: 13,
  ASHOKA_PILLAR_BLOCK: 14,
  MOHENJO_GREAT_BATH_BLOCK: 15,
  BITUMEN_MORTAR: 16,
};

/**
 * Metadata registry for all voxel materials.
 */
export const SHILP_BLOCK_REGISTRY = {
  [ShilpBlockId.AIR]: {
    id: ShilpBlockId.AIR,
    name: 'Air (आकाश)',
    solid: false,
    transparent: true,
    isHeritage: false,
    colorHex: 0x000000,
  },
  [ShilpBlockId.SINDHU_ALLUVIAL_SOIL]: {
    id: ShilpBlockId.SINDHU_ALLUVIAL_SOIL,
    name: 'Sindhu Alluvial Loam (सिन्धु कछार मृत्तिका)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x6d4c41,
  },
  [ShilpBlockId.SINDHU_GRASS]: {
    id: ShilpBlockId.SINDHU_GRASS,
    name: 'Sindhu Riverbank Grass (सिन्धु हरित दूर्वा)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x4a7c59,
    topColorHex: 0x5a9367,
  },
  [ShilpBlockId.HARAPPAN_BAKED_BRICK]: {
    id: ShilpBlockId.HARAPPAN_BAKED_BRICK,
    name: 'Harappan Baked Brick (पक्व इष्टिका)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0xb85d3f,
  },
  [ShilpBlockId.DECCAN_BASALT]: {
    id: ShilpBlockId.DECCAN_BASALT,
    name: 'Deccan Basalt Stone (दक्कन श्याम शैल)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x3e424b,
  },
  [ShilpBlockId.CHUNAR_SANDSTONE]: {
    id: ShilpBlockId.CHUNAR_SANDSTONE,
    name: 'Chunar Buff Sandstone (चुनार पीत पाषाण)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0xd4a373,
  },
  [ShilpBlockId.KANSA_BRONZE_ORE]: {
    id: ShilpBlockId.KANSA_BRONZE_ORE,
    name: 'Kansa Bronze Ore (कांस्य खनिज)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0xcd7f32,
  },
  [ShilpBlockId.LAJWARD_LAPIS]: {
    id: ShilpBlockId.LAJWARD_LAPIS,
    name: 'Badakhshan Lajward (लाजवर्द - Lapis Lazuli)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x1a4b8c,
  },
  [ShilpBlockId.BANYAN_WOOD]: {
    id: ShilpBlockId.BANYAN_WOOD,
    name: 'Sacred Banyan Trunk (वटवृक्ष काष्ठ)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x543d2b,
  },
  [ShilpBlockId.BANYAN_LEAVES]: {
    id: ShilpBlockId.BANYAN_LEAVES,
    name: 'Sacred Banyan Foliage (वट पर्ण)',
    solid: true,
    transparent: true,
    isHeritage: false,
    colorHex: 0x2d6a4f,
  },
  [ShilpBlockId.BANYAN_PROP_ROOT]: {
    id: ShilpBlockId.BANYAN_PROP_ROOT,
    name: 'Banyan Aerial Prop Root (वट जटा स्तम्भ)',
    solid: true,
    transparent: true,
    isHeritage: false,
    colorHex: 0x774936,
  },
  [ShilpBlockId.PEEPAL_WOOD]: {
    id: ShilpBlockId.PEEPAL_WOOD,
    name: 'Sacred Peepal Wood (अश्वत्थ काष्ठ)',
    solid: true,
    transparent: false,
    isHeritage: false,
    colorHex: 0x605043,
  },
  [ShilpBlockId.PEEPAL_LEAVES]: {
    id: ShilpBlockId.PEEPAL_LEAVES,
    name: 'Peepal Sacred Leaves (अश्वत्थ पत्र)',
    solid: true,
    transparent: true,
    isHeritage: false,
    colorHex: 0x52796f,
  },
  [ShilpBlockId.SACRED_WATER]: {
    id: ShilpBlockId.SACRED_WATER,
    name: 'Sindhu Sacred River Water (सिन्धु पावन जल)',
    solid: false,
    transparent: true,
    isHeritage: false,
    colorHex: 0x2b6cb0,
  },
  [ShilpBlockId.ASHOKA_PILLAR_BLOCK]: {
    id: ShilpBlockId.ASHOKA_PILLAR_BLOCK,
    name: 'Ashoka Sthambha Pillar (अशोक स्तम्भ - अक्षत धरोहर)',
    solid: true,
    transparent: false,
    isHeritage: true, // STRICT LOW-LEVEL INVARIANT: INDESTRUCTIBLE
    colorHex: 0xe0a96d,
  },
  [ShilpBlockId.MOHENJO_GREAT_BATH_BLOCK]: {
    id: ShilpBlockId.MOHENJO_GREAT_BATH_BLOCK,
    name: 'Great Bath Baked Masonry (सिन्धु महास्नानागार)',
    solid: true,
    transparent: false,
    isHeritage: true, // STRICT LOW-LEVEL INVARIANT: INDESTRUCTIBLE
    colorHex: 0xa8422b,
  },
  [ShilpBlockId.BITUMEN_MORTAR]: {
    id: ShilpBlockId.BITUMEN_MORTAR,
    name: 'Ancient Waterproof Bitumen (शिलाजतु संधानक)',
    solid: true,
    transparent: false,
    // NOT heritage (was true). WHY: bitumen is a building material the player
    // carries (16 in the starting hotbar) and places, like baked brick - it is
    // not a monument. As heritage it could be placed but never removed, so one
    // misplaced click was permanent. World generation never places bitumen,
    // so no monument loses protection: the Ashoka pillar and Great Bath stay
    // indestructible. USED BY: canBreakVoxel() below, which now applies the
    // ordinary rule (solid blocks can be mined); the mined block goes back
    // into the inventory through ShilpEngine.tryMineTargetVoxel().
    isHeritage: false,
    colorHex: 0x1f1f1f,
  },
};

/**
 * Strict low-level validation gate enforcing heritage monument indestructibility.
 * 
 * @param {number} blockId - Candidate voxel block ID.
 * @returns {boolean} True if the block can be destroyed; false if prohibited by heritage invariant.
 */
export function canBreakVoxel(blockId) {
  const meta = SHILP_BLOCK_REGISTRY[blockId];
  if (!meta) return false;
  if (meta.isHeritage === true) {
    console.warn(`[ShilpLoka Heritage Invariant] Denied break attempt on indestructible monument: ${meta.name}`);
    return false;
  }
  return meta.solid;
}
