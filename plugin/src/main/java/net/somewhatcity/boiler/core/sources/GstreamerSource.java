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
import net.somewhatcity.boiler.core.BoilerPlugin;

import com.google.gson.JsonObject;
import com.sun.jna.Platform;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.api.util.GraphicUtils;
import net.somewhatcity.boiler.core.GstreamerUtils;

import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.audio.BAudioPlayer;
import org.bukkit.command.CommandSender;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.PlayBin;
import org.freedesktop.gstreamer.event.SeekFlags;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.TimerTask;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class GstreamerSource implements IBoilerSource {
    private final Object frameLock = new Object();
    private final Object overlayLock = new Object();
    private volatile boolean stopped;
    private BAudioPlayer bap;
    private BufferedImage currentImage;
    private BufferedImage overlay;
    private PlayBin playbin;
    private AppSink videoSink;
    private AppSink audioSink;
    private JPanel panel;
    private Timer timer;
    private volatile long lastInteraction;
    private boolean updatingPosition;
    private CToggleButton loopButton;
    private CSlider position;

    @Override public void load(IBoilerDisplay display, JsonObject data) {
        if (!Util.ensureGstreamer()) throw new IllegalStateException("GStreamer unavailable");
        bap = BoilerPlugin.getPlugin().callOnMain(() -> new BAudioPlayer(display));
        try { EventQueue.invokeAndWait(() -> setupControls(display)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        videoSink = new AppSink("BoilerVideo-" + display.id());
        videoSink.set("emit-signals", true);
        videoSink.set("max-buffers", 2);
        videoSink.set("drop", true);
        videoSink.setCaps(Caps.fromString("video/x-raw,format=BGRx"));
        videoSink.connect((AppSink.NEW_SAMPLE) sink -> {
            Sample sample = sink.pullSample();
            if (sample == null) return FlowReturn.OK;
            Buffer buffer = null;
            boolean mapped = false;
            try {
                if (stopped) return FlowReturn.OK;
                Structure caps = sample.getCaps().getStructure(0);
                int width = caps.getInteger("width"), height = caps.getInteger("height");
                buffer = sample.getBuffer();
                ByteBuffer bytes = buffer.map(false);
                if (bytes == null) return FlowReturn.OK;
                mapped = true;
                synchronized (frameLock) {
                    if (currentImage == null || currentImage.getWidth() != width || currentImage.getHeight() != height)
                        currentImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    int stride = bytes.remaining() / height;
                    BgrxFrames.copy(bytes, currentImage, stride);
                }
                return FlowReturn.OK;
            } finally {
                if (mapped) buffer.unmap();
                sample.dispose();
            }
        });
        audioSink = new AppSink("BoilerAudio-" + display.id());
        audioSink.set("emit-signals", true);
        audioSink.set("max-buffers", 4);
        audioSink.setCaps(Caps.fromString("audio/x-raw,format=S16LE,channels=1,rate=48000"));
        audioSink.connect((AppSink.NEW_SAMPLE) sink -> {
            Sample sample = sink.pullSample();
            if (sample == null) return FlowReturn.OK;
            Buffer buffer = null;
            boolean mapped = false;
            try {
                if (stopped) return FlowReturn.OK;
                buffer = sample.getBuffer();
                ByteBuffer bytes = buffer.map(false);
                if (bytes == null) return FlowReturn.OK;
                mapped = true;
                byte[] samples = new byte[bytes.remaining()];
                bytes.get(samples);
                bap.play(samples);
                return FlowReturn.OK;
            } finally {
                if (mapped) buffer.unmap();
                sample.dispose();
            }
        });
        playbin = new PlayBin("BoilerPlayer-" + display.id());
        playbin.setVideoSink(videoSink);
        playbin.setAudioSink(audioSink);
        playbin.getBus().connect((Bus.EOS) source -> EventQueue.invokeLater(() -> {
            if (stopped) return;
            if (loopButton.isSelected()) playbin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), 0);
            else { playbin.stop(); updatingPosition = true; position.setValue(0); updatingPosition = false; }
        }));
        try { playbin.setURI(new URI(data.get("url").getAsString())); }
        catch (URISyntaxException e) { throw new IllegalArgumentException(e); }
        playbin.play();
    }

    private void setupControls(IBoilerDisplay display) {
        if (stopped) return;
        panel = new JPanel();
        panel.setSize(display.width(), display.height());
        panel.setLayout(null);
        panel.setOpaque(false);
        CButton play = new CButton("play");
        play.addActionListener(e -> { if (!stopped && playbin != null) playbin.play(); });
        play.setBounds(10, display.height() - 40, 50, 30);
        panel.add(play);
        CButton pause = new CButton("pause");
        pause.addActionListener(e -> { if (!stopped && playbin != null) playbin.pause(); });
        pause.setBounds(70, display.height() - 40, 50, 30);
        panel.add(pause);
        loopButton = new CToggleButton("loop", true);
        loopButton.setBounds(130, display.height() - 40, 50, 30);
        panel.add(loopButton);
        position = new CSlider(0, 1000, 0);
        position.addChangeListener(e -> {
            if (updatingPosition || stopped || playbin == null) return;
            long duration = playbin.queryDuration(Format.TIME);
            if (duration > 0) playbin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH), (long) (position.getValue() / 1000.0 * duration));
        });
        position.setBounds(190, display.height() - 40, display.width() - 200, 30);
        panel.add(position);
        panel.doLayout();
        overlay = new BufferedImage(display.width(), display.height(), BufferedImage.TYPE_INT_ARGB);
        timer = new Timer(100, e -> {
            if (stopped || System.currentTimeMillis() - lastInteraction >= 5000) return;
            if (!position.getValueIsAdjusting() && playbin != null) {
                long duration = playbin.queryDuration(Format.TIME);
                if (duration > 0) {
                    updatingPosition = true;
                    try { position.setValue((int) (playbin.queryPosition(Format.TIME) * 1000.0 / duration)); }
                    finally { updatingPosition = false; }
                }
            }
            paintOverlay();
        });
        timer.start();
    }

    private void paintOverlay() {
        synchronized (overlayLock) {
            Graphics2D graphics = overlay.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, overlay.getWidth(), overlay.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                if (System.currentTimeMillis() - lastInteraction < 5000) panel.paint(graphics);
            } finally { graphics.dispose(); }
        }
    }

    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        synchronized (frameLock) { graphics.drawImage(currentImage, 0, 0, viewport.width, viewport.height, null); }
        synchronized (overlayLock) {
            // The EDT owns Swing; rendering consumes only its raster snapshot.
            if (System.currentTimeMillis() - lastInteraction < 5000) graphics.drawImage(overlay, 0, 0, null);
        }
    }

    @Override public void onClick(CommandSender sender, int x, int y, boolean right) {
        EventQueue.invokeLater(() -> {
            if (stopped) return;
            lastInteraction = System.currentTimeMillis();
            Component clicked = panel.getComponentAt(x, y);
            if (clicked instanceof AbstractButton button) button.doClick(0);
            else if (clicked instanceof JSlider slider) slider.setValue((int) Util.map(x - slider.getX() - 7, 0, slider.getWidth() - 14, 0, slider.getMaximum()));
            paintOverlay();
        });
    }

    @Override public void unload() {
        if (stopped) return;
        stopped = true;
        if (bap != null) bap.stop(); // release blocked native audio callbacks before stopping the pipeline
        EventQueue.invokeLater(() -> { if (timer != null) timer.stop(); });
        if (playbin != null) { playbin.stop(); playbin.dispose(); }
        if (videoSink != null) videoSink.dispose();
        if (audioSink != null) audioSink.dispose();
        synchronized (frameLock) { currentImage = null; }
        synchronized (overlayLock) { overlay = null; }
    }

    public static class CButton extends JButton {
        private boolean pressed = false;
        public CButton(String text) {
            super(text);
            addActionListener(e -> {
                pressed = true;
                javax.swing.Timer reset = new javax.swing.Timer(100, event -> pressed = false);
                reset.setRepeats(false);
                reset.start();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.DARK_GRAY);
            if(pressed) g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(0, 0, getBounds().width, getBounds().height);
            g2.setColor(Color.WHITE);
            GraphicUtils.centeredString(g2, new Rectangle(0, 0, getBounds().width, getBounds().height), getText());
        }
    }

    public static class CToggleButton extends JToggleButton {
        public CToggleButton(String text, boolean selected) {
            super(text, selected);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.DARK_GRAY);
            if(isSelected()) g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(0, 0, getBounds().width, getBounds().height);
            g2.setColor(Color.WHITE);
            GraphicUtils.centeredString(g2, new Rectangle(0, 0, getBounds().width, getBounds().height), getText());
        }
    }

    public static class CSlider extends JSlider {
        public CSlider(int min, int max, int value) {
            super(min, max, value);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getBounds().width, getBounds().height);
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect(7, getBounds().height / 2 - 4, getBounds().width - 14, 8);
            float normalizedValue = Util.map(getValue(), getMinimum(), getMaximum(), 0, 1);
            g2.setColor(Color.WHITE);
            g2.fillRect(7, getBounds().height / 2 - 4, (int) ((getBounds().width - 14) * normalizedValue), 8);
        }
    }
}
