package shilploka.entity.living.hostile;

import shilploka.ai.MeleeAttackGoal;
import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.List;

/**
 * WHY:
 * Represents historical forest and ravine bandits (डाकू / Daaku) who preyed on ancient trade routes.
 *
 * WHAT:
 * Humanoid hostile entity armed with an iron or bronze Talwar, patrolling wilderness canyons and ruins.
 * Drops copper barter mudras upon defeat.
 */
public class DaakuEntity extends LivingEntity {

    public DaakuEntity(double x, double y, double z) {
        super(x, y, z, 0.6f, 1.8f, 30.0f);
        this.moveSpeed = 4.2f;
    }

    public DaakuEntity(double x, double y, double z, ChunkManager chunkManager, Player player) {
        this(x, y, z);
        addGoal(new MeleeAttackGoal(this, player, chunkManager, 5.0f, 16.0f, 1.8f));
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public String getEntityName() { return "Wilderness Daaku"; }

    @Override
    public String getHindiName() { return "डाकू (Daaku)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return List.of(new ItemStack(ItemRegistry.MUDRA_COPPER, 3));
    }
}
