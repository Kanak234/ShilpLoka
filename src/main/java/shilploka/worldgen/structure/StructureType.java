package shilploka.worldgen.structure;

/**
 * WHY:
 * Enumerates all authentic historical monument structures of Bharat that naturally
 * generate across the ShilpLoka terrain.
 *
 * WHAT:
 * Defines monument classifications, target biomes, dimensions, and cultural era lore.
 *
 * USED FOR:
 * Referenced by HistoricalStructureManager, WorldGenerator, and CodexScreen (Itihaas Granth).
 */
public enum StructureType {
    HARAPPAN_BATH(
            "Harappan Great Bath & Granary (सिंधु-सरस्वती स्नानागार)",
            "Sindhu-Saraswati civilization fired brick civic complex with waterproof bitumen-lined bath and colonnaded cloister.",
            14, 14, 8
    ),
    BUDDHIST_STUPA(
            "Great Buddhist Stupa (महान बौद्ध स्तूप)",
            "Mauryan hemispherical sandstone Anda dome, Harmika square railing, and triple Chhatra umbrella spire.",
            14, 14, 12
    ),
    CHOLA_STEPWELL(
            "Ancient Stepwell Baoli (प्राचीन बावड़ी / कल्याणी)",
            "Deep subterranean stone reservoir with geometric descending pyramidal steps and carved granite pillars.",
            12, 12, 14
    ),
    HILL_FORT(
            "Maratha-Rajput Hill Fort Ruin (पर्वतीय दुर्ग के अवशेष)",
            "Fortified sandstone and basalt battlements, defensive watchtowers, arrow slits, and ancient armory.",
            14, 14, 9
    ),
    ASHOKA_STHAMBHA(
            "Monolithic Ashokan Edict Pillar (अशोक स्तम्भ)",
            "Polished monolithic sandstone column inscribed with Dhamma edicts and crowned with the Dharma Chakra.",
            6, 6, 11
    ),
    GAON_VILLAGE(
            "Rural Indian Village (ग्रामीण भारत का गांव)",
            "Thriving rural community with central Panchayat Chaupal under sacred banyan, freshwater well, and artisan workshops.",
            16, 16, 10
    );

    private final String displayName;
    private final String description;
    private final int width;
    private final int depth;
    private final int height;

    StructureType(String displayName, String description, int width, int depth, int height) {
        this.displayName = displayName;
        this.description = description;
        this.width = width;
        this.depth = depth;
        this.height = height;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }

    public int getHeight() {
        return height;
    }
}
