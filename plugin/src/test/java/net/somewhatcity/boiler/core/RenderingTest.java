package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.display.RasterRenderer;
import net.somewhatcity.boiler.core.sources.KeyboardSource;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RenderingTest {
    static class Source implements IBoilerSource {
        @Override public void load(IBoilerDisplay display, JsonObject data) {}
        @Override public void unload() {}
    }

    @Test void reusedRasterClearsTransparencyAndResetsGraphicsState() {
        RasterRenderer renderer = new RasterRenderer();
        Source first = new Source() {
            @Override public void draw(Graphics2D graphics, Rectangle viewport) {
                graphics.setColor(Color.RED);
                graphics.fillRect(0, 0, 8, 8);
                graphics.translate(100, 100);
                graphics.setClip(0, 0, 1, 1);
                graphics.setComposite(AlphaComposite.Clear);
            }
        };
        BufferedImage reused = renderer.drawImage(first, 8, 8);
        Source second = new Source() {
            @Override public void draw(Graphics2D graphics, Rectangle viewport) {
                graphics.setColor(new Color(0, 0, 255, 128));
                graphics.fillRect(1, 1, 2, 2);
            }
        };
        assertSame(reused, renderer.drawImage(second, 8, 8));
        assertEquals(0, reused.getRGB(0, 0));
        assertEquals(0x800000ff, reused.getRGB(1, 1));
        assertNotSame(reused, renderer.drawImage(second, 16, 8));
    }

    @Test void graphicsOutputMatchesOriginalAdapterAtMultipleScales() {
        BufferedImage input = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        input.setRGB(1, 1, 0x80775533);
        input.setRGB(2, 2, 0xffabcdef);
        Source source = new Source() {
            @Override public void draw(Graphics2D graphics, Rectangle viewport) {
                graphics.drawImage(input, 0, 0, viewport.width, viewport.height, null);
            }
        };
        RasterRenderer renderer = new RasterRenderer();
        for (int width : new int[]{4, 12, 128}) {
            BufferedImage original = new BufferedImage(width, 9, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = original.createGraphics();
            try { source.draw(graphics, new Rectangle(0, 0, width, 9)); }
            finally { graphics.dispose(); }
            BufferedImage actual = renderer.drawImage(source, width, 9);
            assertArrayEquals(original.getRGB(0, 0, width, 9, null, 0, width), actual.getRGB(0, 0, width, 9, null, 0, width));
        }
    }

    @Test void customDrawingSpaceOverrideIsNeverBypassed() {
        AtomicInteger custom = new AtomicInteger();
        Source source = new Source() {
            @Override public void draw(IDrawingSpace space) { custom.incrementAndGet(); space.pixel(0, 0, 0xffffffff); }
            @Override public void draw(Graphics2D graphics, Rectangle viewport) { fail("Graphics fallback invoked for custom drawing space"); }
        };
        IDrawingSpace space = mock(IDrawingSpace.class);
        new RasterRenderer().draw(source, space, 8, 8);
        assertEquals(1, custom.get());
        verify(space).pixel(0, 0, 0xffffffff);
        verify(space, never()).image(any(), anyInt(), anyInt());
    }

    @Test void legacySourceUsesRasterPath() {
        AtomicInteger draws = new AtomicInteger();
        Source source = new Source() {
            @Override public void draw(Graphics2D graphics, Rectangle viewport) { draws.incrementAndGet(); }
        };
        IDrawingSpace space = mock(IDrawingSpace.class);
        new RasterRenderer().draw(source, space, 8, 8);
        assertEquals(1, draws.get());
        verify(space).clear();
        verify(space).image(any(BufferedImage.class), eq(0), eq(0));
    }

    static class CountingKeyboard extends KeyboardSource {
        int renders;
        @Override public synchronized void renderKeyboard() { renders++; super.renderKeyboard(); }
    }

    @Test void keyboardIdleFramesReuseImageAndKeepIdenticalPixels() throws Exception {
        CountingKeyboard keyboard = keyboard();
        BufferedImage output = new BufferedImage(384, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        try {
            keyboard.draw(graphics, new Rectangle(0, 0, 384, 128));
            int[] first = output.getRGB(0, 0, 384, 128, null, 0, 384);
            for (int i = 0; i < 50; i++) keyboard.draw(graphics, new Rectangle(0, 0, 384, 128));
            assertEquals(1, keyboard.renders);
            assertArrayEquals(first, output.getRGB(0, 0, 384, 128, null, 0, 384));
            keyboard.renderKeyboard();
            keyboard.draw(graphics, new Rectangle(0, 0, 384, 128));
            assertArrayEquals(first, output.getRGB(0, 0, 384, 128, null, 0, 384));
        } finally { graphics.dispose(); }
    }

    static CountingKeyboard keyboard() throws Exception {
        CountingKeyboard keyboard = new CountingKeyboard();
        Field field = KeyboardSource.class.getDeclaredField("json");
        field.setAccessible(true);
        try (var stream = RenderingTest.class.getResourceAsStream("/assets/keyboards/kb_layout_de.json")) {
            assertNotNull(stream);
            field.set(keyboard, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        keyboard.loadFromJson();
        return keyboard;
    }
}
