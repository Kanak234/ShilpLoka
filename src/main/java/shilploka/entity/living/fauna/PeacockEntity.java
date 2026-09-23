package shilploka.entity.living.fauna;

import shilploka.ai.PanicGoal;
import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.List;

/**
 * WHY:
 * Represents the Indian Peafowl (मयूर / मोर), the sacred national bird of Bharat.
 * Known for its majestic plumage and vigilance.
 *
 * WHAT:
 * Agile bird that wanders peacefully, flees in panic when a player approaches quickly,
 * and sheds iridescent feathers (Mor Pankh).
 */
public class PeacockEntity extends LivingEntity {

    private int featherShedTimer = 2400; // Drops feather periodically

    public PeacockEntity(double x, double y, double z, ChunkManager chunkManager, Player player) {
        super(x, y, z, 0.6f, 0.8f, 10.0f);
        this.moveSpeed = 3.5f;
        addGoal(new PanicGoal(this, player, chunkManager, 6.0f));
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public void tick(float dt, ChunkManager chunkManager, Player player) {
        super.tick(dt, chunkManager, player);
        if (--featherShedTimer <= 0) {
            featherShedTimer = 3600; // Every 3 minutes
        }
    }

    @Override
    public String getEntityName() { return "Indian Peacock"; }

    @Override
    public String getHindiName() { return "मयूर / मोर (Mor)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return List.of(new ItemStack(ItemRegistry.MOR_PANKH, 1));
    }
}
