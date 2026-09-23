package shilploka.ui;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.audio.SoundCategory;
import shilploka.audio.SoundEngine;
import shilploka.event.FestivalManager;
import shilploka.event.FestivalType;
import shilploka.render.Shader;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Full graphical settings and options screen providing runtime control over
 * video configuration, audio volume sliders, bilingual language toggling,
 * accessibility features, and Indian cultural festivals.
 *
 * WHAT:
 * - Interactive keyboard/mouse navigation.
 * - Live synchronization with SoundEngine, Localization, and FestivalManager.
 */
public class SettingsScreen {

    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsScreen.class);

    public interface SettingsListener {
        void onRenderDistanceChanged(int newDistance);
        void onFovChanged(float newFov);
    }

    private boolean open = false;
    private int selectedRow = 0;
    private static final int TOTAL_ROWS = 10;

    // Setting values
    private int renderDistance = 6; // Chunks
    private float fov = 75.0f; // Degrees
    private float masterVolume = 1.0f;
    private float musicVolume = 0.8f;
    private float soundVolume = 1.0f;
    private float weatherVolume = 0.8f;
    private int guiScale = 2;
    private boolean subtitles = true;
    private boolean reducedMotion = false;

    private final SoundEngine soundEngine;
    private final FestivalManager festivalManager;
    private SettingsListener listener;
    private final Matrix4f orthoMatrix = new Matrix4f();

    public SettingsScreen(SoundEngine soundEngine, FestivalManager festivalManager) {
        this.soundEngine = soundEngine;
        this.festivalManager = festivalManager;
    }

    public void setListener(SettingsListener listener) {
        this.listener = listener;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public boolean handleKey(int key, int action) {
        if (!open || (action != GLFW_PRESS && action != GLFW_REPEAT)) return false;

        if (key == GLFW_KEY_ESCAPE) {
            this.open = false;
            return true;
        }

        if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            selectedRow = (selectedRow - 1 + TOTAL_ROWS) % TOTAL_ROWS;
            return true;
        }
        if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            selectedRow = (selectedRow + 1) % TOTAL_ROWS;
            return true;
        }

        if (key == GLFW_KEY_LEFT || key == GLFW_KEY_A) {
            adjustSelected(-1);
            return true;
        }
        if (key == GLFW_KEY_RIGHT || key == GLFW_KEY_D || key == GLFW_KEY_ENTER || key == GLFW_KEY_SPACE) {
            adjustSelected(1);
            return true;
        }

        return true;
    }

    private void adjustSelected(int dir) {
        switch (selectedRow) {
            case 0 -> { // Render Distance
                renderDistance = Math.max(2, Math.min(16, renderDistance + dir * 2));
                if (listener != null) listener.onRenderDistanceChanged(renderDistance);
            }
            case 1 -> { // FOV
                fov = Math.max(60.0f, Math.min(110.0f, fov + dir * 5.0f));
                if (listener != null) listener.onFovChanged(fov);
            }
            case 2 -> { // Master Volume
                masterVolume = Math.max(0.0f, Math.min(1.0f, masterVolume + dir * 0.1f));
                if (soundEngine != null) soundEngine.setCategoryVolume(SoundCategory.MASTER, masterVolume);
            }
            case 3 -> { // Music Volume
                musicVolume = Math.max(0.0f, Math.min(1.0f, musicVolume + dir * 0.1f));
                if (soundEngine != null) soundEngine.setCategoryVolume(SoundCategory.MUSIC, musicVolume);
            }
            case 4 -> { // Sound Volume
                soundVolume = Math.max(0.0f, Math.min(1.0f, soundVolume + dir * 0.1f));
                if (soundEngine != null) soundEngine.setCategoryVolume(SoundCategory.BLOCKS, soundVolume);
            }
            case 5 -> { // Language toggle
                if (Localization.isHindi()) {
                    Localization.setLanguage(Localization.DEFAULT_LANG);
                } else {
                    Localization.setLanguage(Localization.HINDI_LANG);
                }
            }
            case 6 -> { // GUI Scale
                guiScale = Math.max(1, Math.min(4, guiScale + dir));
            }
            case 7 -> { // Directional Subtitles
                subtitles = !subtitles;
            }
            case 8 -> { // Reduced Motion
                reducedMotion = !reducedMotion;
            }
            case 9 -> { // Cultural Festivals
                if (festivalManager != null) {
                    FestivalType[] fVals = FestivalType.values();
                    int curIdx = festivalManager.getActiveFestival().ordinal();
                    int nextIdx = (curIdx + dir + fVals.length) % fVals.length;
                    festivalManager.setActiveFestival(fVals[nextIdx]);
                }
            }
        }
    }

    /**
     * Renders settings options and current values.
     */
    public void render(BitmapFont font, Shader uiShader, int screenWidth, int screenHeight) {
        if (!open || uiShader == null || font == null) return;

        orthoMatrix.identity().ortho(0.0f, screenWidth, screenHeight, 0.0f, -1.0f, 1.0f);
        uiShader.bind();
        uiShader.setMat4("u_Projection", orthoMatrix);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        // Header
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();
        font.drawString(Localization.get("settings.title"), 40, 40, 2.0f, 1.0f, 0.85f, 0.2f, 1.0f, true);

        // Options List
        String[] labels = new String[]{
                Localization.get("settings.render_distance") + ": " + renderDistance + " Chunks",
                Localization.get("settings.fov") + ": " + (int) fov + " deg",
                Localization.get("settings.master_volume") + ": " + (int) (masterVolume * 100) + "%",
                Localization.get("settings.music_volume") + ": " + (int) (musicVolume * 100) + "%",
                Localization.get("settings.sound_volume") + ": " + (int) (soundVolume * 100) + "%",
                Localization.get("settings.language") + ": " + (Localization.isHindi() ? "Hindi (Devanagari)" : "English"),
                Localization.get("settings.gui_scale") + ": " + guiScale + "x",
                Localization.get("settings.subtitles") + ": " + (subtitles ? "ON" : "OFF"),
                Localization.get("settings.reduced_motion") + ": " + (reducedMotion ? "ON" : "OFF"),
                Localization.get("settings.festivals") + ": " + (festivalManager != null ? festivalManager.getActiveFestival().getEnglishName() : "None")
        };

        int y = 90;
        for (int i = 0; i < labels.length; i++) {
            boolean selected = (i == selectedRow);
            float r = selected ? 1.0f : 0.8f;
            float g = selected ? 0.9f : 0.8f;
            float b = selected ? 0.3f : 0.8f;
            String prefix = selected ? "> " : "  ";
            font.drawString(prefix + labels[i], 50, y, 1.5f, r, g, b, 1.0f, true);
            y += 28;
        }

        font.drawString("[W/S/Up/Down: Select]  [A/D/Left/Right/Enter: Adjust]  [Esc: Close]", 40, screenHeight - 40, 1.0f, 0.6f, 0.6f, 0.6f, 1.0f, true);
        font.flush();
        font.unbindTexture();

        glEnable(GL_DEPTH_TEST);
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public float getFov() {
        return fov;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public int getGuiScale() {
        return guiScale;
    }

    public boolean isSubtitles() {
        return subtitles;
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public int getSelectedRow() {
        return selectedRow;
    }
}
