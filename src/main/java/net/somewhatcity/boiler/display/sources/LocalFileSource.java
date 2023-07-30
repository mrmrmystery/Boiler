/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.pianoman911.mapengine.media.movingimages.FFmpegFrameSource;
import de.pianoman911.mapengine.media.movingimages.MovingImagePlayer;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.BoilerVoicechatPlugin;
import net.somewhatcity.boiler.api.BoilerCreateCommand;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.api.ui.BoilerUI;
import net.somewhatcity.boiler.api.ui.components.BCheckBox;
import net.somewhatcity.boiler.api.ui.components.BFrame;
import net.somewhatcity.boiler.api.ui.components.BImage;
import net.somewhatcity.boiler.api.ui.components.BSlider;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.util.BoilerAudioPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LocalFileSource implements BoilerSource {

    private static int width = 1280;
    private static int height = 720;
    private LoadedMapDisplay lmd;
    private JsonObject jsonData;


    private boolean videoGrabberRunning = false;
    private int currentFrame = 0;

    private Thread videoDecoderThread;
    private Timer videoTimer;
    private Timer videoControlTimer;
    private BoilerAudioPlayer bap;

    private BoilerUI ui;
    private BImage videoImage;
    private BSlider slider;
    private BCheckBox loop;

    private HashMap<Integer, BufferedImage> videoCache = new HashMap<>();


    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        lmd = display;
        jsonData = data;

        width = display.getMapDisplay().pixelWidth();
        height = display.getMapDisplay().pixelHeight();



        ui = new BoilerUI(display);
        BFrame frame = ui.getFrame();


        videoImage = new BImage(0, 0, width, height, null);
        frame.add(videoImage);

        BFrame videoControls = new BFrame(0, height - 50, width, 50);
        videoControls.backgroundColor = new Color(0, 0, 0, 0.5F);

        frame.addClickListener(player -> {
            videoControls.visible = true;
            if(videoControlTimer != null) {
                videoControlTimer.cancel();
            }
            videoControlTimer = new Timer();
            videoControlTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    videoControls.visible = false;
                }
            }, 5000);
        });
        videoControls.visible = false;

        slider = new BSlider(10, 10, width - 20, 10);
        slider.min = 0;
        slider.max = 100;
        slider.val = 0;
        videoControls.add(slider);

        loop = new BCheckBox(10, 30, "Loop");
        if(data.has("loop")) {
            loop.checked = data.get("loop").getAsBoolean();
        }

        loop.addClickListener(player -> {
            jsonData.addProperty("loop", loop.checked);
            MapDisplayManager.saveMapDisplayData(display.getId(), jsonData.toString());
        });
        videoControls.add(loop);

        frame.add(videoControls);

        String filePath = data.get("file").getAsString();
        File file = Boiler.getPlugin().getDataFolder().toPath().resolve("media/" + filePath).toFile();

        if(file.getName().endsWith(".mp4")) {
            try {
                loadVideo(file);
                bap = new BoilerAudioPlayer(display.getCenter(), file);
                bap.addListener(() -> {
                    if(loop.checked) {
                        MapDisplayManager.setSource(display.getId(), "file", jsonData.toString());
                    }
                });
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void unload() {
        if(bap != null) bap.stop();
        if(videoTimer != null) videoTimer.cancel();
        if(videoDecoderThread != null) videoDecoderThread.interrupt();
        videoGrabberRunning = false;
        videoCache.clear();
        ui.close();
    }

    @Override
    public BufferedImage image() {
        return ui.getImage();
    }

    @BoilerCreateCommand
    public static List<Argument<?>> command() {
        return List.of(new GreedyStringArgument("file"));
    }

    public void loadVideo(File file) throws FFmpegFrameGrabber.Exception {
        FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        grabber.setFormat("mp4");
        grabber.setFrameRate(30);
        grabber.start();



        slider.max = grabber.getLengthInFrames();

        int frameRate = (int) grabber.getFrameRate();

        videoDecoderThread = new Thread(() -> {
            try {
                Java2DFrameConverter converter = new Java2DFrameConverter();
                videoGrabberRunning = true;
                while (videoGrabberRunning) {

                    if(videoCache.size() >= grabber.getLengthInFrames()) {
                        videoGrabberRunning = false;
                        break;
                    }

                    if(videoCache.size() - currentFrame > 20) {
                        Thread.sleep(10);
                        continue;
                    }

                    Frame frame = grabber.grabImage();
                    if(frame == null) {
                        videoGrabberRunning = false;
                        break;
                    }
                    BufferedImage img = converter.convert(frame);
                    BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = canvas.createGraphics();
                    g.drawImage(img, 0, 0, width, height, null);
                    if(Boiler.DEBUG) {
                        g.drawString("rendered Frame: %s/%s".formatted(videoCache.size(), grabber.getLengthInFrames()), 1, 10);
                    }
                    g.dispose();

                    videoCache.put(videoCache.size(), canvas);

                }
                grabber.stop();
                grabber.flush();



            } catch (FFmpegFrameGrabber.Exception e) {

            } catch (FrameGrabber.Exception e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        videoDecoderThread.setDaemon(true);
        videoDecoderThread.start();


        videoTimer = new Timer();
        videoTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if(bap == null) return;
                    currentFrame = (int) (bap.getPosition() / 1000.0 * frameRate);

                    if(Boiler.DEBUG) {
                        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = canvas.createGraphics();
                        g.drawImage(videoCache.get(currentFrame), 0, 0, width, height, null);

                        g.setColor(Color.RED);
                        g.drawString("displayed Frame: %s/%s".formatted(currentFrame, videoCache.size()), 0, 20);
                        g.setColor(Color.GREEN);
                        g.drawString("ahead: %s".formatted(videoCache.size() - currentFrame), 0, 30);
                        g.setColor(Color.MAGENTA);
                        g.drawString("audio: %s".formatted(bap.getPosition()), 0, 40);

                        g.dispose();

                        //renderedImage = canvas;
                        videoImage.image = canvas;
                    } else {
                        //renderedImage = videoCache.get(currentFrame);
                        videoImage.image = videoCache.get(currentFrame);
                    }

                    if(currentFrame > 0) videoCache.put(currentFrame - 1, null);
                    slider.val = currentFrame;

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 20);
    }

    @Override
    public void onclick(int x, int y, Player player) {
        ui.handleClick(x, y, player);
    }
}
