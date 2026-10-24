package org.north.core.architecture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

public class DependencyRegisterer {
    private final Map<Class<?>, Object> objects = new IdentityHashMap<>();

    public <T> T registerDependency(Class<T> aClass, T value) {
        if (objects.containsKey(aClass)) {
            throw new IllegalStateException("Doesn't know what to do if objects map has this type of dependency already");
        }
        objects.put(aClass, value);
        return value;
    }

    public <T> T getDependency(Class<T> aClass)
            throws ClassCastException, ReflectiveOperationException {
        T object = aClass.cast(objects.get(aClass));
        if (object == null) {
            object = registerDependency(aClass);
        }
        return object;
    }

    public Object[] registerDependencies(Class<?>[] classes) throws ReflectiveOperationException {
        Object[] initializedObjects = new Object[classes.length];
        boolean dependencyUpdated;
        do {
            dependencyUpdated = false;
            for (int i = 0; i < classes.length; i++) {
                if (initializedObjects[i] != null) continue;
                Class<?> aClass = classes[i];
                Collection<Object> initializedParams = new ArrayList<>();
                Constructor<?>[] declaredConstructors = aClass.getDeclaredConstructors();
                for (Constructor<?> constructor: declaredConstructors) {
                    if (constructor.getParameterCount() == 0) {
                        constructor.setAccessible(true);
                        initializedObjects[i] = constructor.newInstance();
                        objects.put(aClass, initializedObjects[i]);
                        dependencyUpdated = true;
                        break;
                    }
                    constructor.setAccessible(true);
                    if (!constructor.isAnnotationPresent(Inject.class)) {
                        continue;
                    }
                    Parameter[] constructorParameters = constructor.getParameters();
                    for (Parameter parameter : constructorParameters) {
                        Class<?> parameterType = parameter.getType();
                        Object foundParam = objects.get(parameterType);
                        if (foundParam == null) {
                            initializedParams.clear();
                            break;
                        }
                        initializedParams.add(foundParam);
                    }
                    if (initializedParams.size() != constructorParameters.length) continue;
                    initializedObjects[i] = constructor.newInstance(initializedParams.toArray());
                    objects.put(aClass, initializedObjects[i]);
                    dependencyUpdated = true;
                    break;
                }
            }
        } while (dependencyUpdated);

        return initializedObjects;
    }

    @SuppressWarnings("unchecked")
    public <T> T registerDependency(Class<T> aClass) throws ReflectiveOperationException {
        T object = null;

        Collection<Object> initializedParams = new ArrayList<>();
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) aClass.getDeclaredConstructors();
        Constructor<T> noArgsConstructor = null;

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
                    Object foundParam = objects.get(parameterType);
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
                throw new RuntimeException("Inject annotation not found for a class '" + aClass.getName() + "'");
            }
        }

        objects.put(aClass, object);
        return object;
    }

}
