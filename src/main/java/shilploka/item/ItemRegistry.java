package shilploka.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.world.BlockIds;

import java.util.HashMap;
import java.util.Map;

/**
 * WHY:
 * Central registry cataloging all items, materials, tools, foods, and block drop mappings.
 *
 * WHAT:
 * Thread-safe hash maps indexing items by namespaced string ID and by placed block ID.
 * Defines complete tool progression across 7 tiers and 4 weapon/tool classes.
 *
 * USED FOR:
 * Referenced during block destruction to drop items, by recipes, and during inventory serialization.
 *
 * WHERE NEXT:
 * Used by RecipeManager to validate craft outputs and by ItemEntity to spawn drops.
 */
public final class ItemRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRegistry.class);

    private static final Map<String, Item> ITEMS_BY_ID = new HashMap<>();
    private static final Map<Integer, Item> ITEMS_BY_BLOCK_ID = new HashMap<>();
    private static final Map<Integer, String> BLOCK_DROPS = new HashMap<>();

    // Base Crafting Materials
    public static final Item LAKDI_CHHAD;
    public static final Item KOYLA;
    public static final Item TAAMBA_INGOT;
    public static final Item KANSA_INGOT;
    public static final Item LOHA_INGOT;
    public static final Item WOOTZ_INGOT;
    public static final Item HIRA;
    public static final Item MITTI_CLAY;

    // Workstation Materials & Pottery
    public static final Item SONA_INGOT;
    public static final Item PEETAL_INGOT;
    public static final Item KAPAAS;
    public static final Item SOOT;
    public static final Item KHADI_VASTRA;
    public static final Item ATTA;
    public static final Item HALDI_RHIZOME;
    public static final Item HALDI_POWDER;
    public static final Item KULHAD;
    public static final Item MATKA;
    public static final Item SURAHI;
    public static final Item HANDI;

    // Traditional Indian Foods
    public static final Item ROTI;
    public static final Item AAM;
    public static final Item KHAJOOR;
    public static final Item CHAWAL;
    public static final Item DOODH;

    // Fauna Drops & Barter Currency
    public static final Item MOR_PANKH;
    public static final Item BHASMA;
    public static final Item MUDRA_COPPER;
    public static final Item MUDRA_SILVER;
    public static final Item MUDRA_GOLD;
    public static final Item KAURI;

    // Historical Manuscripts & Antiquities (Phase P11)
    public static final Item HARAPPAN_SEAL;
    public static final Item MAURYA_EDICT;
    public static final Item TAMRAPATRA;
    public static final Item TALPANDULIPI;

    // Phase P13: Kaushal, Ayurveda, Agriculture & Mounts
    public static final Item KAATHI;
    public static final Item GANNA;
    public static final Item GUD;
    public static final Item TULSI_KADHA;
    public static final Item NEEM_LEP;
    public static final Item HALDI_DOODH;
    public static final Item SHILAJIT_RASAYANA;
    public static final Item VAJRA_CORE;
    public static final Item VAJRA_INGOT;

    // Standard Tools (representative instances)
    public static final Item WOOD_KUDAAL;
    public static final Item STONE_KUDAAL;
    public static final Item COPPER_KUDAAL;
    public static final Item BRONZE_KUDAAL;
    public static final Item LOHA_KUDAAL;
    public static final Item WOOTZ_KUDAAL;
    public static final Item VAJRA_KUDAAL;

    public static final Item WOOD_TALWAR;
    public static final Item STONE_TALWAR;
    public static final Item COPPER_TALWAR;
    public static final Item BRONZE_TALWAR;
    public static final Item LOHA_TALWAR;
    public static final Item WOOTZ_TALWAR;
    public static final Item VAJRA_TALWAR;

    static {
        // 1. Register Blocks as Items
        registerBlock(BlockIds.ALLUVIAL_SOIL, "shilploka:alluvial_soil", "Alluvial Soil (जलोढ़ मिट्टी)");
        registerBlock(BlockIds.GRANITE, "shilploka:granite", "Granite Cobble (ग्रेनाइट पत्थर)");
        registerBlock(BlockIds.RED_SANDSTONE, "shilploka:red_sandstone", "Red Sandstone (लाल बलुआ पत्थर)");
        registerBlock(BlockIds.YELLOW_SANDSTONE, "shilploka:yellow_sandstone", "Yellow Sandstone (पीला बलुआ पत्थर)");
        registerBlock(BlockIds.THAR_SAND, "shilploka:thar_sand", "Thar Sand (थार की रेत)");
        registerBlock(BlockIds.BAJRI, "shilploka:bajri", "Bajri Gravel (बजरी)");
        registerBlock(BlockIds.FIRED_BRICK, "shilploka:fired_brick", "Harappan Fired Brick (पक्की ईंट)");
        registerBlock(BlockIds.SAL_LOG, "shilploka:sal_log", "Sal Timber Log (साल का लट्ठा)");
        registerBlock(BlockIds.SAL_PLANKS, "shilploka:sal_planks", "Sal Wood Planks (साल की लकड़ी)");
        registerBlock(BlockIds.SAL_LEAVES, "shilploka:sal_leaves", "Sal Tree Leaves (साल के पत्ते)");
        registerBlock(BlockIds.DEODAR_LOG, "shilploka:deodar_log", "Deodar Cedar Log (देवदार का लट्ठा)");
        registerBlock(BlockIds.DEODAR_LEAVES, "shilploka:deodar_leaves", "Deodar Foliage (देवदार के पत्ते)");
        registerBlock(BlockIds.KHEJRI_LOG, "shilploka:khejri_log", "Khejri Desert Log (खेजड़ी का लट्ठा)");
        registerBlock(BlockIds.KHEJRI_LEAVES, "shilploka:khejri_leaves", "Khejri Foliage (खेजड़ी के पत्ते)");
        registerBlock(BlockIds.HIMALAYAN_SNOW, "shilploka:himalayan_snow", "Himalayan Snow (हिमालयी हिम)");
        registerBlock(BlockIds.DECCAN_BASALT, "shilploka:deccan_basalt", "Deccan Basalt (दक्कन बेसाल्ट)");
        registerBlock(BlockIds.DIYA, "shilploka:diya", "Clay Diya (मिट्टी का दीया)");

        // Ores
        registerBlock(BlockIds.KOYLA_ORE, "shilploka:koyla_ore", "Koyla Ore (कोयला अयस्क)");
        registerBlock(BlockIds.TAAMBA_ORE, "shilploka:taamba_ore", "Taamba Ore (तांबा अयस्क)");
        registerBlock(BlockIds.LOHA_ORE, "shilploka:loha_ore", "Loha Ore (लोहा अयस्क)");
        registerBlock(BlockIds.SONA_ORE, "shilploka:sona_ore", "Sona Ore (सोना अयस्क)");
        registerBlock(BlockIds.HIRA_ORE, "shilploka:hira_ore", "Hira Ore (हीरा अयस्क)");

        // Workstations & Architectural Glass
        registerBlock(BlockIds.KARYASHALA, "shilploka:karyashala", "Karyashala Workbench (शिल्प कार्यशाला)");
        registerBlock(BlockIds.PAKKI_BHATTI, "shilploka:pakki_bhatti", "Pakki Bhatti Kiln (पक्की भट्ठी)");
        registerBlock(BlockIds.SMELTING_BHATTI, "shilploka:smelting_bhatti", "Smelting Bhatti (धातु प्रगलन भट्ठी)");
        registerBlock(BlockIds.CHAAK, "shilploka:chaak", "Kumhar Chaak (कुम्हार का चाक)");
        registerBlock(BlockIds.KARGAH, "shilploka:kargah", "Kargah Loom (करघा)");
        registerBlock(BlockIds.CHAKKI, "shilploka:chakki", "Atta Chakki Mill (हाथ की चक्की)");
        registerBlock(BlockIds.NIHAI, "shilploka:nihai", "Lohar Nihai Anvil (लोहार की निहाई)");
        registerBlock(BlockIds.KAANCH, "shilploka:kaanch", "Silica Glass (कांच)");

        // Phase P10: Regional Indian Terrain & Botanical Blocks
        registerBlock(BlockIds.REGUR_SOIL, "shilploka:regur_soil", "Deccan Regur Soil (रेगुर काली मिट्टी)");
        registerBlock(BlockIds.TEAK_LOG, "shilploka:teak_log", "Teak Timber Log (सागवान का लट्ठा)");
        registerBlock(BlockIds.TEAK_LEAVES, "shilploka:teak_leaves", "Teak Foliage (सागवान के पत्ते)");
        registerBlock(BlockIds.NAMAK_CRUST, "shilploka:namak_crust", "Rann Salt Crust (कच्छ का नमक)");
        registerBlock(BlockIds.MANGROVE_LOG, "shilploka:mangrove_log", "Mangrove Log (सुंदरी का लट्ठा)");
        registerBlock(BlockIds.MANGROVE_LEAVES, "shilploka:mangrove_leaves", "Mangrove Foliage (सुंदरी के पत्ते)");
        registerBlock(BlockIds.BAANS, "shilploka:baans", "Bamboo Culm (बांस की शाखा)");
        registerBlock(BlockIds.SAMUDRA_SAND, "shilploka:samudra_sand", "Coastal Beach Sand (समुद्र तट की रेत)");

        // Phase P11: Historical Architecture & Relics
        registerBlock(BlockIds.CARVED_STONE, "shilploka:carved_stone", "Carved Mandapa Stone (उत्कीर्ण प्रस्तर)");
        registerBlock(BlockIds.ANCIENT_CHEST, "shilploka:ancient_chest", "Ancient Relic Chest (प्राचीन मंजूषा)");
        registerBlock(BlockIds.ASHOKA_CHAKRA, "shilploka:ashoka_chakra", "Ashokan Chakra Pillar (अशोक चक्र स्तम्भ)");

        // Phase P13: Kaushal Altar & Indigenous Crops
        registerBlock(BlockIds.SHILP_MANDAP, "shilploka:shilp_mandap", "Shilp Mandap Upgrade Altar (शिल्प मंडप)");
        registerBlock(BlockIds.TILLED_SOIL, "shilploka:tilled_soil", "Tilled Alluvial Soil (जुती हुई भूमि)");
        registerBlock(BlockIds.DHAN_CROP, "shilploka:dhan_crop", "Flooded Paddy Crop (धान की फसल)");
        registerBlock(BlockIds.GANNA_CROP, "shilploka:ganna_crop", "Sugarcane Crop (गन्ने की फसल)");
        registerBlock(BlockIds.KAPAAS_CROP, "shilploka:kapaas_crop", "Cotton Crop (कपास का पौधा)");

        // Phase P14: Dimensions (Paataal & Akash Dweep) & Cosmic Portals
        registerBlock(BlockIds.PAATAAL_AGNI_ROCK, "shilploka:paataal_agni_rock", "Agni Rock (अग्नि पाषाण)");
        registerBlock(BlockIds.PAATAAL_LAVA_GEL, "shilploka:paataal_lava_gel", "Lava Gel Magma Crust (ज्वालामुखी पिण्ड)");
        registerBlock(BlockIds.AKASH_MARBLE, "shilploka:akash_marble", "Akash Celestial Marble (आकाशीय श्वेत प्रस्तर)");
        registerBlock(BlockIds.AKASH_CRYSTAL, "shilploka:akash_crystal", "Akash Crystal Gem (दिव्य मणि)");
        registerBlock(BlockIds.DIMENSIONAL_PORTAL, "shilploka:dimensional_portal", "Dimensional Portal (आयाम द्वार)");

        // 2. Register Materials
        LAKDI_CHHAD = register(Item.createMaterial("shilploka:lakdi_chhad", "Wood Stick (लकड़ी की छड़)"));
        KOYLA = register(Item.createMaterial("shilploka:koyla", "Koyla Mineral (कोयला)"));
        TAAMBA_INGOT = register(Item.createMaterial("shilploka:taamba_ingot", "Taamba Ingot (तांबे की सिल्ली)"));
        KANSA_INGOT = register(Item.createMaterial("shilploka:kansa_ingot", "Kansa Ingot (कांस्य की सिल्ली)"));
        LOHA_INGOT = register(Item.createMaterial("shilploka:loha_ingot", "Loha Ingot (लोहे की सिल्ली)"));
        WOOTZ_INGOT = register(Item.createMaterial("shilploka:wootz_ingot", "Wootz Steel Ingot (वूट्ज़ फ़ौलाद की सिल्ली)"));
        HIRA = register(Item.createMaterial("shilploka:hira", "Hira Diamond (हीरा रत्न)"));
        MITTI_CLAY = register(Item.createMaterial("shilploka:mitti_clay", "Mitti Clay Lump (चिकनी मिट्टी)"));

        // Workstation Materials & Pottery
        SONA_INGOT = register(Item.createMaterial("shilploka:sona_ingot", "Sona Ingot (सोने की सिल्ली)"));
        PEETAL_INGOT = register(Item.createMaterial("shilploka:peetal_ingot", "Peetal Brass Ingot (पीतल की सिल्ली)"));
        KAPAAS = register(Item.createMaterial("shilploka:kapaas", "Raw Kapaas Cotton (कपास)"));
        SOOT = register(Item.createMaterial("shilploka:soot", "Soot Spun Yarn (सूत)"));
        KHADI_VASTRA = register(Item.createMaterial("shilploka:khadi_vastra", "Khadi Cloth (खादी वस्त्र)"));
        ATTA = register(Item.createMaterial("shilploka:atta", "Atta Flour (गेहूं का आटा)"));
        HALDI_RHIZOME = register(Item.createMaterial("shilploka:haldi_rhizome", "Raw Haldi Rhizome (कच्ची हल्दी की गांठ)"));
        HALDI_POWDER = register(Item.createMaterial("shilploka:haldi_powder", "Haldi Powder (हल्दी चूर्ण)"));
        KULHAD = register(Item.createMaterial("shilploka:kulhad", "Terracotta Kulhad (कुल्हड़)"));
        MATKA = register(Item.createMaterial("shilploka:matka", "Earthen Matka Pot (मिट्टी का मटका)"));
        SURAHI = register(Item.createMaterial("shilploka:surahi", "Clay Surahi (मिट्टी की सुराही)"));
        HANDI = register(Item.createMaterial("shilploka:handi", "Earthen Handi (मिट्टी की हांड़ी)"));

        // 3. Register Foods & Potions
        ROTI = register(Item.createFood("shilploka:roti", "Tandoori Roti (तंदूरी रोटी)", 5, 6.0f));
        AAM = register(Item.createFood("shilploka:aam", "Meetha Aam (मीठा आम)", 4, 3.0f));
        KHAJOOR = register(Item.createFood("shilploka:khajoor", "Khajoor Dates (खजूर)", 3, 2.5f));
        CHAWAL = register(Item.createFood("shilploka:chawal", "Basmati Chawal (बासमती चावल)", 2, 1.5f));
        DOODH = register(Item.createFood("shilploka:doodh", "Pavitra Gau Doodh (पवित्र गो दुग्ध)", 6, 8.0f));
        GANNA = register(Item.createMaterial("shilploka:ganna", "Ganna Reed (गन्ना)"));
        GUD = register(Item.createFood("shilploka:gud", "Gur Jaggery (गुड़)", 3, 4.0f));
        TULSI_KADHA = register(Item.createFood("shilploka:tulsi_kadha", "Tulsi Kadha Antivenom (तुलसी काढ़ा)", 4, 6.0f));
        NEEM_LEP = register(Item.createMaterial("shilploka:neem_lep", "Neem Lep Protective Salve (नीम लेप)"));
        HALDI_DOODH = register(Item.createFood("shilploka:haldi_doodh", "Haldi Doodh Tonic (हल्दी दूध)", 8, 10.0f));
        SHILAJIT_RASAYANA = register(Item.createFood("shilploka:shilajit_rasayana", "Shilajit Rasayana Elixir (शिलाजीत रसायन)", 6, 8.0f));
        KAATHI = register(Item.createMaterial("shilploka:kaathi", "Traditional Kaathi Saddle (काठी)"));
        VAJRA_CORE = register(Item.createMaterial("shilploka:vajra_core", "Vajra Core (इन्द्र वज्र मणिक)"));
        VAJRA_INGOT = register(Item.createMaterial("shilploka:vajra_ingot", "Vajra Ingot (वज्र धातु की सिल्ली)"));

        // Fauna & Cultural Drops
        MOR_PANKH = register(Item.createMaterial("shilploka:mor_pankh", "Mor Pankh Feather (मयूर पंख)"));
        BHASMA = register(Item.createMaterial("shilploka:bhasma", "Pavitra Bhasma Ash (पवित्र भस्म)"));
        MUDRA_COPPER = register(Item.createMaterial("shilploka:mudra_copper", "Taamba Mudra Coin (तांबे की मुद्रा)"));
        MUDRA_SILVER = register(Item.createMaterial("shilploka:mudra_silver", "Chandi Mudra Coin (चांदी की मुद्रा)"));
        MUDRA_GOLD = register(Item.createMaterial("shilploka:mudra_gold", "Swarna Mudra Coin (स्वर्ण मुद्रा)"));
        KAURI = register(Item.createMaterial("shilploka:kauri", "Kauri Barter Shell (कौड़ी)"));

        // Historical Manuscripts & Antiquities (Phase P11)
        HARAPPAN_SEAL = register(Item.createMaterial("shilploka:harappan_seal", "Harappan Pashupati Seal (हड़प्पा की मुहर)"));
        MAURYA_EDICT = register(Item.createMaterial("shilploka:maurya_edict", "Ashokan Edict Inscription (अशोक का शिलालेख)"));
        TAMRAPATRA = register(Item.createMaterial("shilploka:tamrapatra", "Ancient Copper Plate Inscription (ताम्रपत्र)"));
        TALPANDULIPI = register(Item.createMaterial("shilploka:talpandulipi", "Palm Leaf Manuscript (तालपत्र पांडुलिपि)"));

        // 4. Register All 7 Tool Tiers across 4 Tool Classes
        WOOD_KUDAAL = register(Item.createTool("shilploka:wood_kudaal", "Wood Kudaal (लकड़ी की कुदाल)", ToolType.KUDAAL, ToolTier.WOOD));
        STONE_KUDAAL = register(Item.createTool("shilploka:stone_kudaal", "Stone Kudaal (पत्थर की कुदाल)", ToolType.KUDAAL, ToolTier.STONE));
        COPPER_KUDAAL = register(Item.createTool("shilploka:copper_kudaal", "Copper Kudaal (तांबे की कुदाल)", ToolType.KUDAAL, ToolTier.COPPER));
        BRONZE_KUDAAL = register(Item.createTool("shilploka:bronze_kudaal", "Bronze Kudaal (कांस्य की कुदाल)", ToolType.KUDAAL, ToolTier.BRONZE));
        LOHA_KUDAAL = register(Item.createTool("shilploka:loha_kudaal", "Iron Kudaal (लोहे की कुदाल)", ToolType.KUDAAL, ToolTier.IRON));
        WOOTZ_KUDAAL = register(Item.createTool("shilploka:wootz_kudaal", "Wootz Kudaal (वूट्ज़ फ़ौलाद की कुदाल)", ToolType.KUDAAL, ToolTier.WOOTZ));
        VAJRA_KUDAAL = register(Item.createTool("shilploka:vajra_kudaal", "Vajra Kudaal (वज्र कुदाल)", ToolType.KUDAAL, ToolTier.VAJRA));

        WOOD_TALWAR = register(Item.createTool("shilploka:wood_talwar", "Wood Talwar (लकड़ी की तलवार)", ToolType.TALWAR, ToolTier.WOOD));
        STONE_TALWAR = register(Item.createTool("shilploka:stone_talwar", "Stone Talwar (पत्थर की तलवार)", ToolType.TALWAR, ToolTier.STONE));
        COPPER_TALWAR = register(Item.createTool("shilploka:copper_talwar", "Copper Talwar (तांबे की तलवार)", ToolType.TALWAR, ToolTier.COPPER));
        BRONZE_TALWAR = register(Item.createTool("shilploka:bronze_talwar", "Bronze Talwar (कांस्य की तलवार)", ToolType.TALWAR, ToolTier.BRONZE));
        LOHA_TALWAR = register(Item.createTool("shilploka:loha_talwar", "Iron Talwar (लोहे की तलवार)", ToolType.TALWAR, ToolTier.IRON));
        WOOTZ_TALWAR = register(Item.createTool("shilploka:wootz_talwar", "Wootz Talwar (वूट्ज़ फ़ौलादी तलवार)", ToolType.TALWAR, ToolTier.WOOTZ));
        VAJRA_TALWAR = register(Item.createTool("shilploka:vajra_talwar", "Vajra Talwar (वज्र तलवार)", ToolType.TALWAR, ToolTier.VAJRA));

        // Kulhadi (Axes)
        register(Item.createTool("shilploka:wood_kulhadi", "Wood Kulhadi (लकड़ी की कुल्हाड़ी)", ToolType.KULHADI, ToolTier.WOOD));
        register(Item.createTool("shilploka:stone_kulhadi", "Stone Kulhadi (पत्थर की कुल्हाड़ी)", ToolType.KULHADI, ToolTier.STONE));
        register(Item.createTool("shilploka:copper_kulhadi", "Copper Kulhadi (तांबे की कुल्हाड़ी)", ToolType.KULHADI, ToolTier.COPPER));
        register(Item.createTool("shilploka:bronze_kulhadi", "Bronze Kulhadi (कांस्य की कुल्हाड़ी)", ToolType.KULHADI, ToolTier.BRONZE));
        register(Item.createTool("shilploka:loha_kulhadi", "Iron Kulhadi (लोहे की कुल्हाड़ी)", ToolType.KULHADI, ToolTier.IRON));
        register(Item.createTool("shilploka:wootz_kulhadi", "Wootz Kulhadi (वूट्ज़ कुल्हाड़ी)", ToolType.KULHADI, ToolTier.WOOTZ));
        register(Item.createTool("shilploka:vajra_kulhadi", "Vajra Kulhadi (वज्र कुल्हाड़ी)", ToolType.KULHADI, ToolTier.VAJRA));

        // Phaavda (Shovels)
        register(Item.createTool("shilploka:wood_phaavda", "Wood Phaavda (लकड़ी का फावड़ा)", ToolType.PHAAVDA, ToolTier.WOOD));
        register(Item.createTool("shilploka:stone_phaavda", "Stone Phaavda (पत्थर का फावड़ा)", ToolType.PHAAVDA, ToolTier.STONE));
        register(Item.createTool("shilploka:copper_phaavda", "Copper Phaavda (तांबे का फावड़ा)", ToolType.PHAAVDA, ToolTier.COPPER));
        register(Item.createTool("shilploka:bronze_phaavda", "Bronze Phaavda (कांस्य का फावड़ा)", ToolType.PHAAVDA, ToolTier.BRONZE));
        register(Item.createTool("shilploka:loha_phaavda", "Iron Phaavda (लोहे का फावड़ा)", ToolType.PHAAVDA, ToolTier.IRON));
        register(Item.createTool("shilploka:wootz_phaavda", "Wootz Phaavda (वूट्ज़ फावड़ा)", ToolType.PHAAVDA, ToolTier.WOOTZ));
        register(Item.createTool("shilploka:vajra_phaavda", "Vajra Phaavda (वज्र फावड़ा)", ToolType.PHAAVDA, ToolTier.VAJRA));

        // 5. Define Block Drop Mappings
        BLOCK_DROPS.put(BlockIds.KOYLA_ORE, "shilploka:koyla");
        BLOCK_DROPS.put(BlockIds.HIRA_ORE, "shilploka:hira");
        BLOCK_DROPS.put(BlockIds.ALLUVIAL_SOIL, "shilploka:alluvial_soil");
        BLOCK_DROPS.put(BlockIds.GRANITE, "shilploka:granite");
        BLOCK_DROPS.put(BlockIds.RED_SANDSTONE, "shilploka:red_sandstone");
        BLOCK_DROPS.put(BlockIds.YELLOW_SANDSTONE, "shilploka:yellow_sandstone");
        BLOCK_DROPS.put(BlockIds.THAR_SAND, "shilploka:thar_sand");
        BLOCK_DROPS.put(BlockIds.BAJRI, "shilploka:bajri");
        BLOCK_DROPS.put(BlockIds.FIRED_BRICK, "shilploka:fired_brick");
        BLOCK_DROPS.put(BlockIds.SAL_LOG, "shilploka:sal_log");
        BLOCK_DROPS.put(BlockIds.SAL_PLANKS, "shilploka:sal_planks");
        BLOCK_DROPS.put(BlockIds.DEODAR_LOG, "shilploka:deodar_log");
        BLOCK_DROPS.put(BlockIds.KHEJRI_LOG, "shilploka:khejri_log");
        BLOCK_DROPS.put(BlockIds.HIMALAYAN_SNOW, "shilploka:himalayan_snow");
        BLOCK_DROPS.put(BlockIds.DECCAN_BASALT, "shilploka:deccan_basalt");
        BLOCK_DROPS.put(BlockIds.DIYA, "shilploka:diya");
        BLOCK_DROPS.put(BlockIds.TAAMBA_ORE, "shilploka:taamba_ore");
        BLOCK_DROPS.put(BlockIds.LOHA_ORE, "shilploka:loha_ore");
        BLOCK_DROPS.put(BlockIds.SONA_ORE, "shilploka:sona_ore");
        BLOCK_DROPS.put(BlockIds.KARYASHALA, "shilploka:karyashala");
        BLOCK_DROPS.put(BlockIds.PAKKI_BHATTI, "shilploka:pakki_bhatti");
        BLOCK_DROPS.put(BlockIds.SMELTING_BHATTI, "shilploka:smelting_bhatti");
        BLOCK_DROPS.put(BlockIds.CHAAK, "shilploka:chaak");
        BLOCK_DROPS.put(BlockIds.KARGAH, "shilploka:kargah");
        BLOCK_DROPS.put(BlockIds.CHAKKI, "shilploka:chakki");
        BLOCK_DROPS.put(BlockIds.NIHAI, "shilploka:nihai");
        BLOCK_DROPS.put(BlockIds.KAANCH, "shilploka:kaanch");

        LOGGER.info("ItemRegistry initialized with {} items.", ITEMS_BY_ID.size());
    }

    private static Item register(Item item) {
        ITEMS_BY_ID.put(item.getId(), item);
        return item;
    }

    private static void registerBlock(int blockId, String id, String name) {
        Item item = Item.createBlockItem(id, name, blockId);
        ITEMS_BY_ID.put(id, item);
        ITEMS_BY_BLOCK_ID.put(blockId, item);
    }

    public static Item getItem(String id) {
        return ITEMS_BY_ID.get(id);
    }

    public static Item getItemByBlockId(int blockId) {
        return ITEMS_BY_BLOCK_ID.get(blockId);
    }

    /**
     * Determines what item drops when a block is mined.
     */
    public static ItemStack getBlockDrop(int blockId) {
        String dropId = BLOCK_DROPS.get(blockId);
        if (dropId == null) {
            Item blockItem = ITEMS_BY_BLOCK_ID.get(blockId);
            return (blockItem != null) ? new ItemStack(blockItem, 1) : ItemStack.EMPTY;
        }
        Item item = ITEMS_BY_ID.get(dropId);
        return (item != null) ? new ItemStack(item, 1) : ItemStack.EMPTY;
    }

    public static void init() {
        // Triggers static classloader initialization
    }
}
