package shilploka.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating celestial time progression, sunlight factor transitions,
 * atmospheric color interpolation, and traditional Indian lunar phases.
 */
public class DayNightCycleTest {

    @Test
    @DisplayName("DayNightCycle starts at tick 0 (Sunrise / Pratahkaal)")
    void testInitialState() {
        DayNightCycle cycle = new DayNightCycle(0.0);
        assertEquals(0.0, cycle.getWorldTime(), 0.001);
        assertEquals(0, cycle.getTotalDays());
        assertTrue(cycle.getFormattedTime().contains("Pratahkaal"));
    }

    @Test
    @DisplayName("Midday (tick 6000) provides 1.0 sunlight factor and sun angle 0")
    void testMiddayZenith() {
        DayNightCycle cycle = new DayNightCycle(6000.0);
        assertEquals(1.0f, cycle.getSunlightFactor(), 0.001f, "Midday must provide 1.0 sunlight factor");
        assertEquals(0.0f, cycle.getSunAngleRadians(), 0.001f, "Sun angle at noon must be 0 radians");
        assertTrue(cycle.getFormattedTime().contains("12:00"));
        assertTrue(cycle.getFormattedTime().contains("Madhyahn"));
    }

    @Test
    @DisplayName("Midnight (tick 18000) provides minimum 0.15 ambient starlight factor")
    void testMidnightStarlight() {
        DayNightCycle cycle = new DayNightCycle(18000.0);
        assertEquals(0.15f, cycle.getSunlightFactor(), 0.001f, "Midnight must provide 0.15 ambient starlight factor");
        assertEquals((float) Math.PI, cycle.getSunAngleRadians(), 0.001f, "Sun angle at midnight must be PI radians");
        assertTrue(cycle.getFormattedTime().contains("00:00"));
        assertTrue(cycle.getFormattedTime().contains("Ratri"));
    }

    @Test
    @DisplayName("Lunar cycle correctly computes Shukla and Krishna Paksha phases")
    void testMoonPhases() {
        DayNightCycle cycle = new DayNightCycle(0.0);

        // Day 0: Shukla Pratipada
        assertEquals(0, cycle.getMoonPhase());
        assertTrue(cycle.getMoonPhaseName().contains("Shukla Pratipada"));

        // Advance 3 days: Purnima (Full Moon)
        cycle.update(3 * (24000.0f / 20.0f));
        assertEquals(3, cycle.getMoonPhase());
        assertTrue(cycle.getMoonPhaseName().contains("Purnima"));

        // Advance 4 more days (Day 7): Amavasya (New Moon)
        cycle.update(4 * (24000.0f / 20.0f));
        assertEquals(7, cycle.getMoonPhase());
        assertTrue(cycle.getMoonPhaseName().contains("Amavasya"));
    }

    @Test
    @DisplayName("Delta time advances world ticks accurately at 20 ticks per second")
    void testTimeAdvancement() {
        DayNightCycle cycle = new DayNightCycle(0.0);
        // Advance 10 seconds: 10s * 20 ticks/sec = 200 ticks
        cycle.update(10.0f);
        assertEquals(200.0, cycle.getWorldTime(), 0.01);
    }
}
