package org.north.core.system;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.exception.ComponentNotFoundException;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Material;
import org.north.core.graphics.PredefinedMeshes;
import org.north.core.graphics.Window;
import org.north.core.graphics.shader.Shader;
import org.north.core.physics.shape.Rectangle;
import org.north.core.physics.spatial.Aabb;
import org.north.core.scene.Entity;
import org.north.core.scene.SceneSpatialIndex;
import org.north.core.scene.component.Button;
import org.north.core.scene.component.MeshCollider;
import org.north.core.scene.component.MeshRenderer;
import org.north.core.scene.component.Transform;
import org.north.core.scene.component.VisualCursor;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.UpdateProcess;

import java.util.Collection;

/**
 * Drives the physical visual cursor.
 *
 * <p>The cursor follows the pointer with a damped spring instead of being teleported. When the
 * pointer is over a screen-space {@link MeshCollider}, the cursor is magnetized to the collider
 * rectangle. When no UI element is under the pointer, a camera ray is cast into the scene and the
 * nearest world-space collider is selected. Its world bounds are projected back into window pixels
 * and used as the new magnet rectangle.</p>
 */
@ComponentHandler(VisualCursor.class)
public class VisualCursorSystem extends AbstractSystem<VisualCursor>
        implements InitProcess<VisualCursor>, UpdateProcess<VisualCursor> {

    private static final String SHADER_PATH = "assets/shaders/simple_color_shader";
    private static final float  MAX_DELTA_TIME = 1f / 30f;
    private static final float  MAX_PICK_DISTANCE = 1000f;

    private final Input input;
    private final Window window;
    private final Graphics graphics;
    private final SceneSpatialIndex spatialIndex;

    private final Vector2f pointer        = new Vector2f();
    private final Vector2f desiredCenter  = new Vector2f();
    private final Vector2f desiredSize    = new Vector2f();
    private final Vector2f delta          = new Vector2f();
    private final Vector2f acceleration   = new Vector2f();
    private final Vector2f temporary      = new Vector2f();

    private final Rectangle hoveredBounds = new Rectangle(new Vector2f(), new Vector2f());

    private final Matrix4f viewProjection         = new Matrix4f();
    private final Vector3f rayOrigin              = new Vector3f();
    private final Vector3f rayDirection           = new Vector3f();
    private final Vector3f rayFarPoint            = new Vector3f();
    private final Vector3f corner                 = new Vector3f();
    private final Vector4f clip                   = new Vector4f();
    private final Aabb     rayQueryBounds         = new Aabb();
    private final int[]    viewport               = new int[4];

    @Inject
    public VisualCursorSystem(ApplicationContext context) {
        super(VisualCursor.class, context);
        this.input        = context.getDependency(Input.class);
        this.window       = context.getDependency(Window.class);
        this.graphics     = context.getDependency(Graphics.class);
        this.spatialIndex = context.getDependency(SceneSpatialIndex.class);
    }

    @Override
    public void init(VisualCursor cursor) {
        MeshRenderer renderer = cm.get(cursor.getEntity(), MeshRenderer.class);
        if (renderer == null) {
            throw new ComponentNotFoundException(MeshRenderer.class);
        }

        renderer.mesh = PredefinedMeshes.QUAD;
        renderer.screenSpace = true;
        renderer.renderOrder = 100;

        if (renderer.material == null) {
            Shader shader = assetService.get(Shader.class, SHADER_PATH);
            renderer.material = new Material(shader);
        }
        renderer.material.setVector("u_color", cursor.color);

        input.getCursorPosition(pointer);
        cursor.center.set(pointer);
        cursor.previousCenter.set(pointer);
        cursor.targetCenter.set(pointer);
        cursor.velocity.zero();
        cursor.displaySize.set(cursor.idleSize);
        cursor.targetSize.set(cursor.idleSize);

        updateShape(cursor);
        syncTransform(cursor);

        if (cursor.hideSystemCursor) {
            GLFW.glfwSetInputMode(
                    window.getWindow(),
                    GLFW.GLFW_CURSOR,
                    GLFW.GLFW_CURSOR_HIDDEN
            );
        }
    }

    @Override
    public void update(VisualCursor cursor, float deltaTime) {
        updateCursorMode(cursor);

        if (!cursor.visible) {
            cursor.displaySize.zero();
            updateShape(cursor);
            syncTransform(cursor);
            return;
        }

        hideOperatingSystemCursor(cursor);

        float dt = Math.min(Math.max(deltaTime, 0f), MAX_DELTA_TIME);
        if (dt == 0f) return;

        input.getCursorPosition(pointer);

        MeshCollider target = findHoveredTarget(cursor, pointer);
        cursor.previousHoveredEntity = cursor.hoveredEntity;

        if (target != null) {
            hoveredBounds.getCenter(desiredCenter);
            desiredSize.set(hoveredBounds.width(), hoveredBounds.height());

            if (target.space == MeshCollider.Space.WORLD) {
                desiredSize.x = Math.min(desiredSize.x, cursor.maxWorldTargetSize);
                desiredSize.y = Math.min(desiredSize.y, cursor.maxWorldTargetSize);
            }

            cursor.targetShape.setFromCenter(desiredCenter, desiredSize);
            setHoveredEntity(cursor, target.getEntity());
        } else {
            desiredCenter.set(pointer);
            desiredSize.set(cursor.idleSize);
            cursor.targetShape.topLeft.set(pointer);
            cursor.targetShape.bottomRight.set(pointer);
            setHoveredEntity(cursor, null);
        }

        cursor.targetCenter.set(desiredCenter);
        cursor.targetSize.set(desiredSize);

        updatePosition(cursor, dt);
        updateSize(cursor, dt);
        updateColor(cursor, dt);

        updateShape(cursor);
        syncTransform(cursor);
    }

    private void updateCursorMode(VisualCursor cursor) {
        if (input.isPressed(GLFW.GLFW_KEY_ESCAPE)) {
            cursor.visible = true;
        }
        if (input.isPressed(GLFW.GLFW_MOUSE_BUTTON_1)) {
            cursor.visible = false;
        }
    }

    private MeshCollider findHoveredTarget(VisualCursor cursor, Vector2f pointer) {
        Collection<MeshCollider> colliders = cm.getAllByType(MeshCollider.class);
        if (colliders == null || colliders.isEmpty()) return null;

        Entity cursorEntity = cursor.getEntity();

        // Keep the current screen-space target while the pointer is inside it. This avoids target
        // flickering when rectangles overlap or when the pointer is exactly on a border. World
        // targets do not need this: the camera ray is stable and is re-evaluated every frame.
        if (cursor.hoveredEntity != null) {
            MeshCollider current = cm.get(cursor.hoveredEntity, MeshCollider.class);
            if (isScreenTarget(current, cursorEntity) && current.body.isInside(pointer)) {
                copyBounds(current.body);
                return current;
            }

            // Light-up a hovered object with a MeshRenderer component
            if (cm.has(cursor.hoveredEntity, MeshRenderer.class)) {
                MeshRenderer renderer = cm.get(cursor.hoveredEntity, MeshRenderer.class);
                renderer.material.setVector("u_color", new Vector4f(1, 1, 1, 0.1f));
            }
        }

        // UI is an overlay: when the pointer is over a screen-space collider, it wins over any
        // world object that happens to be behind it.
        MeshCollider bestScreen = null;
        float bestScore = Float.MAX_VALUE;

        for (MeshCollider collider : colliders) {
            if (!isScreenTarget(collider, cursorEntity)) continue;

            Rectangle body = collider.body;
            if (!body.isInside(pointer)) continue;

            // Prefer the smallest target when several colliders overlap.
            float score = body.width() * body.height();
            if (score < bestScore) {
                bestScore = score;
                bestScreen = collider;
            }
        }

        if (bestScreen != null) {
            copyBounds(bestScreen.body);
            return bestScreen;
        }

        return findWorldTarget(cursor, pointer, colliders);
    }

    private MeshCollider findWorldTarget(VisualCursor cursor,
                                         Vector2f pointer,
                                         Collection<MeshCollider> colliders) {
        spatialIndex.rebuildIfDirty(colliders);
        if (spatialIndex.isEmpty()) return null;

        if (!updateCameraRay(pointer)) return null;

        rayFarPoint.set(rayDirection).mul(MAX_PICK_DISTANCE).add(rayOrigin);
        rayQueryBounds.setFromPoints(rayOrigin, rayFarPoint);

        Entity cursorEntity = cursor.getEntity();
        MeshCollider best = null;
        float bestDistance = Float.POSITIVE_INFINITY;

        for (MeshCollider collider : spatialIndex.query(rayQueryBounds)) {
            if (!isWorldTarget(collider, cursorEntity)) continue;

            float distance = collider.bounds.intersectRay(rayOrigin, rayDirection, MAX_PICK_DISTANCE);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = collider;
            }
        }

        if (best == null || !projectBounds(best.bounds)) {
            return null;
        }

        return best;
    }

    private boolean updateCameraRay(Vector2f pointer) {
        int width  = window.getWidth();
        int height = window.getHeight();
        if (width <= 0 || height <= 0) return false;

        viewProjection.set(graphics.projection).mul(graphics.view);

        viewport[0] = 0;
        viewport[1] = 0;
        viewport[2] = width;
        viewport[3] = height;

        // GLFW reports the cursor from the top-left, while OpenGL/JOML unprojection expects
        // window coordinates from the bottom-left.
        //
        // JOML's unprojectRay treats this matrix as the forward view-projection matrix and
        // computes its inverse internally. Passing an already inverted matrix here produced rays
        // that only hit objects near the window edges.
        viewProjection.unprojectRay(
                pointer.x,
                height - pointer.y,
                viewport,
                rayOrigin,
                rayDirection
        );

        float directionLengthSquared = rayDirection.lengthSquared();
        if (directionLengthSquared <= 0.000001f) return false;

        rayDirection.mul(1f / (float) Math.sqrt(directionLengthSquared));
        return true;
    }

    private boolean projectBounds(Aabb bounds) {
        int width  = window.getWidth();
        int height = window.getHeight();
        if (width <= 0 || height <= 0 || bounds == null || bounds.isEmpty()) return false;

        viewProjection.set(graphics.projection).mul(graphics.view);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            float x = (cornerIndex & 1) == 0 ? bounds.xMin() : bounds.xMax();
            float y = (cornerIndex & 2) == 0 ? bounds.yMin() : bounds.yMax();
            float z = (cornerIndex & 4) == 0 ? bounds.zMin() : bounds.zMax();

            corner.set(x, y, z);
            clip.set(corner.x, corner.y, corner.z, 1f);
            viewProjection.transform(clip);

            if (clip.w <= 0.0001f) return false;

            float ndcZ = clip.z / clip.w;
            if (ndcZ < -1f || ndcZ > 1f) return false;

            float screenX = ((clip.x / clip.w) * 0.5f + 0.5f) * width;
            float screenY = (1f - ((clip.y / clip.w) * 0.5f + 0.5f)) * height;

            minX = Math.min(minX, screenX);
            minY = Math.min(minY, screenY);
            maxX = Math.max(maxX, screenX);
            maxY = Math.max(maxY, screenY);
        }

        if (maxX <= minX || maxY <= minY) return false;

        hoveredBounds.topLeft.set(minX, minY);
        hoveredBounds.bottomRight.set(maxX, maxY);
        return true;
    }

    private void copyBounds(Rectangle source) {
        hoveredBounds.topLeft.set(source.topLeft);
        hoveredBounds.bottomRight.set(source.bottomRight);
    }

    private boolean isScreenTarget(MeshCollider collider, Entity cursorEntity) {
        return collider != null
                && collider.isActive()
                && collider.space == MeshCollider.Space.SCREEN
                && collider.getEntity() != cursorEntity
                && collider.body != null
                && !collider.body.isEmpty();
    }

    private boolean isWorldTarget(MeshCollider collider, Entity cursorEntity) {
        return collider != null
                && collider.isActive()
                && collider.space == MeshCollider.Space.WORLD
                && collider.selectable
                && collider.getEntity() != cursorEntity
                && collider.bounds != null
                && !collider.bounds.isEmpty();
    }

    private void setHoveredEntity(VisualCursor cursor, Entity entity) {
        if (cursor.hoveredEntity == entity) return;

        if (cursor.hoveredEntity != null) {
            Button previousButton = cm.get(cursor.hoveredEntity, Button.class);
            if (previousButton != null) {
                previousButton.hovered = false;
            }
        }

        cursor.hoveredEntity = entity;

        if (entity != null) {
            Button button = cm.get(entity, Button.class);
            if (button != null) {
                button.hovered = true;
            }
        }
    }

    private void updatePosition(VisualCursor cursor, float dt) {
        cursor.previousCenter.set(cursor.center);

        delta.set(cursor.center).sub(cursor.targetCenter);

        acceleration
                .set(delta)
                .mul(-cursor.stiffness)
                .sub(temporary.set(cursor.velocity).mul(cursor.damping));

        cursor.velocity.add(temporary.set(acceleration).mul(dt));
        cursor.center.add(temporary.set(cursor.velocity).mul(dt));

        if (delta.lengthSquared() < 0.0004f && cursor.velocity.lengthSquared() < 0.0004f) {
            cursor.center.set(cursor.targetCenter);
            cursor.velocity.zero();
        }
    }

    private void updateSize(VisualCursor cursor, float dt) {
        float ratio = smoothingFactor(cursor.magnetizationSpeed, dt);
        cursor.displaySize.lerp(cursor.targetSize, ratio);
    }

    private void updateColor(VisualCursor cursor, float dt) {
        Vector4f targetColor = cursor.hoveredEntity != null
                ? cursor.hoverColor
                : cursor.idleColor;
        cursor.color.lerp(targetColor, smoothingFactor(cursor.colorSpeed, dt));
    }

    private float smoothingFactor(float speed, float dt) {
        return Math.min(1f, Math.max(0f, 1f - (float) Math.exp(-speed * dt)));
    }

    private void updateShape(VisualCursor cursor) {
        cursor.shape.setFromCenter(cursor.center, cursor.displaySize);
    }

    private void syncTransform(VisualCursor cursor) {
        Transform transform = cm.get(cursor.getEntity(), Transform.class);
        if (transform == null) return;

        transform.moveTo(cursor.center.x, cursor.center.y, 0f);
        transform.rescaleTo(
                Math.max(cursor.displaySize.x, 0.0001f),
                Math.max(cursor.displaySize.y, 0.0001f),
                1f
        );
    }

    private void hideOperatingSystemCursor(VisualCursor cursor) {
        if (!cursor.hideSystemCursor) return;

        long handle = window.getWindow();
        int mode = GLFW.glfwGetInputMode(handle, GLFW.GLFW_CURSOR);
        if (mode == GLFW.GLFW_CURSOR_NORMAL) {
            GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }
}
