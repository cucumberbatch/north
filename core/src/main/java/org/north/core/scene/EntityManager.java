package org.north.core.scene;

import org.north.core.scene.component.management.ComponentManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Owns the flat collection of scene entities and their ids.
 *
 * <p>Entities are intentionally not a tree. Parent/child relationships, when needed, should be
 * expressed with components (for example {@code Transform.parentTransform}) or specialized
 * hierarchy components instead of putting a tree inside {@link Entity}.</p>
 */
public class EntityManager {
    private final ComponentManager componentManager;
    private final AtomicInteger idSequence = new AtomicInteger(1);

    private final List<Entity> entities = new ArrayList<>();
    private final Map<String, Entity> entitiesByName = new HashMap<>();

    private final Entity root;

    public EntityManager(ComponentManager componentManager) {
        this.componentManager = Objects.requireNonNull(componentManager, "componentManager");
        this.root = create("root");
    }

    public Entity getRoot() {
        return root;
    }

    public Entity create(String name) {
        Entity entity = new Entity(idSequence.getAndIncrement(), name, componentManager);
        entities.add(entity);
        entitiesByName.putIfAbsent(entity.getName(), entity);
        return entity;
    }

    /**
     * Registers an already constructed entity. Most callers should use {@link #create(String)}.
     */
    public boolean add(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entities.contains(entity)) {
            return false;
        }
        entities.add(entity);
        entitiesByName.putIfAbsent(entity.getName(), entity);
        return true;
    }

    public boolean remove(Entity entity) {
        if (entity == null || !entities.remove(entity)) {
            return false;
        }
        entitiesByName.remove(entity.getName(), entity);
        return true;
    }

    /**
     * Looks up the first entity registered with the given name.
     */
    public Entity find(String name) {
        return entitiesByName.get(name);
    }

    public Entity findFirst(Predicate<Entity> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        for (Entity entity : entities) {
            if (predicate.test(entity)) {
                return entity;
            }
        }
        return null;
    }

    public List<Entity> getAll() {
        return Collections.unmodifiableList(entities);
    }

    public int size() {
        return entities.size();
    }
}
