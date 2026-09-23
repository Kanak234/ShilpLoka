package shilploka.world.weather;

/**
 * WHY:
 * Time in ancient Bharat is divided into Shat-Ritu (षडऋतु / Six Seasons), each lasting
 * approximately 2 solar months (represented in ShilpLoka by 4 in-game days per season).
 *
 * WHAT:
 * - VASANTA (Spring / वसंत): Blossoming trees, temperate warmth.
 * - GRISHMA (Summer / ग्रीष्म): Arid heatwaves, higher evaporation.
 * - VARSHA (Monsoon / वर्षा): Torrential monsoon rains (Barsaat), lush vegetation growth.
 * - SHARAD (Autumn / शरद): Crisp azure skies, harvest festival season.
 * - HEMANTA (Pre-Winter / हेमंत): Cooling mountain breezes, frosty mornings.
 * - SHISHIRA (Winter / शिशिर): Coldest alpine temperatures, heavy Himalayan snowfall.
 */
public enum SeasonType {
    VASANTA("Vasanta Ritu (वसंत ऋतु - Spring)", 0.2f, 0.4f),
    GRISHMA("Grishma Ritu (ग्रीष्म ऋतु - Summer)", 0.8f, 0.1f),
    VARSHA("Varsha Ritu (वर्षा ऋतु - Monsoon)", 0.4f, 0.9f),
    SHARAD("Sharad Ritu (शरद ऋतु - Autumn)", 0.3f, 0.3f),
    HEMANTA("Hemanta Ritu (हेमंत ऋतु - Pre-Winter)", -0.2f, 0.3f),
    SHISHIRA("Shishira Ritu (शिशिर ऋतु - Deep Winter)", -0.6f, 0.4f);

    private final String displayName;
    private final float temperatureBonus;
    private final float rainfallMultiplier;

    SeasonType(String displayName, float temperatureBonus, float rainfallMultiplier) {
        this.displayName = displayName;
        this.temperatureBonus = temperatureBonus;
        this.rainfallMultiplier = rainfallMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public float getTemperatureBonus() { return temperatureBonus; }
    public float getRainfallMultiplier() { return rainfallMultiplier; }

    public static SeasonType fromDay(int dayCount) {
        // 4 in-game days per season -> 24 days per full year cycle
        int cycleDay = (dayCount % 24) / 4;
        return values()[cycleDay % values().length];
    }
}
