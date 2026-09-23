package shilploka.item;

/**
 * WHY:
 * Defines material tiers for authentic Indian tools and weaponry in ShilpLoka.
 * Higher tiers enable harvesting harder mineral ores (e.g. Wootz Steel for Hira diamond),
 * accelerate mining speed, and withstand greater durability wear.
 *
 * WHAT:
 * Enum representing 7 distinct progression tiers:
 * Wood (Lakdi), Stone (Patthar), Copper (Taamba), Bronze (Kansa), Iron (Loha),
 * Wootz Steel (Wootz Faulaad), and Vajra.
 *
 * USED FOR:
 * Consulted during block breaking hardness validation and tool damage calculation.
 *
 * WHERE NEXT:
 * Applied in Item definitions and checked by Player interaction pipelines.
 */
public enum ToolTier {
    WOOD(0, 2.0f, 59, 2.0f, "Lakdi (Wood)"),
    STONE(1, 4.0f, 131, 3.0f, "Patthar (Stone)"),
    COPPER(1, 5.0f, 190, 3.5f, "Taamba (Copper)"),
    BRONZE(2, 6.0f, 280, 4.0f, "Kansa (Bronze)"),
    IRON(2, 7.0f, 380, 4.5f, "Loha (Iron)"),
    WOOTZ(3, 9.5f, 800, 6.0f, "Wootz Faulaad (Wootz Steel)"),
    VAJRA(4, 14.0f, 2500, 9.0f, "Vajra (Celestial)");

    private final int harvestLevel;
    private final float miningSpeed;
    private final int durability;
    private final float attackDamage;
    private final String displayName;

    ToolTier(int harvestLevel, float miningSpeed, int durability, float attackDamage, String displayName) {
        this.harvestLevel = harvestLevel;
        this.miningSpeed = miningSpeed;
        this.durability = durability;
        this.attackDamage = attackDamage;
        this.displayName = displayName;
    }

    public int getHarvestLevel() { return harvestLevel; }
    public float getMiningSpeed() { return miningSpeed; }
    public int getDurability() { return durability; }
    public float getAttackDamage() { return attackDamage; }
    public String getDisplayName() { return displayName; }
}
