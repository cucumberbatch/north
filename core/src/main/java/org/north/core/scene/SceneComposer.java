package org.north.core.scene;

import org.north.core.scene.component.management.ComponentManager;

public interface SceneComposer {
    void compose(EntityManager entityManager, ComponentManager componentManager);
}
