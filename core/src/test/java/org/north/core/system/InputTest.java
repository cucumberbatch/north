package org.north.core.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

class InputTest {

    @Test
    void pressIsReportedOnceAndTurnsIntoAHeldKey() {
        Input input = new Input();

        input.injectKeyPress(GLFW_KEY_SPACE);
        Assertions.assertTrue(input.isPressed(GLFW_KEY_SPACE));
        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));

        input.updateInput();

        Assertions.assertFalse(input.isPressed(GLFW_KEY_SPACE));
        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

        input.injectKeyRelease(GLFW_KEY_SPACE);

        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
        Assertions.assertTrue(input.isReleased(GLFW_KEY_SPACE));
    }

    @Test
    void quickTapIsNotLostWhenTheReleaseArrivesInTheSameFrame() {
        Input input = new Input();

        input.injectKeyPress(GLFW_KEY_SPACE);
        input.injectKeyRelease(GLFW_KEY_SPACE);

        // the game still gets a chance to see the press
        Assertions.assertTrue(input.isPressed(GLFW_KEY_SPACE));

        input.updateInput();

        Assertions.assertFalse(input.isPressed(GLFW_KEY_SPACE));
        Assertions.assertFalse(input.isHeld(GLFW_KEY_SPACE));
        Assertions.assertTrue(input.isReleased(GLFW_KEY_SPACE));
    }

    @Test
    void repeatedPressOfTheSameKeyIsIgnored() {
        Input input = new Input();

        input.injectKeyPress(GLFW_KEY_SPACE);
        input.injectKeyPress(GLFW_KEY_SPACE);

        input.updateInput();

        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));
    }

    @Test
    void axesAreStoredAndReadable() {
        Input input = new Input();

        Assertions.assertEquals(0f, input.getAxis("leftX"), 0.0001f);

        input.setAxis("leftX", -0.75f);

        Assertions.assertEquals(-0.75f, input.getAxis("leftX"), 0.0001f);
        Assertions.assertTrue(input.hasAxis("leftX"));
        Assertions.assertFalse(input.hasAxis("leftY"));
    }

    @Test
    void heldKeyCanBeCleared() {
        Input input = new Input();

        input.injectKeyPress(GLFW_KEY_A);
        input.updateInput();
        Assertions.assertTrue(input.isHeld(GLFW_KEY_A));

        input.clearKeyHold(GLFW_KEY_A);

        Assertions.assertFalse(input.isHeld(GLFW_KEY_A));
        Assertions.assertFalse(input.isPressed(GLFW_KEY_A));
        Assertions.assertFalse(input.isHeld(GLFW_KEY_D));
    }

    @Test
    void glfwKeyboardCallbackIsRegisteredInTheInput() {
        Input input = new Input();
        Input.KeyboardInputCallback callback = new Input.KeyboardInputCallback();

        callback.invoke(0, GLFW_KEY_W, 0, GLFW_PRESS, 0);
        Assertions.assertTrue(input.isPressed(GLFW_KEY_W));

        input.updateInput();
        Assertions.assertTrue(input.isHeld(GLFW_KEY_W));

        callback.invoke(0, GLFW_KEY_W, 0, GLFW_RELEASE, 0);
        Assertions.assertFalse(input.isHeld(GLFW_KEY_W));
        Assertions.assertTrue(input.isReleased(GLFW_KEY_W));
    }
}
