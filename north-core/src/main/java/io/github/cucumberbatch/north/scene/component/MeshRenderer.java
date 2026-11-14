package io.github.cucumberbatch.north.scene.component;

import io.github.cucumberbatch.north.graphics.Material;
import io.github.cucumberbatch.north.graphics.Mesh;
import io.github.cucumberbatch.north.graphics.PredefinedMeshes;

public class MeshRenderer extends AbstractComponent {
    /** Screen-space renderers use window pixel coordinates instead of camera projections. */
    public static final int WORLD_RENDER_LAYER  = 0;
    public static final int SCREEN_RENDER_LAYER = 100;

    public Mesh mesh = PredefinedMeshes.QUAD;
    public Material material;

    /**
     * When true, the entity transform is interpreted as a position and size in window pixels.
     * Screen-space meshes are drawn after all world-space meshes.
     */
    public boolean screenSpace = false;

    /** Additional ordering inside the same render layer. Higher values are drawn later. */
    public int renderOrder = 0;

    @Override
    public int getRenderLayer() {
        return screenSpace ? SCREEN_RENDER_LAYER + renderOrder : WORLD_RENDER_LAYER;
    }
}
