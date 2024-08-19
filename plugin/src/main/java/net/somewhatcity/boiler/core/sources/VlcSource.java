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
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import net.kyori.adventure.text.Component;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.audio.BAudioPlayer;
import org.bukkit.Bukkit;
import uk.co.caprica.vlcj.binding.lib.LibVlc;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.media.AudioTrackInfo;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.callback.AudioCallbackAdapter;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class VlcSource implements IBoilerSource {

    private BufferedImage image;
    private int width = 720;
    private int height = 480;
    private final int[] rgbBuffer = new int[width * height];

    private EmbeddedMediaPlayer mediaPlayer;
    private BAudioPlayer bap;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {

        bap = new BAudioPlayer(display);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setAccelerationPriority(1.0f);

        MediaPlayerFactory factory = new MediaPlayerFactory();

         

        mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        mediaPlayer.audio().callback("", 48000, 1, new BoilerVlcAudioCallback());
        mediaPlayer.videoSurface().set(factory.videoSurfaces().newVideoSurface(new BoilerVlcBufferFormatCallback(), new BoilerVlcRenderCallback(), true));

        mediaPlayer.media().play(data.get("url").getAsString());

    }

    @Override
    public void unload() {
        if(mediaPlayer != null) mediaPlayer.release();
        if(bap != null) bap.stop();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.drawImage(image, 0, 0, viewport.width, viewport.height, null);
    }

    private final class BoilerVlcAudioCallback extends AudioCallbackAdapter {

        @Override
        public void play(MediaPlayer mediaPlayer, Pointer samples, int sampleCount, long pts) {
            byte[] audioData = samples.getByteArray(0, sampleCount * 2);
            bap.play(audioData);
        }
    }

    private final class BoilerVlcRenderCallback implements RenderCallback {

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] byteBuffers, BufferFormat bufferFormat) {
            ByteBuffer bb = byteBuffers[0];
            IntBuffer ib = bb.asIntBuffer();
            ib.get(rgbBuffer);
            image.setRGB(0, 0, width, height, rgbBuffer, 0, width);
        }
    }

    private final class BoilerVlcBufferFormatCallback extends BufferFormatCallbackAdapter {

        @Override
        public BufferFormat getBufferFormat(int i, int i1) {
            return new RV32BufferFormat(width, height);
        }
    }
}
