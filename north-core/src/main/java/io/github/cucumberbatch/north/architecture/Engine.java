package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.asset.AssetService;
import io.github.cucumberbatch.north.asset.SimpleAssetService;
import io.github.cucumberbatch.north.asset.loader.AudioAssetLoader;
import io.github.cucumberbatch.north.audio.Audio;
import io.github.cucumberbatch.north.audio.AudioClip;
import io.github.cucumberbatch.north.graphics.Display;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.glfw.Window;
import io.github.cucumberbatch.north.graphics.opengl.OpenGLGraphics;
import io.github.cucumberbatch.north.graphics.opengl.loader.OpenGLAssetLoaders;
import io.github.cucumberbatch.north.io.Input;
import io.github.cucumberbatch.north.io.LocalResourceLoader;
import io.github.cucumberbatch.north.io.ResourceLoader;
import io.github.cucumberbatch.north.io.remote.RemoteInputServer;
import org.lwjgl.glfw.GLFW;

/**
 * An entry point of all engine dependencies.
 *
 * <p>This class is the composition root of the desktop runtime for now. It wires the platform
 * implementations (GLFW window, OpenGL graphics, OpenAL audio) into the neutral APIs and
 * registers the backend specific asset loaders.</p>
 */
public class Engine {
    public final Window window;
    public final Graphics graphics;
    public final Audio audio;
    public final AssetService assetService;
    public final Pipeline pipeline;
    public final ApplicationContext context;
    public final RemoteInputServer remoteInputServer;

    public Engine() {
        context = new ApplicationContext();

        window = new Window();
        context.register(Window.class, window);
        context.register(Display.class, window);

        graphics = new OpenGLGraphics(window);
        context.register(Graphics.class, graphics);

        audio = new Audio();
        context.register(Audio.class, audio);

        assetService = new SimpleAssetService();
        OpenGLAssetLoaders.registerLoaders(assetService, graphics);
        ResourceLoader resourceLoader = new LocalResourceLoader();
        assetService.registerAssetLoader(AudioClip.class, new AudioAssetLoader(resourceLoader, audio));
        context.register(AssetService.class, assetService);

        pipeline = new Pipeline(context);
        context.register(Pipeline.class, pipeline);

        remoteInputServer = new RemoteInputServer(context);
        context.register(RemoteInputServer.class, remoteInputServer);

    }

    public void run() {
        try {
            window.init(graphics);
            installInputCallbacks();
            remoteInputServer.start();
            pipeline.run();
        } finally {
            pipeline.close();
            remoteInputServer.stop();
            window.destroy();
            audio.close();
        }
    }

    /**
     * Installs the GLFW input callbacks of the desktop backend.
     *
     * <p>The callbacks currently live in the {@link Input} class. They will move to the desktop
     * input wiring in a later step of the modularization.</p>
     */
    private void installInputCallbacks() {
        GLFW.glfwSetKeyCallback(window.handle(), new Input.KeyboardInputCallback());
        GLFW.glfwSetMouseButtonCallback(window.handle(), new Input.MouseInputCallback());
        GLFW.glfwSetCursorPosCallback(window.handle(), new Input.CursorPositionCallback());
    }

}
