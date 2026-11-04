package org.north.core.graphics;

public class PredefinedMeshes {
    public static final Mesh CUBE = new Mesh(
            new float[]{
                    -0.5f,  0.5f,  0.5f, // 1
                    -0.5f, -0.5f,  0.5f, // 2
                     0.5f, -0.5f,  0.5f, // 3
                     0.5f,  0.5f,  0.5f, // 4
                    -0.5f,  0.5f, -0.5f, // 5
                     0.5f,  0.5f, -0.5f, // 6
                    -0.5f, -0.5f, -0.5f, // 7
                     0.5f, -0.5f, -0.5f, // 8
            },
            new byte[]{
                    0, 1, 3,
                    3, 1, 2,
                    4, 0, 3,
                    5, 4, 3,
                    3, 2, 7,
                    5, 3, 7,
                    6, 1, 0,
                    6, 0, 4,
                    2, 1, 6,
                    2, 6, 7,
                    7, 6, 4,
                    7, 4, 5,
            },
            new float[]{
                    0.5f, 0.0f, 0.0f,
                    0.0f, 0.5f, 0.0f,
                    0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.5f,
                    0.5f, 0.0f, 0.0f,
                    0.0f, 0.5f, 0.0f,
                    0.0f, 0.0f, 0.5f,
                    0.0f, 0.5f, 0.5f,
            }
    );

    public static final Mesh QUAD = new Mesh(
            new float[]{
                    -0.5f,  0.5f, 0.0f,
                    -0.5f, -0.5f, 0.0f,
                     0.5f, -0.5f, 0.0f,
                     0.5f,  0.5f, 0.0f
            },
            new byte[]{ 0, 1, 2, 2, 3, 0 },
            new float[]{
                    0,  1,
                    0,  0,
                    1,  0,
                    1,  1
            }
    );

}
