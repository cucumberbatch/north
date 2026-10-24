package org.north.core.asset;

import org.north.core.asset.loader.AssetLoader;
import org.north.core.asset.loader.ObjMeshAssetLoader;
import org.north.core.asset.loader.ShaderAssetLoader;
import org.north.core.asset.loader.TextureAssetLoader;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Mesh;
import org.north.core.graphics.Texture;
import org.north.core.graphics.shader.Shader;
import org.north.core.io.LocalResourceLoader;
import org.north.core.io.ResourceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleAssetService implements AssetService {
    private final Map<Class<?>, AssetLoader<?>> loaderRegistry = new ConcurrentHashMap<>();

    public SimpleAssetService(Graphics graphics) {
        ResourceLoader resourceLoader = new LocalResourceLoader();
        loaderRegistry.put(Texture.class, new TextureAssetLoader(resourceLoader, graphics));
        loaderRegistry.put(Shader.class, new ShaderAssetLoader(resourceLoader, graphics));
        loaderRegistry.put(Mesh.class, new ObjMeshAssetLoader(resourceLoader, graphics));
    }

    @Override
    public <T extends Asset> void registerAssetLoader(Class<T> type, AssetLoader<T> assetLoader) {
        loaderRegistry.put(type, assetLoader);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Asset> T get(Class<T> type, String path) {
        AssetLoader<T> assetLoader = (AssetLoader<T>) loaderRegistry.get(type);
        if (assetLoader == null) {
            throw new IllegalArgumentException("No asset loader registered for type " + type.getName());
        }
        return assetLoader.get(path, this);
    }

    @Override
    public void close() {
        for (AssetLoader<?> assetLoader : loaderRegistry.values()) {
            assetLoader.close();
        }
        loaderRegistry.clear();
    }
}

