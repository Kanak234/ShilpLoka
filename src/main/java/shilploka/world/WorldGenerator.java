package shilploka.world;

import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.biome.BiomeProvider;
import shilploka.worldgen.biome.BiomeType;
import shilploka.worldgen.cave.CaveCarver;
import shilploka.worldgen.feature.TreeGenerator;
import shilploka.worldgen.noise.OctaveNoise;
import shilploka.worldgen.ore.OrePopulator;

import java.util.Random;

/**
 * WHY:
 * Generates continuous, deterministic 3D voxel terrain across infinite horizontal chunk columns
 * (height 0 to 320) using regional Indian biomes, multi-octave OpenSimplex2 Fractal Brownian Motion,
 * 3D subterranean cave carvers, mineral ore veins, and indigenous trees.
 *
 * WHAT:
 * - Evaluates climate noise fields for Ganga Maidan, Thar Marusthal, and Himalaya Shikhar.
 * - Fills geological strata from Deccan Basalt bedrock (Y=0..3) up to biome surface sediments.
 * - Carves 3D subterranean worm caves and cavern rooms (Y=8..56).
 * - Distributes 5 Indian mineral ores (Koyla, Taamba, Loha, Rajavarta, Hira).
 * - Generates procedural Sal, Deodar cedar, and Khejri trees.
 * - Preserves the foundational Harappan Brick Pavilion at world origin (0, 0).
 *
 * USED FOR:
 * Invoked asynchronously by background worker threads in ChunkManager.
 *
 * WHERE NEXT:
 * The populated ChunkColumn sections are passed to GreedyMesher for background polygon meshing.
 */
public class WorldGenerator {

    // Backwards-compatible Block ID delegates referencing BlockIds catalogue
    public static final int BLOCK_AIR = BlockIds.AIR;
    public static final int BLOCK_STONE = BlockIds.STONE;
    public static final int BLOCK_GRANITE = BlockIds.GRANITE;
    public static final int BLOCK_RED_SANDSTONE = BlockIds.RED_SANDSTONE;
    public static final int BLOCK_ALLUVIAL_SOIL = BlockIds.ALLUVIAL_SOIL;
    public static final int BLOCK_YELLOW_SANDSTONE = BlockIds.YELLOW_SANDSTONE;
    public static final int BLOCK_DECCAN_BASALT = BlockIds.DECCAN_BASALT;
    public static final int BLOCK_FIRED_BRICK = BlockIds.FIRED_BRICK;
    public static final int BLOCK_DIYA = BlockIds.DIYA;
    public static final int BLOCK_SAL_LOG = BlockIds.SAL_LOG;
    public static final int BLOCK_SAL_PLANKS = BlockIds.SAL_PLANKS;
    public static final int BLOCK_WATER = BlockIds.WATER;
    public static final int BLOCK_THAR_SAND = BlockIds.THAR_SAND;
    public static final int BLOCK_HIMALAYAN_SNOW = BlockIds.HIMALAYAN_SNOW;
    public static final int BLOCK_SAL_LEAVES = BlockIds.SAL_LEAVES;
    public static final int BLOCK_DEODAR_LEAVES = BlockIds.DEODAR_LEAVES;
    public static final int BLOCK_KHEJRI_LEAVES = BlockIds.KHEJRI_LEAVES;
    public static final int BLOCK_KOYLA_ORE = BlockIds.KOYLA_ORE;
    public static final int BLOCK_TAAMBA_ORE = BlockIds.TAAMBA_ORE;
    public static final int BLOCK_LOHA_ORE = BlockIds.LOHA_ORE;
    public static final int BLOCK_RAJAVARTA_ORE = BlockIds.RAJAVARTA_ORE;
    public static final int BLOCK_HIRA_ORE = BlockIds.HIRA_ORE;

    private final long seed;
    private final BiomeProvider biomeProvider;
    private final OctaveNoise continentalNoise;
    private final OctaveNoise detailNoise;
    private final CaveCarver caveCarver;
    private final OrePopulator orePopulator;
    private final shilploka.worldgen.structure.HistoricalStructureManager structureManager;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.biomeProvider = new BiomeProvider(seed);
        this.continentalNoise = new OctaveNoise(seed + 1000L, 4, 0.5, 2.0);
        this.detailNoise = new OctaveNoise(seed + 2000L, 3, 0.5, 2.0);
        this.caveCarver = new CaveCarver(seed);
        this.orePopulator = new OrePopulator(seed);
        this.structureManager = new shilploka.worldgen.structure.HistoricalStructureManager(seed);
    }

    /**
     * WHY:
     * Generates a complete 16x320x16 vertical ChunkColumn for world coordinates (chunkX, chunkZ).
     *
     * WHAT:
     * Executes the multi-stage world generation pipeline:
     * 1. Strata & Surface Generation
     * 2. 3D Cave Carving
     * 3. Mineral Ore Distribution
     * 4. Procedural Indigenous Flora Placement
     * 5. Origin Monument Construction (at 0, 0)
     */
    public ChunkColumn generateChunkColumn(int chunkX, int chunkZ) {
        ChunkColumn column = new ChunkColumn(chunkX, chunkZ);
        int[][] heightMap = new int[16][16];
        BiomeType[][] biomeMap = new BiomeType[16][16];

        // 1. First Pass: Bedrock, geological strata, and biome surface sediments
        for (int x = 0; x < 16; x++) {
            int worldX = (chunkX * 16) + x;
            for (int z = 0; z < 16; z++) {
                int worldZ = (chunkZ * 16) + z;

                BiomeType biome = biomeProvider.getBiomeAt(worldX, worldZ);
                biomeMap[x][z] = biome;
                int surfaceHeight = calculateTerrainHeight(worldX, worldZ, biome);
                heightMap[x][z] = surfaceHeight;

                // Bedrock foundation (Y=0..3): Deccan Basalt
                column.setBlock(x, 0, z, BlockIds.DECCAN_BASALT);
                column.setBlock(x, 1, z, BlockIds.DECCAN_BASALT);
                column.setBlock(x, 2, z, BlockIds.DECCAN_BASALT);
                column.setBlock(x, 3, z, (worldX + worldZ) % 2 == 0 ? BlockIds.DECCAN_BASALT : BlockIds.GRANITE);

                int surfaceDepth = biome.getSurfaceDepth();
                int subSurfaceStart = Math.max(4, surfaceHeight - surfaceDepth);

                // Subterranean host rock strata (Y=4 to subSurfaceStart-1)
                for (int y = 4; y < subSurfaceStart; y++) {
                    int rock = ((worldX * 7 + y * 13 + worldZ * 31 + (int) seed) & 3) == 0 ? BlockIds.GRANITE : BlockIds.STONE;
                    column.setBlock(x, y, z, rock);
                }

                // Sub-surface transitional layer
                for (int y = subSurfaceStart; y < surfaceHeight; y++) {
                    column.setBlock(x, y, z, biome.getUnderBlock());
                }

                // Surface cap block
                column.setBlock(x, surfaceHeight, z, biome.getTopBlock());
            }
        }

        // 2. Second Pass: 3D Subterranean Cave Carving (Y=8..56)
        caveCarver.carveColumn(column, chunkX, chunkZ, heightMap);

        // 3. Third Pass: Subterranean Mineral Ore Vein Distribution
        orePopulator.populateChunk(column, chunkX, chunkZ);

        // 4. Fourth Pass: Procedural Flora (Indigenous Trees)
        long floraSeed = seed ^ (((long) chunkX * 918273645L) + ((long) chunkZ * 564738291L));
        Random floraRand = new Random(floraSeed);

        for (int x = 2; x <= 13; x += 3) {
            for (int z = 2; z <= 13; z += 3) {
                BiomeType biome = biomeMap[x][z];
                if (floraRand.nextFloat() < biome.getTreeDensity() * 4.0f) {
                    int surfaceY = heightMap[x][z];
                    int surfaceBlock = column.getBlock(x, surfaceY, z);
                    // Trees only root on solid surface blocks with open air above
                    if (BlockIds.isSolid(surfaceBlock) && column.getBlock(x, surfaceY + 1, z) == BlockIds.AIR) {
                        TreeGenerator.generateTree(column, x, surfaceY, z, biome, floraRand);
                    }
                }
            }
        }

        // 5. Fifth Pass: Cultural Origin Monument (Harappan Brick Pavilion at 0, 0) and Historical Structures
        if (chunkX == 0 && chunkZ == 0) {
            placeOriginMonument(column, heightMap[8][8]);
        } else {
            structureManager.tryGenerateStructure(column, chunkX, chunkZ, biomeMap[8][8], heightMap[8][8]);
        }

        column.setDirty(false);
        return column;
    }

    /**
     * WHY:
     * Computes deterministic surface elevation using multi-octave Fractal Brownian Motion
     * modulated by regional biome properties.
     */
    public int calculateTerrainHeight(int worldX, int worldZ) {
        BiomeType biome = biomeProvider.getBiomeAt(worldX, worldZ);
        return calculateTerrainHeight(worldX, worldZ, biome);
    }

    /**
     * Internal elevation calculator using pre-resolved BiomeType.
     */
    private int calculateTerrainHeight(int worldX, int worldZ, BiomeType biome) {
        double continental = continentalNoise.noise2(worldX * 0.003, worldZ * 0.003);
        double detail = detailNoise.noise2(worldX * 0.012, worldZ * 0.012);

        double elevation = biome.getBaseHeight() +
                (continental * biome.getHeightScale()) +
                (detail * biome.getRoughness() * 8.0);

        int height = (int) Math.round(elevation);
        return Math.max(16, Math.min(height, 260)); // Clamped safely inside 0-319
    }

    /**
     * WHY:
     * Places the iconic Harappan Brick Pavilion with Sal wood pillars and glowing Diyas
     * at world origin (0, 0).
     */
    private void placeOriginMonument(ChunkColumn column, int groundHeight) {
        int baseY = groundHeight + 1;

        // Foundation platform (8x8)
        for (int x = 4; x <= 11; x++) {
            for (int z = 4; z <= 11; z++) {
                column.setBlock(x, baseY, z, BlockIds.FIRED_BRICK);
            }
        }

        // Corner Sal pillars (height 4)
        for (int py = 1; py <= 3; py++) {
            column.setBlock(4, baseY + py, 4, BlockIds.SAL_LOG);
            column.setBlock(11, baseY + py, 4, BlockIds.SAL_LOG);
            column.setBlock(4, baseY + py, 11, BlockIds.SAL_LOG);
            column.setBlock(11, baseY + py, 11, BlockIds.SAL_LOG);
        }

        // Sal plank roof beams
        for (int x = 4; x <= 11; x++) {
            column.setBlock(x, baseY + 4, 4, BlockIds.SAL_PLANKS);
            column.setBlock(x, baseY + 4, 11, BlockIds.SAL_PLANKS);
        }
        for (int z = 4; z <= 11; z++) {
            column.setBlock(4, baseY + 4, z, BlockIds.SAL_PLANKS);
            column.setBlock(11, baseY + 4, z, BlockIds.SAL_PLANKS);
        }

        // Clay Diyas atop corner pillars
        column.setBlock(4, baseY + 4, 4, BlockIds.DIYA);
        column.setBlock(11, baseY + 4, 4, BlockIds.DIYA);
        column.setBlock(4, baseY + 4, 11, BlockIds.DIYA);
        column.setBlock(11, baseY + 4, 11, BlockIds.DIYA);

        // Central altar with golden diya
        column.setBlock(7, baseY + 1, 7, BlockIds.RED_SANDSTONE);
        column.setBlock(8, baseY + 1, 7, BlockIds.RED_SANDSTONE);
        column.setBlock(7, baseY + 1, 8, BlockIds.RED_SANDSTONE);
        column.setBlock(8, baseY + 1, 8, BlockIds.RED_SANDSTONE);
        column.setBlock(7, baseY + 2, 7, BlockIds.DIYA);
    }

    public long getSeed() {
        return seed;
    }

    public BiomeProvider getBiomeProvider() {
        return biomeProvider;
    }

    public shilploka.worldgen.structure.HistoricalStructureManager getStructureManager() {
        return structureManager;
    }
}