package shilploka.render;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * WHY:
 * In an infinite voxel world with render distances spanning hundreds of sections,
 * rendering chunks behind or outside the camera's field of view wastes GPU rasterization
 * capacity. Frustum culling discards sections before issuing draw calls.
 *
 * WHAT:
 * Extracts the 6 clipping planes (Left, Right, Bottom, Top, Near, Far) from the 4x4
 * View-Projection matrix using the Gribb-Hartmann algorithm and provides an optimal
 * AABB (Axis-Aligned Bounding Box) intersection test for 16³ chunk sections.
 *
 * USED FOR:
 * Evaluated in ChunkManager prior to rendering each section.
 *
 * WHERE NEXT:
 * Only visible sections are queued for OpenGL draw call dispatch.
 */
public class FrustumCuller {

    private final Vector4f[] planes = new Vector4f[6];

    public FrustumCuller() {
        for (int i = 0; i < 6; i++) {
            planes[i] = new Vector4f();
        }
    }

    /**
     * WHY:
     * Extracts and normalizes the 6 frustum clipping planes from the combined View-Projection matrix.
     *
     * WHAT:
     * Performs row additions and subtractions on matrix coefficients and divides by plane normal length.
     *
     * USED FOR:
     * Called at the start of each frame when camera matrices update.
     */
    public void update(Matrix4f m) {
        // Left Plane: row 4 + row 1
        planes[0].set(m.m03() + m.m00(), m.m13() + m.m10(), m.m23() + m.m20(), m.m33() + m.m30());
        // Right Plane: row 4 - row 1
        planes[1].set(m.m03() - m.m00(), m.m13() - m.m10(), m.m23() - m.m20(), m.m33() - m.m30());
        // Bottom Plane: row 4 + row 2
        planes[2].set(m.m03() + m.m01(), m.m13() + m.m11(), m.m23() + m.m21(), m.m33() + m.m31());
        // Top Plane: row 4 - row 2
        planes[3].set(m.m03() - m.m01(), m.m13() - m.m11(), m.m23() - m.m21(), m.m33() - m.m31());
        // Near Plane: row 4 + row 3
        planes[4].set(m.m03() + m.m02(), m.m13() + m.m12(), m.m23() + m.m22(), m.m33() + m.m32());
        // Far Plane: row 4 - row 3
        planes[5].set(m.m03() - m.m02(), m.m13() - m.m12(), m.m23() - m.m22(), m.m33() - m.m32());

        // Normalize all 6 plane vectors
        for (int i = 0; i < 6; i++) {
            Vector4f plane = planes[i];
            float len = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
            if (len > 0.0f) {
                plane.div(len);
            }
        }
    }

    /**
     * WHY:
     * Fast test determining whether a 16³ chunk section bounding box intersects the view frustum.
     *
     * WHAT:
     * Computes the positive vertex for each plane normal and checks if it falls behind the plane.
     *
     * @return true if the box is partially or fully inside the view frustum; false if completely culled.
     */
    public boolean isBoxVisible(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        for (int i = 0; i < 6; i++) {
            Vector4f p = planes[i];
            float px = (p.x > 0.0f) ? maxX : minX;
            float py = (p.y > 0.0f) ? maxY : minY;
            float pz = (p.z > 0.0f) ? maxZ : minZ;

            if (p.x * px + p.y * py + p.z * pz + p.w < 0.0f) {
                return false; // Box is entirely on the outside of this clipping plane
            }
        }
        return true;
    }
}
