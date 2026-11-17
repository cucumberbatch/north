package io.github.cucumberbatch.north.memory;

import org.joml.Vector3f;

public class Vector3fPool extends Pool<Vector3f> {
    public Vector3fPool(int capacity) {
        super(Vector3f::new, capacity);
    }
}
