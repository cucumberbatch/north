package org.north.core.system;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.north.core.architecture.ApplicationContext;
import org.north.core.architecture.ComponentHandler;
import org.north.core.architecture.Inject;
import org.north.core.exception.ComponentNotFoundException;
import org.north.core.graphics.Graphics;
import org.north.core.graphics.Material;
import org.north.core.graphics.PredefinedMeshes;
import org.north.core.graphics.Texture;
import org.north.core.graphics.Window;
import org.north.core.graphics.shader.Shader;
import org.north.core.scene.Entity;
import org.north.core.scene.component.MeshRenderer;
import org.north.core.scene.component.Transform;
import org.north.core.scene.component.VisualCursor;
import org.north.core.scene.component.VisualCursorLabel;
import org.north.core.system.process.InitProcess;
import org.north.core.system.process.UpdateProcess;
import org.north.core.utils.BufferUtils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.IntBuffer;

/**
 * Renders the name of the entity currently hovered by the visual cursor.
 *
 * <p>The text is rasterized into a small ARGB texture with Java2D. A texture is rebuilt only when
 * the hovered entity name changes; every frame the label entity is positioned next to the cursor
 * and flipped to the other side when it would leave the window.</p>
 */
@ComponentHandler(VisualCursorLabel.class)
public class VisualCursorLabelSystem extends AbstractSystem<VisualCursorLabel>
        implements InitProcess<VisualCursorLabel>, UpdateProcess<VisualCursorLabel> {

    private static final String SHADER_PATH = "assets/shaders/texture_shader";

    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 6;
    private static final int MAX_TEXT_WIDTH = 260;

    private final Graphics graphics;
    private final Window window;
    private final Font font = new Font("SansSerif", Font.BOLD, 15);

    @Inject
    public VisualCursorLabelSystem(ApplicationContext context) {
        super(VisualCursorLabel.class, context);
        this.graphics = context.getDependency(Graphics.class);
        this.window = context.getDependency(Window.class);
    }

    @Override
    public void init(VisualCursorLabel label) {
        MeshRenderer renderer = cm.get(label.getEntity(), MeshRenderer.class);
        if (renderer == null) {
            throw new ComponentNotFoundException(MeshRenderer.class);
        }

        renderer.mesh = PredefinedMeshes.QUAD;
        renderer.screenSpace = true;
        renderer.renderOrder = label.renderOrder;

        Shader shader = assetService.get(Shader.class, SHADER_PATH);
        renderer.material = new Material(shader);
        renderer.material.setVector("u_color", new Vector4f(1f, 1f, 1f, 1f));

        label.texture = createTransparentTexture();
        label.textureWidth = label.texture.getWidth();
        label.textureHeight = label.texture.getHeight();
        renderer.material.setTexture("u_texture", label.texture);

        // The label is invisible until the cursor hovers an entity.
        renderer.setActive(false);
    }

    @Override
    public void update(VisualCursorLabel label, float deltaTime) {
        MeshRenderer renderer = cm.get(label.getEntity(), MeshRenderer.class);
        if (renderer == null || renderer.material == null) return;

        VisualCursor cursor = label.cursorEntity != null
                ? cm.get(label.cursorEntity, VisualCursor.class)
                : null;

        Entity hovered = cursor != null ? cursor.hoveredEntity : null;
        if (cursor == null || !cursor.visible || hovered == null) {
            renderer.setActive(false);
            return;
        }

        String text = hovered.getName();
        if (text == null || text.isEmpty()) {
            renderer.setActive(false);
            return;
        }

        if (!text.equals(label.currentText) || label.texture == null) {
            replaceTexture(label, renderer, text);
        }

        renderer.setActive(true);
        positionLabel(label, cursor);
    }

    private void replaceTexture(VisualCursorLabel label, MeshRenderer renderer, String text) {
        BufferedImage image = renderText(text);
        Texture newTexture = createTexture(image);

        Texture oldTexture = label.texture;
        renderer.material.setTexture("u_texture", newTexture);

        label.texture = newTexture;
        label.currentText = text;
        label.textureWidth = newTexture.getWidth();
        label.textureHeight = newTexture.getHeight();

        if (oldTexture != null) {
            graphics.deleteTexture(oldTexture.id());
        }
    }

    private BufferedImage renderText(String text) {
        BufferedImage measuringImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measuringGraphics = measuringImage.createGraphics();
        measuringGraphics.setFont(font);
        FontMetrics metrics = measuringGraphics.getFontMetrics();

        String displayText = ellipsize(text, metrics, MAX_TEXT_WIDTH);
        int textWidth = Math.max(1, metrics.stringWidth(displayText));
        int textHeight = Math.max(1, metrics.getAscent() + metrics.getDescent());

        int width = textWidth + PADDING_X * 2;
        int height = textHeight + PADDING_Y * 2;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();

        graphics2D.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        graphics2D.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        graphics2D.setColor(new Color(21, 21, 21, 127));
        graphics2D.fillRect(0, 0, width, height);

        graphics2D.setFont(font);
        graphics2D.setColor(new Color(235, 244, 255, 255));
        graphics2D.drawString(displayText, PADDING_X, PADDING_Y + metrics.getAscent());

        graphics2D.dispose();
        measuringGraphics.dispose();

        return image;
    }

    private Texture createTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        IntBuffer imageBuffer = BufferUtils.createIntBuffer(pixels);
        int id = graphics.createTexture(width, height, imageBuffer, GL11.GL_LINEAR, GL11.GL_LINEAR);

        return new Texture(id, width, height);
    }

    private Texture createTransparentTexture() {
        int[] pixel = new int[]{0x00000000};
        IntBuffer buffer = BufferUtils.createIntBuffer(pixel);
        int id = graphics.createTexture(1, 1, buffer, GL11.GL_LINEAR, GL11.GL_LINEAR);
        return new Texture(id, 1, 1);
    }

    private String ellipsize(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        int end = text.length();

        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }

        return text.substring(0, end) + ellipsis;
    }

    private void positionLabel(VisualCursorLabel label, VisualCursor cursor) {
        Transform transform = cm.get(label.getEntity(), Transform.class);
        if (transform == null) return;

        float labelWidth = Math.max(1f, label.textureWidth);
        float labelHeight = Math.max(1f, label.textureHeight);
        float halfWidth = labelWidth / 2f;
        float halfHeight = labelHeight / 2f;

        float screenWidth = window.getWidth();
        float screenHeight = window.getHeight();
        float margin = label.screenMargin;

        float cursorHalfWidth = Math.max(1f, cursor.displaySize.x) / 2f;
        float cursorHalfHeight = Math.max(1f, cursor.displaySize.y) / 2f;

        float x = cursor.center.x + cursorHalfWidth + label.offset + halfWidth;
        if (x + halfWidth > screenWidth - margin) {
            x = cursor.center.x - cursorHalfWidth - label.offset - halfWidth;
        }

        float y = cursor.center.y - cursorHalfHeight + halfHeight;

        x = clamp(x, halfWidth + margin, screenWidth - halfWidth - margin);
        y = clamp(y, halfHeight + margin, screenHeight - halfHeight - margin);

        transform.moveTo(x, y, 0f);
        transform.rescaleTo(labelWidth, labelHeight, 1f);
    }

    private float clamp(float value, float min, float max) {
        if (max < min) {
            return (min + max) * 0.5f;
        }
        return Math.max(min, Math.min(max, value));
    }

}
