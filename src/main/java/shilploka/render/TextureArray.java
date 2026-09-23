package shilploka.render;

import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * WHY:
 * Conventional 2D texture atlases suffer from mipmap bleeding at polygon boundaries
 * where distant sampling causes neighboring block textures to seep into edges.
 * GL_TEXTURE_2D_ARRAY completely eliminates edge bleeding because each 16x16 face
 * resides in an isolated 2D slice/layer of a 3D texture container.
 *
 * WHAT:
 * Allocates a 16x16 multi-layer texture array on the GPU, procedurally generates
 * authentic Indian mineral, wood, soil, and craft pixel-art textures (16x16 RGBA),
 * uploads them via glTexImage3D, and generates hardware mipmaps.
 *
 * USED FOR:
 * Bound to texture unit 0 during chunk rendering in ChunkMesh.render().
 *
 * WHERE NEXT:
 * The fragment shader chunk.frag samples from this array using vec3(UV.x, UV.y, LayerIndex).
 */
public class TextureArray {

    private static final Logger LOGGER = LoggerFactory.getLogger(TextureArray.class);

    public static final int TEXTURE_SIZE = 16;       // 16x16 pixels per block face
    public static final int TOTAL_LAYERS = 64;       // Total texture slices allocated

    // Static layer index constants matching block definitions
    public static final int LAYER_AIR = 0;
    public static final int LAYER_STONE = 1;
    public static final int LAYER_GRANITE = 2;
    public static final int LAYER_RED_SANDSTONE = 3;
    public static final int LAYER_YELLOW_SANDSTONE = 4;
    public static final int LAYER_DECCAN_BASALT = 5;
    public static final int LAYER_ALLUVIAL_SOIL = 6;
    public static final int LAYER_FIRED_BRICK = 7;
    public static final int LAYER_DIYA = 8;
    public static final int LAYER_SAL_LOG_SIDE = 9;
    public static final int LAYER_SAL_LOG_TOP = 10;
    public static final int LAYER_SAL_PLANKS = 11;
    public static final int LAYER_WATER = 12;
    public static final int LAYER_THAR_SAND = 13;
    public static final int LAYER_HIMALAYAN_SNOW = 14;
    public static final int LAYER_SAL_LEAVES = 15;
    public static final int LAYER_DEODAR_LEAVES = 16;
    public static final int LAYER_KHEJRI_LEAVES = 17;
    public static final int LAYER_KOYLA_ORE = 18;
    public static final int LAYER_TAAMBA_ORE = 19;
    public static final int LAYER_LOHA_ORE = 20;
    public static final int LAYER_RAJAVARTA_ORE = 21;
    public static final int LAYER_HIRA_ORE = 22;
    public static final int LAYER_LAVA = 23;          // Molten incandescent magma (लावा)
    public static final int LAYER_BAJRI = 24;         // River gravel & pebbles (बजरी)
    public static final int LAYER_WATER_FLOWING = 25; // Flowing river water stream (बहता जल)
    public static final int LAYER_DEODAR_LOG = 26;    // Himalayan Deodar Cedar bark (देवदार)
    public static final int LAYER_KHEJRI_LOG = 27;    // Thar Khejri desert rough bark (खेजड़ी)
    public static final int LAYER_SONA_ORE = 28;      // Gold ore in white quartz matrix (सोना)
    public static final int LAYER_KARYASHALA = 29;      // 3x3 Workbench top/sides (शिल्प कार्यशाला)
    public static final int LAYER_PAKKI_BHATTI = 30;    // Mud brick kiln furnace (पक्की भट्ठी)
    public static final int LAYER_SMELTING_BHATTI = 31; // Metallurgical alloy furnace (धातु प्रगलन भट्ठी)
    public static final int LAYER_CHAAK = 32;           // Potter's Wheel (कुम्हार का चाक)
    public static final int LAYER_KARGAH = 33;          // Handloom frame (करघा)
    public static final int LAYER_CHAKKI = 34;          // Stone mill rotary quern (हाथ की चक्की)
    public static final int LAYER_NIHAI = 35;           // Forged anvil surface (लोहार की निहाई)
    public static final int LAYER_KAANCH = 36;          // Architectural glass (कांच)
    public static final int LAYER_REGUR_SOIL = 37;      // Deccan Black Cotton Soil (रेगुर काली मिट्टी)
    public static final int LAYER_TEAK_LOG = 38;        // Sahyadri Teak Timber Log (सागवान का लट्ठा)
    public static final int LAYER_TEAK_LEAVES = 39;     // Sahyadri Teak Foliage (सागवान के पत्ते)
    public static final int LAYER_NAMAK_CRUST = 40;     // Rann of Kutch Salt Crust (कच्छ का नमक)
    public static final int LAYER_MANGROVE_LOG = 41;    // Sundarban Mangrove Log (सुंदरी का लट्ठा)
    public static final int LAYER_MANGROVE_LEAVES = 42; // Sundarban Mangrove Foliage (सुंदरी के पत्ते)
    public static final int LAYER_BAANS = 43;           // Poorvottar Bamboo Culm (बांस की शाखा)
    public static final int LAYER_SAMUDRA_SAND = 44;    // Coastal Marine Beach Sand (समुद्र तट की रेत)
    public static final int LAYER_CARVED_STONE = 45;    // Carved Mandapa Relief Stone (उत्कीर्ण प्रस्तर)
    public static final int LAYER_ANCIENT_CHEST = 46;   // Historic Relic Chest (प्राचीन मंजूषा)
    public static final int LAYER_ASHOKA_CHAKRA = 47;   // Ashokan Chakra Capital (अशोक चक्र)
    public static final int LAYER_SHILP_MANDAP = 48;    // Shilp Mandap Upgrade Altar (शिल्प मंडप)
    public static final int LAYER_TILLED_SOIL = 49;     // Furrowed moist alluvial soil (जुती हुई भूमि)
    public static final int LAYER_DHAN_CROP = 50;       // Paddy rice emerald stalks (धान)
    public static final int LAYER_GANNA_CROP = 51;      // Sugarcane green jointed cane (गन्ना)
    public static final int LAYER_KAPAAS_CROP = 52;     // Cotton bush with white fluffy bolls (कपास)
    public static final int LAYER_PAATAAL_AGNI_ROCK = 53;   // Volcanic fire rock / brimstone (अग्नि पाषाण)
    public static final int LAYER_PAATAAL_LAVA_GEL = 54;    // Magma crust / slag (ज्वालामुखी पिण्ड)
    public static final int LAYER_AKASH_MARBLE = 55;        // Celestial sky marble (आकाशीय प्रस्तर)
    public static final int LAYER_AKASH_CRYSTAL = 56;       // Luminous sky crystal (दिव्य मणि)
    public static final int LAYER_DIMENSIONAL_PORTAL = 57;  // Swirling cosmic portal gateway (आयाम द्वार)

    private final int textureId;

    /**
     * WHY:
     * Initializes the texture array and populates it with procedural 16x16 pixel art.
     *
     * WHAT:
     * Generates an OpenGL texture handle, binds GL_TEXTURE_2D_ARRAY, synthesizes
     * color bytes for each layer, and uploads the entire array.
     *
     * USED FOR:
     * Constructed once during Game startup.
     *
     * WHERE NEXT:
     * Bound to OpenGL texture unit 0 before rendering chunks.
     */
    public TextureArray() {
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, this.textureId);

        // Configure texture filtering: crisp pixel-art with smooth distant mipmapping
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Allocate native buffer for all layers: Width * Height * Layers * 4 bytes (RGBA)
        int totalBytes = TEXTURE_SIZE * TEXTURE_SIZE * TOTAL_LAYERS * 4;
        ByteBuffer buffer = MemoryUtil.memAlloc(totalBytes);

        try {
            populateProceduralTextures(buffer);
            buffer.flip();

            // Upload 3D texture slice data
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, TEXTURE_SIZE, TEXTURE_SIZE, TOTAL_LAYERS,
                    0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            // Generate mipmaps for smooth distance rendering
            glGenerateMipmap(GL_TEXTURE_2D_ARRAY);

            LOGGER.info("Successfully initialized GL_TEXTURE_2D_ARRAY [{}] ({}x{} pixels, {} layers)",
                    this.textureId, TEXTURE_SIZE, TEXTURE_SIZE, TOTAL_LAYERS);
        } finally {
            MemoryUtil.memFree(buffer);
        }

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    /**
     * WHY:
     * Fills the byte buffer with procedural pixel art for every block type.
     *
     * WHAT:
     * Computes RGBA values per pixel for all 16 layers, incorporating natural color
     * noise, brick mortar patterns, wood grains, and crystalline flecks.
     *
     * USED FOR:
     * Pre-loading block visuals before manual artwork replaces them.
     *
     * WHERE NEXT:
     * Bytes are uploaded to GPU memory via glTexImage3D.
     */
    private void populateProceduralTextures(ByteBuffer buffer) {
        Random rand = new Random(1337L); // Fixed seed for reproducible procedural textures

        for (int layer = 0; layer < TOTAL_LAYERS; layer++) {
            for (int y = 0; y < TEXTURE_SIZE; y++) {
                for (int x = 0; x < TEXTURE_SIZE; x++) {
                    int r = 0, g = 0, b = 0, a = 255;
                    float noise = (rand.nextFloat() - 0.5f) * 0.15f;

                    switch (layer) {
                        case LAYER_AIR -> {
                            // Layer 0: Transparent air
                            r = 0; g = 0; b = 0; a = 0;
                        }
                        case LAYER_STONE -> {
                            // Flaked grey stone: base #757575 with crystalline noise
                            int base = (int) (117 * (1.0f + noise));
                            r = clamp(base); g = clamp(base); b = clamp(base + 5);
                        }
                        case LAYER_GRANITE -> {
                            // Pinkish-grey granite: base #9E7D75 with feldspar flecks
                            float fleck = (rand.nextFloat() > 0.8f) ? 0.2f : noise;
                            r = clamp((int) (165 * (1.0f + fleck)));
                            g = clamp((int) (125 * (1.0f + fleck)));
                            b = clamp((int) (120 * (1.0f + fleck)));
                        }
                        case LAYER_RED_SANDSTONE -> {
                            // Warm Rajasthani red sandstone: #C85A32 with subtle sedimentary banding
                            float band = ((y % 4 == 0) ? -0.08f : 0.04f) + noise;
                            r = clamp((int) (195 * (1.0f + band)));
                            g = clamp((int) (88 * (1.0f + band)));
                            b = clamp((int) (55 * (1.0f + band)));
                        }
                        case LAYER_YELLOW_SANDSTONE -> {
                            // Golden Jaisalmer sandstone: #D4B87E
                            float var = noise * 0.8f;
                            r = clamp((int) (212 * (1.0f + var)));
                            g = clamp((int) (184 * (1.0f + var)));
                            b = clamp((int) (126 * (1.0f + var)));
                        }
                        case LAYER_DECCAN_BASALT -> {
                            // Dark columnar basalt: #2E2E33 with subtle vertical fissures
                            float column = (x % 4 == 0) ? -0.15f : noise;
                            int grey = clamp((int) (50 * (1.0f + column)));
                            r = grey; g = grey; b = grey + 4;
                        }
                        case LAYER_ALLUVIAL_SOIL -> {
                            // Fertile deep Gangetic loam: #523B2A with organic matter
                            float humus = noise * 1.2f;
                            r = clamp((int) (82 * (1.0f + humus)));
                            g = clamp((int) (59 * (1.0f + humus)));
                            b = clamp((int) (42 * (1.0f + humus)));
                        }
                        case LAYER_FIRED_BRICK -> {
                            // Harappan baked brick: terracotta bricks (#BD5338) with grey mortar lines (#7A7570)
                            boolean isMortar = (y % 4 == 3) || ((y / 4) % 2 == 0 ? (x % 8 == 7) : (x % 8 == 3));
                            if (isMortar) {
                                r = 125; g = 120; b = 115; // Mortar
                            } else {
                                r = clamp((int) (185 * (1.0f + noise)));
                                g = clamp((int) (80 * (1.0f + noise)));
                                b = clamp((int) (55 * (1.0f + noise)));
                            }
                        }
                        case LAYER_DIYA -> {
                            // Clay Diya: terracotta dish with warm golden center flame
                            int distFromCenter = Math.max(Math.abs(x - 8), Math.abs(y - 8));
                            if (distFromCenter <= 2) {
                                // Golden flame core
                                r = 255; g = 220; b = 60;
                            } else if (distFromCenter <= 4) {
                                // Warm orange flame glow
                                r = 240; g = 130; b = 30;
                            } else {
                                // Terracotta lamp dish
                                r = clamp((int) (175 * (1.0f + noise)));
                                g = clamp((int) (80 * (1.0f + noise)));
                                b = clamp((int) (50 * (1.0f + noise)));
                            }
                        }
                        case LAYER_SAL_LOG_SIDE -> {
                            // Sal tree bark: deep grey-brown with vertical furrow striations
                            float furrow = (x % 3 == 0) ? -0.18f : noise;
                            r = clamp((int) (90 * (1.0f + furrow)));
                            g = clamp((int) (70 * (1.0f + furrow)));
                            b = clamp((int) (55 * (1.0f + furrow)));
                        }
                        case LAYER_SAL_LOG_TOP -> {
                            // Sal tree log cross-section: concentric heartwood growth rings
                            double dist = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                            boolean isRing = ((int) dist) % 2 == 0;
                            float ringMod = isRing ? -0.1f : 0.08f;
                            r = clamp((int) (150 * (1.0f + ringMod + noise)));
                            g = clamp((int) (120 * (1.0f + ringMod + noise)));
                            b = clamp((int) (90 * (1.0f + ringMod + noise)));
                        }
                        case LAYER_SAL_PLANKS -> {
                            // Sawn planks: clean horizontal grain with border seam
                            boolean isSeam = (y % 4 == 0);
                            float seamMod = isSeam ? -0.2f : noise;
                            r = clamp((int) (165 * (1.0f + seamMod)));
                            g = clamp((int) (130 * (1.0f + seamMod)));
                            b = clamp((int) (95 * (1.0f + seamMod)));
                        }
                        case LAYER_WATER -> {
                            // Translucent turquoise ocean/river water
                            r = 30; g = 140; b = 180; a = 200;
                        }
                        case LAYER_THAR_SAND -> {
                            // Golden Thar desert dunes: #DEB352 with subtle wind ripple striations
                            boolean isRipple = ((x + y * 2) % 6 == 0);
                            float rippleMod = isRipple ? -0.08f : (noise * 0.7f);
                            r = clamp((int) (222 * (1.0f + rippleMod)));
                            g = clamp((int) (179 * (1.0f + rippleMod)));
                            b = clamp((int) (82 * (1.0f + rippleMod)));
                        }
                        case LAYER_HIMALAYAN_SNOW -> {
                            // Pristine Himalayan glacial snow: #F0F6FC with subtle blue glints
                            float crystal = (rand.nextFloat() > 0.85f) ? 0.05f : (noise * 0.3f);
                            r = clamp((int) (240 * (1.0f + crystal)));
                            g = clamp((int) (246 * (1.0f + crystal)));
                            b = clamp((int) (252 * (1.0f + crystal)));
                        }
                        case LAYER_SAL_LEAVES -> {
                            // Lush Gangetic Sal tree broadleaf: #3D6B2A with vein highlights
                            boolean isVein = (x == y || x == 15 - y || (x % 4 == 0 && y % 4 == 0));
                            if (isVein) {
                                r = clamp((int) (94 * (1.0f + noise)));
                                g = clamp((int) (150 * (1.0f + noise)));
                                b = clamp((int) (60 * (1.0f + noise)));
                            } else {
                                r = clamp((int) (61 * (1.0f + noise)));
                                g = clamp((int) (107 * (1.0f + noise)));
                                b = clamp((int) (42 * (1.0f + noise)));
                            }
                        }
                        case LAYER_DEODAR_LEAVES -> {
                            // High-altitude Himalayan cedar needles: deep blue-green #23432B
                            boolean needleTip = (rand.nextFloat() > 0.7f);
                            float mod = needleTip ? 0.15f : noise;
                            r = clamp((int) (35 * (1.0f + mod)));
                            g = clamp((int) (67 * (1.0f + mod)));
                            b = clamp((int) (43 * (1.0f + mod + 0.05f)));
                        }
                        case LAYER_KHEJRI_LEAVES -> {
                            // Arid Thar desert Khejri foliage: dusty sage olive #6E7D4E
                            boolean spine = (x % 5 == 0 && y % 5 == 0);
                            if (spine) {
                                r = 160; g = 175; b = 130; // Pale thorn/leaflet highlight
                            } else {
                                r = clamp((int) (110 * (1.0f + noise)));
                                g = clamp((int) (125 * (1.0f + noise)));
                                b = clamp((int) (78 * (1.0f + noise)));
                            }
                        }
                        case LAYER_KOYLA_ORE -> {
                            // Stone host rock with rough jet-black bituminous coal patches
                            boolean isCoal = ((x / 3 + y / 3) % 2 == 0) && (rand.nextFloat() > 0.35f);
                            if (isCoal) {
                                int coalVal = clamp((int) (30 * (1.0f + noise)));
                                r = coalVal; g = coalVal; b = coalVal + 2;
                            } else {
                                int base = (int) (117 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 5);
                            }
                        }
                        case LAYER_TAAMBA_ORE -> {
                            // Stone host rock with metallic copper flecks and green malachite patina
                            float oreSample = rand.nextFloat();
                            if (oreSample > 0.75f) {
                                // Metallic copper
                                r = clamp((int) (214 * (1.0f + noise)));
                                g = clamp((int) (104 * (1.0f + noise)));
                                b = clamp((int) (56 * (1.0f + noise)));
                            } else if (oreSample > 0.65f) {
                                // Malachite green oxidation
                                r = clamp((int) (60 * (1.0f + noise)));
                                g = clamp((int) (168 * (1.0f + noise)));
                                b = clamp((int) (102 * (1.0f + noise)));
                            } else {
                                int base = (int) (117 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 5);
                            }
                        }
                        case LAYER_LOHA_ORE -> {
                            // Stone host rock with reddish hematite bands and metallic iron sheen
                            boolean isIron = (x + y * 2) % 4 == 0 && rand.nextFloat() > 0.3f;
                            if (isIron) {
                                r = clamp((int) (140 * (1.0f + noise)));
                                g = clamp((int) (68 * (1.0f + noise)));
                                b = clamp((int) (56 * (1.0f + noise)));
                            } else {
                                int base = (int) (117 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 5);
                            }
                        }
                        case LAYER_RAJAVARTA_ORE -> {
                            // Deep royal ultramarine lapis lazuli with sparkling golden pyrite flecks
                            float oreRoll = rand.nextFloat();
                            if (oreRoll > 0.82f) {
                                // Golden pyrite crystal specks
                                r = clamp((int) (230 * (1.0f + noise)));
                                g = clamp((int) (185 * (1.0f + noise)));
                                b = clamp((int) (55 * (1.0f + noise)));
                            } else if (oreRoll > 0.65f) {
                                // Ultramarine lapis lazuli
                                r = clamp((int) (28 * (1.0f + noise)));
                                g = clamp((int) (62 * (1.0f + noise)));
                                b = clamp((int) (165 * (1.0f + noise)));
                            } else {
                                int base = (int) (117 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 5);
                            }
                        }
                        case LAYER_HIRA_ORE -> {
                            // Basaltic kimberlite host rock (#323238) with brilliant diamond facets
                            boolean isDiamond = (Math.abs(x - 8) + Math.abs(y - 8) <= 3) && rand.nextFloat() > 0.4f;
                            if (isDiamond) {
                                // Crystalline diamond facets catching light
                                float sparkle = rand.nextFloat();
                                if (sparkle > 0.5f) {
                                    r = 245; g = 252; b = 255; // Pure brilliant white glint
                                } else {
                                    r = 168; g = 236; b = 240; // Pale cyan ice-blue refractive facet
                                }
                            } else {
                                // Dark kimberlite basalt
                                int base = (int) (50 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 4);
                            }
                        }
                        case LAYER_LAVA -> {
                            // Molten magma: incandescent orange-yellow cores with dark hardening crust ridges
                            boolean crust = ((x + y * 3) % 5 == 0) || ((x * 2 + y) % 7 == 0);
                            if (crust) {
                                // Dark cooling basalt crust
                                int crustBase = (int) (70 * (1.0f + noise));
                                r = clamp(crustBase + 40); g = clamp(crustBase / 3); b = 15;
                            } else {
                                // Glowing incandescent magma
                                float heat = rand.nextFloat();
                                if (heat > 0.65f) {
                                    r = 255; g = 215; b = 40; // Bright yellow-gold heat
                                } else {
                                    r = 235; g = 100 + (int)(noise * 50); b = 15; // Deep orange
                                }
                            }
                        }
                        case LAYER_BAJRI -> {
                            // River gravel & pebbles: natural mottled grey, silt, and quartz speckles
                            int pebble = ((x * 3 + y * 7 + (x ^ y)) & 7);
                            int base = 120 + (int)(noise * 60);
                            if (pebble == 1) {
                                r = clamp(base + 25); g = clamp(base + 25); b = clamp(base + 30); // Quartz fleck
                            } else if (pebble == 2) {
                                r = clamp(base - 30); g = clamp(base - 30); b = clamp(base - 25); // Dark river stone
                            } else {
                                r = clamp(base); g = clamp(base - 2); b = clamp(base - 5); // Natural silt gravel
                            }
                        }
                        case LAYER_WATER_FLOWING -> {
                            // Flowing river stream: translucent blue with animated flow ripples
                            boolean ripple = ((x + y * 2) % 4 == 0);
                            r = ripple ? 45 : 30;
                            g = ripple ? 125 : 100;
                            b = ripple ? 230 : 210;
                            a = 210; // Translucent
                        }
                        case LAYER_DEODAR_LOG -> {
                            // Himalayan Deodar Cedar bark: deep reddish-brown aromatic bark with vertical fissures
                            boolean fissure = (x % 4 == 0) || ((x + (y / 4)) % 5 == 0);
                            if (fissure) {
                                r = clamp((int) (60 * (1.0f + noise)));
                                g = clamp((int) (38 * (1.0f + noise)));
                                b = clamp((int) (22 * (1.0f + noise)));
                            } else {
                                r = clamp((int) (105 * (1.0f + noise)));
                                g = clamp((int) (65 * (1.0f + noise)));
                                b = clamp((int) (40 * (1.0f + noise)));
                            }
                        }
                        case LAYER_KHEJRI_LOG -> {
                            // Thar Desert Khejri bark: rough, weathered grey-brown fibrous trunk
                            boolean fiber = ((x * 2 + y) % 3 == 0);
                            if (fiber) {
                                r = clamp((int) (85 * (1.0f + noise)));
                                g = clamp((int) (75 * (1.0f + noise)));
                                b = clamp((int) (60 * (1.0f + noise)));
                            } else {
                                r = clamp((int) (120 * (1.0f + noise)));
                                g = clamp((int) (108 * (1.0f + noise)));
                                b = clamp((int) (90 * (1.0f + noise)));
                            }
                        }
                        case LAYER_SONA_ORE -> {
                            // Gold ore: white quartz stone matrix with lustrous golden nuggets
                            boolean isGold = ((x * 3 + y * 5) % 7 == 0) && rand.nextFloat() > 0.35f;
                            if (isGold) {
                                r = clamp((int) (245 * (1.0f + noise)));
                                g = clamp((int) (200 * (1.0f + noise)));
                                b = clamp((int) (35 * (1.0f + noise)));
                            } else {
                                int base = (int) (160 * (1.0f + noise));
                                r = clamp(base + 10); g = clamp(base + 10); b = clamp(base + 12);
                            }
                        }
                        case LAYER_KARYASHALA -> {
                            // Carved Teak Workbench with brass corner brackets and chiseling grid
                            boolean isCorner = (x <= 1 && y <= 1) || (x >= 14 && y <= 1) || (x <= 1 && y >= 14) || (x >= 14 && y >= 14);
                            boolean isGrid = (x == 7 || y == 7);
                            if (isCorner) {
                                // Brass corner bracket (#E0A838)
                                r = clamp((int) (224 * (1.0f + noise)));
                                g = clamp((int) (168 * (1.0f + noise)));
                                b = clamp((int) (56 * (1.0f + noise)));
                            } else if (isGrid) {
                                // Inlaid wood scoring groove
                                r = clamp((int) (70 * (1.0f + noise)));
                                g = clamp((int) (40 * (1.0f + noise)));
                                b = clamp((int) (20 * (1.0f + noise)));
                            } else {
                                // Rich polished teak grain
                                r = clamp((int) (125 * (1.0f + noise)));
                                g = clamp((int) (75 * (1.0f + noise)));
                                b = clamp((int) (42 * (1.0f + noise)));
                            }
                        }
                        case LAYER_PAKKI_BHATTI -> {
                            // Terracotta Kiln: mud bricks with fiery arched opening at bottom
                            boolean isFireArch = (y >= 8 && x >= 4 && x <= 11);
                            if (isFireArch) {
                                // Glowing thermal kiln coals
                                r = clamp((int) (240 + noise * 15));
                                g = clamp((int) (110 + noise * 40));
                                b = 20;
                            } else {
                                // Harappan terracotta fired mud brick (#B05B3B)
                                boolean mortar = (y % 4 == 0) || ((x + (y / 4) * 4) % 6 == 0);
                                if (mortar) {
                                    r = 135; g = 115; b = 95;
                                } else {
                                    r = clamp((int) (176 * (1.0f + noise)));
                                    g = clamp((int) (91 * (1.0f + noise)));
                                    b = clamp((int) (59 * (1.0f + noise)));
                                }
                            }
                        }
                        case LAYER_SMELTING_BHATTI -> {
                            // Metallurgical Alloy Smelter: Deccan Basalt masonry with crucible glow
                            boolean isCrucible = (y >= 6 && y <= 11 && x >= 5 && x <= 10);
                            if (isCrucible) {
                                r = clamp((int) (255 * (1.0f + noise)));
                                g = clamp((int) (180 * (1.0f + noise)));
                                b = 30; // Intense molten yellow-white heat
                            } else {
                                // Dark heat-tempered basalt masonry
                                int base = (int) (65 * (1.0f + noise));
                                r = clamp(base); g = clamp(base); b = clamp(base + 5);
                            }
                        }
                        case LAYER_CHAAK -> {
                            // Potter's Wheel: circular stone ring with wet brown clay center
                            int distSq = (x - 8) * (x - 8) + (y - 8) * (y - 8);
                            if (distSq <= 9) {
                                // Moist earthenware clay (#7A4E2D)
                                r = clamp((int) (122 * (1.0f + noise)));
                                g = clamp((int) (78 * (1.0f + noise)));
                                b = clamp((int) (45 * (1.0f + noise)));
                            } else if (distSq <= 49) {
                                // Spinning stone disc (#9E988E)
                                int stone = (int) (158 * (1.0f + noise));
                                r = clamp(stone); g = clamp(stone - 5); b = clamp(stone - 15);
                            } else {
                                // Wooden base mount
                                r = clamp((int) (95 * (1.0f + noise)));
                                g = clamp((int) (60 * (1.0f + noise)));
                                b = clamp((int) (35 * (1.0f + noise)));
                            }
                        }
                        case LAYER_KARGAH -> {
                            // Loom: vertical cream cotton warp threads stretched on wooden frame
                            boolean isFrame = (x <= 1 || x >= 14 || y <= 1 || y >= 14);
                            if (isFrame) {
                                r = clamp((int) (105 * (1.0f + noise)));
                                g = clamp((int) (65 * (1.0f + noise)));
                                b = clamp((int) (38 * (1.0f + noise)));
                            } else {
                                // Spun cotton threads (alternating warp highlights)
                                boolean thread = (x % 2 == 0);
                                int threadCol = thread ? 238 : 210;
                                r = clamp((int) (threadCol * (1.0f + noise * 0.2f)));
                                g = clamp((int) ((threadCol - 8) * (1.0f + noise * 0.2f)));
                                b = clamp((int) ((threadCol - 18) * (1.0f + noise * 0.2f)));
                            }
                        }
                        case LAYER_CHAKKI -> {
                            // Stone Mill: heavy carved sandstone disc with grinding grooves and wood peg
                            boolean isPeg = (x == 11 && y == 8);
                            int distCenter = (x - 7) * (x - 7) + (y - 7) * (y - 7);
                            if (isPeg) {
                                r = 60; g = 35; b = 18; // Dark wood turning peg
                            } else if (distCenter <= 2) {
                                r = 40; g = 38; b = 35; // Center feed hole
                            } else {
                                // Chiseled grinding grooves
                                boolean groove = ((x + y) % 3 == 0);
                                int base = groove ? 130 : 160;
                                r = clamp((int) (base * (1.0f + noise)));
                                g = clamp((int) (base * (1.0f + noise)));
                                b = clamp((int) ((base - 10) * (1.0f + noise)));
                            }
                        }
                        case LAYER_NIHAI -> {
                            // Forged Wootz Anvil: deep gunmetal grey with polished top face
                            boolean isFace = (y >= 3 && y <= 6 && x >= 2 && x <= 13);
                            if (isFace) {
                                // Polished striking surface with metallic sheen
                                int sheen = (int) (120 * (1.0f + noise));
                                r = clamp(sheen); g = clamp(sheen + 5); b = clamp(sheen + 12);
                            } else {
                                // Dark forged steel body
                                int steel = (int) (52 * (1.0f + noise));
                                r = clamp(steel); g = clamp(steel); b = clamp(steel + 6);
                            }
                        }
                        case LAYER_KAANCH -> {
                            // Silica Glass: translucent crystal with diagonal glare streaks
                            boolean glare = (x + y == 8) || (x + y == 9) || (x + y == 22);
                            r = glare ? 235 : 180;
                            g = glare ? 250 : 215;
                            b = glare ? 255 : 235;
                            a = glare ? 160 : 75; // Translucent
                        }
                        case LAYER_REGUR_SOIL -> {
                            // Deccan Black Cotton Soil: rich organic charcoal black loam #282422
                            int blackEarth = (int) (42 * (1.0f + noise));
                            r = clamp(blackEarth); g = clamp(blackEarth - 2); b = clamp(blackEarth - 4);
                        }
                        case LAYER_TEAK_LOG -> {
                            // Sahyadri Teak Log: deep golden-brown hardwood bark #4A3319
                            int bark = (int) (68 * (1.0f + ((x % 3 == 0) ? -0.1f : 0.05f) + noise));
                            r = clamp(bark + 10); g = clamp(bark - 8); b = clamp(bark - 22);
                        }
                        case LAYER_TEAK_LEAVES -> {
                            // Tropical Teak Foliage: lush rainforest green with leaf veins
                            boolean vein = (x == y || x + y == 15);
                            r = vein ? 65 : clamp((int) (45 * (1.0f + noise)));
                            g = vein ? 120 : clamp((int) (95 * (1.0f + noise)));
                            b = vein ? 35 : clamp((int) (28 * (1.0f + noise)));
                            a = (rand.nextFloat() > 0.15f) ? 255 : 0; // Cutout foliage
                        }
                        case LAYER_NAMAK_CRUST -> {
                            // Rann of Kutch Salt Crust: bright white crystal with specular sparkles
                            boolean sparkle = rand.nextFloat() > 0.85f;
                            int salt = sparkle ? 255 : (int) (230 * (1.0f + noise * 0.4f));
                            r = clamp(salt); g = clamp(salt + 2); b = clamp(salt + 5);
                        }
                        case LAYER_MANGROVE_LOG -> {
                            // Sundarban Mangrove Log: dark salt-drenched timber #383025
                            int wood = (int) (55 * (1.0f + noise));
                            r = clamp(wood); g = clamp(wood - 5); b = clamp(wood - 12);
                        }
                        case LAYER_MANGROVE_LEAVES -> {
                            // Dense mangrove canopy: dark waxy evergreen leaves #1E4620
                            r = clamp((int) (30 * (1.0f + noise)));
                            g = clamp((int) (70 * (1.0f + noise)));
                            b = clamp((int) (32 * (1.0f + noise)));
                            a = (rand.nextFloat() > 0.18f) ? 255 : 0;
                        }
                        case LAYER_BAANS -> {
                            // Bamboo Culm: smooth lime-green cane with horizontal node rings
                            boolean node = (y % 8 == 0);
                            int green = node ? 115 : (int) (155 * (1.0f + noise * 0.3f));
                            r = node ? 110 : 120;
                            g = clamp(green);
                            b = node ? 45 : 60;
                        }
                        case LAYER_SAMUDRA_SAND -> {
                            // Marine Coastal Sand: fine sunlit golden beach grains #E8D49E
                            r = clamp((int) (228 * (1.0f + noise * 0.5f)));
                            g = clamp((int) (210 * (1.0f + noise * 0.5f)));
                            b = clamp((int) (155 * (1.0f + noise * 0.5f)));
                        }
                        case LAYER_CARVED_STONE -> {
                            // Carved Stone: Chola/Maurya stone relief with decorative concentric border
                            boolean border = (x == 1 || x == 14 || y == 1 || y == 14);
                            boolean motif = ((x - 8) * (x - 8) + (y - 8) * (y - 8) <= 9);
                            int base = border ? 120 : (motif ? 175 : (int) (140 * (1.0f + noise * 0.4f)));
                            r = clamp(base + 5);
                            g = clamp(base);
                            b = clamp(base - 5);
                        }
                        case LAYER_ANCIENT_CHEST -> {
                            // Ancient Relic Chest: Terracotta & aged bronze banding with lock latch
                            boolean band = (y == 3 || y == 12 || x == 3 || x == 12);
                            boolean lock = (x >= 7 && x <= 8 && y >= 6 && y <= 9);
                            if (lock) {
                                r = 218; g = 165; b = 32; // Gold/bronze latch
                            } else if (band) {
                                r = 90; g = 110; b = 80;  // Verdigris bronze trim
                            } else {
                                r = clamp((int) (165 * (1.0f + noise * 0.4f))); // Terracotta body
                                g = clamp((int) (85 * (1.0f + noise * 0.4f)));
                                b = clamp((int) (55 * (1.0f + noise * 0.4f)));
                            }
                        }
                        case LAYER_ASHOKA_CHAKRA -> {
                            // Ashokan Lion Capital & Dharma Chakra: Polished sandstone with deep blue wheel
                            int dx = x - 8;
                            int dy = y - 8;
                            int distSq = dx * dx + dy * dy;
                            if (distSq <= 16 && (distSq >= 9 || (dx == 0 || dy == 0 || Math.abs(dx) == Math.abs(dy)))) {
                                r = 24; g = 44; b = 140; // Navy Blue 24-spoke Chakra #182C8C
                            } else {
                                r = clamp((int) (210 * (1.0f + noise * 0.3f))); // Polished sandstone buff
                                g = clamp((int) (185 * (1.0f + noise * 0.3f)));
                                b = clamp((int) (145 * (1.0f + noise * 0.3f)));
                            }
                        }
                        case LAYER_SHILP_MANDAP -> {
                            // Shilp Mandap Altar: Deep basalt plinth with glowing lapis/gold lotus mandala
                            boolean centerLotus = (Math.hypot(x - 7.5, y - 7.5) <= 4.0);
                            if (centerLotus) {
                                r = 30; g = 90; b = 220; // Glowing Lapis Lazuli
                            } else if (x == 0 || x == 15 || y == 0 || y == 15) {
                                r = 218; g = 165; b = 32; // Gold filigree rim
                            } else {
                                r = 45; g = 45; b = 50;  // Basalt foundation
                            }
                        }
                        case LAYER_TILLED_SOIL -> {
                            // Furrowed moist alluvial soil: horizontal cultivation ridges
                            boolean furrow = (y % 4 == 0);
                            int base = furrow ? 60 : 90;
                            r = clamp((int) (base * 1.1f * (1.0f + noise * 0.3f)));
                            g = clamp((int) (base * 0.75f * (1.0f + noise * 0.3f)));
                            b = clamp((int) (base * 0.45f * (1.0f + noise * 0.3f)));
                        }
                        case LAYER_DHAN_CROP -> {
                            // Paddy rice crop: slender vibrant emerald green leaves
                            boolean stalk = (x % 3 == 0 || y > 10);
                            if (stalk) {
                                r = 40; g = clamp((int) (190 * (1.0f + noise * 0.2f))); b = 30;
                            } else {
                                a = 0; // Translucent foliage
                            }
                        }
                        case LAYER_GANNA_CROP -> {
                            // Sugarcane reed: segmented bamboo-green stalks
                            boolean ring = (y % 5 == 0);
                            r = ring ? 130 : 70;
                            g = ring ? 180 : 160;
                            b = ring ? 40 : 35;
                        }
                        case LAYER_KAPAAS_CROP -> {
                            // Cotton bush: green foliage with white fluffy cotton bolls
                            boolean boll = ((x >= 5 && x <= 10) && (y >= 5 && y <= 10) && Math.hypot(x - 7.5, y - 7.5) <= 3.0);
                            if (boll) {
                                r = 245; g = 245; b = 245; // Pure white cotton
                            } else {
                                r = 40; g = 120; b = 35;   // Green leaves
                            }
                        }
                        case LAYER_PAATAAL_AGNI_ROCK -> {
                            // Deep brimstone crimson and charcoal with fire specks
                            boolean ember = ((x * 3 + y * 7) % 5 == 0);
                            int var = (int) (noise * 100);
                            r = clamp(ember ? 220 + var : 90 + var);
                            g = clamp(ember ? 60 + var : 25 + var);
                            b = clamp(ember ? 20 : 25 + var);
                            a = 255;
                        }
                        case LAYER_PAATAAL_LAVA_GEL -> {
                            // Dark obsidian slag crust with fiery glowing veins
                            boolean fissure = (x == y || x + y == 15 || (x * y) % 7 == 0);
                            int var = (int) (noise * 100);
                            r = clamp(fissure ? 255 : 45 + var);
                            g = clamp(fissure ? 140 + var : 25 + var);
                            b = clamp(fissure ? 20 : 30 + var);
                            a = 255;
                        }
                        case LAYER_AKASH_MARBLE -> {
                            // Pristine ivory marble with delicate celestial azure striations
                            boolean azureVein = ((x * 2 + y) % 7 == 0);
                            int var = (int) (noise * 50);
                            r = clamp(azureVein ? 210 : 245 + var);
                            g = clamp(azureVein ? 230 : 245 + var);
                            b = 255;
                            a = 255;
                        }
                        case LAYER_AKASH_CRYSTAL -> {
                            // Luminous iridescent cyan and divine gold crystal
                            boolean core = (x >= 4 && x <= 11 && y >= 4 && y <= 11);
                            int var = (int) (noise * 100);
                            r = clamp(core ? 180 + var : 100 + var);
                            g = clamp(core ? 240 + var : 210 + var);
                            b = 255;
                            a = 220;
                        }
                        case LAYER_DIMENSIONAL_PORTAL -> {
                            // Swirling cosmic astral gateway: violet, magenta, and celestial gold
                            int distFromCenter = Math.abs(x - 8) + Math.abs(y - 8);
                            boolean swirl = ((distFromCenter + x * 2) % 4 == 0);
                            int var = (int) (noise * 100);
                            r = clamp(swirl ? 210 : 120 + var);
                            g = clamp(swirl ? 80 : 30 + var);
                            b = clamp(swirl ? 240 : 200 + var);
                            a = 210;
                        }
                        default -> {
                            // Default filler texture
                            r = 100; g = 100; b = 100; a = 255;
                        }
                    }

                    buffer.put((byte) r);
                    buffer.put((byte) g);
                    buffer.put((byte) b);
                    buffer.put((byte) a);
                }
            }
        }
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }

    /**
     * WHY:
     * Binds this texture array to the active OpenGL texture unit.
     *
     * WHAT:
     * Calls glBindTexture(GL_TEXTURE_2D_ARRAY, textureId).
     *
     * USED FOR:
     * Invoked prior to rendering voxel chunks.
     */
    public void bind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, this.textureId);
    }

    /**
     * WHY:
     * Unbinds the texture array from the active OpenGL texture unit.
     *
     * WHAT:
     * Calls glBindTexture(GL_TEXTURE_2D_ARRAY, 0).
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    /**
     * WHY:
     * Frees the OpenGL texture memory upon game termination.
     *
     * WHAT:
     * Calls glDeleteTextures.
     */
    public void cleanup() {
        glDeleteTextures(this.textureId);
        LOGGER.info("Disposed GL_TEXTURE_2D_ARRAY [{}]", this.textureId);
    }
}
