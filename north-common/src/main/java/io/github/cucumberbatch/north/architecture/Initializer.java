package io.github.cucumberbatch.north.architecture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class Initializer {

    @SuppressWarnings("unchecked")
    public <T> T init(Class<? extends T> type, ApplicationContext context) {
        Objects.requireNonNull(type,    "type");
        Objects.requireNonNull(context, "context");

        Collection<Object> dependencies = new ArrayList<>();
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) type.getDeclaredConstructors();
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
                        Object dependency = context.resolve(parameterType);
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
                return foundConstructor.newInstance(dependencies.toArray());
            } else {
                throw new RuntimeException("Inject annotation not found for '" + type.getName() + "'");
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Initialization failed for '" + type.getName() + "'", e);
        }
    }
}
