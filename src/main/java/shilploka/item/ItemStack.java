package shilploka.item;

/**
 * WHY:
 * Represents a quantity of items bundled with individual durability damage wear.
 *
 * WHAT:
 * Mutable item stack containing Item reference, item count (1-64), and wear damage.
 * Supports splitting, stacking, and durability depletion logic.
 *
 * USED FOR:
 * Carried in PlayerInventory slots, dropped in ItemEntity, and crafted in CraftingGrid.
 *
 * WHERE NEXT:
 * Displayed in Hotbar and inventory GUI screens.
 */
public class ItemStack {

    public static final ItemStack EMPTY = new ItemStack(null, 0, 0);

    private final Item item;
    private int count;
    private int damage;
    private shilploka.kaushal.KaushalUpgradeType kaushal;

    public ItemStack(Item item) {
        this(item, 1, 0);
    }

    public ItemStack(Item item, int count) {
        this(item, count, 0);
    }

    public ItemStack(Item item, int count, int damage) {
        this.item = item;
        this.count = (item == null) ? 0 : Math.max(0, count);
        this.damage = Math.max(0, damage);
    }

    public Item getItem() { return item; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = Math.max(0, count); }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public boolean isEmpty() {
        return this == EMPTY || this.item == null || this.count <= 0;
    }

    public boolean isFull() {
        return !isEmpty() && this.count >= this.item.getMaxStackSize();
    }

    public void grow(int amount) {
        setCount(this.count + amount);
    }

    public void shrink(int amount) {
        setCount(this.count - amount);
    }

    /**
     * Splits off a portion of this stack up to amount.
     */
    public ItemStack split(int amount) {
        if (isEmpty()) return EMPTY;
        int toTake = Math.min(amount, this.count);
        this.shrink(toTake);
        return new ItemStack(this.item, toTake, this.damage);
    }

    /**
     * Returns true if both stacks hold identical items and damage (stackable).
     */
    public boolean isStackableWith(ItemStack other) {
        if (other == null || other.isEmpty() || this.isEmpty()) return false;
        if (!this.item.getId().equals(other.getItem().getId())) return false;
        if (this.item.isDamageable()) {
            return false; // Damageable tools never stack together
        }
        return true;
    }

    /**
     * Applies durability wear. Returns true if tool has broken.
     */
    public boolean applyDamage(int amount) {
        if (!item.isDamageable()) return false;
        this.damage += amount;
        if (this.damage >= item.getMaxDurability()) {
            this.count = 0;
            return true; // Broken
        }
        return false;
    }

    public shilploka.kaushal.KaushalUpgradeType getKaushal() {
        return kaushal;
    }

    public void setKaushal(shilploka.kaushal.KaushalUpgradeType kaushal) {
        this.kaushal = kaushal;
    }

    public boolean hasKaushal() {
        return kaushal != null;
    }

    public ItemStack copy() {
        if (isEmpty()) return EMPTY;
        ItemStack cp = new ItemStack(this.item, this.count, this.damage);
        cp.setKaushal(this.kaushal);
        return cp;
    }

    @Override
    public String toString() {
        if (isEmpty()) return "Empty Stack";
        return String.format("%dx %s (Damage: %d/%d)",
                count, item.getDisplayName(), damage, item.getMaxDurability());
    }
}
