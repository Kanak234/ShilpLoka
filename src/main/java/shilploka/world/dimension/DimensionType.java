package shilploka.world.dimension;

/**
 * WHY:
 * Defines the sacred realms/dimensions of ShilpLoka based on ancient Bharatiya cosmology.
 * - Prithvi (भूलोक / पृथ्वी): The terrestrial surface world of Bharat with 11 biomes and historical monuments.
 * - Paataal (पाताल लोक): The subterranean volcanic depths of burning Agni stone, lava seas, and the lair of the Andhakaar Naag serpent.
 * - Akash Dweep (आकाश द्वीप / स्वर्गलोक): Floating celestial islands of ivory marble and divine crystal gems in the higher atmosphere.
 *
 * WHAT:
 * Encapsulates dimension IDs, localized names, build heights, sea levels, and lighting rules.
 */
public enum DimensionType {
    PRITHVI(0, "Prithvi (Earth)", "पृथ्वी (भूलोक)", 320, 64, true, 1.0f),
    PAATAAL(1, "Paataal (Volcanic Underworld)", "पाताल लोक", 128, 32, false, 0.2f),
    AKASH_DWEEP(2, "Akash Dweep (Celestial Isles)", "आकाश द्वीप", 256, 0, true, 1.25f);

    private final int id;
    private final String englishName;
    private final String hindiName;
    private final int buildHeight;
    private final int seaLevel;
    private final boolean hasSkyLight;
    private final float ambientLight;

    DimensionType(int id, String englishName, String hindiName, int buildHeight, int seaLevel,
                  boolean hasSkyLight, float ambientLight) {
        this.id = id;
        this.englishName = englishName;
        this.hindiName = hindiName;
        this.buildHeight = buildHeight;
        this.seaLevel = seaLevel;
        this.hasSkyLight = hasSkyLight;
        this.ambientLight = ambientLight;
    }

    public int getId() {
        return id;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getHindiName() {
        return hindiName;
    }

    public int getBuildHeight() {
        return buildHeight;
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public boolean hasSkyLight() {
        return hasSkyLight;
    }

    public float getAmbientLight() {
        return ambientLight;
    }

    public static DimensionType fromId(int id) {
        for (DimensionType type : values()) {
            if (type.id == id) return type;
        }
        return PRITHVI;
    }
}
