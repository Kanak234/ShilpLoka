package shilploka.entity;

import org.joml.Vector3d;
import org.joml.Vector3f;
import shilploka.entity.living.LivingEntity;
import shilploka.entity.living.fauna.BuffaloEntity;
import shilploka.entity.living.fauna.ElephantEntity;
import shilploka.entity.living.fauna.GauEntity;
import shilploka.entity.living.fauna.PeacockEntity;
import shilploka.entity.living.hostile.CobraEntity;
import shilploka.entity.living.hostile.DaakuEntity;
import shilploka.entity.living.hostile.TigerEntity;
import shilploka.entity.living.hostile.VetaalEntity;
import shilploka.item.ItemStack;
import shilploka.world.DayNightCycle;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * WHY:
 * Central lifecycle manager for all non-player living entities across the active ShilpLoka world.
 *
 * WHAT:
 * Handles entity updates, natural biome spawning, combat hit validation, death drop dispersion,
 * and despawning beyond loaded chunk radii.
 *
 * USED FOR:
 * Consulted and ticked directly in Game loop.
 */
public class EntityManager {

    public static final int MAX_ACTIVE_ENTITIES = 40;
    public static final double DESPAWN_RADIUS_SQ = 64.0 * 64.0; // 64 blocks

    private final List<LivingEntity> entities = new ArrayList<>();
    private final Random random = new Random();
    private int spawnTimer = 0;

    public void tick(float dt, ChunkManager chunkManager, Player player, DayNightCycle dayNightCycle, List<ItemEntity> itemDrops) {
        if (player == null || chunkManager == null) return;

        // 1. Tick living entities and process deaths
        Iterator<LivingEntity> it = entities.iterator();
        while (it.hasNext()) {
            LivingEntity entity = it.next();

            // Despawn if far from player
            double dx = entity.getX() - player.getPosition().x;
            double dz = entity.getZ() - player.getPosition().z;
            if (dx * dx + dz * dz > DESPAWN_RADIUS_SQ) {
                it.remove();
                continue;
            }

            entity.tick(dt, chunkManager, player);

            if (entity.isDead()) {
                // Spawn death drops
                List<ItemStack> drops = entity.getDeathDrops();
                if (drops != null && itemDrops != null) {
                    for (ItemStack drop : drops) {
                        float vx = (float) ((random.nextDouble() - 0.5) * 2.0);
                        float vy = 3.0f;
                        float vz = (float) ((random.nextDouble() - 0.5) * 2.0);
                        itemDrops.add(new ItemEntity(entity.getX(), entity.getY() + 0.5, entity.getZ(), drop.copy(), vx, vy, vz));
                    }
                }
                it.remove();
            }
        }

        // 2. Periodic natural fauna & hostile mob spawning
        if (++spawnTimer >= 100 && entities.size() < MAX_ACTIVE_ENTITIES) {
            spawnTimer = 0;
            spawnNaturalEntity(chunkManager, player, dayNightCycle);
        }
    }

    private void spawnNaturalEntity(ChunkManager chunkManager, Player player, DayNightCycle dayNightCycle) {
        // Pick random coordinate 20-35 blocks from player
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = 20.0 + random.nextDouble() * 15.0;
        int spawnX = (int) Math.floor(player.getPosition().x + Math.cos(angle) * dist);
        int spawnZ = (int) Math.floor(player.getPosition().z + Math.sin(angle) * dist);

        // Find surface ground height
        int spawnY = 80;
        while (spawnY > 10 && chunkManager.getBlock(spawnX, spawnY, spawnZ) == 0) {
            spawnY--;
        }
        spawnY++; // Air block directly above ground

        if (spawnY <= 12 || spawnY >= shilploka.world.chunk.ChunkColumn.CHUNK_HEIGHT - 5) return;

        boolean isNight = (dayNightCycle != null && dayNightCycle.isNight());
        LivingEntity spawned = null;

        if (isNight) {
            // Night hostile spawns: Vetaal spirit or Daaku bandit
            if (random.nextBoolean()) {
                spawned = new VetaalEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager, player, dayNightCycle);
            } else {
                spawned = new DaakuEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager, player);
            }
        } else {
            // Day passive & predator spawns
            int roll = random.nextInt(6);
            switch (roll) {
                case 0 -> spawned = new GauEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager);
                case 1 -> spawned = new ElephantEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager);
                case 2 -> spawned = new PeacockEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager, player);
                case 3 -> spawned = new BuffaloEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager);
                case 4 -> spawned = new TigerEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager, player);
                case 5 -> spawned = new CobraEntity(spawnX + 0.5, spawnY, spawnZ + 0.5, chunkManager, player);
            }
        }

        if (spawned != null) {
            entities.add(spawned);
        }
    }

    /**
     * Executes player melee attack targeting the closest living entity along sightline.
     */
    public LivingEntity attackTarget(Vector3f eyePos, Vector3f lookDir, float reach, float damage) {
        LivingEntity closest = null;
        double closestDist = reach;
        Vector3d eyePosD = new Vector3d(eyePos.x, eyePos.y, eyePos.z);

        for (LivingEntity e : entities) {
            if (e.isDead()) continue;

            // Simple ray-AABB test
            AABB box = e.getBoundingBox();
            if (box.intersectsRay(eyePos, lookDir, reach)) {
                double dist = eyePosD.distance(e.getPosition());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }
        }

        if (closest != null) {
            closest.damage(damage);
        }
        return closest;
    }

    public void addEntity(LivingEntity entity) {
        if (entity != null) entities.add(entity);
    }

    public void spawnEntity(LivingEntity entity) {
        addEntity(entity);
    }

    public List<LivingEntity> getEntities() {
        return entities;
    }

    public void clear() {
        entities.clear();
    }
}
