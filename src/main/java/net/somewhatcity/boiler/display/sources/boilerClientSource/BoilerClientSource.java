/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.display.sources.boilerClientSource;

import com.google.gson.JsonObject;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import net.somewhatcity.boiler.BoilerVoicechatPlugin;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.display.LoadedMapDisplay;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Supplier;

public class BoilerClientSource implements BoilerSource {

    private BoilerServer boilerServer;
    private LocationalAudioChannel channel;
    private AudioPlayer audioPlayer;
    private Queue<short[]> audioQueue;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        audioQueue = new ArrayDeque<>();
        VoicechatServerApi serverApi = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi;
        channel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(),
                serverApi.fromServerLevel(display.getCenter().getWorld()),
                serverApi.createPosition(display.getCenter().getX(), display.getCenter().getY(), display.getCenter().getZ())
        );

        AudioPlayer player = serverApi.createAudioPlayer(channel, serverApi.createEncoder(), new Supplier<short[]>() {
            @Override
            public short[] get() {
                if(audioQueue.isEmpty()) {
                    return new short[960];
                }
                return audioQueue.poll();
            }
        });
        player.startPlaying();

        channel.setCategory("boiler");
        channel.setDistance(100);


        boilerServer = new BoilerServer(this);
    }

    @Override
    public void unload() {
        new Thread(() -> {
            boilerServer.shutdown();
        }).start();
    }

    @Override
    public BufferedImage image() {
        return BoilerServerHandler.getImage();
    }

    public AudioChannel getAudioChannel() {
        return channel;
    }

    public void queueAudio(short[] audio) {
        audioQueue.add(audio);
    }

    public void emptyQueue() {
        audioQueue.clear();
    }
}
