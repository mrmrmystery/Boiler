/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.audio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import de.maxhenkel.voicechat.api.packets.*;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerConfig;
import net.somewhatcity.boiler.core.audio.simplevoicechat.BoilerVoicechatPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Pig;
import org.checkerframework.checker.units.qual.A;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BoilerAudioPlayer {

    private static final VoicechatServerApi API = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi();
    private AudioPlayer audioPlayer;
    private long position = 0;
    private Queue<Short> audioQueue;
    public BoilerAudioPlayer(IBoilerDisplay display) {
        audioQueue = new ArrayDeque<>();

        LocationalAudioChannel channel = API.createLocationalAudioChannel(
                UUID.randomUUID(),
                API.fromServerLevel(display.cornerA().getWorld()),
                API.createPosition(display.center().getX(), display.center().getY(), display.center().getZ())
        );
        audioPlayer = API.createAudioPlayer(channel, API.createEncoder(), new Supplier<short[]>() {
            @Override
            public short[] get() {
                position += 20;
                //System.out.println("aq-size: %s".formatted(audioQueue.size()));
                short[] data = new short[960];
                short lastData = 0;
                for(int i = 0; i < 960 && !audioQueue.isEmpty(); i++) {
                    data[i] = Short.MIN_VALUE;
                    Object o = audioQueue.poll();
                    if(o != null) {
                        data[i] = (short) o;
                    }
                }
                return data;
            }
        });
        audioPlayer.startPlaying();

        if(channel == null) {
            return;
        }
        channel.setCategory(BoilerConfig.svcChannelName);
        channel.setDistance(100);
    }
    public void queue(short data) {
        audioQueue.add(data);
    };

    public void queue(byte[] data) {
        short[] audio = API.getAudioConverter().bytesToShorts(data);
        for(short s : audio) {
            queue(s);
        }
    }

    public int getAudioQueueSize() {
        return audioQueue.size();
    }
    public void stop() {
        if(audioPlayer != null) audioPlayer.stopPlaying();
    };

    public long getPosition() {
        return position;
    }
}
