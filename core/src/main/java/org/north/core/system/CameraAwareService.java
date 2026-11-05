package org.north.core.system;

import org.joml.Vector3f;
import org.north.core.architecture.Inject;
import org.north.core.scene.component.Camera;
import org.north.core.scene.component.Component;
import org.north.core.scene.component.Transform;
import org.north.core.scene.component.management.ComponentContainer;
import org.north.core.system.process.RenderProcess;

import java.util.*;

public class CameraAwareService {
    private final ComponentContainer componentContainer;
    private final CameraDistanceComparator cameraDistanceComparator;
    private Camera camera;

    @Inject
    public CameraAwareService(ComponentContainer componentContainer) {
        this.componentContainer = componentContainer;
        cameraDistanceComparator = new CameraDistanceComparator(componentContainer);
    }

    public void setCameraComponent(Camera camera) {
        this.camera = camera;
    }

    public List<? extends Component> sortComponentsByDistanceToCamera(RenderProcess<?> process) {
        if (camera == null) return Collections.emptyList();
        if (!cameraDistanceComparator.isCameraSet()) {
            Transform transform = componentContainer.get(camera.getEntity(), Transform.class);
            cameraDistanceComparator.setCameraPosition(transform.getPosition());
        }

        // Do not cache the list: components can be added or removed at runtime (for example the
        // gas clouds emitted by the spaceship), and a cached snapshot would never render them.
        System<? extends Component> system = (System<? extends Component>) process;
        List<? extends Component> components = system.getComponentList();
        components.sort(cameraDistanceComparator);
        return components;
    }

    private static class CameraDistanceComparator implements Comparator<Component> {
        private Vector3f cameraPosition;
        private final ComponentContainer componentContainer;

        private CameraDistanceComparator(ComponentContainer componentContainer) {
            this.componentContainer = componentContainer;
        }

        //note: we need to be careful because of passing a reference to camera position only once at the start of Camera component life,
        // so if we accidentally replace Transform.position vec instance we loose all position changes and scene will be rendered in wrong order
        private void setCameraPosition(Vector3f cameraPosition) {
            this.cameraPosition = cameraPosition;
        }

        private boolean isCameraSet() {
            return this.cameraPosition != null;
        }

        private final Vector3f o1Position = new Vector3f();
        private final Vector3f o2Position = new Vector3f();

        @Override
        public int compare(Component o1, Component o2) {
            int layerComparison = Integer.compare(o1.getRenderLayer(), o2.getRenderLayer());
            if (layerComparison != 0) return layerComparison;

            Transform t1 = componentContainer.get(o1.getEntity(), Transform.class);
            Transform t2 = componentContainer.get(o2.getEntity(), Transform.class);
            float o1Distance = t1.getGlobalPosition(o1Position).distance(cameraPosition);
            float o2Distance = t2.getGlobalPosition(o2Position).distance(cameraPosition);
            return Float.compare(o2Distance, o1Distance);
        }
    }

}
