package org.north.core.system.process;

import org.north.core.scene.component.Component;

@FunctionalInterface
public interface UpdateProcess<C extends Component> extends Process {

    /**
     * An interface method that updates all the component content
     *
     * @param component is a component on which it needs to perform update
     * @param deltaTime is a time interval between present and previous frames
     * @author cucumberbatch
     */
    void update(C component, float deltaTime);

}
