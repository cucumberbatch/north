package io.github.cucumberbatch.north.architecture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class Initializer {

    @SuppressWarnings("unchecked")
    public <T> T init(Class<? extends T> clazz, ApplicationContext context) {
        Objects.requireNonNull(clazz,   "clazz");
        Objects.requireNonNull(context, "context");

        T object = null;
        
        Collection<Object> dependencies = new ArrayList<>();
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
        Constructor<T> foundConstructor = null;

        try {
            for (Constructor<T> constructor : declaredConstructors) {
                // firstly, we check for no args constructor
                if (constructor.getParameterCount() == 0) {
                    constructor.setAccessible(true);
                    foundConstructor = constructor;
                    continue;
                }

                // only then if there is no such, we search for a specific Inject-marked one
                constructor.setAccessible(true);
                if (constructor.isAnnotationPresent(Inject.class)) {
                    Parameter[] constructorParameters = constructor.getParameters();
                    for (Parameter parameter : constructorParameters) {
                        Class<?> parameterType = parameter.getType();
                        Object dependency = context.getDependency(parameterType);
                        if (dependency == null) {
                            dependencies.clear();
                            break;
                        }
                        dependencies.add(dependency);
                    }
                    foundConstructor = constructor;
                    break;
                }
            }

            if (foundConstructor != null) {
                object = foundConstructor.newInstance(dependencies.toArray());
            } else {
                throw new RuntimeException("Inject annotation not found for a class '" + clazz.getName() + "'");
            }

            return object;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Initialization of '" + clazz.getName() + "' failed", e);
        }
    }
}
