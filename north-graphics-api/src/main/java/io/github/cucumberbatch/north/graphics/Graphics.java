package io.github.cucumberbatch.north.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.IntBuffer;

public interface Graphics {

    Matrix4f view();

    Matrix4f projection();

    Matrix4f screenView();

    Matrix4f screenProjection();

    void updateScreenProjection();

    void updateViewport(int x, int y, int width, int height);

    /**
     * Clears the color and depth buffers of the current render target.
     */
    void clear();

    /**
     * Sets the color which is used by {@link #clear()}.
     */
    void setClearColor(float red, float green, float blue, float alpha);

    /**
     * Enables or disables the depth test for subsequent draws.
     */
    void setDepthTestEnabled(boolean enabled);

    int createShaderProgram(String vertexSource, String fragmentSource);

    void deleteShaderProgram(int programId);

    void bindShaderProgram(int shaderId);

    int findUniformLocation(int shaderId, String name);

    int getUniformLocation(int shaderId, String name);

    void setUniformInt(int uniformLocation, int value);

    void setUniformFloat(int uniformLocation, float value);

    void setUniformFloat(int uniformLocation, Vector2f vector2);

    void setUniformFloat(int uniformLocation, Vector3f vector3);

    void setUniformFloat(int uniformLocation, Vector4f vector4);

    void setUniformMatrixFloat(int uniformLocation, Matrix4f matrix4f);

    int createTexture(int width, int height, IntBuffer imageBuffer);

    int createTexture(int width, int height, IntBuffer imageBuffer,
                      TextureFilter minFilter, TextureFilter magFilter);

    void deleteTexture(int textureId);

    void bindTexture(int textureId, int textureUnitSlot);

    VertexArrayHandle createVertexArray();

    void bindVertexArray(VertexArrayHandle vao);

    BufferHandle createArrayBuffer(float[] data, int attributeIndex, int coordinateSize);

    BufferHandle createElementBuffer(int[] indices);

    @Deprecated
    BufferHandle createElementBuffer(byte[] indices);

    void deleteVertexArray(VertexArrayHandle vao);

    void deleteBuffer(BufferHandle buffer);

    void drawIndexed(VertexArrayHandle vao, int indexCount);

}
