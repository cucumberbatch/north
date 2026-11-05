package org.north.core.scene.tree;

public interface Action<E extends TreeNode<E>> {
    void execute(E node);

    default boolean isStoppingConditionSatisfied() {
        return false;
    }
}
