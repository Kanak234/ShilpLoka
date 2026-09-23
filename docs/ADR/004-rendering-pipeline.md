# ADR 004: Voxel Rendering Architecture (Greedy Meshing & 2D Texture Array)

* **Status**: Proposed (Awaiting Kanak approval)
* **Date**: 2026-09-23
* **Deciders**: Lead Engineer, Kanak Prabhakar

## Context & Problem Statement
In a voxel sandbox with a world height of 0–320 and render distance 8–16, rendering individual cube faces creates millions of vertices, saturating memory bandwidth and vertex shader stages. Additionally, texture atlases suffer from texture bleeding at mipmap boundaries (neighboring block pixels bleeding onto edges).

## Decision
1. **Section Greedy Meshing**:
   - Each chunk column (16×16×320) is split into twenty 16³ sections.
   - For each section, a greedy meshing algorithm merges adjacent coplanar block faces with identical texture IDs, light values, and normal directions into single large quads.
   - Reduces vertex and triangle count by 60%–85% compared to naive cubic meshing.
2. **2D Texture Array (`GL_TEXTURE_2D_ARRAY`)**:
   - Instead of a monolithic 2D texture atlas, all 16×16 block textures are loaded as separate 2D layers in a single 2D Texture Array.
   - Eliminates edge bleeding artifacts across all mipmap levels completely.
   - Layer index is passed directly to the fragment shader via vertex attributes.
3. **Multi-Pass Rendering**:
   - **Pass 1 (Opaque)**: Granite, sandstone, dirt, bricks. Depth writing enabled, back-face culling enabled.
   - **Pass 2 (Cutout)**: Foliage, leaves, crops, jaali panels. Alpha-to-coverage / discard testing enabled, depth writing enabled.
   - **Pass 3 (Translucent)**: Water, tinted glass, ice. Rendered back-to-front with depth writing disabled, blending enabled (`GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA`).
4. **Vertex Packing**:
   - Pack vertex attributes (position, normal, UV, light, texture layer, AO) into compact 32-bit integer structures to optimize GPU cache locality.

### Consequences
- High frame rates (60+ FPS on RTX 3050 and 30+ FPS on integrated AMD Radeon graphics).
- Clean visual fidelity with crisp pixel-art textures and zero bleeding.
