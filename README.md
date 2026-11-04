# A primitive work in progress Java ECS game engine on OpenGL
Primitive java game engine with using LWJGL (Light Weight Java Graphic Library) on ECS (Entity-Component-System) pattern.

## Meshes and textures

Wavefront OBJ files are loaded through the asset service. The OBJ loader supports triangles,
polygons, negative indices, and vertex UV coordinates:

```java
Mesh mesh = assetService.get(Mesh.class, "assets/models/character.obj");
Texture texture = assetService.get(Texture.class, "assets/models/character.png");
Shader shader = assetService.get(Shader.class, "assets/shaders/texture_shader");

MeshRenderer renderer = componentManager.add(entity, MeshRenderer.class);
renderer.mesh = mesh;
renderer.material = new Material(shader)
        .setTexture("u_texture", texture)
        .setVector("u_color", new Vector4f(1f, 1f, 1f, 1f));
```

For a world-space collider, add a `MeshCollider` and leave `localBounds` empty. The collider system
derives the local AABB from the renderer mesh and transforms it into world space. Set
`localBounds` explicitly (or call `refreshLocalBounds()` after changing the mesh) when the collider
needs different bounds.

```java
MeshCollider collider = componentManager.add(entity, MeshCollider.class);
collider.space = MeshCollider.Space.WORLD;
```

## Audio

WAV clips are loaded through the asset service and uploaded to OpenAL. If no audio device can be
opened, the audio backend disables itself and the rest of the engine keeps running.

```java
AudioClip clip = assetService.get(AudioClip.class, "assets/audio/footstep.wav");
AudioSource source = audio.createSource();
source.setClip(clip);
source.setGain(0.8f);
source.setPosition(new Vector3f(0f, 0f, 0f));
source.play();
// ...
source.close();
```

The `Audio` instance is registered in the application context and is closed by the engine after
assets are unloaded.

The built-in `application.scene=footsteps` scene is a third-person test scene. Walk with
W/A/S/D or the arrow keys; the player emits one-shot footsteps from the loaded WAV while moving.
Use another value for `application.scene` (or remove the property) to use the original cubes scene.

## Remote gamepad

The engine starts a small web server which serves a touch friendly gamepad. Open the printed URL
in the browser of a phone of the same local network and the phone turns into a gamepad of the
running game.

Every button and every stick of the web interface is delivered into the `Input` of the engine, so
the game world reacts to the phone in realtime, exactly like it reacts to a physical keyboard.

```
http://<local-network-address>:8080/
```

### Configuration

| property                                    | default   | description                                       |
|---------------------------------------------|-----------|---------------------------------------------------|
| `application.remote-input.enabled`          | `true`    | enables the remote gamepad server                 |
| `application.remote-input.port`             | `8080`    | port of the server                                |
| `application.remote-input.bind`             | `0.0.0.0` | bind address                                      |
| `application.remote-input.auto-open-browser`| `false`   | opens the page in the browser of the host machine |
| `application.remote-input.cursor-speed`     | `1.0`     | sensitivity of the virtual cursor                 |

The values can be overridden with a system property, e.g.
`-Dnorth.application.remote-input.port=9090`.

### Bindings

The controls of the gamepad are bound to GLFW keys, so the game logic can use the ordinary
`Input` API. The default bindings are:

| control                | key                | control   | key               |
|------------------------|--------------------|-----------|-------------------|
| A / B / X / Y          | SPACE / ESCAPE / I / Y | L1 / R1 | Q / E             |
| direction pad          | arrow keys         | L2 / R2   | LEFT/RIGHT CONTROL|
| left stick             | A / D / W / S      | START     | ENTER             |
| right stick            | arrow keys         | SELECT    | G                 |
| PAUSE                  | P                  | MOUSE     | left mouse button |

The analog values of the sticks are available as well:

```java
float x = input.getAxis("leftX");
float y = input.getAxis("leftY");
```

The bindings can be changed with a `remote-gamepad.properties` file in the working directory of
the engine, see `core/src/main/resources/remote/gamepad.properties` for an example.

### Protocol

The page is served as plain HTTP by the same port, the events travel over a WebSocket connection
which the page opens on `/ws`. The events are binary, because a fast reaction of the game world
matters more than a readable payload:

```
byte 0      number of the events in the message (1..255)
byte 1..n   the events, one after another
```

An event is a one byte type followed by its payload:

| event   | payload                                              | size |
|---------|------------------------------------------------------|------|
| `BUTTON`| control code, state (1 = pressed, 0 = released)      | 3 B  |
| `AXIS`  | control code, i16 value, scaled by 32767             | 4 B  |
| `CURSOR`| i16 horizontal movement, i16 vertical movement        | 5 B  |

The control codes are handed over to the page once, when it is loaded. A button press therefore
costs three bytes of payload and ten bytes on the wire, instead of a HTTP request of about two
hundred bytes per event. The events of one animation frame are packed into a single message, a
button is sent right away, so the latency stays at one network round trip.

The server pings the client every two seconds and closes a connection which does not answer
within ten seconds. The page reconnects on its own and repeats the state of the controls which
are still active. When a client goes away, all of its keys are released.

