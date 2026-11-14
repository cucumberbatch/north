package io.github.cucumberbatch.north.scene.component.management;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.scene.Entity;

import java.util.Collection;
import java.util.Set;

public interface ComponentContainer {
    boolean has(Entity entity, Class<? extends Component> type);

    <C extends Component> C get(Entity entity, Class<C> type);

    <C extends Component> void add(Entity entity, C component);

    <C extends Component> C remove(Entity entity, Class<C> type);

    <C extends Component> Collection<C> getComponentsByType(Class<C> type);

    Set<Class<? extends Component>> getComponentTypesByEntity(Entity entity);

}
