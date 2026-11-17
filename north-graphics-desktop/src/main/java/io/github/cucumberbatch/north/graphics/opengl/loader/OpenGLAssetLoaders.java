package io.github.cucumberbatch.north.graphics.opengl.loader;

import io.github.cucumberbatch.north.asset.AssetService;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.Mesh;
import io.github.cucumberbatch.north.graphics.Texture;
import io.github.cucumberbatch.north.graphics.shader.Shader;
import io.github.cucumberbatch.north.io.LocalResourceLoader;
import io.github.cucumberbatch.north.io.ResourceLoader;

/**
 * Registers the asset loaders of the OpenGL backend on an {@link AssetService}.
 *
 * <p>The application composition root is responsible for calling this method, so the neutral
 * asset module stays independent of the graphics backend.</p>
 */
public final class OpenGLAssetLoaders {

    private OpenGLAssetLoaders() {
    }

    public static void registerLoaders(AssetService assets, Graphics graphics) {
        ResourceLoader resourceLoader = new LocalResourceLoader();
        assets.registerAssetLoader(Texture.class, new TextureAssetLoader(resourceLoader, graphics));
        assets.registerAssetLoader(Shader.class, new ShaderAssetLoader(resourceLoader, graphics));
        assets.registerAssetLoader(Mesh.class, new ObjMeshAssetLoader(resourceLoader, graphics));
    }
}
