package org.north.core.architecture;

import org.north.core.audio.Audio;
import org.north.core.scene.component.management.ComponentManager;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Window;
import org.north.core.system.SystemManager;
import org.north.core.system.remote.RemoteInputServer;

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
