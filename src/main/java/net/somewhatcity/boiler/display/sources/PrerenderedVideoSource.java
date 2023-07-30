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
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.api.BoilerCreateCommand;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.api.ui.BoilerUI;
import net.somewhatcity.boiler.api.ui.components.BFrame;
import net.somewhatcity.boiler.api.ui.components.BImage;
import net.somewhatcity.boiler.api.ui.components.BSlider;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.util.BoilerAudioPlayer;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

public class PrerenderedVideoSource implements BoilerSource {

    private int width;
    private int height;

    private Thread prerenderThread;
    private LoadedMapDisplay display;
    private BoilerUI ui;
    private BoilerAudioPlayer bap;

    private BImage renderPreview;
    private BSlider renderProgress;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        this.display = display;
        width = display.getMapDisplay().pixelWidth();
        height = display.getMapDisplay().pixelHeight();

        ui = new BoilerUI(display);
        BFrame frame = ui.getFrame();

        String filePath = data.get("file").getAsString();
        File file = Boiler.getPlugin().getDataFolder().toPath().resolve("media/" + filePath).toFile();

        File cachedVideos = Boiler.getPlugin().getDataFolder().toPath().resolve("data/cached/").toFile();
        if (!cachedVideos.exists()) cachedVideos.mkdirs();

        File cachedFrames = new File(cachedVideos, file.getName());
        if (!cachedFrames.exists()) {
            cachedFrames.mkdirs();
            renderPreview = new BImage(30, 10, width - 60, height - 100);
            renderProgress = new BSlider(30, height - 60, width - 60, 20);
            frame.add(renderPreview);
            frame.add(renderProgress);
            prerenderVideo(file, cachedFrames);
        } else {
            renderPreview = new BImage(0, 0, width, height);
            frame.add(renderPreview);
            playPrerenderedVideo(file, cachedFrames);
        }
    }

    @Override
    public void unload() {
        ui.close();
        if(bap != null) bap.stop();
    }

    @Override
    public BufferedImage image() {
        return ui.getImage();
    }

    @BoilerCreateCommand
    public static List<Argument<?>> command() {
        return List.of(new GreedyStringArgument("file"));
    }

    public void prerenderVideo(File video, File destination) {
        prerenderThread = new Thread(() -> {

            FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(video);
            Java2DFrameConverter converter = new Java2DFrameConverter();




            try {
                grabber.start();

                File infoFile = new File(destination, "info.txt");
                infoFile.createNewFile();
                Files.writeString(infoFile.toPath(), String.valueOf(grabber.getFrameRate()));

                renderProgress.max = grabber.getLengthInFrames();
                Frame frame;
                while ((frame = grabber.grabFrame()) != null) {
                    BufferedImage image = converter.convert(frame);
                    renderPreview.image = image;
                    renderProgress.val = grabber.getFrameNumber();
                    if(image != null) ImageIO.write(image, "png", new File(destination, grabber.getFrameNumber() + ".png"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        prerenderThread.setDaemon(true);
        prerenderThread.start();
    }

    public void playPrerenderedVideo(File video, File cachedFrames) {
        Thread playThread = new Thread(() -> {
            bap = new BoilerAudioPlayer(display.getCenter(), video);
            File[] files = cachedFrames.listFiles();
            List<File> fileList = new java.util.ArrayList<>(List.of(files));
            File infoFile = new File(cachedFrames, "info.txt");

            HashMap<String, File> fileMap = new HashMap<>();
            for (File file : fileList) {
                fileMap.put(file.getName(), file);
            }

            fileList.remove(infoFile);
            double frameRate = 0;
            try {
                frameRate = Double.parseDouble(Files.readString(infoFile.toPath()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (!bap.hasEnded()) {
                long currentFrame = (int) (bap.getPosition() / 1000.0 * frameRate);
                try {
                    if(fileMap.containsKey(currentFrame + ".png")) {
                        renderPreview.image = ImageIO.read(fileMap.get(currentFrame + ".png"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        playThread.setDaemon(true);
        playThread.start();
    }
}
