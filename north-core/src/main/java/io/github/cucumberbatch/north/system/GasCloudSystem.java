package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.graphics.Material;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.scene.component.GasCloud;
import io.github.cucumberbatch.north.scene.component.MeshRenderer;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.UpdateProcess;

@ComponentHandler(GasCloud.class)
public class GasCloudSystem extends AbstractSystem<GasCloud> implements InitProcess<GasCloud>, UpdateProcess<GasCloud> {
    private static final String SPRITE_INDEX_UNIFORM = "u_sprite_index";
    private static final int SPRITE_COUNT = 6;
    private static final int LAST_SPRITE_INDEX = SPRITE_COUNT - 1;

    @Inject
    public GasCloudSystem(ApplicationContext context) {
        super(GasCloud.class, context);
    }

    @Override
    public void init(GasCloud gasCloud) throws RuntimeException {
        Entity entity = gasCloud.getEntity();

        MeshRenderer meshRenderer = cm.get(entity, MeshRenderer.class);
        Material material = meshRenderer.material;
        material.setInt(SPRITE_INDEX_UNIFORM, LAST_SPRITE_INDEX);
    }

    @Override
    public void update(GasCloud gasCloud, float deltaTime) {
        Entity entity = gasCloud.getEntity();

        MeshRenderer meshRenderer = cm.get(entity, MeshRenderer.class);
        Material material = meshRenderer.material;
        int spriteIndex = material.getInt(SPRITE_INDEX_UNIFORM);

        if (gasCloud.acc > 1f) {
            int nextSpriteIndex = spriteIndex - 1;
            if (nextSpriteIndex <= 0) {
                //todo: we need to completely remove entity,
                // so for now we just remove components of it
                cm.remove(entity, MeshRenderer.class);
                cm.remove(entity, GasCloud.class);

                // wierd operation, but anyway...
                entityManager.remove(entity);
                return;
            }

            material.setInt(SPRITE_INDEX_UNIFORM, nextSpriteIndex);
            gasCloud.acc = 0;
        }

        gasCloud.acc += deltaTime * 7f;
    }
}
