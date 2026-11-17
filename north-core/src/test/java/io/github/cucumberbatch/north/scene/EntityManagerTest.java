package io.github.cucumberbatch.north.scene;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.scene.component.management.ComponentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityManagerTest {

    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        ApplicationContext context = new ApplicationContext();
        ComponentManager componentManager = context.resolve(ComponentManager.class);
        entityManager = new EntityManager(componentManager);
    }

    @Test
    void createsRootEntityOnConstruction() {
        assertEquals(1, entityManager.size());
        assertSame(entityManager.getRoot(), entityManager.find("root"));
    }

    @Test
    void createsAndFindsEntitiesByName() {
        Entity player = entityManager.create("player");

        assertEquals(2, entityManager.size());
        assertSame(player, entityManager.find("player"));
        assertSame(player, entityManager.findFirst(entity -> entity.getId() == player.getId()));
        assertTrue(entityManager.getAll().contains(player));
    }

    @Test
    void removesEntities() {
        Entity player = entityManager.create("player");

        assertTrue(entityManager.remove(player));
        assertFalse(entityManager.getAll().contains(player));
        assertNull(entityManager.find("player"));
        assertFalse(entityManager.remove(player));
    }
}
