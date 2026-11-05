package org.north.core.system.remote;

/**
 * Binary protocol of the remote gamepad.
 *
 * <p>The events of the gamepad are exchanged over a WebSocket connection as binary frames. The
 * protocol is intentionally tiny, because every event of the gamepad travels over the network and
 * a fast reaction of the game world matters more than the comfort of a readable payload.</p>
 *
 * <p>Every message of the protocol is a sequence of events:</p>
 *
 * <pre>
 * byte 0      number of the events in the message (1..255)
 * byte 1..n   the events, one after another
 * </pre>
 *
 * <p>An event is a one byte type followed by its payload:</p>
 *
 * <pre>
 * BUTTON  u8 control code, u8 state (1 = pressed, 0 = released)
 * AXIS    u8 control code, i16 value, scaled by {@link #AXIS_SCALE}
 * CURSOR  i16 horizontal movement, i16 vertical movement
 * PONG    u64 timestamp of the ping which is answered
 * </pre>
 *
 * <p>A button press is therefore three bytes, a stick movement is four bytes and a cursor movement
 * is five bytes. The events of one animation frame are packed into a single message, so a busy
 * stick costs one frame of nine bytes instead of one request per event.</p>
 *
 * <p>The control codes are assigned by the {@link GamepadLayout} and are handed over to the web
 * interface once, when the page is loaded.</p>
 */
public final class RemoteProtocol {

    // ------------------------------------------------------------------
    // Events of the client
    // ------------------------------------------------------------------

    /** A digital button was pressed or released. */
    public static final int EVENT_BUTTON = 1;
    /** An analog control was moved. */
    public static final int EVENT_AXIS = 2;
    /** The virtual cursor was moved. */
    public static final int EVENT_CURSOR = 3;
    /** The answer to a ping of the server. */
    public static final int EVENT_PONG = 4;

    // ------------------------------------------------------------------
    // Messages of the server
    // ------------------------------------------------------------------

    /** The handshake is complete, the payload carries the id of the client. */
    public static final int MESSAGE_ACK = 1;
    /** A ping, the payload carries the timestamp which the client has to echo. */
    public static final int MESSAGE_PING = 2;

    /** Scale of the analog values, an i16 covers the whole {@code [-1, 1]} range. */
    public static final int AXIS_SCALE = 32767;

    /** Size of the timestamp of a ping in bytes. */
    public static final int TIMESTAMP_SIZE = 8;

    private RemoteProtocol() {
    }

    /**
     * Converts an analog value into its wire representation.
     *
     * @param value value of the control, usually within the {@code [-1, 1]} range
     * @return the scaled value
     */
    public static int encodeAxis(float value) {
        if (Float.isNaN(value)) return 0;
        long scaled = Math.round(value * AXIS_SCALE);
        if (scaled > AXIS_SCALE) scaled = AXIS_SCALE;
        if (scaled < -AXIS_SCALE) scaled = -AXIS_SCALE;
        return (int) scaled;
    }

    /**
     * Converts the wire representation of an analog value back into a float.
     */
    public static float decodeAxis(int raw) {
        return raw / (float) AXIS_SCALE;
    }
}
