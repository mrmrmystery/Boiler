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

    private Thread playbackThread;
    private BAudioPlayer bap;
    private BufferedImage currentImage;
    private PlayBin playbin;
    private JPanel panel = new JPanel();
    private Timer timer;
    private long lastInteraction;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        if(Platform.isWindows() || Platform.isMac()) GstreamerUtils.configurePaths();

        Gst.init("boiler_display_" + display.id());
        bap = new BAudioPlayer(display);

        Dimension dimension = new Dimension(display.width(), display.height());

        panel.setSize(dimension);
        panel.setMinimumSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setPreferredSize(dimension);
        panel.setLayout(null);
        panel.setBackground(new Color(0, 0, 0, 0));

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new FlowLayout(FlowLayout.LEFT));

        CButton playButton = new CButton("play");
        playButton.addActionListener(e -> playbin.play());
        playButton.setBounds(10, display.height() - 40, 50, 30);
        panel.add(playButton);


        CButton pauseButton = new CButton("pause");
        pauseButton.addActionListener(e -> playbin.pause());
        pauseButton.setBounds(70, display.height() - 40, 50, 30);
        panel.add(pauseButton);

        CToggleButton loopButton = new CToggleButton("loop", true);
        loopButton.setBounds(130, display.height() - 40, 50, 30);
        panel.add(loopButton);


        CSlider position = new CSlider(0, 1000, 0);
        position.addChangeListener(e -> {
            long dur = playbin.queryDuration(Format.TIME);
            if (dur > 0) {
                double relPos = position.getValue() / 1000.0;
                playbin.seekSimple(Format.TIME,
                        EnumSet.of(SeekFlags.FLUSH),
                        (long) (relPos * dur));
            }
        });
        position.setBounds(190, display.height() - 40, display.width() - 200, 30);
        panel.add(position);

        timer = new Timer(500, e -> {
            if (!position.getValueIsAdjusting()) {
                if(playbin != null) {
                    long dur = playbin.queryDuration(Format.TIME);
                    long pos = playbin.queryPosition(Format.TIME);

                    if (dur > 0) {
                        double relPos = (double) pos / dur;
                        position.setValue((int) (relPos * 1000));
                    }
                }
            }
        });
        timer.start();

        panel.doLayout();
        panel.validate();

        playbackThread = new Thread(() -> {
            AppSink videoSink = new AppSink("GstVideoComponent");
            videoSink.set("emit-signals", true);
            videoSink.setCaps(Caps.fromString("video/x-raw,format=BGRx"));
            videoSink.connect(new AppSink.NEW_SAMPLE() {
                @Override
                public FlowReturn newSample(AppSink appSink) {
                    Sample sample = appSink.pullSample();
                    Structure capsStruct = sample.getCaps().getStructure(0);

                    int width = capsStruct.getInteger("width");
                    int height = capsStruct.getInteger("height");

                    Buffer buffer = sample.getBuffer();
                    ByteBuffer bb = buffer.map(false);

                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            int index = y * width * 4 + x * 4;
                            int b = bb.get(index) & 0xFF;
                            int g = bb.get(index + 1) & 0xFF;
                            int r = bb.get(index + 2) & 0xFF;
                            int rgb = (r << 16) | (g << 8) | b;
                            img.setRGB(x, y, rgb);
                        }
                    }

                    currentImage = img;

                    buffer.unmap();
                    sample.dispose();

                    return FlowReturn.OK;
                }
            });

            AppSink audioSink = new AppSink("GstAudioComponent");
            audioSink.set("emit-signals", true);
            audioSink.setCaps(Caps.fromString("audio/x-raw,format=S16LE,channels=1,rate=48000"));
            audioSink.connect(new AppSink.NEW_SAMPLE() {
                @Override
                public FlowReturn newSample(AppSink appSink) {
                    Sample sample = appSink.pullSample();
                    Buffer buffer = sample.getBuffer();
                    ByteBuffer bb = buffer.map(false);

                    byte[] audioData = new byte[bb.remaining()];
                    bb.get(audioData);

                    bap.play(audioData);

                    buffer.unmap();
                    sample.dispose();

                    return FlowReturn.OK;
                }
            });

            playbin = new PlayBin("playbin");
            playbin.setVideoSink(videoSink);
            playbin.setAudioSink(audioSink);

            playbin.getBus().connect((Bus.EOS) source -> {
                EventQueue.invokeLater(() -> {
                    if (loopButton.isSelected()) {
                        playbin.seekSimple(Format.TIME,
                                EnumSet.of(SeekFlags.FLUSH),
                                0);
                    } else {
                        playbin.stop();
                        position.setValue(0);
                    }
                });
            });

            try {
                playbin.setURI(new URI(data.get("url").getAsString()));
                playbin.play();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            Gst.main();

        });
        playbackThread.start();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.drawImage(currentImage, 0, 0, viewport.width, viewport.height, null);

        if(System.currentTimeMillis() - lastInteraction < 5000) {
            panel.paint(g2);
        }
    }

    @Override
    public void onClick(CommandSender sender, int x, int y, boolean right) {
        lastInteraction = System.currentTimeMillis();

        Component clicked = panel.getComponentAt(x, y);


        if(clicked instanceof JButton button) {
            button.doClick(100);
        } else if(clicked instanceof JCheckBox checkBox) {
            checkBox.doClick(100);
        } else if(clicked instanceof JRadioButton radioButton) {
            radioButton.doClick(100);
        } else if(clicked instanceof JToggleButton toggleButton) {
            toggleButton.doClick(100);
        } else if(clicked instanceof JSlider slider) {
            int clickPos = x - slider.getBounds().x - 7;
            int sliderWidth = slider.getBounds().width - 14;
            slider.setValue((int) Util.map(clickPos, 0, sliderWidth, 0, slider.getMaximum()));
        }
    }

    @Override
    public void unload() {

        if(playbin != null) {
            playbin.stop();
            playbin.dispose();
        }
        if(bap != null) bap.stop();
        if(timer != null) timer.stop();

        if(playbackThread != null) playbackThread.interrupt();
    }

    public static class CButton extends JButton {
        private boolean pressed = false;
        public CButton(String text) {
            super(text);
            addActionListener(e -> {
                pressed = true;
                new java.util.Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        pressed = false;
                    }
                }, 100);
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
