package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.audio.Audio;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.OpenGLGraphics;
import io.github.cucumberbatch.north.graphics.Window;
import io.github.cucumberbatch.north.io.remote.RemoteInputServer;

/**
 * An entry point of all engine dependencies
 */
public class Engine {
    public final Window window;
    public final Graphics graphics;
    public final Audio audio;
    public final Pipeline pipeline;
    public final ApplicationContext context;
    public final RemoteInputServer remoteInputServer;

    public Engine() {
        context = new ApplicationContext();

        window = new Window();
        context.addDependency(Window.class, window);

        graphics = new OpenGLGraphics(window);
        context.addDependency(Graphics.class, graphics);

        audio = new Audio();
        context.addDependency(Audio.class, audio);

        pipeline = new Pipeline(context);
        context.addDependency(Pipeline.class, pipeline);

        remoteInputServer = new RemoteInputServer(context);
        context.addDependency(RemoteInputServer.class, remoteInputServer);

    }

    public void run() {
        try {
            window.init(graphics);
            remoteInputServer.start();
            pipeline.run();
        } finally {
            pipeline.close();
            remoteInputServer.stop();
            window.destroy(graphics);
            audio.close();
        }
    }

}
