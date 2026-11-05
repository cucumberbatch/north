package org.north.core.system.remote;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.north.core.architecture.ApplicationContext;
import org.north.core.graphics.Window;
import org.north.core.system.Input;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;

class RemoteInputServerTest {

    private static final String PORT_PROPERTY = "north.application.remote-input.port";

    private RemoteInputServer server;
    private Input input;
    private String previousPort;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() throws Exception {
        previousPort = System.getProperty(PORT_PROPERTY);
        // port 0 lets the operating system choose a free port
        System.setProperty(PORT_PROPERTY, "0");

        ApplicationContext context = new ApplicationContext();
        Window window = new Window("test", 800, 600, false);
        context.addDependency(Window.class, window);

        input = context.getDependency(Input.class);
        server = new RemoteInputServer(context);
        server.start();

        httpClient = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop();
        if (previousPort == null) System.clearProperty(PORT_PROPERTY);
        else System.setProperty(PORT_PROPERTY, previousPort);
    }

    @Test
    void servesTheGamepadPage() throws IOException {
        HttpURLConnection connection = open("/");
        Assertions.assertEquals(200, connection.getResponseCode());
        Assertions.assertTrue(connection.getHeaderField("Content-Type").contains("text/html"));

        String page = read(connection.getInputStream());
        Assertions.assertTrue(page.contains("stick-left"));
        Assertions.assertFalse(page.contains("__CODES_JSON__"));
        Assertions.assertTrue(page.contains("\"a\":1"));
    }

    @Test
    void buttonEventsAreDeliveredIntoTheEngineInput() throws Exception {
        try (TestClient client = connect()) {
            send(client.socket, button(codeOf("a"), true));

            awaitPressed(GLFW_KEY_SPACE);

            input.updateInput();
            Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

            send(client.socket, button(codeOf("a"), false));

            awaitHeld(GLFW_KEY_SPACE, false);
        }
    }

    @Test
    void axisEventsAreDeliveredIntoTheEngineInput() throws Exception {
        try (TestClient client = connect()) {
            send(client.socket, axis(codeOf("left_x"), -0.9f));

            awaitAxis("leftX", -0.9f);
        }
    }

    @Test
    void severalEventsArePackedIntoASingleMessage() throws Exception {
        try (TestClient client = connect()) {
            ByteBuffer message = ByteBuffer.allocate(7);
            message.put((byte) 2);
            message.put((byte) RemoteProtocol.EVENT_BUTTON);
            message.put((byte) codeOf("a"));
            message.put((byte) 1);
            message.put((byte) RemoteProtocol.EVENT_BUTTON);
            message.put((byte) codeOf("l1"));
            message.put((byte) 1);
            message.flip();
            send(client.socket, message);

            awaitPressed(GLFW_KEY_SPACE);
            awaitPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_Q);

            input.updateInput();
            Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));
            Assertions.assertTrue(input.isHeld(org.lwjgl.glfw.GLFW.GLFW_KEY_Q));
        }
    }

    @Test
    void disconnectingClientReleasesItsKeys() throws Exception {
        TestClient client = connect();
        send(client.socket, button(codeOf("a"), true));
        awaitPressed(GLFW_KEY_SPACE);

        input.updateInput();
        Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));

        client.socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        Assertions.assertTrue(client.closed.await(5, TimeUnit.SECONDS), "The WebSocket did not close");

        // the server needs a moment to notice the closed connection
        awaitHeld(GLFW_KEY_SPACE, false);
    }

    @Test
    void malformedEventsDoNotBreakTheConnection() throws Exception {
        try (TestClient client = connect()) {
            // a truncated event
            send(client.socket, ByteBuffer.wrap(new byte[]{2, (byte) RemoteProtocol.EVENT_BUTTON, 1}));
            // an unknown event type
            send(client.socket, ByteBuffer.wrap(new byte[]{1, 99, 1, 2, 3}));

            send(client.socket, button(codeOf("a"), true));

            awaitPressed(GLFW_KEY_SPACE);

            input.updateInput();
            Assertions.assertTrue(input.isHeld(GLFW_KEY_SPACE));
        }
    }

    @Test
    void theStateOfTheEngineIsReadableOverHttp() throws Exception {
        try (TestClient client = connect()) {
            send(client.socket, button(codeOf("a"), true));
            awaitPressed(GLFW_KEY_SPACE);
            input.updateInput();

            HttpURLConnection connection = open("/api/state");
            Assertions.assertEquals(200, connection.getResponseCode());

            String state = read(connection.getInputStream());
            Assertions.assertTrue(state.contains("\"clients\":1"));
            Assertions.assertTrue(state.contains("\"heldKeys\""));
            Assertions.assertTrue(state.contains(String.valueOf(GLFW_KEY_SPACE)));
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private int codeOf(String id) {
        GamepadLayout.Control control = server.getLayout().get(id);
        Assertions.assertNotNull(control, "Unknown control '" + id + "'");
        return control.getCode();
    }

    /**
     * Waits until the server has delivered the press of the key.
     */
    private void awaitPressed(int key) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (input.isPressed(key)) return;
            Thread.sleep(10);
        }
        Assertions.fail("The key " + key + " was not pressed");
    }

    /**
     * Waits until the key is held, or released, as it is expected.
     */
    private void awaitHeld(int key, boolean expected) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (input.isHeld(key) == expected) return;
            Thread.sleep(10);
        }
        Assertions.fail("The key " + key + (expected ? " was not held" : " is still held"));
    }

    /**
     * Waits until the axis reaches the expected value.
     */
    private void awaitAxis(String name, float expected) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (Math.abs(input.getAxis(name) - expected) < 0.0001f) return;
            Thread.sleep(10);
        }
        Assertions.fail("The axis '" + name + "' is " + input.getAxis(name) + " instead of " + expected);
    }

    private TestClient connect() throws Exception {
        URI uri = URI.create("ws://127.0.0.1:" + server.getBoundPort() + RemoteInputServer.WEBSOCKET_PATH);
        TestClient client = new TestClient();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                client.opened.countDown();
                webSocket.request(1);
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                client.closed.countDown();
                return null;
            }
        };

        client.socket = httpClient.newWebSocketBuilder().buildAsync(uri, listener).get(5, TimeUnit.SECONDS);
        Assertions.assertTrue(client.opened.await(5, TimeUnit.SECONDS), "The WebSocket did not open");
        return client;
    }

    /**
     * A connected client of the gamepad.
     */
    private static class TestClient implements AutoCloseable {
        private final CountDownLatch opened = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private WebSocket socket;

        @Override
        public void close() {
            try {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "");
            } catch (RuntimeException e) {
                // the socket is closed already
            }
            socket.abort();
        }
    }

    private static void send(WebSocket socket, ByteBuffer message) throws Exception {
        socket.sendBinary(message, true).get(5, TimeUnit.SECONDS);
    }

    private static ByteBuffer button(int code, boolean pressed) {
        return ByteBuffer.wrap(new byte[]{
                1,
                (byte) RemoteProtocol.EVENT_BUTTON,
                (byte) code,
                (byte) (pressed ? 1 : 0)
        });
    }

    private static ByteBuffer axis(int code, float value) {
        int raw = RemoteProtocol.encodeAxis(value);
        return ByteBuffer.wrap(new byte[]{
                1,
                (byte) RemoteProtocol.EVENT_AXIS,
                (byte) code,
                (byte) ((raw >> 8) & 0xFF),
                (byte) (raw & 0xFF)
        });
    }

    private HttpURLConnection open(String path) throws IOException {
        URL url = java.net.URI.create("http://127.0.0.1:" + server.getBoundPort() + path).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }

    private String read(InputStream stream) throws IOException {
        try (InputStream inputStream = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = inputStream.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
