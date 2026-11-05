package org.north.core.scene.component;

import org.joml.Vector2f;

public class RigidBody2d extends AbstractComponent {

    public float mass = 1.0f;

    public Vector2f centerOfMass = new Vector2f();

    public Vector2f velocity = new Vector2f();
    public Vector2f acceleration = new Vector2f();

    public Vector2f angularVelocity = new Vector2f();
    public Vector2f angularAcceleration = new Vector2f();

    public float frictionFactor = 0.0f;

    public boolean isGravitational = true;

    // Hooke's law
    public Vector2f restoringForce = new Vector2f();
    public float springFactor = 0.0f;


    @Override
    public String toString() {
        return "RigidBody2d{" +
                "mass=" + mass +
                ",centerOfMass=" + centerOfMass +
                ",velocity=" + velocity +
                ",acceleration=" + acceleration +
                ",angularVelocity=" + angularVelocity +
                ",angularAcceleration=" + angularAcceleration +
                ",isGravitational=" + isGravitational +
                ",frictionFactor=" + frictionFactor +
                ",restoringForce=" + restoringForce +
                ",springFactor=" + springFactor +
                "}";
    }

}
