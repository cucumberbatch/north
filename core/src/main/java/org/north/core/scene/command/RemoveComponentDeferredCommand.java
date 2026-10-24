package org.north.core.scene.command;

import org.north.core.scene.component.Component;
import org.north.core.scene.Entity;
import org.north.core.system.SystemManager;

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
        component.setActive(false);
        systemManager.getSystemByComponentType(componentType).removeComponent(component);
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
