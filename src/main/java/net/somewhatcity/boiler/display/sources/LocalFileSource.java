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
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

public class LocalFileSource extends Source {

    private BufferedImage image;
    private boolean grabberRunning = false;
    private long nextFrameTime;

    @Override
    public void load(LoadedMapDisplay loadedMapDisplay, IMapDisplay display, JsonObject data) {
        String filePath = data.get("file").getAsString();
        File file = Boiler.getPlugin().getDataFolder().toPath().resolve("media/" + filePath).toFile();

        if(file.getName().endsWith(".mp4")) {
            try {
                loadVideo(file);
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void loadVideo(File file) throws FFmpegFrameGrabber.Exception {
        FFmpegLogCallback.setLevel(avutil.AV_LOG_QUIET);
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(file);
        grabber.setFormat("mp4");

        Java2DFrameConverter converter = new Java2DFrameConverter();
        grabber.start();

        double frameRate = grabber.getFrameRate();
        double frameTime = 1000 / frameRate;
        nextFrameTime = System.currentTimeMillis() + (long) frameTime;

        grabberRunning = true;

        new Thread(() -> {
            while (grabberRunning) {
                try {
                    BufferedImage frame = converter.convert(grabber.grabImage());
                    if (frame != null) {
                        image = frame;
                    } else {
                        grabberRunning = false;
                    }
                    long currentTime = System.currentTimeMillis();
                    long delay = Math.max(0, nextFrameTime - currentTime);
                    Thread.sleep(delay);
                    nextFrameTime += frameTime;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void unload() {
        grabberRunning = false;
    }

    @Override
    public BufferedImage getFrame() {
        return image;
    }
}
