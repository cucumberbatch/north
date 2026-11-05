package org.north.core.exception;

import org.north.core.scene.component.Component;

import java.util.UUID;

public class ComponentNotFoundException extends RuntimeException {
    public ComponentNotFoundException(UUID id) {
        super(String.format("Component with id=%s not found!", id.toString()));
    }

    public ComponentNotFoundException(Class<? extends Component> clazz) {
        super(String.format("Component of type=%s not found!", clazz.getSimpleName()));
    }
}
