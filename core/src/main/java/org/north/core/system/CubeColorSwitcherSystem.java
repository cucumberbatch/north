package org.north.core.system;

import org.joml.Vector4f;
import org.north.core.scene.Entity;
import org.north.core.scene.component.CubeColorSwitcher;
import org.north.core.scene.component.MeshRenderer;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.system.process.UpdateProcess;

import java.util.Random;

@ComponentHandler(CubeColorSwitcher.class)
public class CubeColorSwitcherSystem extends AbstractSystem<CubeColorSwitcher> implements UpdateProcess<CubeColorSwitcher> {
    private final Random rnd = new Random();
    private static final float MAX_ACC_VALUE = 1.2f;
    private static final Vector4f turnedOffCubeColor = new Vector4f(1f, 1f, 1f, 0.0125f);
    private static final Vector4f turnedOnCubeColor = new Vector4f(0f, 0f, 0f, .01f);

    @Inject
    public CubeColorSwitcherSystem(ApplicationContext context) {
        super(CubeColorSwitcher.class, context);
    }

    @Override
    public void update(CubeColorSwitcher component, float deltaTime) {
        /*if (component.switched) {
            component.getEntity().get(MeshRenderer.class).color.set(0.75f, 0.75f, 0.75f, 0f);
            component.switched = false;
            return;
        } else*/
        Entity entity = component.getEntity();
        MeshRenderer renderer = cm.get(entity, MeshRenderer.class);
        Vector4f color = renderer.material.getVector4("u_color");
        if (component.acc > MAX_ACC_VALUE) {
//            float r = rnd.nextFloat();
//            float g = rnd.nextFloat();
//            float b = rnd.nextFloat();
//            color.set(r, g, b, 1f);

            color.set(turnedOnCubeColor);
            component.acc = 0f;
            component.switched = true;
        } else if (component.acc > MAX_ACC_VALUE * ((float) 8 / 9)) {
            // a bit of smoothness
            color.lerp(turnedOnCubeColor, deltaTime * 10);
        } else if (component.switched) {
//            Vector4f color = color;
//            color.set(color.x,color.y, color.z, color.w * 0.786f);
            color.lerp(turnedOffCubeColor, deltaTime * 10);
//            color.set(targetVec);

//            Transform transform = component.getTransform();
//            float factor = (MAX_ACC_VALUE - component.acc) / MAX_ACC_VALUE;
//            transform.rescaleTo(0.499f * factor, 0.499f * factor, 0.499f * factor);
        }
        component.acc += deltaTime;
    }
}
