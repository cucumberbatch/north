package org.north.core.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.north.core.exception.ShaderUniformNotFoundException;
import org.north.core.utils.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

//todo: this class is gonna be an OpenGL implementation of a future Graphics interface
public class Graphics {
    public Window window;

    public Matrix4f projection = new Matrix4f().identity();
    public Matrix4f view = new Matrix4f().lookAt(new Vector3f(0, 0, -1), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0));

    /** Orthographic projection for UI elements whose coordinates are window pixels with a top-left origin. */
    public final Matrix4f screenProjection = new Matrix4f().identity();
    /** Identity view matrix used together with {@link #screenProjection}. */
    public final Matrix4f screenView = new Matrix4f().identity();

    private final float[] matrixValues = new float[16];

    public Graphics(Window window) {
        this.window = window;
        updateScreenProjection();
    }

    public void updateScreenProjection() {
        float width  = Math.max(1, window.getWidth());
        float height = Math.max(1, window.getHeight());
        screenProjection.identity().ortho(0f, width, height, 0f, -1f, 1f);
    }

    public long getWindow() {
        return window.getWindow();
    }

    public int createShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
        int fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);
        int shaderProgramId = GL20.glCreateProgram();

        GL20.glAttachShader(shaderProgramId, vertexShaderId);
        GL20.glAttachShader(shaderProgramId, fragmentShaderId);

        GL20.glLinkProgram(shaderProgramId);

        if (GL20.glGetProgrami(shaderProgramId, GL20.GL_LINK_STATUS) == GL_FALSE) {
            String infoLog = GL20.glGetProgramInfoLog(shaderProgramId);
            GL20.glDeleteProgram(shaderProgramId);
            GL20.glDeleteShader(vertexShaderId);
            GL20.glDeleteShader(fragmentShaderId);
            throw new IllegalStateException("Failed to link shader program! Reason: " + infoLog);
        }

        GL20.glValidateProgram(shaderProgramId);

        GL20.glDeleteShader(fragmentShaderId);
        GL20.glDeleteShader(vertexShaderId);

        return shaderProgramId;
    }

    private int compileShader(int type, String shaderCode) {
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

    public void deleteShaderProgram(int programId) {
        GL20.glDeleteProgram(programId);
    }

    public void bindShaderProgram(int shaderId) {
        GL20.glUseProgram(shaderId);
    }

    public int findUniformLocation(int shaderId, String name) {
        return GL20.glGetUniformLocation(shaderId, name);
    }

    public int getUniformLocation(int shaderId, String name) {
        int uniformLocation = findUniformLocation(shaderId, name);
        if (uniformLocation == -1) {
            throw new ShaderUniformNotFoundException(shaderId, name);
        }
        return uniformLocation;
    }

    public void setUniformInt(int uniformLocation, int value) {
        GL20.glUniform1i(uniformLocation, value);
    }

    public void setUniformFloat(int uniformLocation, float value) {
        GL20.glUniform1f(uniformLocation, value);
    }

    public void setUniformFloat(int uniformLocation, Vector2f vector2) {
        GL20.glUniform2f(uniformLocation, vector2.x, vector2.y);
    }

    public void setUniformFloat(int uniformLocation, Vector3f vector3) {
        GL20.glUniform3f(uniformLocation, vector3.x, vector3.y, vector3.z);
    }

    public void setUniformFloat(int uniformLocation, Vector4f vector4) {
        GL20.glUniform4f(uniformLocation, vector4.x, vector4.y, vector4.z, vector4.w);
    }

    public void setUniformMatrixFloat(int uniformLocation, Matrix4f matrix4f) {
        matrix4f.get(matrixValues);
        GL20.glUniformMatrix4fv(uniformLocation, false, matrixValues);
    }

    public int createTexture(int width, int height, IntBuffer imageBuffer) {
        return createTexture(width, height, imageBuffer, GL_NEAREST, GL_NEAREST);
    }

    public int createTexture(int width, int height, IntBuffer imageBuffer, int minFilter, int magFilter) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        glBindTexture(GL_TEXTURE_2D, 0);
        return textureId;
    }

    public void deleteTexture(int textureId) {
        glDeleteTextures(textureId);
    }

    public void bindTexture(int textureId, int textureUnitSlot) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitSlot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    public VertexArrayHandle createVertexArray() {
        return new VertexArrayHandle(GL30.glGenVertexArrays());
    }

    public void bindVertexArray(VertexArrayHandle vao) {
        GL30.glBindVertexArray(vao != null ? vao.id() : 0);
    }

    public BufferHandle createArrayBuffer(float[] data, int attributeIndex, int coordinateSize) {
        int vbo = GL20.glGenBuffers();
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(data);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, buffer, GL20.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(attributeIndex, coordinateSize, GL20.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(attributeIndex);

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
        return new BufferHandle(vbo);
    }

    public BufferHandle createElementBuffer(int[] indices) {
        int ibo = GL20.glGenBuffers();
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, ibo);

        IntBuffer buffer = BufferUtils.createIntBuffer(indices);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, buffer, GL20.GL_STATIC_DRAW);
        // The element buffer stays bound to the currently bound vertex array.
        return new BufferHandle(ibo);
    }

    /**
     * @deprecated use {@link #createElementBuffer(int[])}. Byte indices cap meshes at 256 vertices.
     */
    @Deprecated
    public BufferHandle createElementBuffer(byte[] indices) {
        int[] converted = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            converted[i] = indices[i] & 0xFF;
        }
        return createElementBuffer(converted);
    }

    public void deleteVertexArray(VertexArrayHandle vao) {
        GL30.glDeleteVertexArrays(vao.id());
    }

    public void deleteBuffer(BufferHandle buffer) {
        GL20.glDeleteBuffers(buffer.id());
    }

    public void drawIndexed(VertexArrayHandle vao, int indexCount) {
        GL30.glBindVertexArray(vao.id());
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

}
