package org.north.core.scene.component.management;

import org.north.core.scene.component.Component;
import org.north.core.scene.Entity;
import org.north.core.exception.ComponentAlreadyExistsException;

import java.util.*;

public class MapBasedComponentContainer implements ComponentContainer {
    
    private final
    Map<Class<? extends Component>, Map<Entity, ? extends Component>> componentByEntityStorage;

    private final
    Map<Class<? extends Component>, List<Component>> componentByTypeStorage;


    public MapBasedComponentContainer() {
        this.componentByEntityStorage = new HashMap<>();
        this.componentByTypeStorage   = new IdentityHashMap<>();
    }

    private <C extends Component> C tryGetComponent(Entity entity, Class<C> type) {
        Map<Entity, ? extends Component> componentsMap = this.componentByEntityStorage.get(type);
        return (componentsMap != null) ? type.cast(componentsMap.get(entity)) : null;
    }
    
    @Override
    public boolean has(Entity entity, Class<? extends Component> type) {
        assert(entity != null);
        assert(type   != null);
        
        return tryGetComponent(entity, type) != null;
    }

    @Override
    public <C extends Component> C get(Entity entity, Class<C> type) {
        assert(entity != null);
        assert(type   != null);
        
        return tryGetComponent(entity, type);
    }

    @Override
    public <C extends Component> void add(Entity entity, C component) {
        assert(entity    != null);
        assert(component != null);
        
        Class<? extends Component> type = component.getClass();

        @SuppressWarnings("unchecked")
        Map<Entity, Component> componentsMap = (Map<Entity, Component>) componentByEntityStorage.get(type);
        List<Component>        components    = componentByTypeStorage.get(type);

        if (componentsMap == null || components == null) {
            componentsMap = new HashMap<>();
            components    = new ArrayList<>();
            componentByEntityStorage.put(type, componentsMap);
            componentByTypeStorage  .put(type, components);
        } else if (componentsMap.containsKey(entity)) {
            throw new ComponentAlreadyExistsException(type);
        }

        componentsMap.put(entity, component);
        components   .add(component);
    }

    @Override
    public <C extends Component> C remove(Entity entity, Class<C> type) {
        assert(entity != null);
        assert(type   != null);
        
        Map<Entity, ? extends Component> componentsMap = componentByEntityStorage.get(type);

        Component component;
        if (componentsMap == null || (component = componentsMap.remove(entity)) == null) {
            throw new NullPointerException("Component not found!");
        }
        componentByTypeStorage.get(type).remove(component);
        return type.cast(component);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends Component> Collection<C> getComponentsByType(Class<C> type) {
        assert(type != null);
        return (Collection<C>) componentByTypeStorage.get(type);
    }

    @Override
    public Set<Class<? extends Component>> getComponentTypesByEntity(Entity entity) {
        assert(entity != null);
        
        Set<Class<? extends Component>> componentTypes = new HashSet<>();
        for (Map<Entity, ? extends Component> entityToComponentMap : componentByEntityStorage.values()) {
            Component component = entityToComponentMap.get(entity);
            if (component != null) {
                componentTypes.add(component.getClass());
            }
        }
        return componentTypes;
    }
}
