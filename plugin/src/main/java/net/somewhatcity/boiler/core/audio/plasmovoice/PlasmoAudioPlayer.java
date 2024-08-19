/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.audio.plasmovoice;

import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.audio.IBoilerAudioPlayer;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.server.position.ServerPos3d;
import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.audio.codec.AudioEncoder;
import su.plo.voice.api.audio.codec.CodecException;
import su.plo.voice.api.encryption.Encryption;
import su.plo.voice.api.encryption.EncryptionException;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerSourceLine;
import su.plo.voice.api.server.audio.provider.AudioFrameProvider;
import su.plo.voice.api.server.audio.provider.AudioFrameResult;
import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerStaticSource;

import java.util.ArrayDeque;
import java.util.Queue;

public class PlasmoAudioPlayer implements IBoilerAudioPlayer {

    private PlasmoVoiceServer voiceServer;
    private ServerStaticSource source;
    private AudioSender audioSender;
    private boolean started = false;
    private boolean stop = false;
    private Queue<Short> audioQueue;
    @Override
    public void create(IBoilerDisplay display) {
        audioQueue = new ArrayDeque<>();
        voiceServer = BoilerPlasmoAddon.plasmoVoiceServer();

        ServerSourceLine sourceLine = voiceServer.getSourceLineManager()
                .getLineByName("boiler")
                .orElseThrow(() -> new IllegalStateException("Proximity source line not found"));

        Location loc = display.center().toCenterLocation();
        McServerWorld world = voiceServer.getMinecraftServer().getWorld(loc.getWorld());

        ServerPos3d position = new ServerPos3d(world, loc.getX(), loc.getY(), loc.getZ());
        source = sourceLine.createStaticSource(position, false);
        source.setIconVisible(false);

        int audioDistance = 100;
        if(display.settings().has("soundDistance")) {
            audioDistance = display.settings().get("soundDistance").getAsInt();
        }

        BoilerPlasmoAudioFrameProvider frameProvider = new BoilerPlasmoAudioFrameProvider();
        audioSender = source.createAudioSender(frameProvider,  (short) audioDistance);

        audioSender.onStop(() -> {
            source.remove();
        });
    }

    @Override
    public void play(byte[] samples) {
        short[] audio = bytesToShorts(samples);
        for(short s : audio) {
            queue(s);
        }

        if(!started) {
            audioSender.start();
            started = true;
        }
    }

    @Override
    public void destroy() {
        stop = true;
        source.remove();
        audioSender.stop();
    }

    @Override
    public int getAudioQueueSize() {
        return audioQueue.size();
    }

    public void queue(short data) {
        audioQueue.add(data);
    };

    public static short[] bytesToShorts(byte[] bytes) {
        if (bytes.length % 2 != 0) {
            throw new IllegalArgumentException("Input bytes need to be divisible by 2");
        }
        short[] data = new short[bytes.length / 2];
        for (int i = 0; i < bytes.length; i += 2) {
            data[i / 2] = bytesToShort(bytes[i], bytes[i + 1]);
        }
        return data;
    }

    public static short bytesToShort(byte b1, byte b2) {
        return (short) (((b2 & 0xFF) << 8) | (b1 & 0xFF));
    }

    public class BoilerPlasmoAudioFrameProvider implements AudioFrameProvider {

        private AudioEncoder encoder;
        private Encryption encryption;
        public BoilerPlasmoAudioFrameProvider() {
            encoder = voiceServer.createOpusEncoder(false); // false means mono
            encryption = voiceServer.getDefaultEncryption();
        }
        @NotNull
        @Override
        public AudioFrameResult provide20ms() {
            if(stop) {
                encoder.close();
                return AudioFrameResult.Finished.INSTANCE;
            }

            if(audioQueue.size() < 960) {
                return new AudioFrameResult.Provided(null);
            }

            short[] data = new short[960];
            short lastData = 0;
            for(int i = 0; i < 960 && !audioQueue.isEmpty(); i++) {
                data[i] = Short.MIN_VALUE;
                Object o = audioQueue.poll();
                if(o != null) {
                    data[i] = (short) o;
                }
            }

            try {
                byte[] encodedFrame = encoder.encode(data);
                byte[] encryptedFrame = encryption.encrypt(encodedFrame);

                return new AudioFrameResult.Provided(encryptedFrame);
            } catch (EncryptionException | CodecException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
