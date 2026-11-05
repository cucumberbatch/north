package org.north.core.scene.component;

import org.north.core.scene.Entity;

public abstract class AbstractComponent implements Component {

    /* Entity which this component is belongs to */
    public transient Entity entity;

    /* Activity state of component */
    public boolean active = true;

    /* State of component lifecycle */
    private ComponentState state = ComponentState.READY_TO_INIT_STATE;


    /*
     Getters and setters implementation by an abstract component class
     */
    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public ComponentState getState() {
        return state;
    }

    @Override
    public void setState(ComponentState state) {
        if (state == null) throw new NullPointerException("Component state cannot be null!");
        if (this.state == state) return;
        this.state = state;
    }

    @Override
    public String toString() {
        return "AbstractComponent{" +
                "entityId=" + (entity == null ? null : entity.getId()) +
                ", isActive=" + active +
                ", state=" + state +
                '}';
    }

}
