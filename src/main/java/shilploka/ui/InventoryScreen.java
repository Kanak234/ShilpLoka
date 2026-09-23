package shilploka.ui;

import org.joml.Matrix4f;
import shilploka.crafting.CraftingGrid2x2;
import shilploka.entity.Player;
import shilploka.entity.PlayerInventory;
import shilploka.item.ItemStack;
import shilploka.render.Shader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Provides an interactive 2D GUI for the player's 36-slot inventory and 2x2 personal crafting matrix.
 *
 * WHAT:
 * Renders slot grids, item stack counts, held cursor items, and handles drag-and-drop / click slot swapping.
 *
 * USED FOR:
 * Opened when player presses 'E'.
 *
 * WHERE NEXT:
 * Shaded via ui.vert and ui.frag using an orthographic projection.
 */
public class InventoryScreen {

    public static final int SLOT_SIZE = 32;
    public static final int SLOT_GAP = 4;

    private boolean open = false;
    private ItemStack heldStack = ItemStack.EMPTY;
    private final Matrix4f orthoMatrix = new Matrix4f();

    // Mouse coordinates in screen space
    private double mouseX = 0;
    private double mouseY = 0;

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open, Player player) {
        this.open = open;
        if (!open && player != null) {
            // Return any lingering items in the crafting grid or held in cursor to player inventory
            if (!heldStack.isEmpty()) {
                heldStack = player.getInventory().addItem(heldStack);
            }
            var returned = player.getInventory().getCraftingGrid().clearAndReturnItems();
            for (ItemStack s : returned) {
                player.getInventory().addItem(s);
            }
        }
    }

    public void updateMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    /**
     * WHY:
     * Handles keyboard events (e.g. 'E' or 'Escape' to close inventory).
     */
    public boolean handleKey(int key, int action, Player player) {
        if (!open) return false;
        if (action == GLFW_PRESS) {
            if (key == GLFW_KEY_E || key == GLFW_KEY_ESCAPE) {
                setOpen(false, player);
                return true;
            }
        }
        return true;
    }

    /**
     * WHY:
     * Handles mouse clicks on inventory and crafting slots.
     */
    public boolean handleMouseClick(int button, int action, Player player, int screenWidth, int screenHeight) {
        if (!open || action != GLFW_PRESS || player == null) return false;

        PlayerInventory inv = player.getInventory();
        CraftingGrid2x2 craft = inv.getCraftingGrid();

        float invWidth = (9 * SLOT_SIZE) + (8 * SLOT_GAP) + 32;
        float invHeight = (6 * SLOT_SIZE) + (5 * SLOT_GAP) + 64;
        float startX = (screenWidth - invWidth) / 2.0f;
        float startY = (screenHeight - invHeight) / 2.0f;

        // 1. Check Crafting Matrix Slots (4 slots: 2x2)
        float craftStartX = startX + 40;
        float craftStartY = startY + 36;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                int slot = r * 2 + c;
                float sx = craftStartX + (c * (SLOT_SIZE + SLOT_GAP));
                float sy = craftStartY + (r * (SLOT_SIZE + SLOT_GAP));

                if (isInside(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE)) {
                    handleCraftSlotClick(slot, craft, button);
                    return true;
                }
            }
        }

        // 2. Check Crafting Result Slot
        float resultX = craftStartX + (2 * (SLOT_SIZE + SLOT_GAP)) + 32;
        float resultY = craftStartY + ((SLOT_SIZE + SLOT_GAP) / 2.0f);
        if (isInside(mouseX, mouseY, resultX, resultY, SLOT_SIZE, SLOT_SIZE)) {
            handleResultSlotClick(craft);
            return true;
        }

        // 3. Check Main Inventory Slots (3 rows of 9: indices 9 to 35)
        float mainStartX = startX + 16;
        float mainStartY = startY + 120;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
                float sy = mainStartY + (row * (SLOT_SIZE + SLOT_GAP));

                if (isInside(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE)) {
                    handleInvSlotClick(slotIndex, inv, button);
                    return true;
                }
            }
        }

        // 4. Check Hotbar Slots (1 row of 9: indices 0 to 8)
        float hotbarStartY = mainStartY + (3 * (SLOT_SIZE + SLOT_GAP)) + 16;
        for (int col = 0; col < 9; col++) {
            float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
            float sy = hotbarStartY;

            if (isInside(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE)) {
                handleInvSlotClick(col, inv, button);
                return true;
            }
        }

        return true;
    }

    private void handleInvSlotClick(int slotIndex, PlayerInventory inv, int button) {
        ItemStack slotStack = inv.getSlot(slotIndex);

        if (button == GLFW_MOUSE_BUTTON_1) {
            // Left Click: Full stack swap or merge
            if (heldStack.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    heldStack = slotStack;
                    inv.setSlot(slotIndex, ItemStack.EMPTY);
                }
            } else {
                if (slotStack.isEmpty()) {
                    inv.setSlot(slotIndex, heldStack);
                    heldStack = ItemStack.EMPTY;
                } else if (slotStack.isStackableWith(heldStack)) {
                    int space = slotStack.getItem().getMaxStackSize() - slotStack.getCount();
                    int toAdd = Math.min(space, heldStack.getCount());
                    slotStack.grow(toAdd);
                    heldStack.shrink(toAdd);
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                } else {
                    // Swap held stack with slot stack
                    inv.setSlot(slotIndex, heldStack);
                    heldStack = slotStack;
                }
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
            // Right Click: Split half or drop 1 item
            if (heldStack.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    int half = (slotStack.getCount() + 1) / 2;
                    heldStack = slotStack.split(half);
                    if (slotStack.isEmpty()) inv.setSlot(slotIndex, ItemStack.EMPTY);
                }
            } else {
                if (slotStack.isEmpty()) {
                    inv.setSlot(slotIndex, heldStack.split(1));
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                } else if (slotStack.isStackableWith(heldStack) && !slotStack.isFull()) {
                    slotStack.grow(1);
                    heldStack.shrink(1);
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                }
            }
        }
    }

    private void handleCraftSlotClick(int slot, CraftingGrid2x2 craft, int button) {
        ItemStack slotStack = craft.getMatrixSlot(slot);

        if (button == GLFW_MOUSE_BUTTON_1) {
            if (heldStack.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    heldStack = slotStack;
                    craft.setMatrixSlot(slot, ItemStack.EMPTY);
                }
            } else {
                if (slotStack.isEmpty()) {
                    craft.setMatrixSlot(slot, heldStack);
                    heldStack = ItemStack.EMPTY;
                } else if (slotStack.isStackableWith(heldStack)) {
                    int space = slotStack.getItem().getMaxStackSize() - slotStack.getCount();
                    int toAdd = Math.min(space, heldStack.getCount());
                    slotStack.grow(toAdd);
                    heldStack.shrink(toAdd);
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                    craft.recalculateResult();
                } else {
                    craft.setMatrixSlot(slot, heldStack);
                    heldStack = slotStack;
                }
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
            if (heldStack.isEmpty()) {
                if (!slotStack.isEmpty()) {
                    int half = (slotStack.getCount() + 1) / 2;
                    heldStack = slotStack.split(half);
                    if (slotStack.isEmpty()) craft.setMatrixSlot(slot, ItemStack.EMPTY);
                    craft.recalculateResult();
                }
            } else {
                if (slotStack.isEmpty()) {
                    craft.setMatrixSlot(slot, heldStack.split(1));
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                } else if (slotStack.isStackableWith(heldStack) && !slotStack.isFull()) {
                    slotStack.grow(1);
                    heldStack.shrink(1);
                    if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                    craft.recalculateResult();
                }
            }
        }
    }

    private void handleResultSlotClick(CraftingGrid2x2 craft) {
        ItemStack result = craft.getResultSlot();
        if (result.isEmpty()) return;

        if (heldStack.isEmpty()) {
            heldStack = craft.takeResult();
        } else if (heldStack.isStackableWith(result)) {
            int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
            if (space >= result.getCount()) {
                heldStack.grow(result.getCount());
                craft.takeResult();
            }
        }
    }

    private static boolean isInside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /**
     * WHY:
     * Renders the complete 2D inventory interface and held cursor item onto the screen.
     */
    public void render(BitmapFont font, Shader uiShader, Player player, int screenWidth, int screenHeight) {
        if (!open || player == null) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        orthoMatrix.identity().ortho(0.0f, (float) screenWidth, (float) screenHeight, 0.0f, -1.0f, 1.0f);
        uiShader.bind();
        uiShader.setMat4("u_OrthoProjection", orthoMatrix);
        uiShader.setInt("u_FontTexture", 0);

        PlayerInventory inv = player.getInventory();
        CraftingGrid2x2 craft = inv.getCraftingGrid();

        float invWidth = (9 * SLOT_SIZE) + (8 * SLOT_GAP) + 32;
        float invHeight = (6 * SLOT_SIZE) + (5 * SLOT_GAP) + 64;
        float startX = (screenWidth - invWidth) / 2.0f;
        float startY = (screenHeight - invHeight) / 2.0f;

        // 1. Dark background panel with golden border
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(startX - 2, startY - 2, startX + invWidth + 2, startY + invHeight + 2, 0.85f, 0.65f, 0.20f, 0.95f);
        font.drawRect(startX, startY, startX + invWidth, startY + invHeight, 0.10f, 0.12f, 0.16f, 0.95f);
        font.flush();

        // 2. Titles & Labels
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString("Vastra & Samagri (Inventory)", startX + 16, startY + 12, 1.0f, 0.9f, 0.85f, 0.5f, 1.0f, true);
        font.drawString("Shilp (Crafting)", startX + 40, startY + 24, 1.0f, 0.8f, 0.8f, 0.8f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        // 3. Render 2x2 Crafting Matrix
        float craftStartX = startX + 40;
        float craftStartY = startY + 36;
        for (int r = 0; r < 2; r++) {
            for (int c = 0; c < 2; c++) {
                int slot = r * 2 + c;
                float sx = craftStartX + (c * (SLOT_SIZE + SLOT_GAP));
                float sy = craftStartY + (r * (SLOT_SIZE + SLOT_GAP));
                renderSlot(font, uiShader, sx, sy, craft.getMatrixSlot(slot));
            }
        }

        // Crafting Arrow Symbol
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString("==>", craftStartX + (2 * (SLOT_SIZE + SLOT_GAP)) + 6, craftStartY + 12, 1.0f, 0.9f, 0.8f, 0.3f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        // Crafting Result Slot
        float resultX = craftStartX + (2 * (SLOT_SIZE + SLOT_GAP)) + 36;
        float resultY = craftStartY + ((SLOT_SIZE + SLOT_GAP) / 2.0f);
        renderSlot(font, uiShader, resultX, resultY, craft.getResultSlot());

        // 4. Render Main Backpack Slots (3x9)
        float mainStartX = startX + 16;
        float mainStartY = startY + 120;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
                float sy = mainStartY + (row * (SLOT_SIZE + SLOT_GAP));
                renderSlot(font, uiShader, sx, sy, inv.getSlot(slotIndex));
            }
        }

        // 5. Render Hotbar Slots (1x9)
        float hotbarStartY = mainStartY + (3 * (SLOT_SIZE + SLOT_GAP)) + 16;
        for (int col = 0; col < 9; col++) {
            float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
            renderSlot(font, uiShader, sx, hotbarStartY, inv.getSlot(col));
        }

        // 6. Render Floating Held Cursor Stack
        if (!heldStack.isEmpty()) {
            renderSlot(font, uiShader, (float) mouseX - (SLOT_SIZE / 2.0f), (float) mouseY - (SLOT_SIZE / 2.0f), heldStack);
        }

        uiShader.unbind();
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderSlot(BitmapFont font, Shader uiShader, float x, float y, ItemStack stack) {
        // Slot border & background
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0.05f, 0.06f, 0.08f, 0.90f);
        font.drawRect(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0.18f, 0.20f, 0.25f, 0.85f);
        font.flush();

        // Stack text & count
        if (stack != null && !stack.isEmpty()) {
            uiShader.setBoolean("u_UseTexture", true);
            font.bindTexture();

            // First 2 letters as item shorthand glyph
            String name = stack.getItem().getId();
            int colon = name.indexOf(':');
            String tag = (colon >= 0 && colon + 2 < name.length()) ? name.substring(colon + 1, colon + 3).toUpperCase() : "IT";
            font.drawString(tag, x + 4, y + 4, 1.0f, 0.95f, 0.90f, 0.40f, 1.0f, true);

            // Stack count if > 1
            if (stack.getCount() > 1) {
                String countStr = String.valueOf(stack.getCount());
                font.drawString(countStr, x + SLOT_SIZE - (countStr.length() * 8) - 2, y + SLOT_SIZE - 10,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, true);
            }

            font.flush();
            font.unbindTexture();
        }
    }
}
