package io.github.cucumberbatch.north.io.remote;

import io.github.cucumberbatch.north.io.Input;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.lwjgl.glfw.GLFW.*;

class GamepadStateTest {

    private GamepadState state(Input input) {
        GamepadLayout layout = new GamepadLayout();
        RemoteInputBus bus = new RemoteInputBus(input);
        return new GamepadState("test-client", layout, bus, null);
    }

    @Test
    void buttonPressIsDeliveredAsAKeyEvent() {
        Input input = new Input();
        GamepadState state = state(input);

        state.setButton("a", true);

        Assertions.assertTrue(input.isPressed(GLFW_KEY_SPACE));

        input.updateInput();
        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

        state.setButton("a", false);

        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
    }

    @Test
    void stickIsDeliveredAsAnAxisAndAsEmulatedKeys() {
        Input input = new Input();
        GamepadState state = state(input);

        state.setAxis("left_x", -1f);

        Assertions.assertEquals(-1f, input.getAxis("leftX"), 0.0001f);
        Assertions.assertTrue(input.isPressed(GLFW_KEY_A));
        Assertions.assertFalse(input.isPressed(GLFW_KEY_D));

        state.setAxis("left_x", 1f);

        Assertions.assertFalse(input.isHeld(GLFW_KEY_A));
        Assertions.assertTrue(input.isPressed(GLFW_KEY_D));
    }

    @Test
    void disconnectedClientReleasesItsKeys() {
        Input input = new Input();
        GamepadState state = state(input);

        state.setButton("a", true);
        state.setAxis("left_x", -1f);
        input.updateInput();

        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));
        Assertions.assertTrue(input.isHeld(GLFW_KEY_A));

        state.releaseAll();

        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
        Assertions.assertFalse(input.isHeld(GLFW_KEY_A));
    }

    @Test
    void twoClientsWhichPressTheSameButtonShareTheKey() {
        Input input = new Input();
        GamepadLayout layout = new GamepadLayout();
        RemoteInputBus bus = new RemoteInputBus(input);

        GamepadState first = new GamepadState("client-1", layout, bus, null);
        GamepadState second = new GamepadState("client-2", layout, bus, null);

        first.setButton("a", true);
        second.setButton("a", true);
        input.updateInput();

        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

        // the first client lets the button go, the second one still holds it
        first.setButton("a", false);
        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

        second.setButton("a", false);
        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
    }

    @Test
    void aDisconnectedClientReleasesOnlyItsOwnKeys() {
        Input input = new Input();
        GamepadLayout layout = new GamepadLayout();
        RemoteInputBus bus = new RemoteInputBus(input);

        GamepadState first = new GamepadState("client-1", layout, bus, null);
        GamepadState second = new GamepadState("client-2", layout, bus, null);

        first.setButton("a", true);
        second.setButton("a", true);
        input.updateInput();

        first.releaseAll();

        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE), "The key of the second client must stay");

        second.releaseAll();

        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
    }

}
