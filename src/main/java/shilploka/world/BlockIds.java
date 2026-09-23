package shilploka.world;

/**
 * WHY:
 * Central catalogue of numeric block identifiers across the ShilpLoka engine.
 * Fast integer IDs are stored directly in BitStorage palette slots and used by
 * world generation, greedy meshing, physics, and gameplay logic.
 *
 * WHAT:
 * Defines unique primitive integer constants for Indian geological strata, soils,
 * indigenous woods, architectural elements, ores, and flora.
 *
 * USED FOR:
 * Referenced by WorldGenerator, Biome rules, OrePopulator, GreedyMesher, and TextureArray.
 *
 * WHERE NEXT:
 * GreedyMesher and TextureArray map these IDs to texture array layer slices and render passes.
 */
public final class BlockIds {

    private BlockIds() {
        // Private constructor prevents instantiation
    }

    // 0: Air & Void
    public static final int AIR = 0;

    // 1-6: Geological Strata & Sediments
    public static final int STONE = 1;
    public static final int GRANITE = 2;
    public static final int RED_SANDSTONE = 3;
    public static final int ALLUVIAL_SOIL = 4;
    public static final int YELLOW_SANDSTONE = 5;
    public static final int DECCAN_BASALT = 6;

    // 7-10: Architectural Masonry & Timber
    public static final int FIRED_BRICK = 7;
    public static final int DIYA = 8;
    public static final int SAL_LOG = 9;
    public static final int SAL_PLANKS = 10;

    // 11: Fluids
    public static final int WATER = 11;

    // 12-13: Regional Biome Surface Sediments
    public static final int THAR_SAND = 12;
    public static final int HIMALAYAN_SNOW = 13;

    // 14-16: Botanical Foliage
    public static final int SAL_LEAVES = 14;
    public static final int DEODAR_LEAVES = 15;
    public static final int KHEJRI_LEAVES = 16;

    // 17-21: Subterranean Mineral Ores
    public static final int KOYLA_ORE = 17;     // Coal / Lignite (कोयला)
    public static final int TAAMBA_ORE = 18;    // Copper / Malachite (तांबा)
    public static final int LOHA_ORE = 19;      // Iron / Hematite (लोहा)
    public static final int RAJAVARTA_ORE = 20; // Lapis Lazuli with Pyrite (राजावर्त)
    public static final int HIRA_ORE = 21;      // Diamond in Kimberlite (हीरा)

    // 22-24: Volcanic Fluids, River Sediments & Flowing Water
    public static final int LAVA = 22;          // Volcanic molten rock / Magma (लावा)
    public static final int BAJRI = 23;         // River gravel / pebbles (बजरी)
    public static final int WATER_FLOWING = 24; // Flowing water currents (बहता जल)

    // 25-27: Regional Woods & Precious Ores
    public static final int DEODAR_LOG = 25;    // Himalayan Deodar Cedar Log (देवदार)
    public static final int KHEJRI_LOG = 26;    // Thar Desert Khejri Log (खेजड़ी)
    public static final int SONA_ORE = 27;      // Gold ore in quartz veins (सोना)

    // 28-35: Specialized Crafting Workstations & Architectural Glass
    public static final int KARYASHALA = 28;      // 3x3 Workbench (शिल्प कार्यशाला)
    public static final int PAKKI_BHATTI = 29;    // Ceramic & Terracotta Kiln (पक्की भट्ठी)
    public static final int SMELTING_BHATTI = 30; // Metallurgical Alloy Smelter (धातु प्रगलन भट्ठी)
    public static final int CHAAK = 31;           // Potter's Wheel (कुम्हार का चाक)
    public static final int KARGAH = 32;          // Handloom for Weaving Khadi (करघा)
    public static final int CHAKKI = 33;          // Stone Rotary Hand Quern / Mill (हाथ की चक्की)
    public static final int NIHAI = 34;           // Metallurgical Forged Anvil (लोहार की निहाई)
    public static final int KAANCH = 35;          // Melted Silica Sand Architectural Glass (कांच)

    // 36-43: Regional Indian Terrain & Botanical Strata (Phase P10)
    public static final int REGUR_SOIL = 36;      // Deccan Black Cotton Soil (रेगुर काली मिट्टी)
    public static final int TEAK_LOG = 37;        // Sahyadri Teak Timber Log (सागवान का लट्ठा)
    public static final int TEAK_LEAVES = 38;     // Sahyadri Teak Foliage (सागवान के पत्ते)
    public static final int NAMAK_CRUST = 39;     // Rann of Kutch Salt Crust (कच्छ का नमक)
    public static final int MANGROVE_LOG = 40;    // Sundarban Mangrove Log (सुंदरी का लट्ठा)
    public static final int MANGROVE_LEAVES = 41; // Sundarban Mangrove Foliage (सुंदरी के पत्ते)
    public static final int BAANS = 42;           // Poorvottar Bamboo Culm (बांस की शाखा)
    public static final int SAMUDRA_SAND = 43;    // Coastal Marine Beach Sand (समुद्र तट की रेत)

    // 44-46: Historical Monument Architecture & Relics (Phase P11)
    public static final int CARVED_STONE = 44;    // Carved Mandapa Relief Stone (उत्कीर्ण प्रस्तर)
    public static final int ANCIENT_CHEST = 45;   // Historic Terracotta/Bronze Relic Chest (प्राचीन मंजूषा)
    public static final int ASHOKA_CHAKRA = 46;   // Monolithic Ashokan Wheel / Lion Capital (अशोक चक्र स्तम्भ)

    // 47-51: Kaushal Upgrades & Indigenous Agriculture (Phase P13)
    public static final int SHILP_MANDAP = 47;    // Vedic Kaushal Upgrade Altar (शिल्प मंडप)
    public static final int TILLED_SOIL = 48;     // Plowed Fertile Alluvial Soil (जुती हुई भूमि)
    public static final int DHAN_CROP = 49;       // Flooded Paddy Rice Crop (धान की फसल)
    public static final int GANNA_CROP = 50;      // Riverbank Sugarcane Reed (गन्ने की फसल)
    public static final int KAPAAS_CROP = 51;     // Indigenous Cotton Bush (कपास का पौधा)

    // 52-56: Dimensions (Paataal & Akash Dweep) & Cosmic Portals (Phase P14)
    public static final int PAATAAL_AGNI_ROCK = 52;   // Volcanic Fire Rock / Brimstone (अग्नि पाषाण)
    public static final int PAATAAL_LAVA_GEL = 53;    // Molten Magma Crust / Slag (ज्वालामुखी पिण्ड)
    public static final int AKASH_MARBLE = 54;        // Floating Celestial Sky Marble (आकाशीय श्वेत प्रस्तर)
    public static final int AKASH_CRYSTAL = 55;       // Luminous Sky Gem Crystal (दिव्य मणि)
    public static final int DIMENSIONAL_PORTAL = 56;  // Swirling Gateway Between Realms (आयाम द्वार)

    /**
     * Determines whether a block is solid and opaque for greedy meshing face occlusion.
     */
    public static boolean isOpaque(int blockId) {
        return blockId > 0 && blockId != WATER && blockId != WATER_FLOWING &&
               blockId != LAVA && blockId != SAL_LEAVES &&
               blockId != DEODAR_LEAVES && blockId != KHEJRI_LEAVES &&
               blockId != TEAK_LEAVES && blockId != MANGROVE_LEAVES &&
               blockId != BAANS && blockId != DIYA && blockId != KAANCH &&
               blockId != DHAN_CROP && blockId != GANNA_CROP && blockId != KAPAAS_CROP &&
               blockId != DIMENSIONAL_PORTAL && blockId != AKASH_CRYSTAL;
    }

    /**
     * Returns true if the block is an interactive crafting workstation.
     */
    public static boolean isWorkstation(int blockId) {
        return (blockId >= KARYASHALA && blockId <= NIHAI) || blockId == SHILP_MANDAP;
    }

    /**
     * Returns true if the block is solid and obstructs player movement/collision.
     */
    public static boolean isSolid(int blockId) {
        return blockId > 0 && blockId != WATER && blockId != WATER_FLOWING && blockId != LAVA &&
               blockId != DHAN_CROP && blockId != GANNA_CROP && blockId != KAPAAS_CROP &&
               blockId != DIMENSIONAL_PORTAL;
    }

    /**
     * Returns true if the block is subject to gravitational falling when unsupported below.
     */
    public static boolean isFallingBlock(int blockId) {
        return blockId == THAR_SAND || blockId == BAJRI;
    }

    /**
     * Returns true if the block is a fluid (water or lava).
     */
    public static boolean isFluid(int blockId) {
        return blockId == WATER || blockId == WATER_FLOWING || blockId == LAVA;
    }

    /**
     * Retrieves the intrinsic block light emission level (0 to 15).
     */
    public static int getLightEmission(int blockId) {
        return switch (blockId) {
            case DIYA -> 14;               // Clay oil lamp (दीया)
            case LAVA -> 15;               // Molten magma / Agni (अग्नि)
            case AKASH_CRYSTAL -> 15;      // Luminous celestial sky crystal (दिव्य मणि)
            case DIMENSIONAL_PORTAL -> 11; // Swirling cosmic gateway
            case PAATAAL_LAVA_GEL -> 10;   // Molten magma crust
            case PAATAAL_AGNI_ROCK -> 5;   // Fire rock embers
            default -> 0;
        };
    }

    /**
     * Retrieves light opacity / absorption factor (0 = passes freely, 15 = opaque solid).
     */
    public static int getLightAbsorption(int blockId) {
        if (blockId == AIR) return 0;
        if (blockId == WATER || blockId == WATER_FLOWING) return 2; // Water attenuates light faster
        if (blockId == LAVA) return 0; // Lava glows and passes its own light
        if (blockId == SAL_LEAVES || blockId == DEODAR_LEAVES || blockId == KHEJRI_LEAVES) return 1;
        if (blockId == DIYA || blockId == DIMENSIONAL_PORTAL || blockId == AKASH_CRYSTAL) return 0;
        return isOpaque(blockId) ? 15 : 0;
    }
}
