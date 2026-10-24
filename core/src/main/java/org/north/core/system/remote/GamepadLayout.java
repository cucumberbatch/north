package org.north.core.system.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Description of the virtual gamepad which is rendered by the web interface of the remote input
 * server.
 *
 * <p>Every control of the gamepad is bound to a GLFW key code, to an analog axis or to both.
 * The bindings can be redefined with a properties file without recompiling the engine, see
 * {@link #load()}.</p>
 */
public class GamepadLayout {

    private static final Logger log = LoggerFactory.getLogger(GamepadLayout.class);

    /** System property which points to a file with custom bindings. */
    public static final String BINDINGS_FILE_PROPERTY = "north.remote.gamepad.bindings";
    /** Default name of the bindings file inside the working directory. */
    public static final String DEFAULT_BINDINGS_FILE = "remote-gamepad.properties";
    /** Classpath location of the packaged bindings. */
    public static final String CLASSPATH_BINDINGS = "remote/gamepad.properties";

    public enum ControlType {
        /** A digital button which is either pressed or released. */
        BUTTON,
        /** An analog control, e.g. a stick or a trigger. */
        AXIS
    }

    /**
     * A single control of the virtual gamepad.
     */
    public static class Control {
        public final String id;
        public final String label;
        public final String group;
        public final ControlType type;

        /** Short code of the control, which is used by the binary protocol. */
        private int code = -1;
        private int key = -1;
        private String axis = null;
        private int negativeKey = -1;
        private int positiveKey = -1;

        Control(String id, String label, String group, ControlType type) {
            this.id = id;
            this.label = label;
            this.group = group;
            this.type = type;
        }

        /**
         * @return the short code of the control, which identifies it in the binary protocol
         */
        public int getCode() {
            return code;
        }

        /** @return the GLFW key code bound to the control, {@code -1} when it is not bound */
        public int getKey() {
            return key;
        }

        /** @return the name of the analog axis of the control, {@code null} when it is not analog */
        public String getAxis() {
            return axis;
        }

        /** @return the key which is pressed when the axis is moved into its negative range */
        public int getNegativeKey() {
            return negativeKey;
        }

        /** @return the key which is pressed when the axis is moved into its positive range */
        public int getPositiveKey() {
            return positiveKey;
        }

        public boolean isAnalog() {
            return type == ControlType.AXIS;
        }

        void setKey(int key) {
            this.key = key;
        }

        void setCode(int code) {
            this.code = code;
        }

        void setAxis(String axis) {
            this.axis = axis;
        }

        void setNegativeKey(int negativeKey) {
            this.negativeKey = negativeKey;
        }

        void setPositiveKey(int positiveKey) {
            this.positiveKey = positiveKey;
        }
    }

    private final Map<String, Control> controls = new LinkedHashMap<>();
    private final Map<Integer, Control> controlsByCode = new LinkedHashMap<>();

    /** Codes of the controls are handed over to the web interface, so they have to be short. */
    private int codeCounter = 0;

    public GamepadLayout() {
        registerDefaults();
    }

    private void registerDefaults() {
        // face buttons
        addButton("a", "A", "face", GlfwKeys.keyCode("SPACE"));
        addButton("b", "B", "face", GlfwKeys.keyCode("ESCAPE"));
        addButton("x", "X", "face", GlfwKeys.keyCode("I"));
        addButton("y", "Y", "face", GlfwKeys.keyCode("Y"));

        // direction pad
        addButton("dpad_up", "UP", "dpad", GlfwKeys.keyCode("UP"));
        addButton("dpad_down", "DOWN", "dpad", GlfwKeys.keyCode("DOWN"));
        addButton("dpad_left", "LEFT", "dpad", GlfwKeys.keyCode("LEFT"));
        addButton("dpad_right", "RIGHT", "dpad", GlfwKeys.keyCode("RIGHT"));

        // shoulder buttons and triggers
        addButton("l1", "L1", "shoulder", GlfwKeys.keyCode("Q"));
        addButton("r1", "R1", "shoulder", GlfwKeys.keyCode("E"));
        addAxis("l2", "L2", "trigger", "l2", GlfwKeys.keyCode("LEFT_CONTROL"), -1, -1);
        addAxis("r2", "R2", "trigger", "r2", GlfwKeys.keyCode("RIGHT_CONTROL"), -1, -1);

        // system buttons
        addButton("start", "START", "system", GlfwKeys.keyCode("ENTER"));
        addButton("select", "SELECT", "system", GlfwKeys.keyCode("G"));
        addButton("pause", "PAUSE", "system", GlfwKeys.keyCode("P"));
        addButton("lmb", "MOUSE", "system", GlfwKeys.keyCode(GlfwKeys.MOUSE_LEFT));

        // analog sticks
        addAxis("left_x", "L", "stick", "leftX", -1, GlfwKeys.keyCode("A"), GlfwKeys.keyCode("D"));
        addAxis("left_y", "L", "stick", "leftY", -1, GlfwKeys.keyCode("S"), GlfwKeys.keyCode("W"));
        addAxis("right_x", "R", "stick", "rightX", -1, GlfwKeys.keyCode("LEFT"), GlfwKeys.keyCode("RIGHT"));
        addAxis("right_y", "R", "stick", "rightY", -1, GlfwKeys.keyCode("DOWN"), GlfwKeys.keyCode("UP"));
    }

    private void addButton(String id, String label, String group, int key) {
        Control control = new Control(id, label, group, ControlType.BUTTON);
        control.setKey(key);
        register(control);
    }

    private void addAxis(String id, String label, String group, String axis, int key, int negativeKey, int positiveKey) {
        Control control = new Control(id, label, group, ControlType.AXIS);
        control.setAxis(axis);
        control.setKey(key);
        control.setNegativeKey(negativeKey);
        control.setPositiveKey(positiveKey);
        register(control);
    }

    private void register(Control control) {
        control.setCode(++codeCounter);
        controls.put(control.id, control);
        controlsByCode.put(control.getCode(), control);
    }

    public Control get(String id) {
        return id == null ? null : controls.get(id);
    }

    /**
     * @param code short code of a control, as it is used by the binary protocol
     * @return the control, {@code null} when the code is unknown
     */
    public Control byCode(int code) {
        return controlsByCode.get(code);
    }

    /**
     * @return the short codes of all controls, indexed by their id
     */
    public Map<String, Object> codeMap() {
        Map<String, Object> codes = new LinkedHashMap<>();
        for (Control control : controls.values()) {
            codes.put(control.id, control.getCode());
        }
        return codes;
    }

    public Collection<Control> controls() {
        return controls.values();
    }

    public List<Control> controlsOfGroup(String group) {
        List<Control> result = new ArrayList<>();
        for (Control control : controls.values()) {
            if (control.group.equals(group)) result.add(control);
        }
        return result;
    }

    /**
     * Loads the layout and applies the custom bindings, if there are any.
     *
     * <p>The bindings are looked up in the following order:</p>
     * <ol>
     *     <li>the file pointed to by the {@value #BINDINGS_FILE_PROPERTY} system property,</li>
     *     <li>{@value #DEFAULT_BINDINGS_FILE} inside the working directory,</li>
     *     <li>{@value #CLASSPATH_BINDINGS} inside the classpath.</li>
     * </ol>
     */
    public static GamepadLayout load() {
        GamepadLayout layout = new GamepadLayout();
        Properties properties = readBindings();
        if (properties != null) layout.apply(properties);
        return layout;
    }

    private static Properties readBindings() {
        String configuredPath = System.getProperty(BINDINGS_FILE_PROPERTY);
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            File file = new File(configuredPath);
            if (file.isFile()) {
                Properties properties = readProperties(file);
                if (properties != null) {
                    log.info("Remote gamepad bindings loaded from '{}'", file.getAbsolutePath());
                    return properties;
                }
            } else {
                log.warn("Remote gamepad bindings file '{}' does not exist", file.getAbsolutePath());
            }
        }

        File localFile = new File(DEFAULT_BINDINGS_FILE);
        if (localFile.isFile()) {
            Properties properties = readProperties(localFile);
            if (properties != null) {
                log.info("Remote gamepad bindings loaded from '{}'", localFile.getAbsolutePath());
                return properties;
            }
        }

        ClassLoader classLoader = GamepadLayout.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(CLASSPATH_BINDINGS)) {
            if (stream == null) return null;
            Properties properties = new Properties();
            properties.load(stream);
            log.info("Remote gamepad bindings loaded from the classpath resource '{}'", CLASSPATH_BINDINGS);
            return properties;
        } catch (IOException e) {
            log.warn("Failed to read the remote gamepad bindings from the classpath", e);
            return null;
        }
    }

    private static Properties readProperties(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            Properties properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            log.warn("Failed to read the remote gamepad bindings from '{}'", file.getAbsolutePath(), e);
            return null;
        }
    }

    /**
     * Applies custom bindings.
     *
     * <p>Supported properties are {@code <control>.key}, {@code <control>.axis},
     * {@code <control>.negative} and {@code <control>.positive}.</p>
     */
    public void apply(Properties properties) {
        for (String name : properties.stringPropertyNames()) {
            int separator = name.indexOf('.');
            if (separator <= 0) continue;

            String controlId = name.substring(0, separator);
            String property = name.substring(separator + 1);
            String value = properties.getProperty(name);

            Control control = controls.get(controlId);
            if (control == null) {
                log.warn("Unknown remote gamepad control '{}' in the bindings", controlId);
                continue;
            }

            switch (property) {
                case "key": {
                    int key = GlfwKeys.keyCode(value);
                    if (key < 0) {
                        log.warn("Unknown key '{}' for the remote gamepad control '{}'", value, controlId);
                    } else {
                        control.setKey(key);
                    }
                    break;
                }
                case "axis":
                    control.setAxis(value.trim());
                    break;
                case "negative": {
                    int key = GlfwKeys.keyCode(value);
                    if (key < 0) {
                        log.warn("Unknown key '{}' for the remote gamepad control '{}'", value, controlId);
                    } else {
                        control.setNegativeKey(key);
                    }
                    break;
                }
                case "positive": {
                    int key = GlfwKeys.keyCode(value);
                    if (key < 0) {
                        log.warn("Unknown key '{}' for the remote gamepad control '{}'", value, controlId);
                    } else {
                        control.setPositiveKey(key);
                    }
                    break;
                }
                default:
                    log.warn("Unknown remote gamepad binding property '{}'", name);
                    break;
            }
        }
    }

    /**
     * @return the layout in a form which is understood by the web interface
     */
    public Map<String, Object> toJson() {
        List<Map<String, Object>> controlList = new ArrayList<>();
        for (Control control : controls.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", control.id);
            entry.put("label", control.label);
            entry.put("group", control.group);
            entry.put("type", control.type.name().toLowerCase());
            entry.put("key", control.getKey());
            entry.put("axis", control.getAxis());
            controlList.add(entry);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("controls", controlList);
        return root;
    }
}
