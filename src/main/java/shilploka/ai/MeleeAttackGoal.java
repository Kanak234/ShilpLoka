package shilploka.ai;

import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.world.chunk.ChunkManager;

/**
 * WHY:
 * Drives aggressive tracking and striking behaviors for predators (Baagh, Naag)
 * and nocturnal hostiles (Vetaal, Daaku).
 *
 * WHAT:
 * Engages player within detection range (16 blocks), paths toward player, and strikes
 * within melee range (2.0 blocks), delivering damage to PlayerVitals.
 */
public class MeleeAttackGoal implements AIGoal {

    private final LivingEntity entity;
    private final Player player;
    private final ChunkManager chunkManager;
    private final float attackDamage;
    private final float detectionRange;
    private final float attackRange;

    private int attackCooldown = 0;
    private int repathTimer = 0;

    public MeleeAttackGoal(LivingEntity entity, Player player, ChunkManager chunkManager, float attackDamage, float detectionRange, float attackRange) {
        this.entity = entity;
        this.player = player;
        this.chunkManager = chunkManager;
        this.attackDamage = attackDamage;
        this.detectionRange = detectionRange;
        this.attackRange = attackRange;
    }

    @Override
    public boolean shouldExecute() {
        if (player == null || entity.isDead()) return false;
        double dx = player.getPosition().x - entity.getX();
        double dy = player.getPosition().y - entity.getY();
        double dz = player.getPosition().z - entity.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq <= detectionRange * detectionRange;
    }

    @Override
    public void tick() {
        if (player == null) return;

        if (attackCooldown > 0) attackCooldown--;

        double dx = player.getPosition().x - entity.getX();
        double dy = player.getPosition().y - entity.getY();
        double dz = player.getPosition().z - entity.getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        // Path toward player every 20 ticks (1 sec)
        if (--repathTimer <= 0) {
            repathTimer = 20;
            entity.pathTo(player.getPosition().x, player.getPosition().y, player.getPosition().z, chunkManager);
        }

        // Strike when within attack range
        if (distSq <= attackRange * attackRange && attackCooldown <= 0) {
            attackCooldown = 20; // 1 second attack rate
            if (player.getVitals() != null) {
                player.getVitals().damage(attackDamage, "creature_attack");
            }
        }
    }
}
