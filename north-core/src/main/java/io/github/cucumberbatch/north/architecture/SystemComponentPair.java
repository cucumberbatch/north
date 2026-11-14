package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.system.System;

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
