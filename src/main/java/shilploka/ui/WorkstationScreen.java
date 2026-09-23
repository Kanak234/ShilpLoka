package shilploka.ui;

import org.joml.Matrix4f;
import shilploka.crafting.CraftingGrid3x3;
import shilploka.entity.Player;
import shilploka.entity.PlayerInventory;
import shilploka.item.ItemStack;
import shilploka.render.Shader;
import shilploka.workstation.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Provides an interactive 2D GUI for the 7 historical Indian crafting workstations:
 * - Karyashala: 3x3 Master Workbench
 * - Pakki Bhatti: Terracotta Kiln (Fuel & Cooking indicators)
 * - Smelting Bhatti: Metallurgical Alloy Furnace
 * - Kumhar Chaak: Potter's Wheel shape selector
 * - Kargah: Indigenous Handloom
 * - Atta Chakki: Rotary Stone Mill
 * - Lohar Nihai: Forged Anvil repair station
 *
 * WHAT:
 * Renders workstation interfaces, monitors craft/burn timers, and handles drag-and-drop slot interactions.
 *
 * USED FOR:
 * Opened when the player right-clicks an in-world workstation block.
 */
public class WorkstationScreen {

    public static final int SLOT_SIZE = 32;
    public static final int SLOT_GAP = 4;

    private boolean open = false;
    private WorkstationType activeType = WorkstationType.KARYASHALA;
    private ItemStack heldStack = ItemStack.EMPTY;
    private final Matrix4f orthoMatrix = new Matrix4f();

    private double mouseX = 0;
    private double mouseY = 0;

    // Subsystem Managers
    private final CraftingGrid3x3 karyashalaGrid = new CraftingGrid3x3();
    private final KilnManager kilnManager = new KilnManager();
    private final AlloyManager alloyManager = new AlloyManager();
    private final ChaakManager chaakManager = new ChaakManager();
    private final LoomManager loomManager = new LoomManager();
    private final ChakkiManager chakkiManager = new ChakkiManager();
    private final NihaiManager nihaiManager = new NihaiManager();

    public boolean isOpen() {
        return open;
    }

    public WorkstationType getActiveType() {
        return activeType;
    }

    public void open(WorkstationType type) {
        this.activeType = (type != null) ? type : WorkstationType.KARYASHALA;
        this.open = true;
    }

    public void close(Player player) {
        this.open = false;
        if (player != null) {
            // Return any held stack
            if (!heldStack.isEmpty()) {
                heldStack = player.getInventory().addItem(heldStack);
            }
            // Return karyashala items
            var returned = karyashalaGrid.clearAndReturnItems();
            for (ItemStack s : returned) {
                player.getInventory().addItem(s);
            }
            // Return input items from workstations
            if (!kilnManager.getInputSlot().isEmpty()) {
                player.getInventory().addItem(kilnManager.getInputSlot());
                kilnManager.setInputSlot(ItemStack.EMPTY);
            }
            if (!kilnManager.getFuelSlot().isEmpty()) {
                player.getInventory().addItem(kilnManager.getFuelSlot());
                kilnManager.setFuelSlot(ItemStack.EMPTY);
            }
            if (!kilnManager.getOutputSlot().isEmpty()) {
                player.getInventory().addItem(kilnManager.getOutputSlot());
                kilnManager.setOutputSlot(ItemStack.EMPTY);
            }
            if (!alloyManager.getPrimarySlot().isEmpty()) {
                player.getInventory().addItem(alloyManager.getPrimarySlot());
                alloyManager.setPrimarySlot(ItemStack.EMPTY);
            }
            if (!alloyManager.getSecondarySlot().isEmpty()) {
                player.getInventory().addItem(alloyManager.getSecondarySlot());
                alloyManager.setSecondarySlot(ItemStack.EMPTY);
            }
            if (!alloyManager.getFuelSlot().isEmpty()) {
                player.getInventory().addItem(alloyManager.getFuelSlot());
                alloyManager.setFuelSlot(ItemStack.EMPTY);
            }
            if (!alloyManager.getOutputSlot().isEmpty()) {
                player.getInventory().addItem(alloyManager.getOutputSlot());
                alloyManager.setOutputSlot(ItemStack.EMPTY);
            }
            if (!chaakManager.getClaySlot().isEmpty()) {
                player.getInventory().addItem(chaakManager.getClaySlot());
                chaakManager.setClaySlot(ItemStack.EMPTY);
            }
            if (!chaakManager.getOutputSlot().isEmpty()) {
                player.getInventory().addItem(chaakManager.getOutputSlot());
                chaakManager.setOutputSlot(ItemStack.EMPTY);
            }
            if (!loomManager.getFiberSlot().isEmpty()) {
                player.getInventory().addItem(loomManager.getFiberSlot());
                loomManager.setFiberSlot(ItemStack.EMPTY);
            }
            if (!loomManager.getOutputSlot().isEmpty()) {
                player.getInventory().addItem(loomManager.getOutputSlot());
                loomManager.setOutputSlot(ItemStack.EMPTY);
            }
            if (!chakkiManager.getGrainSlot().isEmpty()) {
                player.getInventory().addItem(chakkiManager.getGrainSlot());
                chakkiManager.setGrainSlot(ItemStack.EMPTY);
            }
            if (!chakkiManager.getFlourSlot().isEmpty()) {
                player.getInventory().addItem(chakkiManager.getFlourSlot());
                chakkiManager.setFlourSlot(ItemStack.EMPTY);
            }
            if (!nihaiManager.getToolSlot().isEmpty()) {
                player.getInventory().addItem(nihaiManager.getToolSlot());
                nihaiManager.setToolSlot(ItemStack.EMPTY);
            }
            if (!nihaiManager.getMaterialSlot().isEmpty()) {
                player.getInventory().addItem(nihaiManager.getMaterialSlot());
                nihaiManager.setMaterialSlot(ItemStack.EMPTY);
            }
            if (!nihaiManager.getResultSlot().isEmpty()) {
                player.getInventory().addItem(nihaiManager.getResultSlot());
                nihaiManager.takeRepairedTool();
            }
        }
    }

    public void updateMouse(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public void tick() {
        if (!open) return;
        if (activeType == WorkstationType.PAKKI_BHATTI) {
            kilnManager.tick();
        } else if (activeType == WorkstationType.SMELTING_BHATTI) {
            alloyManager.tick();
        }
    }

    public boolean handleKey(int key, int action, Player player) {
        if (!open) return false;
        if (action == GLFW_PRESS) {
            if (key == GLFW_KEY_E || key == GLFW_KEY_ESCAPE) {
                close(player);
                return true;
            }
        }
        return true;
    }

    public boolean handleMouseClick(int button, int action, Player player, int screenWidth, int screenHeight) {
        if (!open || action != GLFW_PRESS || player == null) return false;

        float invWidth = (9 * SLOT_SIZE) + (8 * SLOT_GAP) + 32;
        float invHeight = (7 * SLOT_SIZE) + (6 * SLOT_GAP) + 64;
        float startX = (screenWidth - invWidth) / 2.0f;
        float startY = (screenHeight - invHeight) / 2.0f;

        PlayerInventory inv = player.getInventory();

        // Check workstation specific slot clicks
        switch (activeType) {
            case KARYASHALA -> handleKaryashalaClick(startX, startY, button);
            case PAKKI_BHATTI -> handleKilnClick(startX, startY, button);
            case SMELTING_BHATTI -> handleSmelterClick(startX, startY, button);
            case CHAAK -> handleChaakClick(startX, startY, button);
            case KARGAH -> handleLoomClick(startX, startY, button);
            case CHAKKI -> handleChakkiClick(startX, startY, button);
            case NIHAI -> handleNihaiClick(startX, startY, button);
        }

        // Check Backpack Slots (3x9)
        float mainStartX = startX + 16;
        float mainStartY = startY + 150;
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

        // Check Hotbar Slots (1x9)
        float hotbarStartY = mainStartY + (3 * (SLOT_SIZE + SLOT_GAP)) + 12;
        for (int col = 0; col < 9; col++) {
            float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
            if (isInside(mouseX, mouseY, sx, hotbarStartY, SLOT_SIZE, SLOT_SIZE)) {
                handleInvSlotClick(col, inv, button);
                return true;
            }
        }

        return true;
    }

    private void handleKaryashalaClick(float startX, float startY, int button) {
        float craftStartX = startX + 32;
        float craftStartY = startY + 36;

        // 3x3 matrix slots
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slot = r * 3 + c;
                float sx = craftStartX + (c * (SLOT_SIZE + SLOT_GAP));
                float sy = craftStartY + (r * (SLOT_SIZE + SLOT_GAP));

                if (isInside(mouseX, mouseY, sx, sy, SLOT_SIZE, SLOT_SIZE)) {
                    ItemStack slotStack = karyashalaGrid.getMatrixSlot(slot);
                    if (button == GLFW_MOUSE_BUTTON_1) {
                        if (heldStack.isEmpty()) {
                            if (!slotStack.isEmpty()) {
                                heldStack = slotStack;
                                karyashalaGrid.setMatrixSlot(slot, ItemStack.EMPTY);
                            }
                        } else {
                            if (slotStack.isEmpty()) {
                                karyashalaGrid.setMatrixSlot(slot, heldStack);
                                heldStack = ItemStack.EMPTY;
                            } else if (slotStack.isStackableWith(heldStack)) {
                                int space = slotStack.getItem().getMaxStackSize() - slotStack.getCount();
                                int toAdd = Math.min(space, heldStack.getCount());
                                slotStack.grow(toAdd);
                                heldStack.shrink(toAdd);
                                if (heldStack.isEmpty()) heldStack = ItemStack.EMPTY;
                                karyashalaGrid.updateResult();
                            } else {
                                karyashalaGrid.setMatrixSlot(slot, heldStack);
                                heldStack = slotStack;
                            }
                        }
                    }
                    return;
                }
            }
        }

        // Result slot
        float resultX = craftStartX + (3 * (SLOT_SIZE + SLOT_GAP)) + 36;
        float resultY = craftStartY + (SLOT_SIZE + SLOT_GAP);
        if (isInside(mouseX, mouseY, resultX, resultY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack res = karyashalaGrid.getResultSlot();
            if (!res.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = karyashalaGrid.takeResult();
                } else if (heldStack.isStackableWith(res)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    if (space >= res.getCount()) {
                        heldStack.grow(res.getCount());
                        karyashalaGrid.takeResult();
                    }
                }
            }
        }
    }

    private void handleKilnClick(float startX, float startY, int button) {
        float inX = startX + 60, inY = startY + 40;
        float fuelX = startX + 60, fuelY = startY + 84;
        float outX = startX + 160, outY = startY + 62;

        if (isInside(mouseX, mouseY, inX, inY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack current = kilnManager.getInputSlot();
            kilnManager.setInputSlot(heldStack);
            heldStack = current;
        } else if (isInside(mouseX, mouseY, fuelX, fuelY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack current = kilnManager.getFuelSlot();
            kilnManager.setFuelSlot(heldStack);
            heldStack = current;
        } else if (isInside(mouseX, mouseY, outX, outY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack current = kilnManager.getOutputSlot();
            if (!current.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = current;
                    kilnManager.setOutputSlot(ItemStack.EMPTY);
                } else if (heldStack.isStackableWith(current)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    int toAdd = Math.min(space, current.getCount());
                    heldStack.grow(toAdd);
                    current.shrink(toAdd);
                    if (current.isEmpty()) kilnManager.setOutputSlot(ItemStack.EMPTY);
                }
            }
        }
    }

    private void handleSmelterClick(float startX, float startY, int button) {
        float pX = startX + 40, pY = startY + 40;
        float sX = startX + 80, sY = startY + 40;
        float fuelX = startX + 60, fuelY = startY + 84;
        float outX = startX + 170, outY = startY + 62;

        if (isInside(mouseX, mouseY, pX, pY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = alloyManager.getPrimarySlot();
            alloyManager.setPrimarySlot(heldStack);
            heldStack = cur;
        } else if (isInside(mouseX, mouseY, sX, sY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = alloyManager.getSecondarySlot();
            alloyManager.setSecondarySlot(heldStack);
            heldStack = cur;
        } else if (isInside(mouseX, mouseY, fuelX, fuelY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = alloyManager.getFuelSlot();
            alloyManager.setFuelSlot(heldStack);
            heldStack = cur;
        } else if (isInside(mouseX, mouseY, outX, outY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = alloyManager.getOutputSlot();
            if (!cur.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = cur;
                    alloyManager.setOutputSlot(ItemStack.EMPTY);
                } else if (heldStack.isStackableWith(cur)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    int toAdd = Math.min(space, cur.getCount());
                    heldStack.grow(toAdd);
                    cur.shrink(toAdd);
                    if (cur.isEmpty()) alloyManager.setOutputSlot(ItemStack.EMPTY);
                }
            }
        }
    }

    private void handleChaakClick(float startX, float startY, int button) {
        float clayX = startX + 40, clayY = startY + 50;
        float outX = startX + 220, outY = startY + 50;

        if (isInside(mouseX, mouseY, clayX, clayY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = chaakManager.getClaySlot();
            chaakManager.setClaySlot(heldStack);
            heldStack = cur;
            return;
        }
        if (isInside(mouseX, mouseY, outX, outY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = chaakManager.getOutputSlot();
            if (!cur.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = cur;
                    chaakManager.setOutputSlot(ItemStack.EMPTY);
                } else if (heldStack.isStackableWith(cur)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    int toAdd = Math.min(space, cur.getCount());
                    heldStack.grow(toAdd);
                    cur.shrink(toAdd);
                    if (cur.isEmpty()) chaakManager.setOutputSlot(ItemStack.EMPTY);
                }
            }
            return;
        }

        // Shape buttons (Kulhad, Handi, Matka, Surahi)
        float btnStartX = startX + 90;
        for (int i = 0; i < ChaakManager.SHAPES.size(); i++) {
            float by = startY + 36 + (i * 24);
            if (isInside(mouseX, mouseY, btnStartX, by, 110, 20)) {
                chaakManager.setSelectedShapeIndex(i);
                chaakManager.craftShape(i);
                return;
            }
        }
    }

    private void handleLoomClick(float startX, float startY, int button) {
        float inX = startX + 50, inY = startY + 54;
        float outX = startX + 220, outY = startY + 54;

        if (isInside(mouseX, mouseY, inX, inY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = loomManager.getFiberSlot();
            loomManager.setFiberSlot(heldStack);
            heldStack = cur;
            return;
        }
        if (isInside(mouseX, mouseY, outX, outY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = loomManager.getOutputSlot();
            if (!cur.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = cur;
                    loomManager.setOutputSlot(ItemStack.EMPTY);
                } else if (heldStack.isStackableWith(cur)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    int toAdd = Math.min(space, cur.getCount());
                    heldStack.grow(toAdd);
                    cur.shrink(toAdd);
                    if (cur.isEmpty()) loomManager.setOutputSlot(ItemStack.EMPTY);
                }
            }
            return;
        }

        // Spin button & Weave button
        if (isInside(mouseX, mouseY, startX + 96, startY + 40, 110, 22)) {
            loomManager.spinYarn();
        } else if (isInside(mouseX, mouseY, startX + 96, startY + 70, 110, 22)) {
            loomManager.weaveCloth();
        }
    }

    private void handleChakkiClick(float startX, float startY, int button) {
        float inX = startX + 50, inY = startY + 54;
        float outX = startX + 220, outY = startY + 54;

        if (isInside(mouseX, mouseY, inX, inY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = chakkiManager.getGrainSlot();
            chakkiManager.setGrainSlot(heldStack);
            heldStack = cur;
            return;
        }
        if (isInside(mouseX, mouseY, outX, outY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = chakkiManager.getFlourSlot();
            if (!cur.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = cur;
                    chakkiManager.setFlourSlot(ItemStack.EMPTY);
                } else if (heldStack.isStackableWith(cur)) {
                    int space = heldStack.getItem().getMaxStackSize() - heldStack.getCount();
                    int toAdd = Math.min(space, cur.getCount());
                    heldStack.grow(toAdd);
                    cur.shrink(toAdd);
                    if (cur.isEmpty()) chakkiManager.setFlourSlot(ItemStack.EMPTY);
                }
            }
            return;
        }

        // Rotary Grind Button
        if (isInside(mouseX, mouseY, startX + 96, startY + 50, 110, 30)) {
            chakkiManager.grind();
        }
    }

    private void handleNihaiClick(float startX, float startY, int button) {
        float toolX = startX + 40, toolY = startY + 54;
        float matX = startX + 90, matY = startY + 54;
        float resX = startX + 200, resY = startY + 54;

        if (isInside(mouseX, mouseY, toolX, toolY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = nihaiManager.getToolSlot();
            nihaiManager.setToolSlot(heldStack);
            heldStack = cur;
            return;
        }
        if (isInside(mouseX, mouseY, matX, matY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack cur = nihaiManager.getMaterialSlot();
            nihaiManager.setMaterialSlot(heldStack);
            heldStack = cur;
            return;
        }
        if (isInside(mouseX, mouseY, resX, resY, SLOT_SIZE, SLOT_SIZE)) {
            ItemStack rep = nihaiManager.takeRepairedTool();
            if (!rep.isEmpty()) {
                if (heldStack.isEmpty()) {
                    heldStack = rep;
                } else {
                    heldStack = rep;
                }
            }
        }
    }

    private void handleInvSlotClick(int slotIndex, PlayerInventory inv, int button) {
        ItemStack slotStack = inv.getSlot(slotIndex);

        if (button == GLFW_MOUSE_BUTTON_1) {
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
                    inv.setSlot(slotIndex, heldStack);
                    heldStack = slotStack;
                }
            }
        } else if (button == GLFW_MOUSE_BUTTON_2) {
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

    private static boolean isInside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /**
     * WHY:
     * Renders the workstation GUI according to active workstation type.
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

        float invWidth = (9 * SLOT_SIZE) + (8 * SLOT_GAP) + 32;
        float invHeight = (7 * SLOT_SIZE) + (6 * SLOT_GAP) + 64;
        float startX = (screenWidth - invWidth) / 2.0f;
        float startY = (screenHeight - invHeight) / 2.0f;

        // 1. Dark background panel with golden border
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(startX - 2, startY - 2, startX + invWidth + 2, startY + invHeight + 2, 0.85f, 0.65f, 0.20f, 0.95f);
        font.drawRect(startX, startY, startX + invWidth, startY + invHeight, 0.10f, 0.12f, 0.16f, 0.95f);
        font.flush();

        // 2. Title & Hindi name
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString(activeType.getHindiName(), startX + 16, startY + 12, 1.0f, 0.95f, 0.85f, 0.40f, 1.0f, true);
        font.drawString(activeType.getEnglishDescription(), startX + 16, startY + 24, 0.8f, 0.70f, 0.70f, 0.70f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        // 3. Workstation specific controls & slots
        switch (activeType) {
            case KARYASHALA -> renderKaryashala(font, uiShader, startX, startY);
            case PAKKI_BHATTI -> renderKiln(font, uiShader, startX, startY);
            case SMELTING_BHATTI -> renderSmelter(font, uiShader, startX, startY);
            case CHAAK -> renderChaak(font, uiShader, startX, startY);
            case KARGAH -> renderLoom(font, uiShader, startX, startY);
            case CHAKKI -> renderChakki(font, uiShader, startX, startY);
            case NIHAI -> renderNihai(font, uiShader, startX, startY);
        }

        // 4. Render Main Backpack Slots (3x9)
        float mainStartX = startX + 16;
        float mainStartY = startY + 150;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                float sx = mainStartX + (col * (SLOT_SIZE + SLOT_GAP));
                float sy = mainStartY + (row * (SLOT_SIZE + SLOT_GAP));
                renderSlot(font, uiShader, sx, sy, inv.getSlot(slotIndex));
            }
        }

        // 5. Render Hotbar Slots (1x9)
        float hotbarStartY = mainStartY + (3 * (SLOT_SIZE + SLOT_GAP)) + 12;
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

    private void renderKaryashala(BitmapFont font, Shader uiShader, float startX, float startY) {
        float craftStartX = startX + 32;
        float craftStartY = startY + 36;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int slot = r * 3 + c;
                float sx = craftStartX + (c * (SLOT_SIZE + SLOT_GAP));
                float sy = craftStartY + (r * (SLOT_SIZE + SLOT_GAP));
                renderSlot(font, uiShader, sx, sy, karyashalaGrid.getMatrixSlot(slot));
            }
        }

        // Arrow
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString("==>", craftStartX + (3 * (SLOT_SIZE + SLOT_GAP)) + 8, craftStartY + 36, 1.0f, 0.9f, 0.8f, 0.3f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        // Result
        float resultX = craftStartX + (3 * (SLOT_SIZE + SLOT_GAP)) + 36;
        float resultY = craftStartY + (SLOT_SIZE + SLOT_GAP);
        renderSlot(font, uiShader, resultX, resultY, karyashalaGrid.getResultSlot());
    }

    private void renderKiln(BitmapFont font, Shader uiShader, float startX, float startY) {
        float inX = startX + 60, inY = startY + 40;
        float fuelX = startX + 60, fuelY = startY + 84;
        float outX = startX + 160, outY = startY + 62;

        renderSlot(font, uiShader, inX, inY, kilnManager.getInputSlot());
        renderSlot(font, uiShader, fuelX, fuelY, kilnManager.getFuelSlot());
        renderSlot(font, uiShader, outX, outY, kilnManager.getOutputSlot());

        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        if (kilnManager.isBurning()) {
            font.drawString("FIRE", inX + 2, inY + 36, 0.8f, 1.0f, 0.4f, 0.1f, 1.0f, true);
        }
        font.drawString("==>", inX + 44, outY + 10, 1.0f, 0.9f, 0.8f, 0.3f, 1.0f, true);
        font.flush();
        font.unbindTexture();
    }

    private void renderSmelter(BitmapFont font, Shader uiShader, float startX, float startY) {
        float pX = startX + 40, pY = startY + 40;
        float sX = startX + 80, sY = startY + 40;
        float fuelX = startX + 60, fuelY = startY + 84;
        float outX = startX + 170, outY = startY + 62;

        renderSlot(font, uiShader, pX, pY, alloyManager.getPrimarySlot());
        renderSlot(font, uiShader, sX, sY, alloyManager.getSecondarySlot());
        renderSlot(font, uiShader, fuelX, fuelY, alloyManager.getFuelSlot());
        renderSlot(font, uiShader, outX, outY, alloyManager.getOutputSlot());

        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        if (alloyManager.isBurning()) {
            font.drawString("MELT", fuelX + 2, fuelY - 10, 0.8f, 1.0f, 0.5f, 0.1f, 1.0f, true);
        }
        font.drawString("==>", sX + 40, outY + 10, 1.0f, 0.9f, 0.8f, 0.3f, 1.0f, true);
        font.flush();
        font.unbindTexture();
    }

    private void renderChaak(BitmapFont font, Shader uiShader, float startX, float startY) {
        float clayX = startX + 40, clayY = startY + 50;
        float outX = startX + 220, outY = startY + 50;

        renderSlot(font, uiShader, clayX, clayY, chaakManager.getClaySlot());
        renderSlot(font, uiShader, outX, outY, chaakManager.getOutputSlot());

        float btnStartX = startX + 90;
        for (int i = 0; i < ChaakManager.SHAPES.size(); i++) {
            float by = startY + 36 + (i * 24);
            boolean sel = (i == chaakManager.getSelectedShapeIndex());

            uiShader.setBoolean("u_UseTexture", false);
            if (sel) {
                font.drawRect(btnStartX - 1, by - 1, btnStartX + 111, by + 21, 1.0f, 0.8f, 0.2f, 0.9f);
                font.drawRect(btnStartX, by, btnStartX + 110, by + 20, 0.25f, 0.22f, 0.15f, 0.9f);
            } else {
                font.drawRect(btnStartX, by, btnStartX + 110, by + 20, 0.16f, 0.18f, 0.22f, 0.85f);
            }
            font.flush();

            uiShader.setBoolean("u_UseTexture", true);
            font.bindTexture();
            font.drawString(ChaakManager.SHAPES.get(i).id().toUpperCase(), btnStartX + 6, by + 6, 0.9f, 1.0f, 1.0f, 1.0f, 1.0f, true);
            font.flush();
            font.unbindTexture();
        }
    }

    private void renderLoom(BitmapFont font, Shader uiShader, float startX, float startY) {
        float inX = startX + 50, inY = startY + 54;
        float outX = startX + 220, outY = startY + 54;

        renderSlot(font, uiShader, inX, inY, loomManager.getFiberSlot());
        renderSlot(font, uiShader, outX, outY, loomManager.getOutputSlot());

        // Buttons
        renderButton(font, uiShader, startX + 96, startY + 40, 110, 22, "SPIN YARN");
        renderButton(font, uiShader, startX + 96, startY + 70, 110, 22, "WEAVE KHADI");
    }

    private void renderChakki(BitmapFont font, Shader uiShader, float startX, float startY) {
        float inX = startX + 50, inY = startY + 54;
        float outX = startX + 220, outY = startY + 54;

        renderSlot(font, uiShader, inX, inY, chakkiManager.getGrainSlot());
        renderSlot(font, uiShader, outX, outY, chakkiManager.getFlourSlot());

        renderButton(font, uiShader, startX + 96, startY + 50, 110, 30, "GRIND (पीसो)");
    }

    private void renderNihai(BitmapFont font, Shader uiShader, float startX, float startY) {
        float toolX = startX + 40, toolY = startY + 54;
        float matX = startX + 90, matY = startY + 54;
        float resX = startX + 200, resY = startY + 54;

        renderSlot(font, uiShader, toolX, toolY, nihaiManager.getToolSlot());
        renderSlot(font, uiShader, matX, matY, nihaiManager.getMaterialSlot());
        renderSlot(font, uiShader, resX, resY, nihaiManager.getResultSlot());

        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString("+", toolX + 38, toolY + 10, 1.0f, 0.8f, 0.8f, 0.8f, 1.0f, true);
        font.drawString("==>", matX + 44, toolY + 10, 1.0f, 0.9f, 0.8f, 0.3f, 1.0f, true);
        font.flush();
        font.unbindTexture();
    }

    private void renderButton(BitmapFont font, Shader uiShader, float x, float y, float w, float h, String text) {
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(x, y, x + w, y + h, 0.20f, 0.24f, 0.30f, 0.9f);
        font.drawRect(x + 1, y + 1, x + w - 1, y + h - 1, 0.28f, 0.32f, 0.40f, 0.85f);
        font.flush();

        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString(text, x + 8, y + (h / 2.0f) - 4, 0.9f, 1.0f, 0.95f, 0.80f, 1.0f, true);
        font.flush();
        font.unbindTexture();
    }

    private void renderSlot(BitmapFont font, Shader uiShader, float x, float y, ItemStack stack) {
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0.05f, 0.06f, 0.08f, 0.90f);
        font.drawRect(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0.18f, 0.20f, 0.25f, 0.85f);
        font.flush();

        if (stack != null && !stack.isEmpty()) {
            uiShader.setBoolean("u_UseTexture", true);
            font.bindTexture();

            String name = stack.getItem().getId();
            int colon = name.indexOf(':');
            String tag = (colon >= 0 && colon + 2 < name.length()) ? name.substring(colon + 1, colon + 3).toUpperCase() : "IT";
            font.drawString(tag, x + 4, y + 4, 1.0f, 0.95f, 0.90f, 0.40f, 1.0f, true);

            if (stack.getCount() > 1) {
                String countStr = String.valueOf(stack.getCount());
                font.drawString(countStr, x + SLOT_SIZE - (countStr.length() * 8) - 2, y + SLOT_SIZE - 10,
                        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, true);
            }

            font.flush();
            font.unbindTexture();
        }
    }

    public CraftingGrid3x3 getKaryashalaGrid() { return karyashalaGrid; }
    public KilnManager getKilnManager() { return kilnManager; }
    public AlloyManager getAlloyManager() { return alloyManager; }
    public ChaakManager getChaakManager() { return chaakManager; }
    public LoomManager getLoomManager() { return loomManager; }
    public ChakkiManager getChakkiManager() { return chakkiManager; }
    public NihaiManager getNihaiManager() { return nihaiManager; }
}
