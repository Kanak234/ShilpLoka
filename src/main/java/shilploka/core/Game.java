package shilploka.core;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.entity.AABB;
import shilploka.entity.Player;
import shilploka.physics.RaycastResult;
import shilploka.physics.Raycaster;
import shilploka.render.Camera;
import shilploka.render.FrustumCuller;
import shilploka.render.Shader;
import shilploka.render.TextureArray;
import shilploka.ui.DebugOverlay;
import shilploka.ui.Hotbar;
import shilploka.world.BlockIds;
import shilploka.world.DayNightCycle;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkManager;
import shilploka.world.save.PlayerData;
import shilploka.world.save.SaveManager;
import shilploka.world.save.WorldMetadata;
import shilploka.crafting.RecipeManager;
import shilploka.entity.ItemEntity;
import shilploka.entity.PlayerInventory;
import shilploka.item.Item;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.ui.InventoryScreen;
import shilploka.ui.WorkstationScreen;
import shilploka.ui.WorldSelectScreen;
import shilploka.workstation.WorkstationType;
import shilploka.entity.EntityManager;
import shilploka.entity.living.LivingEntity;
import shilploka.entity.living.fauna.GauEntity;
import shilploka.entity.render.EntityRenderer;
import shilploka.world.weather.WeatherManager;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * Central engine coordinator managing the GLFW window lifecycle, OpenGL 3.3 Core context,
 * input dispatching, Player physics, swept-AABB collision, hotbar HUD, 3D raycast interaction,
 * infinite chunk streaming with background worker threads, greedy meshing, GPU frustum culling,
 * cellular lighting, dynamic celestial day/night cycle, fluid simulation, and the F3 telemetry overlay.
 *
 * WHAT:
 * Creates a 1280x720 window, configures GLFW input callbacks with cursor capture,
 * compiles chunk and UI shaders, initializes the 2D texture array, boots ChunkManager with
 * background workers, executes the decoupled render loop, and handles graceful teardown.
 *
 * USED FOR:
 * The primary application runtime for ShilpLoka (Java Edition).
 *
 * WHERE NEXT:
 * The main() entry point initializes this class and invokes run().
 */
public class Game {

    private static final Logger LOGGER = LoggerFactory.getLogger(Game.class);

    public static final String GAME_TITLE = "ShilpLoka (शिल्पलोक) — Phase P6 Persistence Engine & Saves";
    public static final int DEFAULT_WIDTH = 1280;
    public static final int DEFAULT_HEIGHT = 720;

    private long window;
    private int windowWidth = DEFAULT_WIDTH;
    private int windowHeight = DEFAULT_HEIGHT;
    private boolean running = true;
    private boolean cursorLocked = true;

    // Subsystems
    private Player player;
    private Camera camera;
    private Hotbar hotbar;
    private Shader chunkShader;
    private TextureArray textureArray;
    private ChunkManager chunkManager;
    private FrustumCuller frustumCuller;
    private DebugOverlay debugOverlay;
    private WorldGenerator worldGen;
    private DayNightCycle dayNightCycle;
    private RaycastResult currentTarget = RaycastResult.MISS;

    // Persistence subsystems
    private SaveManager saveManager;
    private WorldMetadata worldMetadata;
    private PlayerData playerData;
    private WorldSelectScreen worldSelectScreen;
    private boolean inWorldSelect = false;
    private double autosaveTimer = 0.0;
    public static final double AUTOSAVE_INTERVAL_SECONDS = 300.0; // 5 minutes periodic autosave
    private shilploka.world.dimension.DimensionManager dimensionManager;

    // Inventory, Crafting & Dropped Items
    private InventoryScreen inventoryScreen;
    private WorkstationScreen workstationScreen;
    private final shilploka.ui.CodexScreen codexScreen = new shilploka.ui.CodexScreen();
    private final shilploka.ui.BarterScreen barterScreen = new shilploka.ui.BarterScreen();
    private final shilploka.ui.KaushalScreen kaushalScreen = new shilploka.ui.KaushalScreen();
    private boolean inInventory = false;
    private final List<ItemEntity> itemEntities = new ArrayList<>();

    // Living Entity & Fauna System
    private final EntityManager entityManager = new EntityManager();
    private EntityRenderer entityRenderer;

    // Dynamic Weather & Indian Seasons
    private final WeatherManager weatherManager = new WeatherManager();

    // Spatial Audio & Ambient Music (Phase P15)
    private final shilploka.audio.SoundEngine soundEngine = new shilploka.audio.SoundEngine();
    private final shilploka.audio.MusicManager musicManager = new shilploka.audio.MusicManager(soundEngine);

    // Cultural Festivals & Settings Screen (Phase P16)
    private final shilploka.event.FestivalManager festivalManager = new shilploka.event.FestivalManager();
    private shilploka.ui.SettingsScreen settingsScreen;

    // Render configuration
    private int renderDistance = ChunkManager.DEFAULT_RENDER_DISTANCE; // 4 chunks radius

    // Input state tracking
    private boolean keyForward = false;
    private boolean keyBackward = false;
    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false;
    private boolean keyDown = false;
    private boolean keySprint = false;
    private boolean keySneak = false;

    // Mouse tracking
    private double lastMouseX = DEFAULT_WIDTH / 2.0;
    private double lastMouseY = DEFAULT_HEIGHT / 2.0;
    private boolean firstMouse = true;

    // Telemetry & frame timing
    private double lastFrameTime = 0.0;
    private int currentFps = 60;
    private double currentFrameTimeMs = 16.6;
    private int frameCount = 0;
    private double fpsTimer = 0.0;

    /**
     * WHY:
     * Launches the game engine lifecycle.
     *
     * WHAT:
     * Executes init(), loop(), and cleanup() in strict order.
     */
    public void run() {
        LOGGER.info("Starting ShilpLoka (Java Edition) — Phase P6 Persistence Engine & Saves...");
        try {
            init();
            loop();
        } catch (Throwable t) {
            LOGGER.error("Fatal unhandled exception in game lifecycle", t);
            throw t;
        } finally {
            cleanup();
        }
    }

    /**
     * WHY:
     * Initializes GLFW, creates the OpenGL 3.3 Core window, and boots rendering subsystems.
     *
     * WHAT:
     * Sets error callbacks, window hints, context creation, input handlers, shaders,
     * texture array, chunk manager, and F3 overlay.
     */
    private void init() {
        // Setup GLFW error callback logging to SLF4J / System.err
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW native library");
        }

        // Configure GLFW Window Hints for OpenGL 3.3 Core Profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        this.window = glfwCreateWindow(windowWidth, windowHeight, GAME_TITLE, 0L, 0L);
        if (this.window == 0L) {
            throw new RuntimeException("Failed to create GLFW window with OpenGL 3.3 Core Profile");
        }

        // Setup input callbacks
        setupCallbacks();

        // Lock cursor to window for first-person 3D mouse look
        setCursorLock(true);

        // Make OpenGL context current immediately
        glfwMakeContextCurrent(this.window);

        // Initialize OpenGL bindings for current thread before any GL calls or window events
        GL.createCapabilities();
        LOGGER.info("OpenGL Context Created: {} ({})", glGetString(GL_VERSION), glGetString(GL_RENDERER));

        // Enable VSync (1 = synchronized to display refresh rate)
        glfwSwapInterval(1);

        // Center window on primary monitor (skip on Wayland where compositor manages position)
        boolean isWayland = System.getenv("WAYLAND_DISPLAY") != null;
        if (!isWayland) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                glfwGetWindowSize(this.window, pWidth, pHeight);
                long monitor = glfwGetPrimaryMonitor();
                if (monitor != 0L) {
                    var vidmode = glfwGetVideoMode(monitor);
                    if (vidmode != null) {
                        glfwSetWindowPos(this.window,
                                (vidmode.width() - pWidth.get(0)) / 2,
                                (vidmode.height() - pHeight.get(0)) / 2);
                    }
                }
            }
        }

        // Show window after OpenGL context and capabilities are ready
        glfwShowWindow(this.window);

        // Configure 3D global OpenGL pipeline states
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Set warm Indian morning horizon clear color (soft sky blue #8CB4D2)
        glClearColor(0.55f, 0.70f, 0.85f, 1.0f);

        // Initialize game subsystems
        this.chunkShader = new Shader("/shaders/chunk.vert", "/shaders/chunk.frag");
        this.textureArray = new TextureArray();
        this.frustumCuller = new FrustumCuller();

        // Initialize persistence subsystems and saves directory
        File savesDir = new File("saves");
        if (!savesDir.exists()) savesDir.mkdirs();

        String worldProp = System.getProperty("shilploka.world");
        String initialWorld = (worldProp != null) ? worldProp : "Mera Bharat";
        this.saveManager = new SaveManager(savesDir, initialWorld);

        this.worldMetadata = this.saveManager.loadWorldMetadata();
        if (this.worldMetadata == null) {
            this.worldMetadata = new WorldMetadata(initialWorld, 19470815L, "creative");
            try {
                this.saveManager.saveWorldMetadata(this.worldMetadata);
            } catch (IOException e) {
                LOGGER.warn("Failed initial save of world metadata: {}", e.getMessage());
            }
        }

        this.playerData = this.saveManager.loadPlayerData();
        if (this.playerData == null) {
            this.playerData = new PlayerData();
            try {
                this.saveManager.savePlayerData(this.playerData);
            } catch (IOException e) {
                LOGGER.warn("Failed initial save of player data: {}", e.getMessage());
            }
        }

        // Automatic pre-session backup
        this.saveManager.createBackup();

        // Initialize world generator and chunk streaming manager with background workers
        long seed = this.worldMetadata.getSeed();
        this.worldGen = new WorldGenerator(seed);
        this.chunkManager = new ChunkManager(this.worldGen, this.saveManager);
        this.dimensionManager = new shilploka.world.dimension.DimensionManager(seed);
        this.chunkManager.setDimensionManager(this.dimensionManager);

        // Initialize Player avatar, hotbar HUD, and first-person camera from persisted state
        double[] pos = this.playerData.getPosition();
        float[] rot = this.playerData.getRotation();
        this.player = new Player(pos[0], pos[1], pos[2]);
        this.player.setFlying(this.playerData.isFlying());
        this.camera = new Camera((float) player.getEyeX(), (float) player.getEyeY(), (float) player.getEyeZ());
        this.camera.setYaw(rot[0]);
        this.camera.setPitch(rot[1]);

        this.hotbar = new Hotbar();
        this.hotbar.selectSlot(this.playerData.getSelectedHotbarSlot());

        // Initialize F3 diagnostic overlay
        this.debugOverlay = new DebugOverlay();

        // Initialize Day/Night celestial cycle
        this.dayNightCycle = new DayNightCycle();
        this.dayNightCycle.setTime(this.worldMetadata.getWorldTimeTicks() % 24000L);
        this.dayNightCycle.setDayCount((int) this.worldMetadata.getDayCount());

        // Initialize World Selection UI
        this.worldSelectScreen = new WorldSelectScreen(savesDir, this::switchWorld);

        // Initialize Settings & Options Screen
        this.settingsScreen = new shilploka.ui.SettingsScreen(this.soundEngine, this.festivalManager);
        this.settingsScreen.setListener(new shilploka.ui.SettingsScreen.SettingsListener() {
            @Override
            public void onRenderDistanceChanged(int newDistance) {
                renderDistance = newDistance;
            }

            @Override
            public void onFovChanged(float newFov) {
                if (camera != null) camera.setFov(newFov);
            }
        });

        // Initialize Item & Recipe Catalogs
        ItemRegistry.init();
        RecipeManager.init();

        // Restore player survival vitals from persisted state
        if (this.playerData.getSurvivalStats() != null && this.player.getVitals() != null) {
            var ss = this.playerData.getSurvivalStats();
            this.player.getVitals().setHealth(ss.health);
            this.player.getVitals().setBhookh(ss.bhookh);
            this.player.getVitals().setSaturation(ss.saturation);
            this.player.getVitals().setExhaustion(ss.exhaustion);
            this.player.getVitals().setAirBubbles(ss.airBubbles);
        }

        // Restore player inventory from persisted state
        if (this.playerData.getInventory() != null && this.player.getInventory() != null) {
            for (PlayerData.InventoryItem invItem : this.playerData.getInventory()) {
                Item item = ItemRegistry.getItem(invItem.item);
                if (item != null) {
                    ItemStack stack = new ItemStack(item, invItem.count);
                    stack.setDamage(invItem.damage);
                    this.player.getInventory().setSlot(invItem.slot, stack);
                }
            }
        }

        // Initialize 2D interactive inventory and crafting screen
        this.inventoryScreen = new InventoryScreen();
        this.workstationScreen = new WorkstationScreen();
        this.entityRenderer = new EntityRenderer();

        this.lastFrameTime = glfwGetTime();
        LOGGER.info("ShilpLoka Phase P9 initialization completed successfully (World: '{}').", this.worldMetadata.getWorldName());
    }

    /**
     * WHY:
     * Registers GLFW event listeners for keyboard input, mouse look, block interactions,
     * scroll wheel hotbar selection, and window resizing.
     */
    private void setupCallbacks() {
        // Keyboard handler
        // Keyboard handler
        glfwSetKeyCallback(this.window, (w, key, scancode, action, mods) -> {
            boolean pressed = (action == GLFW_PRESS || action == GLFW_REPEAT);

            // Handle World Select Screen keys
            if (inWorldSelect && worldSelectScreen != null) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                    inWorldSelect = false;
                    setCursorLock(true);
                    return;
                }
                if (worldSelectScreen.handleKey(key, action)) {
                    return;
                }
            }

            // Handle Workstation Screen keys
            if (workstationScreen != null && workstationScreen.isOpen()) {
                if (workstationScreen.handleKey(key, action, player)) {
                    if (!workstationScreen.isOpen()) {
                        setCursorLock(true);
                    }
                    return;
                }
            }

            // Handle Inventory Screen keys
            if (inInventory && inventoryScreen != null) {
                if (inventoryScreen.handleKey(key, action, player)) {
                    if (!inventoryScreen.isOpen()) {
                        inInventory = false;
                        setCursorLock(true);
                    }
                    return;
                }
            }

            // Handle Codex Screen keys
            if (codexScreen.isOpen()) {
                if (action == GLFW_PRESS) {
                    codexScreen.handleKeyPress(key);
                    if (!codexScreen.isOpen()) {
                        setCursorLock(true);
                    }
                }
                return;
            }

            // Handle Barter Screen keys
            if (barterScreen.isOpen()) {
                if (action == GLFW_PRESS) {
                    barterScreen.handleKeyPress(key, player);
                    if (!barterScreen.isOpen()) {
                        setCursorLock(true);
                    }
                }
                return;
            }

            // Handle Kaushal Screen keys
            if (kaushalScreen.isOpen()) {
                if (action == GLFW_PRESS) {
                    kaushalScreen.handleKeyPress(key, player);
                    if (!kaushalScreen.isOpen()) {
                        setCursorLock(true);
                    }
                }
                return;
            }

            // Handle Settings Screen keys
            if (settingsScreen != null && settingsScreen.isOpen()) {
                if (settingsScreen.handleKey(key, action)) {
                    if (!settingsScreen.isOpen()) {
                        setCursorLock(true);
                    }
                    return;
                }
            }

            switch (key) {
                case GLFW_KEY_ESCAPE -> {
                    if (action == GLFW_PRESS || action == GLFW_RELEASE) {
                        if (settingsScreen != null && settingsScreen.isOpen()) {
                            settingsScreen.setOpen(false);
                            setCursorLock(true);
                            return;
                        }
                        if (kaushalScreen.isOpen()) {
                            kaushalScreen.close();
                            setCursorLock(true);
                            return;
                        }
                        if (barterScreen.isOpen()) {
                            barterScreen.close();
                            setCursorLock(true);
                            return;
                        }
                        if (codexScreen.isOpen()) {
                            codexScreen.setOpen(false);
                            setCursorLock(true);
                            return;
                        }
                        if (workstationScreen != null && workstationScreen.isOpen()) {
                            workstationScreen.close(player);
                            setCursorLock(true);
                            return;
                        }
                        if (inInventory) {
                            inInventory = false;
                            if (inventoryScreen != null) inventoryScreen.setOpen(false, player);
                            setCursorLock(true);
                            return;
                        }
                        if (action == GLFW_RELEASE) {
                            setCursorLock(!cursorLocked);
                        }
                    }
                }
                case GLFW_KEY_G -> {
                    if (action == GLFW_PRESS && !inWorldSelect) {
                        if (codexScreen.isOpen()) {
                            codexScreen.setOpen(false);
                            setCursorLock(true);
                        } else {
                            if (workstationScreen != null && workstationScreen.isOpen()) workstationScreen.close(player);
                            if (inInventory) {
                                inInventory = false;
                                if (inventoryScreen != null) inventoryScreen.setOpen(false, player);
                            }
                            codexScreen.setOpen(true);
                            setCursorLock(false);
                        }
                    }
                }
                case GLFW_KEY_O -> {
                    if (action == GLFW_PRESS && !inWorldSelect) {
                        if (settingsScreen != null) {
                            settingsScreen.toggle();
                            setCursorLock(!settingsScreen.isOpen());
                        }
                    }
                }
                case GLFW_KEY_E -> {
                    if (action == GLFW_PRESS && !inWorldSelect && player != null) {
                        if (workstationScreen != null && workstationScreen.isOpen()) {
                            workstationScreen.close(player);
                            setCursorLock(true);
                            return;
                        }
                        inInventory = !inInventory;
                        if (inventoryScreen != null) {
                            inventoryScreen.setOpen(inInventory, player);
                        }
                        setCursorLock(!inInventory);
                    }
                }
                case GLFW_KEY_Q -> {
                    if (action == GLFW_PRESS && !inInventory && (workstationScreen == null || !workstationScreen.isOpen()) && !inWorldSelect && player != null) {
                        int selected = (hotbar != null) ? hotbar.getSelectedSlot() : 0;
                        ItemStack stack = player.getInventory().getSlot(selected);
                        if (stack != null && !stack.isEmpty()) {
                            ItemStack dropped = stack.split(1);
                            if (stack.isEmpty()) {
                                player.getInventory().setSlot(selected, ItemStack.EMPTY);
                            }
                            float yawRad = (float) Math.toRadians(camera.getYaw());
                            float pitchRad = (float) Math.toRadians(camera.getPitch());
                            float vx = (float) (-Math.sin(yawRad) * Math.cos(pitchRad) * 4.0);
                            float vy = (float) (-Math.sin(pitchRad) * 4.0 + 1.5);
                            float vz = (float) (Math.cos(yawRad) * Math.cos(pitchRad) * 4.0);
                            itemEntities.add(new ItemEntity(player.getEyeX(), player.getEyeY() - 0.2, player.getEyeZ(), dropped, vx, vy, vz));
                        }
                    }
                }
                case GLFW_KEY_M -> {
                    if (action == GLFW_PRESS) {
                        if (workstationScreen != null && workstationScreen.isOpen()) {
                            workstationScreen.close(player);
                        }
                        if (inInventory) {
                            inInventory = false;
                            if (inventoryScreen != null) inventoryScreen.setOpen(false, player);
                        }
                        inWorldSelect = !inWorldSelect;
                        if (inWorldSelect) {
                            setCursorLock(false);
                            if (worldSelectScreen != null) worldSelectScreen.refreshWorldList();
                        } else {
                            setCursorLock(true);
                        }
                    }
                }
                case GLFW_KEY_F5 -> {
                    if (action == GLFW_PRESS) {
                        saveCurrentWorldState();
                    }
                }
                case GLFW_KEY_F3 -> {
                    if (action == GLFW_PRESS) {
                        debugOverlay.toggle();
                    }
                }
                case GLFW_KEY_F -> {
                    if (action == GLFW_PRESS && player != null) {
                        player.toggleFly();
                    }
                }
                case GLFW_KEY_W -> keyForward = pressed;
                case GLFW_KEY_S -> keyBackward = pressed;
                case GLFW_KEY_A -> keyLeft = pressed;
                case GLFW_KEY_D -> keyRight = pressed;
                case GLFW_KEY_SPACE -> keyUp = pressed;
                case GLFW_KEY_LEFT_SHIFT -> { keyDown = pressed; keySneak = pressed; }
                case GLFW_KEY_LEFT_CONTROL -> keySprint = pressed;
                case GLFW_KEY_1 -> { if (pressed && hotbar != null) hotbar.selectSlot(0); }
                case GLFW_KEY_2 -> { if (pressed && hotbar != null) hotbar.selectSlot(1); }
                case GLFW_KEY_3 -> { if (pressed && hotbar != null) hotbar.selectSlot(2); }
                case GLFW_KEY_4 -> { if (pressed && hotbar != null) hotbar.selectSlot(3); }
                case GLFW_KEY_5 -> { if (pressed && hotbar != null) hotbar.selectSlot(4); }
                case GLFW_KEY_6 -> { if (pressed && hotbar != null) hotbar.selectSlot(5); }
                case GLFW_KEY_7 -> { if (pressed && hotbar != null) hotbar.selectSlot(6); }
                case GLFW_KEY_8 -> { if (pressed && hotbar != null) hotbar.selectSlot(7); }
                case GLFW_KEY_9 -> { if (pressed && hotbar != null) hotbar.selectSlot(8); }
                default -> {}
            }
        });

        // Unicode character typing handler for World Creation name and seed inputs
        glfwSetCharCallback(this.window, (w, codepoint) -> {
            if (inWorldSelect && worldSelectScreen != null) {
                worldSelectScreen.handleChar(codepoint);
            }
        });

        // Mouse look and cursor position handler
        glfwSetCursorPosCallback(this.window, (w, xpos, ypos) -> {
            lastMouseX = xpos;
            lastMouseY = ypos;

            if (workstationScreen != null && workstationScreen.isOpen()) {
                workstationScreen.updateMouse(xpos, ypos);
                return;
            }

            if (inInventory && inventoryScreen != null) {
                inventoryScreen.updateMouse(xpos, ypos);
                return;
            }

            if (!cursorLocked || inWorldSelect) return;

            if (firstMouse) {
                lastMouseX = xpos;
                lastMouseY = ypos;
                firstMouse = false;
            }

            float dx = (float) (xpos - lastMouseX);
            float dy = (float) (ypos - lastMouseY);

            camera.processMouseLook(dx, dy);
        });

        // Mouse scroll wheel handler for 9-slot Hotbar cycling
        glfwSetScrollCallback(this.window, (w, xoffset, yoffset) -> {
            if (inWorldSelect || inInventory || (workstationScreen != null && workstationScreen.isOpen())) return;
            if (hotbar != null) {
                hotbar.scroll((int) yoffset);
            }
        });

        // Mouse click handler for block breaking (Left-Click), placing (Right-Click), and UI interactions
        glfwSetMouseButtonCallback(this.window, (w, button, action, mods) -> {
            if (inWorldSelect) return;

            if (workstationScreen != null && workstationScreen.isOpen()) {
                if (action == GLFW_PRESS) {
                    workstationScreen.handleMouseClick(button, action, player, windowWidth, windowHeight);
                }
                return;
            }

            if (inInventory && inventoryScreen != null) {
                if (action == GLFW_PRESS) {
                    inventoryScreen.handleMouseClick(button, action, player, windowWidth, windowHeight);
                }
                return;
            }

            if (!cursorLocked) {
                if (action == GLFW_PRESS) setCursorLock(true);
                return;
            }

            if (action == GLFW_PRESS) {
                RaycastResult hit = Raycaster.raycast(camera.getPosition(), camera.getFront(), Raycaster.DEFAULT_REACH, chunkManager);

                if (button == GLFW_MOUSE_BUTTON_1) {
                    // 1. Try melee attack on living entities along line of sight
                    ItemStack held = (player != null && hotbar != null) ? player.getInventory().getSlot(hotbar.getSelectedSlot()) : ItemStack.EMPTY;
                    float attackDmg = (!held.isEmpty() && held.getItem().isTool()) ? held.getItem().getToolTier().getAttackDamage() : 1.0f;
                    LivingEntity hitEntity = entityManager.attackTarget(camera.getPosition(), camera.getFront(), 3.5f, attackDmg);
                    if (hitEntity != null) {
                        return; // Entity struck, block break skipped
                    }

                    // 2. Left-Click: block break and drop ItemEntity
                    if (hit.hasHit()) {
                        int brokenId = chunkManager.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
                        chunkManager.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), BlockIds.AIR);
                        if (soundEngine != null) {
                            soundEngine.playSound(shilploka.audio.SoundEvent.BLOCK_BREAK, hit.blockX(), hit.blockY(), hit.blockZ(), 1.0f, 1.0f);
                        }

                        ItemStack drop = ItemRegistry.getBlockDrop(brokenId);
                        if (!drop.isEmpty()) {
                            float vx = (float) ((Math.random() - 0.5) * 1.5);
                            float vy = 2.0f;
                            float vz = (float) ((Math.random() - 0.5) * 1.5);
                            itemEntities.add(new ItemEntity(hit.blockX() + 0.5, hit.blockY() + 0.5, hit.blockZ() + 0.5,
                                    drop, vx, vy, vz));
                        }
                    }
                } else if (button == GLFW_MOUSE_BUTTON_2) {
                    // Check entity interaction first (e.g. milking sacred Gau or trading with Gramwasi)
                    for (LivingEntity e : entityManager.getEntities()) {
                        if (e instanceof GauEntity gau && e.getBoundingBox().intersectsRay(camera.getPosition(), camera.getFront(), 3.5f)) {
                            if (gau.interact(player)) return;
                        } else if (e instanceof shilploka.entity.living.npc.GramwasiEntity gramwasi &&
                                   e.getBoundingBox().intersectsRay(camera.getPosition(), camera.getFront(), 3.5f)) {
                            barterScreen.open(gramwasi);
                            setCursorLock(false);
                            return;
                        } else if (e instanceof shilploka.entity.living.mount.RideableMount mount &&
                                   e.getBoundingBox().intersectsRay(camera.getPosition(), camera.getFront(), 3.5f)) {
                            ItemStack held = (player != null && hotbar != null) ? player.getInventory().getSlot(hotbar.getSelectedSlot()) : ItemStack.EMPTY;
                            if (mount.interact(player, held)) {
                                return;
                            }
                        }
                    }

                    // Workstation UI & Shilp Mandap opening
                    if (hit.hasHit()) {
                        int clickedBlock = chunkManager.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
                        if (clickedBlock == BlockIds.SHILP_MANDAP) {
                            kaushalScreen.open();
                            setCursorLock(false);
                            return;
                        }

                        if (BlockIds.isWorkstation(clickedBlock)) {
                            var wsType = shilploka.workstation.WorkstationType.fromBlockId(clickedBlock);
                            if (wsType != null && workstationScreen != null) {
                                workstationScreen.open(wsType);
                                setCursorLock(false);
                                return;
                            }
                        }

                        // Soil tilling with Phaavda
                        ItemStack held = (player != null && hotbar != null) ? player.getInventory().getSlot(hotbar.getSelectedSlot()) : ItemStack.EMPTY;
                        if (!held.isEmpty() && held.getItem().getToolType() == shilploka.item.ToolType.PHAAVDA) {
                            if (shilploka.agriculture.CropManager.tillSoil(chunkManager, hit.blockX(), hit.blockY(), hit.blockZ())) {
                                held.applyDamage(1);
                                return;
                            }
                        }

                        // Crop planting on Tilled Soil
                        if (!held.isEmpty()) {
                            int plantCropId = -1;
                            if (held.getItem().getId() == ItemRegistry.CHAWAL.getId()) plantCropId = BlockIds.DHAN_CROP;
                            else if (held.getItem().getId() == ItemRegistry.GANNA.getId()) plantCropId = BlockIds.GANNA_CROP;
                            else if (held.getItem().getId() == ItemRegistry.KAPAAS.getId()) plantCropId = BlockIds.KAPAAS_CROP;

                            if (plantCropId != -1 && shilploka.agriculture.CropManager.plantCrop(chunkManager, hit.placeX(), hit.placeY(), hit.placeZ(), plantCropId)) {
                                held.shrink(1);
                                return;
                            }
                        }
                    }

                    // Block placement with player body intersection prevention
                    if (hit.hasHit() && hotbar != null && player != null) {
                        AABB placeBox = AABB.fromBlock(hit.placeX(), hit.placeY(), hit.placeZ());
                        if (!player.getBoundingBox().intersects(placeBox)) {
                            chunkManager.setBlock(hit.placeX(), hit.placeY(), hit.placeZ(), hotbar.getSelectedBlockId());
                            if (soundEngine != null) {
                                soundEngine.playSound(shilploka.audio.SoundEvent.BLOCK_PLACE, hit.placeX(), hit.placeY(), hit.placeZ(), 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
        });

        // Window resize handler
        glfwSetFramebufferSizeCallback(this.window, (w, width, height) -> {
            if (width > 0 && height > 0) {
                this.windowWidth = width;
                this.windowHeight = height;
                glViewport(0, 0, width, height);
            }
        });
    }

    /**
     * WHY:
     * Toggles between raw captured cursor (for 3D look) and normal desktop cursor.
     */
    private void setCursorLock(boolean lock) {
        this.cursorLocked = lock;
        if (lock) {
            glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            this.firstMouse = true;
        } else {
            glfwSetInputMode(this.window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    /**
     * WHY:
     * The core uncoupled game loop.
     *
     * WHAT:
     * Measures frame delta time, updates chunk streaming and camera physics,
     * performs frustum culling and multi-pass rendering, updates FPS telemetry,
     * and swaps display buffers.
     */
    private void loop() {
        String testTicksProp = System.getProperty("shilploka.testTicks");
        int maxTicks = (testTicksProp != null) ? Integer.parseInt(testTicksProp) : -1;
        int tickCounter = 0;

        while (!glfwWindowShouldClose(this.window) && running) {
            if (maxTicks > 0 && ++tickCounter >= maxTicks) {
                LOGGER.info("Reached automated verification tick limit ({}), exiting cleanly.", maxTicks);
                break;
            }

            double currentTime = glfwGetTime();
            float deltaTime = (float) (currentTime - lastFrameTime);
            lastFrameTime = currentTime;

            // Update FPS metrics once per second
            frameCount++;
            fpsTimer += deltaTime;
            if (fpsTimer >= 1.0) {
                currentFps = frameCount;
                currentFrameTimeMs = (fpsTimer / frameCount) * 1000.0;
                frameCount = 0;
                fpsTimer = 0.0;
            }

            // Periodic autosave (every 5 minutes / 300 seconds)
            autosaveTimer += deltaTime;
            if (autosaveTimer >= AUTOSAVE_INTERVAL_SECONDS) {
                autosaveTimer = 0.0;
                saveCurrentWorldState();
            }

            // Poll OS window events
            glfwPollEvents();

            if (!inWorldSelect) {
                // 1. Advance celestial day/night cycle & dynamic weather
                if (dayNightCycle != null) {
                    dayNightCycle.update(deltaTime);
                }
                if (weatherManager != null && player != null && worldGen != null && dayNightCycle != null) {
                    weatherManager.update(deltaTime, (int) player.getPosition().x, (int) player.getPosition().z, (int) dayNightCycle.getDayCount(), worldGen.getBiomeProvider());
                }

                // Tick active workstation timers (Kiln, Alloy smelter)
                if (workstationScreen != null && workstationScreen.isOpen()) {
                    workstationScreen.tick();
                }

                // 2. Update targeted block via 3D DDA raycasting along sightline
                this.currentTarget = Raycaster.raycast(camera.getPosition(), camera.getFront(), Raycaster.DEFAULT_REACH, chunkManager);

                // 3. Update player physics simulation and synchronize camera position
                if (cursorLocked && player != null) {
                    player.tick(keyForward, keyBackward, keyLeft, keyRight,
                            keyUp, keySneak, keySprint,
                            camera.getYaw(), camera.getPitch(),
                            deltaTime, chunkManager);

                    camera.getPosition().set((float) player.getEyeX(), (float) player.getEyeY(), (float) player.getEyeZ());
                }

                // 4. Update infinite chunk streaming around player
                chunkManager.update(camera, renderDistance);

                // 5. Update dropped ItemEntity physics, magnet pull, and inventory pickup
                Iterator<ItemEntity> it = itemEntities.iterator();
                while (it.hasNext()) {
                    ItemEntity itemEnt = it.next();
                    itemEnt.tick(deltaTime, chunkManager, player);
                    if (itemEnt.isDead()) {
                        it.remove();
                        continue;
                    }
                    if (player != null && itemEnt.getPickupDelay() <= 0) {
                        double dx = player.getPosition().x - itemEnt.getPosition().x;
                        double dy = (player.getPosition().y + 0.5) - itemEnt.getPosition().y;
                        double dz = player.getPosition().z - itemEnt.getPosition().z;
                        double distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq <= ItemEntity.PICKUP_RADIUS * ItemEntity.PICKUP_RADIUS) {
                            ItemStack remaining = player.getInventory().addItem(itemEnt.getItemStack());
                            if (remaining.isEmpty()) {
                                itemEnt.setDead(true);
                                it.remove();
                            } else {
                                itemEnt.setItemStack(remaining);
                            }
                        }
                    }
                }

                // 6. Update living entities, predators, and nocturnal wildlife
                entityManager.tick(deltaTime, chunkManager, player, dayNightCycle, itemEntities);

                // 7. Update dimensions and portal detection
                if (dimensionManager != null && player != null && chunkManager != null) {
                    dimensionManager.tick(deltaTime);
                    if (dimensionManager.checkPortalCollision(player, chunkManager)) {
                        chunkManager.unloadAllChunks();
                        if (dimensionManager.getCurrentDimension() == shilploka.world.dimension.DimensionType.PAATAAL) {
                            dimensionManager.spawnBossInPaataal(entityManager, chunkManager, player);
                        }
                    }
                }

                // 8. Update Spatial Audio & Dynamic Classical Music
                if (soundEngine != null && player != null && camera != null) {
                    var front = camera.getFront();
                    var up = camera.getUp();
                    soundEngine.setListenerOrientation(player.getPosition().x, player.getPosition().y, player.getPosition().z,
                            front.x, front.y, front.z, up.x, up.y, up.z);
                    if (musicManager != null && dimensionManager != null && dayNightCycle != null) {
                        shilploka.worldgen.biome.BiomeType curBiome = (worldGen != null) ?
                                worldGen.getBiomeProvider().getBiomeAt((int) player.getPosition().x, (int) player.getPosition().z) :
                                shilploka.worldgen.biome.BiomeType.GANGA_MAIDAN;
                        musicManager.tick(deltaTime, curBiome, dimensionManager.getCurrentDimension(), dayNightCycle.isNight());
                    }
                }
            }

            // 8. Render 3D and 2D scenes
            render();

            // Swap front and back OpenGL buffers
            glfwSwapBuffers(this.window);
        }
    }

    /**
     * WHY:
     * Executes the rendering pipeline: clears buffers, updates frustum planes,
     * draws 3D chunk geometry with greedy meshing and frustum culling, and overlays 2D F3 diagnostics.
     */
    private void render() {
        // Update background clear color dynamically to match the celestial sky
        if (dayNightCycle != null) {
            var skyCol = dayNightCycle.getSkyColor();
            glClearColor(skyCol.x, skyCol.y, skyCol.z, 1.0f);
        }

        // Clear color and depth buffers
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Aspect ratio calculation
        float aspectRatio = (float) windowWidth / Math.max(1, windowHeight);

        // Update view-projection matrix and extract frustum culling planes
        Matrix4f viewProj = camera.getViewProjectionMatrix(aspectRatio);
        frustumCuller.update(viewProj);

        // --- 1. RENDER 3D CHUNK MESHES ---
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        chunkShader.bind();
        chunkShader.setMat4("u_ViewProjection", viewProj);
        chunkShader.setInt("u_TextureArray", 0);

        // Transmit dynamic celestial lighting & atmospheric fog uniforms
        float sunFactor = (dayNightCycle != null) ? dayNightCycle.getSunlightFactor() : 1.0f;
        if (weatherManager != null) {
            sunFactor = weatherManager.getSunlightFactor(sunFactor);
        }
        chunkShader.setFloat("u_SunlightFactor", sunFactor);

        var fogCol = (dayNightCycle != null) ? dayNightCycle.getFogColor() : new org.joml.Vector3f(0.6f, 0.75f, 0.95f);
        if (weatherManager != null) {
            fogCol = weatherManager.getModifiedFogColor(fogCol);
        }
        chunkShader.setVec3("u_FogColor", fogCol.x, fogCol.y, fogCol.z);
        chunkShader.setVec3("u_CameraPos", camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

        float fogDistMult = (weatherManager != null) ? weatherManager.getFogDistanceMultiplier() : 1.0f;
        chunkShader.setFloat("u_FogNear", Math.max(16.0f, (renderDistance - 1) * 16.0f * fogDistMult));
        chunkShader.setFloat("u_FogFar", (renderDistance + 1) * 16.0f * fogDistMult);

        // Bind 2D Texture Array to unit 0
        textureArray.bind();

        // Render visible chunks with GPU frustum culling
        chunkManager.render(chunkShader, frustumCuller);

        textureArray.unbind();
        chunkShader.unbind();

        // --- 1.5 RENDER LIVING ENTITIES ---
        if (entityRenderer != null) {
            entityRenderer.renderEntities(entityManager.getEntities(), camera, aspectRatio, sunFactor);
        }

        // --- 2. RENDER 2D UI & OVERLAYS ---
        if (inWorldSelect && worldSelectScreen != null) {
            worldSelectScreen.render(debugOverlay.getFont(), debugOverlay.getUiShader(), windowWidth, windowHeight);
        } else {
            debugOverlay.render(camera, chunkManager, this.worldGen, hotbar, player, currentTarget, dayNightCycle, windowWidth, windowHeight, currentFps, currentFrameTimeMs);
            if (inInventory && inventoryScreen != null) {
                inventoryScreen.render(debugOverlay.getFont(), debugOverlay.getUiShader(), player, windowWidth, windowHeight);
            }
            if (workstationScreen != null && workstationScreen.isOpen()) {
                workstationScreen.render(debugOverlay.getFont(), debugOverlay.getUiShader(), player, windowWidth, windowHeight);
            }
            if (codexScreen != null && codexScreen.isOpen()) {
                codexScreen.render(debugOverlay.getUiShader(), debugOverlay.getFont(), windowWidth, windowHeight);
            }
            if (barterScreen != null && barterScreen.isOpen()) {
                barterScreen.render(debugOverlay.getUiShader(), debugOverlay.getFont(), windowWidth, windowHeight);
            }
            if (kaushalScreen != null && kaushalScreen.isOpen()) {
                kaushalScreen.render(debugOverlay.getUiShader(), debugOverlay.getFont(), windowWidth, windowHeight);
            }
            if (settingsScreen != null && settingsScreen.isOpen()) {
                settingsScreen.render(debugOverlay.getFont(), debugOverlay.getUiShader(), windowWidth, windowHeight);
            }
        }
    }

    /**
     * WHY:
     * Commits all modified chunk voxels, celestial time ticks, and player vitals to disk.
     */
    public synchronized void saveCurrentWorldState() {
        if (saveManager == null || worldMetadata == null) return;

        LOGGER.info("Executing world save for '{}'...", worldMetadata.getWorldName());

        // Update player data
        if (player != null && camera != null && hotbar != null && playerData != null) {
            playerData.setPosition(new double[]{player.getPosition().x, player.getPosition().y, player.getPosition().z});
            playerData.setRotation(new float[]{camera.getYaw(), camera.getPitch()});
            playerData.setFlying(player.isFlying());
            playerData.setSelectedHotbarSlot(hotbar.getSelectedSlot());
            if (dimensionManager != null) {
                playerData.setDimension(dimensionManager.getCurrentDimension().name());
            }

            // Persist survival vitals
            if (player.getVitals() != null) {
                PlayerData.SurvivalStats ss = playerData.getSurvivalStats();
                if (ss == null) {
                    ss = new PlayerData.SurvivalStats();
                    playerData.setSurvivalStats(ss);
                }
                ss.health = player.getVitals().getHealth();
                ss.bhookh = player.getVitals().getBhookh();
                ss.saturation = player.getVitals().getSaturation();
                ss.exhaustion = player.getVitals().getExhaustion();
                ss.airBubbles = player.getVitals().getAirBubbles();
            }

            // Persist inventory
            if (player.getInventory() != null) {
                List<PlayerData.InventoryItem> items = new ArrayList<>();
                PlayerInventory inv = player.getInventory();
                for (int i = 0; i < PlayerInventory.TOTAL_SLOTS; i++) {
                    ItemStack stack = inv.getSlot(i);
                    if (!stack.isEmpty()) {
                        items.add(new PlayerData.InventoryItem(i, stack.getItem().getId(), stack.getCount(), stack.getDamage()));
                    }
                }
                playerData.setInventory(items);
            }
        }

        // Update world metadata
        if (dayNightCycle != null) {
            worldMetadata.setWorldTimeTicks((long) dayNightCycle.getWorldTime());
            worldMetadata.setDayCount(dayNightCycle.getDayCount());
        }

        saveManager.saveWorld(worldMetadata, playerData, (chunkManager != null) ? chunkManager.getLoadedChunks() : null);
    }

    /**
     * WHY:
     * Transitions active gameplay session to another saved or newly created world.
     */
    private void switchWorld(SaveManager newSm, WorldMetadata newMeta) {
        LOGGER.info("Switching active world to '{}'...", newMeta.getWorldName());

        // 1. Save and close current world
        saveCurrentWorldState();
        try {
            if (this.saveManager != null) {
                this.saveManager.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing previous SaveManager: {}", e.getMessage());
        }

        // 2. Cleanup current chunk manager and geometry
        if (this.chunkManager != null) {
            this.chunkManager.cleanup();
        }

        // 3. Clear active item entities, living creatures & close open screens
        this.itemEntities.clear();
        this.entityManager.clear();
        if (this.inventoryScreen != null) {
            this.inventoryScreen.setOpen(false, player);
        }
        if (this.workstationScreen != null && this.workstationScreen.isOpen()) {
            this.workstationScreen.close(player);
        }
        this.inInventory = false;

        // 4. Bind new SaveManager and Metadata
        this.saveManager = newSm;
        this.worldMetadata = newMeta;
        this.saveManager.createBackup();

        this.playerData = this.saveManager.loadPlayerData();
        if (this.playerData == null) {
            this.playerData = new PlayerData();
            try {
                this.saveManager.savePlayerData(this.playerData);
            } catch (IOException e) {
                LOGGER.warn("Failed saving default player data: {}", e.getMessage());
            }
        }

        // 5. Rebuild WorldGenerator & ChunkManager
        this.worldGen = new WorldGenerator(this.worldMetadata.getSeed());
        this.chunkManager = new ChunkManager(this.worldGen, this.saveManager);
        this.dimensionManager = new shilploka.world.dimension.DimensionManager(this.worldMetadata.getSeed());
        this.chunkManager.setDimensionManager(this.dimensionManager);

        // 6. Restore Player coordinates and orientation
        double[] pos = this.playerData.getPosition();
        float[] rot = this.playerData.getRotation();
        this.player.setPosition(pos[0], pos[1], pos[2]);
        this.player.setFlying(this.playerData.isFlying());
        this.camera.getPosition().set((float) player.getEyeX(), (float) player.getEyeY(), (float) player.getEyeZ());
        this.camera.setYaw(rot[0]);
        this.camera.setPitch(rot[1]);
        this.hotbar.selectSlot(this.playerData.getSelectedHotbarSlot());

        // Restore survival vitals
        if (this.playerData.getSurvivalStats() != null && this.player.getVitals() != null) {
            var ss = this.playerData.getSurvivalStats();
            this.player.getVitals().setHealth(ss.health);
            this.player.getVitals().setBhookh(ss.bhookh);
            this.player.getVitals().setSaturation(ss.saturation);
            this.player.getVitals().setExhaustion(ss.exhaustion);
            this.player.getVitals().setAirBubbles(ss.airBubbles);
        }

        // Restore inventory
        if (this.player.getInventory() != null) {
            this.player.getInventory().clear();
            if (this.playerData.getInventory() != null) {
                for (PlayerData.InventoryItem invItem : this.playerData.getInventory()) {
                    Item item = ItemRegistry.getItem(invItem.item);
                    if (item != null) {
                        ItemStack stack = new ItemStack(item, invItem.count);
                        stack.setDamage(invItem.damage);
                        this.player.getInventory().setSlot(invItem.slot, stack);
                    }
                }
            }
        }

        // 7. Restore celestial time
        if (this.dayNightCycle != null) {
            this.dayNightCycle.setTime(this.worldMetadata.getWorldTimeTicks() % 24000L);
            this.dayNightCycle.setDayCount((int) this.worldMetadata.getDayCount());
        }

        // 8. Transition back into game and lock cursor
        this.inWorldSelect = false;
        setCursorLock(true);
        LOGGER.info("Successfully transitioned to world '{}'.", newMeta.getWorldName());
    }

    /**
     * WHY:
     * Releases all OpenGL, worker threads, and native GLFW resources upon application shutdown.
     */
    private void cleanup() {
        LOGGER.info("Cleaning up ShilpLoka engine resources...");

        // Save active world state and dirty chunks
        saveCurrentWorldState();

        if (saveManager != null) {
            try {
                saveManager.close();
            } catch (IOException e) {
                LOGGER.error("Error closing SaveManager: {}", e.getMessage());
            }
        }

        if (chunkManager != null) chunkManager.cleanup();
        if (textureArray != null) textureArray.cleanup();
        if (chunkShader != null) chunkShader.cleanup();
        if (debugOverlay != null) debugOverlay.cleanup();
        if (entityRenderer != null) entityRenderer.cleanup();
        if (soundEngine != null) soundEngine.cleanup();

        if (this.window != 0L) {
            glfwDestroyWindow(this.window);
        }
        glfwTerminate();

        var callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }

        LOGGER.info("ShilpLoka shutdown complete.");
    }

    public shilploka.ui.CodexScreen getCodexScreen() {
        return codexScreen;
    }

    public shilploka.ui.BarterScreen getBarterScreen() {
        return barterScreen;
    }

    public shilploka.ui.KaushalScreen getKaushalScreen() {
        return kaushalScreen;
    }

    public shilploka.world.dimension.DimensionManager getDimensionManager() {
        return dimensionManager;
    }

    public shilploka.audio.SoundEngine getSoundEngine() {
        return soundEngine;
    }

    public shilploka.audio.MusicManager getMusicManager() {
        return musicManager;
    }

    public shilploka.event.FestivalManager getFestivalManager() {
        return festivalManager;
    }

    public shilploka.ui.SettingsScreen getSettingsScreen() {
        return settingsScreen;
    }

    public static void main(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if (arg.startsWith("--test-ticks=")) {
                    System.setProperty("shilploka.testTicks", arg.substring("--test-ticks=".length()));
                } else if (arg.startsWith("-Dshilploka.testTicks=")) {
                    System.setProperty("shilploka.testTicks", arg.substring("-Dshilploka.testTicks=".length()));
                }
            }
        }
        new Game().run();
    }
}