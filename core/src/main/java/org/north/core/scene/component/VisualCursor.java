package org.north.core.scene.component;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.north.core.physics.shape.Rectangle;
import org.north.core.scene.Entity;

/**
 * A physical, spring-driven rectangular cursor.
 *
 * <p>The cursor is a regular entity with a {@link MeshRenderer}, so it is rendered through the
 * standard material/mesh pipeline. Its transform is expressed in window pixels while
 * {@link MeshRenderer#screenSpace} is enabled.</p>
 *
 * <p>Any entity that has a {@link MeshCollider} can act as a magnet target. Screen-space colliders
 * are picked by pointer position, while world-space colliders are picked with a camera ray and
 * their bounds are projected back into window pixels.</p>
 */
public class VisualCursor extends AbstractComponent {

    /** Cursor size in window pixels when it does not overlap any target. */
    public final Vector2f idleSize = new Vector2f(18f, 18f);

    /** Current center of the visual rectangle in window pixels. */
    public final Vector2f center = new Vector2f();

    /** Center at the previous physics step. Kept for debugging and custom integrations. */
    public final Vector2f previousCenter = new Vector2f();

    /** Current cursor velocity in pixels per second. */
    public final Vector2f velocity = new Vector2f();

    /** Center the cursor is currently attracted to. */
    public final Vector2f targetCenter = new Vector2f();

    /** Size the cursor is currently morphing into. */
    public final Vector2f targetSize = new Vector2f(idleSize);

    /** Current visual size in window pixels. */
    public final Vector2f displaySize = new Vector2f(idleSize);

    /** Current cursor rectangle in window pixels. */
    public final Rectangle shape = new Rectangle(new Vector2f(), new Vector2f());

    /** Rectangle of the current magnet target. */
    public final Rectangle targetShape = new Rectangle(new Vector2f(), new Vector2f());

    public final Vector4f color      = new Vector4f(0.55f, 0.85f, 1.0f, 0.35f);
    public final Vector4f idleColor  = new Vector4f(0.55f, 0.85f, 1.0f, 0.35f);
    public final Vector4f hoverColor = new Vector4f(0.70f, 0.95f, 1.0f, 0.28f);

    /** Spring stiffness. Higher values make the cursor catch up faster. */
    public float stiffness = 170f;

    /** Spring damping. Lower values add bounce and inertia. */
    public float damping = 18f;

    /** How fast the cursor morphs into a target rectangle. */
    public float magnetizationSpeed = 14f;

    /** How fast the cursor color changes between idle and hover states. */
    public float colorSpeed = 12f;

    /**
     * Maximum size, in pixels, of a cursor magnetized to a world-space object. UI targets keep
     * their real size; this limit only prevents a close 3D object from covering the whole screen.
     */
    public float maxWorldTargetSize = 220f;

    /** Replaces the operating system cursor with this visual cursor when possible. */
    public boolean hideSystemCursor = true;

    /** Can be used to temporarily hide the visual cursor. */
    public boolean visible = true;

    /** The entity whose collider is currently under the pointer, if any. */
    public Entity hoveredEntity;

    /** The previously hovered entity. Useful for systems that need edge-triggered reactions. */
    public Entity previousHoveredEntity;

    public boolean isHovered() {
        return hoveredEntity != null;
    }

}
