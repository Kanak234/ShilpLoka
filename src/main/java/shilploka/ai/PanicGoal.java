package shilploka.ai;

import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.world.chunk.ChunkManager;

/**
 * WHY:
 * Causes docile creatures (Peacocks, deer, young calves) to flee in panic when threatened
 * or injured.
 *
 * WHAT:
 * Moves rapidly in the vector opposite to the approaching player or threat source.
 */
public class PanicGoal implements AIGoal {

    private final LivingEntity entity;
    private final Player player;
    private final ChunkManager chunkManager;
    private final float panicDistance;

    public PanicGoal(LivingEntity entity, Player player, ChunkManager chunkManager, float panicDistance) {
        this.entity = entity;
        this.player = player;
        this.chunkManager = chunkManager;
        this.panicDistance = panicDistance;
    }

    @Override
    public boolean shouldExecute() {
        if (player == null || entity.isDead()) return false;
        double dx = player.getPosition().x - entity.getX();
        double dz = player.getPosition().z - entity.getZ();
        double distSq = dx * dx + dz * dz;
        return distSq <= panicDistance * panicDistance;
    }

    @Override
    public void tick() {
        if (player == null) return;
        double dx = entity.getX() - player.getPosition().x;
        double dz = entity.getZ() - player.getPosition().z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0.001) {
            double dirX = dx / dist;
            double dirZ = dz / dist;
            double escapeX = entity.getX() + dirX * 8.0;
            double escapeZ = entity.getZ() + dirZ * 8.0;
            entity.pathTo(escapeX, entity.getY(), escapeZ, chunkManager);
        }
    }
}
