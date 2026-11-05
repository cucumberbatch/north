package org.north.core.physics.collision;

import org.joml.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class RayCastTest {

    @Test
    void isIntersectsWithSphere() {
        Vector3f rayOrigin    = new Vector3f(0, 0, 0);
        Vector3f rayDirection = new Vector3f(1, 1, 1);
        Vector3f sphereCenter = new Vector3f(2, 2, 2);
        float sphereRadius    = 1f;

        Sphere sphere   = new Sphere(sphereCenter, sphereRadius);
        RayCast rayCast = new RayCast(rayOrigin, rayDirection, null);

        Assertions.assertTrue(rayCast.isIntersects(sphere));

    }
}