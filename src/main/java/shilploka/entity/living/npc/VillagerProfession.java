package shilploka.entity.living.npc;

import shilploka.world.BlockIds;

/**
 * WHY:
 * Enumerates the 9 traditional artisanal and civic professions of Indian village life.
 *
 * WHAT:
 * Maps each profession to its Devanagari title, associated crafting workstation,
 * and commercial barter specialization.
 *
 * USED FOR:
 * Governs GramwasiEntity daily schedule, workstation attraction, and trade catalog.
 */
public enum VillagerProfession {
    KISAN("Kisan (किसान)", "Farmer tending wheat, paddy, cotton, and sugarcane fields.", BlockIds.ALLUVIAL_SOIL),
    KUMHAR("Kumhar (कुम्हार)", "Master potter crafting earthen Kulhads, Handis, and Matkas.", BlockIds.CHAAK),
    LOHAR("Lohar (लोहार)", "Blacksmith forging tools, weapons, and Wootz steel blades.", BlockIds.NIHAI),
    BUNKAR("Bunkar / Julaha (बुनकर)", "Textile artisan spinning raw cotton and weaving Khadi cloth.", BlockIds.KARGAH),
    VAIDYA("Vaidya (वैद्य)", "Ayurvedic healer preparing herbal remedies, antivenoms, and teas.", BlockIds.PAKKI_BHATTI),
    SUNAR("Sunar (सुनार)", "Goldsmith and jeweler exchanging precious Mudra coins and lapis.", BlockIds.SMELTING_BHATTI),
    MACHHUARA("Machhuara (मछुआरा)", "Fisherman gathering coastal catch, salt crust, and river shells.", BlockIds.WATER),
    SHILPI("Shilpi (शिल्पी)", "Stonemason carving temple reliefs, bricks, and architectural pillars.", BlockIds.KARYASHALA),
    SARPANCH("Sarpanch (सरपंच)", "Village elder administering the Panchayat Chaupal and trade charters.", BlockIds.SAL_PLANKS);

    private final String displayName;
    private final String description;
    private final int workstationBlockId;

    VillagerProfession(String displayName, String description, int workstationBlockId) {
        this.displayName = displayName;
        this.description = description;
        this.workstationBlockId = workstationBlockId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getWorkstationBlockId() {
        return workstationBlockId;
    }
}
