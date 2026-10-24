package org.north.core.scene;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL30;
import org.north.core.asset.AssetService;
import org.north.core.graphics.Material;
import org.north.core.graphics.Mesh;
import org.north.core.graphics.PredefinedMeshes;
import org.north.core.graphics.Texture;
import org.north.core.graphics.shader.Shader;
import org.north.core.physics.shape.Rectangle;
import org.north.core.scene.component.*;
import org.north.core.scene.component.MeshCollider.Space;
import org.north.core.scene.component.management.ComponentManager;

import java.util.List;

public class DefaultSceneComposer implements SceneComposer {
    private static final String SHADERS_PATH = "assets/shaders/";

    private final AssetService assetService;

    public DefaultSceneComposer(AssetService assetService) {
        this.assetService = assetService;
    }

    @Override
    public void compose(final EntityManager scene, final ComponentManager cm) {
        // initCubeAndCamera(scene, cm);
        // initReferenceScene(scene, cm);
        // testScene(scene, cm);
        //initSpaceshipOnScreen(scene, cm);

        // Create the camera and world objects first so their update systems run before the cursor
        // picking system. The cursor itself is created last and is still rendered on top because
        // its renderer uses the screen-space render layer.
        testManyTransparentCubesGrid(scene, cm);
        //initCursorDemoOverlay(scene, cm);
    }

    private void initCursorDemoOverlay(EntityManager scene, ComponentManager cm) {
        createCamera(scene, cm, false);
        
        createUiButton(scene, cm, "ui_button_1", 256f, 112f, 180f, 46f,
                new Vector4f(0.22f, 0.32f, 0.44f, 0.94f),
                new Vector4f(0.32f, 0.72f, 1.00f, 1.00f));

        createUiButton(scene, cm, "ui_button_2", 256f, 180f, 180f, 46f,
                new Vector4f(0.34f, 0.26f, 0.20f, 0.94f),
                new Vector4f(1.00f, 0.66f, 0.30f, 1.00f));

        createUiButton(scene, cm, "ui_button_3", 256f, 248f, 180f, 46f,
                new Vector4f(0.24f, 0.34f, 0.26f, 0.94f),
                new Vector4f(0.42f, 0.92f, 0.52f, 1.00f));

        Entity cursorEntity = scene.create("visual_cursor");
        Transform cursorTransform = cm.add(cursorEntity, Transform.class);
        cursorTransform.moveTo(256f, 320f, 0f);

        VisualCursor visualCursor = new VisualCursor();
        visualCursor.center.set(256f, 320f);
        visualCursor.previousCenter.set(256f, 320f);
        visualCursor.targetCenter.set(256f, 320f);

        MeshRenderer cursorRenderer = cm.add(cursorEntity, MeshRenderer.class);
        cursorRenderer.mesh = PredefinedMeshes.QUAD;
        cursorRenderer.screenSpace = true;
        cursorRenderer.renderOrder = 100;
        cursorRenderer.material = simpleColorMaterial(visualCursor.color);

        cm.add(cursorEntity, visualCursor);

        createCursorLabel(scene, cm, cursorEntity);
    }

    private void createCursorLabel(EntityManager scene, ComponentManager cm, Entity cursorEntity) {
        // A screen-space label that displays the name of the entity under the cursor. It is added
        // after the cursor so its update system runs after cursor picking has selected a target.
        Entity labelEntity = scene.create("visual_cursor_label");
        cm.add(labelEntity, Transform.class);

        MeshRenderer labelRenderer = cm.add(labelEntity, MeshRenderer.class);
        labelRenderer.mesh = PredefinedMeshes.QUAD;
        labelRenderer.screenSpace = true;
        labelRenderer.renderOrder = 200;

        VisualCursorLabel label = cm.add(labelEntity, VisualCursorLabel.class);
        label.cursorEntity = cursorEntity;

    }

    private void createUiButton(EntityManager scene,
                                ComponentManager cm,
                                String name,
                                float x,
                                float y,
                                float width,
                                float height,
                                Vector4f idleColor,
                                Vector4f hoverColor) {
        Entity entity = scene.create(name);

        Transform transform = cm.add(entity, Transform.class);
        transform.moveTo(x, y, 0f);
        transform.rescaleTo(width, height, 1f);

        Button button = new Button();
        button.buttonDefaultColor.set(idleColor);
        button.buttonOnHoverColor.set(hoverColor);
        button.buttonColor.set(idleColor);

        MeshRenderer renderer = cm.add(entity, MeshRenderer.class);
        renderer.mesh = PredefinedMeshes.QUAD;
        renderer.screenSpace = true;
        renderer.renderOrder = 0;
        renderer.material = simpleColorMaterial(button.buttonColor);

        MeshCollider collider = cm.add(entity, MeshCollider.class);
        collider.body = new Rectangle(
                new Vector2f(-width / 2f, -height / 2f),
                new Vector2f(width / 2f, height / 2f)
        );

        cm.add(entity, button);
    }

    private void testManyTransparentCubesGrid(EntityManager scene, ComponentManager cm) {
        GL30.glClearColor(.1f, .1f, .1f, 1f);

        createCamera(scene, cm, true);
        
        Transform sceneTransform = cm.get(scene.getRoot(), Transform.class);

        // Entity camera = scene.create("camera");

        // cm.add(camera, new Transform(sceneTransform));
        // cm.add(camera, Camera.class, CameraControls.class);


        Shader shader = shader("simple_color_shader");
        int N = 8;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                for (int k = 0; k < N; k++) {
                    Entity cube = scene.create("cube_" + i + "_" + j + "_" + k);
                    final float x = i;
                    final float z = j;
                    final float y = k;

                    Transform transform = new Transform(sceneTransform);
//                    Transform transform = new Transform();
                    transform.moveTo(0.5f * x, 0.5f * y, 0.5f * z);
                    transform.rescaleTo(0.4999f, 0.4999f, 0.4999f);

                    MeshRenderer meshRenderer = new MeshRenderer();
                    meshRenderer.mesh = PredefinedMeshes.CUBE;
                    meshRenderer.material = new Material(shader)
                            .setVector("u_color", new Vector4f(0.3f, 0.3f, 0.3f, 1f));

                    cm.add(cube, transform);
                    cm.add(cube, meshRenderer);

                    MeshCollider collider = cm.add(cube, MeshCollider.class);
                    collider.space = MeshCollider.Space.WORLD;
                    collider.selectable = true;

                    cm.add(cube, CubeColorSwitcher.class).acc = (x / N) * (y / N) * (z / N);


//                    scene = cube;
                }
            }
        }
        
        Entity theThing = scene.create("theThing");
        Mesh mesh = assetService.get(Mesh.class, "assets/meshes/the-thing.obj");
        // Texture texture = assetService.get(Texture.class, "assets/models/character.png");
        Shader thingShader = assetService.get(Shader.class, "assets/shaders/texture_shader");

        Transform thingTransform = cm.add(theThing, Transform.class);
        thingTransform.moveTo(-1, -1, -1);

        MeshRenderer renderer = cm.add(theThing, MeshRenderer.class);
        renderer.mesh = mesh;
        renderer.material = new Material(thingShader)
                // .setTexture("u_texture", texture)
                .setVector("u_color", new Vector4f(1f, 1f, 1f, 1f));

        cm.add(theThing, MeshCollider.class).space = Space.WORLD;

        Entity cursorEntity = scene.create("visual_cursor");
        Transform cursorTransform = cm.add(cursorEntity, Transform.class);
        cursorTransform.moveTo(256f, 320f, 0f);

        VisualCursor visualCursor = new VisualCursor();
        visualCursor.center.set(256f, 320f);
        visualCursor.previousCenter.set(256f, 320f);
        visualCursor.targetCenter.set(256f, 320f);

        MeshRenderer cursorRenderer = cm.add(cursorEntity, MeshRenderer.class);
        cursorRenderer.mesh = PredefinedMeshes.QUAD;
        cursorRenderer.screenSpace = false;
        cursorRenderer.renderOrder = 100;
        cursorRenderer.material = simpleColorMaterial(visualCursor.color);

        cm.add(cursorEntity, visualCursor);

        createCursorLabel(scene, cm, cursorEntity);
    }

    private void testScene(EntityManager scene, ComponentManager cm) {
        Entity testCube = scene.create("test_cube");

        Transform transform = new Transform();
        transform.moveTo(0.5f, 0.5f, 0.5f);
        transform.rescaleTo(0.8f, 0.8f, 0.8f);

        MeshRenderer meshRenderer = new MeshRenderer();
        meshRenderer.mesh = PredefinedMeshes.CUBE;
        meshRenderer.material = simpleColorMaterial(new Vector4f(0.42f, 0.42f, 0.54f, 0.5f));

        cm.add(testCube, transform);
        cm.add(testCube, meshRenderer);

        Entity camera = scene.create("camera");

        cm.add(camera, Transform.class, Camera.class, CameraControls.class);

//        transform = (Transform) components.get(0);
//        transform.getPosition().set(0.5f, 0.5f, 0.5f);
//        transform.scale.set(0.8f, 0.8f, 0.8f);


    }

    private void initCubeAndCamera(EntityManager scene, ComponentManager cm) {
        Entity camera = scene.create("camera");
        Entity center = scene.create("center");
        Entity left   = scene.create("left");
        Entity right  = scene.create("right");


        List<? extends Component> components;
        Transform    transform;
        MeshRenderer renderer;

        components = cm.add(camera, Transform.class, Camera.class, CameraControls.class);

        ((Transform) components.get(0)).moveTo(0, 0, 1);


        components = cm.add(center, Transform.class, MeshRenderer.class);

        renderer = (MeshRenderer) components.get(1);
        renderer.mesh     = PredefinedMeshes.QUAD;
        renderer.material = textureMaterial("assets/textures/screen-frame-1024.png");
        //        renderer.texture2 = new Texture("assets/textures/Capture001.png");


        components = cm.add(left, Transform.class, MeshRenderer.class);

        transform = (Transform) components.get(0);
        transform.moveTo(-2f, 0f, 0f);
        //        transform.scale = new Vector3f(0.5f, 0.2f, 0.2f);

        renderer = (MeshRenderer) components.get(1);
        renderer.mesh     = PredefinedMeshes.QUAD;
        renderer.material = textureMaterial("assets/textures/screen-frame-1024.png");
        //        renderer.texture2 = new Texture("core/src/main/resources/assets/textures/spaceship-16.png");
        //        renderer.texture = new Texture("core/src/main/resources/assets/textures/Capture001.png");


        components = cm.add(right, Transform.class, MeshRenderer.class);

        transform = (Transform) components.get(0);
        transform.moveTo(2f, 0f, 0f);
        //        transform.rotation = new Vector3f(0f, -5f, 0f);
        //        transform.scale = new Vector3f(0.2f, 0.2f, 0.2f);

        renderer = (MeshRenderer) components.get(1);
        renderer.mesh     = PredefinedMeshes.CUBE;
        renderer.material = simpleColorMaterial(new Vector4f(1, 0, 0, 1));
        //        renderer.texture = new Texture("core/src/main/resources/assets/textures/screen-frame-1024.png");

    }

    private void initReferenceScene(EntityManager scene, ComponentManager cm) {
        Entity referenceBox = scene.create("referenceBox");

        List<? extends Component> componentList =
                cm.add(referenceBox, Transform.class, MeshRenderer.class, RigidBody.class);

        MeshRenderer renderer = (MeshRenderer) componentList.get(1);
        renderer.material = simpleColorMaterial(new Vector4f(0.25f, 0.5f, 0.8f, 1f));
        renderer.mesh     = PredefinedMeshes.QUAD;

        createCamera(scene, cm, true);
    }

    private void initSpaceshipOnScreen(EntityManager scene, ComponentManager cm) {
        Transform    transform;
        MeshRenderer renderer;

        // TV Screen object
        Entity tvScreen = scene.create("tvScreen");
        transform = cm.add(tvScreen, Transform.class);
        transform.rescaleTo(new Vector3f(2, 2, 2));

        renderer = cm.add(tvScreen, MeshRenderer.class);
        renderer.material = textureMaterial("assets/textures/screen-frame-1024.png");

        // Player spaceship in the middle of the screen
        Entity player = scene.create("player");
        transform = cm.add(player, Transform.class);
        transform.moveTo(new Vector3f(0f, 0f, 1f));
        transform.rescaleTo(0.2f, 0.2f, 0.2f);

        renderer = cm.add(player, MeshRenderer.class);
        renderer.material = textureMaterial("assets/textures/spaceship-16.png");

        cm.add(player, CloudEmitter.class);


        Entity gasCloudSpawner = scene.create("gasCloudSpawner");
        transform = cm.add(gasCloudSpawner, Transform.class);
        transform.moveTo( new Vector3f(0f, -0.225f, 0.5f));


        /*

        // Spaceship gas clouds
        Entity cloud = em.create("cloud");
        transform = cm.take(cloud).add(Transform.class);
        transform.moveTo(new Vector3f(0f, -0.25f, 1f);
        transform.scale = new Vector3f(0.2f, 0.2f, 0.2f);

        renderer = cm.take(cloud).add(MeshRenderer.class);
        renderer.material = new Material(shader("atlas_texture_animation_shader"))
                .setTexture("u_texture", assetService.get(Texture.class, "assets/textures/cloud-sprites-atlas.png"))
                .setInt("u_sprite_width", 12)
                .setInt("u_sprite_height", 12)
                .setInt("u_sprite_index", 0);

        GasCloud animation = cm.take(cloud).add(GasCloud.class);

         */

        // Background texture behind tv screen, player and other gameplay objects
        Entity background = scene.create("background");
        transform = cm.add(background, Transform.class);
        transform.moveTo(new Vector3f(0f, 0f, 2f));
        transform.rescaleTo(new Vector3f(2, 2, 2));

        renderer = cm.add(background, MeshRenderer.class);
        renderer.material = textureMaterial("assets/textures/screen-background-1024.png");

        // Camera
        createCamera(scene, cm, false);
        // Entity camera = scene.create("camera");
        // transform = cm.add(camera, Transform.class);
        // transform.moveTo( new Vector3f(0f, 0f, 0f));

        // cm.add(camera, Camera.class);
        

        // World that moves when player is "moving"
        Entity movableWorld = scene.create("movableWorld");
        cm.add(movableWorld, RigidBody.class);
        transform = cm.add(movableWorld, Transform.class);
        //        transform.scale = new Vector3f(5, 5, 5);




    }

    private Entity createCamera(EntityManager scene, ComponentManager cm, boolean enableControls) {
        Entity camera = scene.findFirst(entity -> cm.has(entity, Camera.class));
        if (camera == null) {
            camera = scene.create("camera");
            cm.add(camera, Transform.class, Camera.class);
            if (enableControls) {
                cm.add(camera, CameraControls.class, PlayerControls.class);
            }
        }
        return camera;
    }

    private Shader shader(String name) {
        return assetService.get(Shader.class, SHADERS_PATH + name);
    }

    private Material simpleColorMaterial(Vector4f color) {
        return new Material(shader("simple_color_shader"))
                .setVector("u_color", color);
    }

    private Material textureMaterial(String texturePath) {
        Texture texture = assetService.get(Texture.class, texturePath);
        return new Material(shader("texture_shader"))
                .setTexture("u_texture", texture)
                .setVector("u_color", new Vector4f(1f, 1f, 1f, 1f));
    }

}
