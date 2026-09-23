package shilploka.worldgen.structure;

import shilploka.world.chunk.ChunkColumn;
import shilploka.worldgen.biome.BiomeType;

import java.util.Random;

/**
 * WHY:
 * Deterministically orchestrates historical monument and ruin generation across the
 * endless voxel terrain of Bharat.
 *
 * WHAT:
 * Evaluates chunk coordinates against seed hashing to spawn Harappan Great Baths,
 * Buddhist Stupas, Chola Stepwells, Rajput-Maratha Hill Forts, and Ashokan Pillars.
 *
 * USED FOR:
 * Called during world generation pass in WorldGenerator.
 */
public final class HistoricalStructureManager {

    private final long seed;

    public HistoricalStructureManager(long seed) {
        this.seed = seed;
    }

    /**
     * Attempts to place a historical monument structure in the given chunk column.
     * Generates a monument roughly every 16-20 chunks in a grid layout.
     *
     * @return The StructureType generated, or null if no structure was placed.
     */
    public StructureType tryGenerateStructure(ChunkColumn column, int chunkX, int chunkZ,
                                              BiomeType centerBiome, int surfaceY) {
        // Skip spawn chunk (chunkX == 0 && chunkZ == 0 has the Harappan Origin Monument)
        if (chunkX == 0 && chunkZ == 0) {
            return null;
        }

        // Chunk grid placement: 1 candidate structure per 16x16 chunk region
        int regionX = Math.floorDiv(chunkX, 16);
        int regionZ = Math.floorDiv(chunkZ, 16);

        long regionHash = (((long) regionX * 341873128712L) ^ ((long) regionZ * 132897987541L) ^ seed);
        Random regionRand = new Random(regionHash);

        int targetChunkX = (regionX * 16) + (regionRand.nextInt(12) + 2);
        int targetChunkZ = (regionZ * 16) + (regionRand.nextInt(12) + 2);

        if (chunkX != targetChunkX || chunkZ != targetChunkZ) {
            return null;
        }

        // Determine structure type based on biome
        StructureType type = selectStructureForBiome(centerBiome, regionRand);
        if (type == null) {
            return null;
        }

        // Structure placed with offset 1, 1 inside chunk
        generateStructureDirect(column, 1, surfaceY, 1, type, regionRand);
        return type;
    }

    /**
     * Selects an era-accurate historical structure type matching the regional biome.
     */
    public StructureType selectStructureForBiome(BiomeType biome, Random rand) {
        if (biome == null) return StructureType.ASHOKA_STHAMBHA;

        return switch (biome) {
            case GANGA_MAIDAN, SUNDARBAN -> {
                int r = rand.nextInt(4);
                yield switch (r) {
                    case 0 -> StructureType.HARAPPAN_BATH;
                    case 1 -> StructureType.BUDDHIST_STUPA;
                    case 2 -> StructureType.ASHOKA_STHAMBHA;
                    default -> StructureType.GAON_VILLAGE;
                };
            }
            case THAR_MARUSTHAL, RANN_NAMAK -> StructureType.CHOLA_STEPWELL;
            case HIMALAYA_SHIKHAR, VINDHYA_JANGAL -> StructureType.HILL_FORT;
            case DECCAN_PATHAR -> {
                int r = rand.nextInt(3);
                yield (r == 0) ? StructureType.BUDDHIST_STUPA : (r == 1 ? StructureType.CHOLA_STEPWELL : StructureType.GAON_VILLAGE);
            }
            case SAHYADRI_VARSHAVAN, POORVOTTAR_BAANS -> (rand.nextBoolean()) ? StructureType.HILL_FORT : StructureType.GAON_VILLAGE;
            case SAMUDRA_TAT, SAGAR -> StructureType.ASHOKA_STHAMBHA;
        };
    }

    /**
     * Directly places a historical structure at specified local chunk coordinates.
     */
    public void generateStructureDirect(ChunkColumn column, int startX, int baseY, int startZ,
                                        StructureType type, Random rand) {
        switch (type) {
            case HARAPPAN_BATH -> GreatBathGenerator.generate(column, startX, baseY, startZ, rand);
            case BUDDHIST_STUPA -> StupaGenerator.generate(column, startX, baseY, startZ, rand);
            case CHOLA_STEPWELL -> StepwellGenerator.generate(column, startX, baseY, startZ, rand);
            case HILL_FORT -> HillFortGenerator.generate(column, startX, baseY, startZ, rand);
            case ASHOKA_STHAMBHA -> AshokaSthambhaGenerator.generate(column, startX, baseY, startZ, rand);
            case GAON_VILLAGE -> GaonGenerator.generate(column, startX, baseY, startZ, rand);
        }
    }
}
