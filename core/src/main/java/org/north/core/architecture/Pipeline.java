package org.north.core.architecture;

import org.lwjgl.glfw.GLFW;
import org.north.core.asset.AssetService;
import org.north.core.asset.SimpleAssetService;
import org.north.core.audio.Audio;
import org.north.core.config.ApplicationProperties;
import org.north.core.exception.ComponentNotFoundException;
import org.north.core.exception.ShaderUniformNotFoundException;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Window;
import org.north.core.system.SystemManager;
import org.north.core.physics.spatial.Aabb;
import org.north.core.physics.collision.Collision;
import org.north.core.physics.collision.CollisionState;
import org.north.core.scene.DefaultSceneComposer;
import org.north.core.scene.Entity;
import org.north.core.scene.EntityManager;
import org.north.core.scene.SceneComposer;
import org.north.core.scene.command.CommandApplier;
import org.north.core.scene.component.Component;
import org.north.core.scene.component.ComponentState;
import org.north.core.scene.component.Transform;
import org.north.core.scene.component.management.ComponentContainer;
import org.north.core.scene.component.management.ComponentManager;
import org.north.core.system.CameraAwareService;
import org.north.core.system.CameraControlsSystem;
import org.north.core.system.Input;
import org.north.core.system.System;
import org.north.core.system.process.*;
import org.north.core.utils.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.opengl.GL11.*;
import static org.north.core.architecture.Pipeline.UpdateFlowState.*;

public class Pipeline implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

    private final Window window;
    private final Graphics graphics;
    private final ComponentManager componentManager;
    private final SystemManager systemManager;
    private final CommandApplier commandApplier;
    private final FrameTiming timingContext;
    private final Input input;

    private final EntityManager entityManager;
    private final Entity rootEntity;

    private final boolean editorEnabled;
    private final boolean smoothStopAndStartUpdateProcess;
    private final CameraAwareService cameraAwareService;

    private final AssetService assetService;

    enum UpdateFlowState { RUNNING, STOPPED, RUN_TO_STOP, STOP_TO_RUN }

    private UpdateFlowState updateFlowState = RUNNING;
    private final boolean load = false;
    private boolean stopped = false;

    private final ComponentContainer componentContainer;

    public Pipeline(ApplicationContext context) {
        this.window = context.getDependency(Window.class);
        this.graphics = context.getDependency(Graphics.class);
        this.componentManager = context.getDependency(ComponentManager.class);
        this.systemManager = context.getDependency(SystemManager.class);
        this.commandApplier = context.getDependency(CommandApplier.class);
        this.timingContext = new FrameTiming(65);
        this.input = context.getDependency(Input.class);

//        this.componentContainer = new MapBasedComponentContainer();

        this.editorEnabled =
                ApplicationProperties.getBoolean("application.editor.enabled");

        this.smoothStopAndStartUpdateProcess =
                ApplicationProperties.getBoolean("application.editor.process.update.smooth-stop-and-start");

        try {
//            context.addDependency(ComponentContainer.class, componentContainer);
            this.assetService = context.addDependency(
                    AssetService.class,
                    new SimpleAssetService(graphics, context.getDependency(Audio.class))
            );
            this.componentContainer = context.getDependency(ComponentContainer.class);
            cameraAwareService = context.getDependency(CameraAwareService.class);
            this.entityManager = context.addDependency(
                    EntityManager.class, new EntityManager(componentManager));
            this.rootEntity = entityManager.getRoot();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void run() {
        // Logger.info("Game loop started");

        composeScene(new DefaultSceneComposer(assetService));
        //applyDeferredCommands();

        while (window.shouldNotClose() && !stopped) {
            try {
                tick();
            } catch (RuntimeException e) {
                log.error("Exiting game loop in case of thrown exception", e);
                break;
            }
        }
    }

    final int maxCountedFrames = 120;
    int fpsAverageCount = 0;
    int countedFrames = 0;

    public synchronized void tick() {
        timingContext.updateTiming();
        final float elapsedTime = timingContext.getElapsedTime();

        init();

        handleInput();
        updateInput();

        update(elapsedTime);

        registerCollisions();
        handleCollisions();

        applyDeferredCommands();

        render(window);

        timingContext.sync();
        countedFrames++;
        fpsAverageCount += timingContext.getActualFrameRate();

//        if (countedFrames > maxCountedFrames) {
//            log.info("Average FPS: {}", fpsAverageCount / maxCountedFrames);
//            fpsAverageCount = 0;
//            countedFrames = 0;
//        }
    }

    public void stop() {
        stopped = true;
    }

    public void close() {
        try {
            assetService.close();
        } catch (Exception e) {
            log.error("Failed to close assets", e);
        }
    }

    private void composeScene(SceneComposer composer) {
        componentManager.add(rootEntity, Transform.class);
        applyDeferredCommands();
        composer.compose(entityManager, componentManager);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void init() throws RuntimeException {
        // todo: needs to check for adding new components in runtime and checks for running out of components
        //  to initialize, so we can skip iteration moment in init() method call until the next component addition
        //
        //    if (systemManager.hasNoComponentsToInit()) return;
        //

        boolean hasComponentsToInit = false;
        Stopwatch.start();
        for (InitProcess process : systemManager.getProcessList(InitProcess.class)) {
            org.north.core.system.System<? extends Component> system   = (org.north.core.system.System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                if (component.inState(ComponentState.READY_TO_INIT_STATE)) {
                    hasComponentsToInit = true;
//                    log.info("Handling init component [{}: {}]",
//                            system.getClass().getName(), component.getEntity().getName());
                    try {
                        process.init(component);
                        component.setState(ComponentState.READY_TO_OPERATE_STATE);
                    } catch (ComponentNotFoundException e) {
                        // Logger.error(e);
                        log.warn("Component not found", e);
                        component.setState(ComponentState.LATE_INIT_STATE);
                    } catch (NullPointerException e) {
                        // Logger.error(e);
                        log.warn("Null pointer exception while initializing component", e);
                        component.setState(ComponentState.LATE_INIT_STATE);
                    }
                } else if (component.inState(ComponentState.LATE_INIT_STATE)) {
                    component.setState(ComponentState.READY_TO_INIT_STATE);
                }
            }
        }

        if (hasComponentsToInit) Stopwatch.stop("Handled init process");
        else                     Stopwatch.reset();
    }

    public void updateInput() {
        input.updateInput();

        if (input.isHeld(GLFW.GLFW_KEY_P)) {
            if (updateFlowState.equals(RUNNING)) {
                if (smoothStopAndStartUpdateProcess) {
                    updateFlowState = RUN_TO_STOP;
                } else {
                    updateFlowState = STOPPED;
                    runFactor = 0;
                }
            } else if (updateFlowState.equals(STOPPED)) {
                if (smoothStopAndStartUpdateProcess) {
                    updateFlowState = STOP_TO_RUN;
                } else {
                    updateFlowState = RUNNING;
                    runFactor = 1;
                }
            } else if (updateFlowState.equals(RUN_TO_STOP)) {
                updateFlowState = STOP_TO_RUN;
            } else {
                updateFlowState = RUN_TO_STOP;
            }
            input.clearKeyHold(GLFW.GLFW_KEY_P);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void handleInput() {
        for (InputHandlerProcess process : systemManager.getProcessList(InputHandlerProcess.class)) {
            org.north.core.system.System<? extends Component> system = (org.north.core.system.System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                if (component.inState(ComponentState.READY_TO_OPERATE_STATE)) {
                    try {
                        process.handleInput(component, input);
                    } catch (Exception e) {
                        log.warn("failed handling input events", e);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void registerCollisions() {
        systemManager.registerCollisions();

        /*
        if (systemManager.getProcessList(CollisionHandlingProcess.class).isEmpty()) return;

//        Stopwatch.start();

        List<Collision> collisions = systemManager.collisions;

        int collisionsListSize = collisions.size();

        Collision collision;
        for (CollisionHandlingProcess<? extends Component> process : systemManager.getProcessList(CollisionHandlingProcess.class)) {
            if (!MeshColliderSystem.class.isAssignableFrom(process.getClass())) continue;
            List<MeshCollider> componentList = ((MeshColliderSystem) process).getComponentList();
            int componentsCount = componentList.size();
            for (int lindex = 0; lindex < componentsCount - 1; lindex++) {
                MeshCollider that = componentList.get(lindex);
                for (int rindex = lindex + 1; rindex < componentsCount; rindex++) {
                    MeshCollider other = componentList.get(rindex);
                    Entity A = that.entity;
                    Entity B = other.entity;
                    if (that.isStatic && other.isStatic) continue;
                    if (that.body == null || other.body == null) continue;
                    Collision previousFrameCollision;
                    if (that.body.isIntersects(other.body)) {
                        Vector3f positionA = A.getTransform().getPosition();
                        Vector3f positionB = B.getTransform().getPosition();
                        boolean collisionFound = false;
                        for (int cindex = 0; cindex < collisionsListSize; cindex++) {
                            previousFrameCollision = collisions.get(cindex);
                            if (isSameCollisionAsInPreviousFrame(previousFrameCollision, A, B)) {
                                collisionFound = true;
                                if (previousFrameCollision.inState(CollisionState.ENTERED)) {
                                    //todo: add collision pool
                                    previousFrameCollision.setPair(new CollisionPair(positionA, positionB));
                                    previousFrameCollision.setState(CollisionState.CONTINUED);
                                    // Logger.info(String.format(
//                                            "Collision modified! a1: %s\tstate: %s",
//                                            previousFrameCollision,
//                                            previousFrameCollision.state
//                                    ));
                                }
                                break;
                            }
                        }
                        if (!collisionFound) {
                            //todo: add collision pool
                            CollisionPair pair = new CollisionPair(positionA, positionB);
                            collision = new Collision(A, B, pair);
                            collision.setModified(true);
                            collisions.add(collision);
                            collisionsListSize++;
                            // Logger.info(String.format(
//                                    "Collision added! a1: %s\tstate: %s",
//                                    collision,
//                                    collision.state
//                            ));
                        }
                    } else {
                        for (int cindex = 0; cindex < collisionsListSize; cindex++) {
                            previousFrameCollision = collisions.get(cindex);
                            if (isSameCollisionAsInPreviousFrame(previousFrameCollision, A, B)) {
                                if (!previousFrameCollision.inState(CollisionState.EXITED)) {
                                    previousFrameCollision.setState(CollisionState.EXITED);
                                    // Logger.info(String.format(
//                                            "Collision modified! a1: %s\tstate: %s",
//                                            previousFrameCollision,
//                                            previousFrameCollision.state
//                                    ));
                                } else {
                                    previousFrameCollision.setModified(false);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        for (int cindex = collisionsListSize - 1; cindex >= 0; cindex--) {
            collision = collisions.get(cindex);
            if (collision.inState(CollisionState.EXITED) && !collision.isModified()) {
                collisions.remove(cindex);
                collisionsListSize--;
                // Logger.info(String.format(
//                        "Collision removed! a1: %s\tstate: %s",
//                        collision,
//                        collision.state
//                ));
            }
        }

//        Stopwatch.stop("Collision register systems handling ended!");
        */

    }

    private boolean isSameCollisionAsInPreviousFrame(Collision previousFrameCollision, Aabb A, Aabb B) {
        return (previousFrameCollision.getA() == A && previousFrameCollision.getB() == B) ||
               (previousFrameCollision.getA() == B && previousFrameCollision.getB() == A);
    }

    float runFactor = 1;

    //todo: performance
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void update(final float deltaTime) {
//        Stopwatch.start();
        glfwPollEvents();

        float modifiedDeltaTime = deltaTime;

        if (updateFlowState.equals(RUN_TO_STOP)) {
            runFactor = runFactor - runFactor * deltaTime * 5;
            modifiedDeltaTime = deltaTime * runFactor;
            if (runFactor < 0.01f) {
                runFactor = 0;
                updateFlowState = STOPPED;
            }
        } else if (updateFlowState.equals(STOP_TO_RUN)) {
            runFactor = runFactor + (1 - runFactor) * deltaTime * 5;
            modifiedDeltaTime = deltaTime * runFactor;
            if (runFactor > 0.98f) {
                runFactor = 1;
                updateFlowState = RUNNING;
            }
        }

        for (UpdateProcess process : systemManager.getProcessList(UpdateProcess.class)) {
            boolean isCameraControlsSystem = CameraControlsSystem.class.isAssignableFrom(process.getClass());
            if (updateFlowState.equals(STOPPED) && !isCameraControlsSystem) continue;
            org.north.core.system.System<? extends Component> system = (org.north.core.system.System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                if (component.isActive() && component.inState(ComponentState.READY_TO_OPERATE_STATE)) {
                    // Logger.trace(String.format(
//                            "Handling update component [%s: %s]",
//                            system.getClass().getName(),
//                            component.getEntity().getName())
//                    );
                    try {
                        if (isCameraControlsSystem) {
                            process.update(component, deltaTime);
                        } else {
                            process.update(component, modifiedDeltaTime);
                        }
                    } catch (ComponentNotFoundException | NullPointerException e) {
                        // Logger.error(e);
                        throw new RuntimeException("Something went wrong in frame updating", e);
                        //component.setState(ComponentState.READY_TO_INIT_STATE);
                    }
                }
            }
        }

//        Stopwatch.stop("Update systems handling ended!");
    }

    //todo: performance
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleCollisionEnter() {
//        Stopwatch.start();

        for (CollisionHandlerProcess process : systemManager.getProcessList(CollisionHandlerProcess.class)) {
            org.north.core.system.System<? extends Component> system = (org.north.core.system.System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                for (Collision collision : systemManager.collisions) {
                    // Logger.trace(String.format("Visiting enter collision [%s] for component [%s]", collision, component));
                    // Collision currently stores bounds. The entity comparison is kept as an
                    // identity check for the legacy collision-handler path.
                    if ((Object) collision.getA() != component.getEntity() && (Object) collision.getB() != component.getEntity()) continue;
                    if (collision.inState(CollisionState.ENTERED)) {
                        if ((Object) component.getEntity() == collision.getA()) collision.swapAB();
                        process.onCollisionStarted(component, collision);
                    }
                }
            }
        }

//        Stopwatch.stop("Collision enter handling ended!");
    }

    //todo: performance
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleCollisionHold() {
//        Stopwatch.start();

        for (CollisionHandlerProcess process : systemManager.getProcessList(CollisionHandlerProcess.class)) {
            org.north.core.system.System<? extends Component> system = (org.north.core.system.System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                for (Collision collision : systemManager.collisions) {
                    // Logger.trace(String.format("Visiting hold collision [%s] for component [%s]", collision, component));
                    if ((Object) collision.getA() != component.getEntity() && (Object) collision.getB() != component.getEntity()) continue;
                    if (collision.inState(CollisionState.CONTINUED)) {
                        if ((Object) component.getEntity() == collision.getA()) collision.swapAB();
                        process.onCollisionContinued(component, collision);
                    }
                }
            }
        }

//        Stopwatch.stop("Collision hold handling ended!");
    }

    //todo: performance
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleCollisionExit() {
//        Stopwatch.start();

        for (CollisionHandlerProcess process : systemManager.getProcessList(CollisionHandlerProcess.class)) {
            org.north.core.system.System<? extends Component> system = (System<? extends Component>) process;
            Iterator<? extends Component> iterator = system.getComponentIterator();
            while (iterator.hasNext()) {
                Component component = iterator.next();
                for (Collision collision : systemManager.collisions) {
                    // Logger.trace(String.format("Visiting exit collision [%s] for component [%s]", collision, component));
                    if ((Object) collision.getA() != component.getEntity() && (Object) collision.getB() != component.getEntity()) continue;
                    if (collision.inState(CollisionState.EXITED)) {
                        if ((Object) component.getEntity() == collision.getA()) collision.swapAB();
                        process.onCollisionEnded(component, collision);
                    }
                }
            }
        }

//        Stopwatch.stop("Collision exit handling ended!");
    }

    public void handleCollisions() {
        handleCollisionEnter();
        handleCollisionHold();
        handleCollisionExit();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void render(Window window) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//        Stopwatch.start();

        //fixme: no way it's a good approach to render things on screen because of different systems on which we iterate,
        // which means that components of different render groups would be sorted only in certain group,
        // not in the whole world context, so it can cause a lot of graphical bugs for more than one render system
        for (RenderProcess process : systemManager.getProcessList(RenderProcess.class)) {
            List<? extends Component> components = cameraAwareService.sortComponentsByDistanceToCamera(process);
            for (Component component : components) {
                if (component.isActive() && component.inState(ComponentState.READY_TO_OPERATE_STATE)) {
                    // Logger.trace(String.format(
//                            "Handling render component [%s: %s]",
//                            system.getClass().getName(),
//                            component.getEntity().getName())
//                    );
                    try {
                        process.render(component, graphics);
                    } catch (ComponentNotFoundException | NullPointerException e) {
                        // Logger.error(e);
                        throw new RuntimeException("Something went wrong in rendering", e);
                        //component.setState(ComponentState.READY_TO_INIT_STATE);
                    } catch (ShaderUniformNotFoundException e) {
                        // Logger.error(String.format("Error while trying to find shader uniform with name '%s' in shader '%s'",
//                                e.getUniformName(), e.getShaderName()));
                        throw e;
                    }
                }
            }
        }

//        Stopwatch.stop("Graphics systems handling ended!");

//        Stopwatch.start();
        glfwSwapBuffers(window.getWindow());
//        Stopwatch.stop("Graphics buffer swap ended!");
    }


    private void applyDeferredCommands() {
        commandApplier.applyDeferredCommands();
    }

}
