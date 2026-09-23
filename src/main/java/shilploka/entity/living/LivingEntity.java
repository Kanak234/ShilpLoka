package shilploka.entity.living;

import org.joml.Vector3d;
import org.joml.Vector3i;
import shilploka.ai.AIGoal;
import shilploka.ai.Pathfinder;
import shilploka.entity.AABB;
import shilploka.entity.Player;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * WHY:
 * Base class for all living creatures, wild fauna, and nocturnal folklore entities in ShilpLoka.
 *
 * WHAT:
 * Encapsulates continuous 3D physics (gravity, collision clipping, step-up), health management,
 * path navigation, and modular AI goal execution.
 *
 * USED FOR:
 * Extended by passive fauna (Gau, Hathi, Mor, Bhains) and hostile entities (Baagh, Naag, Vetaal, Daaku).
 */
public abstract class LivingEntity {

    protected double x, y, z;
    protected double vx, vy, vz;
    protected float yaw;
    protected float pitch;

    protected float width;
    protected float height;
    protected AABB boundingBox;
    protected boolean onGround = false;
    protected boolean inWater = false;

    protected float health;
    protected float maxHealth;
    protected float moveSpeed = 3.0f; // Blocks per second
    protected boolean dead = false;
    protected int hurtTime = 0; // Flash red / invulnerability ticks

    protected final List<AIGoal> goals = new ArrayList<>();
    protected List<Vector3i> currentPath = null;
    protected int currentPathIndex = 0;
    protected double pathTargetX, pathTargetY, pathTargetZ;

    public LivingEntity(double x, double y, double z, float width, float height, float maxHealth) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        updateBoundingBox();
    }

    public void addGoal(AIGoal goal) {
        this.goals.add(goal);
    }

    public void updateBoundingBox() {
        double halfW = width / 2.0;
        this.boundingBox = new AABB(x - halfW, y, z - halfW, x + halfW, y + height, z + halfW);
    }

    public void tick(float dt, ChunkManager chunkManager, Player player) {
        if (dead) return;

        if (hurtTime > 0) {
            hurtTime--;
        }

        // 1. Execute active AI goals
        for (AIGoal goal : goals) {
            if (goal.shouldExecute()) {
                goal.tick();
            }
        }

        // 2. Follow current navigation path
        if (currentPath != null && currentPathIndex < currentPath.size()) {
            Vector3i targetNode = currentPath.get(currentPathIndex);
            double dx = (targetNode.x + 0.5) - x;
            double dz = (targetNode.z + 0.5) - z;
            double distSq = dx * dx + dz * dz;

            if (distSq < 0.25) {
                currentPathIndex++;
            } else {
                double dist = Math.sqrt(distSq);
                double dirX = dx / dist;
                double dirZ = dz / dist;
                this.vx = dirX * moveSpeed;
                this.vz = dirZ * moveSpeed;
                this.yaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));

                // Jump if next node is elevated by 1 block
                if (targetNode.y > y + 0.5 && onGround) {
                    this.vy = 5.5; // Jump impulse
                }
            }
        } else {
            // Apply horizontal friction when not navigating
            this.vx *= 0.75;
            this.vz *= 0.75;
            if (Math.abs(vx) < 0.01) vx = 0;
            if (Math.abs(vz) < 0.01) vz = 0;
        }

        // 3. Fluid immersion detection and buoyancy
        this.inWater = false;
        if (chunkManager != null) {
            int bx = (int) Math.floor(x);
            int by = (int) Math.floor(y + height * 0.4);
            int bz = (int) Math.floor(z);
            int block = chunkManager.getBlock(bx, by, bz);
            this.inWater = (block == BlockIds.WATER || block == BlockIds.WATER_FLOWING);
        }

        if (inWater) {
            // Fluid buoyancy: float toward the surface with viscous water drag
            this.vy = Math.min(2.5, this.vy + 14.0 * dt);
            this.vx *= Math.pow(0.80, dt * 20.0);
            this.vz *= Math.pow(0.80, dt * 20.0);
        } else {
            // Standard gravitational acceleration
            this.vy -= 22.0 * dt;
            if (this.vy < -35.0) this.vy = -35.0; // Terminal velocity
        }

        // 4. Perform Swept-AABB terrain collision
        moveWithCollision(dt, chunkManager);
    }

    protected void moveWithCollision(float dt, ChunkManager chunkManager) {
        double mx = vx * dt;
        double my = vy * dt;
        double mz = vz * dt;

        AABB broadphase = boundingBox.expand(mx, my, mz);
        List<AABB> obstacles = getCollidingBlocks(broadphase, chunkManager);

        // 1. Y collision (ground contact / ceiling collision)
        double originalMy = my;
        for (AABB box : obstacles) {
            my = boundingBox.clipYCollide(box, my);
        }
        this.boundingBox = boundingBox.offset(0, my, 0);
        this.onGround = (originalMy < 0 && my != originalMy);
        if (my != originalMy) {
            this.vy = 0;
        }

        // 2. X collision with 1-block auto step-up
        double originalMx = mx;
        for (AABB box : obstacles) {
            mx = boundingBox.clipXCollide(box, mx);
        }
        this.boundingBox = boundingBox.offset(mx, 0, 0);
        if (mx != originalMx && onGround && !inWater) {
            // Step-up attempt
            this.vy = 4.5;
        }

        // 3. Z collision with 1-block auto step-up
        double originalMz = mz;
        for (AABB box : obstacles) {
            mz = boundingBox.clipZCollide(box, mz);
        }
        this.boundingBox = boundingBox.offset(0, 0, mz);
        if (mz != originalMz && onGround && !inWater) {
            this.vy = 4.5;
        }

        this.x = (boundingBox.minX() + boundingBox.maxX()) / 2.0;
        this.y = boundingBox.minY();
        this.z = (boundingBox.minZ() + boundingBox.maxZ()) / 2.0;
    }

    protected List<AABB> getCollidingBlocks(AABB box, ChunkManager chunkManager) {
        List<AABB> blocks = new ArrayList<>();
        if (chunkManager == null) return blocks;
        int minX = (int) Math.floor(box.minX());
        int maxX = (int) Math.ceil(box.maxX());
        int minY = Math.max(0, (int) Math.floor(box.minY()));
        int maxY = Math.min(shilploka.world.chunk.ChunkColumn.CHUNK_HEIGHT, (int) Math.ceil(box.maxY()));
        int minZ = (int) Math.floor(box.minZ());
        int maxZ = (int) Math.ceil(box.maxZ());

        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    int block = chunkManager.getBlock(bx, by, bz);
                    if (BlockIds.isSolid(block)) {
                        blocks.add(AABB.fromBlock(bx, by, bz));
                    }
                }
            }
        }
        return blocks;
    }

    public void pathTo(double tx, double ty, double tz, ChunkManager chunkManager) {
        Vector3i start = new Vector3i((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        Vector3i target = new Vector3i((int) Math.floor(tx), (int) Math.floor(ty), (int) Math.floor(tz));
        this.currentPath = Pathfinder.findPath(start, target, chunkManager, 100);
        this.currentPathIndex = 0;
        this.pathTargetX = tx;
        this.pathTargetY = ty;
        this.pathTargetZ = tz;
    }

    public void damage(float amount) {
        if (dead || hurtTime > 0) return;
        this.health -= amount;
        this.hurtTime = 10; // Half second invulnerability
        if (this.health <= 0) {
            this.health = 0;
            this.dead = true;
        }
    }

    public void resetHurtTime() {
        this.hurtTime = 0;
    }

    public abstract String getEntityName();
    public abstract String getHindiName();
    public abstract List<ItemStack> getDeathDrops();

    // Getters and Setters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public Vector3d getPosition() { return new Vector3d(x, y, z); }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
    public boolean isDead() { return dead; }
    public boolean isOnGround() { return onGround; }
    public boolean isInWater() { return inWater; }
    public AABB getBoundingBox() { return boundingBox; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getMoveSpeed() { return moveSpeed; }
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateBoundingBox();
    }
}
