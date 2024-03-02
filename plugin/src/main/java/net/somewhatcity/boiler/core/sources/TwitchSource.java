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
import com.google.gson.JsonParser;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.pianoman911.mapengine.media.converter.MapEngineConverter;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.audio.simplevoicechat.BoilerVoicechatPlugin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

public class TwitchSource implements IBoilerSource {
    private boolean running;
    private Queue<Short> audioQueue = new ArrayDeque<>();
    private AudioPlayer audioPlayer;
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private BufferedImage image;
    private AudioFormat SOURCE_FORMAT = new AudioFormat(48000, 16, 1, true, true);
    private final AudioFormat TARGET_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);

    static {
        avutil.av_log_set_level(avutil.AV_LOG_QUIET);
    }
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://pwn.sh/tools/streamapi.py?url=%s".formatted(data.get("url").getAsString()))
                .get()
                .build();

        running = true;
        try(Response response = client.newCall(request).execute()) {
            JsonObject obj = (JsonObject) JsonParser.parseString(response.body().string());
            String streamUrl = obj.getAsJsonObject("urls").get("480p").getAsString();
            JsonObject load = new JsonObject();
            load.addProperty("url", streamUrl);
            load.addProperty("buffer", 100);
            display.source("ffmpeg-buffered", load);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void unload() {
        running = false;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.drawImage(image, 0, 0, viewport.width, viewport.height, null);
    }

    public static java.util.List<Argument<?>> creationArguments() {
        return List.of(new GreedyStringArgument("url"));
    }
}
