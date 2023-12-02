/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.pianoman911.mapengine.media.converter.MapEngineConverter;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.audio.simplevoicechat.BoilerVoicechatPlugin;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class FFMPEGSource implements IBoilerSource {

    private boolean running;
    private Queue<Short> audioQueue = new ArrayDeque<>();
    private AudioPlayer audioPlayer;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private BufferedImage image;
    private AudioFormat SOURCE_FORMAT = new AudioFormat(48000, 16, 1, true, true);
    private final AudioFormat TARGET_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    private boolean loop = false;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        String streamUrl = data.get("url").getAsString();
        loop = data.get("loop").getAsBoolean();

        VoicechatServerApi serverApi = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi();
        LocationalAudioChannel channel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(),
                serverApi.fromServerLevel(display.cornerA().getWorld()),
                serverApi.createPosition(display.center().getX(), display.center().getY(), display.center().getZ())
        );

        audioPlayer = serverApi.createAudioPlayer(channel, serverApi.createEncoder(), new Supplier<short[]>() {
            @Override
            public short[] get() {
                short[] data = new short[960];
                for(int i = 0; i < 960 && !audioQueue.isEmpty(); i++) {
                    data[i] = audioQueue.poll();
                }
                return data;
            }
        });
        audioPlayer.startPlaying();

        if(channel == null) {
            return;
        }
        channel.setCategory("boiler");
        channel.setDistance(100);

        File file = new File(streamUrl);
        if(!file.exists()) file = null;

        running = true;
        File finalFile = file;
        EXECUTOR.execute(() -> {
            try {
                FFmpegFrameGrabber grabber;
                if(finalFile == null) {
                    grabber = new FFmpegFrameGrabber(new URL(streamUrl));
                } else {
                    grabber = new FFmpegFrameGrabber(finalFile);
                }

                Java2DFrameConverter jconverter = new Java2DFrameConverter();
                MapEngineConverter converter = new MapEngineConverter();

                grabber.start();

                SOURCE_FORMAT = new AudioFormat(grabber.getSampleRate(), 16, grabber.getAudioChannels(), true, true);
                while (running) {
                    try {
                        long start = System.nanoTime();
                        Frame frame = grabber.grabFrame();

                        if(frame == null) {
                            if(loop) {
                                grabber.restart();
                                continue;
                            } else {
                                break;
                            }
                        }

                        if(frame.samples != null) {
                            ShortBuffer channelSamplesShortBuffer = (ShortBuffer) frame.samples[0];
                            channelSamplesShortBuffer.rewind();
                            ByteBuffer outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2);
                            for (int i = 0; i < channelSamplesShortBuffer.capacity(); i++) {
                                short val = channelSamplesShortBuffer.get(i);
                                outBuffer.putShort(val);
                            }
                            byte[] audioData = outBuffer.array();

                            AudioInputStream source = new AudioInputStream(new ByteArrayInputStream(audioData), SOURCE_FORMAT, audioData.length);
                            AudioInputStream converted = AudioSystem.getAudioInputStream(TARGET_FORMAT, source);
                            short[] audio = serverApi.getAudioConverter().bytesToShorts(converted.readAllBytes());

                            for(short s : audio) {
                                audioQueue.add(s);
                            }
                        }

                        if(frame.image != null) {
                            image = jconverter.getBufferedImage(frame);

                            long offset = (long) ((1.0 / grabber.getFrameRate()) * 1000000000);
                            long end = System.nanoTime();
                            long sleep = offset - (end - start);
                            if (sleep > 0) {
                                LockSupport.parkNanos(sleep);
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                JsonObject err = new JsonObject();
                err.addProperty("message", "End of content");
                display.source("error", err);
            } catch (MalformedURLException | FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void unload() {
        running = false;
        //bap.stop();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.drawImage(image, 0, 0, viewport.width, viewport.height, null);
    }

    public static java.util.List<Argument<?>> creationArguments() {
        return List.of(new BooleanArgument("loop"), new GreedyStringArgument("url"));
    }
}
