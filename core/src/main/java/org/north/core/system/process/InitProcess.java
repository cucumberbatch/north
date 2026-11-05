package org.north.core.system.process;

import org.north.core.scene.component.Component;

@FunctionalInterface
public interface InitProcess<C extends Component> extends Process {

    /**
     * An interface method that allows the executions of initialize
     * instructions on the passed in component at the next frame
     * of after the component instantiation
     *
     * @param component is a component that needs to initialize
     * @throws RuntimeException if a runtime exception occurs..
     * @author cucumberbatch
     */
    void init(C component) throws RuntimeException;

}
