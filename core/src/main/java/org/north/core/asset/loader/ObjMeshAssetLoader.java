package org.north.core.asset.loader;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.north.core.asset.AssetService;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Mesh;
import org.north.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads Wavefront OBJ files into {@link Mesh} assets.
 *
 * <p>The loader supports arbitrary triangle and polygon faces, negative indices, and vertex
 * texture coordinates. Normals and material files are intentionally ignored for now; the current
 * rendering pipeline only consumes position and UV attributes. A texture can still be assigned to
 * the resulting mesh through a {@code Material}.</p>
 */
public class ObjMeshAssetLoader extends AssetLoader<Mesh> {
    private final ResourceLoader resourceLoader;
    private final Graphics graphics;

    public ObjMeshAssetLoader(ResourceLoader resourceLoader, Graphics graphics) {
        this.resourceLoader = resourceLoader;
        this.graphics = graphics;
    }

    @Override
    protected Mesh load(String assetPath, AssetService assetService) {
        if (!assetPath.toLowerCase(Locale.ROOT).endsWith(".obj")) {
            throw new IllegalArgumentException(
                    "Only Wavefront OBJ files are supported for meshes: " + assetPath
            );
        }

        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> textureCoordinates = new ArrayList<>();

        List<Float> vertexData = new ArrayList<>();
        List<Float> uvData = new ArrayList<>();
        List<Integer> triangleIndices = new ArrayList<>();
        Map<Long, Integer> vertexCache = new HashMap<>();

        try (InputStream stream = resourceLoader.loadAsStream(assetPath)) {
            if (stream == null) {
                throw new IllegalArgumentException("Mesh resource not found: " + assetPath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    int commentIndex = line.indexOf('#');
                    if (commentIndex >= 0) {
                        line = line.substring(0, commentIndex);
                    }
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    switch (parts[0]) {
                        case "v":
                            parsePosition(parts, positions, assetPath, lineNumber);
                            break;
                        case "vt":
                            parseTextureCoordinate(parts, textureCoordinates, assetPath, lineNumber);
                            break;
                        case "f":
                            parseFace(
                                    parts,
                                    positions,
                                    textureCoordinates,
                                    vertexData,
                                    uvData,
                                    triangleIndices,
                                    vertexCache,
                                    assetPath,
                                    lineNumber
                            );
                            break;
                        default:
                            // Other OBJ records (normals, groups, materials, smoothing) are not used yet.
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load OBJ mesh '" + assetPath + "'", e);
        }

        if (vertexData.isEmpty() || triangleIndices.isEmpty()) {
            throw new IllegalArgumentException("OBJ mesh contains no faces: " + assetPath);
        }

        return new Mesh(
                toFloatArray(vertexData),
                toIntArray(triangleIndices),
                toFloatArray(uvData)
        );
    }

    private void parsePosition(String[] parts, List<Vector3f> positions,
                               String assetPath, int lineNumber) {
        if (parts.length < 4) {
            throw malformed(assetPath, lineNumber, "a vertex must have at least x, y and z");
        }
        try {
            positions.add(new Vector3f(
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3])
            ));
        } catch (NumberFormatException e) {
            throw malformed(assetPath, lineNumber, "invalid vertex position");
        }
    }

    private void parseTextureCoordinate(String[] parts, List<Vector2f> textureCoordinates,
                                        String assetPath, int lineNumber) {
        if (parts.length < 3) {
            throw malformed(assetPath, lineNumber, "a texture coordinate must have u and v");
        }
        try {
            textureCoordinates.add(new Vector2f(
                    Float.parseFloat(parts[1]),
                    Float.parseFloat(parts[2])
            ));
        } catch (NumberFormatException e) {
            throw malformed(assetPath, lineNumber, "invalid texture coordinate");
        }
    }

    private void parseFace(String[] parts,
                           List<Vector3f> positions,
                           List<Vector2f> textureCoordinates,
                           List<Float> vertexData,
                           List<Float> uvData,
                           List<Integer> triangleIndices,
                           Map<Long, Integer> vertexCache,
                           String assetPath,
                           int lineNumber) {
        int faceVertexCount = parts.length - 1;
        if (faceVertexCount < 3) {
            throw malformed(assetPath, lineNumber, "a face must have at least three vertices");
        }

        int[] faceVertices = new int[faceVertexCount];
        for (int i = 0; i < faceVertexCount; i++) {
            String[] vertexParts = parts[i + 1].split("/", -1);

            int positionIndex = parseIndex(
                    vertexParts[0], positions.size(), "vertex", assetPath, lineNumber
            );
            int textureIndex = -1;
            if (vertexParts.length > 1 && !vertexParts[1].isEmpty()) {
                textureIndex = parseIndex(
                        vertexParts[1], textureCoordinates.size(), "texture coordinate", assetPath, lineNumber
                );
            }

            faceVertices[i] = getOrCreateVertex(
                    positionIndex,
                    textureIndex,
                    positions,
                    textureCoordinates,
                    vertexData,
                    uvData,
                    vertexCache
            );
        }

        // OBJ faces may be n-gons. Triangulate them with a simple fan.
        for (int i = 1; i < faceVertexCount - 1; i++) {
            triangleIndices.add(faceVertices[0]);
            triangleIndices.add(faceVertices[i]);
            triangleIndices.add(faceVertices[i + 1]);
        }
    }

    private int getOrCreateVertex(int positionIndex,
                                  int textureIndex,
                                  List<Vector3f> positions,
                                  List<Vector2f> textureCoordinates,
                                  List<Float> vertexData,
                                  List<Float> uvData,
                                  Map<Long, Integer> vertexCache) {
        long key = ((long) positionIndex << 32) | (textureIndex & 0xFFFFFFFFL);
        Integer existing = vertexCache.get(key);
        if (existing != null) {
            return existing;
        }

        int newIndex = vertexData.size() / 3;
        Vector3f position = positions.get(positionIndex);
        vertexData.add(position.x);
        vertexData.add(position.y);
        vertexData.add(position.z);

        if (textureIndex >= 0) {
            Vector2f uv = textureCoordinates.get(textureIndex);
            uvData.add(uv.x);
            uvData.add(uv.y);
        } else {
            uvData.add(0f);
            uvData.add(0f);
        }

        vertexCache.put(key, newIndex);
        return newIndex;
    }

    private int parseIndex(String value, int elementCount, String elementName,
                           String assetPath, int lineNumber) {
        if (value == null || value.isEmpty()) {
            throw malformed(assetPath, lineNumber, "missing " + elementName + " index");
        }

        try {
            int index = Integer.parseInt(value);
            if (index < 0) {
                index = elementCount + index;
            } else {
                index = index - 1;
            }
            if (index < 0 || index >= elementCount) {
                throw malformed(assetPath, lineNumber,
                        elementName + " index " + value + " is out of range");
            }
            return index;
        } catch (NumberFormatException e) {
            throw malformed(assetPath, lineNumber, "invalid " + elementName + " index: " + value);
        }
    }

    private IllegalArgumentException malformed(String assetPath, int lineNumber, String reason) {
        return new IllegalArgumentException(
                "Malformed OBJ '" + assetPath + "' at line " + lineNumber + ": " + reason
        );
    }

    private float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    @Override
    protected void unload(Mesh mesh) {
        if (graphics != null) {
            mesh.unload(graphics);
        }
    }
}
