package org.north.core.exception;

public class ShaderUniformNotFoundException extends RuntimeException {
    private final int shaderId;
    private final String uniformName;

    public ShaderUniformNotFoundException(int shaderId, String uniformName) {
        super(String.format("Unable to find uniform variable '%s' of a shader with id=%d!", uniformName, shaderId));
        this.shaderId = shaderId;
        this.uniformName = uniformName;
    }

    public int shaderId() {
        return shaderId;
    }

    public String uniformName() {
        return uniformName;
    }
}
