package shilploka.ai;

import shilploka.entity.living.LivingEntity;
import shilploka.world.chunk.ChunkManager;

import java.util.Random;

/**
 * WHY:
 * Implements passive idle roaming for domestic and wild Indian fauna.
 *
 * WHAT:
 * Periodically chooses a random walkable waypoint within 8 blocks and initiates pathfinding.
 */
public class WanderGoal implements AIGoal {

    private final LivingEntity entity;
    private final ChunkManager chunkManager;
    private final Random random = new Random();
    private int cooldown = 0;

    public WanderGoal(LivingEntity entity, ChunkManager chunkManager) {
        this.entity = entity;
        this.chunkManager = chunkManager;
        this.cooldown = 40 + random.nextInt(60);
    }

    @Override
    public boolean shouldExecute() {
        if (--cooldown <= 0) {
            cooldown = 80 + random.nextInt(120); // Wander every 4-10 seconds
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = 4.0 + random.nextDouble() * 6.0;
        double targetX = entity.getX() + Math.cos(angle) * dist;
        double targetZ = entity.getZ() + Math.sin(angle) * dist;
        double targetY = entity.getY();

        entity.pathTo(targetX, targetY, targetZ, chunkManager);
    }
}
