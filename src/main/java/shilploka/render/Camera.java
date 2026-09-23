package shilploka.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * WHY:
 * 3D voxel sandbox rendering requires a 6-DOF (Degrees of Freedom) camera controller
 * to calculate view and projection transformations, facilitate free-flight inspection,
 * and provide spatial coordinates and compass headings for navigation and debugging.
 *
 * WHAT:
 * Encapsulates camera position, yaw, pitch, and orientation vectors (front, right, up).
 * Computes 4x4 View and Perspective Projection matrices using JOML without heap churn,
 * handles WASD + Space + Shift flying controls, and translates rotation angles into
 * authentic Indian cardinal compass directions (Uttar, Dakshin, Purab, Paschim).
 *
 * USED FOR:
 * Managed by Game to render the world from the player's vantage point and drive the
 * F3 debug display.
 *
 * WHERE NEXT:
 * View and projection matrices are multiplied and passed to chunk.vert via Shader.setMat4().
 */
public class Camera {

    // Position and orientation vectors
    private final Vector3f position = new Vector3f();
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

    // Euler angles (in degrees)
    private float yaw = -90.0f; // -90 degrees faces towards -Z initially
    private float pitch = 0.0f;

    // Camera settings
    private float fov = 75.0f;
    private float nearPlane = 0.1f;
    private float farPlane = 500.0f;

    // Movement speeds
    private static final float BASE_FLY_SPEED = 12.0f;   // Blocks per second
    private static final float SPRINT_FLY_SPEED = 30.0f; // Blocks per second (Ctrl pressed)
    private static final float SNEAK_FLY_SPEED = 4.0f;   // Blocks per second (Shift pressed)
    private static final float MOUSE_SENSITIVITY = 0.12f;

    // Cached reusable transformation matrices (zero heap allocation in render loop)
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewProjectionMatrix = new Matrix4f();
    private final Vector3f tempMove = new Vector3f();

    /**
     * WHY:
     * Initializes camera at designated world coordinates.
     *
     * WHAT:
     * Sets position vector and updates directional front, right, and up vectors.
     *
     * USED FOR:
     * Spawning the player into a newly loaded world.
     *
     * WHERE NEXT:
     * Ready for movement updates and view matrix calculations.
     */
    public Camera(float startX, float startY, float startZ) {
        this.position.set(startX, startY, startZ);
        updateVectors();
    }

    /**
     * WHY:
     * Recalculates front, right, and up orthonormal basis vectors when yaw or pitch changes.
     *
     * WHAT:
     * Applies trigonometric conversion from spherical Euler angles to Cartesian direction vectors,
     * normalizes them, and derives the right vector via cross-product with worldUp.
     *
     * USED FOR:
     * Called whenever mouse movement alters yaw or pitch.
     *
     * WHERE NEXT:
     * Updated front vector informs lookAt matrix creation and keyboard movement direction.
     */
    public void updateVectors() {
        float radYaw = (float) Math.toRadians(this.yaw);
        float radPitch = (float) Math.toRadians(this.pitch);

        this.front.x = (float) (Math.cos(radYaw) * Math.cos(radPitch));
        this.front.y = (float) Math.sin(radPitch);
        this.front.z = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        this.front.normalize();

        // Right = Front x WorldUp
        this.front.cross(this.worldUp, this.right).normalize();

        // Up = Right x Front
        this.right.cross(this.front, this.up).normalize();
    }

    /**
     * WHY:
     * Translates mouse delta movements into smooth camera rotational pitch and yaw.
     *
     * WHAT:
     * Multiplies mouse offsets by sensitivity, applies to yaw and pitch, clamps vertical
     * pitch between -89.0 and +89.0 degrees to prevent gimbal lock, and updates basis vectors.
     *
     * USED FOR:
     * Invoked from GLFW cursor position callback.
     *
     * WHERE NEXT:
     * Calls {@link #updateVectors()} to refresh orientation matrices.
     */
    public void processMouseLook(float dx, float dy) {
        this.yaw += dx * MOUSE_SENSITIVITY;
        this.pitch -= dy * MOUSE_SENSITIVITY;

        // Prevent camera flipping at vertical poles
        if (this.pitch > 89.0f) this.pitch = 89.0f;
        if (this.pitch < -89.0f) this.pitch = -89.0f;

        updateVectors();
    }

    /**
     * WHY:
     * Processes flying keyboard movement in 3D space during Phase P1 testing and Creative mode.
     *
     * WHAT:
     * Moves along forward, backward, left, right, upward (Space), and downward (Shift) vectors
     * scaled by delta time and sprint modifiers.
     *
     * USED FOR:
     * Called in Game.update() every simulation frame.
     *
     * WHERE NEXT:
     * Position vector is updated; subsequent getViewMatrix() calls reflect new position.
     */
    public void processFlyingMovement(boolean forward, boolean backward, boolean left, boolean right,
                                     boolean up, boolean down, boolean sprint, boolean sneak, float delta) {
        float speed = sprint ? SPRINT_FLY_SPEED : (sneak ? SNEAK_FLY_SPEED : BASE_FLY_SPEED);
        float velocity = speed * delta;

        if (forward) {
            tempMove.set(this.front).mul(velocity);
            this.position.add(tempMove);
        }
        if (backward) {
            tempMove.set(this.front).mul(velocity);
            this.position.sub(tempMove);
        }
        if (left) {
            tempMove.set(this.right).mul(velocity);
            this.position.sub(tempMove);
        }
        if (right) {
            tempMove.set(this.right).mul(velocity);
            this.position.add(tempMove);
        }
        if (up) {
            this.position.y += velocity;
        }
        if (down) {
            this.position.y -= velocity;
        }
    }

    /**
     * WHY:
     * Computes the 4x4 View Transformation Matrix without allocating heap memory.
     *
     * WHAT:
     * Evaluates lookAt(position, position + front, up) into the pre-allocated viewMatrix.
     *
     * USED FOR:
     * Chunk shader transformation.
     */
    public Matrix4f getViewMatrix() {
        tempMove.set(this.position).add(this.front);
        this.viewMatrix.identity().lookAt(this.position, tempMove, this.up);
        return this.viewMatrix;
    }

    /**
     * WHY:
     * Computes the 4x4 Perspective Projection Matrix matching the current window aspect ratio.
     *
     * WHAT:
     * Evaluates perspective(fov, aspectRatio, nearPlane, farPlane) into projectionMatrix.
     *
     * USED FOR:
     * Clip-space perspective projection in shaders.
     */
    public Matrix4f getProjectionMatrix(float aspectRatio) {
        this.projectionMatrix.identity().perspective((float) Math.toRadians(this.fov), aspectRatio, this.nearPlane, this.farPlane);
        return this.projectionMatrix;
    }

    /**
     * WHY:
     * Computes the combined View * Projection matrix into a single cached structure.
     *
     * WHAT:
     * Multiplies Projection x View in-place into viewProjectionMatrix.
     *
     * USED FOR:
     * Passed directly to shader uniform u_ViewProjection in a single call.
     */
    public Matrix4f getViewProjectionMatrix(float aspectRatio) {
        getProjectionMatrix(aspectRatio);
        getViewMatrix();
        this.projectionMatrix.mul(this.viewMatrix, this.viewProjectionMatrix);
        return this.viewProjectionMatrix;
    }

    /**
     * WHY:
     * Translates horizontal yaw angle into authentic Indian cardinal compass directions
     * (Uttar, Dakshin, Purab, Paschim) for the HUD compass strip and F3 overlay.
     *
     * WHAT:
     * Normalizes yaw to [0, 360) degrees and maps into 8 directional sectors.
     *
     * USED FOR:
     * Displayed on F3 screen and top-center compass ribbon.
     */
    public String getCardinalDirection() {
        float normalizedYaw = (this.yaw % 360.0f + 360.0f) % 360.0f;

        if (normalizedYaw >= 337.5f || normalizedYaw < 22.5f) {
            return "Purab (East) [+X]";
        } else if (normalizedYaw >= 22.5f && normalizedYaw < 67.5f) {
            return "Dakshin-Purab (South-East)";
        } else if (normalizedYaw >= 67.5f && normalizedYaw < 112.5f) {
            return "Dakshin (South) [+Z]";
        } else if (normalizedYaw >= 112.5f && normalizedYaw < 157.5f) {
            return "Dakshin-Paschim (South-West)";
        } else if (normalizedYaw >= 157.5f && normalizedYaw < 202.5f) {
            return "Paschim (West) [-X]";
        } else if (normalizedYaw >= 202.5f && normalizedYaw < 247.5f) {
            return "Uttar-Paschim (North-West)";
        } else if (normalizedYaw >= 247.5f && normalizedYaw < 292.5f) {
            return "Uttar (North) [-Z]";
        } else {
            return "Uttar-Purab (North-East)";
        }
    }

    // Getters
    public Vector3f getPosition() { return position; }
    public Vector3f getFront() { return front; }
    public Vector3f getRight() { return right; }
    public Vector3f getUp() { return up; }
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) {
        this.yaw = yaw;
        updateVectors();
    }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) {
        this.pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        updateVectors();
    }

    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }
}
