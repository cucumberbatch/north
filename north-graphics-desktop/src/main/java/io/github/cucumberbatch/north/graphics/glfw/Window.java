package io.github.cucumberbatch.north.graphics.glfw;

import io.github.cucumberbatch.north.config.ApplicationProperties;
import io.github.cucumberbatch.north.graphics.CursorMode;
import io.github.cucumberbatch.north.graphics.Display;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.opengl.OpenGLWindowSizeCallback;
import io.github.cucumberbatch.north.utils.Stopwatch;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A GLFW window which implements the platform-neutral {@link Display} contract.
 */
public class Window implements Display {
    private static final Logger log = LoggerFactory.getLogger(Window.class);

    private final String  title;
    private final boolean vSync;

    private long handle;
    private int  width;
    private int  height;

    public Window() {
        this(
                ApplicationProperties.getProperty("application.window.title"),
                ApplicationProperties.getInt("application.window.width"),
                ApplicationProperties.getInt("application.window.height"),
                ApplicationProperties.getBoolean("application.window.vsync")
        );
    }

    public Window(String title, int width, int height, boolean vSync) {
        this.width  = width;
        this.height = height;
        this.title  = title;
        this.vSync  = vSync;

        this.handle = -1;
    }

    public String title()  { return title;  }
    public long   handle() { return handle; }

    @Override
    public int width()  { return width;  }

    @Override
    public int height() { return height; }

    @Override
    public float aspectRatio() {
        return (float) width / Math.max(1, height);
    }

    public void resize(int width, int height) {
        this.width  = width;
        this.height = height;
    }

    public void init(Graphics graphics) {
        Stopwatch.start();
        if (handle != -1) return;

        GLFW.glfwSetErrorCallback((code, message) -> log.error(String.format("err_code 0x%08X: %s ", code, message)));

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints(); // optional, the current window hints are already the default
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // the window will be resizable

        handle = GLFW.glfwCreateWindow(width, height, title, NULL, NULL);
        log.info("Created GLFW window handle: {}", Long.toHexString(handle));

        if (handle == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // The platform input callbacks are installed by the composition root, so this backend
        // does not have to know about the input abstraction.
        GLFW.glfwSetWindowSizeCallback(handle, new OpenGLWindowSizeCallback(graphics));

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*
            GLFW.glfwGetWindowSize(handle, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

            if (videoMode == null)
                throw new RuntimeException("Video mode is not found!");

            // Center the window
            GLFW.glfwSetWindowPos(
                    handle,
                    (videoMode.width() - pWidth.get(0)) / 2 * 8 / 5,
                    (videoMode.height() - pHeight.get(0)) / 2
            );
        }
        // the stack frame is popped automatically
        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(handle);
        // Enable v-sync
        GLFW.glfwSwapInterval(vSync ? 1 : 0);

        GLFW.glfwShowWindow(handle);
        GL.createCapabilities();

        GL30.glClearColor(1f, 1f, 1f, 1f);

        GL30.glClearDepth(1f);
        GL30.glDepthRange(0f, 1f);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
        GL30.glDepthFunc(GL11.GL_LEQUAL);

        // enable blending
        GL30.glEnable(GL30.GL_BLEND);
        GL30.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE);

        Stopwatch.stop("Window initialization completed");
    }

    @Override
    public void destroy() {
        log.info("Window destruction process started");

        if (handle == -1) return;

        Callbacks.glfwFreeCallbacks(handle);
        GLFW.glfwDestroyWindow(handle);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();

        log.info("Window destruction process ended");
    }

    @Override
    public boolean shouldNotClose() {
        return !GLFW.glfwWindowShouldClose(handle);
    }

    @Override
    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    @Override
    public void swapBuffers() {
        GLFW.glfwSwapBuffers(handle);
    }

    @Override
    public void setCursorMode(CursorMode mode) {
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, toGlfwCursorMode(mode));
    }

    @Override
    public CursorMode getCursorMode() {
        return fromGlfwCursorMode(GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR));
    }

    private static int toGlfwCursorMode(CursorMode mode) {
        if (mode == null) return GLFW.GLFW_CURSOR_NORMAL;

        switch (mode) {
            case HIDDEN:
                return GLFW.GLFW_CURSOR_HIDDEN;
            case DISABLED:
                return GLFW.GLFW_CURSOR_DISABLED;
            case NORMAL:
            default:
                return GLFW.GLFW_CURSOR_NORMAL;
        }
    }

    private static CursorMode fromGlfwCursorMode(int glfwMode) {
        switch (glfwMode) {
            case GLFW.GLFW_CURSOR_HIDDEN:
                return CursorMode.HIDDEN;
            case GLFW.GLFW_CURSOR_DISABLED:
                return CursorMode.DISABLED;
            default:
                return CursorMode.NORMAL;
        }
    }
}
