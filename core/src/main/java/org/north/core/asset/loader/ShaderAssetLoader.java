package org.north.core.asset.loader;

import org.north.core.asset.AssetService;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.shader.Shader;
import org.north.core.io.ResourceLoader;

public class ShaderAssetLoader extends AssetLoader<Shader> {
    private static final String VERTEX_EXTENSION   = ".vert";
    private static final String FRAGMENT_EXTENSION = ".frag";

    private final ResourceLoader resourceLoader;
    private final Graphics graphics;

    public ShaderAssetLoader(ResourceLoader resourceLoader, Graphics graphics) {
        this.resourceLoader = resourceLoader;
        this.graphics = graphics;
    }

    @Override
    protected Shader load(String assetPath, AssetService assetService) {
        String fragmentShaderPath;
        String vertexShaderPath;
        if (assetPath.endsWith(FRAGMENT_EXTENSION)) {
            fragmentShaderPath = assetPath;
            vertexShaderPath = replaceExtension(assetPath, FRAGMENT_EXTENSION, VERTEX_EXTENSION);
        } else if (assetPath.endsWith(VERTEX_EXTENSION)) {
            fragmentShaderPath = replaceExtension(assetPath, VERTEX_EXTENSION, FRAGMENT_EXTENSION);
            vertexShaderPath = assetPath;
        } else {
            fragmentShaderPath = assetPath + FRAGMENT_EXTENSION;
            vertexShaderPath = assetPath + VERTEX_EXTENSION;
        }

        String fragmentShaderSource;
        String vertexShaderSource;
        try {
            fragmentShaderSource = resourceLoader.loadAsString(fragmentShaderPath);
            vertexShaderSource = resourceLoader.loadAsString(vertexShaderPath);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to load shader '%s'", assetPath), e);
        }

        int shaderId = graphics.createShaderProgram(vertexShaderSource, fragmentShaderSource);
        return new Shader(shaderId);
    }

    private static String replaceExtension(String path, String currentExtension, String newExtension) {
        return path.substring(0, path.length() - currentExtension.length()) + newExtension;
    }

    @Override
    protected void unload(Shader shader) {
        graphics.deleteShaderProgram(shader.id());
    }
}
