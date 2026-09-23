package shilploka.entity.living.boss;

import shilploka.ai.AIGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.entity.living.hostile.CobraEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Represents the mythical subterranean cosmic serpent guardian of Paataal:
 * "Andhakaar Naag" (अन्धकार नाग — The Serpent of the Primordial Dark).
 *
 * WHAT:
 * Colossal multi-phase boss entity (300 HP, 2.8x2.8 AABB) inhabiting the volcanic
 * subterranean chambers of Paataal.
 * - Phase 1 (100% - 50% HP): Shadow gliding, circling the player, delivering venomous bites.
 * - Phase 2 (< 50% HP, Enraged): Ignites in volcanic fury, doubles movement speed, charges relentlessly,
 *   and summons attendant Paataal Naag minion serpents.
 * - Upon defeat: Relinquishes the sacred Vajra Core (इन्द्र वज्र मणिक), the key to forging
 *   indestructible Vajra tools and unlocking the celestial ascension to Akash Dweep.
 */
public class AndhakaarNaagEntity extends LivingEntity {

    private boolean enraged = false;
    private float attackTimer = 0.0f;
    private final List<CobraEntity> minions = new ArrayList<>();
    private Player currentTarget = null;

    public AndhakaarNaagEntity(double x, double y, double z, ChunkManager chunkManager, Player player) {
        super(x, y, z, 2.8f, 2.8f, 300.0f);
        this.moveSpeed = 3.6f;
        this.currentTarget = player;

        addGoal(new AndhakaarBossAI(this, chunkManager));
    }

    public AndhakaarNaagEntity(double x, double y, double z) {
        this(x, y, z, null, null);
    }

    public boolean isEnraged() {
        return this.enraged || (this.health <= this.maxHealth * 0.5f);
    }

    public float getHealthFraction() {
        return Math.max(0.0f, Math.min(1.0f, this.health / this.maxHealth));
    }

    public Player getCurrentTarget() {
        return currentTarget;
    }

    public void setCurrentTarget(Player player) {
        this.currentTarget = player;
    }

    public List<CobraEntity> getMinions() {
        minions.removeIf(LivingEntity::isDead);
        return minions;
    }

    public void spawnMinion(ChunkManager chunkManager) {
        if (getMinions().size() >= 3) return;
        double spawnX = this.x + (Math.random() - 0.5) * 6.0;
        double spawnZ = this.z + (Math.random() - 0.5) * 6.0;
        CobraEntity minion = new CobraEntity(spawnX, this.y, spawnZ, chunkManager, currentTarget);
        minions.add(minion);
    }

    @Override
    public void tick(float dt, ChunkManager chunkManager, Player player) {
        super.tick(dt, chunkManager, player);
        if (player != null) {
            this.currentTarget = player;
        }

        // Check enraged transition
        if (!enraged && this.health <= this.maxHealth * 0.5f) {
            this.enraged = true;
            this.moveSpeed = 5.4f; // Surge in velocity
        }

        if (attackTimer > 0.0f) {
            attackTimer = Math.max(0.0f, attackTimer - dt);
        }

        // Minion summoning during enraged phase
        if (enraged && chunkManager != null && currentTarget != null) {
            if (Math.random() < 0.005) { // Periodic spawn check
                spawnMinion(chunkManager);
            }
        }
    }

    @Override
    public String getEntityName() {
        return isEnraged() ? "Andhakaar Naag (Krodhit / Enraged)" : "Andhakaar Naag";
    }

    @Override
    public String getHindiName() {
        return isEnraged() ? "अन्धकार नाग (क्रोधित रूप)" : "अन्धकार नाग (पाताल महासर्प)";
    }

    @Override
    public List<ItemStack> getDeathDrops() {
        List<ItemStack> drops = new ArrayList<>();
        // Defeating the legendary guardian yields the divine Vajra Core
        drops.add(new ItemStack(ItemRegistry.VAJRA_CORE, 1));
        drops.add(new ItemStack(ItemRegistry.SONA_INGOT, 8));
        drops.add(new ItemStack(ItemRegistry.BHASMA, 16));
        return drops;
    }

    /**
     * Specialized Boss AI for Andhakaar Naag combat dynamics.
     */
    private static class AndhakaarBossAI implements AIGoal {
        private final AndhakaarNaagEntity boss;
        private final ChunkManager chunkManager;
        private float repathCooldown = 0.0f;

        public AndhakaarBossAI(AndhakaarNaagEntity boss, ChunkManager chunkManager) {
            this.boss = boss;
            this.chunkManager = chunkManager;
        }

        @Override
        public boolean shouldExecute() {
            return !boss.isDead() && boss.getCurrentTarget() != null;
        }

        @Override
        public void start() {
            repathCooldown = 0.0f;
        }

        @Override
        public void tick() {
            Player target = boss.getCurrentTarget();
            if (target == null || chunkManager == null) return;

            repathCooldown -= 0.05f;
            double dx = target.getPosition().x - boss.getX();
            double dy = target.getPosition().y - boss.getY();
            double dz = target.getPosition().z - boss.getZ();
            double distSq = dx * dx + dz * dz;

            // Direct melee strike
            if (distSq <= 9.0 && boss.attackTimer <= 0.0f) {
                float dmg = boss.isEnraged() ? 7.0f : 4.0f;
                target.getVitals().damage(dmg, "Andhakaar Naag (अन्धकार नाग)");
                boss.attackTimer = boss.isEnraged() ? 0.8f : 1.5f;
            }

            // Path towards player
            if (repathCooldown <= 0.0f) {
                boss.pathTo(target.getPosition().x, target.getPosition().y, target.getPosition().z, chunkManager);
                repathCooldown = boss.isEnraged() ? 0.8f : 1.6f;
            }
        }

        @Override
        public boolean shouldContinue() {
            return shouldExecute();
        }
    }
}
