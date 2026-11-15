package io.github.cucumberbatch.north.system.process;

import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.scene.component.Component;

@FunctionalInterface
public interface RenderProcess<C extends Component> extends Process {

    /**
     * An interface method that renders component content
     *
     * @param graphics a graphics api for rendering objects
     * @author cucumberbatch
     */
    void render(C component, Graphics graphics);

}
