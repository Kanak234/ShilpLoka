package shilploka.workstation;

import shilploka.world.BlockIds;

/**
 * WHY:
 * Identifies the distinct historical Indian crafting workstations supported in ShilpLoka.
 * Each workstation features custom physics, recipes, audio identities, and interactive UI panels.
 *
 * WHAT:
 * Enum covering:
 * - KARYASHALA: 3x3 master crafting table for advanced tools and architectural structures.
 * - PAKKI_BHATTI: High-temperature ceramic/terracotta kiln for baking bricks and melting silica glass.
 * - SMELTING_BHATTI: Metallurgical alloy furnace producing ancient Indian bronze, brass, and wootz crucible steel.
 * - CHAAK: Traditional rotating potter's wheel shaping wet river clay into sacred vessels.
 * - KARGAH: Ancient indigenous handloom for spinning cotton yarn and weaving pure Khadi textiles.
 * - CHAKKI: Rotary stone quern hand mill grinding grains and whole spices into culinary and medicinal flours.
 * - NIHAI: Heavy forged anvil for restoring durability to legendary weapons and tools.
 *
 * USED FOR:
 * Referenced by World interaction, WorkstationScreen, and recipe managers.
 */
public enum WorkstationType {
    KARYASHALA(BlockIds.KARYASHALA, "Shilp Karyashala (शिल्प कार्यशाला)", "3x3 Master Crafting Workbench"),
    PAKKI_BHATTI(BlockIds.PAKKI_BHATTI, "Pakki Bhatti (पक्की भट्ठी)", "Terracotta & Ceramic Kiln"),
    SMELTING_BHATTI(BlockIds.SMELTING_BHATTI, "Dhaatu Bhatti (धातु प्रगलन भट्ठी)", "Metallurgical Alloy Furnace"),
    CHAAK(BlockIds.CHAAK, "Kumhar Chaak (कुम्हार का चाक)", "Rotating Potter's Wheel"),
    KARGAH(BlockIds.KARGAH, "Khadi Kargah (करघा)", "Indigenous Handloom"),
    CHAKKI(BlockIds.CHAKKI, "Atta Chakki (हाथ की चक्की)", "Rotary Stone Quern Mill"),
    NIHAI(BlockIds.NIHAI, "Lohar Nihai (लोहार की निहाई)", "Forged Metallurgical Anvil");

    private final int blockId;
    private final String hindiName;
    private final String englishDescription;

    WorkstationType(int blockId, String hindiName, String englishDescription) {
        this.blockId = blockId;
        this.hindiName = hindiName;
        this.englishDescription = englishDescription;
    }

    public int getBlockId() { return blockId; }
    public String getHindiName() { return hindiName; }
    public String getEnglishDescription() { return englishDescription; }

    public static WorkstationType fromBlockId(int blockId) {
        for (WorkstationType type : values()) {
            if (type.blockId == blockId) return type;
        }
        return null;
    }
}
