package shilploka.kaushal;

/**
 * WHY:
 * Enumerates the sacred Vedic enhancements and mastery skills (Kaushal) bestowable upon tools
 * and gear at the Shilp Mandap altar.
 *
 * WHAT:
 * Defines skill types, Devanagari titles, functional stat modifications, and gem costs.
 *
 * USED FOR:
 * Referenced by KaushalManager, Tool items, and KaushalScreen.
 */
public enum KaushalUpgradeType {
    TEZ_KHANAN(
            "Tez Khanan (तेज़ खनन)",
            "Harnesses subterranean earth energy to accelerate block mining speed by +40%.",
            1
    ),
    MAJBOOT_SHILP(
            "Majboot Shilp (मजबूत शिल्प)",
            "Blesses the tool with crystalline resilience; 50% chance not to deplete durability on use.",
            2
    ),
    TEJ_DHAAR(
            "Tej Dhaar (तेज़ धार)",
            "Hones the weapon's edge to keen perfection, dealing +3.0 bonus melee damage.",
            2
    ),
    JAL_SHWAAS(
            "Jal-Shwaas (जल-श्वास)",
            "Endows the artisan with aquatic endurance, doubling underwater Jal-Shwaas oxygen capacity.",
            1
    ),
    KRISHI_VRIDDHI(
            "Krishi Vriddhi (कृषि वृद्धि)",
            "Blesses agricultural implements with the boon of Annapurna, doubling crop harvest yields.",
            1
    );

    private final String displayName;
    private final String description;
    private final int lapisCost;

    KaushalUpgradeType(String displayName, String description, int lapisCost) {
        this.displayName = displayName;
        this.description = description;
        this.lapisCost = lapisCost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getLapisCost() {
        return lapisCost;
    }
}
