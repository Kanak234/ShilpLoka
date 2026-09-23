package shilploka.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WHY:
 * Manages the active cultural festival state across the world.
 *
 * WHAT:
 * Allows toggleable festival states (Diwali, Holi, Makar Sankranti, Harvest)
 * through settings or calendar progression.
 */
public class FestivalManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(FestivalManager.class);

    private FestivalType activeFestival = FestivalType.NONE;

    public FestivalType getActiveFestival() {
        return activeFestival;
    }

    public void setActiveFestival(FestivalType festival) {
        if (festival != null && festival != this.activeFestival) {
            LOGGER.info("Active festival transitioned to [{}]", festival.getEnglishName());
            this.activeFestival = festival;
        }
    }

    public float getCropGrowthMultiplier() {
        return activeFestival.getCropGrowthMultiplier();
    }

    public boolean isDiwaliActive() {
        return activeFestival == FestivalType.DIWALI;
    }
}
