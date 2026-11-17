package io.github.cucumberbatch.north.graphics;

/**
 * Platform-neutral access to the window/display which hosts the graphics backend.
 *
 * <p>The engine and its systems talk to the display through this interface, so they do not need
 * to know whether the game runs on GLFW, Android or in a browser.</p>
 */
public interface Display {

    boolean shouldNotClose();

    int width();

    int height();

    float aspectRatio();

    void pollEvents();

    void swapBuffers();

    void setCursorMode(CursorMode mode);

    CursorMode getCursorMode();

    void destroy();
}
