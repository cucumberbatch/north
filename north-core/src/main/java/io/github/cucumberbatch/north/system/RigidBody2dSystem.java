package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.scene.component.RigidBody2d;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.physics.Physics;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.system.process.UpdateProcess;

@ComponentHandler(RigidBody2d.class)
public class RigidBody2dSystem extends AbstractSystem<RigidBody2d>
        implements UpdateProcess<RigidBody2d> {

    @Inject
    public RigidBody2dSystem(ApplicationContext context) {
        super(RigidBody2d.class, context);
    }

    @Override
    public void update(RigidBody2d rigidBody, float deltaTime) {
        Transform transform = cm.get(rigidBody.getEntity(), Transform.class);

        rigidBody.velocity.add(rigidBody.acceleration.x * deltaTime, rigidBody.acceleration.y * deltaTime);
        rigidBody.angularVelocity.add(rigidBody.angularAcceleration.x * deltaTime, rigidBody.angularAcceleration.y * deltaTime);

        transform.moveRel(rigidBody.velocity.x * deltaTime, rigidBody.velocity.y * deltaTime, 0.0f);
        transform.getRotation().add(rigidBody.angularVelocity.x * deltaTime, rigidBody.angularVelocity.y * deltaTime, 0.0f);

        if (rigidBody.isGravitational) {
            rigidBody.velocity.set(Physics.gravityVector.x * deltaTime, Physics.gravityVector.y * deltaTime);
        }
    }

}
