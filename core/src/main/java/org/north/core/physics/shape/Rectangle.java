package org.north.core.physics.shape;

import org.joml.Vector2f;

public class Rectangle {

    public Vector2f topLeft;

    // todo: i think, more convenient usage would be using height and width
    //  instead of using bottom right corner point
    public Vector2f bottomRight;


    public Rectangle(Vector2f topLeft, Vector2f bottomRight) {
        this.topLeft     = topLeft;
        this.bottomRight = bottomRight;
    }

    public void moveTo(Vector2f position) {
        moveTo(position.x, position.y);
    }

    public void moveTo(float x, float y) {
        float halfWidth  = width() / 2;
        float halfHeight = height() / 2;

        this.topLeft.set(x - halfWidth, y - halfHeight);
        this.bottomRight.set(x + halfWidth, y + halfHeight);
    }

    public void setFromCenter(Vector2f center, Vector2f size) {
        float halfWidth  = size.x / 2;
        float halfHeight = size.y / 2;

        this.topLeft.set(center.x - halfWidth, center.y - halfHeight);
        this.bottomRight.set(center.x + halfWidth, center.y + halfHeight);
    }

    public Vector2f getCenter(Vector2f destination) {
        return destination.set(topLeft).add(bottomRight).mul(0.5f);
    }

    public float width() {
        return Math.abs(this.bottomRight.x - this.topLeft.x);
    }

    public float height() {
        return Math.abs(this.bottomRight.y - this.topLeft.y);
    }

    public boolean isEmpty() {
        return width() == 0 || height() == 0;
    }

    public boolean isInside(Vector2f point) {
        float minX = Math.min(topLeft.x, bottomRight.x);
        float maxX = Math.max(topLeft.x, bottomRight.x);
        float minY = Math.min(topLeft.y, bottomRight.y);
        float maxY = Math.max(topLeft.y, bottomRight.y);

        return point.x >= minX && point.x <= maxX &&
               point.y >= minY && point.y <= maxY;
    }

    public boolean isInside(Rectangle rectangle) {
        return isInside(rectangle.topLeft) &&
               isInside(rectangle.bottomRight);
    }

    public boolean isIntersects(Rectangle rectangle) {
        float thisMinX = Math.min(topLeft.x, bottomRight.x);
        float thisMaxX = Math.max(topLeft.x, bottomRight.x);
        float thisMinY = Math.min(topLeft.y, bottomRight.y);
        float thisMaxY = Math.max(topLeft.y, bottomRight.y);

        float otherMinX = Math.min(rectangle.topLeft.x, rectangle.bottomRight.x);
        float otherMaxX = Math.max(rectangle.topLeft.x, rectangle.bottomRight.x);
        float otherMinY = Math.min(rectangle.topLeft.y, rectangle.bottomRight.y);
        float otherMaxY = Math.max(rectangle.topLeft.y, rectangle.bottomRight.y);

        return !(otherMaxX < thisMinX ||
                 otherMaxY < thisMinY ||
                 otherMinX > thisMaxX ||
                 otherMinY > thisMaxY);
    }

    public boolean isIdentical(Rectangle rectangle) {
        float epsilon = 0.01f;

        return Math.abs(rectangle.width()  - width())  < epsilon &&
               Math.abs(rectangle.height() - height()) < epsilon;
    }

}
