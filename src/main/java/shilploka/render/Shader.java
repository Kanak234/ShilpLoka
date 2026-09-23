package shilploka.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

/**
 * WHY:
 * OpenGL 3.3 Core requires programmable vertex and fragment shaders for all rendering.
 * This class abstracts shader compilation, program linking, error diagnostics, and
 * efficient uniform variable caching.
 *
 * WHAT:
 * Compiles GLSL vertex and fragment source files loaded from classpath resources, links
 * them into a GPU program object, and provides typed helper methods to transmit matrices,
 * vectors, integers, and floats to GPU memory.
 *
 * USED FOR:
 * Utilized by ChunkMesh to render 3D voxels and by DebugOverlay / GUI to render 2D screens.
 *
 * WHERE NEXT:
 * The bound shader program receives vertex attributes from VAOs and renders geometry
 * during the main render loop in Game.render().
 */
public class Shader {

    private static final Logger LOGGER = LoggerFactory.getLogger(Shader.class);

    private final int programId;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    /**
     * WHY:
     * Constructs and links a complete shader program from vertex and fragment files.
     *
     * WHAT:
     * Loads shader text from classpath resources, compiles each stage, links them,
     * checks for GPU driver compiler errors, and deletes intermediate shader objects.
     *
     * USED FOR:
     * Instantiated during Game initialization for chunk rendering and UI overlays.
     *
     * WHERE NEXT:
     * The compiled program is ready for use via {@link #bind()}.
     *
     * @param vertexResourcePath   Classpath path to the vertex shader (e.g. "/shaders/chunk.vert")
     * @param fragmentResourcePath Classpath path to the fragment shader (e.g. "/shaders/chunk.frag")
     */
    public Shader(String vertexResourcePath, String fragmentResourcePath) {
        String vertexSource = loadResource(vertexResourcePath);
        String fragmentSource = loadResource(fragmentResourcePath);

        int vertexShader = compileStage(GL_VERTEX_SHADER, vertexSource, vertexResourcePath);
        int fragmentShader = compileStage(GL_FRAGMENT_SHADER, fragmentSource, fragmentResourcePath);

        this.programId = glCreateProgram();
        glAttachShader(this.programId, vertexShader);
        glAttachShader(this.programId, fragmentShader);
        glLinkProgram(this.programId);

        // Verify program linking status
        if (glGetProgrami(this.programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(this.programId, 1024);
            LOGGER.error("Shader program linking failed: {}", log);
            throw new RuntimeException("Shader linking failure (" + vertexResourcePath + " + " + fragmentResourcePath + "): " + log);
        }

        // Clean up intermediate shader objects once linked
        glDetachShader(this.programId, vertexShader);
        glDetachShader(this.programId, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        LOGGER.info("Successfully compiled and linked shader program [{}] from ({}, {})",
                this.programId, vertexResourcePath, fragmentResourcePath);
    }

    /**
     * WHY:
     * Compiles an individual GLSL shader stage and reports line-accurate compiler logs.
     *
     * WHAT:
     * Creates an OpenGL shader object, supplies source text, triggers compilation,
     * and asserts GL_COMPILE_STATUS.
     *
     * USED FOR:
     * Internal stage compilation for vertex and fragment programs.
     *
     * WHERE NEXT:
     * The compiled shader ID is attached to the parent program in the constructor.
     */
    private int compileStage(int stageType, String source, String resourcePath) {
        int shaderId = glCreateShader(stageType);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId, 1024);
            String stageName = (stageType == GL_VERTEX_SHADER) ? "Vertex" : "Fragment";
            LOGGER.error("{} shader compilation failed for [{}]:\n{}", stageName, resourcePath, log);
            throw new RuntimeException(stageName + " shader compilation error in " + resourcePath + ": " + log);
        }

        return shaderId;
    }

    /**
     * WHY:
     * Loads shader source code from JAR / classpath resources as clean UTF-8 text.
     *
     * WHAT:
     * Reads lines using BufferedReader and builds a complete string with newline preservation.
     *
     * USED FOR:
     * Loading shader files packaged in src/main/resources/shaders/.
     *
     * WHERE NEXT:
     * Supplies raw source code to {@link #compileStage(int, String, String)}.
     */
    private String loadResource(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            // Also try relative path without leading slash
            String alt = path.startsWith("/") ? path.substring(1) : "/" + path;
            is = getClass().getResourceAsStream(alt);
        }
        if (is == null) {
            throw new IllegalArgumentException("Shader resource not found on classpath: " + path);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed reading shader resource at " + path, e);
        }
        return sb.toString();
    }

    /**
     * WHY:
     * Activates this shader program for subsequent OpenGL draw operations.
     *
     * WHAT:
     * Calls glUseProgram(programId).
     *
     * USED FOR:
     * Called before binding vertex buffers and invoking draw commands.
     *
     * WHERE NEXT:
     * OpenGL pipeline routes subsequent draw calls through this program's vertex and fragment stages.
     */
    public void bind() {
        glUseProgram(this.programId);
    }

    /**
     * WHY:
     * Deactivates the current shader program, resetting to fixed pipeline or state 0.
     *
     * WHAT:
     * Calls glUseProgram(0).
     *
     * USED FOR:
     * Called after draw calls complete to avoid unintentional shader bleed into other passes.
     *
     * WHERE NEXT:
     * Pipeline is cleared until the next shader program binds.
     */
    public void unbind() {
        glUseProgram(0);
    }

    /**
     * WHY:
     * Queries and caches the uniform location integer to prevent expensive glGetUniformLocation
     * lookups during the render loop.
     *
     * WHAT:
     * Checks the HashMap cache; if missing, calls glGetUniformLocation and stores the result.
     *
     * USED FOR:
     * Called by all uniform setter methods (setMat4, setVec3, setInt, etc.).
     *
     * WHERE NEXT:
     * Returns the integer uniform location index passed to glUniform* calls.
     */
    private int getUniformLocation(String name) {
        Integer location = uniformLocationCache.get(name);
        if (location == null) {
            location = glGetUniformLocation(this.programId, name);
            uniformLocationCache.put(name, location);
        }
        return location;
    }

    /**
     * WHY:
     * Transmits a 4x4 projection or view matrix to a shader uniform.
     *
     * WHAT:
     * Uses a stack-allocated 16-element FloatBuffer to upload the matrix without heap allocation.
     *
     * USED FOR:
     * Setting u_ViewProjection in chunk.vert and u_OrthoProjection in ui.vert.
     *
     * WHERE NEXT:
     * Vertex shader uses this matrix to transform 3D model vertices to clip-space.
     */
    public void setMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                matrix.get(buffer);
                glUniformMatrix4fv(location, false, buffer);
            }
        }
    }

    /**
     * WHY:
     * Transmits a 3D vector to a shader uniform.
     *
     * WHAT:
     * Calls glUniform3f with (x, y, z) components.
     *
     * USED FOR:
     * Setting u_ChunkOffset for world positioning.
     *
     * WHERE NEXT:
     * Vertex shader offsets chunk-local coordinates into world space.
     */
    public void setVec3(String name, Vector3f vector) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform3f(location, vector.x, vector.y, vector.z);
        }
    }

    /**
     * WHY:
     * Transmits float coordinates to a shader uniform.
     *
     * WHAT:
     * Calls glUniform3f with individual coordinates.
     *
     * USED FOR:
     * Chunk offset positioning without creating temporary Vector3f objects.
     */
    public void setVec3(String name, float x, float y, float z) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform3f(location, x, y, z);
        }
    }

    /**
     * WHY:
     * Transmits a single float value to a shader uniform.
     *
     * WHAT:
     * Calls glUniform1f.
     *
     * USED FOR:
     * Lighting coefficients, fog density, time variables.
     */
    public void setFloat(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    /**
     * WHY:
     * Transmits an integer to a shader uniform.
     *
     * WHAT:
     * Calls glUniform1i.
     *
     * USED FOR:
     * Texture unit samplers (e.g. u_TextureArray = 0).
     */
    public void setInt(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            glUniform1i(location, value);
        }
    }

    /**
     * WHY:
     * Transmits a boolean flag to a shader uniform.
     *
     * WHAT:
     * Converts boolean to 1 or 0 and calls glUniform1i.
     *
     * USED FOR:
     * Toggling texture sampling vs. flat color in UI shaders.
     */
    public void setBoolean(String name, boolean value) {
        setInt(name, value ? 1 : 0);
    }

    /**
     * WHY:
     * Releases OpenGL GPU resources when this shader is no longer needed.
     *
     * WHAT:
     * Calls glDeleteProgram.
     *
     * USED FOR:
     * Cleanup during game shutdown.
     */
    public void cleanup() {
        glDeleteProgram(this.programId);
        uniformLocationCache.clear();
        LOGGER.info("Disposed shader program [{}]", this.programId);
    }
}
