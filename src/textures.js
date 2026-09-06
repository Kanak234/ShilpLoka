/**
 * ============================================================================
 * PROCEDURAL PIXEL-ART TEXTURE & ATLAS GENERATOR
 * ============================================================================
 * Generates high-fidelity 16x16 pixel textures for all block faces and items
 * dynamically onto an HTML5 Canvas. Produces a unified texture atlas for
 * optimal WebGL batching with nearest-neighbor point sampling.
 */

import * as THREE from 'three';
import { BLOCKS, ITEMS } from './constants.js';

// Size of individual block texture in atlas
export const TEXTURE_SIZE = 16;
export const ATLAS_GRID_SIZE = 16; // 16x16 tiles = 256x256 pixels
export const ATLAS_PIXEL_SIZE = TEXTURE_SIZE * ATLAS_GRID_SIZE; // 256px

// Map of texture key -> tile index in atlas
export const TILE_INDICES = {
  grass_top: 0,
  grass_side: 1,
  dirt: 2,
  stone: 3,
  cobblestone: 4,
  oak_log_side: 5,
  oak_log_top: 6,
  oak_leaves: 7,
  oak_planks: 8,
  crafting_table_top: 9,
  crafting_table_side: 10,
  crafting_table_front: 11,
  sand: 12,
  water: 13,
  coal_ore: 14,
  iron_ore: 15,
  diamond_ore: 16,
  glass: 17,
  bedrock: 18,
  torch: 19,
};

// Map each block face (top, bottom, side) to texture keys
export const BLOCK_FACES = {
  [BLOCKS.GRASS]: {
    top: 'grass_top',
    bottom: 'dirt',
    side: 'grass_side',
  },
  [BLOCKS.DIRT]: {
    top: 'dirt',
    bottom: 'dirt',
    side: 'dirt',
  },
  [BLOCKS.STONE]: {
    top: 'stone',
    bottom: 'stone',
    side: 'stone',
  },
  [BLOCKS.COBBLESTONE]: {
    top: 'cobblestone',
    bottom: 'cobblestone',
    side: 'cobblestone',
  },
  [BLOCKS.OAK_LOG]: {
    top: 'oak_log_top',
    bottom: 'oak_log_top',
    side: 'oak_log_side',
  },
  [BLOCKS.OAK_LEAVES]: {
    top: 'oak_leaves',
    bottom: 'oak_leaves',
    side: 'oak_leaves',
  },
  [BLOCKS.OAK_PLANKS]: {
    top: 'oak_planks',
    bottom: 'oak_planks',
    side: 'oak_planks',
  },
  [BLOCKS.CRAFTING_TABLE]: {
    top: 'crafting_table_top',
    bottom: 'oak_planks',
    side: 'crafting_table_side',
    north: 'crafting_table_front',
  },
  [BLOCKS.SAND]: {
    top: 'sand',
    bottom: 'sand',
    side: 'sand',
  },
  [BLOCKS.WATER]: {
    top: 'water',
    bottom: 'water',
    side: 'water',
  },
  [BLOCKS.COAL_ORE]: {
    top: 'coal_ore',
    bottom: 'coal_ore',
    side: 'coal_ore',
  },
  [BLOCKS.IRON_ORE]: {
    top: 'iron_ore',
    bottom: 'iron_ore',
    side: 'iron_ore',
  },
  [BLOCKS.DIAMOND_ORE]: {
    top: 'diamond_ore',
    bottom: 'diamond_ore',
    side: 'diamond_ore',
  },
  [BLOCKS.GLASS]: {
    top: 'glass',
    bottom: 'glass',
    side: 'glass',
  },
  [BLOCKS.BEDROCK]: {
    top: 'bedrock',
    bottom: 'bedrock',
    side: 'bedrock',
  },
  [BLOCKS.TORCH]: {
    top: 'torch',
    bottom: 'torch',
    side: 'torch',
  },
};

/**
 * Procedural Pixel Art Helper
 */
function createNoisePixelData(w, h, baseR, baseG, baseB, noiseAmp) {
  const data = new Uint8ClampedArray(w * h * 4);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const idx = (y * w + x) * 4;
      const n = (Math.random() - 0.5) * 2 * noiseAmp;
      data[idx + 0] = Math.max(0, Math.min(255, baseR + n));
      data[idx + 1] = Math.max(0, Math.min(255, baseG + n));
      data[idx + 2] = Math.max(0, Math.min(255, baseB + n));
      data[idx + 3] = 255;
    }
  }
  return data;
}

/**
 * Draws specific authentic voxel textures on 16x16 canvas contexts
 */
const TILE_PAINTERS = {
  dirt(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const n = Math.floor((Math.sin(x * 3.7 + y * 5.2) + Math.cos(x * 12.1 - y * 4.3)) * 14);
        d[i + 0] = Math.max(0, Math.min(255, 120 + n)); // R
        d[i + 1] = Math.max(0, Math.min(255, 82 + n));  // G
        d[i + 2] = Math.max(0, Math.min(255, 52 + n));  // B
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  grass_top(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const n = Math.floor((Math.sin(x * 7.1 + y * 9.3) + Math.cos(x * 4.4 - y * 8.1)) * 18);
        d[i + 0] = Math.max(0, Math.min(255, 85 + n));  // R
        d[i + 1] = Math.max(0, Math.min(255, 168 + n)); // G
        d[i + 2] = Math.max(0, Math.min(255, 52 + n));  // B
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  grass_side(ctx) {
    // Dirt base
    TILE_PAINTERS.dirt(ctx);
    const imgData = ctx.getImageData(0, 0, 16, 16);
    const d = imgData.data;
    // Overlap lush green grass blades on top with hanging fringes
    const fringes = [3, 4, 3, 5, 4, 3, 4, 5, 3, 4, 5, 4, 3, 4, 3, 4];
    for (let x = 0; x < 16; x++) {
      const depth = fringes[x];
      for (let y = 0; y < depth; y++) {
        const i = (y * 16 + x) * 4;
        const n = Math.floor((Math.sin(x * 5.1 + y * 4.2)) * 16);
        d[i + 0] = Math.max(0, Math.min(255, 85 + n));
        d[i + 1] = Math.max(0, Math.min(255, 168 + n));
        d[i + 2] = Math.max(0, Math.min(255, 52 + n));
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  stone(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const n = Math.floor((Math.sin(x * 6.3 + y * 3.1) + Math.cos(x * 2.1 - y * 7.7)) * 16);
        const shade = Math.max(0, Math.min(255, 128 + n));
        d[i + 0] = shade;
        d[i + 1] = shade;
        d[i + 2] = shade;
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  cobblestone(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        // Irregular mortar grid
        const isMortar = (x % 5 === 0 && (y % 4 === 0 || y % 7 === 0)) ||
                         (y % 6 === 0) ||
                         ((x + y) % 9 === 0);
        let val = isMortar ? 70 : 135;
        const n = Math.floor(Math.sin(x * 11 + y * 13) * 20);
        val = Math.max(0, Math.min(255, val + n));
        d[i + 0] = val;
        d[i + 1] = val;
        d[i + 2] = val;
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  oak_log_side(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        // Vertical bark grooves
        const groove = (x % 4 === 0) ? -28 : (x % 2 === 0 ? 12 : 0);
        const n = Math.floor(Math.sin(y * 8.0 + x * 2.0) * 10);
        const r = Math.max(0, Math.min(255, 107 + groove + n));
        const g = Math.max(0, Math.min(255, 84 + groove + n));
        const b = Math.max(0, Math.min(255, 51 + groove + n));
        d[i + 0] = r;
        d[i + 1] = g;
        d[i + 2] = b;
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  oak_log_top(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    const cx = 7.5, cy = 7.5;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const dist = Math.sqrt((x - cx) ** 2 + (y - cy) ** 2);
        if (dist > 6.8) {
          // Outer bark ring
          d[i + 0] = 95; d[i + 1] = 75; d[i + 2] = 45;
        } else {
          // Inner tree rings
          const ring = Math.floor(dist * 2.5) % 2 === 0 ? -15 : 15;
          d[i + 0] = Math.max(0, Math.min(255, 185 + ring));
          d[i + 1] = Math.max(0, Math.min(255, 150 + ring));
          d[i + 2] = Math.max(0, Math.min(255, 95 + ring));
        }
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  oak_leaves(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        // Pixel cutouts for authentic leaf canopy transparency
        const cutout = (x * 7 + y * 13) % 11 === 0;
        if (cutout) {
          d[i + 3] = 0;
        } else {
          const n = Math.floor(Math.sin(x * 12 + y * 9) * 22);
          d[i + 0] = Math.max(0, Math.min(255, 45 + n));
          d[i + 1] = Math.max(0, Math.min(255, 130 + n));
          d[i + 2] = Math.max(0, Math.min(255, 35 + n));
          d[i + 3] = 255;
        }
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  oak_planks(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        // 4 horizontal wood slats (lines at y=3, 7, 11, 15)
        const isSlatBorder = (y % 4 === 3);
        const nail = ((x === 2 || x === 13) && (y % 4 === 1));
        let r = 180, g = 135, b = 82;
        if (isSlatBorder) {
          r = 110; g = 80; b = 45;
        } else if (nail) {
          r = 80; g = 60; b = 40;
        } else {
          const n = Math.floor(Math.sin(x * 4 + y * 2) * 12);
          r += n; g += n; b += n;
        }
        d[i + 0] = Math.max(0, Math.min(255, r));
        d[i + 1] = Math.max(0, Math.min(255, g));
        d[i + 2] = Math.max(0, Math.min(255, b));
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  crafting_table_top(ctx) {
    TILE_PAINTERS.oak_planks(ctx);
    // Draw 3x3 crafting grid border in center
    ctx.strokeStyle = '#5a3d1e';
    ctx.lineWidth = 1;
    ctx.strokeRect(2.5, 2.5, 11, 11);
    ctx.beginPath();
    ctx.moveTo(6.5, 2.5); ctx.lineTo(6.5, 13.5);
    ctx.moveTo(10.5, 2.5); ctx.lineTo(10.5, 13.5);
    ctx.moveTo(2.5, 6.5); ctx.lineTo(13.5, 6.5);
    ctx.moveTo(2.5, 10.5); ctx.lineTo(13.5, 10.5);
    ctx.stroke();
    // Inner tool icons
    ctx.fillStyle = '#8b5a2b';
    ctx.fillRect(4, 4, 1, 1);
    ctx.fillRect(8, 8, 1, 1);
    ctx.fillRect(12, 12, 1, 1);
  },

  crafting_table_side(ctx) {
    TILE_PAINTERS.oak_planks(ctx);
    // Saw & hammer silhouette
    ctx.fillStyle = '#4a2f13';
    ctx.fillRect(2, 2, 12, 2);
    ctx.fillStyle = '#a6adb5';
    // Saw blade
    ctx.fillRect(4, 6, 8, 2);
    ctx.fillRect(5, 8, 1, 1);
    ctx.fillRect(7, 8, 1, 1);
    ctx.fillRect(9, 8, 1, 1);
    ctx.fillRect(11, 8, 1, 1);
  },

  crafting_table_front(ctx) {
    TILE_PAINTERS.crafting_table_side(ctx);
    // Extra pair of scissors / pliers
    ctx.fillStyle = '#c0392b';
    ctx.fillRect(3, 11, 3, 2);
    ctx.fillStyle = '#7f8c8d';
    ctx.fillRect(6, 12, 4, 1);
  },

  sand(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const n = Math.floor(Math.sin(x * 14.2 + y * 17.5) * 14);
        d[i + 0] = Math.max(0, Math.min(255, 222 + n));
        d[i + 1] = Math.max(0, Math.min(255, 205 + n));
        d[i + 2] = Math.max(0, Math.min(255, 145 + n));
        d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  water(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const wave = Math.floor(Math.sin(x * 0.8 + y * 0.6) * 20);
        d[i + 0] = Math.max(0, Math.min(255, 45 + wave));
        d[i + 1] = Math.max(0, Math.min(255, 95 + wave));
        d[i + 2] = Math.max(0, Math.min(255, 225 + wave));
        d[i + 3] = 190; // Semi-transparent
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  coal_ore(ctx) {
    TILE_PAINTERS.stone(ctx);
    const imgData = ctx.getImageData(0, 0, 16, 16);
    const d = imgData.data;
    // Coal chunks
    const oreSpots = [
      [3, 3], [4, 3], [3, 4], [4, 4], [5, 4],
      [9, 8], [10, 8], [10, 9], [11, 9],
      [6, 12], [7, 12], [6, 13], [7, 13], [8, 13],
    ];
    for (const [ox, oy] of oreSpots) {
      const i = (oy * 16 + ox) * 4;
      d[i + 0] = 25; d[i + 1] = 25; d[i + 2] = 25;
    }
    ctx.putImageData(imgData, 0, 0);
  },

  iron_ore(ctx) {
    TILE_PAINTERS.stone(ctx);
    const imgData = ctx.getImageData(0, 0, 16, 16);
    const d = imgData.data;
    // Peach-tan iron vein deposits
    const oreSpots = [
      [4, 4], [5, 4], [5, 5], [6, 5],
      [10, 6], [11, 6], [10, 7], [11, 7],
      [3, 11], [4, 11], [4, 12], [8, 12], [9, 12], [9, 13],
    ];
    for (const [ox, oy] of oreSpots) {
      const i = (oy * 16 + ox) * 4;
      d[i + 0] = 216; d[i + 1] = 175; d[i + 2] = 147;
    }
    ctx.putImageData(imgData, 0, 0);
  },

  diamond_ore(ctx) {
    TILE_PAINTERS.stone(ctx);
    const imgData = ctx.getImageData(0, 0, 16, 16);
    const d = imgData.data;
    // Glowing cyan diamond crystal flecks
    const oreSpots = [
      [3, 4], [4, 4], [4, 5], [5, 5],
      [10, 3], [11, 3], [11, 4],
      [7, 9], [8, 9], [8, 10], [9, 10],
      [4, 12], [5, 12], [12, 12], [13, 12],
    ];
    for (const [ox, oy] of oreSpots) {
      const i = (oy * 16 + ox) * 4;
      d[i + 0] = 46; d[i + 1] = 236; d[i + 2] = 226;
    }
    ctx.putImageData(imgData, 0, 0);
  },

  glass(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const isBorder = (x === 0 || x === 15 || y === 0 || y === 15);
        const isGlare = (x === y && x >= 3 && x <= 6) || (x === y + 4 && x >= 8 && x <= 11);
        if (isBorder) {
          d[i + 0] = 230; d[i + 1] = 240; d[i + 2] = 255; d[i + 3] = 240;
        } else if (isGlare) {
          d[i + 0] = 255; d[i + 1] = 255; d[i + 2] = 255; d[i + 3] = 220;
        } else {
          d[i + 0] = 200; d[i + 1] = 225; d[i + 2] = 255; d[i + 3] = 40;
        }
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  bedrock(ctx) {
    const imgData = ctx.createImageData(16, 16);
    const d = imgData.data;
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const i = (y * 16 + x) * 4;
        const n = (Math.sin(x * 19.1 + y * 23.4) * 0.5 + 0.5);
        const val = n > 0.6 ? 75 : (n > 0.3 ? 40 : 15);
        d[i + 0] = val; d[i + 1] = val; d[i + 2] = val; d[i + 3] = 255;
      }
    }
    ctx.putImageData(imgData, 0, 0);
  },

  torch(ctx) {
    // Clear transparent background
    ctx.clearRect(0, 0, 16, 16);
    // Wooden shaft
    ctx.fillStyle = '#7a5127';
    ctx.fillRect(7, 6, 2, 10);
    // Ember top
    ctx.fillStyle = '#4a2f13';
    ctx.fillRect(7, 4, 2, 2);
    // Flame core
    ctx.fillStyle = '#f39c12';
    ctx.fillRect(6, 2, 4, 3);
    ctx.fillStyle = '#f1c40f';
    ctx.fillRect(7, 1, 2, 3);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(7, 2, 2, 1);
  },
};

/**
 * Generates the unified Texture Atlas Canvas and Three.js CanvasTexture.
 */
export class TextureManager {
  constructor() {
    this.canvas = document.createElement('canvas');
    this.canvas.width = ATLAS_PIXEL_SIZE;
    this.canvas.height = ATLAS_PIXEL_SIZE;
    this.ctx = this.canvas.getContext('2d', { willReadFrequently: true });
    this.ctx.imageSmoothingEnabled = false;

    // Build the atlas
    this.buildAtlas();

    // Create Three.js texture
    this.texture = new THREE.CanvasTexture(this.canvas);
    this.texture.magFilter = THREE.NearestFilter;
    this.texture.minFilter = THREE.NearestMipmapNearestFilter;
    this.texture.generateMipmaps = true;
    this.texture.colorSpace = THREE.SRGBColorSpace;

    // Cache item icon data URLs for inventory & hotbar UI
    this.iconCache = {};
    this.buildItemIcons();
  }

  buildAtlas() {
    const tileCanvas = document.createElement('canvas');
    tileCanvas.width = TEXTURE_SIZE;
    tileCanvas.height = TEXTURE_SIZE;
    const tileCtx = tileCanvas.getContext('2d', { willReadFrequently: true });
    tileCtx.imageSmoothingEnabled = false;

    for (const [key, index] of Object.entries(TILE_INDICES)) {
      tileCtx.clearRect(0, 0, TEXTURE_SIZE, TEXTURE_SIZE);
      if (TILE_PAINTERS[key]) {
        TILE_PAINTERS[key](tileCtx);
      }

      // Calculate position in atlas grid
      const col = index % ATLAS_GRID_SIZE;
      const row = Math.floor(index / ATLAS_GRID_SIZE);
      const px = col * TEXTURE_SIZE;
      const py = row * TEXTURE_SIZE;

      this.ctx.drawImage(tileCanvas, px, py);
    }
  }

  /**
   * Retrieves UV bounding box for a given tile name in the atlas.
   * Format: [uMin, vMin, uMax, vMax]
   */
  getTileUVs(tileName) {
    const index = TILE_INDICES[tileName] ?? 0;
    const col = index % ATLAS_GRID_SIZE;
    const row = Math.floor(index / ATLAS_GRID_SIZE);

    const uMin = col / ATLAS_GRID_SIZE;
    const uMax = (col + 1) / ATLAS_GRID_SIZE;
    // Three.js V axis is inverted (0 at bottom, 1 at top)
    const vMax = 1.0 - (row / ATLAS_GRID_SIZE);
    const vMin = 1.0 - ((row + 1) / ATLAS_GRID_SIZE);

    return { uMin, vMin, uMax, vMax };
  }

  /**
   * Generates discrete 32x32 pixel art data URLs for inventory / hotbar icons
   */
  buildItemIcons() {
    const createIcon = (drawFn) => {
      const c = document.createElement('canvas');
      c.width = 32;
      c.height = 32;
      const ctx = c.getContext('2d');
      ctx.imageSmoothingEnabled = false;
      drawFn(ctx);
      return c.toDataURL('image/png');
    };

    // Block icons from atlas
    for (const [blockId, info] of Object.entries(BLOCK_FACES)) {
      this.iconCache[blockId] = createIcon((ctx) => {
        const topKey = info.top;
        const sideKey = info.side;
        // Draw isometric voxel mini block
        const tileIdx = TILE_INDICES[topKey] || 0;
        const col = tileIdx % ATLAS_GRID_SIZE;
        const row = Math.floor(tileIdx / ATLAS_GRID_SIZE);
        ctx.drawImage(this.canvas, col * 16, row * 16, 16, 16, 4, 4, 24, 24);
      });
    }

    // Stick
    this.iconCache[ITEMS.STICK] = createIcon((ctx) => {
      ctx.strokeStyle = '#6b4623';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(6, 26);
      ctx.lineTo(26, 6);
      ctx.stroke();
    });

    // Tool painter helper
    const drawTool = (ctx, headColor, stickColor = '#7a5127') => {
      // Stick handle
      ctx.strokeStyle = stickColor;
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(6, 26);
      ctx.lineTo(22, 10);
      ctx.stroke();

      // Pickaxe head
      ctx.strokeStyle = headColor;
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.arc(22, 10, 10, -Math.PI * 0.75, -Math.PI * 0.1);
      ctx.stroke();
    };

    this.iconCache[ITEMS.WOODEN_PICKAXE] = createIcon((ctx) => drawTool(ctx, '#b8860b'));
    this.iconCache[ITEMS.STONE_PICKAXE] = createIcon((ctx) => drawTool(ctx, '#7f8c8d'));
    this.iconCache[ITEMS.IRON_PICKAXE] = createIcon((ctx) => drawTool(ctx, '#ecf0f1'));
    this.iconCache[ITEMS.DIAMOND_PICKAXE] = createIcon((ctx) => drawTool(ctx, '#3498db'));

    // Wooden Axe
    this.iconCache[ITEMS.WOODEN_AXE] = createIcon((ctx) => {
      ctx.strokeStyle = '#7a5127';
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(6, 26); ctx.lineTo(20, 10); ctx.stroke();
      ctx.fillStyle = '#b8860b';
      ctx.fillRect(16, 6, 8, 8);
    });

    // Wooden Shovel
    this.iconCache[ITEMS.WOODEN_SHOVEL] = createIcon((ctx) => {
      ctx.strokeStyle = '#7a5127';
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(6, 26); ctx.lineTo(20, 12); ctx.stroke();
      ctx.fillStyle = '#b8860b';
      ctx.fillRect(18, 6, 6, 6);
    });

    // Wooden Sword
    this.iconCache[ITEMS.WOODEN_SWORD] = createIcon((ctx) => {
      ctx.strokeStyle = '#7a5127';
      ctx.lineWidth = 3;
      ctx.beginPath(); ctx.moveTo(6, 26); ctx.lineTo(10, 22); ctx.stroke();
      ctx.strokeStyle = '#b8860b';
      ctx.lineWidth = 4;
      ctx.beginPath(); ctx.moveTo(11, 21); ctx.lineTo(26, 6); ctx.stroke();
    });

    // Coal item
    this.iconCache[ITEMS.COAL] = createIcon((ctx) => {
      ctx.fillStyle = '#222222';
      ctx.beginPath();
      ctx.ellipse(16, 16, 8, 7, 0.4, 0, Math.PI * 2);
      ctx.fill();
    });

    // Diamond item
    this.iconCache[ITEMS.DIAMOND] = createIcon((ctx) => {
      ctx.fillStyle = '#2de0d4';
      ctx.beginPath();
      ctx.moveTo(16, 6);
      ctx.lineTo(25, 14);
      ctx.lineTo(16, 26);
      ctx.lineTo(7, 14);
      ctx.closePath();
      ctx.fill();
    });

    // Iron Ingot
    this.iconCache[ITEMS.IRON_INGOT] = createIcon((ctx) => {
      ctx.fillStyle = '#e0e0e0';
      ctx.fillRect(8, 11, 16, 10);
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(9, 12, 14, 2);
    });
  }

  getItemIconUrl(itemId) {
    return this.iconCache[itemId] || '';
  }
}
