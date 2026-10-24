package org.north.core.system.remote;

import org.north.core.system.Input;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Delivers the key events of the remote gamepad into the {@link Input} of the engine.
 *
 * <p>Several sources may be bound to the same key, e.g. the "A" button of the gamepad and the
 * left direction of the analog stick. Therefore every key keeps a reference count of its active
 * sources and is only released when the last source lets it go.</p>
 *
 * <p>All methods are safe to call from the HTTP worker threads.</p>
 */
public class RemoteInputBus {

    private final Input input;

    /** key code -> (source id -> active) */
    private final Map<Integer, Map<String, Boolean>> keySources = new LinkedHashMap<>();

    /** client id -> (source id -> key code) */
    private final Map<String, Map<String, Integer>> clientSources = new HashMap<>();

    private final Object lock = new Object();

    public RemoteInputBus(Input input) {
        this.input = input;
    }

    /**
     * Activates a key for the given source.
     *
     * @param clientId id of the client which owns the source
     * @param sourceId unique id of the source within the client
     * @param keyCode  GLFW key code to press
     */
    public void press(String clientId, String sourceId, int keyCode) {
        if (keyCode < 0) return;
        synchronized (lock) {
            Map<String, Integer> sources = clientSources.computeIfAbsent(clientId, id -> new LinkedHashMap<>());
            if (sources.containsKey(sourceId)) return;

            Map<String, Boolean> holders = keySources.computeIfAbsent(keyCode, code -> new LinkedHashMap<>());
            boolean wasIdle = holders.isEmpty();

            // the holder is unique per client, otherwise two clients which press the same button
            // would share one source and the first one to release would free the key for both
            holders.put(holder(clientId, sourceId), Boolean.TRUE);
            sources.put(sourceId, keyCode);

            if (wasIdle) input.injectKeyPress(keyCode);
        }
    }

    /**
     * Deactivates a key of the given source.
     */
    public void release(String clientId, String sourceId, int keyCode) {
        if (keyCode < 0) return;
        synchronized (lock) {
            Map<String, Integer> sources = clientSources.get(clientId);
            if (sources == null) return;
            Integer boundKey = sources.remove(sourceId);
            if (boundKey == null) return;

            releaseKey(boundKey, holder(clientId, sourceId));
        }
    }

    /**
     * Releases every key which belongs to the given client, e.g. when the client disconnects.
     */
    public void releaseClient(String clientId) {
        synchronized (lock) {
            Map<String, Integer> sources = clientSources.remove(clientId);
            if (sources == null) return;

            for (Map.Entry<String, Integer> entry : sources.entrySet()) {
                releaseKey(entry.getValue(), holder(clientId, entry.getKey()));
            }
        }
    }

    /**
     * Builds the id of the holder of a key, which is unique for the whole server.
     */
    private static String holder(String clientId, String sourceId) {
        return clientId + "/" + sourceId;
    }

    private void releaseKey(int keyCode, String sourceId) {
        Map<String, Boolean> holders = keySources.get(keyCode);
        if (holders == null) return;

        holders.remove(sourceId);
        if (holders.isEmpty()) {
            keySources.remove(keyCode);
            input.injectKeyRelease(keyCode);
        }
    }

    /**
     * Updates an analog axis of the engine input.
     */
    public void setAxis(String name, float value) {
        input.setAxis(name, value);
    }

    /**
     * Moves the virtual cursor of the engine input.
     */
    public void setCursorPosition(float x, float y) {
        input.setCursorPosition(x, y);
    }

    /**
     * @return the number of the currently active keys
     */
    public int activeKeyCount() {
        synchronized (lock) {
            return keySources.size();
        }
    }
}
