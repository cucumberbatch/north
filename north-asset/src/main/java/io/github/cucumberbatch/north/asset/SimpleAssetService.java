package io.github.cucumberbatch.north.asset;

import io.github.cucumberbatch.north.asset.loader.AssetLoader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A backend-neutral asset registry.
 *
 * <p>The service only knows about {@link AssetLoader} implementations which have been registered
 * with it. Backends and the application composition root register their own loaders, so this
 * module does not depend on graphics or audio implementations.</p>
 */
public class SimpleAssetService implements AssetService {
    private final Map<Class<?>, AssetLoader<?>> loaderRegistry = new ConcurrentHashMap<>();

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
