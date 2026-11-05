package org.north.core.graphics;

import org.north.core.asset.Asset;

public class Texture implements Asset {
    private final int textureId;
    private final int width;
    private final int height;

    public Texture(int textureId, int width, int height) {
        this.width = width;
        this.height = height;
        this.textureId = textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public int id() {
        return textureId;
    }
}
