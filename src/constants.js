/**
 * ============================================================================
 * CONSTANTS & CONFIGURATION
 * ============================================================================
 * Defines global block types, item definitions, world generation settings,
 * physics constants, and the complete crafting recipe database.
 */

export const CHUNK_SIZE_X = 16;
export const CHUNK_SIZE_Y = 64;
export const CHUNK_SIZE_Z = 16;
export const SEA_LEVEL = 22;
export const VIEW_DISTANCE = 2; // Radius in chunks loaded around player (5x5 area = 21 chunks for silky smooth 60 FPS)

// Block Type Identifiers
export const BLOCKS = {
  AIR: 0,
  GRASS: 1,
  DIRT: 2,
  STONE: 3,
  COBBLESTONE: 4,
  OAK_LOG: 5,
  OAK_LEAVES: 6,
  OAK_PLANKS: 7,
  CRAFTING_TABLE: 8,
  SAND: 9,
  WATER: 10,
  COAL_ORE: 11,
  IRON_ORE: 12,
  DIAMOND_ORE: 13,
  GLASS: 14,
  BEDROCK: 15,
  TORCH: 16,
};

// Item Type Identifiers (Craftable tools and materials)
export const ITEMS = {
  STICK: 100,
  WOODEN_PICKAXE: 101,
  WOODEN_AXE: 102,
  WOODEN_SHOVEL: 103,
  WOODEN_SWORD: 104,
  STONE_PICKAXE: 105,
  IRON_INGOT: 106,
  IRON_PICKAXE: 107,
  DIAMOND: 108,
  DIAMOND_PICKAXE: 109,
  COAL: 110,
};

// Full Registry metadata for UI rendering, naming, tools, and drops
export const REGISTRY = {
  // Blocks
  [BLOCKS.AIR]: { id: BLOCKS.AIR, name: 'Air', isBlock: false, transparent: true, solid: false },
  [BLOCKS.GRASS]: { id: BLOCKS.GRASS, name: 'Grass Block', isBlock: true, transparent: false, solid: true, drop: BLOCKS.DIRT, sound: 'grass' },
  [BLOCKS.DIRT]: { id: BLOCKS.DIRT, name: 'Dirt', isBlock: true, transparent: false, solid: true, drop: BLOCKS.DIRT, sound: 'dirt' },
  [BLOCKS.STONE]: { id: BLOCKS.STONE, name: 'Stone', isBlock: true, transparent: false, solid: true, drop: BLOCKS.COBBLESTONE, sound: 'stone' },
  [BLOCKS.COBBLESTONE]: { id: BLOCKS.COBBLESTONE, name: 'Cobblestone', isBlock: true, transparent: false, solid: true, drop: BLOCKS.COBBLESTONE, sound: 'stone' },
  [BLOCKS.OAK_LOG]: { id: BLOCKS.OAK_LOG, name: 'Oak Wood', isBlock: true, transparent: false, solid: true, drop: BLOCKS.OAK_LOG, sound: 'wood' },
  [BLOCKS.OAK_LEAVES]: { id: BLOCKS.OAK_LEAVES, name: 'Oak Leaves', isBlock: true, transparent: true, solid: true, drop: BLOCKS.AIR, sound: 'grass' },
  [BLOCKS.OAK_PLANKS]: { id: BLOCKS.OAK_PLANKS, name: 'Oak Planks', isBlock: true, transparent: false, solid: true, drop: BLOCKS.OAK_PLANKS, sound: 'wood' },
  [BLOCKS.CRAFTING_TABLE]: { id: BLOCKS.CRAFTING_TABLE, name: 'Crafting Table', isBlock: true, transparent: false, solid: true, drop: BLOCKS.CRAFTING_TABLE, sound: 'wood', isInteractive: true },
  [BLOCKS.SAND]: { id: BLOCKS.SAND, name: 'Sand', isBlock: true, transparent: false, solid: true, drop: BLOCKS.SAND, sound: 'sand' },
  [BLOCKS.WATER]: { id: BLOCKS.WATER, name: 'Water', isBlock: true, transparent: true, solid: false, drop: BLOCKS.AIR, sound: 'water' },
  [BLOCKS.COAL_ORE]: { id: BLOCKS.COAL_ORE, name: 'Coal Ore', isBlock: true, transparent: false, solid: true, drop: ITEMS.COAL, sound: 'stone' },
  [BLOCKS.IRON_ORE]: { id: BLOCKS.IRON_ORE, name: 'Iron Ore', isBlock: true, transparent: false, solid: true, drop: BLOCKS.IRON_ORE, sound: 'stone' },
  [BLOCKS.DIAMOND_ORE]: { id: BLOCKS.DIAMOND_ORE, name: 'Diamond Ore', isBlock: true, transparent: false, solid: true, drop: ITEMS.DIAMOND, sound: 'stone' },
  [BLOCKS.GLASS]: { id: BLOCKS.GLASS, name: 'Glass', isBlock: true, transparent: true, solid: true, drop: BLOCKS.AIR, sound: 'glass' },
  [BLOCKS.BEDROCK]: { id: BLOCKS.BEDROCK, name: 'Bedrock', isBlock: true, transparent: false, solid: true, indestructible: true, drop: BLOCKS.AIR, sound: 'stone' },
  [BLOCKS.TORCH]: { id: BLOCKS.TORCH, name: 'Torch', isBlock: true, transparent: true, solid: false, drop: BLOCKS.TORCH, sound: 'wood', lightLevel: 14 },

  // Items
  [ITEMS.STICK]: { id: ITEMS.STICK, name: 'Stick', isBlock: false, drop: ITEMS.STICK },
  [ITEMS.WOODEN_PICKAXE]: { id: ITEMS.WOODEN_PICKAXE, name: 'Wooden Pickaxe', isBlock: false, isTool: true, toolType: 'pickaxe', tier: 1 },
  [ITEMS.WOODEN_AXE]: { id: ITEMS.WOODEN_AXE, name: 'Wooden Axe', isBlock: false, isTool: true, toolType: 'axe', tier: 1 },
  [ITEMS.WOODEN_SHOVEL]: { id: ITEMS.WOODEN_SHOVEL, name: 'Wooden Shovel', isBlock: false, isTool: true, toolType: 'shovel', tier: 1 },
  [ITEMS.WOODEN_SWORD]: { id: ITEMS.WOODEN_SWORD, name: 'Wooden Sword', isBlock: false, isTool: true, toolType: 'sword', tier: 1 },
  [ITEMS.STONE_PICKAXE]: { id: ITEMS.STONE_PICKAXE, name: 'Stone Pickaxe', isBlock: false, isTool: true, toolType: 'pickaxe', tier: 2 },
  [ITEMS.IRON_INGOT]: { id: ITEMS.IRON_INGOT, name: 'Iron Ingot', isBlock: false },
  [ITEMS.IRON_PICKAXE]: { id: ITEMS.IRON_PICKAXE, name: 'Iron Pickaxe', isBlock: false, isTool: true, toolType: 'pickaxe', tier: 3 },
  [ITEMS.DIAMOND]: { id: ITEMS.DIAMOND, name: 'Diamond', isBlock: false },
  [ITEMS.DIAMOND_PICKAXE]: { id: ITEMS.DIAMOND_PICKAXE, name: 'Diamond Pickaxe', isBlock: false, isTool: true, toolType: 'pickaxe', tier: 4 },
  [ITEMS.COAL]: { id: ITEMS.COAL, name: 'Coal', isBlock: false },
};

// Physics Constants
export const PHYSICS = {
  GRAVITY: -26.0,
  TERMINAL_VELOCITY: -40.0,
  WALK_SPEED: 4.8,
  SPRINT_SPEED: 7.2,
  JUMP_FORCE: 8.6,
  FLY_SPEED: 12.0,
  PLAYER_WIDTH: 0.6,
  PLAYER_HEIGHT: 1.8,
  EYE_HEIGHT: 1.62,
  REACH_DISTANCE: 6.0,
  MOUSE_SENSITIVITY: 0.0022,
};

// Crafting Recipe Definitions
export const RECIPES = [
  // 1 Oak Log -> 4 Oak Planks (shapeless 1 log)
  {
    name: 'Oak Planks',
    width: 1,
    height: 1,
    pattern: [BLOCKS.OAK_LOG],
    result: { id: BLOCKS.OAK_PLANKS, count: 4 },
  },
  // 2 Oak Planks vertical -> 4 Sticks
  {
    name: 'Sticks',
    width: 1,
    height: 2,
    pattern: [
      BLOCKS.OAK_PLANKS,
      BLOCKS.OAK_PLANKS,
    ],
    result: { id: ITEMS.STICK, count: 4 },
  },
  // 4 Oak Planks (2x2) -> 1 Crafting Table
  {
    name: 'Crafting Table',
    width: 2,
    height: 2,
    pattern: [
      BLOCKS.OAK_PLANKS, BLOCKS.OAK_PLANKS,
      BLOCKS.OAK_PLANKS, BLOCKS.OAK_PLANKS,
    ],
    result: { id: BLOCKS.CRAFTING_TABLE, count: 1 },
  },
  // 1 Coal + 1 Stick vertical -> 4 Torches
  {
    name: 'Torches',
    width: 1,
    height: 2,
    pattern: [
      ITEMS.COAL,
      ITEMS.STICK,
    ],
    result: { id: BLOCKS.TORCH, count: 4 },
  },
  // Wooden Pickaxe (3 Planks horizontal, 2 Sticks vertical down center)
  {
    name: 'Wooden Pickaxe',
    width: 3,
    height: 3,
    pattern: [
      BLOCKS.OAK_PLANKS, BLOCKS.OAK_PLANKS, BLOCKS.OAK_PLANKS,
      0,                 ITEMS.STICK,       0,
      0,                 ITEMS.STICK,       0,
    ],
    result: { id: ITEMS.WOODEN_PICKAXE, count: 1 },
  },
  // Wooden Axe (3 Planks, 2 Sticks)
  {
    name: 'Wooden Axe',
    width: 3,
    height: 3,
    pattern: [
      BLOCKS.OAK_PLANKS, BLOCKS.OAK_PLANKS, 0,
      BLOCKS.OAK_PLANKS, ITEMS.STICK,       0,
      0,                 ITEMS.STICK,       0,
    ],
    result: { id: ITEMS.WOODEN_AXE, count: 1 },
  },
  // Wooden Shovel (1 Plank, 2 Sticks)
  {
    name: 'Wooden Shovel',
    width: 1,
    height: 3,
    pattern: [
      BLOCKS.OAK_PLANKS,
      ITEMS.STICK,
      ITEMS.STICK,
    ],
    result: { id: ITEMS.WOODEN_SHOVEL, count: 1 },
  },
  // Wooden Sword (2 Planks, 1 Stick)
  {
    name: 'Wooden Sword',
    width: 1,
    height: 3,
    pattern: [
      BLOCKS.OAK_PLANKS,
      BLOCKS.OAK_PLANKS,
      ITEMS.STICK,
    ],
    result: { id: ITEMS.WOODEN_SWORD, count: 1 },
  },
  // Stone Pickaxe (3 Cobblestone, 2 Sticks)
  {
    name: 'Stone Pickaxe',
    width: 3,
    height: 3,
    pattern: [
      BLOCKS.COBBLESTONE, BLOCKS.COBBLESTONE, BLOCKS.COBBLESTONE,
      0,                  ITEMS.STICK,        0,
      0,                  ITEMS.STICK,        0,
    ],
    result: { id: ITEMS.STONE_PICKAXE, count: 1 },
  },
  // Diamond Pickaxe (3 Diamonds, 2 Sticks)
  {
    name: 'Diamond Pickaxe',
    width: 3,
    height: 3,
    pattern: [
      ITEMS.DIAMOND, ITEMS.DIAMOND, ITEMS.DIAMOND,
      0,             ITEMS.STICK,   0,
      0,             ITEMS.STICK,   0,
    ],
    result: { id: ITEMS.DIAMOND_PICKAXE, count: 1 },
  },
  // 4 Sand (2x2) -> 4 Glass
  {
    name: 'Glass',
    width: 2,
    height: 2,
    pattern: [
      BLOCKS.SAND, BLOCKS.SAND,
      BLOCKS.SAND, BLOCKS.SAND,
    ],
    result: { id: BLOCKS.GLASS, count: 4 },
  },
];
