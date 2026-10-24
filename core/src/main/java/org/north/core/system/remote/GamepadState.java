package org.north.core.system.remote;

import org.north.core.graphics.Window;
import org.north.core.system.Input;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * State of a single connected client (a phone or a tablet) of the remote gamepad.
 *
 * <p>The state translates the events of the web interface into the events of the engine: digital
 * buttons become key presses of the {@link Input}, analog controls update the axes of the
 * {@link Input} and, additionally, emulate key presses, so a game which is written against
 * keyboard input reacts to the sticks as well.</p>
 */
public class GamepadState {

    /** Axis value which activates the emulated key of a stick direction. */
    private static final float ENGAGE_THRESHOLD = 0.5f;
    /** Axis value which keeps the emulated key of a stick direction activated. */
    private static final float RELEASE_THRESHOLD = 0.3f;

    private final String clientId;
    private final GamepadLayout layout;
    private final RemoteInputBus bus;
    private final Window window;

    private final Map<String, Boolean> buttons = new ConcurrentHashMap<>();
    private final Map<String, Float> axes = new ConcurrentHashMap<>();
    private final Map<String, Boolean> emulatedKeys = new ConcurrentHashMap<>();

    private volatile long lastActivity = System.currentTimeMillis();
    private float cursorX;
    private float cursorY;

    public GamepadState(String clientId, GamepadLayout layout, RemoteInputBus bus, Window window) {
        this.clientId = clientId;
        this.layout = layout;
        this.bus = bus;
        this.window = window;

        if (window != null) {
            this.cursorX = window.getWidth() / 2f;
            this.cursorY = window.getHeight() / 2f;
        }
    }

    public String getClientId() {
        return clientId;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    /**
     * Presses or releases a digital button of the gamepad.
     */
    public void setButton(String id, boolean pressed) {
        GamepadLayout.Control control = layout.get(id);
        if (control == null) return;

        lastActivity = System.currentTimeMillis();
        buttons.put(id, pressed);

        if (control.isAnalog()) {
            // an analog control is driven by its axis, the button event only emulates it
            setAxis(id, pressed ? 1f : 0f);
            return;
        }

        int keyCode = control.getKey();
        if (keyCode < 0) return;

        String source = "button:" + id;
        if (pressed) {
            bus.press(clientId, source, keyCode);
        } else {
            bus.release(clientId, source, keyCode);
        }
    }

    /**
     * Updates an analog control of the gamepad.
     *
     * @param id    id of the control
     * @param value value of the control, within the {@code [-1, 1]} range
     */
    public void setAxis(String id, float value) {
        GamepadLayout.Control control = layout.get(id);
        if (control == null) return;

        lastActivity = System.currentTimeMillis();

        float clamped = Math.max(-1f, Math.min(1f, value));
        axes.put(id, clamped);

        if (control.getAxis() != null) {
            bus.setAxis(control.getAxis(), clamped);
        }

        if (!control.isAnalog()) return;

        // a trigger is bound to a single key which is pressed while the trigger is pulled
        if (control.getKey() >= 0) {
            updateEmulatedKey("axis:" + id, control.getKey(), clamped, 0f);
        }

        // a stick direction is bound to a pair of keys
        updateEmulatedKey("axis:" + id + ":negative", control.getNegativeKey(), clamped, -1f);
        updateEmulatedKey("axis:" + id + ":positive", control.getPositiveKey(), clamped, 1f);
    }

    /**
     * Moves the virtual cursor of the game, e.g. to look around with the camera.
     *
     * @param dx horizontal movement
     * @param dy vertical movement
     */
    public void moveCursor(float dx, float dy) {
        lastActivity = System.currentTimeMillis();

        if (window != null) {
            int width = window.getWidth();
            int height = window.getHeight();
            cursorX = Math.max(0f, Math.min(width - 1f, cursorX + dx));
            cursorY = Math.max(0f, Math.min(height - 1f, cursorY + dy));
        } else {
            cursorX += dx;
            cursorY += dy;
        }

        bus.setCursorPosition(cursorX, cursorY);
    }

    /**
     * Releases every button and axis of the client, e.g. when it disconnects.
     */
    public void releaseAll() {
        for (String id : buttons.keySet()) {
            GamepadLayout.Control control = layout.get(id);
            if (control == null) continue;
            buttons.put(id, Boolean.FALSE);
            if (control.isAnalog() || control.getKey() < 0) continue;
            bus.release(clientId, "button:" + id, control.getKey());
        }

        for (String id : axes.keySet()) {
            GamepadLayout.Control control = layout.get(id);
            if (control == null) continue;
            axes.put(id, 0f);
            if (control.getAxis() != null) bus.setAxis(control.getAxis(), 0f);
            if (control.getKey() >= 0) {
                releaseEmulatedKey("axis:" + id, control.getKey());
            }
            releaseEmulatedKey("axis:" + id + ":negative", control.getNegativeKey());
            releaseEmulatedKey("axis:" + id + ":positive", control.getPositiveKey());
        }

        bus.releaseClient(clientId);
    }

    private void releaseEmulatedKey(String source, int keyCode) {
        if (keyCode < 0) return;
        if (emulatedKeys.remove(source) == null) return;
        bus.release(clientId, source, keyCode);
    }

    /**
     * Activates or deactivates the key which emulates a direction of an analog control.
     *
     * @param source    unique id of the emulated key
     * @param keyCode   key to press
     * @param value     current value of the control
     * @param direction {@code -1} for the negative, {@code 1} for the positive and {@code 0} for
     *                  a control with a single direction
     */
    private void updateEmulatedKey(String source, int keyCode, float value, float direction) {
        if (keyCode < 0) return;

        boolean active = Boolean.TRUE.equals(emulatedKeys.get(source));

        boolean shouldBeActive;
        if (direction < 0) {
            shouldBeActive = active ? value < -RELEASE_THRESHOLD : value <= -ENGAGE_THRESHOLD;
        } else if (direction > 0) {
            shouldBeActive = active ? value > RELEASE_THRESHOLD : value >= ENGAGE_THRESHOLD;
        } else {
            shouldBeActive = active ? value > RELEASE_THRESHOLD : value >= ENGAGE_THRESHOLD;
        }

        if (shouldBeActive == active) return;

        emulatedKeys.put(source, shouldBeActive);
        if (shouldBeActive) {
            bus.press(clientId, source, keyCode);
        } else {
            bus.release(clientId, source, keyCode);
        }
    }

    /**
     * @return a snapshot of the state of the client
     */
    public Map<String, Object> toJson() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("clientId", clientId);
        snapshot.put("lastActivity", lastActivity);

        Map<String, Object> buttonStates = new LinkedHashMap<>();
        for (Map.Entry<String, Boolean> entry : buttons.entrySet()) {
            buttonStates.put(entry.getKey(), entry.getValue());
        }
        snapshot.put("buttons", buttonStates);

        Map<String, Object> axisStates = new LinkedHashMap<>();
        for (Map.Entry<String, Float> entry : axes.entrySet()) {
            axisStates.put(entry.getKey(), entry.getValue());
        }
        snapshot.put("axes", axisStates);

        return snapshot;
    }
}
