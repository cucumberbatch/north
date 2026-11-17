package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.config.ApplicationProperties;

import java.util.IdentityHashMap;
import java.util.Map;

public class ApplicationContext {
    private final Initializer initializer = new Initializer();
    private final Map<Class<?>, Object> objects = new IdentityHashMap<>();
    private final boolean preventOverwrites;

    public ApplicationContext() {
        save(Initializer.class, initializer);
        save(ApplicationContext.class, this);
        preventOverwrites = ApplicationProperties.getBoolean("application.context.prevent-overwrites");
    }

    /**
     * Returns the dependency of the given type from the context.
     * If no instance is registered, one is created via the initializer and registered.
     *
     * @param type the class of the dependency to retrieve
     * @param <T>  the dependency type
     * @return the dependency instance (existing or newly created)
     */
    public <T> T resolve(Class<T> type) {
        T object = type.cast(objects.get(type));
        if (object == null) {
            object = register(type);
        }
        return object;
    }

    /**
     * Creates and registers a new instance of the given type using the initializer.
     *
     * @param type the class to instantiate and register
     * @param <T>  the dependency type
     * @return the newly created and registered instance
     * @throws IllegalStateException if an instance of the type is already registered
     */
    public <T> T register(Class<T> type) {
        checkExistence(type);
        return save(type, initializer.init(type, this));
    }

    /**
     * Registers the provided object under the given type.
     *
     * @param type   the class to register the object under
     * @param object the instance to register
     * @param <T>    the dependency type
     * @return the registered object
     * @throws IllegalStateException if an instance of the type is already registered
     */
    public <T> T register(Class<T> type, T object) {
        checkExistence(type);
        return save(type, object);
    }

    private void checkExistence(Class<?> type) {
        if (objects.containsKey(type) && preventOverwrites) {
            throw new IllegalStateException("An object of type '" + type.getName() + "' is already in context");
        }
    }
    
    private <T> T save(Class<T> type, T value) {
        objects.put(type, value);
        return value;
    }

}
