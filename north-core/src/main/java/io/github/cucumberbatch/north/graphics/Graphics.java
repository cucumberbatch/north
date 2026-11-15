package io.github.cucumberbatch.north.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FALSE;

public interface Graphics {

    Matrix4f view();

    Matrix4f projection();

    Matrix4f screenView();

    Matrix4f screenProjection();

    void updateScreenProjection();

    long getWindow();

    int createShaderProgram(String vertexSource, String fragmentSource);

    default int compileShader(int type, String shaderCode) {
        int shaderId = GL20.glCreateShader(type);

        GL20.glShaderSource(shaderId, shaderCode);
        GL20.glCompileShader(shaderId);

        int compileStatus = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (compileStatus == GL_FALSE) {
            String infoLog = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId);
            throw new IllegalStateException("Failed to compile shader! Reason: " + infoLog);
        }

        return shaderId;
    }

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

    int createTexture(int width, int height, IntBuffer imageBuffer, int minFilter, int magFilter);

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
