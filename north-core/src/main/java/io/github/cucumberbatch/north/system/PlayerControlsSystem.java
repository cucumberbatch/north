package io.github.cucumberbatch.north.system;

import org.lwjgl.glfw.GLFW;
import io.github.cucumberbatch.north.scene.component.PlayerControls;
import io.github.cucumberbatch.north.scene.component.RigidBody;
import io.github.cucumberbatch.north.scene.component.Transform;
import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandler;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.system.process.InputHandlerProcess;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.UpdateProcess;

@ComponentHandler(PlayerControls.class)
public class PlayerControlsSystem extends AbstractSystem<PlayerControls>
        implements InitProcess<PlayerControls>, InputHandlerProcess<PlayerControls>, UpdateProcess<PlayerControls> {

    private RigidBody rigidBody;
    private boolean gravitationalState = false;
    private boolean keyIIsPressed;

    @Inject
    public PlayerControlsSystem(ApplicationContext context) {
        super(PlayerControls.class, context);
    }

    @Override
    public void init(PlayerControls playerControls) {
        rigidBody = cm.get(entityManager.find("referenceBox"), RigidBody.class);
    }

    @Override
    public void handleInput(PlayerControls component, Input input) {
        keyIIsPressed = input.isPressed(GLFW.GLFW_KEY_I);
    }

    @Override
    public void update(PlayerControls playerControls, float deltaTime) {
        if (keyIIsPressed) {
            gravitationalState = !gravitationalState;
            rigidBody.isGravitational = gravitationalState;
            rigidBody.velocity.set(0);
            rigidBody.acceleration.set(0);
            cm.get(rigidBody.getEntity(), Transform.class).moveTo(0f, 0f, 0f);
        }
    }
}
