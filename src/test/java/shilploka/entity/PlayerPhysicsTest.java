package shilploka.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockGetter;
import shilploka.world.BlockIds;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies player entity physics, gravity acceleration, ground contact landing,
 * jumping impulses, creative flight toggling, and sneak ledge-fall prevention.
 *
 * WHAT:
 * Tests numerical simulation stability across multiple physics frames (dt=0.05s).
 *
 * USED FOR:
 * Automated CI verification of player movement physics.
 */
class PlayerPhysicsTest {

    @Test
    @DisplayName("Falling player accumulates gravity and lands firmly on ground block (Y=1.0)")
    void testGravityAndGroundCollision() {
        // Flat ground layer at Y=0 (block bounds Y: 0.0 to 1.0)
        Map<String, Integer> ground = new HashMap<>();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                ground.put(x + ",0," + z, BlockIds.ALLUVIAL_SOIL);
            }
        }
        BlockGetter world = (x, y, z) -> ground.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        Player player = new Player(0.0, 5.0, 0.0);
        player.setFlying(false); // Enable survival gravity

        // Simulate 20 ticks (1 second) of freefall
        double dt = 0.05;
        for (int i = 0; i < 20; i++) {
            player.tick(false, false, false, false, false, false, false, 0.0f, 0.0f, dt, world);
        }

        // Player must have landed on top of block (Y = 1.0) and be grounded
        assertEquals(1.0, player.getPosition().y, 1e-4, "Player must land on top of surface block at Y=1.0");
        assertTrue(player.isOnGround(), "Player onGround state must be true after landing");
        assertEquals(0.0, player.getVelocity().y, 1e-4, "Vertical velocity must be zeroed upon landing");
    }

    @Test
    @DisplayName("Grounded player receives jump impulse and becomes airborne")
    void testJump() {
        Map<String, Integer> ground = new HashMap<>();
        ground.put("0,0,0", BlockIds.STONE);
        BlockGetter world = (x, y, z) -> ground.getOrDefault(x + "," + y + "," + z, BlockIds.AIR);

        Player player = new Player(0.0, 1.0, 0.0);
        player.setFlying(false);

        // Ground player first
        player.tick(false, false, false, false, false, false, false, 0.0f, 0.0f, 0.05, world);
        assertTrue(player.isOnGround());

        // Jump input
        player.tick(false, false, false, false, true, false, false, 0.0f, 0.0f, 0.05, world);

        assertTrue(player.getVelocity().y > 0.0, "Jumping must impart positive upward velocity");
        assertFalse(player.isOnGround(), "Jumping player must leave ground contact");
    }

    @Test
    @DisplayName("Flight toggle enables 6-DOF movement without gravity depletion")
    void testFlightToggle() {
        Player player = new Player(0.0, 50.0, 0.0);
        player.setFlying(true);

        BlockGetter emptyWorld = (x, y, z) -> BlockIds.AIR;

        // Tick in air without input
        player.tick(false, false, false, false, false, false, false, 0.0f, 0.0f, 0.1, emptyWorld);

        // In flight mode without input, position should remain stationary
        assertEquals(50.0, player.getPosition().y, 1e-4, "Flying player must hover without falling");

        // Toggle flight off
        player.toggleFly();
        assertFalse(player.isFlying());

        // Tick in air with gravity
        player.tick(false, false, false, false, false, false, false, 0.0f, 0.0f, 0.1, emptyWorld);
        assertTrue(player.getPosition().y < 50.0, "Survival player must fall under gravity");
    }

    @Test
    @DisplayName("Water fluid immersion applies buoyant cushioning and upward swim impulse on jump")
    void testWaterSwimmingPhysics() {
        // Water pool from Y=0 to Y=10
        BlockGetter waterWorld = (x, y, z) -> (y >= 0 && y <= 10) ? BlockIds.WATER : BlockIds.AIR;

        Player player = new Player(0.0, 5.0, 0.0);
        player.setFlying(false);

        // Tick submerged in water with jump key held (swimming up)
        for (int i = 0; i < 5; i++) {
            player.tick(false, false, false, false, true, false, false, 0.0f, 0.0f, 0.05, waterWorld);
        }

        assertTrue(player.getVelocity().y > 0.0, "Swimming with jump must yield positive upward velocity");
        assertTrue(player.getPosition().y > 5.0, "Submerged player jumping must swim upwards");
        assertEquals(20.0f, player.getVitals().getHealth(), 1e-4, "Player is alive with full health");
    }

    @Test
    @DisplayName("Bamboo culm blocks allow vertical climbing on jump/forward and clinging when sneaking")
    void testBambooClimbingPhysics() {
        // Bamboo pole at X=0, Z=0 from Y=0 to Y=20
        BlockGetter bambooWorld = (x, y, z) -> (x == 0 && z == 0 && y >= 0 && y <= 20) ? BlockIds.BAANS : BlockIds.AIR;

        Player player = new Player(0.5, 5.0, 0.5);
        player.setFlying(false);

        // Climb up with jump
        player.tick(false, false, false, false, true, false, false, 0.0f, 0.0f, 0.05, bambooWorld);
        assertEquals(3.0, player.getVelocity().y, 1e-4, "Climbing with jump must set ascent velocity to 3.0 m/s");

        // Cling in place with sneak
        player.tick(false, false, false, false, false, true, false, 0.0f, 0.0f, 0.05, bambooWorld);
        assertEquals(0.0, player.getVelocity().y, 1e-4, "Clinging with sneak must freeze vertical velocity to 0.0 m/s");
    }

    @Test
    @DisplayName("Sneaking player cannot walk off ledge into air")
    void testSneakLedgeFallPrevention() {
        // 1 solid block at (0, 0, 0)
        BlockGetter oneBlockWorld = (x, y, z) -> (x == 0 && y == 0 && z == 0) ? BlockIds.STONE : BlockIds.AIR;

        Player player = new Player(0.5, 1.0, 0.5);
        player.setFlying(false);

        // Ground player first
        player.tick(false, false, false, false, false, false, false, 0.0f, 0.0f, 0.05, oneBlockWorld);
        assertTrue(player.isOnGround());

        // Sneak forward off the ledge towards positive X
        for (int i = 0; i < 20; i++) {
            // forward=true, sneak=true, yaw=270 (faces +X)
            player.tick(true, false, false, false, false, true, false, 270.0f, 0.0f, 0.05, oneBlockWorld);
        }

        // Must remain on the block surface and grounded, not falling off
        assertTrue(player.isOnGround(), "Sneaking player must stay grounded on block");
        assertTrue(player.getPosition().x < 1.0, "Player must not step off the edge of block at X=1.0");
    }
}
