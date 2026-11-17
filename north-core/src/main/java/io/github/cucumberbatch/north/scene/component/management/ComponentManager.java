package io.github.cucumberbatch.north.scene.component.management;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.scene.command.CommandApplier;
import io.github.cucumberbatch.north.scene.command.AddComponentDeferredCommand;
import io.github.cucumberbatch.north.scene.command.RemoveComponentDeferredCommand;
import io.github.cucumberbatch.north.architecture.Inject;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ComponentManager {
    private final ComponentContainer container;
    private final CommandApplier     commandApplier;

    @Inject
    public ComponentManager(ApplicationContext context) {     
        this.container      = context.addDependency(ComponentContainer.class, new MapBasedComponentContainer());
        this.commandApplier = context.getDependency(CommandApplier.class);
    }

    public <C extends Component> C add(Entity entity, C component) {
        if (entity == null || component == null) {
            throw new IllegalArgumentException("Entity or component must not be null");
        }
        return registerComponent(entity, component);        
    }

    public final <C extends Component> C add(Entity entity, Class<C> type) {
        if (entity == null || type == null) {
            throw new IllegalArgumentException("Entity or component class must not be null");
        }
        return instantiateAndRegisterComponent(entity, type);
    }

    @SafeVarargs
    public final List<? extends Component> add(Entity entity, Class<? extends Component>... types) {
        if (entity == null || types == null) {
            throw new IllegalArgumentException("Entity or component types must not be null or empty");
        }
        List<Component> addedComponents = new ArrayList<>(types.length);
        for (Class<? extends Component> type : types) {
            addedComponents.add(instantiateAndRegisterComponent(entity, type));
        }
        return addedComponents;
    }

    public final <C extends Component> C get(Entity entity, Class<C> type) {
        return container.get(entity, type);
    }

    @SafeVarargs
    public final List<? extends Component> get(Entity entity, Class<? extends Component>... types) {
        List<Component> foundComponents = new ArrayList<>(types.length);
        for (Class<? extends Component> type : types) {
            Component component = container.get(entity, type);
            foundComponents.add(component);
        }
        return foundComponents;
    }

    public final boolean has(Entity entity, Class<? extends Component> type) {
        return container.has(entity, type);
    }

    public final <C extends Component> C remove(Entity entity, Class<C> type) {
        C component = container.get(entity, type);

        commandApplier.addDeferredCommand(new RemoveComponentDeferredCommand(entity, component));

        return component;
    }

    @SafeVarargs
    public final List<? extends Component> remove(Entity entity, Class<? extends Component>... types) {
        List<Component> removedComponents = new ArrayList<>();
        for (Class<? extends Component> type : types) {
            Component remove = remove(entity, type);
            removedComponents.add(remove);
        }
        return removedComponents;
    }

    public final <C extends Component> Collection<C> getAllByType(Class<C> type) {
        return container.getComponentsByType(type);
    }

    public final Set<Class<? extends Component>> getComponentTypes(Entity entity) {
        return container.getComponentTypesByEntity(entity);
    }

    private <C extends Component> C instantiateAndRegisterComponent(Entity entity, Class<C> componentType) {
        C component = instantiateComponentOfType(componentType);
        return registerComponent(entity, component);
    }

    private <C extends Component> C instantiateComponentOfType(Class<C> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            String message = String.format(
                    "Failed to instantiate component of type '%s' using an empty constructor.", type
            );
            throw new RuntimeException(message, e);
        }
    }

    private <C extends Component> C registerComponent(Entity entity, C component) {
        component.setEntity(entity);
        commandApplier.addDeferredCommand(new AddComponentDeferredCommand(entity, component));
        return component;
    }

}
