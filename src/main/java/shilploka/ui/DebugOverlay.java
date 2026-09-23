package shilploka.ui;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import shilploka.entity.PlayerVitals;
import shilploka.render.Camera;
import shilploka.render.ChunkMesh;
import shilploka.render.Shader;

import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * The F3 debug overlay provides real-time telemetry (FPS, frame time, player coordinates,
 * chunk sections, cardinal compass headings, GPU vendor, OpenGL version, and heap usage)
 * essential for performance profiling, spatial orientation, and verifying engine correctness.
 *
 * WHAT:
 * Renders an orthographic 2D diagnostic HUD displaying system and world metrics, accompanied
 * by a screen-centered crosshair reticle. Toggleable dynamically via the F3 key.
 *
 * USED FOR:
 * Invoked at the end of Game.render() after 3D world geometry passes.
 *
 * WHERE NEXT:
 * Shaded via ui.vert and ui.frag using an orthographic projection matching current window pixels.
 */
public class DebugOverlay {

    private final Shader uiShader;
    private final BitmapFont font;
    private final Matrix4f orthoMatrix = new Matrix4f();

    private boolean visible = true; // Enabled by default in Phase P1

    // Hardware strings cached on first query
    private String glRenderer = null;
    private String glVendor = null;
    private String glVersion = null;

    /**
     * WHY:
     * Initializes the UI shader program and bitmap font atlas.
     *
     * WHAT:
     * Loads /shaders/ui.vert and /shaders/ui.frag and creates the BitmapFont renderer.
     */
    public DebugOverlay() {
        this.uiShader = new Shader("/shaders/ui.vert", "/shaders/ui.frag");
        this.font = new BitmapFont();
    }

    /**
     * WHY:
     * Toggles visibility state when user presses F3.
     */
    public void toggle() {
        this.visible = !this.visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * WHY:
     * Renders the complete F3 overlay and center crosshair reticle onto the screen.
     *
     * WHAT:
     * Disables depth testing, activates 2D alpha blending, binds the UI shader with
     * orthographic projection, queues diagnostic text and boxes, and flushes to the GPU.
     *
     * USED FOR:
     * Called in Game.render() after all 3D chunk rendering completes.
     *
     * WHERE NEXT:
     * Clears shader binding, restores depth testing, and returns control to swapBuffers.
     */
    public void render(Camera camera, shilploka.world.chunk.ChunkManager chunkManager,
                       shilploka.world.WorldGenerator worldGen,
                       Hotbar hotbar,
                       shilploka.entity.Player player,
                       shilploka.physics.RaycastResult targetHit,
                       int screenWidth, int screenHeight, int fps, double frameTimeMs) {
        render(camera, chunkManager, worldGen, hotbar, player, targetHit, null, screenWidth, screenHeight, fps, frameTimeMs);
    }

    public void render(Camera camera, shilploka.world.chunk.ChunkManager chunkManager,
                       shilploka.world.WorldGenerator worldGen,
                       Hotbar hotbar,
                       shilploka.entity.Player player,
                       shilploka.physics.RaycastResult targetHit,
                       shilploka.world.DayNightCycle dayNightCycle,
                       int screenWidth, int screenHeight, int fps, double frameTimeMs) {
        // Cache GPU info once context is active
        if (glRenderer == null) {
            glRenderer = glGetString(GL_RENDERER);
            glVendor = glGetString(GL_VENDOR);
            glVersion = glGetString(GL_VERSION);
            if (glRenderer == null) glRenderer = "Unknown GPU";
            if (glVendor == null) glVendor = "Unknown Vendor";
            if (glVersion == null) glVersion = "3.3 Core";
        }

        // Configure 2D render states
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Compute orthographic projection: (0,0) top-left, (screenWidth, screenHeight) bottom-right
        orthoMatrix.identity().ortho(0.0f, (float) screenWidth, (float) screenHeight, 0.0f, -1.0f, 1.0f);

        uiShader.bind();
        uiShader.setMat4("u_OrthoProjection", orthoMatrix);
        uiShader.setInt("u_FontTexture", 0);

        // 1. Draw crosshair in center of screen
        renderCrosshair(screenWidth, screenHeight, targetHit != null && targetHit.hasHit());

        // 2. Draw 9-slot creative hotbar and survival vitals at bottom
        if (hotbar != null) {
            uiShader.setBoolean("u_UseTexture", false);
            hotbar.render(font, screenWidth, screenHeight);
            if (player != null && player.getVitals() != null) {
                renderVitals(player.getVitals(), screenWidth, screenHeight);
            }
            font.flush();
        }

        // 3. Draw F3 telemetry lines if toggled on
        if (visible) {
            renderDiagnosticText(camera, chunkManager, worldGen, player, targetHit, dayNightCycle, screenWidth, screenHeight, fps, frameTimeMs);
        }

        uiShader.unbind();

        // Restore OpenGL states for 3D passes
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * WHY:
     * Renders a precise crosshair '+' reticle at the center of the viewport.
     *
     * WHAT:
     * Draws horizontal and vertical 2-pixel lines with a subtle dark border.
     * Glows amber/gold when aiming at an interactable block.
     */
    private void renderCrosshair(int width, int height, boolean targetingBlock) {
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        float len = 7.0f;
        float thick = 1.0f;

        uiShader.setBoolean("u_UseTexture", false);

        // Black shadow border
        font.drawRect(cx - len - 1, cy - thick - 1, cx + len + 1, cy + thick + 1, 0.0f, 0.0f, 0.0f, 0.6f);
        font.drawRect(cx - thick - 1, cy - len - 1, cx + thick + 1, cy + len + 1, 0.0f, 0.0f, 0.0f, 0.6f);

        // Core reticle (amber highlight if targeting block, crisp white otherwise)
        float r = targetingBlock ? 1.0f : 1.0f;
        float g = targetingBlock ? 0.85f : 1.0f;
        float b = targetingBlock ? 0.2f : 1.0f;
        font.drawRect(cx - len, cy - thick, cx + len, cy + thick, r, g, b, 0.95f);
        font.drawRect(cx - thick, cy - len, cx + thick, cy + len, r, g, b, 0.95f);

        font.flush();
    }

    /**
     * WHY:
     * Compiles and renders real-time engine telemetry strings.
     *
     * WHAT:
     * Formats player position, chunk section, biome, targeted block, FPS, memory stats, and controls.
     */
    private void renderDiagnosticText(Camera camera, shilploka.world.chunk.ChunkManager chunkMgr,
                                     shilploka.world.WorldGenerator worldGen,
                                     shilploka.entity.Player player,
                                     shilploka.physics.RaycastResult targetHit,
                                     shilploka.world.DayNightCycle dayNightCycle,
                                     int width, int height, int fps, double frameTimeMs) {
        Vector3f pos = camera.getPosition();
        int blockX = (int) Math.floor(pos.x);
        int blockY = (int) Math.floor(pos.y);
        int blockZ = (int) Math.floor(pos.z);

        int chunkX = blockX >> 4;
        int chunkY = blockY >> 4;
        int chunkZ = blockZ >> 4;

        // Query JVM heap memory
        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;
        long memPercent = (maxMem > 0) ? (usedMem * 100 / maxMem) : 0;

        float scale = 1.0f;
        float lineHeight = 10.0f;
        float startY = 6.0f;

        // --- LEFT COLUMN ---
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        String biomeName = (worldGen != null) ? worldGen.getBiomeProvider().getBiomeAt(blockX, blockZ).getDisplayName() : "Unknown Biome";
        String playerMode = (player != null && player.isFlying()) ? "[CREATIVE FLYING] (F to land)" : "[SURVIVAL GRAVITY] (F to fly)";

        String line1 = "ShilpLoka (Java Edition) v0.1.0 [P5 Light, Celestial Cycle & Fluids]";
        String line2 = String.format("FPS: %d (%.2f ms) | %s", fps, frameTimeMs, playerMode);
        String line3 = String.format("XYZ: %.3f / %.3f / %.3f", pos.x, pos.y, pos.z);
        String line4 = String.format("Block: %d, %d, %d", blockX, blockY, blockZ);
        String line5 = String.format("Chunk-Section: [%d, %d, %d] in chunk (%d, %d)", chunkX, chunkY, chunkZ, chunkX, chunkZ);
        String line6 = String.format("Facing: %s (Yaw: %.1f, Pitch: %.1f)", camera.getCardinalDirection(), camera.getYaw(), camera.getPitch());

        int skyLight = (chunkMgr != null) ? chunkMgr.getSkyLight(blockX, blockY, blockZ) : 15;
        int blockLight = (chunkMgr != null) ? chunkMgr.getBlockLight(blockX, blockY, blockZ) : 0;
        String line7 = String.format("Light: Sky %d (Dhoop), Block %d (Prakash)", skyLight, blockLight);

        String line8 = (dayNightCycle != null) ?
                String.format("Time: %s | %s | Sun: %.2f", dayNightCycle.getFormattedTime(), dayNightCycle.getMoonPhaseName(), dayNightCycle.getSunlightFactor()) :
                "Time: 12:00 [Madhyahn]";

        String line9 = "Biome: " + biomeName;

        String line10;
        if (targetHit != null && targetHit.hasHit()) {
            line10 = String.format("Looking at: Block #%d at (%d, %d, %d) [Dist: %.2fm]",
                    targetHit.blockId(), targetHit.blockX(), targetHit.blockY(), targetHit.blockZ(), targetHit.distance());
        } else {
            line10 = "Looking at: None";
        }

        String line11 = String.format("GPU: %s (%s)", glRenderer, glVendor);

        drawDebugLine(line1, 6, startY + 0 * lineHeight, scale, 1.0f, 0.85f, 0.3f); // Warm gold title
        drawDebugLine(line2, 6, startY + 1 * lineHeight, scale, 0.3f, 1.0f, 0.3f); // Green FPS
        drawDebugLine(line3, 6, startY + 2 * lineHeight, scale, 1.0f, 1.0f, 1.0f);
        drawDebugLine(line4, 6, startY + 3 * lineHeight, scale, 0.9f, 0.9f, 0.9f);
        drawDebugLine(line5, 6, startY + 4 * lineHeight, scale, 0.8f, 0.8f, 0.8f);
        drawDebugLine(line6, 6, startY + 5 * lineHeight, scale, 0.4f, 0.8f, 1.0f); // Teal heading
        drawDebugLine(line7, 6, startY + 6 * lineHeight, scale, 1.0f, 0.9f, 0.4f); // Golden light
        drawDebugLine(line8, 6, startY + 7 * lineHeight, scale, 0.9f, 0.7f, 1.0f); // Violet celestial
        drawDebugLine(line9, 6, startY + 8 * lineHeight, scale, 0.9f, 0.8f, 0.4f); // Biome
        drawDebugLine(line10, 6, startY + 9 * lineHeight, scale, 1.0f, 0.6f, 0.4f); // Orange target
        drawDebugLine(line11, 6, startY + 10 * lineHeight, scale, 0.7f, 0.7f, 0.7f);

        // --- RIGHT COLUMN ---
        int loadedChunks = (chunkMgr != null) ? chunkMgr.getLoadedChunkCount() : 0;
        int loadedMeshes = (chunkMgr != null) ? chunkMgr.getTotalLoadedMeshes() : 0;
        int renderedSections = (chunkMgr != null) ? chunkMgr.getRenderedSections() : 0;
        int renderedQuads = (chunkMgr != null) ? chunkMgr.getRenderedQuads() : 0;
        int culledSections = (chunkMgr != null) ? chunkMgr.getCulledSections() : 0;

        String rLine1 = String.format("Java: %s 64-bit | Mem: %dMB / %dMB (%d%%)", System.getProperty("java.version"), usedMem, maxMem, memPercent);
        String rLine2 = String.format("Chunks: %d loaded | Sections: %d meshes", loadedChunks, loadedMeshes);
        String rLine3 = String.format("Rendered: %d sections (%d quads)", renderedSections, renderedQuads);
        String rLine4 = String.format("Frustum Culled: %d sections", culledSections);
        String rLine5 = "Controls: WASD=Move, Space=Jump, F=Fly, 1-9/Scroll=Hotbar";
        String rLine6 = "Mouse: Left-Click=Break Block, Right-Click=Place Block, F3=HUD";

        drawDebugLineRight(rLine1, width - 6, startY + 0 * lineHeight, scale, 0.8f, 0.8f, 0.8f);
        drawDebugLineRight(rLine2, width - 6, startY + 1 * lineHeight, scale, 1.0f, 0.9f, 0.5f);
        drawDebugLineRight(rLine3, width - 6, startY + 2 * lineHeight, scale, 0.3f, 1.0f, 0.3f);
        drawDebugLineRight(rLine4, width - 6, startY + 3 * lineHeight, scale, 1.0f, 0.5f, 0.5f);
        drawDebugLineRight(rLine5, width - 6, startY + 4 * lineHeight, scale, 1.0f, 0.9f, 0.5f);
        drawDebugLineRight(rLine6, width - 6, startY + 5 * lineHeight, scale, 0.6f, 0.8f, 1.0f);

        font.flush();
    }

    private void drawDebugLine(String text, float x, float y, float scale, float r, float g, float b) {
        font.drawString(text, x, y, scale, r, g, b, 1.0f, true);
    }

    private void drawDebugLineRight(String text, float rightX, float y, float scale, float r, float g, float b) {
        float textWidth = text.length() * BitmapFont.GLYPH_WIDTH * scale;
        font.drawString(text, rightX - textWidth, y, scale, r, g, b, 1.0f, true);
    }

    public BitmapFont getFont() { return font; }
    public Shader getUiShader() { return uiShader; }

    /**
     * WHY:
     * Renders survival vitals: 10 Kamal (Lotus) health hearts, 10 Roti hunger icons,
     * and Jal-Shwaas air bubbles above the hotbar tray.
     */
    private void renderVitals(PlayerVitals vitals, int screenWidth, int screenHeight) {
        if (vitals == null) return;

        int totalWidth = (Hotbar.SLOT_COUNT * Hotbar.SLOT_SIZE) + ((Hotbar.SLOT_COUNT - 1) * Hotbar.SLOT_GAP) + 8;
        float startX = (screenWidth - totalWidth) / 2.0f;
        float hotbarY = screenHeight - (Hotbar.SLOT_SIZE + 8) - 12.0f;
        float vitalsY = hotbarY - 28.0f;

        // 1. Health Bar: 10 Kamal Lotus hearts on left side
        float hp = vitals.getHealth();
        for (int i = 0; i < 10; i++) {
            float x = startX + (i * 12);
            float y = vitalsY;
            // Slot background
            font.drawRect(x, y, x + 10, y + 10, 0.25f, 0.05f, 0.08f, 0.85f);
            if (hp >= (i + 1) * 2.0f) {
                // Full heart (vibrant lotus pink-red #E03B5B)
                font.drawRect(x + 1, y + 1, x + 9, y + 9, 0.95f, 0.25f, 0.40f, 0.95f);
            } else if (hp >= i * 2.0f + 1.0f) {
                // Half heart
                font.drawRect(x + 1, y + 1, x + 5, y + 9, 0.95f, 0.25f, 0.40f, 0.95f);
            }
        }

        // 2. Bhookh Bar: 10 Roti icons on right side
        int hunger = vitals.getBhookh();
        float hungerStartX = startX + totalWidth - (10 * 12);
        for (int i = 0; i < 10; i++) {
            float x = hungerStartX + (i * 12);
            float y = vitalsY;
            // Slot background
            font.drawRect(x, y, x + 10, y + 10, 0.22f, 0.16f, 0.10f, 0.85f);
            if (hunger >= (i + 1) * 2) {
                // Full Roti (golden wheat brown)
                font.drawRect(x + 1, y + 1, x + 9, y + 9, 0.88f, 0.68f, 0.35f, 0.95f);
            } else if (hunger >= i * 2 + 1) {
                // Half Roti
                font.drawRect(x + 5, y + 1, x + 9, y + 9, 0.88f, 0.68f, 0.35f, 0.95f);
            }
        }

        // 3. Jal-Shwaas (Air bubbles) when submerged
        int air = vitals.getAirBubbles();
        if (air < PlayerVitals.MAX_AIR) {
            float airY = vitalsY - 14.0f;
            int bubbles = (int) Math.ceil((air / (float) PlayerVitals.MAX_AIR) * 10.0f);
            for (int i = 0; i < 10; i++) {
                float x = hungerStartX + (i * 12);
                font.drawRect(x, airY, x + 8, airY + 8, 0.1f, 0.2f, 0.3f, 0.6f);
                if (i < bubbles) {
                    font.drawRect(x + 1, airY + 1, x + 7, airY + 7, 0.25f, 0.75f, 0.95f, 0.9f);
                }
            }
        }
    }

    /**
     * WHY:
     * Cleans up GPU resources upon game termination.
     */
    public void cleanup() {
        uiShader.cleanup();
        font.cleanup();
    }
}
