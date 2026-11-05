package org.north.core.utils;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Vector3fPool implements Pool<Vector3f> {
    protected List<Vector3f> list;

    public Vector3fPool(int capacity) {
        list = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            list.add(create());
        }
    }

    public Vector3fPool() {
        this(100);
    }

    @Override
    public Vector3f create() {
        return new Vector3f();
    }

    @Override
    public void put(Vector3f o) {
        list.add(o);
    }

    @Override
    public Vector3f get() {
        return list.isEmpty() ? create() : list.remove(0);
    }
}
