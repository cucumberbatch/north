package io.github.cucumberbatch.north.graphics.shader;

import io.github.cucumberbatch.north.asset.Asset;
import io.github.cucumberbatch.north.exception.ShaderUniformNotFoundException;
import io.github.cucumberbatch.north.graphics.Graphics;

import java.util.HashMap;
import java.util.Map;

public class Shader implements Asset {
    private final int id;
    private final Map<String, Integer> uniformPointers;

    public Shader(int id) {
        this.id = id;
        this.uniformPointers = new HashMap<>();
    }

    /**
     * Returns a cached uniform location or {@code -1} if the shader does not declare the uniform.
     */
    public int findUniformLocation(Graphics graphics, String uniformParameter) {
        Integer location = uniformPointers.get(uniformParameter);
        if (location == null) {
            location = graphics.findUniformLocation(id, uniformParameter);
            uniformPointers.put(uniformParameter, location);
        }
        return location;
    }

    public int getUniformLocation(Graphics graphics, String uniformParameter) {
        int location = findUniformLocation(graphics, uniformParameter);
        if (location == -1) {
            throw new ShaderUniformNotFoundException(id, uniformParameter);
        }
        return location;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Shader shader = (Shader) o;
        return id == shader.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
