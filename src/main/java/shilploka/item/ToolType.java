package shilploka.item;

/**
 * WHY:
 * Classifies tool functions to determine effective block harvesting categories
 * (e.g. Kudaal for stones, Kulhadi for timber, Phaavda for earth/sand).
 *
 * WHAT:
 * Enum covering Indian craft tool types: Kudaal (Pickaxe), Kulhadi (Axe),
 * Phaavda (Shovel), Talwar (Sword), and None.
 *
 * USED FOR:
 * Checking preferred harvest tools against block material types in BlockIds.
 *
 * WHERE NEXT:
 * Assigned to items in ItemRegistry and evaluated when Player destroys blocks.
 */
public enum ToolType {
    KUDAAL("Kudaal (Pickaxe)"),
    KULHADI("Kulhadi (Axe)"),
    PHAAVDA("Phaavda (Shovel)"),
    TALWAR("Talwar (Sword)"),
    NONE("None");

    private final String displayName;

    ToolType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
