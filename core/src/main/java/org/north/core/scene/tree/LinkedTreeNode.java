package org.north.core.scene.tree;

import java.util.*;

public abstract class LinkedTreeNode<Node extends LinkedTreeNode<Node>> implements TreeNode<Node> {
    protected Node parent;
    protected Node nextSibling;
    protected Node leftMostDaughter;

    protected void setLeftMostDaughter(Node leftMostDaughter) {
        this.leftMostDaughter = leftMostDaughter;
    }

    protected void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
    }

    protected void setParent(Node parent) {
        this.parent = parent;
    }

    @Override
    public Node getParent() {
        return parent;
    }

    @Override
    public boolean isLeaf() {
        return getLeftMostDaughter() == null;
    }

    @Override
    public Node getLeftMostDaughter() {
        return leftMostDaughter;
    }

    @Override
    public boolean hasNextSibling() {
        return this.getNextSibling() != null;
    }

    @Override
    public Node getNextSibling() {
        return nextSibling;
    }

    @Override
    public boolean hasPreviousSibling() {
        return !isRoot() && !getParent().getLeftMostDaughter().equals(this);
    }

    @Override
    public Node getPreviousSibling(Node subtree) {
        Node parent = subtree.getParent();
        Node daughter = parent.getLeftMostDaughter();
        if (daughter == null || daughter == subtree) {
            return null;
        }
        while (daughter.getNextSibling() != null) {
            if (daughter.getNextSibling() == subtree) {
                return daughter;
            }
            daughter = daughter.getNextSibling();
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Node> getSubtrees() {
        return isLeaf() ? Collections.emptyList() : new UnmodifiableLinkedSubtreeCollection<>((Node) this);
    }

    @Override
    public int size() {
        final int[] counter = new int[]{0};
        traversePreorder(node -> counter[0]++);
        return counter[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean add(Node subtree) {
        if (subtree == null) {
            return false;
        }
        if (isLeaf()) {
            setLeftMostDaughter(subtree);
        } else {
            Node daughter = getLeftMostDaughter();
            while (daughter.hasNextSibling()) {
                daughter = daughter.getNextSibling();
            }
            daughter.setNextSibling(subtree);
        }
        subtree.setParent((Node) this);
        return true;
    }

    @Override
    public boolean remove(Node subtree) {
        if (subtree == null || subtree.isRoot()) {
            return false;
        }
        Node root = getRoot();
        if (!root.isAncestorOf(subtree)) {
            return false;
        }
        Node subtreeParent = subtree.getParent();
        Node previousSibling = getPreviousSibling(subtree);
        if (previousSibling != null) {
            previousSibling.setNextSibling(null);
        } else {
            subtreeParent.setLeftMostDaughter(getLeftMostDaughter());
        }
        subtreeParent.setParent(null);
        return false;
    }


    static class UnmodifiableLinkedSubtreeCollection<Node extends LinkedTreeNode<Node>> extends AbstractCollection<Node> {
        private final Node firstNode;
        private final int size;

        UnmodifiableLinkedSubtreeCollection(Node parentNode) {
            this.firstNode = parentNode.getLeftMostDaughter();
            this.size = countSubtrees(parentNode);
        }

        private int countSubtrees(Node parentNode) {
            if (parentNode.isLeaf()) return 0;
            int count = 1;
            for (Node daughter = parentNode.getLeftMostDaughter(); daughter.hasNextSibling(); daughter = daughter.getNextSibling()) {
                count++;
            }
            return count;
        }

        @Override
        public Iterator<Node> iterator() {
            return new LinkedSubtreeIterator<>(firstNode);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public String toString() {
            return "UnmodifiableLinkedSubtreeCollection{" +
                    "size=" + size +
                    ", firstNode=" + firstNode +
                    '}';
        }

        static class LinkedSubtreeIterator<Node extends LinkedTreeNode<Node>> implements Iterator<Node> {
            private Node subtreeNode;

            LinkedSubtreeIterator(Node subtreeNode) {
                this.subtreeNode = subtreeNode;
            }

            @Override
            public boolean hasNext() {
                return subtreeNode != null;
            }

            @Override
            public Node next() {
                Node previousNode = subtreeNode;
                subtreeNode = previousNode.getNextSibling();
                return previousNode;
            }
        }
    }

}
