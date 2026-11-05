package org.north.core.scene.tree;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class TreeNodeTest {

    private final Logger logger = LoggerFactory.getLogger(TreeNodeTest.class);

    static class TreeEntry extends LinkedTreeNode<TreeEntry> {
    }

    private TreeEntry n1;
    private TreeEntry n2;
    private TreeEntry n3;
    private TreeEntry n4;
    private TreeEntry n5;
    private TreeEntry n6;
    private TreeEntry n7;
    private TreeEntry n8;
    private TreeEntry n9;

    @BeforeEach
    void initEntities() {
        n1 = new TreeEntry();
        n2 = new TreeEntry();
        n3 = new TreeEntry();
        n4 = new TreeEntry();
        n5 = new TreeEntry();
        n6 = new TreeEntry();
        n7 = new TreeEntry();
        n8 = new TreeEntry();
        n9 = new TreeEntry();
    }

    void growAdvancedTree() {
        // 1st level
        n1.add(n2);
        n1.add(n3);

        // 2nd level
        n2.add(n7);
        n3.add(n4);
        n3.add(n5);
        n3.add(n6);

        // 3rd level
        n5.add(n8);
        n5.add(n9);
    }

    @Test
    void parent() {
        n1.add(n2);

        Assertions.assertEquals(n2.getParent(), n1);
        Assertions.assertNotEquals(n2.getParent(), n2);
        Assertions.assertNotEquals(n1.getParent(), n2);
        Assertions.assertNotEquals(n1.getParent(), n1);
    }

    @Test
    void getRoot() {
        growAdvancedTree();


    }

    @Test
    void add() {
        n1.add(n2);
        n1.add(n3);

        n2.add(n4);
        n2.add(n5);
        n3.add(n6);
        n3.add(n7);
        n3.add(n8);

        n8.add(n9);

        Assertions.assertFalse(n1.isLeaf());

        Assertions.assertFalse(n1.hasNextSibling());
        Assertions.assertTrue(n2.hasNextSibling());
        Assertions.assertFalse(n3.hasNextSibling());

        Assertions.assertSame(n3, n2.getNextSibling());

        Assertions.assertSame(n1, n2.getParent());
        Assertions.assertSame(n1, n3.getParent());
        Assertions.assertSame(n2, n4.getParent());
        Assertions.assertSame(n2, n5.getParent());
        Assertions.assertSame(n3, n6.getParent());
        Assertions.assertSame(n3, n7.getParent());
        Assertions.assertSame(n3, n8.getParent());
        Assertions.assertSame(n8, n9.getParent());

        Assertions.assertTrue(n4.isLeaf());
        Assertions.assertTrue(n5.isLeaf());
        Assertions.assertTrue(n6.isLeaf());
        Assertions.assertTrue(n7.isLeaf());
        Assertions.assertTrue(n9.isLeaf());
    }

    @Test
    void remove() {
    }

    @Test
    void hasNextSibling() {
    }

    @Test
    void getSubtrees() {
        n1.add(n2);

        Assertions.assertTrue(n1.getSubtrees().contains(n2));
        Assertions.assertFalse(n2.getSubtrees().contains(n2));
        Assertions.assertFalse(n2.getSubtrees().contains(n1));
        Assertions.assertFalse(n1.getSubtrees().contains(n1));
    }

    @Test
    void getSubtreesWide() {
        n1.add(n2);
        n1.add(n3);
        n1.add(n4);

        Assertions.assertTrue(n1.getSubtrees().contains(n2));
        Assertions.assertFalse(n2.getSubtrees().contains(n2));
        Assertions.assertFalse(n2.getSubtrees().contains(n1));
        Assertions.assertFalse(n1.getSubtrees().contains(n1));
        Assertions.assertTrue(n1.getSubtrees().contains(n4));
    }

    @Test
    void isLeaf() {
        growAdvancedTree();

        Assertions.assertTrue(n4.isLeaf());
        Assertions.assertTrue(n6.isLeaf());
        Assertions.assertTrue(n7.isLeaf());
        Assertions.assertTrue(n8.isLeaf());
        Assertions.assertTrue(n9.isLeaf());

        Assertions.assertFalse(n1.isLeaf());
        Assertions.assertFalse(n2.isLeaf());
        Assertions.assertFalse(n3.isLeaf());
        Assertions.assertFalse(n5.isLeaf());
    }

    @Test
    void isRoot() {
        growAdvancedTree();

        Assertions.assertSame(n1, n2.getRoot());
        Assertions.assertSame(n1, n3.getRoot());
        Assertions.assertSame(n1, n4.getRoot());
        Assertions.assertSame(n1, n5.getRoot());
        Assertions.assertSame(n1, n6.getRoot());
        Assertions.assertSame(n1, n7.getRoot());
        Assertions.assertSame(n1, n8.getRoot());
        Assertions.assertSame(n1, n9.getRoot());
    }

    @Test
    void isParentOf() {
        n1.add(n2);

        Assertions.assertTrue(n1.isParentOf(n2));
        Assertions.assertFalse(n2.isParentOf(n2));
        Assertions.assertFalse(n2.isParentOf(n1));
        Assertions.assertFalse(n1.isParentOf(n1));
    }

    @Test
    void isParentOfWithBiggerTree() {
        n1.add(n2);
        n1.add(n3);
        n2.add(n4);

        Assertions.assertTrue(n1.isParentOf(n2));
        Assertions.assertFalse(n2.isParentOf(n1));
        Assertions.assertTrue(n2.isParentOf(n4));
        Assertions.assertFalse(n3.isParentOf(n4));
        Assertions.assertFalse(n1.isParentOf(n4));
    }

    @Test
    void isAncestorOf() {
    }

    @Test
    void ascendantTraverse() {
        n1.add(n2);
        n2.add(n3);
        n3.add(n4);

        List<TreeEntry> expectedResult = new ArrayList<>(){{ add(n4); add(n3); add(n2); add(n1); }};
        List<TreeEntry> actualResult = new ArrayList<>();

        n4.ascendantTraverse(actualResult::add);

        Assertions.assertArrayEquals(expectedResult.toArray(), actualResult.toArray());
    }

    @Test
    void traversePreorderDepth() {
        n1.add(n2);
        n2.add(n3);
        n3.add(n4);

        n1.traversePreorder(node -> logger.info("{}", node));
    }

    @Test
    void traversePreorderWidth() {
        n1.add(n2);
        n1.add(n3);
        n1.add(n4);

        n1.traversePreorder(node -> logger.info("{}", node));
    }

    @Test
    void traversePreorderAdvanced() {
        growAdvancedTree();
        List<TreeNode<?>> expectedResult = new ArrayList<>();

        n1.traversePreorder(node -> logger.info("{}", node));
    }

    @Test
    void size() {
        growAdvancedTree();

        Assertions.assertEquals(9, n1.size());
        Assertions.assertEquals(3, n5.size());
        Assertions.assertEquals(6, n3.size());
        Assertions.assertEquals(2, n2.size());
        Assertions.assertEquals(1, n4.size());
    }

    @Test
    void iteratorRightToLeftSimple() {
        n1.add(n2);
        n2.add(n3);
        n3.add(n4);

        Iterator<TreeEntry> it = n1.iterator();

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n1, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n2, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n3, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n4, it.next());

    }

    @Test
    void iteratorRightToLeftAdvanced() {
        growAdvancedTree();

        Iterator<TreeEntry> it = n1.iterator();

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n1, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n3, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n6, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n5, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n9, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n8, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n4, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n2, it.next());

        Assertions.assertTrue(it.hasNext());
        Assertions.assertSame(n7, it.next());

        Assertions.assertFalse(it.hasNext());
    }
}