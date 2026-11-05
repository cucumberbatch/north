package org.north.core.physics.spatial;

import org.joml.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AabbTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void rayIntersectsBox() {
        Aabb box = new Aabb(-1f, 1f, -1f, 1f, -1f, 1f);

        float distance = box.intersectRay(
                new Vector3f(-2f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                100f
        );

        Assertions.assertEquals(1f, distance, EPSILON);
    }

    @Test
    void rayMissesBox() {
        Aabb box = new Aabb(-1f, 1f, -1f, 1f, -1f, 1f);

        float distance = box.intersectRay(
                new Vector3f(-2f, 2f, 0f),
                new Vector3f(1f, 0f, 0f),
                100f
        );

        Assertions.assertEquals(Float.POSITIVE_INFINITY, distance);
    }

    @Test
    void rayFromInsideBoxStartsAtZero() {
        Aabb box = new Aabb(-1f, 1f, -1f, 1f, -1f, 1f);

        float distance = box.intersectRay(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                100f
        );

        Assertions.assertEquals(0f, distance, EPSILON);
    }

    @Test
    void rayBeyondMaximumDistanceDoesNotHit() {
        Aabb box = new Aabb(10f, 11f, -1f, 1f, -1f, 1f);

        float distance = box.intersectRay(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                5f
        );

        Assertions.assertEquals(Float.POSITIVE_INFINITY, distance);
    }

    @Test
    void boxFromPointsCoversBothPoints() {
        Aabb box = new Aabb().setFromPoints(
                new Vector3f(2f, -1f, 4f),
                new Vector3f(-3f, 5f, 1f)
        );

        Assertions.assertEquals(-3f, box.xMin(), EPSILON);
        Assertions.assertEquals(2f, box.xMax(), EPSILON);
        Assertions.assertEquals(-1f, box.yMin(), EPSILON);
        Assertions.assertEquals(5f, box.yMax(), EPSILON);
        Assertions.assertEquals(1f, box.zMin(), EPSILON);
        Assertions.assertEquals(4f, box.zMax(), EPSILON);
    }

    @Test
    void boxesIntersectWhenTheyOverlapOrTouch() {
        Aabb aabb1 = new Aabb(0f, 2f, 0f, 2f, 0f, 2f);
        Aabb aabb2 = new Aabb(1f, 3f, 1f, 3f, 1f, 3f);
        Aabb aabb3 = new Aabb(3f, 5f, 3f, 5f, 3f, 5f);

        Aabb flat1 = new Aabb(0f, 2f, 0f, 0f, 0f, 2f);
        Aabb flat2 = new Aabb(0.5f, 1.5f, -1f, 1f, 0f, 0f);

        Assertions.assertTrue(aabb1.isIntersects(aabb2));
        Assertions.assertTrue(aabb2.isIntersects(aabb1));
        Assertions.assertFalse(aabb1.isIntersects(aabb3));
        Assertions.assertFalse(aabb3.isIntersects(aabb1));
        Assertions.assertTrue(flat1.isIntersects(flat2));
        Assertions.assertTrue(flat2.isIntersects(flat1));
    }

    @Test
    void boxCanBeInsideAnotherBox() {
        Aabb outer = new Aabb(0f, 2f, 0f, 2f, 0f, 2f);
        Aabb inner = new Aabb(0f, 1f, 0f, 1f, 0f, 1f);
        Aabb flat1 = new Aabb(0f, 2f, 0f, 0f, 0f, 2f);
        Aabb flat2 = new Aabb(0.5f, 1.5f, -1f, 1f, 0f, 0f);

        Assertions.assertTrue(inner.isInside(outer));
        Assertions.assertFalse(outer.isInside(inner));
        Assertions.assertFalse(flat1.isInside(flat2));
        Assertions.assertFalse(flat2.isInside(flat1));
    }

    @Test
    void insideAlsoIntersects() {
        Aabb outer = new Aabb(-2f, 2f, -2f, 2f, -2f, 2f);
        Aabb inner = new Aabb(-1f, 1f, -1f, 1f, -1f, 1f);

        Assertions.assertFalse(outer.isInside(inner));
        Assertions.assertTrue(inner.isInside(outer));
        Assertions.assertTrue(outer.isIntersects(inner));
        Assertions.assertTrue(inner.isIntersects(outer));
    }
}
