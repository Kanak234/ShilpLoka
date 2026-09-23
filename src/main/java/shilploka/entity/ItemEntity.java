package shilploka.entity;

import org.joml.Vector3d;
import shilploka.item.ItemStack;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

/**
 * WHY:
 * Broken blocks and dropped items exist in 3D voxel space as physical item entities
 * that can be attracted by player magnetism and picked up into the inventory.
 *
 * WHAT:
 * Floating 3D item entity with gravity, ground collision, rotational bobbing,
 * magnet attraction toward the player avatar, and automatic 5-minute despawn timers.
 *
 * USED FOR:
 * Spawned when a player mines a block in Survival mode or presses 'Q' to toss an item.
 *
 * WHERE NEXT:
 * Ticked by Game/World entity pipeline and picked up into PlayerInventory.
 */
public class ItemEntity {

    public static final double GRAVITY = 18.0; // m/s^2
    public static final double TERMINAL_VELOCITY = -20.0;
    public static final double MAGNET_RADIUS = 2.0; // Magnet attraction distance in blocks
    public static final double PICKUP_RADIUS = 0.8; // Distance to absorb item
    public static final int DESPAWN_TICKS = 6000;   // 5 minutes at 20 ticks/sec

    private final Vector3d position = new Vector3d();
    private final Vector3d velocity = new Vector3d();
    private ItemStack itemStack;

    private int age = 0;
    private int pickupDelay = 30; // 1.5 seconds delay before player can re-pickup
    private boolean onGround = false;
    private boolean inWater = false;
    private boolean dead = false;
    private float rotationAngle = 0.0f;

    public ItemEntity(double x, double y, double z, ItemStack stack) {
        this.position.set(x, y, z);
        this.itemStack = (stack != null) ? stack : ItemStack.EMPTY;
        // Random slight horizontal toss
        this.velocity.x = (Math.random() - 0.5) * 2.0;
        this.velocity.y = 2.5 + Math.random() * 1.5;
        this.velocity.z = (Math.random() - 0.5) * 2.0;
    }

    public ItemEntity(double x, double y, double z, ItemStack stack, double vx, double vy, double vz) {
        this.position.set(x, y, z);
        this.itemStack = (stack != null) ? stack : ItemStack.EMPTY;
        this.velocity.set(vx, vy, vz);
    }

    /**
     * WHY:
     * Advances item physics, gravity, ground contact, player magnet attraction, and despawn timers.
     */
    public void tick(double dt, BlockGetter world, Player player) {
        if (dead || itemStack.isEmpty()) {
            dead = true;
            return;
        }

        age++;
        if (age >= DESPAWN_TICKS) {
            dead = true;
            return;
        }

        if (pickupDelay > 0) {
            pickupDelay--;
        }

        // 1. Player magnet attraction
        if (player != null && pickupDelay <= 0) {
            double dx = player.getPosition().x - position.x;
            double dy = (player.getPosition().y + Player.HEIGHT * 0.5) - position.y;
            double dz = player.getPosition().z - position.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= MAGNET_RADIUS * MAGNET_RADIUS) {
                double dist = Math.sqrt(distSq);
                if (dist > 1e-4) {
                    double speed = 6.0;
                    velocity.x = (dx / dist) * speed;
                    velocity.y = (dy / dist) * speed;
                    velocity.z = (dz / dist) * speed;
                }

                // Pickup absorption
                if (dist <= PICKUP_RADIUS) {
                    // Handled externally in collision pass
                }
            }
        }

        // 2. Check fluid immersion (water buoyancy)
        inWater = false;
        if (world != null) {
            int curBx = (int) Math.floor(position.x);
            int curBy = (int) Math.floor(position.y);
            int curBz = (int) Math.floor(position.z);
            int blockAtPos = world.getBlock(curBx, curBy, curBz);
            inWater = (blockAtPos == BlockIds.WATER || blockAtPos == BlockIds.WATER_FLOWING);
        }

        // Apply gravity or buoyant upward force
        if (inWater) {
            // Fluid buoyancy: items float towards the surface with viscous water drag
            velocity.y = Math.min(1.8, velocity.y + 14.0 * dt);
            double fluidDamping = Math.pow(0.80, dt * 20.0);
            velocity.x *= fluidDamping;
            velocity.y *= fluidDamping;
            velocity.z *= fluidDamping;
            onGround = false;
        } else if (!onGround) {
            velocity.y = Math.max(TERMINAL_VELOCITY, velocity.y - GRAVITY * dt);
        } else {
            velocity.x *= 0.85;
            velocity.z *= 0.85;
        }

        // 3. Collision resolution against solid blocks
        double nextX = position.x + velocity.x * dt;
        double nextY = position.y + velocity.y * dt;
        double nextZ = position.z + velocity.z * dt;

        int blockX = (int) Math.floor(nextX);
        int blockY = (int) Math.floor(nextY);
        int blockZ = (int) Math.floor(nextZ);

        if (world != null && BlockIds.isSolid(world.getBlock(blockX, blockY, blockZ))) {
            if (velocity.y < 0.0) {
                position.x = nextX;
                position.y = blockY + 1.0;
                position.z = nextZ;
                velocity.y = 0.0;
                onGround = true;
            } else if (velocity.y > 0.0) {
                position.x = nextX;
                position.y = blockY - 0.1;
                position.z = nextZ;
                velocity.y = 0.0;
            } else {
                position.set(nextX, nextY, nextZ);
            }
        } else {
            position.set(nextX, nextY, nextZ);
            if (onGround && world != null) {
                // Verify whether support block underneath still exists
                int belowY = (int) Math.floor(position.y - 0.1);
                if (!BlockIds.isSolid(world.getBlock(blockX, belowY, blockZ))) {
                    onGround = false;
                }
            }
        }

        // 4. Update visual rotation
        rotationAngle = (float) ((rotationAngle + 90.0f * dt) % 360.0f);
    }

    public Vector3d getPosition() { return position; }
    public Vector3d getVelocity() { return velocity; }
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }
    public boolean isDead() { return dead; }
    public void setDead(boolean dead) { this.dead = dead; }
    public boolean isOnGround() { return onGround; }
    public boolean isInWater() { return inWater; }
    public int getPickupDelay() { return pickupDelay; }
    public void setPickupDelay(int pickupDelay) { this.pickupDelay = pickupDelay; }
    public float getRotationAngle() { return rotationAngle; }
}
