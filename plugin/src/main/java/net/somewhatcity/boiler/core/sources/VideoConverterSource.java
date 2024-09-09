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
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.api.util.GraphicUtils;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.awt.*;
import java.io.File;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class VideoConverterSource implements IBoilerSource {

    private boolean running = true;
    private int totalFrames;
    private int currentFrame = 0;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {

        String url = data.get("url").getAsString();
        String output = "./plugins/Boiler/temp/video_%s_%s_%s.mp4".formatted(display.width(), display.height(), url.replace("/", "")
                .replace(":", "")
                .replace(".", "")
                .replace("=", "")
                .replace("&", "")
                .replace("?", "")
                .replace("!", ""));

        File outputFile = new File(output);

        if(outputFile.exists()) {
            JsonObject load = new JsonObject();
            load.addProperty("url", output);
            load.addProperty("buffer", 10);
            load.addProperty("keepLastSourceData", false);
            display.source("ffmpeg-buffered", load);
            return;
        }

        outputFile.getParentFile().mkdirs();

        new Thread(() -> {
            try {
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(url);
                grabber.start();

                totalFrames = grabber.getLengthInFrames();

                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(output, display.width(), display.height(), 1);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_OPUS);
                recorder.setAudioBitrate(96000);
                recorder.setSampleRate(48000);
                recorder.setFormat("mp4");
                recorder.setFrameRate(30);
                recorder.start();

                while (running) {
                    try {
                        Frame frame = grabber.grab();
                        if(frame == null) {
                            break;
                        }
                        recorder.record(frame);
                        if(frame.image != null) currentFrame++;
                    } catch (FFmpegFrameGrabber.Exception | FFmpegFrameRecorder.Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

                recorder.stop();
                grabber.stop();
                recorder.release();
                grabber.release();

                JsonObject load = new JsonObject();
                load.addProperty("url", output);
                load.addProperty("buffer", 10);
                load.addProperty("keepLastSourceData", false);
                display.source("ffmpeg-buffered", load);

            } catch (FFmpegFrameGrabber.Exception | FFmpegFrameRecorder.Exception e) {
                throw new RuntimeException(e);
            }

        }).start();

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, viewport.width, viewport.height);

        g2.setColor(Color.WHITE);
        Rectangle rect = new Rectangle(0, 0, viewport.width, viewport.height);
        GraphicUtils.centeredString(g2, rect, "Converting: %s/%s".formatted(currentFrame, totalFrames));
    }

    @Override
    public void unload() {
        running = false;
    }
}
