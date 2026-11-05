package org.north.core.system;

import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.scene.component.Camera;
import org.north.core.scene.component.CameraControls;
import org.north.core.scene.component.Transform;
import org.north.core.config.ApplicationProperties;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Window;
import org.north.core.system.process.InputHandlerProcess;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.UpdateProcess;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.opengl.GL30;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Queue;

import static java.lang.Math.PI;

@ComponentHandler(CameraControls.class)
public class CameraControlsSystem extends AbstractSystem<CameraControls>
        implements InitProcess<CameraControls>, InputHandlerProcess<CameraControls>, UpdateProcess<CameraControls> { //}, RenderProcess<CameraControls> {

    private static final float MAX_CAMERA_ANGLE = 89.986f;

    private static ProjectionState projectionState = ProjectionState.ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE;
    private static Camera camera;
    private final Graphics graphics;
    private final boolean intermediateCameraStateOnFocusLossEnabled;

    //todo implement camera trace effect
    private final Queue<Vector3f> cameraTrace = new ArrayDeque<>();

    private float projectionProgress = 0f;
    private float cameraMovementSpeed = 0f;
    private float cameraDragFactor = 0f;
    private float verticalMousePosition = 0f;

    private boolean mouseCaptured = false;

    private boolean escapeIsPressed = false;
    private boolean mouseLeftButtonIsPressed = false;
    private boolean key1IsPressed = false;
    private boolean leftShiftKeyIsHeld = false;
    private boolean keyQIsHeld = false;
    private boolean keyEIsHeld = false;
    private boolean keyWIsHeld = false;
    private boolean keySIsHeld = false;
    private boolean keyAIsHeld = false;
    private boolean keyDIsHeld = false;

    private Vector2f lastCursorPosition;

    public enum ProjectionState {
        PERSPECTIVE_VIEW_STATE,
        PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE,
        PERSPECTIVE_TO_INTERMEDIATE_VIEW_STATE,
        ORTHOGRAPHIC_VIEW_STATE,
        ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE,
        ORTHOGRAPHIC_TO_INTERMEDIATE_VIEW_STATE,
        INTERMEDIATE_VIEW_STATE,
        INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE,
        INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE,
    }

    @Inject
    public CameraControlsSystem(ApplicationContext context) throws ReflectiveOperationException {
        super(CameraControls.class, context);
        this.graphics = context.getDependency(Graphics.class);
        this.intermediateCameraStateOnFocusLossEnabled =
                ApplicationProperties.getBoolean("application.editor.camera.intermediate-state-on-focus-loss");
    }

    @Override
    public void init(CameraControls cameraControls) {
        camera = cm.get(cameraControls.entity, Camera.class);
    }

    @Override
    public void handleInput(CameraControls component, Input input) {
        escapeIsPressed = input.isPressed(GLFW.GLFW_KEY_ESCAPE);
        mouseLeftButtonIsPressed = input.isPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        key1IsPressed = input.isPressed(GLFW.GLFW_KEY_1);
        leftShiftKeyIsHeld = input.isHeld(GLFW.GLFW_KEY_LEFT_SHIFT);

        keyQIsHeld = input.isHeld(GLFW.GLFW_KEY_Q);
        keyEIsHeld = input.isHeld(GLFW.GLFW_KEY_E);

        keyWIsHeld = input.isHeld(GLFW.GLFW_KEY_W);
        keySIsHeld = input.isHeld(GLFW.GLFW_KEY_S);
        keyAIsHeld = input.isHeld(GLFW.GLFW_KEY_A);
        keyDIsHeld = input.isHeld(GLFW.GLFW_KEY_D);

        lastCursorPosition = input.getCursorPosition();
    }

    @Override
    public void update(CameraControls cameraControls, float deltaTime) {
        updateScreenCapture(graphics);
        updateCameraProjection(graphics, deltaTime);
        updateCameraMovement(cameraControls, deltaTime);
    }

//    @Override
//    public void render(CameraControls cameraControls, Graphics graphics) {
//
//    }

    private ProjectionState lastProjectionStateBeforeFocusLoss = ProjectionState.PERSPECTIVE_VIEW_STATE;

    private void updateScreenCapture(Graphics graphics) {
        if (escapeIsPressed) {
            GLFW.glfwSetInputMode(graphics.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);

            if (intermediateCameraStateOnFocusLossEnabled) {
                switch (projectionState) {
                    case INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE:
                    case ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE:
                    case PERSPECTIVE_VIEW_STATE:
                        projectionState = ProjectionState.PERSPECTIVE_TO_INTERMEDIATE_VIEW_STATE;
                        projectionProgress = 0f;
                        lastProjectionStateBeforeFocusLoss = ProjectionState.PERSPECTIVE_VIEW_STATE;
                        break;
                    case INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE:
                    case PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE:
                    case ORTHOGRAPHIC_VIEW_STATE:
                        projectionState = ProjectionState.ORTHOGRAPHIC_TO_INTERMEDIATE_VIEW_STATE;
                        projectionProgress = 0f;
                        lastProjectionStateBeforeFocusLoss = ProjectionState.ORTHOGRAPHIC_VIEW_STATE;
                        break;
                    default:
                        break;
                }
            }
            mouseCaptured = false;
        }

        if (mouseLeftButtonIsPressed && !mouseCaptured) {
            GLFW.glfwSetInputMode(graphics.getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

            if (intermediateCameraStateOnFocusLossEnabled) {
                switch (lastProjectionStateBeforeFocusLoss) {
                    case PERSPECTIVE_TO_INTERMEDIATE_VIEW_STATE:
                    case PERSPECTIVE_VIEW_STATE:
                        projectionState = ProjectionState.INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE;
                        projectionProgress = 0f;
                        break;
                    case ORTHOGRAPHIC_TO_INTERMEDIATE_VIEW_STATE:
                    case ORTHOGRAPHIC_VIEW_STATE:
                        projectionState = ProjectionState.INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE;
                        projectionProgress = 0f;
                        break;
                    default:
                        break;
                }
            }

            previousCursorPosition = lastCursorPosition;
            cursorPositionDifference = new Vector2f();
            mouseCaptured = true;
        }
    }

    private final float PROJECTION_PROGRESS_STEP = 2f;
    private final double HALF_PI = PI / 2;
    private final double SIN_OF_HALF_PI = Math.sin(HALF_PI);

    private void updateCameraProjection(Graphics graphics, float deltaTime) {
        updateViewportProjection(graphics);

        if (key1IsPressed) {
            switch (projectionState) {
                case ORTHOGRAPHIC_VIEW_STATE:
                case INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE:
                case PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE:
                    projectionState = ProjectionState.ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE;
                    projectionProgress = 0f;
                    break;

                case PERSPECTIVE_VIEW_STATE:
                case INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE:
                case ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE:
                    projectionState = ProjectionState.PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE;
                    projectionProgress = 0f;
                    break;
                default:
                    break;
            }
        }

        switch (projectionState) {
            case INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE:
            case PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE: {
                projectionProgress += PROJECTION_PROGRESS_STEP * deltaTime;
                camera.projectionMatrix = new Matrix4f(camera.projectionMatrix).lerp(CameraSystem.ORTHOGRAPHIC_MATRIX, (float) (Math.sin(projectionProgress) / SIN_OF_HALF_PI));
                if (projectionProgress > HALF_PI) {
                    projectionProgress = 0;
                    projectionState = ProjectionState.ORTHOGRAPHIC_VIEW_STATE;
                }
                break;
            }
            case INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE:
            case ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE: {
                projectionProgress += PROJECTION_PROGRESS_STEP * deltaTime;
                camera.projectionMatrix = new Matrix4f(camera.projectionMatrix).lerp(CameraSystem.PERSPECTIVE_MATRIX, (float) (Math.sin(projectionProgress) / SIN_OF_HALF_PI));
                if (projectionProgress > HALF_PI) {
                    projectionProgress = 0;
                    projectionState = ProjectionState.PERSPECTIVE_VIEW_STATE;
                }
                break;
            }
            case ORTHOGRAPHIC_TO_INTERMEDIATE_VIEW_STATE:
            case PERSPECTIVE_TO_INTERMEDIATE_VIEW_STATE: {
                projectionProgress += PROJECTION_PROGRESS_STEP * deltaTime;
                camera.projectionMatrix = new Matrix4f(camera.projectionMatrix).lerp(CameraSystem.INTERMEDIATE_MATRIX, (float) (Math.sin(projectionProgress) / SIN_OF_HALF_PI));
                if (projectionProgress > HALF_PI) {
                    projectionProgress = 0;
                    projectionState = ProjectionState.INTERMEDIATE_VIEW_STATE;
                }
                break;
            }
            default:
                break;
        }
        graphics.projection = camera.projectionMatrix;
    }

    /**
     * Keeps the camera projection in sync with the actual window aspect ratio. Rebuilding the
     * static matrices alone is not enough: {@link #updateCameraProjection} assigns
     * {@code camera.projectionMatrix} to {@code graphics.projection} every frame, so this matrix
     * has to be refreshed as well after a resize/fullscreen transition.
     */
    private void updateViewportProjection(Graphics graphics) {
        if (camera == null) return;

        int width  = graphics.window.getWidth();
        int height = graphics.window.getHeight();
        if (width <= 0 || height <= 0) return;

        float aspect = (float) width / height;
        if (Math.abs(camera.ratio - aspect) < 0.0001f) return;

        camera.ratio = aspect;
        CameraSystem.updateProjectionMatrices(camera);
        resetProjectionMatrixForCurrentState();
        graphics.projection = camera.projectionMatrix;
    }

    /**
     * Re-applies the projection matrix for the current state after the aspect ratio changed. If a
     * projection transition is in progress, it is restarted from its source matrix using the new
     * aspect ratio, so no frame is rendered with the old aspect.
     */
    private void resetProjectionMatrixForCurrentState() {
        switch (projectionState) {
            case PERSPECTIVE_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.PERSPECTIVE_MATRIX;
                break;
            case ORTHOGRAPHIC_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.ORTHOGRAPHIC_MATRIX;
                break;
            case INTERMEDIATE_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.INTERMEDIATE_MATRIX;
                break;
            case PERSPECTIVE_TO_ORTHOGRAPHIC_VIEW_STATE:
            case PERSPECTIVE_TO_INTERMEDIATE_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.PERSPECTIVE_MATRIX;
                projectionProgress = 0f;
                break;
            case ORTHOGRAPHIC_TO_PERSPECTIVE_VIEW_STATE:
            case ORTHOGRAPHIC_TO_INTERMEDIATE_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.ORTHOGRAPHIC_MATRIX;
                projectionProgress = 0f;
                break;
            case INTERMEDIATE_TO_PERSPECTIVE_VIEW_STATE:
            case INTERMEDIATE_TO_ORTHOGRAPHIC_VIEW_STATE:
                camera.projectionMatrix = CameraSystem.INTERMEDIATE_MATRIX;
                projectionProgress = 0f;
                break;
            default:
                break;
        }
    }

    private Vector2f virtualCursorPosition = new Vector2f();
    private Vector2f previousCursorPosition = new Vector2f();
    private Vector2f cursorPositionDifference = new Vector2f();


    private void updateCameraMovement(CameraControls cameraControls, float deltaTime) {
        if (!mouseCaptured) return;

        Vector2f cursorPosition  = lastCursorPosition;
        cursorPositionDifference = new Vector2f(cursorPosition)       .sub(previousCursorPosition);
        virtualCursorPosition    = new Vector2f(virtualCursorPosition).add(cursorPositionDifference);
        previousCursorPosition   = cursorPosition;

        float verticalAngle   =  virtualCursorPosition.y / (graphics.window.getWidth() / 256f) - 180;
        float horizontalAngle = -virtualCursorPosition.x / (graphics.window.getWidth() / 256f) - 180;

        verticalAngle = restrictAngle(verticalAngle, -MAX_CAMERA_ANGLE, MAX_CAMERA_ANGLE);


        Vector3f point = vec3f().set(0f, 0f, 1f)
                .rotateX((float) Math.toRadians(verticalAngle))
                .rotateY((float) Math.toRadians(horizontalAngle));

        cameraMovementSpeed = leftShiftKeyIsHeld ? cameraMovementSpeed + deltaTime * 4.7f : 1f;

        Transform componentTransform = cm.get(cameraControls.getEntity(), Transform.class);
        Vector3f  vec3f = vec3f();

        // up-down movement
        // note: incorrect
        if (keyQIsHeld) {
            Vector3f delta = vec3f.set(0f, -1f, 0f)
//                    .normalize()
//                    .rotateY((float) Math.toRadians(-verticalAngle))
//                    .rotateX((float) Math.toRadians(-horizontalAngle))
                    .mul(deltaTime * cameraMovementSpeed);

            componentTransform.moveRel(delta);
        }
        if (keyEIsHeld) {
            Vector3f delta = vec3f.set(0f, 1f, 0f)
//                    .normalize()
//                    .rotateX((float) Math.toRadians(horizontalAngle))
//                    .rotateY((float) Math.toRadians(verticalAngle))
                    .mul(deltaTime * cameraMovementSpeed);

            componentTransform.moveRel(delta);
        }

        // left-right movement
        if (keyDIsHeld) {
            componentTransform.moveRel(vec3f.set(point.x, 0f, point.z).normalize().rotateY((float) Math.toRadians(90f)).mul(-deltaTime * cameraMovementSpeed));
        }
        if (keyAIsHeld) {
            componentTransform.moveRel(vec3f.set(point.x, 0f, point.z).normalize().rotateY((float) Math.toRadians(90f)).mul(deltaTime * cameraMovementSpeed));
        }

        // forward-backward movement
        if (keyWIsHeld) {
            componentTransform.moveRel(vec3f.set(point).normalize().mul(deltaTime * cameraMovementSpeed));
        }
        if (keySIsHeld) {
            componentTransform.moveRel(vec3f.set(point).normalize().mul(-deltaTime * cameraMovementSpeed));
        }

        // todo: something wrong with projection when position point is not (0, 0, 0)
        //  needs to fix

        camera.eye.set(componentTransform.getPosition());
        camera.at.set(point.add(componentTransform.getPosition()));

        camera.viewMatrix.identity().lookAt(camera.eye, camera.at, vec3f.set(0f, 1f, 0f));

        putBack(vec3f);
        putBack(point);

        graphics.view = camera.viewMatrix;
    }

    private float restrictAngle(float angle, float lowest, float highest) {
        return Math.min(Math.max(angle, lowest), highest);
    }

    // TODO: Move this window callback into another place where it will fit, not the camera stuff
    public static class WindowSizeCallback implements GLFWWindowSizeCallbackI {
        private final Graphics graphics;

        public WindowSizeCallback(Graphics graphics) {
            this.graphics = graphics;
        }

        @Override
        public void invoke(long window, int width, int height) {
            Window.setSize(width, height);
            graphics.updateScreenProjection();
            GL30.glViewport(0, 0, width, height);
            log.info("window size = {}w : {}h", width, height);
        }
    }
}
