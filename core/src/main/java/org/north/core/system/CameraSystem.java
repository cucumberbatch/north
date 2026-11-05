package org.north.core.system;

import org.north.core.architecture.ApplicationContext;
import org.north.core.graphics.Graphics;
import org.north.core.scene.component.Camera;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.RenderProcess;
import org.joml.Matrix4f;


@ComponentHandler(Camera.class)
public class CameraSystem extends AbstractSystem<Camera>
        implements InitProcess<Camera>, RenderProcess<Camera> {

    public static Matrix4f PERSPECTIVE_MATRIX  = new Matrix4f();
    public static Matrix4f INTERMEDIATE_MATRIX = new Matrix4f();
    public static Matrix4f ORTHOGRAPHIC_MATRIX = new Matrix4f();

    private final CameraAwareService cameraAwareService;
    private final Graphics graphics;

    @Inject
    public CameraSystem(ApplicationContext context) {
        super(Camera.class, context);
        cameraAwareService = context.getDependency(CameraAwareService.class);
        graphics = context.getDependency(Graphics.class);
    }

    @Override
    public void init(Camera camera) throws RuntimeException {
        camera.ratio = (float) graphics.window.getWidth() / Math.max(1, graphics.window.getHeight());
        updateProjectionMatrices(camera);

        camera.eye.set(0f, -1f, 0f);
        camera.up.set(0f, 1f, 0f);
        camera.at.set(1.0f, 0.0f, -1.0f).add(camera.eye);
        camera.projectionMatrix = PERSPECTIVE_MATRIX;

        cameraAwareService.setCameraComponent(camera);
    }

    /**
     * Rebuilds the projection matrices from the camera parameters.
     *
     * <p>This must be called whenever the viewport aspect ratio changes. The matrices are stored
     * statically for the existing projection transition code, but the camera's current projection
     * matrix also has to be refreshed by the caller.</p>
     */
    public static void updateProjectionMatrices(Camera camera) {
        PERSPECTIVE_MATRIX   = new Matrix4f().perspective(camera.angle, camera.ratio, camera.near, camera.far);
        INTERMEDIATE_MATRIX  = new Matrix4f().perspective(camera.angle * 1.125f, camera.ratio, camera.near, camera.far);
        ORTHOGRAPHIC_MATRIX  = new Matrix4f().ortho(-camera.ratio, camera.ratio, -1f, 1f, -1f, 1f);
    }

    @Override
    public void render(Camera camera, Graphics graphics) {
//        graphics.drawMesh(PredefinedMeshes.CUBE, new Vector4f(1, 1, 1, 1), component.transform);
    }

}
