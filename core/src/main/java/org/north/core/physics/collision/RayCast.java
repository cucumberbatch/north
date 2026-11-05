package org.north.core.physics.collision;

import org.joml.Vector3f;
import org.north.core.scene.Entity;
import org.north.core.scene.component.Transform;
import org.north.core.scene.component.management.ComponentManager;

public class RayCast {
    private final Vector3f origin;
    private final Vector3f direction;
    private final ComponentManager cm;

    public RayCast(Vector3f origin, Vector3f direction, ComponentManager cm) {
        this.origin = origin;
        this.direction = direction;
        this.cm = cm;
    }

    public boolean isIntersects(Entity entity) {
        Transform gTransform = cm.get(entity, Transform.class).getGlobalTransform();
        Vector3f gPosition = gTransform.getPosition();
        float gSize = gTransform.getScale().length();

        Sphere sphere = new Sphere(gPosition.x, gPosition.y, gPosition.z, gSize);
        return this.isIntersects(sphere);
    }

    // todo: rewrite for object-free usage
    public boolean isIntersects(Sphere sphere) {
        Vector3f sphereCenter = sphere.getCenter();
        float a = direction.dot(direction);
        Vector3f oc = origin.sub(sphereCenter, new Vector3f());
        float b = 2f * oc.dot(direction);
        float c = oc.dot(oc) - sphere.r * sphere.r;
        float discriminant = b * b - 4 * a * c;
        return discriminant >= 0;
    }

}
