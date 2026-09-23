package shilploka.entities;

import shilploka.core.Game;
import shilploka.world.WorldGenerator;
import org.joml.Vector3f;
import static org.lwjgl.glfw.GLFW.*;

public class Player {

    // Position components are public fields in JOML Vector3f
    public float x, y, z;
    
    private float yaw; // horizontal rotation
    private float pitch; // vertical rotation
    
    private boolean onGround = false;
    private boolean isSprinting = false;
    private boolean isSneaking = false;
    
    // Movement velocity
    private float dx, dz;  // horizontal movement
    private float dy;      // vertical movement (gravity/jump)
    
    // Player constants
    private static final float MOVE_SPEED = 0.1f;
    private static final float SPRINT_SPEED = 0.2f;
    private static final float GRAVITY = -0.5f;
    private static final float JUMP_HEIGHT = 0.4f;
    private static final float FRICTION = 0.9f;
    
    private WorldGenerator worldGen;
    
    public Player(float x, float y, float z, WorldGenerator worldGen) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        this.worldGen = worldGen;
    }
    
    public void update(long window, double delta) {
        // Handle input
        handleInput(window, delta);
        
        // Apply gravity if not on ground
        if (!onGround) {
            dy = GRAVITY * (float) delta;
        }
        
        // Apply horizontal movement
        x += dx * (float) delta;
        z += dz * (float) delta;
        
        // Apply vertical movement
        y += dy * (float) delta;
        
        // Check collisions
        checkCollisions();
        
        // Slow down when no input
        if (dx != 0 || dz != 0) {
            dx *= FRICTION;
            dz *= FRICTION;
        }
        
        // Clamp pitch
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;
    }
    
    private void handleInput(long window, double delta) {
        // Reset movement this frame
        dx = 0;
        dz = 0;
        dy = 0;
        
        // Mouse look - rotate camera
        if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS || 
            glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_2) == GLFW_PRESS) {
            double[] mouseX = new double[1];
            double[] mouseY = new double[1];
            glfwGetCursorPos(window, mouseX, mouseY);
            
            float sensitivity = 0.1f;
            yaw += mouseX[0] * sensitivity;
            pitch -= mouseY[0] * sensitivity;
        }
        
        // Movement - calculate direction based on yaw
        float moveSpeed = isSprinting ? SPRINT_SPEED : MOVE_SPEED;
        
        // W (forward)
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            dx += - (float) Math.sin(Math.toRadians(yaw)) * moveSpeed;
            dz += - (float) Math.cos(Math.toRadians(yaw)) * moveSpeed;
        }
        // S (backward)
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            dx += (float) Math.sin(Math.toRadians(yaw)) * moveSpeed;
            dz += (float) Math.cos(Math.toRadians(yaw)) * moveSpeed;
        }
        // A (left)
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            dx += (float) Math.cos(Math.toRadians(yaw)) * moveSpeed;
            dz += - (float) Math.sin(Math.toRadians(yaw)) * moveSpeed;
        }
        // D (right)
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            dx += - (float) Math.cos(Math.toRadians(yaw)) * moveSpeed;
            dz += (float) Math.sin(Math.toRadians(yaw)) * moveSpeed;
        }
        
        // Jump
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS && onGround) {
            dy = JUMP_HEIGHT;
            onGround = false;
        }
        
        // Sprint (left shift held)
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            isSprinting = true;
        } else {
            isSprinting = false;
        }
        
        // Sneak (left shift)
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            isSneaking = true;
        }
    }
    
    private void checkCollisions() {
        // Ensure player doesn't fall below world y=0
        if (y < 0) {
            y = 0;
            dy = 0;
            onGround = true;
        }
        
        // TODO: Add block collision detection here
        // This would check the blocks around the player position
        // and push the player out if they're inside a solid block
    }
}