package org.north.core.system;

import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.graphics.Material;
import org.north.core.graphics.Texture;
import org.north.core.graphics.shader.Shader;
import org.north.core.scene.Entity;
import org.north.core.scene.component.*;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.InputHandlerProcess;
import org.north.core.system.process.UpdateProcess;

import static org.joml.Math.*;

@ComponentHandler(CloudEmitter.class)
public class CloudEmitterSystem extends AbstractSystem<CloudEmitter>
        implements InitProcess<CloudEmitter>, InputHandlerProcess<CloudEmitter>, UpdateProcess<CloudEmitter> {
    private static final String ATLAS_SHADER_PATH = "assets/shaders/atlas_texture_animation_shader";
    private static final String ATLAS_TEXTURE_PATH = "assets/textures/cloud-sprites-atlas.png";
    private static final int SPRITE_COUNT = 6;
    private static final int SPRITE_START_INDEX = 1;

    private static final float ROTATION_SPEED_DEGREES = 120f;
    private static final float MOVEMENT_ACCELERATION  = 4f;
    private static final float SPAWN_INTERVAL         = 1f / 6f;
    private static final float FULL_ROTATION_DEGREES  = 360f;

    private float acc = 0;
    private long  gasCloudEntityNumber = 0;

    private Entity    movableWorld;
    private Transform movableWorldTransform;
    private RigidBody movableWorldRigidBody;
    private Transform spaceshipTransform;
    private Transform spawnerTransform;

    private static final Vector3f emittingScale    = new Vector3f(0.2f, 0.2f, 0.2f);

    private boolean keyAIsHolded;
    private boolean keyDIsHolded;
    private boolean keyWIsHolded;

    @Inject
    public CloudEmitterSystem(ApplicationContext context) {
        super(CloudEmitter.class, context);
    }

    @Override
    public void init(CloudEmitter cloudEmitter) {
        Entity emitterEntity = cloudEmitter.getEntity();
        Entity spawnerEntity = entityManager.find("gasCloudSpawner");
        movableWorld         = entityManager.find("movableWorld");
        
        spaceshipTransform    = cm.get(emitterEntity, Transform.class);
        spawnerTransform      = cm.get(spawnerEntity, Transform.class);
        movableWorldTransform = cm.get(movableWorld,  Transform.class);
        movableWorldRigidBody = cm.get(movableWorld,  RigidBody.class);

        movableWorldRigidBody.isGravitational = false;
    }

    @Override
    public void handleInput(CloudEmitter component, Input input) {
        keyAIsHolded = input.isHeld(GLFW.GLFW_KEY_A);
        keyDIsHolded = input.isHeld(GLFW.GLFW_KEY_D);
        keyWIsHolded = input.isHeld(GLFW.GLFW_KEY_W);
    }

    @Override
    public void update(CloudEmitter cloudEmitter, final float deltaTime) {
        boolean moving = false;

        float rotationIncrement     = deltaTime * ROTATION_SPEED_DEGREES;
        float accelerationIncrement = deltaTime * MOVEMENT_ACCELERATION;
        float angle = spaceshipTransform.getRotation().z;

        int rotationDirection = (keyDIsHolded ? 1 : 0) - (keyAIsHolded ? 1 : 0);
        if (rotationDirection != 0) {
            angle = (angle + rotationDirection * rotationIncrement) % FULL_ROTATION_DEGREES;
            if (angle < 0f) angle += FULL_ROTATION_DEGREES;
            spaceshipTransform.getRotation().set(0f, 0f, angle);
        }

        float angleInRadians = angle * (float) PI / 180f;

        if (keyWIsHolded) {
            moving = true;
            movableWorldRigidBody.addImpulseToMassCenter(
                    +accelerationIncrement * (float) sin(angleInRadians),
                    -accelerationIncrement * (float) cos(angleInRadians),
                    0f
            );
        }

        if (moving && acc >= 1f) {
            emitCloud(angleInRadians);
            acc = 0f;
        }

        acc += deltaTime / SPAWN_INTERVAL;
    }

    private void emitCloud(float angleInRadians) {
        Entity gasCloudEntity = entityManager.create("gas_cloud_" + gasCloudEntityNumber++);

        Vector3f shipPosition = spaceshipTransform.getGlobalPosition(new Vector3f());
        Vector3f spawnerPosition = spawnerTransform.getGlobalPosition(new Vector3f());
        Vector3f exhaustOffset = spawnerPosition.sub(shipPosition, new Vector3f())
                .rotateZ(angleInRadians);
        Vector3f spawnPosition = shipPosition.add(exhaustOffset);

        Vector3f worldPosition = movableWorldTransform.getGlobalPosition(new Vector3f());

        // The cloud is a child of the movable world, so it keeps its position in the world while
        // the world scrolls past the static spaceship.
        Transform transform = new Transform(movableWorldTransform);
        transform.moveTo(spawnPosition.sub(worldPosition, new Vector3f()));
        transform.rescaleTo(emittingScale);

        MeshRenderer renderer = new MeshRenderer();
        Shader shader = assetService.get(Shader.class, ATLAS_SHADER_PATH);
        Texture texture = assetService.get(Texture.class, ATLAS_TEXTURE_PATH);
        renderer.material = new Material(shader)
                .setTexture("u_texture", texture)
                .setInt("u_sprite_count", SPRITE_COUNT)
                .setInt("u_sprite_index", SPRITE_START_INDEX)
                .setVector("u_color", new Vector4f(1f, 1f, 1f, 1f));

        cm.add(gasCloudEntity, transform);
        cm.add(gasCloudEntity, renderer);
        cm.add(gasCloudEntity, new GasCloud());
    }
}
