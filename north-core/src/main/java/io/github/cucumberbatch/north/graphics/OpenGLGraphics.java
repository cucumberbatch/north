package io.github.cucumberbatch.north.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import io.github.cucumberbatch.north.exception.ShaderUniformNotFoundException;
import io.github.cucumberbatch.north.utils.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;

//todo: this class is gonna be an OpenGL implementation of a future OpenGLGraphics interface
public class OpenGLGraphics extends AbstractGraphics {
    public Window window;

    private final float[] matrixValues = new float[16];

    public OpenGLGraphics(Window window) {
        this.window = window;
        updateScreenProjection();
    }

    @Override
    public void updateScreenProjection() {
        float width  = Math.max(1, window.getWidth());
        float height = Math.max(1, window.getHeight());
        super.screenProjection.identity().ortho(0f, width, height, 0f, -1f, 1f);
    }

    @Override
    public void updateViewport(int x, int y, int width, int height) {
        window.setSize(width, height);
        updateScreenProjection();
        GL30.glViewport(x, y, width, height);
    }
    
    @Override
    public long getWindow() {
        return window.getWindow();
    }

    @Override
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

    @Override
    public void deleteShaderProgram(int programId) {
        GL20.glDeleteProgram(programId);
    }

    @Override
    public void bindShaderProgram(int shaderId) {
        GL20.glUseProgram(shaderId);
    }

    @Override
    public int findUniformLocation(int shaderId, String name) {
        return GL20.glGetUniformLocation(shaderId, name);
    }

    @Override
    public int getUniformLocation(int shaderId, String name) {
        int uniformLocation = findUniformLocation(shaderId, name);
        if (uniformLocation == -1) {
            throw new ShaderUniformNotFoundException(shaderId, name);
        }
        return uniformLocation;
    }

    @Override
    public void setUniformInt(int uniformLocation, int value) {
        GL20.glUniform1i(uniformLocation, value);
    }

    @Override
    public void setUniformFloat(int uniformLocation, float value) {
        GL20.glUniform1f(uniformLocation, value);
    }

    @Override
    public void setUniformFloat(int uniformLocation, Vector2f vector2) {
        GL20.glUniform2f(uniformLocation, vector2.x, vector2.y);
    }

    @Override
    public void setUniformFloat(int uniformLocation, Vector3f vector3) {
        GL20.glUniform3f(uniformLocation, vector3.x, vector3.y, vector3.z);
    }

    @Override
    public void setUniformFloat(int uniformLocation, Vector4f vector4) {
        GL20.glUniform4f(uniformLocation, vector4.x, vector4.y, vector4.z, vector4.w);
    }

    @Override
    public void setUniformMatrixFloat(int uniformLocation, Matrix4f matrix4f) {
        matrix4f.get(matrixValues);
        GL20.glUniformMatrix4fv(uniformLocation, false, matrixValues);
    }

    @Override
    public int createTexture(int width, int height, IntBuffer imageBuffer) {
        return createTexture(width, height, imageBuffer, GL_NEAREST, GL_NEAREST);
    }

    @Override
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

    @Override
    public void deleteTexture(int textureId) {
        glDeleteTextures(textureId);
    }

    @Override
    public void bindTexture(int textureId, int textureUnitSlot) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnitSlot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }

    @Override
    public VertexArrayHandle createVertexArray() {
        return new VertexArrayHandle(GL30.glGenVertexArrays());
    }

    @Override
    public void bindVertexArray(VertexArrayHandle vao) {
        GL30.glBindVertexArray(vao != null ? vao.id() : 0);
    }

    @Override
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

    @Override
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
    @Override
    @Deprecated
    public BufferHandle createElementBuffer(byte[] indices) {
        int[] converted = new int[indices.length];
        for (int i = 0; i < indices.length; i++) {
            converted[i] = indices[i] & 0xFF;
        }
        return createElementBuffer(converted);
    }

    @Override
    public void deleteVertexArray(VertexArrayHandle vao) {
        GL30.glDeleteVertexArrays(vao.id());
    }

    @Override
    public void deleteBuffer(BufferHandle buffer) {
        GL20.glDeleteBuffers(buffer.id());
    }

    @Override
    public void drawIndexed(VertexArrayHandle vao, int indexCount) {
        GL30.glBindVertexArray(vao.id());
        GL11.glEnable(GL32.GL_DEPTH_CLAMP);
        GL11.glDrawElements(GL11.GL_TRIANGLES, indexCount, GL11.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

}
