package org.north.core.asset.loader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.north.core.graphics.Mesh;
import org.north.core.io.ResourceLoader;
import org.north.core.physics.spatial.Aabb;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;

class ObjMeshAssetLoaderTest {

    @Test
    void loadsPolygonFacesUvCoordinatesAndBounds() {
        String obj = String.join("\n",
                "v 0 0 0",
                "v 2 0 0",
                "v 2 3 0",
                "v 0 3 0",
                "vt 0 0",
                "vt 1 0",
                "vt 1 1",
                "vt 0 1",
                "f 1/1 2/2 3/3 4/4"
        );

        Mesh mesh = load(obj);

        Assertions.assertArrayEquals(new float[]{
                0, 0, 0,
                2, 0, 0,
                2, 3, 0,
                0, 3, 0
        }, mesh.vertices);
        Assertions.assertArrayEquals(new float[]{
                0, 0,
                1, 0,
                1, 1,
                0, 1
        }, mesh.uv);
        Assertions.assertArrayEquals(new int[]{
                0, 1, 2,
                0, 2, 3
        }, mesh.indices);

        Aabb bounds = mesh.getBounds();
        Assertions.assertEquals(0f, bounds.xMin(), 0.0001f);
        Assertions.assertEquals(2f, bounds.xMax(), 0.0001f);
        Assertions.assertEquals(0f, bounds.yMin(), 0.0001f);
        Assertions.assertEquals(3f, bounds.yMax(), 0.0001f);
        Assertions.assertEquals(0f, bounds.zMin(), 0.0001f);
        Assertions.assertEquals(0f, bounds.zMax(), 0.0001f);
    }

    @Test
    void supportsNegativeFaceIndicesAndMissingTextureCoordinates() {
        String obj = String.join("\n",
                "v 0 0 0",
                "v 1 0 0",
                "v 0 1 0",
                "f -3 -2 -1"
        );

        Mesh mesh = load(obj);

        Assertions.assertArrayEquals(new int[]{0, 1, 2}, mesh.indices);
        Assertions.assertArrayEquals(new float[]{
                0, 0,
                0, 0,
                0, 0
        }, mesh.uv);
    }

    private Mesh load(String obj) {
        ResourceLoader resourceLoader = new StringResourceLoader(obj);
        return new ObjMeshAssetLoader(resourceLoader, null).get("test.obj", null);
    }

    private static final class StringResourceLoader implements ResourceLoader {
        private final byte[] content;

        private StringResourceLoader(String content) {
            this.content = content.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public InputStream loadAsStream(String path) {
            return new ByteArrayInputStream(content);
        }

        @Override
        public String loadAsString(String path) {
            return new String(content, StandardCharsets.UTF_8);
        }

        @Override
        public URL getResourceUrl(String path) {
            return null;
        }

        @Override
        public Iterator<URL> getAllResourceUrl(String path) {
            return Collections.emptyIterator();
        }
    }
}
