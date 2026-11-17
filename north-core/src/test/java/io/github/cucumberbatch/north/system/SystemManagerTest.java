package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.scene.component.management.ComponentManager;
import io.github.cucumberbatch.north.system.process.CollisionHandlerProcess;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.InputHandlerProcess;
import io.github.cucumberbatch.north.system.process.RenderProcess;
import io.github.cucumberbatch.north.system.process.UpdateProcess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemManagerTest {

    @Test
    void initializesProcessQueuesForEveryProcessType() {
        ApplicationContext context = new ApplicationContext();
        context.getDependency(ComponentManager.class);
        SystemManager systemManager = context.getDependency(SystemManager.class);

        assertNotNull(systemManager);
        assertNotNull(systemManager.getProcessList(InitProcess.class));
        assertNotNull(systemManager.getProcessList(UpdateProcess.class));
        assertNotNull(systemManager.getProcessList(InputHandlerProcess.class));
        assertNotNull(systemManager.getProcessList(RenderProcess.class));
        assertNotNull(systemManager.getProcessList(CollisionHandlerProcess.class));
        assertTrue(systemManager.getProcessList(UpdateProcess.class).isEmpty());
    }

    @Test
    void doesNotInstantiateSystemsForComponentsWithoutHandler() {
        ApplicationContext context = new ApplicationContext();
        context.getDependency(ComponentManager.class);
        SystemManager systemManager = context.getDependency(SystemManager.class);

        assertNull(systemManager.getSystemByComponentType(Transform.class));
    }
}
