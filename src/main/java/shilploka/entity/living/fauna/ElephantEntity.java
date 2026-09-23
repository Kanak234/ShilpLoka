package shilploka.entity.living.fauna;

import shilploka.ai.WanderGoal;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.List;

/**
 * WHY:
 * Represents the Asian Indian Elephant (हाथी / ऐरावत का वंशज).
 * Revered for its colossal strength and role in ancient Bharat's architecture and forestry.
 *
 * WHAT:
 * Massive docile creature (100 HP, 1.8x2.4 AABB) with slow, deliberate movement.
 */
public class ElephantEntity extends LivingEntity implements shilploka.entity.living.mount.RideableMount {

    private boolean saddled = false;
    private shilploka.entity.Player rider = null;

    public ElephantEntity(double x, double y, double z, ChunkManager chunkManager) {
        super(x, y, z, 1.8f, 2.4f, 100.0f);
        this.moveSpeed = 2.4f;
        if (chunkManager != null) {
            addGoal(new WanderGoal(this, chunkManager));
        }
    }

    public ElephantEntity(double x, double y, double z) {
        this(x, y, z, null);
    }

    @Override
    public boolean isSaddled() { return saddled; }

    @Override
    public void setSaddled(boolean saddled) { this.saddled = saddled; }

    @Override
    public shilploka.entity.Player getRider() { return rider; }

    @Override
    public void setRider(shilploka.entity.Player rider) { this.rider = rider; }

    @Override
    public boolean isMounted() { return rider != null; }

    @Override
    public void dismount() { this.rider = null; }

    @Override
    public boolean interact(shilploka.entity.Player player, ItemStack heldItem) {
        if (player == null) return false;
        if (!saddled && heldItem != null && !heldItem.isEmpty() && heldItem.getItem() == ItemRegistry.KAATHI) {
            heldItem.shrink(1);
            this.saddled = true;
            return true;
        }
        if (saddled) {
            if (isMounted()) {
                dismount();
            } else {
                setRider(player);
                player.setPosition(x, y + 2.0, z);
            }
            return true;
        }
        return false;
    }

    @Override
    public String getEntityName() { return "Indian Elephant"; }

    @Override
    public String getHindiName() { return "हाथी (Hathi)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return List.of(new ItemStack(ItemRegistry.getItem("shilploka:sal_log"), 3));
    }
}
