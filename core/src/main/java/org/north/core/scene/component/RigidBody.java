package org.north.core.scene.component;

import org.joml.Vector3f;

public class RigidBody extends AbstractComponent {

    public float mass = 1.0f;

    public Vector3f centerOfMass = new Vector3f();

    public Vector3f velocity = new Vector3f();
    public Vector3f acceleration = new Vector3f();

    public Vector3f angularVelocity = new Vector3f();
    public Vector3f angularAcceleration = new Vector3f();

    public boolean isGravitational = true;

    public float frictionFactor = 0.0f;

    public void addImpulseToMassCenter(Vector3f direction, Vector3f destination) {
        this.velocity.add(direction.mul(mass, destination.zero()));
    }

    public void addImpulseToMassCenter(float x, float y, float z) {
        x *= mass;
        y *= mass;
        z *= mass;
        this.velocity.add(x, y, z);
    }

    /*
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeFloat(mass);
        out.writeObject(centerOfMass);
        out.writeObject(velocity);
        out.writeObject(acceleration);
        out.writeObject(angularVelocity);
        out.writeObject(angularAcceleration);
        out.writeBoolean(isGravitational);
        out.writeFloat(frictionFactor);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        mass = in.readFloat();
        centerOfMass = (Vector3f) in.readObject();
        velocity = (Vector3f) in.readObject();
        acceleration = (Vector3f) in.readObject();
        angularVelocity = (Vector3f) in.readObject();
        angularAcceleration = (Vector3f) in.readObject();
        isGravitational = in.readBoolean();
        frictionFactor = in.readFloat();
    }

     */

    @Override
    public String toString() {
        return "RigidBody{" +
                "mass=" + mass +
                ",centerOfMass=" + centerOfMass +
                ",velocity=" + velocity +
                ",acceleration=" + acceleration +
                ",angularVelocity=" + angularVelocity +
                ",angularAcceleration=" + angularAcceleration +
                ",isGravitational=" + isGravitational +
                ",frictionFactor=" + frictionFactor +
                "}";
    }

}
