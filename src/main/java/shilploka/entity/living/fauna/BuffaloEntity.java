package shilploka.entity.living.fauna;

import shilploka.ai.WanderGoal;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.Collections;
import java.util.List;

/**
 * WHY:
 * Represents the Indian Water Buffalo (जल भैंस).
 * Adapted to marshlands, alluvial floodplains, and river valleys.
 *
 * WHAT:
 * Sturdy, peaceful herbivore with high endurance.
 */
public class BuffaloEntity extends LivingEntity {

    public BuffaloEntity(double x, double y, double z, ChunkManager chunkManager) {
        super(x, y, z, 1.0f, 1.3f, 35.0f);
        this.moveSpeed = 2.2f;
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public String getEntityName() { return "Water Buffalo"; }

    @Override
    public String getHindiName() { return "भैंस (Bhains)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return Collections.emptyList();
    }
}
