package shilploka.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WHY:
 * Manages player survival metrics: 20 HP (10 Kamal/Lotus icons), 20 Bhookh (10 Roti icons),
 * saturation, exhaustion, oxygen air bubbles (Jal-Shwaas), fall damage, drowning, and starvation.
 *
 * WHAT:
 * Encapsulates damage pipelines, natural health regeneration, and nutritional consumption.
 *
 * USED FOR:
 * Embedded in Player and rendered on the survival HUD.
 *
 * WHERE NEXT:
 * Ticked in Player.tick() and saved to playerdata/player.json.
 */
public class PlayerVitals {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerVitals.class);

    public static final float MAX_HEALTH = 20.0f;
    public static final int MAX_BHOOKH = 20;
    public static final int MAX_AIR = 300; // 15 seconds at 20 ticks/sec

    private float health = MAX_HEALTH;
    private int bhookh = MAX_BHOOKH;
    private float saturation = 5.0f;
    private float exhaustion = 0.0f;
    private int airBubbles = MAX_AIR;

    private float fallDistance = 0.0f;
    private int regenTimer = 0;
    private int starvationTimer = 0;
    private int drownTimer = 0;
    private int suffocationTimer = 0;

    /**
     * Applies damage to player vitals, respecting health floor at 0.0.
     */
    public void damage(float amount, String cause) {
        if (amount <= 0.0f || isDead()) return;
        this.health = Math.max(0.0f, this.health - amount);
        LOGGER.info("Player took {:.1f} damage from {}. Remaining HP: {:.1f}", amount, cause, this.health);
    }

    /**
     * Restores health up to MAX_HEALTH.
     */
    public void heal(float amount) {
        if (amount <= 0.0f || isDead()) return;
        this.health = Math.min(MAX_HEALTH, this.health + amount);
    }

    /**
     * Consumes food, restoring hunger points and saturation.
     */
    public boolean eat(int foodPoints, float saturationModifier) {
        if (this.bhookh >= MAX_BHOOKH) return false;
        this.bhookh = Math.min(MAX_BHOOKH, this.bhookh + foodPoints);
        this.saturation = Math.min(this.bhookh, this.saturation + (foodPoints * saturationModifier * 2.0f));
        return true;
    }

    /**
     * Accumulates exhaustion from physical exertion (sprinting, jumping, mining).
     */
    public void addExhaustion(float amount) {
        this.exhaustion += amount;
        while (this.exhaustion >= 4.0f) {
            this.exhaustion -= 4.0f;
            if (this.saturation > 0.0f) {
                this.saturation = Math.max(0.0f, this.saturation - 1.0f);
            } else if (this.bhookh > 0) {
                this.bhookh = Math.max(0, this.bhookh - 1);
            }
        }
    }

    /**
     * WHY:
     * Advances survival vitals tick logic: drowning, suffocation, fall damage, and health regeneration.
     */
    public void tick(boolean inWater, boolean headInSolid, boolean onGround, boolean isSprinting) {
        if (isDead()) return;

        // 1. Oxygen & Drowning Simulation
        if (inWater) {
            airBubbles = Math.max(0, airBubbles - 1);
            if (airBubbles == 0) {
                drownTimer++;
                if (drownTimer >= 20) { // 1 second intervals
                    damage(2.0f, "Drowning (जलमग्न)");
                    drownTimer = 0;
                }
            }
        } else {
            airBubbles = Math.min(MAX_AIR, airBubbles + 5);
            drownTimer = 0;
        }

        // 2. Suffocation in Solid Blocks
        if (headInSolid) {
            suffocationTimer++;
            if (suffocationTimer >= 20) {
                damage(1.0f, "Suffocation (दम घुटना)");
                suffocationTimer = 0;
            }
        } else {
            suffocationTimer = 0;
        }

        // 3. Fall Damage Resolution
        if (onGround) {
            if (fallDistance > 3.0f) {
                float damage = fallDistance - 3.0f;
                damage(damage, "Fall Impact (गिरने की चोट)");
            }
            fallDistance = 0.0f;
        }

        // 4. Natural Health Regeneration
        if (bhookh >= 18 && health < MAX_HEALTH) {
            regenTimer++;
            if (regenTimer >= 80) { // 4 seconds
                heal(1.0f);
                addExhaustion(3.0f);
                regenTimer = 0;
            }
        } else {
            regenTimer = 0;
        }

        // 5. Starvation Damage
        if (bhookh == 0) {
            starvationTimer++;
            if (starvationTimer >= 80) {
                if (health > 1.0f) {
                    damage(1.0f, "Starvation (भुखमरी)");
                }
                starvationTimer = 0;
            }
        } else {
            starvationTimer = 0;
        }

        // Sprinting exhaustion accumulation
        if (isSprinting) {
            addExhaustion(0.01f);
        }
    }

    public void accumulateFallDistance(float distance) {
        if (distance > 0.0f) {
            this.fallDistance += distance;
        }
    }

    public void resetFallDistance() {
        this.fallDistance = 0.0f;
    }

    public void reset() {
        this.health = MAX_HEALTH;
        this.bhookh = MAX_BHOOKH;
        this.saturation = 5.0f;
        this.exhaustion = 0.0f;
        this.airBubbles = MAX_AIR;
        this.fallDistance = 0.0f;
    }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0.0f, Math.min(MAX_HEALTH, health)); }

    public int getBhookh() { return bhookh; }
    public void setBhookh(int bhookh) { this.bhookh = Math.max(0, Math.min(MAX_BHOOKH, bhookh)); }

    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) { this.saturation = saturation; }

    public float getExhaustion() { return exhaustion; }
    public void setExhaustion(float exhaustion) { this.exhaustion = exhaustion; }

    public int getAirBubbles() { return airBubbles; }
    public void setAirBubbles(int airBubbles) { this.airBubbles = airBubbles; }

    public float getFallDistance() { return fallDistance; }
    public void setFallDistance(float fallDistance) { this.fallDistance = fallDistance; }

    public boolean isDead() { return health <= 0.0f; }
}
