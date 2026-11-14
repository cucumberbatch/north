package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.scene.component.RigidBody;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.physics.Physics;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.system.process.UpdateProcess;
import org.joml.Vector3f;

@ComponentHandler(RigidBody.class)
public class RigidBodySystem extends AbstractSystem<RigidBody>
        implements UpdateProcess<RigidBody> {

    @Inject
    public RigidBodySystem(ApplicationContext context) {
        super(RigidBody.class, context);
    }

    @Override
    public void update(RigidBody rigidBody, float deltaTime) {
        Transform transform = cm.get(rigidBody.getEntity(), Transform.class);

        /* Catch the free vector from pool for calculations */
        Vector3f temp = vec3f();

        /* Update all kinds of movement for transform and rigid body components */
        updatePosition(transform, rigidBody, deltaTime, temp);
        updateRotation(transform, rigidBody, deltaTime, temp);
        updateVelocity(rigidBody, deltaTime, temp);
        updateAngularVelocity(rigidBody, deltaTime, temp);

        /* Apply gravitational force to an object */
        if (rigidBody.isGravitational) {
            updateGravitationalAcceleration(rigidBody, deltaTime, temp);
        }

        /* Take it back */
        putBack(temp);
    }

    private void updateGravitationalAcceleration(RigidBody rigidBody, float deltaTime, Vector3f temp) {
        rigidBody.velocity.add(Physics.gravityVector.mul(deltaTime, temp));
    }

    private void updateAngularVelocity(RigidBody that, float deltaTime, Vector3f temp) {
        that.angularVelocity.add(that.angularAcceleration.mul(deltaTime * deltaTime, temp));
    }

    private void updateVelocity(RigidBody that, float deltaTime, Vector3f temp) {
        that.velocity.add(that.acceleration.mul(deltaTime * deltaTime, temp));
    }

    private void updateRotation(Transform transform, RigidBody that, float deltaTime, Vector3f temp) {
        transform.getRotation().add(that.angularVelocity.mul(deltaTime, temp));
    }

    private void updatePosition(Transform transform, RigidBody that, float deltaTime, Vector3f temp) {
        transform.moveRel(that.velocity.mul(deltaTime, temp));
    }

    public void addImpulseToMassCenter(RigidBody rigidBody, Vector3f direction, float mass) {
        Vector3f temp = vec3f();
        rigidBody.velocity.add(direction.mul(mass, temp));
        putBack(temp);
    }

}
