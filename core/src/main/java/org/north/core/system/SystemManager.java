package org.north.core.system;

import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandlerScanner;
import org.north.core.architecture.SystemComponentPair;
import org.north.core.scene.component.management.ComponentContainer;
import org.north.core.scene.component.management.MapBasedComponentContainer;
import org.north.core.scene.Entity;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.north.core.scene.component.Component;
import org.north.core.scene.component.ComponentState;
import org.north.core.config.ApplicationProperties;
import org.north.core.physics.collision.Collision;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.Process;

import java.util.*;
import java.util.stream.Collectors;

public class SystemManager {
    private static final Logger log = LoggerFactory.getLogger(SystemManager.class);

    public final ApplicationContext applicationContext;
    public final ComponentHandlerScanner scanner;
    public final Map<Class<? extends Component>, Class<? extends System<?>>> componentToSystemAssociations;
    public final Map<Class<? extends Process>, List<Process>> processMap;
    public final Map<Class<? extends Component>, System<?>> systemMap;
    public final List<System<?>> systemList;

    private final ComponentContainer componentContainer;


    // ----- unrelated to a SystemManager things ------
    // --- collisions ---
    // todo: refactor, and also refactor Pipeline - extract all the collision handling logic into separate systems and add two new pipeline stages, something like preUpdate and postUpdate, which will be firing on each frame
    public final List<Collision> collisions;
    // -------------------


    public SystemManager(ApplicationContext context) {
        applicationContext            = context;
        scanner                       = new ComponentHandlerScanner();
        componentToSystemAssociations = new IdentityHashMap<>();
        collisions  = new ArrayList<>();
        systemList  = new ArrayList<>();
        systemMap   = new IdentityHashMap<>();
        processMap  = new IdentityHashMap<>();

        try {
            componentContainer = context.addDependency(ComponentContainer.class, new MapBasedComponentContainer());
        } catch (ReflectiveOperationException e) {
            log.error("Failed to create and register component container instance!");
            throw new RuntimeException(e);
        }

        initProcessMap(ApplicationProperties.getProperty("process.package"));
        loadComponentSystemsFromPackage(ApplicationProperties.getProperty("system.package"));
    }

    @SuppressWarnings("unchecked")
    public <P extends Process> List<P> getProcessList(Class<P> processType) {
        return (List<P>) processMap.get(processType);
    }

    private void loadComponentSystemsFromPackage(String packagePath) {
        try {
            // Logger.info(String.format("Searching for systems from package '%s'...", packagePath));
            List<SystemComponentPair<?, ?>> annotatedClassesInPackage = scanner.getAnnotatedClassesInPackage(packagePath);
            annotatedClassesInPackage.forEach(pair -> componentToSystemAssociations.put(pair.component, pair.system));

            List<String> classNames = annotatedClassesInPackage.stream()
                    .map(pair -> pair.system.getName())
                    .collect(Collectors.toList());

            // Logger.info(String.format("Found %s system(s) classes: %s", classNames.size(), classNames));
        } catch (Exception e) {
            // Logger.error("Error while loading systems. Reason: " + e.getMessage());
            throw new RuntimeException(String.format("Error while loading systems from package '%s'.", packagePath), e);
        }
    }

    private void initProcessMap(String processPackageName) {
        try {
            log.info("process package name: {}", processPackageName);
            List<Class<? extends Process>> processClasses = scanner.getAllProcessClasses(processPackageName);
            log.info("found {} process classes: {}", processClasses.size(), processClasses);
            for (Class<? extends Process> processClass : processClasses) {
                processMap.put(processClass, new ArrayList<>());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public System<?> getSystemByComponentType(Class<? extends Component> componentType) {
        return systemMap.get(componentType);
    }

    @SuppressWarnings("unchecked")
    public <C extends Component> C addComponent(C component) {
        Entity entity = component.getEntity();
        Class<? extends Component> componentClass = component.getClass();
        Class<? extends System<?>> systemClass = componentToSystemAssociations.get(componentClass);
        System<?> system = systemMap.get(componentClass);

        // initialize system if it is not
        if (system == null) {
            try {
                //                System<?> system = initializer.initSystem(componentToSystemAssociations.get(componentClass));
                if (systemClass != null) {
                    system = applicationContext.addDependency(systemClass);
                    systemMap.put(componentClass, system);
                    systemList.add(system);

                    // Attaching to system list
                    for (Class<? extends Process> processClass : processMap.keySet()) {
                        if (processClass.isAssignableFrom(systemClass)) {
                            processMap.get(processClass).add((Process) system);
                        }
                    }

                }
                // Logger.info(String.format("System %s initialized", system.getClass().getName()));
    
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        
        // change state if it has no init process
        if (systemClass != null && !InitProcess.class.isAssignableFrom(systemClass)) {
            component.setState(ComponentState.READY_TO_OPERATE_STATE);
        }

        // TODO: Return the collision handling feature in the future
        // 
        // if (component instanceof Transform) {
        //     spatialTree.insert((Transform) component);
        // }
        // 

        componentContainer.add(entity, component);
        return (C) component; //system.addComponent(component);
    }

    public void reset() {
        for (System<?> system : systemList) {
            system.reset();
        }
    }

    // todo: refactor, move to a specific system for collision registration/handling
    public void registerCollisions() {
    }


}
