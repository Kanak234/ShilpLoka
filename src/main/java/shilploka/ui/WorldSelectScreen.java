package shilploka.ui;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.render.Shader;
import shilploka.world.save.SaveManager;
import shilploka.world.save.WorldMetadata;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Provides an in-engine graphical user interface for selecting, loading, backing up,
 * and creating singleplayer worlds drawn from Bharat's history and geography.
 *
 * WHAT:
 * Supports two distinct interface modes:
 * 1. World Selection List: displays saved worlds, seeds, game modes, and last-played timestamps.
 * 2. World Creation Screen: interactive text inputs for World Name, procedural Seed, and Game Mode.
 * Renders via BitmapFont and ui.frag shader with responsive keyboard navigation.
 *
 * USED FOR:
 * Loaded when launching the game or accessing the world selection menu.
 *
 * WHERE NEXT:
 * Upon world selection or creation, invokes WorldActionListener to transition Game to the 3D world.
 */
public class WorldSelectScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldSelectScreen.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public enum Mode {
        WORLD_LIST,
        CREATE_WORLD
    }

    public interface WorldActionListener {
        void onPlayWorld(SaveManager saveManager, WorldMetadata metadata);
    }

    private Mode mode = Mode.WORLD_LIST;
    private final File savesDir;
    private final List<WorldMetadata> worlds = new ArrayList<>();
    private int selectedIndex = 0;

    // Create World form state
    private String inputWorldName = "Mera Bharat";
    private String inputSeed = "19470815";
    private String inputGameMode = "creative"; // "creative" or "survival"
    private int activeCreateField = 0; // 0 = World Name, 1 = Seed, 2 = Mode

    private String statusMessage = "";
    private WorldActionListener listener;
    private final Matrix4f orthoMatrix = new Matrix4f();

    public WorldSelectScreen(File savesDir, WorldActionListener listener) {
        this.savesDir = savesDir;
        this.listener = listener;
        refreshWorldList();
    }

    public void setListener(WorldActionListener listener) {
        this.listener = listener;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
        this.statusMessage = "";
    }

    /**
     * Rescans the saves directory and re-indexes all valid world level.json files.
     */
    public void refreshWorldList() {
        worlds.clear();
        worlds.addAll(SaveManager.listSavedWorlds(savesDir));
        if (selectedIndex >= worlds.size()) {
            selectedIndex = Math.max(0, worlds.size() - 1);
        }
    }

    /**
     * WHY:
     * Handles keyboard events for world navigation, screen toggling, and action triggering.
     */
    public boolean handleKey(int key, int action) {
        if (action != GLFW_PRESS && action != GLFW_REPEAT) {
            return false;
        }

        if (mode == Mode.WORLD_LIST) {
            return handleKeyWorldList(key);
        } else {
            return handleKeyCreateWorld(key);
        }
    }

    private boolean handleKeyWorldList(int key) {
        switch (key) {
            case GLFW_KEY_UP -> {
                if (!worlds.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + worlds.size()) % worlds.size();
                }
                return true;
            }
            case GLFW_KEY_DOWN -> {
                if (!worlds.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % worlds.size();
                }
                return true;
            }
            case GLFW_KEY_ENTER -> {
                playSelectedWorld();
                return true;
            }
            case GLFW_KEY_C -> {
                mode = Mode.CREATE_WORLD;
                inputWorldName = "Mera Bharat " + (worlds.size() + 1);
                inputSeed = String.valueOf(System.currentTimeMillis() % 100000000L);
                statusMessage = "Enter world parameters and press [ENTER] to create.";
                return true;
            }
            case GLFW_KEY_B -> {
                backupSelectedWorld();
                return true;
            }
            case GLFW_KEY_R -> {
                refreshWorldList();
                statusMessage = "World list refreshed (" + worlds.size() + " found).";
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleKeyCreateWorld(int key) {
        switch (key) {
            case GLFW_KEY_ESCAPE -> {
                mode = Mode.WORLD_LIST;
                statusMessage = "";
                return true;
            }
            case GLFW_KEY_TAB, GLFW_KEY_DOWN -> {
                activeCreateField = (activeCreateField + 1) % 3;
                return true;
            }
            case GLFW_KEY_UP -> {
                activeCreateField = (activeCreateField - 1 + 3) % 3;
                return true;
            }
            case GLFW_KEY_SPACE -> {
                if (activeCreateField == 2) {
                    inputGameMode = inputGameMode.equals("creative") ? "survival" : "creative";
                    return true;
                }
                return false;
            }
            case GLFW_KEY_BACKSPACE -> {
                if (activeCreateField == 0 && !inputWorldName.isEmpty()) {
                    inputWorldName = inputWorldName.substring(0, inputWorldName.length() - 1);
                } else if (activeCreateField == 1 && !inputSeed.isEmpty()) {
                    inputSeed = inputSeed.substring(0, inputSeed.length() - 1);
                }
                return true;
            }
            case GLFW_KEY_ENTER -> {
                createNewWorldAndPlay();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /**
     * WHY:
     * Receives Unicode text characters typed by the player into active input fields.
     */
    public boolean handleChar(int codepoint) {
        if (mode != Mode.CREATE_WORLD) return false;
        if (codepoint < 32 || codepoint > 126) return false;

        char c = (char) codepoint;
        if (activeCreateField == 0) {
            if (inputWorldName.length() < 28) {
                inputWorldName += c;
            }
            return true;
        } else if (activeCreateField == 1) {
            if (inputSeed.length() < 19 && (Character.isDigit(c) || c == '-')) {
                inputSeed += c;
            }
            return true;
        }
        return false;
    }

    private void playSelectedWorld() {
        if (worlds.isEmpty() || selectedIndex < 0 || selectedIndex >= worlds.size()) {
            statusMessage = "No world selected. Press [C] to create a new world.";
            return;
        }

        WorldMetadata meta = worlds.get(selectedIndex);
        String folderName = meta.getWorldName().trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        SaveManager sm = new SaveManager(savesDir, folderName);

        if (listener != null) {
            listener.onPlayWorld(sm, meta);
        }
    }

    private void backupSelectedWorld() {
        if (worlds.isEmpty() || selectedIndex < 0 || selectedIndex >= worlds.size()) return;

        WorldMetadata meta = worlds.get(selectedIndex);
        String folderName = meta.getWorldName().trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        SaveManager sm = new SaveManager(savesDir, folderName);
        File backup = sm.createBackup();
        if (backup != null) {
            statusMessage = "Backup archived: " + backup.getName();
        } else {
            statusMessage = "Backup creation failed.";
        }
    }

    private void createNewWorldAndPlay() {
        if (inputWorldName.trim().isEmpty()) {
            statusMessage = "World name cannot be blank!";
            return;
        }

        long seed;
        try {
            seed = Long.parseLong(inputSeed.trim());
        } catch (NumberFormatException e) {
            seed = inputSeed.hashCode();
        }

        try {
            SaveManager sm = SaveManager.createNewWorld(savesDir, inputWorldName.trim(), seed, inputGameMode);
            WorldMetadata meta = sm.loadWorldMetadata();
            refreshWorldList();
            if (listener != null) {
                listener.onPlayWorld(sm, meta);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create world: {}", e.getMessage(), e);
            statusMessage = "Error creating world: " + e.getMessage();
        }
    }

    /**
     * WHY:
     * Renders the complete 2D World Selection or Creation interface.
     */
    public void render(BitmapFont font, Shader uiShader, int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        orthoMatrix.identity().ortho(0.0f, (float) screenWidth, (float) screenHeight, 0.0f, -1.0f, 1.0f);
        uiShader.bind();
        uiShader.setMat4("u_OrthoProjection", orthoMatrix);
        uiShader.setInt("u_FontTexture", 0);

        // 1. Dark atmospheric background backdrop
        uiShader.setBoolean("u_UseTexture", false);
        font.drawRect(0, 0, screenWidth, screenHeight, 0.08f, 0.09f, 0.12f, 0.92f);

        // Header and Footer decoration lines
        font.drawRect(0, 56, screenWidth, 58, 0.85f, 0.65f, 0.20f, 1.0f); // Indian Gold accent line
        font.drawRect(0, screenHeight - 48, screenWidth, screenHeight - 46, 0.85f, 0.65f, 0.20f, 0.6f);
        font.flush();

        // 2. Render Header Titles
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        String mainTitle = "SHILPLOKA (शिल्पलोक) — WORLD SELECT (विश्व चयन)";
        font.drawString(mainTitle, (screenWidth - (mainTitle.length() * 16)) / 2.0f, 16, 2.0f,
                0.95f, 0.85f, 0.40f, 1.0f, true);

        String subTitle = (mode == Mode.WORLD_LIST)
                ? "Choose an existing world or press [C] to create a new civilization."
                : "Configure your new world seed and environment parameters.";
        font.drawString(subTitle, (screenWidth - (subTitle.length() * 8)) / 2.0f, 40, 1.0f,
                0.75f, 0.80f, 0.85f, 0.9f, true);
        font.flush();
        font.unbindTexture();

        // 3. Render Mode Specific Content
        if (mode == Mode.WORLD_LIST) {
            renderWorldList(font, uiShader, screenWidth, screenHeight);
        } else {
            renderCreateWorld(font, uiShader, screenWidth, screenHeight);
        }

        // 4. Render Footer and Status Message
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        if (!statusMessage.isEmpty()) {
            font.drawString(statusMessage, (screenWidth - (statusMessage.length() * 8)) / 2.0f,
                    screenHeight - 62, 1.0f, 0.40f, 0.90f, 0.40f, 1.0f, true);
        }

        String footerHelp = (mode == Mode.WORLD_LIST)
                ? "[ENTER] Play World  |  [C] Create New World  |  [B] Archive Backup  |  [R] Refresh"
                : "[ENTER] Create & Play  |  [TAB] Switch Field  |  [SPACE] Toggle Mode  |  [ESC] Cancel";
        font.drawString(footerHelp, (screenWidth - (footerHelp.length() * 8)) / 2.0f,
                screenHeight - 32, 1.0f, 0.70f, 0.75f, 0.80f, 0.85f, true);

        font.flush();
        font.unbindTexture();
        uiShader.unbind();

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderWorldList(BitmapFont font, Shader uiShader, int screenWidth, int screenHeight) {
        float listStartY = 75.0f;
        float cardWidth = Math.min(680.0f, screenWidth - 80.0f);
        float cardHeight = 44.0f;
        float cardGap = 8.0f;
        float startX = (screenWidth - cardWidth) / 2.0f;

        if (worlds.isEmpty()) {
            uiShader.setBoolean("u_UseTexture", true);
            font.bindTexture();
            String emptyMsg = "No saved worlds discovered in saves/ directory.";
            String hintMsg = "Press [C] to create your first world!";
            font.drawString(emptyMsg, (screenWidth - (emptyMsg.length() * 8)) / 2.0f, listStartY + 40, 1.0f,
                    0.8f, 0.8f, 0.8f, 0.9f, true);
            font.drawString(hintMsg, (screenWidth - (hintMsg.length() * 8)) / 2.0f, listStartY + 60, 1.0f,
                    0.95f, 0.85f, 0.3f, 1.0f, true);
            font.flush();
            font.unbindTexture();
            return;
        }

        int maxVisible = (int) ((screenHeight - 160.0f) / (cardHeight + cardGap));
        int renderCount = Math.min(worlds.size(), maxVisible);

        for (int i = 0; i < renderCount; i++) {
            float y = listStartY + (i * (cardHeight + cardGap));
            boolean isSelected = (i == selectedIndex);

            // Background card quad
            uiShader.setBoolean("u_UseTexture", false);
            if (isSelected) {
                // Golden highlight border
                font.drawRect(startX - 2, y - 2, startX + cardWidth + 2, y + cardHeight + 2,
                        0.95f, 0.75f, 0.20f, 0.95f);
                font.drawRect(startX, y, startX + cardWidth, y + cardHeight,
                        0.18f, 0.22f, 0.30f, 0.95f);
            } else {
                font.drawRect(startX, y, startX + cardWidth, y + cardHeight,
                        0.12f, 0.14f, 0.18f, 0.85f);
            }
            font.flush();

            // Card text contents
            uiShader.setBoolean("u_UseTexture", true);
            font.bindTexture();

            WorldMetadata meta = worlds.get(i);
            String title = (i + 1) + ". " + meta.getWorldName();
            String lastPlayed = "Last: " + DATE_FORMAT.format(new Date(meta.getLastPlayedTimestamp() * 1000L));
            String details = String.format("Mode: %s | Seed: %d | Days: %d",
                    meta.getGameMode().toUpperCase(), meta.getSeed(), meta.getDayCount());

            float titleR = isSelected ? 1.0f : 0.9f;
            float titleG = isSelected ? 0.9f : 0.9f;
            float titleB = isSelected ? 0.4f : 0.9f;

            font.drawString(title, startX + 12, y + 8, 1.0f, titleR, titleG, titleB, 1.0f, true);
            font.drawString(lastPlayed, startX + cardWidth - (lastPlayed.length() * 8) - 12, y + 8, 1.0f,
                    0.6f, 0.65f, 0.7f, 0.9f, true);
            font.drawString(details, startX + 12, y + 24, 1.0f, 0.7f, 0.75f, 0.8f, 0.85f, true);

            font.flush();
            font.unbindTexture();
        }
    }

    private void renderCreateWorld(BitmapFont font, Shader uiShader, int screenWidth, int screenHeight) {
        float formWidth = Math.min(540.0f, screenWidth - 80.0f);
        float formX = (screenWidth - formWidth) / 2.0f;
        float startY = 100.0f;

        // Field 0: World Name
        renderInputField(font, uiShader, "World Name (विश्व का नाम):", inputWorldName,
                formX, startY, formWidth, activeCreateField == 0);

        // Field 1: Seed
        renderInputField(font, uiShader, "World Seed (प्रारंभिक बीज):", inputSeed,
                formX, startY + 70, formWidth, activeCreateField == 1);

        // Field 2: Game Mode
        renderInputField(font, uiShader, "Game Mode (खेल विधि - [SPACE] to toggle):",
                inputGameMode.toUpperCase() + (inputGameMode.equals("creative") ? " (Infinite Blocks & Flight)" : " (Survival & Vitals)"),
                formX, startY + 140, formWidth, activeCreateField == 2);
    }

    private void renderInputField(BitmapFont font, Shader uiShader, String label, String value,
                                  float x, float y, float width, boolean isActive) {
        // Label
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString(label, x, y, 1.0f, 0.85f, 0.85f, 0.90f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        // Input Box
        uiShader.setBoolean("u_UseTexture", false);
        float boxY = y + 16.0f;
        float boxHeight = 28.0f;

        if (isActive) {
            font.drawRect(x - 2, boxY - 2, x + width + 2, boxY + boxHeight + 2,
                    0.95f, 0.75f, 0.20f, 0.95f); // Gold active border
            font.drawRect(x, boxY, x + width, boxY + boxHeight,
                    0.15f, 0.18f, 0.25f, 0.95f);
        } else {
            font.drawRect(x, boxY, x + width, boxY + boxHeight,
                    0.10f, 0.12f, 0.16f, 0.85f);
        }
        font.flush();

        // Value text
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        String displayVal = value + (isActive ? "_" : "");
        font.drawString(displayVal, x + 8, boxY + 8, 1.0f,
                isActive ? 1.0f : 0.8f, isActive ? 1.0f : 0.8f, isActive ? 0.6f : 0.8f, 1.0f, true);
        font.flush();
        font.unbindTexture();
    }
}
