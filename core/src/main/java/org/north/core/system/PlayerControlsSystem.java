package org.north.core.system;

import org.lwjgl.glfw.GLFW;
import org.north.core.scene.component.PlayerControls;
import org.north.core.scene.component.RigidBody;
import org.north.core.scene.component.Transform;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.system.process.InputHandlerProcess;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.UpdateProcess;

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
