package io.github.cucumberbatch.north.physics;

import org.joml.Vector3f;

public class Physics {
    public static float gravityFactor = 9.81f;
    public static Vector3f gravityVector = new Vector3f(0, -1, 0).mul(gravityFactor);
}
