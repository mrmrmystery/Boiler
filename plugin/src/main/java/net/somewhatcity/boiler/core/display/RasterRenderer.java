package net.somewhatcity.boiler.core.display;

import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/** Used only under the display's render lock; custom drawing-space overrides retain dispatch. */
public final class RasterRenderer {
    private static final ClassValue<Boolean> LEGACY = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod("draw", IDrawingSpace.class).getDeclaringClass() == IBoilerSource.class; }
            catch (NoSuchMethodException e) { throw new IllegalStateException(e); }
        }
    };
    private BufferedImage image;
    private int[] pixels;

    public void draw(IBoilerSource source, IDrawingSpace space, int width, int height) {
        if (!LEGACY.get(source.getClass())) {
            source.draw(space);
            return;
        }
        space.clear();
        space.image(drawImage(source, width, height), 0, 0);
    }

    public BufferedImage drawImage(IBoilerSource source, int width, int height) {
        if (image == null || image.getWidth() != width || image.getHeight() != height) {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        } else Arrays.fill(pixels, 0);
        Graphics2D graphics = image.createGraphics();
        try {
            source.draw(graphics, new Rectangle(0, 0, width, height));
        } finally { graphics.dispose(); }
        return image;
    }

    public void clear() { image = null; pixels = null; }
}
