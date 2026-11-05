package org.north.core.system;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.north.core.graphics.Mesh;
import org.north.core.scene.SceneSpatialIndex;
import org.north.core.scene.component.MeshCollider;
import org.north.core.scene.component.MeshRenderer;
import org.north.core.scene.component.Transform;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.physics.shape.Rectangle;
import org.north.core.physics.spatial.Aabb;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.UpdateProcess;

@ComponentHandler(MeshCollider.class)
public class MeshColliderSystem extends AbstractSystem<MeshCollider>
        implements InitProcess<MeshCollider>, UpdateProcess<MeshCollider> {

    private static final Aabb UNIT_BOUNDS =
            new Aabb(-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f);

    private final SceneSpatialIndex spatialIndex;

    private final Vector3f position     = new Vector3f();
    private final Vector3f scale        = new Vector3f();
    private final Vector3f localCorner  = new Vector3f();
    private final Vector3f worldCorner  = new Vector3f();
    private final Vector3f min          = new Vector3f();
    private final Vector3f max          = new Vector3f();
    private final Matrix4f modelMatrix  = new Matrix4f();

    @Inject
    public MeshColliderSystem(ApplicationContext context) {
        super(MeshCollider.class, context);
        this.spatialIndex = context.getDependency(SceneSpatialIndex.class);
    }

    @Override
    public void init(MeshCollider meshCollider) {
        Transform transform = cm.get(meshCollider.getEntity(), Transform.class);
        if (transform == null) return;

        if (meshCollider.space == MeshCollider.Space.WORLD) {
            updateWorldBounds(meshCollider, transform);
            return;
        }

        if (meshCollider.body == null) {
            meshCollider.body = new Rectangle(new Vector2f(), new Vector2f());
        }

        if (meshCollider.body.isEmpty()) {
            transform.getGlobalScale(scale);
            float width  = Math.abs(scale.x);
            float height = Math.abs(scale.y);

            meshCollider.body.topLeft.set(-width / 2, -height / 2);
            meshCollider.body.bottomRight.set(width / 2, height / 2);
        }
    }

    @Override
    public void update(MeshCollider meshCollider, float deltaTime) {
        Transform transform = cm.get(meshCollider.getEntity(), Transform.class);
        if (transform == null) return;

        if (meshCollider.space == MeshCollider.Space.WORLD) {
            updateWorldBounds(meshCollider, transform);
            return;
        }

        if (meshCollider.body == null) return;

        transform.getGlobalPosition(position);
        meshCollider.body.moveTo(position.x, position.y);
    }

    private void updateWorldBounds(MeshCollider meshCollider, Transform transform) {
        Aabb localBounds = meshCollider.localBounds;
        if (localBounds == null || localBounds.isEmpty()) {
            localBounds = resolveLocalBounds(meshCollider);
            if (localBounds != null) {
                meshCollider.localBounds = localBounds;
            }
        }

        // Keep an empty localBounds when there is no mesh yet, so adding a MeshRenderer later can
        // still replace the fallback bounds on the next update.
        if (localBounds == null || localBounds.isEmpty()) {
            localBounds = UNIT_BOUNDS;
        }

        transform.getGlobalModelMatrix(modelMatrix);

        for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            float x = (cornerIndex & 1) == 0 ? localBounds.xMin() : localBounds.xMax();
            float y = (cornerIndex & 2) == 0 ? localBounds.yMin() : localBounds.yMax();
            float z = (cornerIndex & 4) == 0 ? localBounds.zMin() : localBounds.zMax();

            localCorner.set(x, y, z);
            modelMatrix.transformPosition(localCorner, worldCorner);

            if (cornerIndex == 0) {
                min.set(worldCorner);
                max.set(worldCorner);
            } else {
                min.min(worldCorner);
                max.max(worldCorner);
            }
        }

        if (!meshCollider.bounds.matches(min, max)) {
            meshCollider.bounds.setMinMax(min, max);
            spatialIndex.invalidate();
        }
    }

    private Aabb resolveLocalBounds(MeshCollider meshCollider) {
        MeshRenderer meshRenderer = cm.get(meshCollider.getEntity(), MeshRenderer.class);
        if (meshRenderer != null && meshRenderer.mesh != null) {
            Mesh mesh = meshRenderer.mesh;
            Aabb meshBounds = mesh.getBounds();
            if (!meshBounds.isEmpty()) {
                return new Aabb().set(meshBounds);
            }
        }
        return null;
    }

}
