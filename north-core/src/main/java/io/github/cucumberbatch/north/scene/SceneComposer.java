package io.github.cucumberbatch.north.scene;

import io.github.cucumberbatch.north.scene.component.management.ComponentManager;

public interface SceneComposer {
    void compose(EntityManager entityManager, ComponentManager componentManager);
}
