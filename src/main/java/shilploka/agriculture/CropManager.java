package shilploka.agriculture;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.kaushal.KaushalUpgradeType;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Governs indigenous Indian agrarian mechanics (Krishi Vigyan).
 * Features flooded paddy rice cultivation (Dhan), tall riverbank sugarcane (Ganna),
 * and indigenous cotton (Kapaas) grown on plowed alluvial soils.
 *
 * WHAT:
 * Validates soil tilling, seed sowing, water irrigation proximity, and crop harvesting yields.
 *
 * USED FOR:
 * Referenced during block interaction and destruction in Game.java.
 */
public final class CropManager {

    private CropManager() {}

    /**
     * Checks whether a Phaavda tool can till the specified block into fertile soil.
     */
    public static boolean canTill(int blockId) {
        return blockId == BlockIds.ALLUVIAL_SOIL;
    }

    /**
     * Checks whether a given seed item can be sown atop the specified soil block.
     */
    public static int getPlantableCropBlock(ItemStack seedStack, int soilBlockId) {
        if (seedStack == null || seedStack.isEmpty()) return BlockIds.AIR;
        if (soilBlockId != BlockIds.TILLED_SOIL && soilBlockId != BlockIds.ALLUVIAL_SOIL) return BlockIds.AIR;

        var item = seedStack.getItem();
        if (item == ItemRegistry.CHAWAL) {
            return BlockIds.DHAN_CROP;
        } else if (item == ItemRegistry.GANNA) {
            return BlockIds.GANNA_CROP;
        } else if (item == ItemRegistry.KAPAAS) {
            return BlockIds.KAPAAS_CROP;
        }
        return BlockIds.AIR;
    }

    /**
     * Generates harvest drops when a mature crop block is broken, applying Kaushal bonuses.
     */
    public static List<ItemStack> getCropHarvest(int cropBlockId, ItemStack harvestingTool) {
        List<ItemStack> drops = new ArrayList<>();
        boolean doubleYield = (harvestingTool != null && harvestingTool.getKaushal() == KaushalUpgradeType.KRISHI_VRIDDHI);
        int multiplier = doubleYield ? 2 : 1;

        switch (cropBlockId) {
            case BlockIds.DHAN_CROP -> drops.add(new ItemStack(ItemRegistry.CHAWAL, 3 * multiplier));
            case BlockIds.GANNA_CROP -> drops.add(new ItemStack(ItemRegistry.GANNA, 2 * multiplier));
            case BlockIds.KAPAAS_CROP -> drops.add(new ItemStack(ItemRegistry.KAPAAS, 2 * multiplier));
        }

        return drops;
    }

    /**
     * Validates whether a tilled block is hydrated by nearby water within a 4-block horizontal radius.
     */
    public static boolean isIrrigated(ChunkManager chunkManager, int bx, int by, int bz) {
        if (chunkManager == null) return true;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                int b = chunkManager.getBlock(bx + dx, by, bz + dz);
                int bDown = chunkManager.getBlock(bx + dx, by - 1, bz + dz);
                if (b == BlockIds.WATER || b == BlockIds.WATER_FLOWING ||
                    bDown == BlockIds.WATER || bDown == BlockIds.WATER_FLOWING) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tillSoil(ChunkManager chunkManager, int bx, int by, int bz) {
        if (chunkManager == null) return false;
        int current = chunkManager.getBlock(bx, by, bz);
        if (canTill(current)) {
            chunkManager.setBlock(bx, by, bz, BlockIds.TILLED_SOIL);
            return true;
        }
        return false;
    }

    public static boolean plantCrop(ChunkManager chunkManager, int px, int py, int pz, int cropBlockId) {
        if (chunkManager == null || cropBlockId == BlockIds.AIR) return false;
        int below = chunkManager.getBlock(px, py - 1, pz);
        int current = chunkManager.getBlock(px, py, pz);
        if (current == BlockIds.AIR && (below == BlockIds.TILLED_SOIL || below == BlockIds.ALLUVIAL_SOIL)) {
            chunkManager.setBlock(px, py, pz, cropBlockId);
            return true;
        }
        return false;
    }
}
