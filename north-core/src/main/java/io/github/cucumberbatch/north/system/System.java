package io.github.cucumberbatch.north.system;

import io.github.cucumberbatch.north.scene.component.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public interface System<C extends Component> {

    Iterator<C> getComponentIterator();

    List<C> getComponentList();

    Collection<C> getComponentUnmodifiableCollection();

    C addComponent(Component component);

    C removeComponent(Component component);

    void reset();
}
