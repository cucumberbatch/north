package io.github.cucumberbatch.north.scene.component.management;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.scene.EntityManager;
import io.github.cucumberbatch.north.scene.command.CommandApplier;
import io.github.cucumberbatch.north.scene.component.AbstractComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentManagerTest {

    public static class TestComponent extends AbstractComponent {
    }

    private ComponentManager componentManager;
    private CommandApplier commandApplier;
    private Entity entity;

    @BeforeEach
    void setUp() {
        ApplicationContext context = new ApplicationContext();
        componentManager = context.resolve(ComponentManager.class);
        commandApplier = context.resolve(CommandApplier.class);
        entity = new EntityManager(componentManager).create("test-entity");
    }

    @Test
    void addingComponentIsDeferredUntilCommandsAreApplied() {
        TestComponent component = componentManager.add(entity, TestComponent.class);

        assertNotNull(component);
        assertSame(entity, component.getEntity());
        assertFalse(componentManager.has(entity, TestComponent.class));

        commandApplier.applyDeferredCommands();

        assertTrue(componentManager.has(entity, TestComponent.class));
        assertSame(component, componentManager.get(entity, TestComponent.class));
    }

    @Test
    void removingComponentIsDeferredUntilCommandsAreApplied() {
        TestComponent component = componentManager.add(entity, TestComponent.class);
        commandApplier.applyDeferredCommands();

        assertSame(component, componentManager.get(entity, TestComponent.class));

        componentManager.remove(entity, TestComponent.class);

        assertTrue(componentManager.has(entity, TestComponent.class));

        commandApplier.applyDeferredCommands();

        assertFalse(componentManager.has(entity, TestComponent.class));
    }

    @Test
    void getAllByTypeReturnsRegisteredComponents() {
        componentManager.add(entity, TestComponent.class);
        commandApplier.applyDeferredCommands();

        assertEquals(1, componentManager.getAllByType(TestComponent.class).size());
    }
}
