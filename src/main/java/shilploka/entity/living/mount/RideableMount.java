package shilploka.entity.living.mount;

import shilploka.entity.Player;
import shilploka.item.ItemStack;

/**
 * WHY:
 * Common contract for rideable animals across Bharat (Marwari Horse, Royal Elephant).
 *
 * WHAT:
 * Governs saddling with Kaathi, mounting/dismounting, and player control.
 */
public interface RideableMount {
    boolean isSaddled();
    void setSaddled(boolean saddled);
    boolean interact(Player player, ItemStack heldItem);
    Player getRider();
    void setRider(Player rider);
    boolean isMounted();
    void dismount();
}
