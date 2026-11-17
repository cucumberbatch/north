package io.github.cucumberbatch.north.graphics.opengl;

import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import io.github.cucumberbatch.north.graphics.Graphics;

public class OpenGLWindowSizeCallback implements GLFWWindowSizeCallbackI {
    private final Graphics graphics;

    public OpenGLWindowSizeCallback(Graphics graphics) {
        this.graphics = graphics;
    }

    @Override
    public void invoke(long window, int width, int height) {
        graphics.updateViewport(0, 0, width, height);
    }
}
