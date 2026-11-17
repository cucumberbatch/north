package io.github.cucumberbatch.north.asset.loader;

import io.github.cucumberbatch.north.asset.AssetService;
import io.github.cucumberbatch.north.graphics.Graphics;
import io.github.cucumberbatch.north.graphics.Texture;
import io.github.cucumberbatch.north.memory.BufferUtils;
import io.github.cucumberbatch.north.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;

public class TextureAssetLoader extends AssetLoader<Texture> {
    private static final Logger log = LoggerFactory.getLogger(TextureAssetLoader.class);
    private final ResourceLoader resourceLoader;
    private final Graphics graphics;

    public TextureAssetLoader(ResourceLoader resourceLoader, Graphics graphics) {
        this.resourceLoader = resourceLoader;
        this.graphics = graphics;
    }

    @Override
    protected Texture load(String assetPath, AssetService assetService) {
        int[] pixels;
        int width;
        int height;

        try (InputStream stream = resourceLoader.loadAsStream(assetPath)) {
            BufferedImage image = ImageIO.read(stream);
            width = image.getWidth();
            height = image.getHeight();
            pixels = new int[width * height];
            image.getRGB(0, 0, width, height, pixels, 0, width);
        } catch (FileNotFoundException e) {
            log.error("Texture '{}' not found!", assetPath, e);
            return null;
        } catch (SecurityException e) {
            log.error("Texture file access denied on path '{}'!", assetPath, e);
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Incorrect texture file path '{}'!", assetPath, e);
            return null;
        } catch (IOException e) {
            log.error("Error while loading texture '{}'!", assetPath, e);
            return null;
        }

        int totalPixelCount = width * height;

        for (int i = 0; i < totalPixelCount / 2; i++) {
            int reversedIndex = totalPixelCount - i - 1;
            int temp = pixels[reversedIndex];
            pixels[reversedIndex] = pixels[i];
            pixels[i] = temp;
        }

        IntBuffer imageBuffer = BufferUtils.createIntBuffer(pixels);
        int id = graphics.createTexture(width, height, imageBuffer);

        return new Texture(id, width, height);
    }

    @Override
    protected void unload(Texture texture) {
        graphics.deleteTexture(texture.id());
    }
}
