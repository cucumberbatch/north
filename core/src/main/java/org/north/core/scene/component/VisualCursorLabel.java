package org.north.core.scene.component;

import org.north.core.graphics.Texture;
import org.north.core.scene.Entity;

/**
 * A screen-space text label attached to a {@link VisualCursor}.
 *
 * <p>The label renders the name of the entity currently hovered by the cursor. It is a separate
 * entity so it can use the regular mesh/material pipeline and be drawn above the cursor.</p>
 */
public class VisualCursorLabel extends AbstractComponent {

    /** The cursor whose hovered entity is displayed. */
    public Entity cursorEntity;

    /** Distance in pixels between the cursor edge and the label. */
    public float offset = 12f;

    /** Empty space kept between the label and the window edges. */
    public float screenMargin = 8f;

    /** Render order inside the screen-space layer. Must be greater than the cursor's order. */
    public int renderOrder = 200;

    /** Name currently baked into {@link #texture}. */
    public String currentText;

    /** Texture containing the rendered label. */
    public Texture texture;

    public int textureWidth;
    public int textureHeight;

}
