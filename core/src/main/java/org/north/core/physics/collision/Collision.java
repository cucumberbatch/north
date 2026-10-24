package org.north.core.physics.collision;

import org.north.core.physics.spatial.Aabb;

public class Collision {
    private Aabb a;
    private Aabb b;
    private CollisionPair pair;
    private CollisionState state;
    private boolean isModified;

    /**
     * Creates a collision object with default state {@link CollisionState#ENTERED}
     *
     * @param a first colliding object
     * @param b second colliding object
     * @param pair object that holds info about collision
     */
    public Collision(Aabb a,
                     Aabb b,
                     CollisionPair pair) {
        this(CollisionState.ENTERED, a, b, pair);
    }

    /**
     * Creates a collision object with specified {@code state}
     *
     * @param state state of collision from {@link CollisionState}
     * @param a first colliding object
     * @param b second colliding object
     * @param pair object that holds info about collision
     */
    public Collision(CollisionState state,
                     Aabb a,
                     Aabb b,
                     CollisionPair pair) {
        this.state = state;
        this.a = a;
        this.b = b;
        this.pair = pair;
    }

    public Aabb getA() {
        return a;
    }

    public Aabb getB() {
        return b;
    }

    public CollisionPair getPair() {
        return pair;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setA(Aabb a) {
        this.a = a;
    }

    public void setB(Aabb b) {
        this.b = b;
    }

    public void setPair(CollisionPair pair) {
        this.pair = pair;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
    }

    public CollisionState getState() {
        return state;
    }

    /**
     * Changes collision state and also sets {@code isModified} to {@code true}
     * @param state new state of collision
     */
    public void setState(CollisionState state) {
        this.state = state;
        this.isModified = true;
    }

    public boolean inState(CollisionState state) {
        return getState().equals(state);
    }

    public void swapAB() {
        Aabb temp = b;
        b = a;
        a = temp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Collision collision = (Collision) o;

        return this.state.equals(collision.state) &&
                (this.a == collision.a && this.b == collision.b ||
                 this.a == collision.b && this.b == collision.a);
    }

    @Override
    public int hashCode() {
        int result = a != null ? a.hashCode() : 0;
        result = 31 * result + (b != null ? b.hashCode() : 0);
        result = 31 * result + (pair != null ? pair.hashCode() : 0);
        return result;
    }
}
