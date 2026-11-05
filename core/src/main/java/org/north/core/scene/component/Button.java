package org.north.core.scene.component;

import org.joml.Vector4f;

/**
 * A rectangular UI button. Interaction is driven by {@link VisualCursor}: the cursor marks the
 * button as hovered, and {@code ButtonSystem} smoothly blends its material color.
 */
public class Button extends AbstractComponent {

    public final Vector4f buttonDefaultColor = new Vector4f(0.18f, 0.21f, 0.27f, 0.92f);
    public final Vector4f buttonOnHoverColor = new Vector4f(0.27f, 0.58f, 0.86f, 1.0f);
    public final Vector4f buttonColor        = new Vector4f(buttonDefaultColor);

    /** Exponential color transition speed. */
    public float colorTransitionSpeed = 14f;

    /** Screen-space buttons interpret their transform as window pixels. */
    public boolean screenSpace = true;

    /** Additional rendering order inside the screen-space layer. */
    public int renderOrder = 0;

    /** Set by {@link org.north.core.system.VisualCursorSystem} while the cursor is over this button. */
    public boolean hovered = false;

}
