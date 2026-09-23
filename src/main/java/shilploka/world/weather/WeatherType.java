package shilploka.world.weather;

/**
 * WHY:
 * Defines atmospheric weather phenomena characteristic of the Indian subcontinent.
 *
 * WHAT:
 * - CLEAR: Clear bright skies and warm sunlight.
 * - BARSAAT: Torrential monsoon deluge (मूसलाधार बरसात) with dark storm clouds.
 * - AANDHI: Thar desert blinding dust storm (रेतीली आंधी) with howling winds and yellow fog.
 * - HIMPAAT: High-altitude Himalayan snowstorm (हिमपात) with falling snowflakes.
 */
public enum WeatherType {
    CLEAR("Khula Mausam (स्वच्छ मौसम)", 0.0f, 1.0f),
    BARSAAT("Monsoon Barsaat (मूसलाधार वर्षा)", 0.8f, 0.45f),
    AANDHI("Thar Aandhi (रेतीली आंधी)", 0.6f, 0.60f),
    HIMPAAT("Himalayan Himpaat (हिमपात)", 0.5f, 0.70f);

    private final String displayName;
    private final float precipitationIntensity;
    private final float sunlightMultiplier;

    WeatherType(String displayName, float precipitationIntensity, float sunlightMultiplier) {
        this.displayName = displayName;
        this.precipitationIntensity = precipitationIntensity;
        this.sunlightMultiplier = sunlightMultiplier;
    }

    public String getDisplayName() { return displayName; }
    public float getPrecipitationIntensity() { return precipitationIntensity; }
    public float getSunlightMultiplier() { return sunlightMultiplier; }
}
