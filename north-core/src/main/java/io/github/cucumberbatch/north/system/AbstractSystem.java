package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.memory.Pool;
import org.joml.Vector3f;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.asset.AssetService;
import io.github.cucumberbatch.north.exception.ComponentAlreadyExistsException;
import io.github.cucumberbatch.north.memory.Vector3fPool;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.scene.EntityManager;
import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.scene.component.management.ComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractSystem<C extends Component> implements System<C> {
    
    protected static final Logger log = LoggerFactory.getLogger(AbstractSystem.class);

    private final Class<C>           componentType;
    private final Pool<Vector3f>     vector3fPool;

    protected final ComponentManager cm;
    protected final AssetService     assetService;
    protected final EntityManager    entityManager;
    protected final Entity           sceneRoot;


    public AbstractSystem(Class<C> componentType, ApplicationContext context) {
        this.componentType = componentType;
        this.cm            = context.resolve(ComponentManager.class);
        this.assetService  = context.resolve(AssetService.class);
        this.entityManager = context.resolve(EntityManager.class);
        this.sceneRoot     = entityManager.getRoot();
        this.vector3fPool  = context.resolve(Vector3fPool.class);
    }

    @Override
    public final Iterator<C> getComponentIterator() {
        return cm.getAllByType(componentType).iterator();
    }

    @Override
    public final List<C> getComponentList() {
        return new ArrayList<>(cm.getAllByType(componentType));
    }

    @Override
    public Collection<C> getComponentUnmodifiableCollection() {
        return Collections.unmodifiableCollection(cm.getAllByType(componentType));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final C addComponent(Component component)
            throws IllegalArgumentException, ClassCastException, ComponentAlreadyExistsException {
        Entity entity = component.getEntity();
        if (cm.has(entity, componentType)) {
            throw new ComponentAlreadyExistsException(component.getClass());
        }
        return (C) component;
    }

    @Override
    public final C removeComponent(Component component) {
        return cm.remove(component.getEntity(), componentType);
    }

    @Override
    public void reset() {
    }

    protected Vector3f vec3f() {
        return vector3fPool.get();
    }

    protected void putBack(Vector3f vector) {
        vector3fPool.put(vector);
    }


}
