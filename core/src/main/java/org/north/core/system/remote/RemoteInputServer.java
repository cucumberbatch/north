package org.north.core.system.remote;

import org.north.core.architecture.ApplicationContext;
import org.north.core.config.ApplicationProperties;
import org.north.core.graphics.Window;
import org.north.core.system.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A small web server which turns a phone of the local network into a gamepad of the running
 * engine.
 *
 * <p>The server is started together with the engine, serves a touch friendly gamepad page and
 * accepts the input events of the page. Every event is delivered into the {@link Input} of the
 * engine, therefore the game world reacts to the phone in realtime exactly like it reacts to a
 * physical keyboard.</p>
 *
 * <p>The events travel over a WebSocket connection as binary frames, see {@link RemoteProtocol}.
 * A button press costs three bytes of payload and one frame of overhead, which keeps the latency
 * of the input low and the load of the network negligible. The page itself is served as plain
 * HTTP by the very same port.</p>
 *
 * <p>Configuration properties (see {@code north.properties}):</p>
 * <ul>
 *     <li>{@code application.remote-input.enabled} - enables the server, {@code true} by default</li>
 *     <li>{@code application.remote-input.port} - port of the server, {@code 8080} by default</li>
 *     <li>{@code application.remote-input.bind} - bind address, {@code 0.0.0.0} by default</li>
 *     <li>{@code application.remote-input.auto-open-browser} - opens the page in the default
 *     browser of the machine which runs the engine, {@code false} by default</li>
 *     <li>{@code application.remote-input.cursor-speed} - sensitivity of the virtual cursor,
 *     {@code 1.0} by default</li>
 * </ul>
 */
public class RemoteInputServer {

    private static final Logger log = LoggerFactory.getLogger(RemoteInputServer.class);

    private static final String PAGE_RESOURCE = "remote/gamepad.html";
    private static final String CODES_PLACEHOLDER = "__CODES_JSON__";

    /** Path of the WebSocket endpoint which carries the events. */
    public static final String WEBSOCKET_PATH = "/ws";

    /** Magic value of RFC 6455 which is used to build the answer of the handshake. */
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static final int MAX_FRAME_SIZE = 64 * 1024;
    private static final int PING_INTERVAL_MILLIS = 2000;
    private static final int PING_TIMEOUT_MILLIS = 10000;

    private static final int OPCODE_CONTINUATION = 0x0;
    private static final int OPCODE_TEXT = 0x1;
    private static final int OPCODE_BINARY = 0x2;
    private static final int OPCODE_CLOSE = 0x8;
    private static final int OPCODE_PING = 0x9;
    private static final int OPCODE_PONG = 0xA;

    private final Input input;
    private final Window window;
    private final GamepadLayout layout;
    private final RemoteInputBus bus;

    private final Map<String, GamepadState> clients = new ConcurrentHashMap<>();
    private final Map<String, ClientConnection> connections = new ConcurrentHashMap<>();
    private final Set<Socket> openSockets = ConcurrentHashMap.newKeySet();
    private final AtomicInteger clientCounter = new AtomicInteger();

    private final String bindAddress;
    private final int port;
    private final boolean enabled;
    private final boolean autoOpenBrowser;
    private final float cursorSpeed;

    private volatile ServerSocket serverSocket;
    private volatile boolean running;
    private volatile String page;
    private ExecutorService workers;
    private ScheduledExecutorService scheduler;
    private Thread acceptThread;

    public RemoteInputServer(ApplicationContext context) {
        this.input = context.getDependency(Input.class);
        this.window = context.getDependency(Window.class);
        this.layout = GamepadLayout.load();
        this.bus = new RemoteInputBus(input);

        this.bindAddress = property("application.remote-input.bind", "0.0.0.0");
        this.port = intProperty("application.remote-input.port", 8080);
        this.enabled = booleanProperty("application.remote-input.enabled", true);
        this.autoOpenBrowser = booleanProperty("application.remote-input.auto-open-browser", false);
        this.cursorSpeed = floatProperty("application.remote-input.cursor-speed", 1.0f);
    }

    // ------------------------------------------------------------------
    // Life cycle
    // ------------------------------------------------------------------

    /**
     * Starts the web server. A failure does not stop the engine, the game stays playable with
     * the keyboard in that case.
     */
    public void start() {
        if (running) return;

        if (!enabled) {
            log.info("The remote gamepad server is disabled");
            return;
        }

        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(bindAddress, port), 32);
            serverSocket = socket;
        } catch (IOException e) {
            log.error("Failed to start the remote gamepad server on {}:{}", bindAddress, port, e);
            serverSocket = null;
            return;
        }

        running = true;
        workers = Executors.newCachedThreadPool(daemonThreadFactory("remote-gamepad-"));
        scheduler = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory("remote-gamepad-ping-"));

        acceptThread = new Thread(this::acceptLoop, "remote-gamepad-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        scheduler.scheduleAtFixedRate(this::pingClients, PING_INTERVAL_MILLIS, PING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);

        List<String> urls = localUrls();
        log.info("Remote gamepad server is up on http://{}:{}/", bindAddress, getBoundPort());
        if (urls.isEmpty()) {
            log.info("Open http://127.0.0.1:{}/ on the machine which runs the engine", getBoundPort());
        } else {
            for (String url : urls) {
                log.info("Open the remote gamepad on your phone: {}", url);
            }
        }

        if (autoOpenBrowser) {
            openBrowser(urls.isEmpty() ? "http://127.0.0.1:" + getBoundPort() + "/" : urls.get(0));
        }
    }

    /**
     * Stops the web server and releases every key which is still pressed.
     */
    public void stop() {
        running = false;

        for (ClientConnection connection : connections.values()) {
            connection.close();
        }
        connections.clear();

        for (Socket socket : openSockets) {
            closeQuietly(socket);
        }
        openSockets.clear();

        for (GamepadState client : clients.values()) {
            client.releaseAll();
        }
        clients.clear();

        ServerSocket socket = serverSocket;
        serverSocket = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.debug("Failed to close the remote gamepad server socket", e);
            }
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (workers != null) {
            workers.shutdownNow();
            workers = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    /**
     * @return the port of the server, which differs from the configured one when the port was
     * chosen by the operating system
     */
    public int getBoundPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? port : socket.getLocalPort();
    }

    /**
     * @return the URLs which are reachable from the local network
     */
    public List<String> localUrls() {
        List<String> urls = new ArrayList<>();
        for (String address : localAddresses()) {
            urls.add("http://" + address + ":" + getBoundPort() + "/");
        }
        return urls;
    }

    /**
     * @return the IPv4 addresses of all non loopback network interfaces
     */
    public static List<String> localAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()) continue;

                Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    InetAddress address = interfaceAddresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            log.warn("Failed to enumerate the network interfaces", e);
        }
        return addresses;
    }

    // ------------------------------------------------------------------
    // Connections
    // ------------------------------------------------------------------

    private void acceptLoop() {
        while (running) {
            ServerSocket socket = serverSocket;
            if (socket == null) return;

            try {
                Socket accepted = socket.accept();
                accepted.setTcpNoDelay(true);
                openSockets.add(accepted);

                ExecutorService executor = workers;
                if (executor == null) {
                    closeQuietly(accepted);
                    return;
                }
                executor.execute(() -> handleConnection(accepted));
            } catch (IOException e) {
                if (running) log.warn("Failed to accept a remote gamepad connection", e);
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream(), 8192);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 8192);

            while (running) {
                HttpRequest request = readRequest(in);
                if (request == null) return;

                if (request.isWebSocketUpgrade() && WEBSOCKET_PATH.equals(request.path)) {
                    runWebSocket(in, out, request);
                    return;
                }

                serveHttp(out, request);
                if (request.closesConnection()) return;
            }
        } catch (IOException e) {
            // the client went away, nothing has to be reported
        } catch (RuntimeException e) {
            log.warn("Failed to handle a remote gamepad connection", e);
        } finally {
            openSockets.remove(socket);
            closeQuietly(socket);
        }
    }

    // ------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------

    private void serveHttp(OutputStream out, HttpRequest request) throws IOException {
        String path = request.path;

        if (!"GET".equalsIgnoreCase(request.method)) {
            // the body of the request is not read, therefore the connection has to be closed
            request.forceClose = true;
            writeResponse(out, 405, "text/plain; charset=utf-8", "Only GET is supported".getBytes(StandardCharsets.UTF_8));
            return;
        }

        if ("/favicon.ico".equals(path)) {
            writeResponse(out, 204, "image/x-icon", new byte[0]);
            return;
        }
        if ("/".equals(path) || "/index.html".equals(path)) {
            writeResponse(out, 200, "text/html; charset=utf-8", page().getBytes(StandardCharsets.UTF_8));
            return;
        }
        if ("/api/ping".equals(path)) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("ok", Boolean.TRUE);
            response.put("clients", clients.size());
            writeResponse(out, 200, "application/json; charset=utf-8", Json.write(response).getBytes(StandardCharsets.UTF_8));
            return;
        }
        if ("/api/state".equals(path)) {
            writeResponse(out, 200, "application/json; charset=utf-8", Json.write(stateSnapshot()).getBytes(StandardCharsets.UTF_8));
            return;
        }

        writeResponse(out, 404, "text/plain; charset=utf-8", "Not found".getBytes(StandardCharsets.UTF_8));
    }

    private void writeResponse(OutputStream out, int status, String contentType, byte[] body) throws IOException {
        StringBuilder head = new StringBuilder(160);
        head.append("HTTP/1.1 ").append(status).append(' ').append(reason(status)).append("\r\n");
        head.append("Content-Type: ").append(contentType).append("\r\n");
        head.append("Content-Length: ").append(body.length).append("\r\n");
        head.append("Cache-Control: no-store\r\n");
        head.append("Access-Control-Allow-Origin: *\r\n");
        head.append("Connection: keep-alive\r\n");
        head.append("\r\n");

        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    private Map<String, Object> stateSnapshot() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", Boolean.TRUE);
        response.put("clients", clients.size());
        response.put("layout", layout.toJson());
        response.put("axes", input.getAxes());
        response.put("heldKeys", input.getHeldKeys());

        List<Object> clientStates = new ArrayList<>();
        for (GamepadState client : clients.values()) {
            clientStates.add(client.toJson());
        }
        response.put("states", clientStates);
        return response;
    }

    // ------------------------------------------------------------------
    // WebSocket
    // ------------------------------------------------------------------

    /**
     * Performs the handshake of RFC 6455 and reads the events of the client until the connection
     * is closed.
     */
    private void runWebSocket(BufferedInputStream in, BufferedOutputStream out, HttpRequest request) throws IOException {
        String key = request.header("sec-websocket-key");
        if (key == null) {
            writeResponse(out, 400, "text/plain; charset=utf-8", "Missing Sec-WebSocket-Key".getBytes(StandardCharsets.UTF_8));
            return;
        }

        StringBuilder response = new StringBuilder(160);
        response.append("HTTP/1.1 101 Switching Protocols\r\n");
        response.append("Upgrade: websocket\r\n");
        response.append("Connection: Upgrade\r\n");
        response.append("Sec-WebSocket-Accept: ").append(websocketAccept(key)).append("\r\n");
        response.append("\r\n");
        out.write(response.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.flush();

        int number = clientCounter.incrementAndGet();
        ClientConnection connection = new ClientConnection("client-" + number, number, in, out);
        connections.put(connection.id, connection);

        // the state of the client is created right away, so a connected client is visible even
        // when it does not touch the gamepad yet
        connection.state();

        // the handshake is complete, the client may send its events now
        connection.sendInt(RemoteProtocol.MESSAGE_ACK, number);

        log.info("Remote gamepad client '{}' connected", connection.id);

        try {
            readFrames(in, connection);
        } finally {
            connections.remove(connection.id);
            disconnect(connection.id);
            log.info("Remote gamepad client '{}' disconnected", connection.id);
        }
    }

    private void readFrames(BufferedInputStream in, ClientConnection connection) throws IOException {
        ByteArrayOutputStream fragments = new ByteArrayOutputStream();

        while (running) {
            int first = in.read();
            if (first < 0) return;
            int second = in.read();
            if (second < 0) return;

            boolean finalFragment = (first & 0x80) != 0;
            int opcode = first & 0x0F;
            boolean masked = (second & 0x80) != 0;
            long length = second & 0x7F;

            if (length == 126) {
                length = ((long) readByte(in) << 8) | readByte(in);
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | readByte(in);
                }
            }

            if (length < 0 || length > MAX_FRAME_SIZE) {
                throw new IOException("The remote gamepad frame is too large: " + length);
            }

            byte[] mask = null;
            if (masked) {
                mask = new byte[4];
                readFully(in, mask);
            }

            byte[] payload = new byte[(int) length];
            readFully(in, payload);

            if (mask != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i & 3];
                }
            }

            switch (opcode) {
                case OPCODE_CONTINUATION:
                    fragments.write(payload);
                    if (finalFragment) {
                        handleMessage(connection, fragments.toByteArray());
                        fragments.reset();
                    }
                    break;
                case OPCODE_TEXT:
                    // the protocol of the gamepad is binary, a text frame is ignored
                    break;
                case OPCODE_BINARY:
                    if (finalFragment) {
                        handleMessage(connection, payload);
                    } else {
                        fragments.write(payload);
                    }
                    break;
                case OPCODE_CLOSE:
                    connection.sendClose();
                    return;
                case OPCODE_PING:
                    connection.sendFrame(OPCODE_PONG, payload);
                    break;
                case OPCODE_PONG:
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Handles a message of the client, which carries one or more events.
     */
    private void handleMessage(ClientConnection connection, byte[] payload) {
        if (payload.length < 1) return;

        int count = payload[0] & 0xFF;
        int offset = 1;

        for (int i = 0; i < count && offset < payload.length; i++) {
            int type = payload[offset++] & 0xFF;

            switch (type) {
                case RemoteProtocol.EVENT_BUTTON: {
                    if (offset + 2 > payload.length) return;
                    int code = payload[offset++] & 0xFF;
                    boolean pressed = payload[offset++] != 0;
                    GamepadLayout.Control control = layout.byCode(code);
                    if (control != null) connection.state().setButton(control.id, pressed);
                    break;
                }
                case RemoteProtocol.EVENT_AXIS: {
                    if (offset + 3 > payload.length) return;
                    int code = payload[offset++] & 0xFF;
                    int raw = (short) (((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF));
                    GamepadLayout.Control control = layout.byCode(code);
                    if (control != null) connection.state().setAxis(control.id, RemoteProtocol.decodeAxis(raw));
                    break;
                }
                case RemoteProtocol.EVENT_CURSOR: {
                    if (offset + 4 > payload.length) return;
                    int dx = (short) (((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF));
                    int dy = (short) (((payload[offset++] & 0xFF) << 8) | (payload[offset++] & 0xFF));
                    connection.state().moveCursor(dx * cursorSpeed, dy * cursorSpeed);
                    break;
                }
                case RemoteProtocol.EVENT_PONG: {
                    if (offset + RemoteProtocol.TIMESTAMP_SIZE > payload.length) return;
                    offset += RemoteProtocol.TIMESTAMP_SIZE;
                    connection.onPong();
                    break;
                }
                default:
                    // an unknown event is skipped, the stream stays in sync because every
                    // event is self describing
                    break;
            }
        }
    }

    private void pingClients() {
        long now = System.currentTimeMillis();
        for (ClientConnection connection : connections.values()) {
            if (now - connection.lastPong() > PING_TIMEOUT_MILLIS) {
                log.warn("The remote gamepad client '{}' does not answer, closing the connection", connection.id);
                connection.close();
                continue;
            }
            connection.sendLong(RemoteProtocol.MESSAGE_PING, now);
        }
    }

    private void disconnect(String clientId) {
        GamepadState state = clients.remove(clientId);
        if (state != null) state.releaseAll();
    }

    // ------------------------------------------------------------------
    // Client connection
    // ------------------------------------------------------------------

    /**
     * A single connected client of the remote gamepad.
     */
    private class ClientConnection {
        private final String id;
        private final int number;
        private final BufferedInputStream in;
        private final BufferedOutputStream out;
        private final Object writeLock = new Object();
        private volatile long lastPong = System.currentTimeMillis();
        private volatile GamepadState state;

        ClientConnection(String id, int number, BufferedInputStream in, BufferedOutputStream out) {
            this.id = id;
            this.number = number;
            this.in = in;
            this.out = out;
        }

        String getId() {
            return id;
        }

        long lastPong() {
            return lastPong;
        }

        void onPong() {
            lastPong = System.currentTimeMillis();
        }

        /**
         * @return the state of the client, it is created on the first event
         */
        GamepadState state() {
            GamepadState current = state;
            if (current != null) return current;

            synchronized (this) {
                if (state == null) {
                    GamepadState created = new GamepadState(id, layout, bus, window);
                    GamepadState existing = clients.putIfAbsent(id, created);
                    state = existing != null ? existing : created;
                }
                return state;
            }
        }

        /**
         * Sends a message of the server, which carries a single event with a 32 bit value.
         *
         * @param type     type of the event
         * @param intValue value of the event
         */
        void sendInt(int type, int intValue) {
            byte[] payload = new byte[6];
            payload[0] = 1;
            payload[1] = (byte) type;
            payload[2] = (byte) ((intValue >> 24) & 0xFF);
            payload[3] = (byte) ((intValue >> 16) & 0xFF);
            payload[4] = (byte) ((intValue >> 8) & 0xFF);
            payload[5] = (byte) (intValue & 0xFF);
            sendFrame(OPCODE_BINARY, payload);
        }

        /**
         * Sends a message of the server, which carries a single event with a 64 bit value.
         *
         * @param type      type of the event
         * @param longValue value of the event
         */
        void sendLong(int type, long longValue) {
            byte[] payload = new byte[10];
            payload[0] = 1;
            payload[1] = (byte) type;
            for (int i = 0; i < RemoteProtocol.TIMESTAMP_SIZE; i++) {
                payload[2 + i] = (byte) ((longValue >> (56 - 8 * i)) & 0xFF);
            }
            sendFrame(OPCODE_BINARY, payload);
        }

        void sendClose() {
            byte[] payload = new byte[]{0x03, (byte) 0xE8}; // 1000, normal closure
            sendFrame(OPCODE_CLOSE, payload);
        }

        /**
         * Writes a single frame into the connection.
         */
        void sendFrame(int opcode, byte[] payload) {
            synchronized (writeLock) {
                try {
                    out.write(0x80 | opcode);

                    if (payload.length < 126) {
                        out.write(payload.length);
                    } else if (payload.length <= 0xFFFF) {
                        out.write(126);
                        out.write((payload.length >> 8) & 0xFF);
                        out.write(payload.length & 0xFF);
                    } else {
                        out.write(127);
                        for (int shift = 56; shift >= 0; shift -= 8) {
                            out.write((int) (((long) payload.length >> shift) & 0xFF));
                        }
                    }

                    out.write(payload);
                    out.flush();
                } catch (IOException e) {
                    close();
                }
            }
        }

        void close() {
            closeQuietly(out);
            closeQuietly(in);
        }
    }

    // ------------------------------------------------------------------
    // HTTP request
    // ------------------------------------------------------------------

    /**
     * A parsed request of the HTTP part of the protocol.
     */
    private static class HttpRequest {
        private String method = "";
        private String path = "";
        private boolean forceClose = false;
        private final Map<String, String> headers = new LinkedHashMap<>();

        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        boolean isWebSocketUpgrade() {
            String upgrade = header("upgrade");
            return upgrade != null && upgrade.toLowerCase(Locale.ROOT).contains("websocket");
        }

        boolean closesConnection() {
            if (forceClose) return true;
            String connection = header("connection");
            return connection != null && connection.toLowerCase(Locale.ROOT).contains("close");
        }
    }

    private HttpRequest readRequest(BufferedInputStream in) throws IOException {
        String line = readLine(in);
        if (line == null || line.isEmpty()) return null;

        int firstSpace = line.indexOf(' ');
        if (firstSpace <= 0) return null;
        int secondSpace = line.indexOf(' ', firstSpace + 1);

        HttpRequest request = new HttpRequest();
        request.method = line.substring(0, firstSpace);
        String target = secondSpace < 0 ? line.substring(firstSpace + 1) : line.substring(firstSpace + 1, secondSpace);

        int query = target.indexOf('?');
        request.path = query < 0 ? target : target.substring(0, query);

        while (true) {
            String header = readLine(in);
            if (header == null || header.isEmpty()) break;

            int colon = header.indexOf(':');
            if (colon <= 0) continue;
            request.headers.put(header.substring(0, colon).trim().toLowerCase(Locale.ROOT), header.substring(colon + 1).trim());
        }

        return request;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String page() {
        if (page == null) {
            String template = readResource(PAGE_RESOURCE);
            if (template == null) {
                template = "<!doctype html><html><body><h1>Remote gamepad page is missing</h1></body></html>";
            }
            page = template.replace(CODES_PLACEHOLDER, Json.write(layout.codeMap()));
        }
        return page;
    }

    private String readResource(String path) {
        ClassLoader classLoader = RemoteInputServer.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) {
                log.error("Remote gamepad page '{}' is not found in the classpath", path);
                return null;
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read the remote gamepad page '{}'", path, e);
            return null;
        }
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder(128);
        int read;
        while ((read = in.read()) != -1) {
            if (read == '\n') {
                int length = builder.length();
                if (length > 0 && builder.charAt(length - 1) == '\r') builder.setLength(length - 1);
                return builder.toString();
            }
            builder.append((char) read);
            if (builder.length() > 8192) throw new IOException("The header line of the request is too long");
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static int readByte(InputStream in) throws IOException {
        int read = in.read();
        if (read < 0) throw new IOException("The connection was closed in the middle of a frame");
        return read;
    }

    private static void readFully(InputStream in, byte[] buffer) throws IOException {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read < 0) throw new IOException("The connection was closed in the middle of a frame");
            offset += read;
        }
    }

    private static String websocketAccept(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest((key + WEBSOCKET_GUID).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    private static String reason(int status) {
        switch (status) {
            case 200: return "OK";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            default: return "OK";
        }
    }

    private static void closeQuietly(Socket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException e) {
            // ignored
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            // ignored
        }
    }

    private static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command;
        if (os.contains("win")) {
            command = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (os.contains("mac")) {
            command = new String[]{"open", url};
        } else {
            command = new String[]{"xdg-open", url};
        }

        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            log.warn("Failed to open '{}' in the default browser", url, e);
        }
    }

    private static String property(String key, String fallback) {
        String override = System.getProperty("north." + key);
        if (override != null && !override.trim().isEmpty()) return override.trim();

        try {
            String value = ApplicationProperties.getProperty(key);
            if (value == null) return fallback;
            value = value.trim();
            return value.isEmpty() ? fallback : value;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static int intProperty(String key, int fallback) {
        try {
            return Integer.parseInt(property(key, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            log.warn("Invalid value of the property '{}', using {}", key, fallback);
            return fallback;
        }
    }

    private static float floatProperty(String key, float fallback) {
        try {
            return Float.parseFloat(property(key, Float.toString(fallback)));
        } catch (NumberFormatException e) {
            log.warn("Invalid value of the property '{}', using {}", key, fallback);
            return fallback;
        }
    }

    private static boolean booleanProperty(String key, boolean fallback) {
        return Boolean.parseBoolean(property(key, Boolean.toString(fallback)));
    }

    /**
     * @return the layout of the virtual gamepad
     */
    public GamepadLayout getLayout() {
        return layout;
    }

    /**
     * @return the states of the connected clients
     */
    public List<GamepadState> getClientStates() {
        return Collections.unmodifiableList(new ArrayList<>(clients.values()));
    }

    /**
     * @return the number of the connected clients
     */
    public int getClientCount() {
        return clients.size();
    }
}
