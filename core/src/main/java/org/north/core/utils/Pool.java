package org.north.core.utils;

public interface Pool<T> {
    void put(T o);
    T get();
    T create();
}
