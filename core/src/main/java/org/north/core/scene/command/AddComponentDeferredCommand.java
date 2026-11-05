package org.north.core.scene.command;

import org.north.core.scene.component.Component;
import org.north.core.scene.Entity;
import org.north.core.system.SystemManager;

public class AddComponentDeferredCommand implements DeferredCommand {
    private final Entity entity;
    private final Component component;

    public AddComponentDeferredCommand(Entity entity, Component component) {
        this.entity = entity;
        this.component = component;
    }

    @Override
    public void execute(SystemManager systemManager) {
        systemManager.addComponent(component);
    }

    @Override
    public String toString() {
        return "AddComponentDeferredCommand{" +
                "entity=" + entity +
                ", component=" + component +
                '}';
    }
}
