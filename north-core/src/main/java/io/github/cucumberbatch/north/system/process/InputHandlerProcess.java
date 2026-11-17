package io.github.cucumberbatch.north.system.process;

import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.io.Input;

@FunctionalInterface
public interface InputHandlerProcess<C extends Component> extends Process {

    /**
     * An interface method that helps to handle input events for selected component
     *
     * @param component is a component on which it needs to perform update
     * @param input is an input context that contains an input events
     * @author cucumberbatch
     */
    void handleInput(C component, Input input);
}
