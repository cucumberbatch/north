package org.north.core.system;

import org.joml.Vector3f;
import org.north.core.architecture.ApplicationContext;
import org.north.core.asset.AssetService;
import org.north.core.exception.ComponentAlreadyExistsException;
import org.north.core.utils.Pool;
import org.north.core.utils.Vector3fPool;
import org.north.core.scene.Entity;
import org.north.core.scene.EntityManager;
import org.north.core.scene.component.Component;
import org.north.core.scene.component.management.ComponentContainer;
import org.north.core.scene.component.management.ComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractSystem<C extends Component> implements System<C> {
    
    protected static final Logger log = LoggerFactory.getLogger(AbstractSystem.class);

    private final Class<C>           componentType;
    private final Pool<Vector3f>     vector3fPool;
    private final ComponentContainer container;
    
    protected final ComponentManager cm;
    protected final AssetService     assetService;
    protected final EntityManager    entityManager;
    protected final Entity           sceneRoot;


    public AbstractSystem(Class<C> componentType, ApplicationContext context) {
        this.componentType = componentType;
        this.container     = context.getDependency(ComponentContainer.class);
        this.cm            = context.getDependency(ComponentManager.class);
        this.assetService  = context.getDependency(AssetService.class);
        this.entityManager = context.getDependency(EntityManager.class);
        this.sceneRoot     = entityManager.getRoot();
        this.vector3fPool  = context.getDependency(Vector3fPool.class);
    }

    @Override
    public final Iterator<C> getComponentIterator() {
        return container.getComponentsByType(componentType).iterator();
    }

    @Override
    public final List<C> getComponentList() {
        return new ArrayList<>(container.getComponentsByType(componentType));
    }

    @Override
    public Collection<C> getComponentUnmodifiableCollection() {
        return Collections.unmodifiableCollection(container.getComponentsByType(componentType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C addComponent(Component component)
            throws IllegalArgumentException, ClassCastException, ComponentAlreadyExistsException {
        Entity entity = component.getEntity();
        if (container.has(entity, componentType)) {
            throw new ComponentAlreadyExistsException(component.getClass());
        }
        return (C) component;
        // Logger.debug(String.format("Component added [id=%d type=%s]", component.getId(), component.getClass().getSimpleName()));
    }

    @Override
    public final C removeComponent(Component component) {
        return container.remove(component.getEntity(), componentType);
    }

    @Override
    public void reset() {
        return;
    }

    protected Vector3f vec3f() {
        return vector3fPool.get();
    }

    protected void putBack(Vector3f vector) {
        vector3fPool.put(vector);
    }


}
