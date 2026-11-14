package io.github.cucumberbatch.north.architecture;

import io.github.cucumberbatch.north.scene.component.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentHandler {
    Class<? extends Component> value();
}
