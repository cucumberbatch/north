package io.github.cucumberbatch.north.scene.command;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.system.SystemManager;

public class RemoveComponentDeferredCommand implements DeferredCommand {
    public final Entity entity;
    public final Class<? extends Component> componentType;
    public final Component component;

    public RemoveComponentDeferredCommand(Entity entity, Component component) {
        this.entity = entity;
        this.componentType = component.getClass();
        this.component = component;
    }

    @Override
    public void execute(SystemManager systemManager) {
        systemManager.removeComponent(component);
    }

    @Override
    public String toString() {
        return "RemoveComponentDeferredCommand{" +
                "entity=" + entity +
                ", componentType=" + componentType +
                ", component=" + component +
                '}';
    }
}
