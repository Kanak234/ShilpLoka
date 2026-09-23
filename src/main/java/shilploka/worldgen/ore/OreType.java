package shilploka.worldgen.ore;

import shilploka.world.BlockIds;

/**
 * WHY:
 * Subterranean Bharat is rich in ancient metallurgical resources:
 * - Koyla (कोयला): Bituminous coal/lignite mined for smelters and kilns.
 * - Taamba (तांबा): Copper mined since the Harappan Bronze Age (Khetri mines).
 * - Loha (लोहा): High-grade hematite iron ores used for legendary Indian Damascus/Wootz steel.
 * - Rajavarta (राजावर्त): Deep ultramarine lapis lazuli prized for ornaments and pigment.
 * - Hira (हीरा): Legendary Golconda diamonds embedded in ancient kimberlite basalt pipes.
 *
 * WHAT:
 * Encapsulates the geological distribution parameters (elevation bands, vein sizes,
 * and chunk frequency) for every ore mineral.
 *
 * USED FOR:
 * Referenced by OrePopulator to seed mineral veins deterministically.
 */
public enum OreType {

    KOYLA(
            "koyla",
            "Koyla Ore (कोयला)",
            BlockIds.KOYLA_ORE,
            16,
            128,
            12,  // Veins per chunk
            12   // Average cluster size
    ),

    TAAMBA(
            "taamba",
            "Taamba Ore (तांबा)",
            BlockIds.TAAMBA_ORE,
            24,
            80,
            8,
            8
    ),

    LOHA(
            "loha",
            "Loha Ore (लोहा)",
            BlockIds.LOHA_ORE,
            10,
            64,
            10,
            6
    ),

    RAJAVARTA(
            "rajavarta",
            "Rajavarta Ore (राजावर्त)",
            BlockIds.RAJAVARTA_ORE,
            10,
            36,
            3,
            4
    ),

    HIRA(
            "hira",
            "Hira Ore (हीरा)",
            BlockIds.HIRA_ORE,
            4,
            16,
            1,
            3
    );

    private final String id;
    private final String displayName;
    private final int blockId;
    private final int minY;
    private final int maxY;
    private final int veinsPerChunk;
    private final int clusterSize;

    OreType(String id, String displayName, int blockId, int minY, int maxY,
            int veinsPerChunk, int clusterSize) {
        this.id = id;
        this.displayName = displayName;
        this.blockId = blockId;
        this.minY = minY;
        this.maxY = maxY;
        this.veinsPerChunk = veinsPerChunk;
        this.clusterSize = clusterSize;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getBlockId() { return blockId; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public int getVeinsPerChunk() { return veinsPerChunk; }
    public int getClusterSize() { return clusterSize; }
}
