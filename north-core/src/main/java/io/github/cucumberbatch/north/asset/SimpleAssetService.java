package io.github.cucumberbatch.north.asset;

import io.github.cucumberbatch.north.asset.loader.AssetLoader;
import io.github.cucumberbatch.north.asset.loader.AudioAssetLoader;
import io.github.cucumberbatch.north.asset.loader.ObjMeshAssetLoader;
import io.github.cucumberbatch.north.asset.loader.ShaderAssetLoader;
import io.github.cucumberbatch.north.asset.loader.TextureAssetLoader;
import io.github.cucumberbatch.north.audio.Audio;
import io.github.cucumberbatch.north.audio.AudioClip;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.Mesh;
import io.github.cucumberbatch.north.graphics.Texture;
import io.github.cucumberbatch.north.graphics.shader.Shader;
import io.github.cucumberbatch.north.io.LocalResourceLoader;
import io.github.cucumberbatch.north.io.ResourceLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleAssetService implements AssetService {
    private final Map<Class<?>, AssetLoader<?>> loaderRegistry = new ConcurrentHashMap<>();

    public SimpleAssetService(Graphics graphics, Audio audio) {
        ResourceLoader resourceLoader = new LocalResourceLoader();
        loaderRegistry.put(Texture.class, new TextureAssetLoader(resourceLoader, graphics));
        loaderRegistry.put(Shader.class, new ShaderAssetLoader(resourceLoader, graphics));
        loaderRegistry.put(Mesh.class, new ObjMeshAssetLoader(resourceLoader, graphics));
        loaderRegistry.put(AudioClip.class, new AudioAssetLoader(resourceLoader, audio));
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

