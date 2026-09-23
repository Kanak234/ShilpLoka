package shilploka.ui;

import org.joml.Matrix4f;
import shilploka.entity.Player;
import shilploka.item.ItemStack;
import shilploka.kaushal.KaushalManager;
import shilploka.kaushal.KaushalUpgradeType;
import shilploka.render.Shader;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Provides the interactive 2D GUI for the Shilp Mandap upgrade altar.
 * Allows artisans to channel celestial and mineral energies into their tools,
 * unlocking heightened mining speed, durability preservation, and combat damage.
 *
 * WHAT:
 * Renders upgrade offerings and handles keyboard/mouse enhancement applications.
 *
 * USED FOR:
 * Opened when the player right-clicks an in-world SHILP_MANDAP block.
 */
public class KaushalScreen {

    private boolean open = false;
    private final KaushalManager kaushalManager = new KaushalManager();
    private int selectedUpgradeIndex = 0;
    private final Matrix4f orthoMatrix = new Matrix4f();

    public boolean isOpen() {
        return open;
    }

    public void open() {
        this.open = true;
        this.selectedUpgradeIndex = 0;
    }

    public void open(ItemStack tool, ItemStack catalyst) {
        this.kaushalManager.setToolSlot(tool);
        this.kaushalManager.setCatalystSlot(catalyst);
        this.selectedUpgradeIndex = 0;
        this.open = true;
    }

    public void close() {
        this.open = false;
    }

    public KaushalManager getKaushalManager() {
        return kaushalManager;
    }

    public int getSelectedUpgradeIndex() {
        return selectedUpgradeIndex;
    }

    public void selectUpgrade(int index) {
        List<KaushalUpgradeType> list = kaushalManager.getAvailableUpgrades();
        if (!list.isEmpty()) {
            this.selectedUpgradeIndex = Math.max(0, Math.min(index, list.size() - 1));
        }
    }

    public KaushalUpgradeType getSelectedUpgrade() {
        List<KaushalUpgradeType> list = kaushalManager.getAvailableUpgrades();
        if (list.isEmpty()) return null;
        int clamped = Math.max(0, Math.min(selectedUpgradeIndex, list.size() - 1));
        return list.get(clamped);
    }

    public boolean performUpgrade() {
        KaushalUpgradeType selected = getSelectedUpgrade();
        if (selected == null) return false;
        return kaushalManager.applyUpgrade(selected);
    }

    public void handleKeyPress(int key, Player player) {
        if (!open) return;

        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_E) {
            close();
        } else if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            if (selectedUpgradeIndex > 0) selectedUpgradeIndex--;
        } else if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            if (selectedUpgradeIndex < kaushalManager.getAvailableUpgrades().size() - 1) {
                selectedUpgradeIndex++;
            }
        } else if (key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
            performUpgrade();
        }
    }

    /**
     * Renders the Shilp Mandap altar upgrade interface.
     */
    public void render(Shader uiShader, BitmapFont font, int windowWidth, int windowHeight) {
        if (!open) return;

        orthoMatrix.identity().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        uiShader.bind();
        uiShader.setMat4("u_Projection", orthoMatrix);

        float guiW = 640;
        float guiH = 400;
        float guiX = (windowWidth - guiW) / 2.0f;
        float guiY = (windowHeight - guiH) / 2.0f;

        // 1. Draw solid background rectangles
        uiShader.setBoolean("u_UseTexture", false);

        // Dim backdrop
        font.drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.65f);

        // Basalt and Gold outer trim
        font.drawRect(guiX - 4, guiY - 4, guiX + guiW + 4, guiY + guiH + 4, 0.85f, 0.65f, 0.12f, 1.0f);
        font.drawRect(guiX, guiY, guiX + guiW, guiY + guiH, 0.18f, 0.20f, 0.25f, 1.0f);

        // Header bar
        font.drawRect(guiX, guiY, guiX + guiW, guiY + 36, 0.10f, 0.12f, 0.18f, 1.0f);

        // Left pane (upgrades list)
        font.drawRect(guiX + 15, guiY + 45, guiX + 320, guiY + guiH - 15, 0.14f, 0.16f, 0.22f, 1.0f);

        // Highlight selected upgrade
        List<KaushalUpgradeType> upgrades = kaushalManager.getAvailableUpgrades();
        float uy = guiY + 55;
        for (int i = 0; i < upgrades.size(); i++) {
            if (i == selectedUpgradeIndex) {
                font.drawRect(guiX + 20, uy - 2, guiX + 315, uy + 26, 0.20f, 0.45f, 0.85f, 0.85f);
            }
            uy += 32;
        }

        // Right pane (upgrade details & apply button)
        font.drawRect(guiX + 335, guiY + 45, guiX + guiW - 15, guiY + guiH - 15, 0.14f, 0.16f, 0.22f, 1.0f);
        // Apply button
        font.drawRect(guiX + 365, guiY + guiH - 60, guiX + guiW - 45, guiY + guiH - 25, 0.15f, 0.55f, 0.85f, 1.0f);

        font.flush();

        // 2. Draw text
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        // Title
        font.drawString("=== SHILP MANDAP (KAUSHAL UPGRADE ALTAR) ===", guiX + 20, guiY + 12, 1.35f, 0.9f, 0.8f, 0.2f, 1.0f, true);
        font.drawString("[ESC: Close  |  W/S: Select  |  ENTER: Bestow]", guiX + guiW - 370, guiY + 14, 1.0f, 0.8f, 0.8f, 0.8f, 1.0f, false);

        // Render available upgrades
        uy = guiY + 55;
        for (int i = 0; i < upgrades.size(); i++) {
            KaushalUpgradeType u = upgrades.get(i);
            boolean sel = (i == selectedUpgradeIndex);
            float r = sel ? 1.0f : 0.7f;
            float g = sel ? 1.0f : 0.7f;
            float b = sel ? 0.4f : 0.7f;

            font.drawString((i + 1) + ". " + u.getDisplayName(), guiX + 25, uy, 1.0f, r, g, b, 1.0f, sel);
            font.drawString("   Cost: " + u.getLapisCost() + "x Catalyst Gem", guiX + 25, uy + 12, 0.95f, 0.4f, 0.7f, 1.0f, 1.0f, false);
            uy += 32;
        }

        // Selected details on right
        KaushalUpgradeType selected = getSelectedUpgrade();
        if (selected != null) {
            float rx = guiX + 355;
            font.drawString("SACRED BLESSING", rx, guiY + 65, 1.3f, 0.9f, 0.8f, 0.2f, 1.0f, true);
            font.drawString(selected.getDisplayName(), rx, guiY + 95, 1.15f, 0.4f, 0.8f, 1.0f, 1.0f, true);
            font.drawString("Cost: " + selected.getLapisCost() + "x Catalyst", rx, guiY + 115, 1.0f, 0.9f, 0.9f, 0.5f, 1.0f, false);

            font.drawString(selected.getDescription(), rx, guiY + 145, 0.95f, 0.8f, 0.8f, 0.8f, 1.0f, false);

            font.drawString("[ PRESS ENTER TO BESTOW ]", rx + 25, guiY + guiH - 48, 1.15f, 1.0f, 1.0f, 1.0f, 1.0f, true);
        }

        font.flush();
        font.unbindTexture();
        uiShader.unbind();

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
}
