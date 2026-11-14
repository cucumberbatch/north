package io.github.cucumberbatch.north.scene.command;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.system.SystemManager;

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
