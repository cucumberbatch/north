package org.north.core.physics.collision;

import org.joml.Vector3f;

public class Sphere {
    public float x, y, z, r;

    public Sphere() {
    }

    public Sphere(Vector3f center, float radius) {
        this(center.x, center.y, center.z, radius);
    }

    public Sphere(float x, float y, float z, float r) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Sphere sphere = (Sphere) o;

        if (Float.compare(x, sphere.x) != 0) return false;
        if (Float.compare(y, sphere.y) != 0) return false;
        if (Float.compare(z, sphere.z) != 0) return false;
        return Float.compare(r, sphere.r) == 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (x != 0.0f ? Float.floatToRawIntBits(x) : 0);
        result = prime * result + (y != 0.0f ? Float.floatToRawIntBits(y) : 0);
        result = prime * result + (z != 0.0f ? Float.floatToRawIntBits(z) : 0);
        result = prime * result + (r != 0.0f ? Float.floatToRawIntBits(r) : 0);
        return result;
    }

    public Vector3f getCenter() {
        return new Vector3f(x, y, z);
    }
}
