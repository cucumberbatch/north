package org.north.core.system.process;

import org.north.core.scene.component.Component;
import org.north.core.physics.collision.Collision;

public interface CollisionHandlerProcess<C extends Component> extends Process {

    /**
     * An interface method that helps to handle collision-starting events for selected component
     *
     * @param component is a component on which it needs to perform update
     * @param collision is a context of a started collision event
     * @author cucumberbatch
     */
    void onCollisionStarted(C component, Collision collision);

    /**
     * An interface method that helps to handle collision-continuing events for selected component
     *
     * @param component is a component on which it needs to perform update
     * @param collision is a context of a continuing collision event
     * @author cucumberbatch
     */
    void onCollisionContinued(C component, Collision collision);

    /**
     * An interface method that helps to handle collision-ending events for selected component
     *
     * @param component is a component on which it needs to perform update
     * @param collision is a context of a ending collision event
     * @author cucumberbatch
     */
    void onCollisionEnded(C component, Collision collision);

}
