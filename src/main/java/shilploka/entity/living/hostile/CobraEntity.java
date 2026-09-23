package shilploka.entity.living.hostile;

import shilploka.ai.MeleeAttackGoal;
import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.Collections;
import java.util.List;

/**
 * WHY:
 * Represents the Indian King Cobra (नागराज / कालसर्प).
 * Lethal venomous serpent lurking in rocky crevices, ruins, and dense brush.
 *
 * WHAT:
 * Low-profile predator (14 HP, 0.4x0.5 AABB) dealing venomous strikes and exhaustion.
 */
public class CobraEntity extends LivingEntity {

    public CobraEntity(double x, double y, double z, ChunkManager chunkManager, Player player) {
        super(x, y, z, 0.4f, 0.5f, 14.0f);
        this.moveSpeed = 3.2f;
        addGoal(new MeleeAttackGoal(this, player, chunkManager, 3.5f, 10.0f, 1.5f));
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public String getEntityName() { return "King Cobra"; }

    @Override
    public String getHindiName() { return "नागराज (Naag)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return Collections.emptyList();
    }
}
