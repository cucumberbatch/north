package org.north.core.system.remote;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RemoteProtocolTest {

    @Test
    void analogValuesAreEncodedWithoutALoss() {
        Assertions.assertEquals(0, RemoteProtocol.encodeAxis(0f));
        Assertions.assertEquals(RemoteProtocol.AXIS_SCALE, RemoteProtocol.encodeAxis(1f));
        Assertions.assertEquals(-RemoteProtocol.AXIS_SCALE, RemoteProtocol.encodeAxis(-1f));
        Assertions.assertEquals(RemoteProtocol.AXIS_SCALE, RemoteProtocol.encodeAxis(2f));
        Assertions.assertEquals(-RemoteProtocol.AXIS_SCALE, RemoteProtocol.encodeAxis(-2f));
    }

    @Test
    void analogValuesSurviveTheRoundTrip() {
        for (float value : new float[]{0f, 0.25f, -0.5f, 0.9f, -0.9f, 1f, -1f}) {
            float decoded = RemoteProtocol.decodeAxis(RemoteProtocol.encodeAxis(value));
            Assertions.assertEquals(value, decoded, 0.0001f);
        }
    }

    @Test
    void brokenAnalogValuesBecomeZero() {
        Assertions.assertEquals(0, RemoteProtocol.encodeAxis(Float.NaN));
        Assertions.assertEquals(0f, RemoteProtocol.decodeAxis(0), 0.0001f);
    }

    @Test
    void everyControlHasAShortCode() {
        GamepadLayout layout = new GamepadLayout();

        for (GamepadLayout.Control control : layout.controls()) {
            int code = control.getCode();
            Assertions.assertTrue(code > 0 && code < 256, "The code of '" + control.id + "' is out of range");
            Assertions.assertSame(control, layout.byCode(code), "The code of '" + control.id + "' is ambiguous");
        }
    }
}
