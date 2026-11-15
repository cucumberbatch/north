package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.graphics.Graphics;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.graphics.Material;
import io.github.cucumberbatch.north.graphics.Mesh;
import io.github.cucumberbatch.north.scene.component.MeshRenderer;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.system.process.RenderProcess;

@ComponentHandler(MeshRenderer.class)
public class MeshRendererSystem
        extends AbstractSystem<MeshRenderer>
        implements RenderProcess<MeshRenderer> {

    private final Matrix4f bufferedMatrix = new Matrix4f();

    @Inject
    public MeshRendererSystem(ApplicationContext context) {
        super(MeshRenderer.class, context);
    }

    @Override
    public void render(MeshRenderer meshRenderer, Graphics graphics) {
        Mesh mesh = meshRenderer.mesh;
        Material material = meshRenderer.material;
        if (mesh == null || material == null) return;

        Transform transform = cm.get(meshRenderer.getEntity(), Transform.class);

        mesh.upload(graphics);
        Matrix4f modelMatrix = transform.getGlobalModelMatrix(bufferedMatrix);

        if (meshRenderer.screenSpace) {
            graphics.updateScreenProjection();
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            try {
                material.apply(graphics, modelMatrix, graphics.screenProjection(), graphics.screenView());
                graphics.drawIndexed(mesh.vao(), mesh.indexCount());
            } finally {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            return;
        }

        material.apply(graphics, modelMatrix);
        graphics.drawIndexed(mesh.vao(), mesh.indexCount());
    }
}
