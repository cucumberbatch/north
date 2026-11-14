package io.github.cucumberbatch.north.scene.component;

import io.github.cucumberbatch.north.scene.Entity;

public interface Component {

    default String getSimpleName() {
        return this.getClass().getSimpleName();
    }

    Entity getEntity();
    void setEntity(Entity entity);

    boolean isActive();
    void setActive(boolean active);

    default void switchActivity() {
        setActive(!isActive());
    }

    ComponentState getState();

    void setState(ComponentState state);

    default boolean inState(ComponentState state) {
        return getState().equals(state);
    }

    /**
     * Render layers are sorted in ascending order, so components with a larger layer are drawn later.
     */
    default int getRenderLayer() {
        return 0;
    }
}
 