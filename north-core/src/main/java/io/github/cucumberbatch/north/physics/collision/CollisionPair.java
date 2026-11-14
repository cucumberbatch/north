package io.github.cucumberbatch.north.physics.collision;

import org.joml.Vector3f;

public class CollisionPair {
    public Vector3f A;
    public Vector3f B;
    public Vector3f normal;
    public float depth;

    public CollisionPair(Vector3f A, Vector3f B) {
        this.A = new Vector3f(A);
        this.B = new Vector3f(B);
        Vector3f direction = new Vector3f(A.x - B.x, A.y - B.y, A.z - B.z);
        this.normal = new Vector3f().normalize(direction);
        this.depth  = direction.length();
    }
}
