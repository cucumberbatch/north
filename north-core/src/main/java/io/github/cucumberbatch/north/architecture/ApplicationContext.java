package io.github.cucumberbatch.north.architecture;

import java.util.IdentityHashMap;
import java.util.Map;

public class ApplicationContext {
    private final Initializer initializer = new Initializer();
    private final Map<Class<?>, Object> objects = new IdentityHashMap<>();
    private final boolean silentRewrites = false;

    public ApplicationContext() {
        this.objects.put(Initializer.class, initializer);
        this.objects.put(ApplicationContext.class, this);
    }

    public <T> T getDependency(Class<T> aClass) {
        T object = aClass.cast(objects.get(aClass));
        if (object == null) {
            object = addDependency(aClass);
        }
        return object;
    }

    public <T> T addDependency(Class<T> aClass) {
        T object = initializer.init(aClass, this);
        objects.put(aClass, object);
        return object;
    }

    public <T> T addDependency(Class<T> aClass, T value) {
        if (objects.containsKey(aClass) && !silentRewrites) {
            throw new IllegalStateException(String.format("An object of type '%s' is already in context", aClass.getName()));
        }
        objects.put(aClass, value);
        return value;
    }

}
