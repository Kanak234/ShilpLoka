package shilploka.ui;

import org.joml.Matrix4f;
import shilploka.entity.Player;
import shilploka.entity.PlayerInventory;
import shilploka.entity.living.npc.GramwasiEntity;
import shilploka.item.ItemStack;
import shilploka.render.Shader;
import shilploka.trade.TradeOffer;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Provides the interactive 2D Barter Trading interface between the player and Indian villagers (Gramwasi).
 * Supports viewing authentic trade offers, selecting transactions, and completing trades with
 * traditional currencies (Kauri, Taamba Mudra, Chandi Mudra, Swarna Mudra) and local artisanal goods.
 *
 * WHAT:
 * Renders dual-pane trade menu and handles keyboard/mouse trade executions.
 *
 * USED FOR:
 * Opened when the player right-clicks a GramwasiEntity in the world.
 */
public class BarterScreen {

    private boolean open = false;
    private GramwasiEntity activeVillager;
    private int selectedTradeIndex = 0;
    private final Matrix4f orthoMatrix = new Matrix4f();

    public boolean isOpen() {
        return open;
    }

    public void open(GramwasiEntity villager) {
        this.activeVillager = villager;
        this.selectedTradeIndex = 0;
        this.open = true;
    }

    public void close() {
        this.open = false;
        this.activeVillager = null;
    }

    public GramwasiEntity getActiveVillager() {
        return activeVillager;
    }

    public int getSelectedTradeIndex() {
        return selectedTradeIndex;
    }

    public void selectTrade(int index) {
        if (activeVillager != null && !activeVillager.getTradeOffers().isEmpty()) {
            this.selectedTradeIndex = Math.max(0, Math.min(index, activeVillager.getTradeOffers().size() - 1));
        }
    }

    public TradeOffer getSelectedTrade() {
        if (activeVillager == null) return null;
        List<TradeOffer> offers = activeVillager.getTradeOffers();
        if (offers.isEmpty()) return null;
        int clamped = Math.max(0, Math.min(selectedTradeIndex, offers.size() - 1));
        return offers.get(clamped);
    }

    /**
     * Attempts to execute the currently selected trade using items from the player's inventory.
     *
     * @return true if the trade succeeded and output was placed into inventory; false otherwise.
     */
    public boolean performTrade(Player player) {
        if (player == null || activeVillager == null) return false;
        TradeOffer offer = getSelectedTrade();
        if (offer == null || offer.isOutOfStock()) return false;

        PlayerInventory inv = player.getInventory();
        ItemStack costA = offer.getCostA();
        ItemStack costB = offer.getCostB();

        // Check if player has sufficient items for costA and costB
        if (!hasItemInInventory(inv, costA)) return false;
        if (!costB.isEmpty() && !hasItemInInventory(inv, costB)) return false;

        // Deduct cost items
        deductFromInventory(inv, costA);
        if (!costB.isEmpty()) {
            deductFromInventory(inv, costB);
        }

        // Add reward stack to player inventory
        ItemStack result = offer.getResult().copy();
        inv.addItem(result);
        return true;
    }

    private boolean hasItemInInventory(PlayerInventory inv, ItemStack cost) {
        if (cost.isEmpty()) return true;
        int total = 0;
        for (int i = 0; i < PlayerInventory.TOTAL_SLOTS; i++) {
            ItemStack stack = inv.getSlot(i);
            if (!stack.isEmpty() && stack.getItem() == cost.getItem()) {
                total += stack.getCount();
            }
        }
        return total >= cost.getCount();
    }

    private void deductFromInventory(PlayerInventory inv, ItemStack cost) {
        if (cost.isEmpty()) return;
        int remaining = cost.getCount();
        for (int i = 0; i < PlayerInventory.TOTAL_SLOTS && remaining > 0; i++) {
            ItemStack stack = inv.getSlot(i);
            if (!stack.isEmpty() && stack.getItem() == cost.getItem()) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (stack.isEmpty()) {
                    inv.setSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    public void handleKeyPress(int key, Player player) {
        if (!open) return;

        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_E) {
            close();
        } else if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            if (activeVillager != null && selectedTradeIndex > 0) {
                selectedTradeIndex--;
            }
        } else if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            if (activeVillager != null && selectedTradeIndex < activeVillager.getTradeOffers().size() - 1) {
                selectedTradeIndex++;
            }
        } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
            performTrade(player);
        }
    }

    /**
     * Renders the 2D barter trading window.
     */
    public void render(Shader uiShader, BitmapFont font, int windowWidth, int windowHeight) {
        if (!open || activeVillager == null) return;

        orthoMatrix.identity().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        uiShader.bind();
        uiShader.setMat4("u_Projection", orthoMatrix);

        float guiW = 680;
        float guiH = 420;
        float guiX = (windowWidth - guiW) / 2.0f;
        float guiY = (windowHeight - guiH) / 2.0f;

        // 1. Draw solid background rectangles
        uiShader.setBoolean("u_UseTexture", false);

        // Dim backdrop
        font.drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.65f);

        // Parchment outer frame & inner container
        font.drawRect(guiX - 4, guiY - 4, guiX + guiW + 4, guiY + guiH + 4, 0.45f, 0.22f, 0.12f, 1.0f);
        font.drawRect(guiX, guiY, guiX + guiW, guiY + guiH, 0.92f, 0.86f, 0.72f, 1.0f);

        // Header banner
        font.drawRect(guiX, guiY, guiX + guiW, guiY + 36, 0.32f, 0.15f, 0.08f, 1.0f);

        // Trade list panel on left (width 320)
        font.drawRect(guiX + 15, guiY + 45, guiX + 335, guiY + guiH - 15, 0.85f, 0.78f, 0.64f, 1.0f);

        // Highlight selected trade
        List<TradeOffer> offers = activeVillager.getTradeOffers();
        float ty = guiY + 55;
        for (int i = 0; i < offers.size(); i++) {
            if (i == selectedTradeIndex) {
                font.drawRect(guiX + 20, ty - 2, guiX + 330, ty + 24, 0.25f, 0.45f, 0.28f, 0.85f);
            }
            ty += 32;
        }

        // Trade execution box on right (width 310)
        font.drawRect(guiX + 350, guiY + 45, guiX + guiW - 15, guiY + guiH - 15, 0.85f, 0.78f, 0.64f, 1.0f);
        // Execute trade button rect
        font.drawRect(guiX + 380, guiY + guiH - 65, guiX + guiW - 45, guiY + guiH - 30, 0.32f, 0.55f, 0.22f, 1.0f);

        font.flush();

        // 2. Draw text
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        // Title
        font.drawString("=== BARTER TRADING: " + activeVillager.getProfession().getDisplayName() + " ===",
                guiX + 20, guiY + 12, 1.4f, 1.0f, 0.88f, 0.1f, 1.0f, true);
        font.drawString("[ESC: Close  |  W/S: Select  |  ENTER: Trade]",
                guiX + guiW - 350, guiY + 14, 1.0f, 0.9f, 0.9f, 0.9f, 1.0f, false);

        // Render offers
        ty = guiY + 55;
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            boolean sel = (i == selectedTradeIndex);
            float r = sel ? 1.0f : 0.2f;
            float g = sel ? 1.0f : 0.15f;
            float b = sel ? 0.8f : 0.1f;

            String costText = offer.getCostA().getCount() + "x " + offer.getCostA().getItem().getDisplayName();
            String resultText = offer.getResult().getCount() + "x " + offer.getResult().getItem().getDisplayName();
            font.drawString((i + 1) + ". " + costText, guiX + 25, ty, 1.0f, r, g, b, 1.0f, sel);
            font.drawString("   -> " + resultText, guiX + 25, ty + 12, 0.95f, r, g, b, 1.0f, sel);
            ty += 32;
        }

        // Details of selected trade
        TradeOffer active = getSelectedTrade();
        if (active != null) {
            float rx = guiX + 370;
            font.drawString("SELECTED TRANSACTION", rx, guiY + 65, 1.3f, 0.35f, 0.15f, 0.05f, 1.0f, true);
            font.drawString("Requires:", rx, guiY + 95, 1.1f, 0.5f, 0.2f, 0.1f, 1.0f, false);
            font.drawString(" * " + active.getCostA().getCount() + "x " + active.getCostA().getItem().getDisplayName(),
                    rx + 10, guiY + 115, 1.05f, 0.2f, 0.15f, 0.1f, 1.0f, false);
            if (!active.getCostB().isEmpty()) {
                font.drawString(" * " + active.getCostB().getCount() + "x " + active.getCostB().getItem().getDisplayName(),
                        rx + 10, guiY + 135, 1.05f, 0.2f, 0.15f, 0.1f, 1.0f, false);
            }

            font.drawString("Yields:", rx, guiY + 165, 1.1f, 0.2f, 0.5f, 0.1f, 1.0f, false);
            font.drawString(" * " + active.getResult().getCount() + "x " + active.getResult().getItem().getDisplayName(),
                    rx + 10, guiY + 185, 1.05f, 0.1f, 0.35f, 0.1f, 1.0f, false);

            // Execute button label
            font.drawString("[ PRESS ENTER TO TRADE ]", rx + 30, guiY + guiH - 52, 1.15f, 1.0f, 1.0f, 1.0f, 1.0f, true);
        }

        font.flush();
        font.unbindTexture();
        uiShader.unbind();

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
}
