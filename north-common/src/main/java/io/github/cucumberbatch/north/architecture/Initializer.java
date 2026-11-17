package io.github.cucumberbatch.north.architecture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;

public class Initializer {

    @SuppressWarnings("unchecked")
    public <T> T init(Class<? extends T> clazz, ApplicationContext context) {
        T object = null;
        
        Collection<Object> initializedParams = new ArrayList<>();
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) clazz.getDeclaredConstructors();
        Constructor<T> noArgsConstructor = null;

        try {
            for (Constructor<T> constructor: declaredConstructors) {
                if (constructor.getParameterCount() == 0) {
                    constructor.setAccessible(true);
                    noArgsConstructor = constructor;
                    continue;
                }
                constructor.setAccessible(true);
                if (constructor.isAnnotationPresent(Inject.class)) {
                    Parameter[] constructorParameters = constructor.getParameters();
                    for (Parameter parameter : constructorParameters) {
                        Class<?> parameterType = parameter.getType();
                        Object foundParam = context.getDependency(parameterType);
                        if (foundParam == null) {
                            initializedParams.clear();
                            break;
                        }
                        initializedParams.add(foundParam);
                    }
                    object = constructor.newInstance(initializedParams.toArray());
                    break;
                }
            }

            if (object == null) {
                if (noArgsConstructor != null) {
                    object = noArgsConstructor.newInstance();
                } else {
                    throw new RuntimeException(String.format("Inject annotation not found for a class '%s'", clazz.getName()));
                }
            }
            
            return object;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(String.format("Initialization of '%s' failed", clazz.getName()), e);
        }
    }
}
