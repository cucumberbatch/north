package org.north.core.scene;

import org.north.core.physics.spatial.Aabb;
import org.north.core.physics.spatial.Octree;
import org.north.core.scene.component.MeshCollider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Broad-phase index for world-space colliders.
 *
 * <p>The index is rebuilt lazily when a collider's world bounds change. It deliberately owns the
 * octree instead of exposing it: the octree is an implementation detail, while the rest of the
 * engine can ask scene-level questions such as "which colliders are near this box?".</p>
 */
public class SceneSpatialIndex {

    private static final float MIN_MARGIN = 0.5f;

    private final Map<Aabb, MeshCollider> colliderByBounds = new IdentityHashMap<>();

    private Octree<Aabb> octree;
    private boolean dirty = true;
    private int indexedColliderCount = -1;

    public void invalidate() {
        dirty = true;
    }

    public boolean isEmpty() {
        return octree == null;
    }

    public void rebuildIfDirty(Collection<MeshCollider> colliders) {
        int colliderCount = colliders != null ? colliders.size() : 0;
        if (!dirty && indexedColliderCount == colliderCount) return;

        dirty = false;
        indexedColliderCount = colliderCount;
        octree = null;
        colliderByBounds.clear();

        if (colliders == null || colliders.isEmpty()) return;

        List<MeshCollider> indexed = new ArrayList<>();
        float minX = 0f, maxX = 0f;
        float minY = 0f, maxY = 0f;
        float minZ = 0f, maxZ = 0f;
        boolean hasBounds = false;

        for (MeshCollider collider : colliders) {
            if (!isIndexable(collider)) continue;

            Aabb bounds = collider.bounds;
            if (!hasBounds) {
                minX = bounds.xMin();
                maxX = bounds.xMax();
                minY = bounds.yMin();
                maxY = bounds.yMax();
                minZ = bounds.zMin();
                maxZ = bounds.zMax();
                hasBounds = true;
            } else {
                minX = Math.min(minX, bounds.xMin());
                maxX = Math.max(maxX, bounds.xMax());
                minY = Math.min(minY, bounds.yMin());
                maxY = Math.max(maxY, bounds.yMax());
                minZ = Math.min(minZ, bounds.zMin());
                maxZ = Math.max(maxZ, bounds.zMax());
            }

            indexed.add(collider);
        }

        if (indexed.isEmpty()) return;

        float largestExtent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        float margin = Math.max(MIN_MARGIN, largestExtent * 0.1f);

        octree = new Octree<>(
                minX - margin, maxX + margin,
                minY - margin, maxY + margin,
                minZ - margin, maxZ + margin
        );

        for (MeshCollider collider : indexed) {
            if (octree.add(collider.bounds)) {
                colliderByBounds.put(collider.bounds, collider);
            }
        }

        if (colliderByBounds.isEmpty()) {
            octree = null;
        }
    }

    /**
     * Returns colliders whose bounds intersect the query box. The result is a broad-phase
     * candidate set; callers still have to perform an exact test for their query shape.
     */
    public Iterable<MeshCollider> query(Aabb queryBox) {
        if (octree == null || queryBox == null) {
            return Collections.emptyList();
        }

        List<MeshCollider> result = new ArrayList<>();
        for (Aabb bounds : octree.query(queryBox)) {
            MeshCollider collider = colliderByBounds.get(bounds);
            if (collider != null) {
                result.add(collider);
            }
        }
        return result;
    }

    private boolean isIndexable(MeshCollider collider) {
        return collider != null
                && collider.isActive()
                && collider.space == MeshCollider.Space.WORLD
                && collider.selectable
                && !collider.bounds.isEmpty();
    }
}
