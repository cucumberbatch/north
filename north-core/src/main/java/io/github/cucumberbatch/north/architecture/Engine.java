package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.audio.Audio;
import io.github.cucumberbatch.north.scene.component.management.ComponentManager;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.Window;
import io.github.cucumberbatch.north.system.SystemManager;
import io.github.cucumberbatch.north.system.remote.RemoteInputServer;

/**
 * An entry point of all engine dependencies
 */
public class Engine {
    public final Window window;
    public final Graphics graphics;
    public final Audio audio;
    public final Pipeline pipeline;
    public final ApplicationContext context;
    public final SystemManager systemManager;
    public final ComponentManager componentManager;
    public final RemoteInputServer remoteInputServer;

    public Engine() throws Exception {
        context = new ApplicationContext();

        window = new Window();
        context.addDependency(Window.class, window);

        graphics = new Graphics(window);
        context.addDependency(Graphics.class, graphics);

        audio = new Audio();
        context.addDependency(Audio.class, audio);

        systemManager = new SystemManager(context);
        context.addDependency(SystemManager.class, systemManager);

        componentManager = new ComponentManager(context);
        context.addDependency(ComponentManager.class, componentManager);

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
