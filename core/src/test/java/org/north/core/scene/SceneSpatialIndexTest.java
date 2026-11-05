package org.north.core.scene;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.north.core.physics.spatial.Aabb;
import org.north.core.scene.component.MeshCollider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class SceneSpatialIndexTest {

    @Test
    void indexesOnlyWorldColliders() {
        MeshCollider world = new MeshCollider();
        world.space = MeshCollider.Space.WORLD;
        world.bounds.setMinMax(-0.25f, 0.25f, -0.25f, 0.25f, -0.25f, 0.25f);

        MeshCollider screen = new MeshCollider();
        screen.space = MeshCollider.Space.SCREEN;
        screen.bounds.setMinMax(-0.25f, 0.25f, -0.25f, 0.25f, -0.25f, 0.25f);

        SceneSpatialIndex index = new SceneSpatialIndex();
        index.rebuildIfDirty(Arrays.asList(world, screen));

        List<MeshCollider> result = new ArrayList<>();
        for (MeshCollider collider : index.query(new Aabb(-0.1f, 0.1f, -0.1f, 0.1f, -0.1f, 0.1f))) {
            result.add(collider);
        }

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(world, result.get(0));
    }
}
