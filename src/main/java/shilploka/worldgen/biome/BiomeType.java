package shilploka.worldgen.biome;

import shilploka.world.BlockIds;

/**
 * WHY:
 * Enumerates all 11 authentic regional biomes across the subcontinent of Bharat.
 * Each biome encapsulates distinct geography, microclimate, geological strata,
 * vegetation foliage, and hydrological parameters.
 *
 * WHAT:
 * 1. Ganga Maidan (गंगा मैदान): Alluvial loam floodplains & Sal forests.
 * 2. Thar Marusthal (थार मरुस्थल): Arid golden dunes, sandstone bluffs, Khejri trees.
 * 3. Himalaya Shikhar (हिमालय शिखर): Alpine snow crags, granite cliffs, Deodar cedars.
 * 4. Deccan Pathar (दक्कन पठार): Volcanic basalt plateau & black cotton Regur soil.
 * 5. Sahyadri Varshavan (सह्याद्रि वर्षावन): Western Ghats dense rainforest & tall Teak trees.
 * 6. Sundarban (सुंदरबन): Mangrove delta swamps, tidal channels & Sundari root arches.
 * 7. Poorvottar Baans (पूर्वोत्तर बांस वन): Northeastern lush rolling hills & Bamboo culms.
 * 8. Rann Namak (कच्छ का रण): White glistening salt flats of Kutch.
 * 9. Samudra Tat (समुद्र तट): Coastal sand beaches, palm shores, and bajri gravel.
 * 10. Sagar (सागर): Deep Indian Ocean waters & continental shelf drop-offs.
 * 11. Vindhya Jangal (विंध्य जंगल): Central Indian rugged plateaus, red sandstone ravines, dry teak.
 */
public enum BiomeType {

    GANGA_MAIDAN(
            "ganga_maidan",
            "Ganga Maidan (गंगा मैदान)",
            BlockIds.ALLUVIAL_SOIL,
            BlockIds.STONE,
            4,
            64,
            12.0,
            0.6,
            0.02f
    ),

    THAR_MARUSTHAL(
            "thar_marusthal",
            "Thar Marusthal (थार मरुस्थल)",
            BlockIds.THAR_SAND,
            BlockIds.YELLOW_SANDSTONE,
            5,
            68,
            16.0,
            0.8,
            0.008f
    ),

    HIMALAYA_SHIKHAR(
            "himalaya_shikhar",
            "Himalaya Shikhar (हिमालय शिखर)",
            BlockIds.HIMALAYAN_SNOW,
            BlockIds.GRANITE,
            3,
            96,
            45.0,
            1.8,
            0.015f
    ),

    DECCAN_PATHAR(
            "deccan_pathar",
            "Deccan Pathar (दक्कन पठार)",
            BlockIds.REGUR_SOIL,
            BlockIds.DECCAN_BASALT,
            3,
            75,
            18.0,
            0.7,
            0.012f
    ),

    SAHYADRI_VARSHAVAN(
            "sahyadri_varshavan",
            "Sahyadri Varshavan (सह्याद्रि वर्षावन)",
            BlockIds.ALLUVIAL_SOIL,
            BlockIds.DECCAN_BASALT,
            4,
            85,
            28.0,
            1.2,
            0.045f // Dense rainforest canopy
    ),

    SUNDARBAN(
            "sundarban",
            "Sundarban Delta (सुंदरबन दलदल)",
            BlockIds.ALLUVIAL_SOIL,
            BlockIds.STONE,
            2,
            62, // Sea-level marsh
            4.0,
            0.3,
            0.035f // Mangrove groves
    ),

    POORVOTTAR_BAANS(
            "poorvottar_baans",
            "Poorvottar Baans (पूर्वोत्तर बांस वन)",
            BlockIds.ALLUVIAL_SOIL,
            BlockIds.STONE,
            4,
            78,
            22.0,
            0.9,
            0.04f
    ),

    RANN_NAMAK(
            "rann_namak",
            "Rann of Kutch (कच्छ का रण)",
            BlockIds.NAMAK_CRUST,
            BlockIds.THAR_SAND,
            2,
            63,
            2.0, // Extremely flat salt crust
            0.1,
            0.0f // Zero trees on salt flat
    ),

    SAMUDRA_TAT(
            "samudra_tat",
            "Samudra Tat (समुद्र तट)",
            BlockIds.SAMUDRA_SAND,
            BlockIds.BAJRI,
            3,
            63,
            3.0,
            0.2,
            0.005f
    ),

    SAGAR(
            "sagar",
            "Sagar Deep Ocean (महासागर)",
            BlockIds.BAJRI,
            BlockIds.STONE,
            2,
            48, // Sunken seabed below sea level 62
            8.0,
            0.4,
            0.0f
    ),

    VINDHYA_JANGAL(
            "vindhya_jangal",
            "Vindhya Jangal (विंध्य जंगल)",
            BlockIds.ALLUVIAL_SOIL,
            BlockIds.RED_SANDSTONE,
            3,
            72,
            20.0,
            1.1,
            0.025f
    );

    private final String id;
    private final String displayName;
    private final int topBlock;
    private final int underBlock;
    private final int surfaceDepth;
    private final int baseHeight;
    private final double heightScale;
    private final double roughness;
    private final float treeDensity;

    BiomeType(String id, String displayName, int topBlock, int underBlock,
              int surfaceDepth, int baseHeight, double heightScale, double roughness,
              float treeDensity) {
        this.id = id;
        this.displayName = displayName;
        this.topBlock = topBlock;
        this.underBlock = underBlock;
        this.surfaceDepth = surfaceDepth;
        this.baseHeight = baseHeight;
        this.heightScale = heightScale;
        this.roughness = roughness;
        this.treeDensity = treeDensity;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getTopBlock() { return topBlock; }
    public int getUnderBlock() { return underBlock; }
    public int getSurfaceDepth() { return surfaceDepth; }
    public int getBaseHeight() { return baseHeight; }
    public double getHeightScale() { return heightScale; }
    public double getRoughness() { return roughness; }
    public float getTreeDensity() { return treeDensity; }
}
