package shilploka.ui;

import shilploka.world.BlockIds;

/**
 * WHY:
 * Provides a 9-slot creative hotbar HUD allowing the player to select, inspect,
 * and place authentic Indian blocks in real time.
 *
 * WHAT:
 * Encapsulates a 9-element array of block IDs, active slot index selection via number keys
 * or mouse scroll wheel, and 2D quad rendering for the HUD bar and selected item name label.
 *
 * USED FOR:
 * Consulted on right-click block placement and rendered during Game.render().
 *
 * WHERE NEXT:
 * Used by Game to supply block IDs to ChunkManager.setBlock().
 */
public class Hotbar {

    public static final int SLOT_COUNT = 9;
    public static final int SLOT_SIZE = 28;
    public static final int SLOT_GAP = 4;

    private final int[] slots = new int[SLOT_COUNT];
    private int selectedSlot = 0;

    public Hotbar() {
        // Pre-populate with foundational Indian creative building blocks
        slots[0] = BlockIds.FIRED_BRICK;
        slots[1] = BlockIds.SAL_PLANKS;
        slots[2] = BlockIds.SAL_LOG;
        slots[3] = BlockIds.DIYA;
        slots[4] = BlockIds.RED_SANDSTONE;
        slots[5] = BlockIds.YELLOW_SANDSTONE;
        slots[6] = BlockIds.ALLUVIAL_SOIL;
        slots[7] = BlockIds.HIMALAYAN_SNOW;
        slots[8] = BlockIds.THAR_SAND;
    }

    public int getSelectedBlockId() {
        return slots[selectedSlot];
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public void selectSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < SLOT_COUNT) {
            this.selectedSlot = slotIndex;
        }
    }

    /**
     * Cycles hotbar slot via mouse scroll wheel (+1 or -1 with wrap-around).
     */
    public void scroll(int direction) {
        if (direction > 0) {
            this.selectedSlot = (this.selectedSlot - 1 + SLOT_COUNT) % SLOT_COUNT;
        } else if (direction < 0) {
            this.selectedSlot = (this.selectedSlot + 1) % SLOT_COUNT;
        }
    }

    public String getBlockName(int blockId) {
        return switch (blockId) {
            case BlockIds.FIRED_BRICK -> "Harappan Fired Brick (पक्की ईंट)";
            case BlockIds.SAL_PLANKS -> "Sal Wood Planks (साल की लकड़ी)";
            case BlockIds.SAL_LOG -> "Sal Timber Log (साल का लट्ठा)";
            case BlockIds.DIYA -> "Clay Diya (मिट्टी का दीया)";
            case BlockIds.RED_SANDSTONE -> "Red Sandstone (लाल बलुआ पत्थर)";
            case BlockIds.YELLOW_SANDSTONE -> "Yellow Sandstone (पीला बलुआ पत्थर)";
            case BlockIds.ALLUVIAL_SOIL -> "Alluvial Soil (जलोढ़ मिट्टी)";
            case BlockIds.HIMALAYAN_SNOW -> "Himalayan Snow (हिमालयी हिम)";
            case BlockIds.THAR_SAND -> "Thar Desert Sand (थार की रेत)";
            default -> "Block #" + blockId;
        };
    }

    /**
     * WHY:
     * Draws the 9-slot hotbar HUD at the bottom center of the display.
     *
     * WHAT:
     * Renders background container quad, slot divider boxes, gold selection highlight,
     * slot numbers, and active item name.
     */
    public void render(BitmapFont font, int screenWidth, int screenHeight) {
        int totalWidth = (SLOT_COUNT * SLOT_SIZE) + ((SLOT_COUNT - 1) * SLOT_GAP) + 8;
        int barHeight = SLOT_SIZE + 8;
        float startX = (screenWidth - totalWidth) / 2.0f;
        float startY = screenHeight - barHeight - 12.0f;

        // 1. Dark translucent background tray
        font.drawRect(startX, startY, startX + totalWidth, startY + barHeight, 0.05f, 0.05f, 0.07f, 0.75f);

        // 2. Individual slots
        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotX = startX + 4.0f + (i * (SLOT_SIZE + SLOT_GAP));
            float slotY = startY + 4.0f;

            if (i == selectedSlot) {
                // Golden highlight border for active slot
                font.drawRect(slotX - 2, slotY - 2, slotX + SLOT_SIZE + 2, slotY + SLOT_SIZE + 2, 1.0f, 0.85f, 0.2f, 0.9f);
                font.drawRect(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0.25f, 0.22f, 0.15f, 0.85f);
            } else {
                // Subtle slot outline
                font.drawRect(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0.15f, 0.15f, 0.18f, 0.8f);
            }

            // Slot number indicator (1-9) in top-left of slot
            font.drawString(String.valueOf(i + 1), slotX + 2, slotY + 2, 1.0f, 0.7f, 0.7f, 0.7f, 0.9f, true);
        }

        // 3. Active block label centered above hotbar
        String activeName = getBlockName(getSelectedBlockId());
        float labelWidth = activeName.length() * 8.0f;
        float labelX = (screenWidth - labelWidth) / 2.0f;
        float labelY = startY - 14.0f;
        font.drawString(activeName, labelX, labelY, 1.0f, 1.0f, 0.9f, 0.5f, 1.0f, true);
    }
}
