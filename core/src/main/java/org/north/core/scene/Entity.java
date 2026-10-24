package org.north.core.scene;

import org.north.core.scene.component.Component;
import org.north.core.scene.component.management.ComponentManager;

import java.util.Collection;
import java.util.Objects;

/**
 * Entity is a flat object that contains a collection of components
 * that describes its nature.
 *
 * <p>Entities are created and tracked by {@link EntityManager}. Entity hierarchy is not
 * part of this class; use components for parent/child relationships.</p>
 *
 * @author cucumberbatch
 */
public class Entity {

    private final int id;
    private final String name;
    private final ComponentManager componentManager;

    Entity(int id, String name, ComponentManager componentManager) {
        if (id <= 0) {
            throw new IllegalArgumentException("Entity id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Entity name must be present as a non blank string");
        }
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.componentManager = Objects.requireNonNull(componentManager, "componentManager");
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public <C extends Component> C getComponent(Class<C> type) {
        return componentManager.get(this, type);
    }

    public <C extends Component> boolean hasComponent(Class<C> type) {
        return componentManager.has(this, type);
    }

    public <C extends Component> C addComponent(Class<C> type) {
        return componentManager.add(this, type);
    }

    public <C extends Component> C addComponent(C component) {
        return componentManager.add(this, component);
    }

    public <C extends Component> C removeComponent(Class<C> type) {
        return componentManager.remove(this, type);
    }

    public Collection<Class<? extends Component>> getComponentTypes() {
        return componentManager.getComponentTypes(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity other = (Entity) o;
        return this.id == other.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Entity{id=" + id + ", name='" + name + "'}";
    }
}
