package org.north.core.physics.spatial;

import org.joml.Vector3f;

import java.util.*;

/**
 * A simple OctreeNode data structure implementation
 */
public class Octree<E extends Aabb> {
    private static class Node<E extends Aabb> extends Aabb {
        private final Node<E>   parent;
        private final List<E>   elements;
        private Node<E>[]       children;

        Node(Node<E> parent, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
            super(xMin, xMax, yMin, yMax, zMin, zMax);
            this.parent = parent;
            this.elements = new ArrayList<>();
        }

        boolean isRoot() {
            return this.parent == null;
        }

        boolean isLeaf() {
            return this.children == null;
        }
    }


    private static final byte DEFAULT_MAX_ELEMENTS_PER_NODE = 2;
    private static final byte DEFAULT_MAX_DEPTH             = 2;

    private final Node<E> root;
    private final int     maxElementsPerNode;
    private final int     maxDepth;

    private int depth;

    public Octree(Vector3f nearBottomLeft, Vector3f farTopRight) {
        this(nearBottomLeft.x, farTopRight.x, nearBottomLeft.y, farTopRight.y, nearBottomLeft.z, farTopRight.z, DEFAULT_MAX_DEPTH, DEFAULT_MAX_ELEMENTS_PER_NODE);
    }

    public Octree(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        this(xMin, xMax, yMin, yMax, zMin, zMax, DEFAULT_MAX_DEPTH, DEFAULT_MAX_ELEMENTS_PER_NODE);
    }

    private Octree(float xMin, float xMax, float yMin, float yMax, float zMin, float zMax, int maxDepth, int densityFactor) {
        this.root = new Node<>(null, xMin, xMax, yMin, yMax, zMin, zMax);
        this.maxDepth = maxDepth;
        this.maxElementsPerNode = densityFactor;
    }

    /**
     * Adds a provided element into an octree
     * @param element element to add into an octree
     */
    public boolean add(E element) {
        return element.isIntersects(root) && add(root, 0, element);
    }

    private boolean add(Node<E> currentNode, int currentDepth, E element) {
        if (currentNode.isLeaf()) {
            currentNode.elements.add(element);
            if (currentNode.elements.size() - 1>= maxElementsPerNode) {
                if (currentDepth + 1 < maxDepth) {
                    subdivide(currentNode);
                    for (E movingElement : currentNode.elements) {
                        for (Node<E> childNode : currentNode.children) {
                            if (!movingElement.isIntersects(childNode)) {
                                continue;
                            }
                            add(childNode, currentDepth + 1, movingElement);
                        }
                    }
                    currentNode.elements.clear();
                }
            }
        } else {
            for (Node<E> childNode : currentNode.children) {
                if (element.isIntersects(childNode))
                    add(childNode, currentDepth + 1, element);
            }
        }
        return true;
    }

    private void subdivide(Node<E> node) {
        float xMin = node.xMin();
        float yMin = node.yMin();
        float zMin = node.zMin();
        float xMax = node.xMax();
        float yMax = node.yMax();
        float zMax = node.zMax();
        float xMid = (xMax + xMin) * 0.5f;
        float yMid = (yMax + yMin) * 0.5f;
        float zMid = (zMax + zMin) * 0.5f;

        node.children = new Node[]{
                new Node<>(node, xMin, xMid, yMin, yMid, zMin, zMid),
                new Node<>(node, xMid, xMax, yMin, yMid, zMin, zMid),
                new Node<>(node, xMin, xMid, yMid, yMax, zMin, zMid),
                new Node<>(node, xMid, xMax, yMid, yMax, zMin, zMid),
                new Node<>(node, xMin, xMid, yMin, yMid, zMid, zMax),
                new Node<>(node, xMid, xMax, yMin, yMid, zMid, zMax),
                new Node<>(node, xMin, xMid, yMid, yMax, zMid, zMax),
                new Node<>(node, xMid, xMax, yMid, yMax, zMid, zMax),
        };
    }

    /**
     * Removes a provided element from an octree entirely
     * @param element element to remove
     */
    public boolean remove(E element) {
        Set<Node<E>> nodesContainingElement = new HashSet<>();
        findNodesContaining(root, element, nodesContainingElement);

        if (nodesContainingElement.isEmpty()) {
            return false;
        }

        // Remove element from all nodes that contain it
        for (Node<E> node : nodesContainingElement) {
            node.elements.remove(element);
        }

        // Try to cleanup empty nodes from bottom up
        for (Node<E> node : nodesContainingElement) {
            cleanupNode(node);
        }

        return true;
    }

    private void findNodesContaining(Node<E> node, E element, Set<Node<E>> result) {
        if (!node.isLeaf() && node.children != null) {
            for (Node<E> child : node.children) {
                if (element.isIntersects(child)) {
                    findNodesContaining(child, element, result);
                }
            }
        }

        if (node.elements.contains(element)) {
            result.add(node);
        }
    }

    private void cleanupNode(Node<E> node) {
        if (!node.elements.isEmpty()) return;

        // If node has children, check if we can merge them
        if (!node.isLeaf()) {
            boolean allChildrenEmpty = true;
            boolean allChildrenLeaves = true;

            for (Node<E> child : node.children) {
                if (child != null) {
                    if (!child.elements.isEmpty()) {
                        allChildrenEmpty = false;
                    }
                    if (!child.isLeaf()) {
                        allChildrenLeaves = false;
                    }
                }
            }

            // If all children are empty leaves, remove them
            if (allChildrenEmpty && allChildrenLeaves) {
                node.children = null;
            }
        }

        // Recursively cleanup parent
        if (!node.isRoot()) {
            cleanupNode(node.parent);
        }
    }

    /**
     * Updates the position of element from old bounds to new bounds
     * @param oldElement element with old position/bounds
     * @param newElement element with new position/bounds
     */
    public void update(E oldElement, E newElement) {
        if (remove(oldElement)) {
            add(newElement);
        }
    }

    /**
     * Query all elements that lies in a provided {@code queryBox}
     * @param queryBox bounding box that represents a query for selecting elements
     * @return an iterable data structure of objects of type {@code E}
     */
    public Iterable<E> query(Aabb queryBox) {
        if (!queryBox.isIntersects(root))
            return Collections.emptyList();

        return () -> new OctreeLazyIterator(queryBox);
    }

    /**
     * Iterator implementation for lazy traversal of octree nodes
     */
    private class OctreeLazyIterator implements Iterator<E> {
        private final Aabb queryBox;
        private final Deque<Node<E>> nodeStack;
        private final Set<E> returnedElements;
        private E nextElement;

        private OctreeLazyIterator(Aabb queryBox) {
            this.queryBox = queryBox;
            this.nodeStack = new ArrayDeque<>();
            this.returnedElements = new HashSet<>();

            // Initialize with root node
            if (queryBox.isIntersects(root)) {
                nodeStack.push(root);
            }

            this.nextElement = findNextElement();
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public E next() {
            if (!hasNext())
                throw new NoSuchElementException();

            E element = nextElement;
            nextElement = findNextElement();
            return element;
        }

        /**
         * Finds the next element that intersects with query box and hasn't been returned yet
         */
        private E findNextElement() {
            while (!nodeStack.isEmpty()) {
                Node<E> currentNode = nodeStack.pop();

                // Check if current node intersects with query box
                if (!queryBox.isIntersects(currentNode)) {
                    continue;
                }

                // Process elements in current node
                for (E element : currentNode.elements) {
                    if (queryBox.isIntersects(element) && !returnedElements.contains(element)) {
                        returnedElements.add(element);

                        // Push current node back if there might be more elements
                        // We'll continue from this node after returning the element
                        if (currentNode.elements.size() > 1) {
                            nodeStack.push(currentNode);
                        }

                        // Push children for subsequent traversal
                        if (!currentNode.isLeaf()) {
                            for (int i = currentNode.children.length - 1; i >= 0; i--) {
                                if (queryBox.isIntersects(currentNode.children[i])) {
                                    nodeStack.push(currentNode.children[i]);
                                }
                            }
                        }

                        return element;
                    }
                }

                // If no elements found in current node, push children for traversal
                if (!currentNode.isLeaf()) {
                    for (int i = currentNode.children.length - 1; i >= 0; i--) {
                        Node<E> child = currentNode.children[i];
                        if (queryBox.isIntersects(child)) {
                            nodeStack.push(child);
                        }
                    }
                }
            }

            return null;
        }
    }

    /**
     * Check if the tree contains a specific element
     */
    public boolean contains(E element) {
        return contains(root, element);
    }

    private boolean contains(Node<E> node, E element) {
        if (node.elements.contains(element)) {
            return true;
        }

        if (!node.isLeaf()) {
            for (Node<E> child : node.children) {
                if (element.isIntersects(child) && contains(child, element)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Clear all elements from the octree
     */
    public void clear() {
        clearNode(root);
    }

    private void clearNode(Node<E> node) {
        node.elements.clear();
        if (!node.isLeaf()) {
            for (Node<E> child : node.children) {
                clearNode(child);
            }
            node.children = null;
        }
    }

    /**
     * Get total number of unique elements in the octree
     */
    public int size() {
        Set<E> uniqueElements = new HashSet<>();
        collectUniqueElements(root, uniqueElements);
        return uniqueElements.size();
    }

    private void collectUniqueElements(Node<E> node, Set<E> uniqueElements) {
        uniqueElements.addAll(node.elements);

        if (!node.isLeaf()) {
            for (Node<E> child : node.children) {
                collectUniqueElements(child, uniqueElements);
            }
        }
    }

    /**
     * Get total number of element references in the octree (including duplicates)
     * @apiNote Useful for debugging
     */
    public int getTotalElementReferences() {
        return countElementReferences(root);
    }

    private int countElementReferences(Node<E> node) {
        int count = node.elements.size();
        if (!node.isLeaf()) {
            for (Node<E> child : node.children) {
                count += countElementReferences(child);
            }
        }
        return count;
    }

    /**
     * Check if an element is stored in multiple nodes
     * @apiNote Useful for debugging
     */
    public boolean isElementDuplicated(E element) {
        return 1 < countElementOccurrences(root, element);
    }

    private int countElementOccurrences(Node<E> node, E element) {
        int count = node.elements.contains(element) ? 1 : 0;
        if (!node.isLeaf()) {
            for (Node<E> child : node.children) {
                if (element.isIntersects(child)) {
                    count += countElementOccurrences(child, element);
                }
            }
        }
        return count;
    }

    private class NodeDepth {
        Node<E> node;
        Integer depth;

        NodeDepth(Node<E> node, Integer depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Deque<NodeDepth> stack = new ArrayDeque<>();
        stack.push(new NodeDepth(root, 0));

        while (!stack.isEmpty()) {
            NodeDepth nodeDepth = stack.pop();
            Node<E> node = nodeDepth.node;
            int depth = nodeDepth.depth;

            String indent = "  ".repeat(depth);
            sb.append(indent)
                    .append(node.isRoot() ? "Root" : node.isLeaf() ? "Leaf" : "Node")
                    .append(" [").append(node.elements.size()).append(" elements]")
                    .append(" bounds: [")
                    .append(String.format("%.1f-%.1f", node.xMin(), node.xMax())).append(", ")
                    .append(String.format("%.1f-%.1f", node.yMin(), node.yMax())).append(", ")
                    .append(String.format("%.1f-%.1f", node.zMin(), node.zMax())).append("]\n");

            if (!node.isLeaf()) {
                for (int i = node.children.length - 1; i >= 0; i--) {
                    stack.push(new NodeDepth(node.children[i], depth + 1));
                }
            }
        }
        return sb.toString();
    }
}
