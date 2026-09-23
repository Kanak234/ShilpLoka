package shilploka.entity.living.fauna;

import shilploka.ai.WanderGoal;
import shilploka.entity.Player;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.chunk.ChunkManager;

import java.util.Collections;
import java.util.List;

/**
 * WHY:
 * Represents the revered Indian indigenous Zebu Cow (गौ / कामधेनु का प्रतीक).
 * In accordance with ancient Indian cultural ethos:
 * - Cows are completely peaceful and protected: player attacks deal NO damage and cause NO harm.
 * - Cows NEVER drop meat and cannot be slaughtered.
 * - Gentle interaction with an earthen vessel (Kulhad/Matka) yields fresh sacred milk (Doodh).
 *
 * WHAT:
 * Docile herbivore that grazes along grassy river plains and village pastures.
 */
public class GauEntity extends LivingEntity {

    private int milkCooldown = 0;

    public GauEntity(double x, double y, double z, ChunkManager chunkManager) {
        super(x, y, z, 0.9f, 1.4f, 30.0f);
        this.moveSpeed = 2.0f;
        addGoal(new WanderGoal(this, chunkManager));
    }

    @Override
    public void tick(float dt, ChunkManager chunkManager, Player player) {
        super.tick(dt, chunkManager, player);
        if (milkCooldown > 0) milkCooldown--;
    }

    @Override
    public void damage(float amount) {
        // Cultural mandate: Gau is completely protected and unharmed by player or weapons
        this.hurtTime = 5;
    }

    /**
     * Milking interaction: Player with earthen cup/pot receives sacred milk.
     */
    public boolean interact(Player player) {
        if (player == null || player.getInventory() == null || milkCooldown > 0) return false;

        for (int i = 0; i < shilploka.entity.PlayerInventory.HOTBAR_SIZE; i++) {
            ItemStack held = player.getInventory().getSlot(i);
            if (!held.isEmpty() && (held.getItem() == ItemRegistry.KULHAD || held.getItem() == ItemRegistry.MATKA)) {
                held.shrink(1);
                if (held.isEmpty()) player.getInventory().setSlot(i, ItemStack.EMPTY);
                player.getInventory().addItem(new ItemStack(ItemRegistry.DOODH, 1));
                this.milkCooldown = 1200; // 1 minute cooldown between milking
                return true;
            }
        }
        return false;
    }

    @Override
    public String getEntityName() { return "Indian Zebu Cow"; }

    @Override
    public String getHindiName() { return "गौ माता (Gau)"; }

    @Override
    public List<ItemStack> getDeathDrops() {
        return Collections.emptyList(); // Cows never drop meat or loot
    }
}
