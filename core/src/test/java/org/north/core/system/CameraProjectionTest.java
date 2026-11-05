package org.north.core.system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.north.core.scene.component.Camera;

class CameraProjectionTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void rebuildsPerspectiveAndOrthographicMatricesForNewAspectRatio() {
        Camera camera = new Camera();
        camera.ratio = 2f;

        try {
            CameraSystem.updateProjectionMatrices(camera);

            Assertions.assertEquals(0.5f, CameraSystem.PERSPECTIVE_MATRIX.m00(), EPSILON);
            Assertions.assertEquals(1f, CameraSystem.PERSPECTIVE_MATRIX.m11(), EPSILON);

            Assertions.assertEquals(0.5f, CameraSystem.ORTHOGRAPHIC_MATRIX.m00(), EPSILON);
            Assertions.assertEquals(1f, CameraSystem.ORTHOGRAPHIC_MATRIX.m11(), EPSILON);
        } finally {
            camera.ratio = 1f;
            CameraSystem.updateProjectionMatrices(camera);
        }
    }
}
