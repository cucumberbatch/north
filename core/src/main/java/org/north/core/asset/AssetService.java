package org.north.core.asset;

import org.north.core.asset.loader.AssetLoader;

public interface AssetService extends AutoCloseable {
    <T extends Asset> void registerAssetLoader(Class<T> type, AssetLoader<T> assetLoader);
    <T extends Asset> T get(Class<T> type, String path);
}
