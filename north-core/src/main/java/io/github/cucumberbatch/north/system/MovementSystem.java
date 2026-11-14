package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.scene.component.MovementComponent;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.UpdateProcess;

@ComponentHandler(MovementComponent.class)
public class MovementSystem extends AbstractSystem<MovementComponent> implements InitProcess<MovementComponent>, UpdateProcess<MovementComponent> {

    @Inject
    public MovementSystem(ApplicationContext context) {
        super(MovementComponent.class, context);
    }

    @Override
    public void init(MovementComponent movementComponent) {
        movementComponent.initialZ = cm.get(movementComponent.getEntity(), Transform.class).getPosition().z;
    }

    @Override
    public void update(MovementComponent movementComponent, float dt) {
        Transform transform = cm.get(movementComponent.getEntity(), Transform.class);
        float acc = movementComponent.acc;
        float range = movementComponent.range;
        float initialZ = movementComponent.initialZ;


        transform.moveTo(
                transform.getPosition().x,
                transform.getPosition().y,
                initialZ + (float) Math.sin(acc) * range
        );

        movementComponent.acc += dt * 2;

    }
}
