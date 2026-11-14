package io.github.cucumberbatch.north.system;

import org.joml.Vector4f;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.exception.ComponentNotFoundException;
import io.github.cucumberbatch.north.graphics.Material;
import io.github.cucumberbatch.north.graphics.PredefinedMeshes;
import io.github.cucumberbatch.north.graphics.shader.Shader;
import io.github.cucumberbatch.north.scene.component.Button;
import io.github.cucumberbatch.north.scene.component.MeshRenderer;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.UpdateProcess;

@ComponentHandler(Button.class)
public class ButtonSystem extends AbstractSystem<Button>
        implements InitProcess<Button>, UpdateProcess<Button> {

    private static final String SHADER_PATH = "assets/shaders/simple_color_shader";

    @Inject
    public ButtonSystem(ApplicationContext context) {
        super(Button.class, context);
    }

    @Override
    public void init(Button button) {
        MeshRenderer renderer = cm.get(button.getEntity(), MeshRenderer.class);
        if (renderer == null) {
            throw new ComponentNotFoundException(MeshRenderer.class);
        }

        renderer.mesh = PredefinedMeshes.QUAD;
        renderer.screenSpace = button.screenSpace;
        renderer.renderOrder = button.renderOrder;

        if (renderer.material == null) {
            Shader shader = assetService.get(Shader.class, SHADER_PATH);
            renderer.material = new Material(shader);
        }

        // Keep the property reference stable: the system mutates it every frame instead of
        // replacing the whole material property.
        renderer.material.setVector("u_color", button.buttonColor);
    }

    @Override
    public void update(Button button, float deltaTime) {
        MeshRenderer renderer = cm.get(button.getEntity(), MeshRenderer.class);
        if (renderer == null || renderer.material == null || renderer.mesh == null) return;

        Vector4f target = button.hovered ? button.buttonOnHoverColor : button.buttonDefaultColor;
        float ratio = 1f - (float) Math.exp(-button.colorTransitionSpeed * deltaTime);
        button.buttonColor.lerp(target, Math.min(1f, Math.max(0f, ratio)));

        renderer.material.setVector("u_color", button.buttonColor);
    }

}
