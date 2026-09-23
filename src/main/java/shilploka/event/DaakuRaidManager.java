package shilploka.event;

import shilploka.entity.EntityManager;
import shilploka.entity.living.hostile.DaakuEntity;
import shilploka.entity.living.npc.GramwasiEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WHY:
 * Orchestrates dynamic Daaku bandit raids on rural Indian villages during dark moon nights.
 * Tests player combat readiness and village defense, rewarding heroes with village favor
 * and Mudra bounties.
 *
 * WHAT:
 * Monitors village proximity, spawns armed Daaku bandits around village perimeter,
 * alerts villagers to seek indoor shelter, and tracks raid victory conditions.
 *
 * USED FOR:
 * Updated in Game.java loop during nighttime cycles.
 */
public class DaakuRaidManager {

    public enum RaidStatus {
        INACTIVE,
        RAID_ACTIVE,
        VICTORY,
        DEFEATED
    }

    private RaidStatus status = RaidStatus.INACTIVE;
    private int villageX;
    private int villageY;
    private int villageZ;
    private final List<DaakuEntity> activeRaiders = new ArrayList<>();
    private int raidersTotal = 0;
    private boolean villageProtected = false;

    public RaidStatus getStatus() {
        return status;
    }

    public boolean isRaidActive() {
        return status == RaidStatus.RAID_ACTIVE;
    }

    public boolean isVillageProtected() {
        return villageProtected;
    }

    public int getRemainingRaiders() {
        return (int) activeRaiders.stream().filter(d -> !d.isDead()).count();
    }

    /**
     * Triggers a Daaku raid event at specified village coordinates.
     */
    public void startRaid(int vx, int vy, int vz, EntityManager entityManager, List<GramwasiEntity> villagers, Random rand) {
        this.villageX = vx;
        this.villageY = vy;
        this.villageZ = vz;
        this.activeRaiders.clear();
        this.status = RaidStatus.RAID_ACTIVE;

        // Alert villagers to take shelter
        if (villagers != null) {
            for (GramwasiEntity villager : villagers) {
                villager.setScheduleState(GramwasiEntity.ScheduleState.SLEEPING);
            }
        }

        // Spawn 4-6 Daaku bandits along outer perimeter (radius ~18 blocks)
        this.raidersTotal = 4 + rand.nextInt(3);
        for (int i = 0; i < raidersTotal; i++) {
            double angle = (2.0 * Math.PI * i) / raidersTotal;
            double spawnX = vx + Math.cos(angle) * 18.0;
            double spawnZ = vz + Math.sin(angle) * 18.0;

            DaakuEntity daaku = new DaakuEntity(spawnX, vy, spawnZ);
            activeRaiders.add(daaku);
            if (entityManager != null) {
                entityManager.spawnEntity(daaku);
            }
        }
    }

    /**
     * Ticks the active raid, verifying whether all raiders have been eliminated.
     */
    public void update() {
        if (status != RaidStatus.RAID_ACTIVE) return;

        // Clean up dead raiders
        activeRaiders.removeIf(DaakuEntity::isDead);

        if (activeRaiders.isEmpty()) {
            this.status = RaidStatus.VICTORY;
            this.villageProtected = true;
        }
    }
}
