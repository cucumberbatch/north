package org.north.core.system;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Central input state of the engine.
 *
 * <p>The state is filled by the GLFW callbacks of the desktop window, but it is also open for
 * any other input source. The remote gamepad server ({@code org.north.core.system.remote})
 * injects its events through {@link #injectKeyPress(int)}, {@link #injectKeyRelease(int)} and
 * {@link #setAxis(String, float)}, so a phone connected over the local network is handled by the
 * very same code path as a physical keyboard.</p>
 *
 * <p>All mutating methods are synchronized, because input events may be delivered from threads
 * other than the render thread (the HTTP worker threads of the remote gamepad server).</p>
 */
public class Input {

    public static final int KEYS_ARRAY_SIZE = 0x100;

    private static Input instance;

    public final BitSet pressedKeys = new BitSet(KEYS_ARRAY_SIZE);
    public final BitSet releasedKeys = new BitSet(KEYS_ARRAY_SIZE);
    public final BitSet heldKeys = new BitSet(KEYS_ARRAY_SIZE);

    public final List<Integer> lastPressedKeys = new ArrayList<>();

    public volatile Vector2f cursorPosition = new Vector2f();

    /**
     * Keys that were pressed but were not consumed by the input update cycle yet.
     * Used to postpone a release which arrives before the game has seen the press.
     */
    private final BitSet pendingPresses = new BitSet(KEYS_ARRAY_SIZE);

    /**
     * Releases which arrived while the matching press was still pending.
     * They are applied right after the press is consumed by {@link #updateInput()}.
     */
    private final List<Integer> deferredReleases = new ArrayList<>();

    /**
     * Analog axes of the input sources, e.g. {@code leftX} / {@code leftY} of the remote gamepad.
     */
    private final Map<String, Float> axes = new ConcurrentHashMap<>();

    private final Object inputLock = new Object();

    public Input() {
        Input.instance = this;
    }

    public static synchronized Input getInstance() {
        if (Input.instance == null) {
            Input.instance = new Input();
        }
        return Input.instance;
    }

    /**
     * Advances the input state by a single frame.
     *
     * <p>Keys which were pressed during the previous frame become "held", so a press is
     * reported to the game exactly once by {@link #isPressed(int)} and stays available through
     * {@link #isHeld(int)} until it is released.</p>
     */
    public void updateInput() {
        synchronized (inputLock) {
            for (Integer key : lastPressedKeys) {
                if (pressedKeys.get(key)) {
                    pressedKeys.set(key, false);
                    heldKeys.set(key, true);
                }
            }
            lastPressedKeys.clear();
            pendingPresses.clear();

            for (Integer key : deferredReleases) {
                releasedKeys.set(key, true);
                pressedKeys.set(key, false);
                heldKeys.set(key, false);
            }
            deferredReleases.clear();
        }
    }

    public boolean isHeld(int key) {
        return heldKeys.get(key);
    }

    public boolean isReleased(int key) {
        return releasedKeys.get(key);
    }

    public boolean isPressed(int key) {
        return pressedKeys.get(key);
    }

    public Vector2f getCursorPosition() {
        return new Vector2f(cursorPosition);
    }

    public Vector2f getCursorPosition(Vector2f destination) {
        return destination.set(cursorPosition);
    }

    public float getCursorX() {
        return cursorPosition.x;
    }

    public float getCursorY() {
        return cursorPosition.y;
    }

    // ------------------------------------------------------------------
    // External input injection
    // ------------------------------------------------------------------

    /**
     * Injects a raw GLFW style key event.
     *
     * @param key   GLFW key code
     * @param action one of {@code GLFW_PRESS}, {@code GLFW_REPEAT}, {@code GLFW_RELEASE}
     */
    public void injectKeyEvent(int key, int action) {
        switch (action) {
            case GLFW_PRESS:
                injectKeyPress(key);
                break;
            case GLFW_REPEAT:
                injectKeyRepeat(key);
                break;
            case GLFW_RELEASE:
                injectKeyRelease(key);
                break;
            default:
                break;
        }
    }

    /**
     * Reports a key press to the engine.
     *
     * <p>The press stays visible for {@link #isPressed(int)} until the next input update and
     * turns into a "held" key afterwards. A release which arrives before the press was consumed
     * is postponed, so quick taps are never lost.</p>
     */
    public void injectKeyPress(int key) {
        if (key < 0) return;
        synchronized (inputLock) {
            deferredReleases.remove(Integer.valueOf(key));
            if (pendingPresses.get(key)) return;

            pressedKeys.set(key, true);
            releasedKeys.set(key, false);
            heldKeys.set(key, false);
            pendingPresses.set(key, true);
            lastPressedKeys.add(key);
        }
    }

    /**
     * Reports a key repeat (a key which is kept pressed) to the engine.
     */
    public void injectKeyRepeat(int key) {
        if (key < 0) return;
        synchronized (inputLock) {
            heldKeys.set(key, true);
        }
    }

    /**
     * Reports a key release to the engine.
     */
    public void injectKeyRelease(int key) {
        if (key < 0) return;
        synchronized (inputLock) {
            if (pendingPresses.get(key)) {
                if (!deferredReleases.contains(key)) deferredReleases.add(key);
                return;
            }

            releasedKeys.set(key, true);
            pressedKeys.set(key, false);
            heldKeys.set(key, false);
        }
    }

    /**
     * Drops the "held" state of a key, e.g. to turn a toggle key into a single shot one.
     */
    public void clearKeyHold(int key) {
        if (key < 0) return;
        synchronized (inputLock) {
            heldKeys.set(key, false);
        }
    }

    // ------------------------------------------------------------------
    // Analog axes
    // ------------------------------------------------------------------

    /**
     * Sets the value of an analog axis, e.g. {@code leftX} of the remote gamepad stick.
     *
     * @param name  axis name
     * @param value axis value, usually within the {@code [-1, 1]} range
     */
    public void setAxis(String name, float value) {
        if (name == null) return;
        axes.put(name, value);
    }

    /**
     * @return the current value of an analog axis, {@code 0} when the axis is unknown
     */
    public float getAxis(String name) {
        Float value = axes.get(name);
        return value == null ? 0f : value;
    }

    public boolean hasAxis(String name) {
        return axes.containsKey(name);
    }

    public Map<String, Float> getAxes() {
        return Collections.unmodifiableMap(axes);
    }

    /**
     * @return the codes of the keys which are currently held down
     */
    public List<Integer> getHeldKeys() {
        List<Integer> keys = new ArrayList<>();
        for (int key = heldKeys.nextSetBit(0); key >= 0; key = heldKeys.nextSetBit(key + 1)) {
            keys.add(key);
        }
        return keys;
    }

    public void resetAxes() {
        axes.clear();
    }

    // ------------------------------------------------------------------
    // Virtual cursor
    // ------------------------------------------------------------------

    /**
     * Moves the virtual cursor, used by the remote gamepad to look around in the game world.
     */
    public void setCursorPosition(float x, float y) {
        cursorPosition = new Vector2f(x, y);
    }

    // ------------------------------------------------------------------
    // GLFW callbacks
    // ------------------------------------------------------------------

    public static class KeyboardInputCallback extends GLFWKeyCallback {
        private final Input input = Input.getInstance();

        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key < 0) return;

            input.injectKeyEvent(key, action);
        }
    }

    public static class MouseInputCallback extends GLFWMouseButtonCallback {
        private final Input input = Input.getInstance();

        @Override
        public void invoke(long window, int button, int action, int mods) {
            input.injectKeyEvent(button, action);
        }
    }

    public static class CursorPositionCallback extends GLFWCursorPosCallback {
        private final Input input = Input.getInstance();

        @Override
        public void invoke(long window, double xpos, double ypos) {
            input.cursorPosition.set((float) xpos, (float) ypos);
        }
    }
}
