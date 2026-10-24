# Game engine dev tasks to do

## High priority
- [ ] Rethink the Node (TreeNode) thing to be more like a collection interface rather than a smart node, the Node (TreeNode) itself must be a simple struct which will be used by a Tree collection
  - `Entity` no longer uses `LinkedTreeNode`; the tree class is now independent and still needs a redesign.
- [x] Don't use TreeNode on Entity, because the Entity must be flat as a data structure, all other fancy hierarchy things can be implemented later using components and EntityManager
  - Implemented with the flat `Entity` + `EntityManager`. Entity hierarchy is no longer stored in `Entity`; components such as `Transform.parentTransform` are responsible for hierarchy.
- [x] An Entity must be just a relatively simple class with id and EntityManager/ComponentManager reference, and also must have a bunch of helper methods to work with components of that entity, at least for a while...
  - Implemented: `Entity` has a final `int` id, a name, a `ComponentManager`, and `getComponent`/`hasComponent`/`addComponent`/`removeComponent`/`getComponentTypes` helpers.
  - [x] Remove hardcoded reference to Transform component, Transform is not necessarily must be present for all of the entities
    - Removed `Entity.getTransform`, `Component.getTransform`, and the `ComponentManager` restriction that prevented removing `Transform`.
  - [ ] Move entity name into separate component, make names of limited length, i.e.: FixedString8, FixedString16, and so on. Also, make FixedString classes implement CharSequence to better Java ecosystem integration
    - Entity names are still stored directly in `Entity`; this is the next ECS cleanup.
- [ ] An EntityManager (or a ComponentManager, who is the boss, idk) must have something for a component bulk instantiation process, and a pool of removed/unused components to be able to reuse them. Probably, that would be a north.depot task for improvement
  - `EntityManager` now owns entity ids, flat storage, and name lookup, but component pooling/bulk instantiation is not implemented yet.
  - [ ] A code generation thing for a bulk component instantiation, which will use a compile-time generated constructors
- [x] Transform components are broken, the hierarchy of entities does not affects the rendering (the actual problem was different, the component attachment logic with searching for related system was incorrect)
  - Transform hierarchy is now expressed with `Transform.parentTransform` instead of the Entity tree.
- [x] Change the type of an Entity id to a simple primitive, don't overengineer things if they aren't necessary
  - Id is now a final `int`; the id sequence lives in `EntityManager`.

## Medium/Low priority
- [ ] Assets
  - [x] An asset manager with convenient API
    - [x] Textures
    - [x] Shaders
    - [x] Models
      - Wavefront OBJ geometry and UV loading is implemented (`ObjMeshAssetLoader`). OBJ/MTL material import is not implemented yet; assign a `Texture` through a `Material` after loading the mesh.
      - `Mesh` now uses 32-bit indices and exposes cached local bounds for collider setup.
- [ ] XML scene file loader
  - [x] XML format of scene
    - A sample scene exists at `assets/scenes/test_scene.xml`, but no loader consumes it yet.
  - [ ] Serialization/deserialization for various class versions
- [ ] Scene object loader
  - The current scene is built directly in `DefaultSceneComposer`; there is no generic scene loader yet.
- [ ] Editor
  - [ ] Multifunctional editor cursor (OpenGL window)
    - [ ] Editor scene game object cursor selection
      - `VisualCursorSystem` already provides hover detection/magnetization for screen-space and world-space colliders, but there is no persistent click-selection state yet.
    - [ ] Editor cursor states
  - [ ] Separate editor window (Swing window, maybe... So, I think we can possibly make a simple CPU 2D renderer for editor window. There isn't gonna be fancy graphics or something, just isometric view without lighting)
    - [ ] Event bus
    - [ ] Console log
    - [ ] Inspector window (entities and components all in one)
    - [ ] Command line ?
- [x] Physics improvements
  - [x] Chop world scene into squares/partitions and store them in tree (QuadTree/Octree)
    - `Octree`, `SceneSpatialIndex`, and `Aabb` are implemented.
  - [x] Derive a `MeshCollider`'s local AABB from the entity's `MeshRenderer` mesh when no custom bounds are supplied.
- [ ] Events and event listeners using an Origin<T> objects
- [ ] Customizable pipeline builder
  - The pipeline is still assembled directly; the API is not builder-based yet.
- [ ] Graphics API
  - [x] Pass into `render()` system method an abstract `Graphics` object instead of `Window`
  - [ ] Implement OpenGL graphics engine API
    - [ ] Solve a multiple textures problem
      - `Material` now binds multiple named textures to consecutive texture slots and skips uniforms that are not present in the shader, but there is no multi-texture shader or regression test yet, so this needs verification.
  - [ ] Implement OpenGL ES graphics engine API (Android API)
  - [ ] **(optional)** Implement Swing/AWT CPU graphics engine API (I think we don't need this)
- [ ] Network
  - [ ] TCP/UDP sockets API for fast game data transferring
  - [ ] Serialization/deserialization of game data for network transferring
  - The remote gamepad uses an HTTP server for input only; it is not a general game-data transport.
- [ ] Sounds
  - [ ] Sounds and music controller API
  - [ ] Different kinds of hardware audio API support
- [ ] Cross-platform-ness
  - [ ] Create Maven (or Gradle) profiles (I think we must separate all platform-dependent things into maven/gradle modules)
    - [ ] Windows
    - [ ] Linux
    - [ ] Web (probably, using TeaVM)
    - [ ] Android
  - The Gradle build now selects LWJGL natives by OS/architecture and supports `-PlwjglNatives=<classifier>`, but platform modules/profiles are not split yet.
- [ ] Optimization
  - [x] A very memory expensive logger!!! Needs to rework or even replace with log4j2 + slf4j
    - Logging now uses SLF4J with Logback; no custom logger was found.
  - [ ] Improve system handling performance (expensive iterations on systems/components in map)
- [x] Logger
  - [x] A very memory expensive logger!!! See the checkbox above in the 'Optimization' section
  - [x] Reimplement logger based on standard Java logger interface
    - SLF4J facade with Logback is used instead of a custom logger.
  - [x] Print log info into `PrintWriter` (not `System.out`)
    - Logback appenders handle output; the main sources do not write directly to `System.out`.
  - [x] Use `printStackTrace` for printing exception stack traces
    - Exceptions are delegated to SLF4J/Logback instead of manual `printStackTrace`.

## Current focus / next implementation order
1. Move entity names into a `FixedString` component and make `FixedString` implement `CharSequence`.
2. Add component pooling and bulk component instantiation to remove the reflection-heavy hot path.
3. Add an XML scene loader that uses the existing scene format.
4. Add a multi-texture shader/test to verify the `Material` texture-slot path.
5. Continue with the editor window and event bus, then network transport.
