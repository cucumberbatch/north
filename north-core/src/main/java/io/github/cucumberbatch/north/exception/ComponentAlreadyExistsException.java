package io.github.cucumberbatch.north.exception;

import io.github.cucumberbatch.north.scene.component.Component;

public class ComponentAlreadyExistsException extends RuntimeException {
    public ComponentAlreadyExistsException(Class<? extends Component> clazz) {
        super(String.format("Component of type '%s' already exists!", clazz.getName()));
    }
}
