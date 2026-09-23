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
 * Represents the Royal Bengal Tiger (रॉयल बंगाल बाघ / व्याघ्र).
 * The apex predator of Bharat's dense jungles and Sundarban mangroves.
 *
 * WHAT:
 * High-speed predator (40 HP, speed 5.5 blocks/sec) with aggressive stalk-and-pounce behavior.
 */
public class TigerEntity extends LivingEntity {

    public TigerEntity(double x, double y, double z, ChunkManager chunkManager, Player player) {
        super(x, y, z, 1.2f, 1.1f, 40.0f);
        this.moveSpeed = 5.5f;
        addGoal(new MeleeAttackGoal(this, player, chunkManager, 6.0f, 18.0f, 2.0f));
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public String getEntityName() { return "Royal Bengal Tiger"; }

    @Override
    public String getHindiName() { return "रॉयल बंगाल बाघ (Baagh)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return Collections.emptyList();
    }
}
