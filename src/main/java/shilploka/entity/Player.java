package shilploka.entity;

import org.joml.Vector3d;
import org.joml.Vector3f;
import shilploka.render.Camera;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Represents the first-person player avatar in the 3D voxel sandbox.
 * Governs physics simulation (gravity, ground friction, velocity decay, terminal velocity),
 * continuous swept-AABB collision resolution with voxel geometry, sneaking ledge-fall prevention,
 * and creative flight mechanics.
 *
 * WHAT:
 * Encapsulates position, velocity, dimensions (0.6m wide, 1.8m tall, 1.62m eye height),
 * collision bounding box, movement flags, and tick updates.
 *
 * USED FOR:
 * Coordinates with Camera for first-person rendering and with Game for input dispatching.
 *
 * WHERE NEXT:
 * Camera view matrix is synchronized to Player eye position (x, y + eyeHeight, z).
 */
public class Player {

    public static final double WIDTH = 0.6;
    public static final double HEIGHT = 1.8;
    public static final double EYE_HEIGHT = 1.62;

    public static final double GRAVITY = 32.0;            // m/s^2 downward acceleration
    public static final double TERMINAL_VELOCITY = -78.4;  // Max downward speed
    public static final double JUMP_VELOCITY = 8.5;       // Initial upward impulse
    public static final double WALK_ACCEL = 40.0;         // Horizontal acceleration
    public static final double FLY_SPEED = 12.0;          // Creative fly velocity
    public static final double GROUND_FRICTION = 0.60;    // Velocity retained per tick on ground
    public static final double AIR_RESISTANCE = 0.91;     // Velocity retained per tick in air

    private final Vector3d position = new Vector3d();
    private final Vector3d velocity = new Vector3d();
    private AABB boundingBox;

    private boolean onGround = false;
    private boolean flying = true; // Creative flight enabled by default in Phase P4
    private boolean sneaking = false;
    private boolean sprinting = false;

    private final PlayerVitals vitals = new PlayerVitals();
    private final PlayerInventory inventory = new PlayerInventory();

    public Player(double startX, double startY, double startZ) {
        this.position.set(startX, startY, startZ);
        this.boundingBox = createAABB(startX, startY, startZ);
    }

    private static AABB createAABB(double x, double y, double z) {
        double hw = WIDTH / 2.0;
        return new AABB(x - hw, y, z - hw, x + hw, y + HEIGHT, z + hw);
    }

    /**
     * WHY:
     * Advances player physics simulation by delta time (seconds).
     *
     * WHAT:
     * Processes input movement vectors aligned with camera yaw, applies gravity and damping,
     * performs swept-AABB collision against surrounding terrain, and updates position.
     */
    public void tick(boolean forward, boolean backward, boolean left, boolean right,
                     boolean jump, boolean sneakKey, boolean sprintKey,
                     float cameraYaw, float cameraPitch, double dt, BlockGetter world) {
        this.sneaking = sneakKey;
        this.sprinting = sprintKey;

        // Calculate horizontal direction vectors from camera yaw (in radians)
        double yawRad = Math.toRadians(cameraYaw);
        double sinYaw = Math.sin(yawRad);
        double cosYaw = Math.cos(yawRad);

        // Forward vector on horizontal plane
        double fwdX = sinYaw;
        double fwdZ = -cosYaw;

        // Right vector (perpendicular to forward)
        double rightX = cosYaw;
        double rightZ = sinYaw;

        // Compute input direction
        double inputX = 0.0;
        double inputZ = 0.0;
        if (forward) { inputX += fwdX; inputZ += fwdZ; }
        if (backward) { inputX -= fwdX; inputZ -= fwdZ; }
        if (left) { inputX -= rightX; inputZ -= rightZ; }
        if (right) { inputX += rightX; inputZ += rightZ; }

        // Normalize input direction
        double inputLen = Math.sqrt(inputX * inputX + inputZ * inputZ);
        if (inputLen > 1e-4) {
            inputX /= inputLen;
            inputZ /= inputLen;
        }

        if (flying) {
            // --- CREATIVE 6-DOF FLIGHT PHYSICS ---
            double speed = FLY_SPEED * (sprinting ? 2.0 : 1.0);
            velocity.x = inputX * speed;
            velocity.z = inputZ * speed;

            // Vertical flight input
            double flyY = 0.0;
            if (jump) flyY += speed;
            if (sneaking) flyY -= speed;
            velocity.y = flyY;

            // Apply direct movement without collision clipping
            position.x += velocity.x * dt;
            position.y += velocity.y * dt;
            position.z += velocity.z * dt;
            boundingBox = createAABB(position.x, position.y, position.z);
            onGround = false;
            return;
        }

        // --- GROUND, FLUID & GRAVITY SURVIVAL PHYSICS ---
        boolean inWater = false;
        boolean climbing = false;
        if (world != null) {
            int feetX = (int) Math.floor(position.x);
            int feetY = (int) Math.floor(position.y);
            int feetZ = (int) Math.floor(position.z);
            int bodyY = (int) Math.floor(position.y + 0.8);
            int feetBlock = world.getBlock(feetX, feetY, feetZ);
            int bodyBlock = world.getBlock(feetX, bodyY, feetZ);
            inWater = (feetBlock == BlockIds.WATER || feetBlock == BlockIds.WATER_FLOWING ||
                       bodyBlock == BlockIds.WATER || bodyBlock == BlockIds.WATER_FLOWING);
            climbing = (feetBlock == BlockIds.BAANS || bodyBlock == BlockIds.BAANS);
        }

        if (climbing) {
            // Bamboo / scaffolding climbing physics
            double climbSpeed = WALK_ACCEL * 0.5;
            velocity.x += inputX * climbSpeed * dt;
            velocity.z += inputZ * climbSpeed * dt;
            if (jump || forward) {
                velocity.y = 3.0; // Ascend
            } else if (sneaking) {
                velocity.y = 0.0; // Cling in place
            } else {
                velocity.y = -2.0; // Gentle descent
            }
            vitals.resetFallDistance();
        } else if (inWater) {
            // Fluid swimming & buoyancy physics
            double swimSpeed = WALK_ACCEL * (sprinting ? 0.9 : 0.65);
            velocity.x += inputX * swimSpeed * dt;
            velocity.z += inputZ * swimSpeed * dt;

            if (jump) {
                velocity.y = Math.min(3.8, velocity.y + 14.0 * dt); // Swim upwards
            } else if (sneaking) {
                velocity.y = Math.max(-4.0, velocity.y - 10.0 * dt); // Dive downwards
            } else {
                velocity.y = Math.max(-2.5, velocity.y - 6.0 * dt); // Gentle buoyancy sinking
            }
            vitals.resetFallDistance(); // Water cushions fall
        } else {
            // Standard ground & air physics
            double moveSpeed = WALK_ACCEL * (sprinting ? 1.3 : (sneaking ? 0.35 : 1.0));
            velocity.x += inputX * moveSpeed * dt;
            velocity.z += inputZ * moveSpeed * dt;

            // Jump impulse
            if (jump && onGround) {
                velocity.y = JUMP_VELOCITY;
                onGround = false;
            }

            // Apply downward gravitational acceleration
            velocity.y = Math.max(TERMINAL_VELOCITY, velocity.y - GRAVITY * dt);
        }

        // Desired displacement for this frame
        double xa = velocity.x * dt;
        double ya = velocity.y * dt;
        double za = velocity.z * dt;

        // Sneaking edge prevention: prevent walking off drops
        if (sneaking && onGround && !inWater && !climbing) {
            xa = preventLedgeFallX(xa, world);
            za = preventLedgeFallZ(za, world);
        }

        // Execute swept AABB collision against world voxels
        move(xa, ya, za, world);

        // Apply friction damping
        double damping;
        if (inWater) {
            damping = Math.pow(0.80, dt * 20.0); // Viscous water drag
        } else if (climbing) {
            damping = Math.pow(0.60, dt * 20.0);
        } else {
            damping = onGround ? Math.pow(GROUND_FRICTION, dt * 20.0) : Math.pow(AIR_RESISTANCE, dt * 20.0);
        }
        velocity.x *= damping;
        velocity.z *= damping;

        // Survival vitals simulation when not flying
        if (!flying && world != null) {
            if (!onGround && velocity.y < 0.0) {
                vitals.accumulateFallDistance((float) (-velocity.y * dt));
            }
            int eyeBx = (int) Math.floor(getEyeX());
            int eyeBy = (int) Math.floor(getEyeY());
            int eyeBz = (int) Math.floor(getEyeZ());
            int headBlock = world.getBlock(eyeBx, eyeBy, eyeBz);
            boolean headInWater = (headBlock == BlockIds.WATER || headBlock == BlockIds.WATER_FLOWING);
            boolean headInSolid = BlockIds.isSolid(headBlock);
            vitals.tick(headInWater, headInSolid, onGround, sprinting);
        }
    }

    /**
     * Continuous swept-AABB collision resolution against surrounding terrain.
     */
    public void move(double xa, double ya, double za, BlockGetter world) {
        if (world == null) {
            position.x += xa;
            position.y += ya;
            position.z += za;
            boundingBox = createAABB(position.x, position.y, position.z);
            return;
        }

        double xaOrg = xa;
        double yaOrg = ya;
        double zaOrg = za;

        // Gather all solid block bounding boxes within expanded swept region
        AABB sweepBox = boundingBox.expand(xa, ya, za);
        List<AABB> obstacles = getSurroundingObstacles(sweepBox, world);

        // 1. Resolve Y axis first (determines ground / ceiling landing)
        for (AABB obstacle : obstacles) {
            ya = boundingBox.clipYCollide(obstacle, ya);
        }
        boundingBox = boundingBox.offset(0.0, ya, 0.0);

        // 2. Resolve X axis (wall sliding)
        for (AABB obstacle : obstacles) {
            xa = boundingBox.clipXCollide(obstacle, xa);
        }
        boundingBox = boundingBox.offset(xa, 0.0, 0.0);

        // 3. Resolve Z axis (wall sliding)
        for (AABB obstacle : obstacles) {
            za = boundingBox.clipZCollide(obstacle, za);
        }
        boundingBox = boundingBox.offset(0.0, 0.0, za);

        // Update player coordinates from resolved bounding box
        position.x = (boundingBox.minX() + boundingBox.maxX()) / 2.0;
        position.y = boundingBox.minY();
        position.z = (boundingBox.minZ() + boundingBox.maxZ()) / 2.0;

        // Update ground contact state
        this.onGround = (yaOrg < 0.0 && ya != yaOrg);

        // Kill velocity on collided axes
        if (xa != xaOrg) velocity.x = 0.0;
        if (ya != yaOrg) velocity.y = 0.0;
        if (za != zaOrg) velocity.z = 0.0;
    }

    private List<AABB> getSurroundingObstacles(AABB region, BlockGetter world) {
        List<AABB> list = new ArrayList<>();
        int minX = (int) Math.floor(region.minX());
        int maxX = (int) Math.floor(region.maxX());
        int minY = Math.max(0, (int) Math.floor(region.minY()));
        int maxY = Math.min(319, (int) Math.floor(region.maxY()));
        int minZ = (int) Math.floor(region.minZ());
        int maxZ = (int) Math.floor(region.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int block = world.getBlock(x, y, z);
                    if (BlockIds.isSolid(block)) {
                        list.add(AABB.fromBlock(x, y, z));
                    }
                }
            }
        }
        return list;
    }

    private double preventLedgeFallX(double xa, BlockGetter world) {
        if (xa == 0.0) return 0.0;
        AABB testBox = boundingBox.offset(xa, -0.1, 0.0);
        if (getSurroundingObstacles(testBox, world).isEmpty()) {
            return 0.0; // Would fall into air, block motion
        }
        return xa;
    }

    private double preventLedgeFallZ(double za, BlockGetter world) {
        if (za == 0.0) return 0.0;
        AABB testBox = boundingBox.offset(0.0, -0.1, za);
        if (getSurroundingObstacles(testBox, world).isEmpty()) {
            return 0.0; // Would fall into air, block motion
        }
        return za;
    }

    public void toggleFly() {
        this.flying = !this.flying;
        if (this.flying) {
            this.velocity.set(0.0, 0.0, 0.0);
        }
    }

    public Vector3d getPosition() { return position; }
    public Vector3d getVelocity() { return velocity; }
    public AABB getBoundingBox() { return boundingBox; }
    public boolean isOnGround() { return onGround; }
    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }
    public boolean isSneaking() { return sneaking; }
    public boolean isSprinting() { return sprinting; }

    public void setPosition(double x, double y, double z) {
        this.position.set(x, y, z);
        this.boundingBox = createAABB(x, y, z);
        this.velocity.set(0.0, 0.0, 0.0);
    }

    public double getEyeX() { return position.x; }
    public double getEyeY() { return position.y + EYE_HEIGHT; }
    public double getEyeZ() { return position.z; }

    public PlayerVitals getVitals() { return vitals; }
    public PlayerInventory getInventory() { return inventory; }
}
