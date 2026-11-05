package org.north.core.graphics;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.joml.Vector2f;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.north.core.config.ApplicationProperties;
import org.north.core.system.CameraControlsSystem;
import org.north.core.system.Input;
import org.north.core.utils.Stopwatch;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private static final Logger log = LoggerFactory.getLogger(Window.class);

    public final String title;
    private static int width;
    private static int height;

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public static void setSize(int newWidth, int newHeight) {
        width  = newWidth;
        height = newHeight;
    }

    private long window = -1;
    private final boolean vSync;

    public Window() {
        this(
                ApplicationProperties.getProperty("application.window.title"),
                ApplicationProperties.getInt("application.window.width"),
                ApplicationProperties.getInt("application.window.height"),
                ApplicationProperties.getBoolean("application.window.vsync")
        );
    }

    public Window(String title, int width, int height, boolean vSync) {
        this.width = width;
        this.height = height;
        this.title = title;
        this.vSync = vSync;
    }

    public void init(Graphics graphics) {
        Stopwatch.start();
        if (window != -1) return;

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

        window = GLFW.glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setting up input callback handlers
        GLFW.glfwSetKeyCallback(window, new Input.KeyboardInputCallback());
        GLFW.glfwSetMouseButtonCallback(window, new Input.MouseInputCallback());
        GLFW.glfwSetCursorPosCallback(window, new Input.CursorPositionCallback());
        GLFW.glfwSetWindowSizeCallback(window, new CameraControlsSystem.WindowSizeCallback(graphics));


//        GLFW.glfwSetWindowRefreshCallback(window, new GLFWWindowRefreshCallback() {
//            @Override
//            public void invoke(long l) {
//                 log.info(String.format("Refreshed window[%s] value[%s]", window, l));
//            }
//        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*
            GLFW.glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

            if (videoMode == null)
                throw new RuntimeException("Video mode is not found!");

            // Center the window
            GLFW.glfwSetWindowPos(
                    window,
                    (videoMode.width() - pWidth.get(0)) / 2 * 8 / 5,
                    (videoMode.height() - pHeight.get(0)) / 2
            );
        }
        // the stack frame is popped automatically
        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(window);
        // Enable v-sync
        GLFW.glfwSwapInterval(vSync ? 1 : 0);

        GLFW.glfwShowWindow(window);
        GL.createCapabilities();


//        GL30.glClearColor(0f, 0f, 0f, 1f);
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

    public void destroy(Graphics graphics) {
        log.info("Window destruction process started");

        if (window == -1) return;

        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();

        log.info("Window destruction process ended");
    }

    public static Vector2f translatePointToWindow(Vector2f point) {
        return new Vector2f(
            -1 + point.x / width  * 2,
             1 - point.y / height * 2);
    }

    public long getWindow() {
        return window;
    }

    public boolean shouldNotClose() {
        return !GLFW.glfwWindowShouldClose(window);
    }
}
