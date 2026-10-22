/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import org.bukkit.command.CommandSender;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwingTest implements IBoilerSource {
    private JPanel panel;
    private final Object frameLock = new Object();
    private final AtomicBoolean capturePending = new AtomicBoolean();
    private volatile boolean stopped;
    private boolean layoutDirty = true; // EDT only
    private BufferedImage frame;
    private Point lastClick;

    @Override public void load(IBoilerDisplay display, JsonObject data) {
        try {
            EventQueue.invokeAndWait(() -> {
                if (stopped) return;
                panel = new DSPPipelinePanel();
                panel.setSize(display.width(), display.height());
                watch(panel);
                capture();
            });
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private void watch(Component component) {
        component.addPropertyChangeListener(event -> {
            String name = event.getPropertyName();
            if ("font".equals(name) || "preferredSize".equals(name) || "visible".equals(name)) layoutDirty = true;
        });
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) watch(child);
            container.addContainerListener(new ContainerAdapter() {
                @Override public void componentAdded(ContainerEvent event) { watch(event.getChild()); layoutDirty = true; }
                @Override public void componentRemoved(ContainerEvent event) { layoutDirty = true; }
            });
        }
    }
    private void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) if (child instanceof Container nested) layout(nested);
    }
    private void capture() {
        if (stopped) return;
        if (layoutDirty) { layout(panel); layoutDirty = false; }
        synchronized (frameLock) {
            if (frame == null || frame.getWidth() != panel.getWidth() || frame.getHeight() != panel.getHeight())
                frame = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = frame.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, frame.getWidth(), frame.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                panel.printAll(graphics);
            } finally { graphics.dispose(); }
        }
    }
    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        if (!stopped && capturePending.compareAndSet(false, true)) EventQueue.invokeLater(() -> {
            try { capture(); }
            finally { capturePending.set(false); }
        });
        synchronized (frameLock) { graphics.drawImage(frame, 0, 0, null); }
    }
    @Override public void unload() {
        stopped = true;
        synchronized (frameLock) { frame = null; }
    }
    @Override public void onClick(CommandSender sender, int x, int y, boolean right) {
        EventQueue.invokeLater(() -> {
            if (stopped) return;
            lastClick = new Point(x, y);
            Component clicked = panel.getComponentAt(x, y);
            if (clicked instanceof AbstractButton button) button.doClick(0);
        });
    }
    @Override public void onInput(CommandSender sender, String input) {
        EventQueue.invokeLater(() -> {
            if (!stopped && lastClick != null && panel.getComponentAt(lastClick) instanceof JTextField field) field.setText(input);
        });
    }
    @Override public void onKey(CommandSender sender, String key) {
        EventQueue.invokeLater(() -> {
            if (!stopped && lastClick != null && panel.getComponentAt(lastClick) instanceof JTextField field) field.setText(field.getText() + key);
        });
    }
}
