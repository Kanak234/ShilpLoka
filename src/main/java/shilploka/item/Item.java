package shilploka.item;

/**
 * WHY:
 * Encapsulates properties of collectible, craftable, and placeable items in ShilpLoka.
 * Supports tool properties, food nutritional values, block placement mappings, and stack limits.
 *
 * WHAT:
 * Immutable item definition registered in ItemRegistry.
 *
 * USED FOR:
 * Referenced by ItemStack instances across Player inventory, crafting grids, and ItemEntity drops.
 *
 * WHERE NEXT:
 * Held in PlayerInventory and consumed on right-click for food eating or block placement.
 */
public class Item {

    private final String id;
    private final String displayName;
    private final int maxStackSize;
    private final ToolType toolType;
    private final ToolTier toolTier;
    private final int blockId; // 0 if not a block item
    private final boolean isFood;
    private final int foodValue;
    private final float saturationModifier;
    private final int maxDurability;

    public Item(String id, String displayName, int maxStackSize,
                ToolType toolType, ToolTier toolTier, int blockId,
                boolean isFood, int foodValue, float saturationModifier, int maxDurability) {
        this.id = id;
        this.displayName = displayName;
        this.maxStackSize = maxStackSize;
        this.toolType = toolType != null ? toolType : ToolType.NONE;
        this.toolTier = toolTier;
        this.blockId = blockId;
        this.isFood = isFood;
        this.foodValue = foodValue;
        this.saturationModifier = saturationModifier;
        this.maxDurability = maxDurability;
    }

    public static Item createBlockItem(String id, String displayName, int blockId) {
        return new Item(id, displayName, 64, ToolType.NONE, null, blockId, false, 0, 0.0f, 0);
    }

    public static Item createTool(String id, String displayName, ToolType type, ToolTier tier) {
        return new Item(id, displayName, 1, type, tier, 0, false, 0, 0.0f, tier.getDurability());
    }

    public static Item createFood(String id, String displayName, int foodValue, float saturation) {
        return new Item(id, displayName, 64, ToolType.NONE, null, 0, true, foodValue, saturation, 0);
    }

    public static Item createMaterial(String id, String displayName) {
        return new Item(id, displayName, 64, ToolType.NONE, null, 0, false, 0, 0.0f, 0);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getMaxStackSize() { return maxStackSize; }
    public ToolType getToolType() { return toolType; }
    public ToolTier getToolTier() { return toolTier; }
    public int getBlockId() { return blockId; }
    public boolean isBlock() { return blockId > 0; }
    public boolean isFood() { return isFood; }
    public int getFoodValue() { return foodValue; }
    public float getSaturationModifier() { return saturationModifier; }
    public int getMaxDurability() { return maxDurability; }
    public boolean isDamageable() { return maxDurability > 0; }
    public boolean isTool() { return toolType != ToolType.NONE && toolType != null; }
}
