package org.north.core.exception;

import org.north.core.scene.component.Component;

public class ComponentAlreadyExistsException extends RuntimeException {
    public ComponentAlreadyExistsException(Class<? extends Component> clazz) {
        super(String.format("Component of type '%s' already exists!", clazz.getName()));
    }
}
