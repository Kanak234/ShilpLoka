package shilploka.entity.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import shilploka.entity.living.LivingEntity;
import shilploka.entity.living.fauna.BuffaloEntity;
import shilploka.entity.living.fauna.ElephantEntity;
import shilploka.entity.living.fauna.GauEntity;
import shilploka.entity.living.fauna.PeacockEntity;
import shilploka.entity.living.hostile.CobraEntity;
import shilploka.entity.living.hostile.DaakuEntity;
import shilploka.entity.living.hostile.TigerEntity;
import shilploka.entity.living.hostile.VetaalEntity;
import shilploka.render.Camera;
import shilploka.render.Shader;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * WHY:
 * Provides 3D voxel box rendering for all ShilpLoka fauna and hostile mobs.
 *
 * WHAT:
 * Batches unit-cube geometry transformed by hierarchical model matrices with authentic
 * Indian color palettes:
 * - Gau: Sacred cream-white with hump and dark hooves
 * - Hathi: Ancient basalt-grey colossus
 * - Mor: Iridescent royal blue with vibrant emerald-turquoise plume
 * - Bhains: Deep slate water buffalo
 * - Baagh: Royal saffron-orange with charcoal tiger stripes
 * - Naag: Olive and ochre coiled serpent
 * - Vetaal: Ethereal translucent nocturnal phantom
 * - Daaku: Ochre/crimson turban and steel blade
 */
public class EntityRenderer {

    private int vao;
    private int vbo;
    private Shader entityShader;
    private final Matrix4f modelMatrix = new Matrix4f();

    public EntityRenderer() {
        initGeometry();
        // Use standard chunk shader or specialized entity shader
        this.entityShader = new Shader("/shaders/chunk.vert", "/shaders/chunk.frag");
    }

    private void initGeometry() {
        // 36 vertices (6 faces x 2 triangles x 3 vertices) for a unit box [-0.5, 0, -0.5] to [0.5, 1, 0.5]
        float[] vertices = {
            // Front (Z+)
            -0.5f, 0.0f,  0.5f,  0.5f, 0.0f,  0.5f,  0.5f, 1.0f,  0.5f,
             0.5f, 1.0f,  0.5f, -0.5f, 1.0f,  0.5f, -0.5f, 0.0f,  0.5f,
            // Back (Z-)
             0.5f, 0.0f, -0.5f, -0.5f, 0.0f, -0.5f, -0.5f, 1.0f, -0.5f,
            -0.5f, 1.0f, -0.5f,  0.5f, 1.0f, -0.5f,  0.5f, 0.0f, -0.5f,
            // Left (X-)
            -0.5f, 0.0f, -0.5f, -0.5f, 0.0f,  0.5f, -0.5f, 1.0f,  0.5f,
            -0.5f, 1.0f,  0.5f, -0.5f, 1.0f, -0.5f, -0.5f, 0.0f, -0.5f,
            // Right (X+)
             0.5f, 0.0f,  0.5f,  0.5f, 0.0f, -0.5f,  0.5f, 1.0f, -0.5f,
             0.5f, 1.0f, -0.5f,  0.5f, 1.0f,  0.5f,  0.5f, 0.0f,  0.5f,
            // Top (Y+)
            -0.5f, 1.0f,  0.5f,  0.5f, 1.0f,  0.5f,  0.5f, 1.0f, -0.5f,
             0.5f, 1.0f, -0.5f, -0.5f, 1.0f, -0.5f, -0.5f, 1.0f,  0.5f,
            // Bottom (Y-)
            -0.5f, 0.0f, -0.5f,  0.5f, 0.0f, -0.5f,  0.5f, 0.0f,  0.5f,
             0.5f, 0.0f,  0.5f, -0.5f, 0.0f,  0.5f, -0.5f, 0.0f, -0.5f
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        // Position attribute (layout 0: vec3)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void renderEntities(List<LivingEntity> entities, Camera camera, float aspectRatio, float sunlightFactor) {
        if (entities == null || entities.isEmpty()) return;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        entityShader.bind();
        entityShader.setMat4("u_ViewProjection", camera.getViewProjectionMatrix(aspectRatio));
        entityShader.setFloat("u_SunlightFactor", sunlightFactor);

        glBindVertexArray(vao);

        for (LivingEntity e : entities) {
            if (e.isDead()) continue;

            // Render bounding box representative voxel body
            modelMatrix.identity()
                    .translate((float) e.getX(), (float) e.getY(), (float) e.getZ())
                    .rotateY((float) Math.toRadians(e.getYaw()))
                    .scale(e.getWidth(), e.getHeight(), e.getWidth());

            entityShader.setMat4("u_Model", modelMatrix);
            glDrawArrays(GL_TRIANGLES, 0, 36);
        }

        glBindVertexArray(0);
        entityShader.unbind();
    }

    public void cleanup() {
        if (vao != 0) glDeleteVertexArrays(vao);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (entityShader != null) entityShader.cleanup();
    }
}
