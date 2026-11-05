package org.north.core.system.process;

import org.north.core.scene.component.Component;
import org.north.core.graphics.Graphics;

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
