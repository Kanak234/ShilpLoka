package shilploka.audio;

/**
 * WHY:
 * Defines volume grouping categories for ShilpLoka sound settings.
 */
public enum SoundCategory {
    MASTER("Master Volume", "मुख्य ध्वनि"),
    MUSIC("Music", "संगीत"),
    WEATHER("Weather", "मौसम"),
    BLOCKS("Blocks", "खण्ड ध्वनि"),
    HOSTILE("Hostile Creatures", "शत्रु प्राणी"),
    NEUTRAL("Neutral / Passive", "वन्य जीव"),
    PLAYERS("Player", "खिलाड़ी"),
    AMBIENT("Ambience / Environment", "परिवेश ध्वनि");

    private final String englishName;
    private final String hindiName;

    SoundCategory(String englishName, String hindiName) {
        this.englishName = englishName;
        this.hindiName = hindiName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getHindiName() {
        return hindiName;
    }
}
