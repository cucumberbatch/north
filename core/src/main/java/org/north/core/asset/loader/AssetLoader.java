package org.north.core.asset.loader;

import org.north.core.asset.Asset;
import org.north.core.asset.AssetService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AssetLoader<T extends Asset> implements AutoCloseable {
    private final Map<String, T> loadedAssets = new ConcurrentHashMap<>();

    public T get(String assetPath, AssetService assetService) {
        return loadedAssets.computeIfAbsent(assetPath, path -> load(path, assetService));
    }

    protected abstract T load(String assetPath, AssetService assetService);
    protected abstract void unload(T asset);

    @Override
    public void close() {
        for (T asset : loadedAssets.values()) {
            unload(asset);
        }
        loadedAssets.clear();
    }
}
