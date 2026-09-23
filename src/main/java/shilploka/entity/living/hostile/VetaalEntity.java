package shilploka.entity.living.hostile;

import shilploka.ai.MeleeAttackGoal;
import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.DayNightCycle;
import shilploka.world.chunk.ChunkManager;

import java.util.List;

/**
 * WHY:
 * Represents the mythical Vetaal (वेताल), a spectral nocturnal being drawn from Indian folklore
 * and ancient Sanskrit tales (Baital Pachisi).
 *
 * WHAT:
 * Nocturnal entity that hovers above the ground and swoops down to attack unwary travelers at night.
 * Drops sacred ash (Bhasma) when vanquished.
 */
public class VetaalEntity extends LivingEntity {

    private final DayNightCycle dayNightCycle;

    public VetaalEntity(double x, double y, double z, ChunkManager chunkManager, Player player, DayNightCycle dayNightCycle) {
        super(x, y, z, 0.7f, 1.8f, 24.0f);
        this.dayNightCycle = dayNightCycle;
        this.moveSpeed = 4.0f;
        addGoal(new MeleeAttackGoal(this, player, chunkManager, 4.5f, 16.0f, 1.8f));
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public void tick(float dt, ChunkManager chunkManager, Player player) {
        // Vetaal hovers slightly above ground and is dormant or vanishes during bright day
        super.tick(dt, chunkManager, player);
        if (dayNightCycle != null && !dayNightCycle.isNight() && !dead) {
            // Suffers sunlight dispel damage during day if exposed to sky
            damage(1.0f);
        }
    }

    @Override
    public String getEntityName() { return "Vetaal Spirit"; }

    @Override
    public String getHindiName() { return "वेताल (Vetaal)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return List.of(new ItemStack(ItemRegistry.BHASMA, 2));
    }
}
