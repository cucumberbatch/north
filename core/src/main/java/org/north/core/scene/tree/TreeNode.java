package org.north.core.scene.tree;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;

/**
 * An interface for implementation of a tree-like data structures
 * @apiNote {@link Node} datatype must extend one of the implementations
 * of {@link TreeNode} interface to have access to this functionality
 * @param <Node> a type of data, that needs to be represented as a tree
 */
// TODO: Rethink the node thing to be more like a collection interface rather than a smart node, the node itself must be a simple struct which will be used by a tree collection
public interface TreeNode<Node extends TreeNode<Node>> extends Iterable<Node>, Serializable {

    /**
     * Checks if the current node does not have any subtrees
     * @return {@code true} if the current node has no children, {@code false} - otherwise
     */
    boolean isLeaf();

    /**
     * Returns a parent node relative to the current node, or null if there is no parent
     * @return parent {@link Node}, or {@code null} if there is no parent node
     */
    Node getParent();

    /**
     * Checks if this node has a reference to the next node on the same tree depth level
     * @return {@code true} if this node has a reference to the next node, {@code false} - otherwise
     */
    boolean hasNextSibling();

    /**
     * Checks if this node has a reference to the previous node on the same tree depth level
     * @return {@code true} if this node has a reference to the previous node, {@code false} - otherwise
     */
    boolean hasPreviousSibling();

    /**
     * Returns a reference to the next sibling node on the same tree depth level
     * @return {@link Node} of the next sibling node, {@code null} - if this is the rightmost node
     */
    Node getNextSibling();

    /**
     * Returns a reference to the previous sibling node on the same tree depth level
     * @return {@link Node} of the previous sibling node, {@code null} - if this is the leftmost node
     */
    Node getPreviousSibling(Node subtree);

    /**
     * Returns a reference to the left most daughter node from this node subtree
     * @return {@link Node} of the left most daughter node, {@code null} - if this node have no daughter nodes
     */
    Node getLeftMostDaughter();

    /**
     * Adds specified {@code subtree} to this entity as its daughter node
     * @param subtree a node that has to be added as a daughter node
     * @return {@code true} if that subtree was added successfully, {@code false} - if specified subtree is {@code null}
     */
    boolean add(Node subtree);

    boolean remove(Node subtree);

    Collection<Node> getSubtrees();


    /**
     * Checks if this node is the rightmost node
     * @return {@code true} if this is the rightmost node, {@code false} - otherwise
     */
    default boolean isRightMostNode() {
        return !hasNextSibling();
    }

    /**
     * Checks if this node is the leftmost node
     * @return {@code true} if this is the leftmost node, {@code false} - otherwise
     */
    default boolean isLeftMostNode() {
        return !hasPreviousSibling();
    }

    /**
     * Returns the root node relative to the current node
     * @return root {@link Node} relative to the current node
     */
    default Node getRoot() {
        if (isRoot()) {
            @SuppressWarnings("unchecked") Node node = (Node) this;
            return node;
        }
        Node root = getParent();
        while (!root.isRoot()) {
            root = root.getParent();
        }
        return root;
    }

    /**
     * Returns the size of the subtree rooted at the current node
     * @return size of a subtree
     */
    int size();

    /**
     * Checks whether there is a parent node for the specified node
     * @return true if there is no parent node, false - otherwise
     */
    default boolean isRoot() {
        return getParent() == null;
    }

    /**
     * Checks whether the current node is the parent of a given subtree
     * @param subtree a given subtree node
     * @return a boolean value that indicates whether the current node
     * is the parent of a given subtree node. If the subtree is not null and
     * the current node equals its parent, the method will return {@code true}.
     * Otherwise, it will return {@code false}.
     */
    default boolean isParentOf(Node subtree) {
        return subtree != null && this.equals(subtree.getParent());
    }

    default boolean isAncestorOf(Node subtree) {
        if (subtree == null || subtree.isRoot()) {
            return false;
        }
        Node parent = subtree;
        do {
            if (this.equals(parent)) {
                return true;
            }
            parent = parent.getRoot();
        } while (!parent.isRoot());
        return false;
    }

    default boolean isDescendantOf(Node subtree) {
        return subtree != null && this.isAncestorOf(subtree);
    }

    /**
     * Returns first occurrence of node which was accepted by provided {@code predicate} filter
     * @param predicate a filter predicate
     * @return the first found element that was accepted by provided filter or {@code null} if there is no such element
     * @throws NullPointerException if the specified {@code predicate} is null
     */
    @SuppressWarnings("unchecked")
    default Node findFirst(Predicate<Node> predicate) {
        if (predicate == null)
            throw new NullPointerException("Predicate must not be null!");

        Deque<Node> stack = new ArrayDeque<>();
        stack.push((Node) this);
        while (!stack.isEmpty()) {
            Node poppedNode = stack.pop();
            if (predicate.test(poppedNode))
                return poppedNode;
            for (Node descentSubtree : poppedNode.getSubtrees()) {
                stack.push(descentSubtree);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    default void ascendantTraverse(Action<Node> action) {
        Node currentNode = (Node) this;
        action.execute(currentNode);
        while (!currentNode.isRoot() && !action.isStoppingConditionSatisfied()) {
            currentNode = currentNode.getParent();
            action.execute(currentNode);
        }
    }

    @SuppressWarnings("unchecked")
    default void traversePreorder(Action<Node> action) {
        Deque<Node> stack = new ArrayDeque<>();
        stack.push((Node) this);
        while (!stack.isEmpty() && !action.isStoppingConditionSatisfied()) {
            Node poppedNode = stack.pop();
            action.execute(poppedNode);
            for (Node descentSubtree : poppedNode.getSubtrees()) {
                stack.push(descentSubtree);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    default Iterator<Node> iterator() {
        return new TreeNodeIterator<Node>((Node) this);
    }

    class TreeNodeIterator<Node extends TreeNode<Node>> implements Iterator<Node> {
        private final Deque<Node> traversalStack;

        public TreeNodeIterator(Node subtree) {
            traversalStack = new ArrayDeque<>();
            traversalStack.push(subtree);
        }

        @Override
        public boolean hasNext() {
            return !traversalStack.isEmpty();
        }

        @Override
        public Node next() {
            if (!hasNext())
                throw new NoSuchElementException();

            Node poppedNode = traversalStack.pop();
            for (Node descentNode : poppedNode.getSubtrees()) {
                traversalStack.push(descentNode);
            }
            return poppedNode;
        }
    }
}
