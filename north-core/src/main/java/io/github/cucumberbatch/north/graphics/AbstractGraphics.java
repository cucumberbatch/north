package io.github.cucumberbatch.north.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class AbstractGraphics implements Graphics {
    protected Matrix4f projection = new Matrix4f().identity();

    protected Matrix4f view = new Matrix4f().lookAt(
            new Vector3f(0, 0, -1),
            new Vector3f(0, 0, 1),
            new Vector3f(0, 1, 0)
    );

    /** Orthographic projection for UI elements whose coordinates are window pixels with a top-left origin. */
    protected final Matrix4f screenProjection = new Matrix4f().identity();

    /** Identity view matrix used together with {@link #screenProjection}. */
    protected final Matrix4f screenView = new Matrix4f().identity();

    @Override
    public Matrix4f view() {
        return view;
    }

    @Override
    public Matrix4f projection() {
        return projection;
    }

    @Override
    public Matrix4f screenView() {
        return screenView;
    }

    @Override
    public Matrix4f screenProjection() {
        return screenProjection;
    }
}
