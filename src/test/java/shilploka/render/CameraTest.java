package shilploka.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying camera matrix generation, 3D movement math,
 * pitch clamping, and authentic Indian cardinal direction calculation.
 */
class CameraTest {

    @Test
    @DisplayName("Camera initialization sets valid position and normalized front vector")
    void testCameraInitialization() {
        Camera camera = new Camera(10.0f, 20.0f, 30.0f);

        Vector3f pos = camera.getPosition();
        assertEquals(10.0f, pos.x, 0.001f);
        assertEquals(20.0f, pos.y, 0.001f);
        assertEquals(30.0f, pos.z, 0.001f);

        assertEquals(-90.0f, camera.getYaw(), 0.001f);
        assertEquals(0.0f, camera.getPitch(), 0.001f);
    }

    @Test
    @DisplayName("Mouse look clamps vertical pitch between -89 and +89 degrees")
    void testPitchClamping() {
        Camera camera = new Camera(0, 0, 0);

        // Extreme downward look
        camera.processMouseLook(0, 1000.0f);
        assertTrue(camera.getPitch() >= -89.0f, "Pitch must not exceed -89 degrees downward");

        // Extreme upward look
        camera.processMouseLook(0, -2000.0f);
        assertTrue(camera.getPitch() <= 89.0f, "Pitch must not exceed +89 degrees upward");
    }

    @Test
    @DisplayName("Cardinal direction calculation correctly maps to Indian names")
    void testCardinalDirectionMapping() {
        Camera camera = new Camera(0, 0, 0);

        // Initial yaw is -90.0 degrees -> corresponds to 270 degrees -> Uttar (North) [-Z]
        String north = camera.getCardinalDirection();
        assertTrue(north.contains("Uttar"), "Heading -90 deg (-Z) must indicate Uttar (North), got: " + north);

        // Rotate 90 degrees right to 0 degrees -> Purab (East) [+X]
        camera.processMouseLook(90.0f / 0.12f, 0);
        String east = camera.getCardinalDirection();
        assertTrue(east.contains("Purab"), "Heading 0 deg (+X) must indicate Purab (East), got: " + east);
    }

    @Test
    @DisplayName("View and Projection matrices are non-null and invertible")
    void testMatrixGeneration() {
        Camera camera = new Camera(5.0f, 10.0f, 15.0f);
        Matrix4f view = camera.getViewMatrix();
        assertNotNull(view);

        Matrix4f proj = camera.getProjectionMatrix(16.0f / 9.0f);
        assertNotNull(proj);

        Matrix4f viewProj = camera.getViewProjectionMatrix(16.0f / 9.0f);
        assertNotNull(viewProj);

        // Ensure determinant is non-zero (matrix is invertible)
        assertNotEquals(0.0f, viewProj.determinant(), 0.0001f);
    }

    @Test
    @DisplayName("Flying movement alters position correctly")
    void testFlyingMovement() {
        Camera camera = new Camera(0, 0, 0);

        // Move up by 1 second at delta = 1.0f
        camera.processFlyingMovement(false, false, false, false, true, false, false, false, 1.0f);
        assertTrue(camera.getPosition().y > 0.0f, "Y position must increase when ascending");

        // Move down
        camera.processFlyingMovement(false, false, false, false, false, true, false, false, 1.0f);
        assertEquals(0.0f, camera.getPosition().y, 0.001f);
    }
}
