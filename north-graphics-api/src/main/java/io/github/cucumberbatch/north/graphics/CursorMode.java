package io.github.cucumberbatch.north.graphics;

/**
 * The visibility state of the mouse cursor on the display.
 */
public enum CursorMode {
    /** The cursor is visible and can leave the window. */
    NORMAL,
    /** The cursor is invisible but its position is still reported. */
    HIDDEN,
    /** The cursor is invisible and its position is locked by the display. */
    DISABLED
}
