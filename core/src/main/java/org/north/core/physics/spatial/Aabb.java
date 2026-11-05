package org.north.core.physics.spatial;

import org.joml.Vector3f;

/**
 * A mutable, axis-aligned bounding box in world space.
 */
public class Aabb {

    private final Vector3f min = new Vector3f();
    private final Vector3f max = new Vector3f();
    private boolean empty = true;

    public Aabb() {
    }

    public Aabb(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        setMinMax(minX, maxX, minY, maxY, minZ, maxZ);
    }

    public Aabb(Vector3f min, Vector3f max) {
        setMinMax(min, max);
    }

    public Aabb setMinMax(float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
        min.set(minX, minY, minZ);
        max.set(maxX, maxY, maxZ);
        empty = false;
        return this;
    }

    public Aabb setMinMax(Vector3f min, Vector3f max) {
        return setMinMax(min.x, max.x, min.y, max.y, min.z, max.z);
    }

    public Aabb set(Aabb other) {
        if (other == null || other.isEmpty()) {
            return clear();
        }
        return setMinMax(other.min, other.max);
    }

    /**
     * Builds a box that contains both points. This is useful for building a broad-phase query
     * around a ray segment.
     */
    public Aabb setFromPoints(Vector3f a, Vector3f b) {
        min.set(
                Math.min(a.x, b.x),
                Math.min(a.y, b.y),
                Math.min(a.z, b.z)
        );
        max.set(
                Math.max(a.x, b.x),
                Math.max(a.y, b.y),
                Math.max(a.z, b.z)
        );
        empty = false;
        return this;
    }

    public Aabb clear() {
        min.zero();
        max.zero();
        empty = true;
        return this;
    }

    public boolean isEmpty() {
        return empty;
    }

    public Vector3f getMin(Vector3f destination) {
        return destination.set(min);
    }

    public Vector3f getMax(Vector3f destination) {
        return destination.set(max);
    }

    public Vector3f getCenter(Vector3f destination) {
        return destination.set(min).add(max).mul(0.5f);
    }

    public Vector3f getSize(Vector3f destination) {
        return destination.set(max).sub(min);
    }

    public boolean matches(Vector3f min, Vector3f max) {
        return !empty
                && this.min.x == min.x && this.min.y == min.y && this.min.z == min.z
                && this.max.x == max.x && this.max.y == max.y && this.max.z == max.z;
    }

    /**
     * Slab-based ray/AABB intersection test.
     *
     * @return distance from the ray origin to the first intersection, or
     *         {@link Float#POSITIVE_INFINITY} when there is no intersection
     */
    public float intersectRay(Vector3f origin, Vector3f direction, float maxDistance) {
        if (empty) {
            return Float.POSITIVE_INFINITY;
        }

        float tMin = 0f;
        float tMax = maxDistance;

        float d = direction.x;
        float o = origin.x;
        if (Math.abs(d) < 0.000001f) {
            if (o < min.x || o > max.x) return Float.POSITIVE_INFINITY;
        } else {
            float t1 = (min.x - o) / d;
            float t2 = (max.x - o) / d;
            if (t1 > t2) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return Float.POSITIVE_INFINITY;
        }

        d = direction.y;
        o = origin.y;
        if (Math.abs(d) < 0.000001f) {
            if (o < min.y || o > max.y) return Float.POSITIVE_INFINITY;
        } else {
            float t1 = (min.y - o) / d;
            float t2 = (max.y - o) / d;
            if (t1 > t2) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return Float.POSITIVE_INFINITY;
        }

        d = direction.z;
        o = origin.z;
        if (Math.abs(d) < 0.000001f) {
            if (o < min.z || o > max.z) return Float.POSITIVE_INFINITY;
        } else {
            float t1 = (min.z - o) / d;
            float t2 = (max.z - o) / d;
            if (t1 > t2) {
                float swap = t1;
                t1 = t2;
                t2 = swap;
            }
            if (t1 > tMin) tMin = t1;
            if (t2 < tMax) tMax = t2;
            if (tMin > tMax) return Float.POSITIVE_INFINITY;
        }

        return tMin;
    }

    public boolean isIntersects(Aabb other) {
        return this.xMin() <= other.xMax() && this.xMax() >= other.xMin()
                && this.yMin() <= other.yMax() && this.yMax() >= other.yMin()
                && this.zMin() <= other.zMax() && this.zMax() >= other.zMin();
    }

    public boolean isInside(Aabb other) {
        return this.xMin() >= other.xMin() && this.xMax() <= other.xMax()
                && this.yMin() >= other.yMin() && this.yMax() <= other.yMax()
                && this.zMin() >= other.zMin() && this.zMax() <= other.zMax();
    }

    public float xMin() {
        return min.x;
    }

    public float xMax() {
        return max.x;
    }

    public float yMin() {
        return min.y;
    }

    public float yMax() {
        return max.y;
    }

    public float zMin() {
        return min.z;
    }

    public float zMax() {
        return max.z;
    }

    @Override
    public String toString() {
        return "Aabb{" +
                "min=" + min +
                ", max=" + max +
                ", empty=" + empty +
                '}';
    }
}
