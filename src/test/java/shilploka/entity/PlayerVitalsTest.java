package shilploka.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Validates survival vitals simulation: Kamal (Lotus) health points, Bhookh (hunger) points,
 * saturation, exhaustion accumulation, natural regeneration, starvation, drowning, and fall damage.
 *
 * WHAT:
 * Tests the damage pipeline, food consumption, oxygen depletion, and healing dynamics.
 *
 * USED FOR:
 * Verifies Phase P7 survival mechanics correctness.
 */
public class PlayerVitalsTest {

    private PlayerVitals vitals;

    @BeforeEach
    void setUp() {
        vitals = new PlayerVitals();
        vitals.reset();
    }

    @Test
    @DisplayName("Vitals initialize with 20 HP, 20 Bhookh, 5.0 Saturation, and 300 Air")
    void testInitialState() {
        assertEquals(20.0f, vitals.getHealth(), 0.001f);
        assertEquals(20, vitals.getBhookh());
        assertEquals(5.0f, vitals.getSaturation(), 0.001f);
        assertEquals(300, vitals.getAirBubbles());
        assertFalse(vitals.isDead());
    }

    @Test
    @DisplayName("Damage reduces health and respects floor at zero")
    void testDamagePipeline() {
        vitals.damage(6.0f, "Test Damage");
        assertEquals(14.0f, vitals.getHealth(), 0.001f);

        vitals.damage(20.0f, "Fatal Blow");
        assertEquals(0.0f, vitals.getHealth(), 0.001f);
        assertTrue(vitals.isDead());

        // Further damage while dead does nothing
        vitals.damage(5.0f, "Post-Mortem");
        assertEquals(0.0f, vitals.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Healing restores health up to MAX_HEALTH (20.0)")
    void testHealing() {
        vitals.damage(10.0f, "Combat");
        assertEquals(10.0f, vitals.getHealth(), 0.001f);

        vitals.heal(4.0f);
        assertEquals(14.0f, vitals.getHealth(), 0.001f);

        vitals.heal(10.0f);
        assertEquals(20.0f, vitals.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Eating restores hunger and saturation up to limits")
    void testFoodConsumption() {
        vitals.setBhookh(12);
        vitals.setSaturation(0.0f);

        // Tandoori Roti restores 5 hunger, 6.0f saturation modifier
        boolean eaten = vitals.eat(5, 6.0f);
        assertTrue(eaten);
        assertEquals(17, vitals.getBhookh());
        assertTrue(vitals.getSaturation() > 0.0f);

        // Fill hunger completely
        vitals.eat(10, 2.0f);
        assertEquals(20, vitals.getBhookh());

        // Cannot eat when completely full
        assertFalse(vitals.eat(3, 1.0f));
    }

    @Test
    @DisplayName("Exhaustion drains saturation first, then hunger points")
    void testExhaustion() {
        vitals.setBhookh(20);
        vitals.setSaturation(2.0f);

        // 4.0 exhaustion consumes 1 saturation
        vitals.addExhaustion(4.0f);
        assertEquals(1.0f, vitals.getSaturation(), 0.001f);
        assertEquals(20, vitals.getBhookh());

        // Another 4.0 exhaustion consumes the last saturation
        vitals.addExhaustion(4.0f);
        assertEquals(0.0f, vitals.getSaturation(), 0.001f);
        assertEquals(20, vitals.getBhookh());

        // Further exhaustion now depletes hunger (Bhookh)
        vitals.addExhaustion(4.0f);
        assertEquals(0.0f, vitals.getSaturation(), 0.001f);
        assertEquals(19, vitals.getBhookh());
    }

    @Test
    @DisplayName("Natural regeneration heals player when Bhookh >= 18")
    void testNaturalRegeneration() {
        vitals.damage(5.0f, "Minor Injury");
        assertEquals(15.0f, vitals.getHealth(), 0.001f);
        vitals.setBhookh(20);

        // Tick 80 times (4 seconds at 20 ticks/sec triggers natural heal)
        for (int i = 0; i < 80; i++) {
            vitals.tick(false, false, true, false);
        }

        assertEquals(16.0f, vitals.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Starvation deals periodic damage when Bhookh is zero")
    void testStarvation() {
        vitals.setHealth(10.0f);
        vitals.setBhookh(0);

        // Tick 80 times (starvation damage occurs every 80 ticks)
        for (int i = 0; i < 80; i++) {
            vitals.tick(false, false, true, false);
        }

        assertEquals(9.0f, vitals.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Drowning depletes air bubbles and deals damage when empty")
    void testDrowning() {
        vitals.setHealth(20.0f);
        vitals.setAirBubbles(5);

        // Tick 5 times in water to deplete remaining air
        for (int i = 0; i < 5; i++) {
            vitals.tick(true, false, false, false);
        }
        assertEquals(0, vitals.getAirBubbles());

        // Next 20 ticks submerged causes 2.0 drowning damage
        for (int i = 0; i < 20; i++) {
            vitals.tick(true, false, false, false);
        }
        assertEquals(18.0f, vitals.getHealth(), 0.001f);
    }
}
