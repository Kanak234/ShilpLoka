package shilploka.entity;

/**
 * WHY:
 * Collision detection and spatial queries in a voxel world require precise, fast,
 * Axis-Aligned Bounding Box (AABB) intersection and swept-volume calculations.
 *
 * WHAT:
 * Immutable 3D bounding box defined by [minX, minY, minZ] to [maxX, maxY, maxZ].
 * Provides intersection checks, swept expansion, and axis-by-axis collision clipping
 * against voxel obstacles, enabling smooth wall-sliding and ground contact resolution.
 *
 * USED FOR:
 * Player bounding box, entity collisions, raycasting boundaries, and block placement prevention.
 *
 * WHERE NEXT:
 * Player.move() queries World solid blocks and clips velocity against their AABBs.
 */
public record AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public AABB {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException(String.format(
                    "Invalid AABB coordinates: min(%.2f, %.2f, %.2f) > max(%.2f, %.2f, %.2f)",
                    minX, minY, minZ, maxX, maxY, maxZ));
        }
    }

    /**
     * Constructs a 1x1x1 unit voxel bounding box at world coordinates (x, y, z).
     */
    public static AABB fromBlock(int x, int y, int z) {
        return new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0);
    }

    /**
     * Translates this bounding box by displacement (dx, dy, dz).
     */
    public AABB offset(double dx, double dy, double dz) {
        return new AABB(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    /**
     * Expands this bounding box to encompass a displacement motion vector.
     */
    public AABB expand(double dx, double dy, double dz) {
        double newMinX = dx < 0.0 ? minX + dx : minX;
        double newMaxX = dx > 0.0 ? maxX + dx : maxX;
        double newMinY = dy < 0.0 ? minY + dy : minY;
        double newMaxY = dy > 0.0 ? maxY + dy : maxY;
        double newMinZ = dz < 0.0 ? minZ + dz : minZ;
        double newMaxZ = dz > 0.0 ? maxZ + dz : maxZ;
        return new AABB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    /**
     * Checks if this bounding box intersects another bounding box.
     */
    public boolean intersects(AABB other) {
        return other.maxX > this.minX && other.minX < this.maxX &&
               other.maxY > this.minY && other.minY < this.maxY &&
               other.maxZ > this.minZ && other.minZ < this.maxZ;
    }

    /**
     * Checks if this bounding box intersects given coordinate bounds.
     */
    public boolean intersects(double x0, double y0, double z0, double x1, double y1, double z1) {
        return x1 > this.minX && x0 < this.maxX &&
               y1 > this.minY && y0 < this.maxY &&
               z1 > this.minZ && z0 < this.maxZ;
    }

    /**
     * WHY:
     * Clips movement along the X axis against obstacle AABB 'c'.
     *
     * WHAT:
     * If Y and Z intervals overlap, clamps displacement 'xa' so this box does not penetrate 'c'.
     */
    public double clipXCollide(AABB c, double xa) {
        if (c.maxY <= this.minY || c.minY >= this.maxY) return xa;
        if (c.maxZ <= this.minZ || c.minZ >= this.maxZ) return xa;

        if (xa > 0.0 && c.minX >= this.maxX) {
            double max = c.minX - this.maxX;
            if (max < xa) xa = max;
        }
        if (xa < 0.0 && c.maxX <= this.minX) {
            double max = c.maxX - this.minX;
            if (max > xa) xa = max;
        }

        return xa;
    }

    /**
     * WHY:
     * Clips movement along the Y axis against obstacle AABB 'c'.
     *
     * WHAT:
     * If X and Z intervals overlap, clamps displacement 'ya' so this box does not penetrate 'c'.
     */
    public double clipYCollide(AABB c, double ya) {
        if (c.maxX <= this.minX || c.minX >= this.maxX) return ya;
        if (c.maxZ <= this.minZ || c.minZ >= this.maxZ) return ya;

        if (ya > 0.0 && c.minY >= this.maxY) {
            double max = c.minY - this.maxY;
            if (max < ya) ya = max;
        }
        if (ya < 0.0 && c.maxY <= this.minY) {
            double max = c.maxY - this.minY;
            if (max > ya) ya = max;
        }

        return ya;
    }

    /**
     * WHY:
     * Clips movement along the Z axis against obstacle AABB 'c'.
     *
     * WHAT:
     * If X and Y intervals overlap, clamps displacement 'za' so this box does not penetrate 'c'.
     */
    public double clipZCollide(AABB c, double za) {
        if (c.maxX <= this.minX || c.minX >= this.maxX) return za;
        if (c.maxY <= this.minY || c.minY >= this.maxY) return za;

        if (za > 0.0 && c.minZ >= this.maxZ) {
            double max = c.minZ - this.maxZ;
            if (max < za) za = max;
        }
        if (za < 0.0 && c.maxZ <= this.minZ) {
            double max = c.maxZ - this.minZ;
            if (max > za) za = max;
        }

        return za;
    }

    /**
     * Tests line-of-sight ray intersection against this AABB using slab method.
     */
    public boolean intersectsRay(org.joml.Vector3d origin, org.joml.Vector3f dir, double maxDist) {
        double tMin = 0.0;
        double tMax = maxDist;

        // X slab
        if (Math.abs(dir.x) < 1e-6) {
            if (origin.x < minX || origin.x > maxX) return false;
        } else {
            double invD = 1.0 / dir.x;
            double t0 = (minX - origin.x) * invD;
            double t1 = (maxX - origin.x) * invD;
            if (t0 > t1) { double tmp = t0; t0 = t1; t1 = tmp; }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) return false;
        }

        // Y slab
        if (Math.abs(dir.y) < 1e-6) {
            if (origin.y < minY || origin.y > maxY) return false;
        } else {
            double invD = 1.0 / dir.y;
            double t0 = (minY - origin.y) * invD;
            double t1 = (maxY - origin.y) * invD;
            if (t0 > t1) { double tmp = t0; t0 = t1; t1 = tmp; }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) return false;
        }

        // Z slab
        if (Math.abs(dir.z) < 1e-6) {
            if (origin.z < minZ || origin.z > maxZ) return false;
        } else {
            double invD = 1.0 / dir.z;
            double t0 = (minZ - origin.z) * invD;
            double t1 = (maxZ - origin.z) * invD;
            if (t0 > t1) { double tmp = t0; t0 = t1; t1 = tmp; }
            tMin = Math.max(tMin, t0);
            tMax = Math.min(tMax, t1);
            if (tMin > tMax) return false;
        }

        return true;
    }

    public boolean intersectsRay(org.joml.Vector3f origin, org.joml.Vector3f dir, double maxDist) {
        return intersectsRay(new org.joml.Vector3d(origin.x, origin.y, origin.z), dir, maxDist);
    }
}
