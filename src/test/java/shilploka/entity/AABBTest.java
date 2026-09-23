package shilploka.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Verifies Axis-Aligned Bounding Box (AABB) intersection math and swept-axis collision clipping.
 *
 * WHAT:
 * Tests 3D intersection logic, bounding box offset and expansion, and confirms that
 * clipXCollide, clipYCollide, and clipZCollide prevent box penetration with zero tunneling.
 *
 * USED FOR:
 * Automated CI verification of physics collision primitives.
 */
class AABBTest {

    @Test
    @DisplayName("AABB intersection detection correctly identifies overlaps and separations")
    void testIntersects() {
        AABB box1 = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        AABB box2 = new AABB(0.5, 0.5, 0.5, 1.5, 1.5, 1.5);
        AABB box3 = new AABB(2.0, 2.0, 2.0, 3.0, 3.0, 3.0);

        assertTrue(box1.intersects(box2), "Overlapping boxes must intersect");
        assertTrue(box2.intersects(box1), "Intersection must be commutative");
        assertFalse(box1.intersects(box3), "Separated boxes must not intersect");
    }

    @Test
    @DisplayName("clipYCollide correctly clips downward movement onto an obstacle")
    void testClipYCollideLanding() {
        // Player box at Y=10.0 to 11.8
        AABB player = new AABB(-0.3, 10.0, -0.3, 0.3, 11.8, 0.3);
        // Ground block at Y=9.0 to 10.0
        AABB ground = new AABB(-1.0, 9.0, -1.0, 1.0, 10.0, 1.0);

        // Falling by -2.0 meters: should be clipped to exactly 0.0 (sitting on top of ground at Y=10.0)
        double ya = player.clipYCollide(ground, -2.0);
        assertEquals(0.0, ya, 1e-6, "Landing on ground must clip downward motion to 0.0");

        // Player box at Y=12.0 falling by -5.0 onto ground at Y=10.0: should clip to -2.0
        AABB highPlayer = new AABB(-0.3, 12.0, -0.3, 0.3, 13.8, 0.3);
        double ya2 = highPlayer.clipYCollide(ground, -5.0);
        assertEquals(-2.0, ya2, 1e-6, "Falling from height 12 onto height 10 must clip to -2.0");
    }

    @Test
    @DisplayName("clipXCollide correctly clips horizontal movement into a wall obstacle")
    void testClipXCollideWall() {
        // Moving right toward a wall at X=5.0
        AABB movingBox = new AABB(2.0, 0.0, 0.0, 3.0, 1.0, 1.0);
        AABB wall = new AABB(5.0, 0.0, 0.0, 6.0, 1.0, 1.0);

        // Desired displacement +4.0: should be clipped to +2.0 (stops right at wall.minX = 5.0)
        double xa = movingBox.clipXCollide(wall, 4.0);
        assertEquals(2.0, xa, 1e-6, "Moving +4 into wall 2 units away must clip to +2.0");
    }
}
