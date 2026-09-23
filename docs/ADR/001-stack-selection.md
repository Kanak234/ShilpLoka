# ADR 001: Game Stack & Engine Architecture Selection

* **Status**: Proposed (Awaiting Kanak approval on Q2)
* **Date**: 2026-09-23
* **Deciders**: Lead Engineer, Kanak Prabhakar

## Context & Problem Statement
ShilpLoka (Java Edition) is a complete rewrite of the original web/Three.js voxel sandbox game. We need to decide whether to build on a third-party framework (such as libGDX or jMonkeyEngine) or develop a dedicated custom voxel engine utilizing Java 21 LTS and LWJGL 3 (Lightweight Java Game Library).

## Decision Drivers
1. **Low-Level Memory & Voxel Optimization**: Voxel games require custom memory management: 16³ chunk section storage, palette compression, bit-packing, and direct NIO buffer manipulation.
2. **Greedy Meshing & 2D Texture Array**: Rendering thousands of voxel faces requires greedy meshing algorithms and OpenGL 3.3 Core features like `GL_TEXTURE_2D_ARRAY` to eliminate mipmap edge bleeding.
3. **Deterministic Threading**: Decoupling 20 TPS simulation logic from uncapped/VSynced rendering without framework overhead.
4. **Pedagogical and Explanatory Quality**: Kanak Prabhakar must be able to present, explain, and maintain every single subsystem of the engine.

## Considered Options
1. **Option A**: Custom Voxel Engine built on **Java 21 LTS + LWJGL 3** (GLFW, OpenGL 3.3 Core, OpenAL, STB) + **JOML**.
2. **Option B**: **libGDX** framework.
3. **Option C**: **jMonkeyEngine 3**.

## Decision Outcome
Chosen **Option A (Custom Engine on LWJGL 3 + JOML)**.

### Positive Consequences
- **Zero Framework Bloat**: No unused scene-graph layers, high-level physics abstractions, or unwanted dependencies.
- **Full Voxel Pipeline Control**: Direct access to OpenGL 3.3 Core buffer objects (VAO, VBO, IBO), shader compilation, texture arrays, and persistent buffer mapping.
- **Memory Footprint**: Target < 2 GB heap at render distance 8 is achievable through tailored palette compression and primitive array memory layouts.
- **Portability & Packaging**: Clean modular Java structure easily packed via `jlink` and `jpackage` without external framework runtime conflicts.

### Negative Consequences / Risks
- Boilerplate setup for window initialization, input handling, and OpenGL context creation.
- GUI layout and font rendering must be built from scratch using STB TrueType / textured quads.
- Strict discipline required to prevent allocation in hot loops.
