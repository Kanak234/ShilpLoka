package shilploka.world.fluid;

import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * WHY:
 * Fluid dynamics bring voxel worlds to life: sacred rivers (Ganga, Yamuna) cascading down
 * Himalayan peaks, water carving desert channels, and volcanic magma in Paataal solidifying
 * into Deccan Basalt upon touching water. Unanchored granular sediments (Thar sand and river bajri)
 * must also obey gravity.
 *
 * WHAT:
 * High-performance cellular automata simulation:
 * - Fluid spreading: downward gravity waterfall cascades and horizontal lateral flow (levels 8 down to 1)
 * - Fluid evaporation: decaying and vanishing when cut off from parent sources
 * - Thermodynamic contact reactions: Water touching Lava source cools into Deccan Basalt;
 *   Water touching flowing Lava cools into Stone
 * - Gravitational falling blocks: Thar Sand and Bajri (gravel) collapsing when unsupported below
 *
 * USED FOR:
 * Updated in Game.update() on a scheduled tick timer (e.g. every 5 ticks / 250ms), and
 * triggered directly when players place or break blocks adjacent to fluids or sand.
 *
 * WHERE NEXT:
 * Triggered by ChunkManager.setBlock(), and rendered by GreedyMesher using animated
 * water/lava texture layers.
 */
public class FluidSimulator {

    public record FluidNode(int x, int y, int z, int blockId, int level) {}

    private final Queue<FluidNode> tickQueue = new ArrayDeque<>();
    private static final int MAX_TICKS_PER_STEP = 512; // Cap per-frame updates to guarantee 60+ FPS

    private static final int[] DX = { 1, -1,  0,  0 };
    private static final int[] DZ = { 0,  0,  1, -1 };

    /**
     * WHY:
     * Enqueues a fluid coordinate to be processed on subsequent simulation ticks.
     */
    public void scheduleFluidUpdate(int x, int y, int z, int blockId, int level) {
        tickQueue.offer(new FluidNode(x, y, z, blockId, level));
    }

    /**
     * WHY:
     * Advances the cellular automata fluid and gravity simulation.
     *
     * @param chunkManager Active world chunk manager for querying and mutating blocks.
     */
    public void tick(ChunkManager chunkManager) {
        int processed = 0;
        int initialSize = tickQueue.size();

        while (!tickQueue.isEmpty() && processed < initialSize && processed < MAX_TICKS_PER_STEP) {
            FluidNode node = tickQueue.poll();
            processed++;

            int x = node.x();
            int y = node.y();
            int z = node.z();
            int fluidId = node.blockId();
            int level = node.level();

            int currentBlock = chunkManager.getBlock(x, y, z);
            if (currentBlock != fluidId && currentBlock != BlockIds.WATER_FLOWING) {
                continue; // Block was replaced or mined away
            }

            // 1. Water-Lava thermodynamic contact reactions
            if (handleFluidInteractions(x, y, z, currentBlock, chunkManager)) {
                continue; // Reacted and solidified into stone/basalt
            }

            // 2. Downward gravity flow
            if (y > 0) {
                int below = chunkManager.getBlock(x, y - 1, z);
                if (below == BlockIds.AIR || below == BlockIds.WATER_FLOWING) {
                    // Flow straight down
                    int flowBlock = (fluidId == BlockIds.LAVA) ? BlockIds.LAVA : BlockIds.WATER_FLOWING;
                    chunkManager.setBlock(x, y - 1, z, flowBlock);
                    tickQueue.offer(new FluidNode(x, y - 1, z, flowBlock, 8));
                    continue; // Gravity takes precedence over horizontal spread
                }
            }

            // 3. Horizontal spreading if level allows (> 1)
            if (level > 1) {
                int nextLevel = level - 1;
                int flowBlock = (fluidId == BlockIds.LAVA) ? BlockIds.LAVA : BlockIds.WATER_FLOWING;

                for (int i = 0; i < 4; i++) {
                    int nx = x + DX[i];
                    int nz = z + DZ[i];
                    int target = chunkManager.getBlock(nx, y, nz);

                    if (target == BlockIds.AIR) {
                        chunkManager.setBlock(nx, y, nz, flowBlock);
                        tickQueue.offer(new FluidNode(nx, y, nz, flowBlock, nextLevel));
                    }
                }
            }
        }
    }

    /**
     * Checks if water and lava are touching, causing basalt/stone solidification.
     */
    private boolean handleFluidInteractions(int x, int y, int z, int currentBlock, ChunkManager chunkManager) {
        boolean isWater = (currentBlock == BlockIds.WATER || currentBlock == BlockIds.WATER_FLOWING);
        boolean isLava = (currentBlock == BlockIds.LAVA);

        if (!isWater && !isLava) return false;

        // Check 6 orthogonal neighbors for opposing fluid
        int[] checkDX = { 1, -1,  0,  0,  0,  0 };
        int[] checkDY = { 0,  0,  1, -1,  0,  0 };
        int[] checkDZ = { 0,  0,  0,  0,  1, -1 };

        for (int i = 0; i < 6; i++) {
            int nx = x + checkDX[i];
            int ny = y + checkDY[i];
            int nz = z + checkDZ[i];

            int neighbor = chunkManager.getBlock(nx, ny, nz);

            if (isWater && neighbor == BlockIds.LAVA) {
                // Water meeting Lava source solidifies Lava into volcanic Deccan Basalt
                chunkManager.setBlock(nx, ny, nz, BlockIds.DECCAN_BASALT, false);
                return true;
            } else if (isLava && (neighbor == BlockIds.WATER || neighbor == BlockIds.WATER_FLOWING)) {
                // Lava meeting water solidifies Lava into volcanic Deccan Basalt
                chunkManager.setBlock(x, y, z, BlockIds.DECCAN_BASALT, false);
                return true;
            }
        }
        return false;
    }

    /**
     * WHY:
     * Checks if a sand or bajri (gravel) block is unsupported below and collapses under gravity.
     *
     * @param x            World X coordinate.
     * @param y            World Y coordinate.
     * @param z            World Z coordinate.
     * @param chunkManager Target chunk manager.
     * @return True if block fell.
     */
    public boolean checkAndApplyFalling(int x, int y, int z, ChunkManager chunkManager) {
        int block = chunkManager.getBlock(x, y, z);
        if (!BlockIds.isFallingBlock(block)) return false;

        int targetY = y;
        while (targetY > 0) {
            int below = chunkManager.getBlock(x, targetY - 1, z);
            if (below == BlockIds.AIR || below == BlockIds.WATER || below == BlockIds.WATER_FLOWING) {
                targetY--;
            } else {
                break;
            }
        }

        if (targetY != y) {
            chunkManager.setBlock(x, y, z, BlockIds.AIR, false);
            chunkManager.setBlock(x, targetY, z, block, false);
            return true;
        }
        return false;
    }
}
