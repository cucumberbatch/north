package io.github.cucumberbatch.north.system;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import io.github.cucumberbatch.north.memory.Vector3fPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cucumberbatch.north.architecture.ApplicationContext;
import io.github.cucumberbatch.north.architecture.ComponentHandlerScanner;
import io.github.cucumberbatch.north.architecture.Initializer;
import io.github.cucumberbatch.north.architecture.Inject;
import io.github.cucumberbatch.north.architecture.SystemComponentPair;
import io.github.cucumberbatch.north.config.ApplicationProperties;
import io.github.cucumberbatch.north.scene.Entity;
import io.github.cucumberbatch.north.scene.component.Component;
import io.github.cucumberbatch.north.scene.component.ComponentState;
import io.github.cucumberbatch.north.scene.component.management.ComponentContainer;
import io.github.cucumberbatch.north.system.process.InitProcess;
import io.github.cucumberbatch.north.system.process.Process;

public class SystemManager {
    private static final Logger log = LoggerFactory.getLogger(SystemManager.class);

    private final ApplicationContext      context;
    private final Initializer             systemInitializer;
    private final ComponentHandlerScanner scanner;
    
    private final Map<Class<? extends Component>, Class<? extends System<?>>> componentToSystemMappings = new IdentityHashMap<>();
    private final Map<Class<? extends Process>, List<Process>>                processMap                = new IdentityHashMap<>();
    private final Map<Class<? extends Component>, System<?>>                  componentTypeToSystems    = new IdentityHashMap<>();

    private final List<System<?>> systems = new ArrayList<>();

    private final ComponentContainer componentContainer;

    @Inject
    public SystemManager(ApplicationContext context) {
        this.context            = context;
        this.systemInitializer  = context.getDependency(Initializer.class);
        this.componentContainer = context.getDependency(ComponentContainer.class);
        this.scanner            = new ComponentHandlerScanner();

        context.addDependency(Vector3fPool.class, new Vector3fPool(32));

        initProcessMap(ApplicationProperties.getProperty("process.package"));
        loadComponentSystemsFromPackage(ApplicationProperties.getProperty("system.package"));
    }

    @SuppressWarnings("unchecked")
    public <P extends Process> List<P> getProcessList(Class<P> processType) {
        return (List<P>) processMap.get(processType);
    }

    private void initProcessMap(String packageName) {
        try {
            log.info("Process package name: {}", packageName);
            List<Class<? extends Process>> processClasses = scanner.getAllProcessClasses(packageName);
            log.info("Found {} process classes: {}", processClasses.size(), processClasses);
            for (Class<? extends Process> processClass : processClasses) {
                processMap.put(processClass, new ArrayList<>());
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("Systems initialization failed '%s'", packageName), e);
        }
    }

    private void loadComponentSystemsFromPackage(String packagePath) {
        try {
            for (SystemComponentPair<?, ?> pair : scanner.getAnnotatedClassesInPackage(packagePath)) {
                componentToSystemMappings.put(pair.component, pair.system);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error while loading systems from package '%s'.", packagePath), e);
        }
    }

    public System<?> getSystemByComponentType(Class<? extends Component> componentType) {
        return componentTypeToSystems.get(componentType);
    }

    public <C extends Component> C addComponent(C component) {
        Entity entity = component.getEntity();
        Class<? extends Component> componentClass = component.getClass();
        Class<? extends System<?>> systemClass = componentToSystemMappings.get(componentClass);
        System<?> system = componentTypeToSystems.get(componentClass);

        // initialize system if it is not
        if (system == null && systemClass != null) {
            system = systemInitializer.init(systemClass, context);
            componentTypeToSystems.put(componentClass, system);
            systems.add(system);

            // Attaching to system list
            for (Class<? extends Process> processClass : processMap.keySet()) {
                if (processClass.isAssignableFrom(systemClass)) {
                    processMap.get(processClass).add((Process) system);
                }
            }
        }
        
        // change state if it has no init process
        if (systemClass != null && !InitProcess.class.isAssignableFrom(systemClass)) {
            component.setState(ComponentState.READY_TO_OPERATE_STATE);
        }

        componentContainer.add(entity, component);
        return (C) component;
   }

    public void reset() {
        systems.forEach(System::reset);
    }

}
