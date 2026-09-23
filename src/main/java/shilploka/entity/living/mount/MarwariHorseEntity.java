package shilploka.entity.living.mount;

import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.List;

/**
 * WHY:
 * Represents the famed Marwari Horse (मारवाड़ी अश्व) of Rajasthan.
 * Renowned for its distinctive lyre-shaped inward-turning ears, high speed, and desert stamina.
 *
 * WHAT:
 * High-speed rideable mount (35 HP, moveSpeed 7.5f) saddled with the traditional Kaathi.
 */
public class MarwariHorseEntity extends LivingEntity implements RideableMount {

    private boolean saddled = false;
    private Player rider = null;

    public MarwariHorseEntity(double x, double y, double z, ChunkManager chunkManager) {
        super(x, y, z, 1.2f, 1.6f, 35.0f);
        this.moveSpeed = 7.5f;
        if (chunkManager != null) {
            addGoal(new WanderGoal(this, chunkManager));
        }
    }

    public MarwariHorseEntity(double x, double y, double z) {
        this(x, y, z, null);
    }

    @Override
    public boolean isSaddled() {
        return saddled;
    }

    @Override
    public void setSaddled(boolean saddled) {
        this.saddled = saddled;
    }

    @Override
    public Player getRider() {
        return rider;
    }

    @Override
    public void setRider(Player rider) {
        this.rider = rider;
    }

    @Override
    public boolean isMounted() {
        return rider != null;
    }

    @Override
    public void dismount() {
        this.rider = null;
    }

    @Override
    public boolean interact(Player player, ItemStack heldItem) {
        if (player == null) return false;

        // 1. Equip saddle
        if (!saddled && heldItem != null && !heldItem.isEmpty() && heldItem.getItem() == ItemRegistry.KAATHI) {
            heldItem.shrink(1);
            this.saddled = true;
            return true;
        }

        // 2. Mount / Dismount
        if (saddled) {
            if (isMounted()) {
                dismount();
            } else {
                setRider(player);
                player.setPosition(x, y + 1.2, z);
            }
            return true;
        }

        return false;
    }

    @Override
    public String getEntityName() {
        return "Marwari Horse";
    }

    @Override
    public String getHindiName() {
        return "मारवाड़ी अश्व (Marwari Ashwa)";
    }

    @Override
    public List<ItemStack> getDeathDrops() {
        return List.of(new ItemStack(ItemRegistry.LAKDI_CHHAD, 2));
    }
}
