package org.north.core.scene.component;

import org.north.core.physics.shape.Rectangle;
import org.north.core.physics.spatial.Aabb;
import org.joml.Vector2f;

public class MeshCollider extends AbstractComponent {

    public enum Space {
        /** The collider lives in window pixels and is used for UI interaction. */
        SCREEN,
        /** The collider lives in world units and is used for scene picking. */
        WORLD
    }

    public Space space = Space.SCREEN;

    /** Screen-space rectangle. Used when {@link #space} is {@link Space#SCREEN}. */
    public Rectangle body = new Rectangle(new Vector2f().zero(), new Vector2f().zero());

    /**
     * Bounds of the collider in local mesh space.
     *
     * <p>An empty box means "derive from the entity's {@link MeshRenderer} mesh". If the entity
     * has no mesh, the collider falls back to a unit cube. Set this explicitly to override the
     * mesh bounds.</p>
     */
    public Aabb localBounds = new Aabb();

    /** World-space bounds. Updated by {@code MeshColliderSystem}. */
    public final Aabb bounds = new Aabb();

    /** World colliders with this flag disabled are skipped by scene picking. */
    public boolean selectable = true;

    /**
     * Clears custom bounds so the next collider update derives them from the current
     * {@link MeshRenderer} mesh again.
     */
    public void refreshLocalBounds() {
        localBounds = new Aabb();
    }

}
