package org.north.core.architecture;

import org.north.core.scene.component.Component;
import org.north.core.system.System;

public class SystemComponentPair<S extends System<C>, C extends Component> {
    public Class<S> system;
    public Class<C> component;

    public SystemComponentPair(Class<S> system, Class<C> component) {
        this.system = system;
        this.component = component;
    }

    @Override
    public String toString() {
        return "Pair{" +
                "system=" + system.getName() +
                ", component=" + component.getName() +
                '}';
    }
}
