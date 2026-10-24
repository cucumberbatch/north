package org.north.core.system.remote;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Maps human readable key names to GLFW key codes.
 *
 * <p>Used by the binding configuration of the remote gamepad, so the bindings can be changed
 * with a plain properties file instead of recompiling the engine.</p>
 */
public final class GlfwKeys {

    /** Name of the pseudo key which maps to the left mouse button. */
    public static final String MOUSE_LEFT = "MOUSE_LEFT";
    /** Name of the pseudo key which maps to the right mouse button. */
    public static final String MOUSE_RIGHT = "MOUSE_RIGHT";
    /** Name of the pseudo key which maps to the middle mouse button. */
    public static final String MOUSE_MIDDLE = "MOUSE_MIDDLE";

    private static final Map<String, Integer> KEYS = new HashMap<>();

    static {
        for (char c = 'A'; c <= 'Z'; c++) KEYS.put(String.valueOf(c), GLFW_KEY_A + (c - 'A'));
        for (char c = '0'; c <= '9'; c++) KEYS.put(String.valueOf(c), GLFW_KEY_0 + (c - '0'));
        for (int i = 1; i <= 12; i++) KEYS.put("F" + i, GLFW_KEY_F1 + (i - 1));

        KEYS.put("SPACE", GLFW_KEY_SPACE);
        KEYS.put("ESCAPE", GLFW_KEY_ESCAPE);
        KEYS.put("ENTER", GLFW_KEY_ENTER);
        KEYS.put("TAB", GLFW_KEY_TAB);
        KEYS.put("BACKSPACE", GLFW_KEY_BACKSPACE);
        KEYS.put("DELETE", GLFW_KEY_DELETE);
        KEYS.put("INSERT", GLFW_KEY_INSERT);
        KEYS.put("HOME", GLFW_KEY_HOME);
        KEYS.put("END", GLFW_KEY_END);
        KEYS.put("PAGE_UP", GLFW_KEY_PAGE_UP);
        KEYS.put("PAGE_DOWN", GLFW_KEY_PAGE_DOWN);
        KEYS.put("UP", GLFW_KEY_UP);
        KEYS.put("DOWN", GLFW_KEY_DOWN);
        KEYS.put("LEFT", GLFW_KEY_LEFT);
        KEYS.put("RIGHT", GLFW_KEY_RIGHT);
        KEYS.put("LEFT_SHIFT", GLFW_KEY_LEFT_SHIFT);
        KEYS.put("RIGHT_SHIFT", GLFW_KEY_RIGHT_SHIFT);
        KEYS.put("LEFT_CONTROL", GLFW_KEY_LEFT_CONTROL);
        KEYS.put("RIGHT_CONTROL", GLFW_KEY_RIGHT_CONTROL);
        KEYS.put("LEFT_ALT", GLFW_KEY_LEFT_ALT);
        KEYS.put("RIGHT_ALT", GLFW_KEY_RIGHT_ALT);
        KEYS.put("LEFT_SUPER", GLFW_KEY_LEFT_SUPER);
        KEYS.put("RIGHT_SUPER", GLFW_KEY_RIGHT_SUPER);
        KEYS.put("CAPS_LOCK", GLFW_KEY_CAPS_LOCK);
        KEYS.put("SCROLL_LOCK", GLFW_KEY_SCROLL_LOCK);
        KEYS.put("PRINT_SCREEN", GLFW_KEY_PRINT_SCREEN);
        KEYS.put("PAUSE", GLFW_KEY_PAUSE);
        KEYS.put("MENU", GLFW_KEY_MENU);
        KEYS.put("KP_0", GLFW_KEY_KP_0);
        KEYS.put("KP_1", GLFW_KEY_KP_1);
        KEYS.put("KP_2", GLFW_KEY_KP_2);
        KEYS.put("KP_3", GLFW_KEY_KP_3);
        KEYS.put("KP_4", GLFW_KEY_KP_4);
        KEYS.put("KP_5", GLFW_KEY_KP_5);
        KEYS.put("KP_6", GLFW_KEY_KP_6);
        KEYS.put("KP_7", GLFW_KEY_KP_7);
        KEYS.put("KP_8", GLFW_KEY_KP_8);
        KEYS.put("KP_9", GLFW_KEY_KP_9);
        KEYS.put("KP_ENTER", GLFW_KEY_KP_ENTER);
        KEYS.put("KP_ADD", GLFW_KEY_KP_ADD);
        KEYS.put("KP_SUBTRACT", GLFW_KEY_KP_SUBTRACT);
        KEYS.put("KP_MULTIPLY", GLFW_KEY_KP_MULTIPLY);
        KEYS.put("KP_DIVIDE", GLFW_KEY_KP_DIVIDE);

        KEYS.put(MOUSE_LEFT, GLFW_MOUSE_BUTTON_LEFT);
        KEYS.put(MOUSE_RIGHT, GLFW_MOUSE_BUTTON_RIGHT);
        KEYS.put(MOUSE_MIDDLE, GLFW_MOUSE_BUTTON_MIDDLE);
    }

    private GlfwKeys() {
    }

    /**
     * @return the GLFW key code of a key name, or {@code -1} when the name is unknown
     */
    public static int keyCode(String name) {
        if (name == null) return -1;
        Integer code = KEYS.get(name.trim().toUpperCase());
        return code == null ? -1 : code;
    }

    /**
     * @return the name of a GLFW key code, or {@code null} when the code is unknown
     */
    public static String keyName(int keyCode) {
        for (Map.Entry<String, Integer> entry : KEYS.entrySet()) {
            if (entry.getValue() == keyCode) return entry.getKey();
        }
        return null;
    }

    public static Map<String, Integer> all() {
        return Collections.unmodifiableMap(KEYS);
    }
}
