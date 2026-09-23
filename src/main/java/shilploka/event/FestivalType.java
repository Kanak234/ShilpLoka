package shilploka.event;

/**
 * WHY:
 * Encapsulates the traditional seasonal cultural festivals of Bharat,
 * providing celebratory environmental variations, illumination bonuses,
 * and accelerated agricultural bounties.
 */
public enum FestivalType {
    NONE("Standard (No Festival)", "साधारण (कोई उत्सव नहीं)", 1.0f, false),
    DIWALI("Diwali (Festival of Lights)", "दीपावली (दीपोत्सव)", 1.0f, true),
    HOLI("Holi (Festival of Colors)", "होली (रंगोत्सव)", 1.0f, false),
    MAKAR_SANKRANTI("Makar Sankranti (Kite Harvest)", "मकर संक्रांति (पतंग उत्सव)", 1.15f, false),
    HARVEST("Utsav (Seasonal Harvest)", "फसल कटाई उत्सव (बैसाखी/पोंगल/ओणम)", 1.50f, false);

    private final String englishName;
    private final String hindiName;
    private final float cropGrowthMultiplier;
    private final boolean goldenNightIllumination;

    FestivalType(String englishName, String hindiName, float cropGrowthMultiplier, boolean goldenNightIllumination) {
        this.englishName = englishName;
        this.hindiName = hindiName;
        this.cropGrowthMultiplier = cropGrowthMultiplier;
        this.goldenNightIllumination = goldenNightIllumination;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getHindiName() {
        return hindiName;
    }

    public float getCropGrowthMultiplier() {
        return cropGrowthMultiplier;
    }

    public boolean hasGoldenNightIllumination() {
        return goldenNightIllumination;
    }
}
