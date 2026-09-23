# ADR 005: Concurrency, Threading Model & Fixed-Step Simulation Loop

* **Status**: Proposed (Awaiting Kanak approval)
* **Date**: 2026-09-23
* **Deciders**: Lead Engineer, Kanak Prabhakar

## Context & Problem Statement
Coupling game simulation (physics, entity AI, fluid spreading, light updates) to the display refresh rate leads to non-deterministic physics, jittery movement on varying frame rates, and render thread hitches during heavy disk or generation workloads. Furthermore, architectural preparation for potential future multiplayer (v2) requires a clear separation between client state and server simulation.

## Decision
1. **Decoupled Simulation & Render Loops**:
   - **Simulation Tick Loop**: Fixed 20 ticks per second (50ms delta per tick). Runs physics, AI behaviors, crop growth, block ticking, and entity pathfinding.
   - **Render Loop**: Variable frame rate (VSync enabled or capped at 60/120/144/Unlimited). Interpolates entity positions between previous tick and current tick using fractional alpha:
     $$\mathbf{P}_{\text{render}} = (1 - \alpha)\mathbf{P}_{\text{prev}} + \alpha\mathbf{P}_{\text{curr}}$$
2. **Internal Client-Server Architecture**:
   - Even in single-player, the core world logic executes behind an internal server interface.
   - The client consumes snapshots of entity states and chunk mesh updates via lock-free queues (`ConcurrentLinkedQueue` / RingBuffers).
   - This ensures LAN multiplayer can be introduced in v2 without refactoring gameplay logic.
3. **Thread Segregation**:
   - **Main Thread (Render & Window)**: Owns GLFW window, receives input events, compiles shaders, uploads vertex buffers to GPU, executes OpenGL draw calls. *Only this thread touches OpenGL.*
   - **Tick Thread (Integrated Server)**: Runs the 20 TPS simulation loop.
   - **Worker Thread Pool (`ForkJoinPool` / Fixed Worker Pool)**: Performs asynchronous OpenSimplex2 noise generation, cave carvers, structure placement, and greedy mesh generation into raw byte buffers.
   - **I/O Thread**: Handles asynchronous disk reads and writes for region files and player data.

### Consequences
- Fluid, hitch-free 60 FPS visuals even when background chunks are generating or saving.
- Deterministic gameplay mechanics regardless of display hardware.
