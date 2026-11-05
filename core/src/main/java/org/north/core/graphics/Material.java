package org.north.core.graphics;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.north.core.graphics.shader.Shader;

import java.util.HashMap;
import java.util.Map;

public class Material {
    private static final String PROJECTION_UNIFORM = "u_projection";
    private static final String VIEW_UNIFORM       = "u_view";
    private static final String MODEL_UNIFORM      = "u_model";

    private final Shader shader;
    private final Map<String, Object> properties;
    private final Map<String, Texture> textures;

    public Material(Shader shader) {
        this.shader = shader;
        this.properties = new HashMap<>();
        this.textures = new HashMap<>();
    }

    public Shader shader() {
        return shader;
    }

    public Material setInt(String parameter, int value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setFloat(String parameter, float value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setVector(String parameter, Vector2f value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setVector(String parameter, Vector3f value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setVector(String parameter, Vector4f value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setMatrix(String parameter, Matrix4f value) {
        properties.put(parameter, value);
        return this;
    }

    public Material setTexture(String parameter, Texture texture) {
        textures.put(parameter, texture);
        return this;
    }

    public int getInt(String parameter) {
        return (Integer) getProperty(parameter);
    }

    public float getFloat(String parameter) {
        return (Float) getProperty(parameter);
    }

    public Vector2f getVector2(String parameter) {
        return (Vector2f) getProperty(parameter);
    }

    public Vector3f getVector3(String parameter) {
        return (Vector3f) getProperty(parameter);
    }

    public Vector4f getVector4(String parameter) {
        return (Vector4f) getProperty(parameter);
    }

    public Matrix4f getMatrix(String parameter) {
        return (Matrix4f) getProperty(parameter);
    }

    private Object getProperty(String parameter) {
        Object value = properties.get(parameter);
        if (value == null) {
            throw new IllegalArgumentException(
                    String.format("Material property '%s' is not set for shader with id=%d", parameter, shader.id())
            );
        }
        return value;
    }

    public void apply(Graphics graphics) {
        apply(graphics, null, graphics.projection, graphics.view);
    }

    public void apply(Graphics graphics, Matrix4f modelMatrix) {
        apply(graphics, modelMatrix, graphics.projection, graphics.view);
    }

    public void apply(Graphics graphics,
                      Matrix4f modelMatrix,
                      Matrix4f projectionMatrix,
                      Matrix4f viewMatrix) {
        graphics.bindShaderProgram(shader.id());

        setUniformIfPresent(graphics, PROJECTION_UNIFORM, projectionMatrix);
        setUniformIfPresent(graphics, VIEW_UNIFORM, viewMatrix);
        if (modelMatrix != null) {
            setUniformIfPresent(graphics, MODEL_UNIFORM, modelMatrix);
        }

        int textureSlot = 0;
        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            Texture texture = entry.getValue();
            if (texture == null) continue;

            int uniformLocation = shader.findUniformLocation(graphics, entry.getKey());
            if (uniformLocation == -1) continue;

            graphics.bindTexture(texture.id(), textureSlot);
            graphics.setUniformInt(uniformLocation, textureSlot);
            textureSlot++;
        }

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            // Shader compilers remove unused uniforms. Materials may be shared between shader
            // variants, so only apply the properties that are actually present in the program.
            int uniformLocation = shader.findUniformLocation(graphics, entry.getKey());
            if (uniformLocation == -1) continue;
            setUniform(graphics, uniformLocation, entry.getValue());
        }
    }

    private void setUniformIfPresent(Graphics graphics, String parameter, Matrix4f matrix) {
        int uniformLocation = shader.findUniformLocation(graphics, parameter);
        if (uniformLocation != -1) {
            graphics.setUniformMatrixFloat(uniformLocation, matrix);
        }
    }

    private void setUniform(Graphics graphics, int uniformLocation, Object value) {
        if (value instanceof Integer) {
            graphics.setUniformInt(uniformLocation, (Integer) value);
        } else if (value instanceof Float) {
            graphics.setUniformFloat(uniformLocation, (Float) value);
        } else if (value instanceof Vector2f) {
            graphics.setUniformFloat(uniformLocation, (Vector2f) value);
        } else if (value instanceof Vector3f) {
            graphics.setUniformFloat(uniformLocation, (Vector3f) value);
        } else if (value instanceof Vector4f) {
            graphics.setUniformFloat(uniformLocation, (Vector4f) value);
        } else if (value instanceof Matrix4f) {
            graphics.setUniformMatrixFloat(uniformLocation, (Matrix4f) value);
        } else {
            throw new IllegalArgumentException(String.format(
                    "Unsupported material property type '%s' for shader with id=%d",
                    value != null ? value.getClass().getName() : "null",
                    shader.id()
            ));
        }
    }
}
