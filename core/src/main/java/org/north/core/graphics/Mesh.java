package org.north.core.graphics;

import org.north.core.asset.Asset;
import org.north.core.physics.spatial.Aabb;

import java.util.concurrent.atomic.AtomicInteger;

public class Mesh implements Asset {
    private static final int VERTEX_ATTRIBUTE = 0;
    private static final int TEXTURE_COORDINATE_ATTRIBUTE = 1;
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(1);

    private final int id = ID_SEQUENCE.getAndIncrement();

    private VertexArrayHandle vao;
    private BufferHandle vertexBuffer;
    private BufferHandle indexBuffer;
    private BufferHandle textureCoordinatesBuffer;

    public final float[] vertices;
    public final int[]   indices;
    public final float[] uv;

    private Aabb bounds;

    public Mesh() {
        this(new float[]{
                -1.0f, -1.0f, +0.0f,
                -1.0f, +1.0f, +0.0f,
                +1.0f, +1.0f, +0.0f,
                +1.0f, -1.0f, +0.0f
        }, new int[]{
                0, 1, 2, 2, 3, 0,
        }, new float[]{
                0,  1,
                0,  0,
                1,  0,
                1,  1
        });
    }

    public Mesh(float[] vertices, byte[] indices, float[] uv) {
        this(vertices, toUnsignedIntArray(indices), uv);
    }

    public Mesh(float[] vertices, int[] indices, float[] uv) {
        if (vertices == null || vertices.length % 3 != 0) {
            throw new IllegalArgumentException("Vertex data must contain complete xyz triples");
        }
        if (uv == null || uv.length % 2 != 0) {
            throw new IllegalArgumentException("Texture coordinate data must contain complete uv pairs");
        }
        if (indices == null || indices.length % 3 != 0) {
            throw new IllegalArgumentException("Mesh indices must describe complete triangles");
        }
        this.vertices = vertices;
        this.indices  = indices;
        this.uv       = uv;
    }

    private static int[] toUnsignedIntArray(byte[] indices) {
        if (indices == null) {
            return null;
        }
        int[] converted = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            converted[i] = indices[i] & 0xFF;
        }
        return converted;
    }

    @Override
    public int id() {
        return id;
    }

    public void upload(Graphics graphics) {
        if (vao != null) return;

        VertexArrayHandle newVao = graphics.createVertexArray();
        graphics.bindVertexArray(newVao);

        BufferHandle newVertexBuffer = graphics.createArrayBuffer(vertices, VERTEX_ATTRIBUTE, 3);
        BufferHandle newTextureCoordinatesBuffer = graphics.createArrayBuffer(
                uv,
                TEXTURE_COORDINATE_ATTRIBUTE,
                2
        );
        BufferHandle newIndexBuffer = graphics.createElementBuffer(indices);

        graphics.bindVertexArray(null);

        this.vao = newVao;
        this.vertexBuffer = newVertexBuffer;
        this.textureCoordinatesBuffer = newTextureCoordinatesBuffer;
        this.indexBuffer = newIndexBuffer;
    }

    public boolean isUploaded() {
        return vao != null;
    }

    public VertexArrayHandle vao() {
        if (vao == null) {
            throw new IllegalStateException("Mesh is not uploaded to the GPU yet");
        }
        return vao;
    }

    public int indexCount() {
        return indices.length;
    }

    /**
     * Returns the local-space axis-aligned bounds of this mesh.
     *
     * <p>The bounds are calculated from {@link #vertices} once and cached. Call
     * {@link #invalidateBounds()} after modifying the vertex array.</p>
     */
    public Aabb getBounds() {
        if (bounds == null) {
            bounds = calculateBounds();
        }
        return bounds;
    }

    /**
     * Copies the local-space bounds into the supplied destination.
     */
    public Aabb getBounds(Aabb destination) {
        return destination.set(getBounds());
    }

    public void invalidateBounds() {
        bounds = null;
    }

    private Aabb calculateBounds() {
        Aabb result = new Aabb();
        if (vertices.length < 3) {
            return result;
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        return result.setMinMax(minX, maxX, minY, maxY, minZ, maxZ);
    }

    public void unload(Graphics graphics) {
        if (vao == null) return;

        graphics.deleteBuffer(vertexBuffer);
        graphics.deleteBuffer(textureCoordinatesBuffer);
        graphics.deleteBuffer(indexBuffer);
        graphics.deleteVertexArray(vao);

        vao = null;
        vertexBuffer = null;
        textureCoordinatesBuffer = null;
        indexBuffer = null;
    }
}
