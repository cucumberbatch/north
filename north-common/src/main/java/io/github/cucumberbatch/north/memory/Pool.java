package io.github.cucumberbatch.north.memory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;

public class Pool<T> {
    private final Queue<T>    storage;
    private final Supplier<T> supplier;

    public Pool(Supplier<T> supplier, int initialSize) {
        this.storage  = new LinkedList<>();
        this.supplier = supplier;

        for (int i = 0; i < initialSize; i++) {
            storage.offer(supplier.get());
        }
    }

    public void put(T object) {
        storage.offer(object);
    }

    public T get() {
        return storage.isEmpty() ? supplier.get() : storage.poll();
    }

}
