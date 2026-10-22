/*
 * Copyright (c) 2023-2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.audio.simplevoicechat;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerConfig;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.audio.PcmBuffer;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SvcAudioPlayer {
    public static final VoicechatServerApi API = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi();
    private final PcmBuffer audioQueue;
    private final List<LocationalAudioChannel> channels = new ArrayList<>();
    private final short[] block = new short[960];
    private OpusEncoder encoder;
    private ScheduledFuture<?> playbackTask;
    private volatile long position;
    private boolean stopped;

    public SvcAudioPlayer(IBoilerDisplay display) { this(display, false); }
    public SvcAudioPlayer(IBoilerDisplay display, boolean live) {
        audioQueue = new PcmBuffer(live ? 24_000 : 48_000, live);
        if (API == null) { audioQueue.close(); stopped = true; return; }
        List<Location> speakers = display.speakers();
        if (speakers.isEmpty()) speakers = List.of(display.center());
        for (Location speaker : speakers) {
            LocationalAudioChannel channel = API.createLocationalAudioChannel(UUID.randomUUID(),
                    API.fromServerLevel(speaker.getWorld()), API.createPosition(speaker.getX(), speaker.getY(), speaker.getZ()));
            channel.setCategory(BoilerConfig.svcChannelName);
            channel.setDistance(display.settings().has("soundDistance") ? display.settings().get("soundDistance").getAsInt() : 100);
            channels.add(channel);
        }
        encoder = API.createEncoder();
        playbackTask = BoilerPlugin.getPlugin().runtime().audio().scheduleAtFixedRate(this::playBlock, 0, 20, TimeUnit.MILLISECONDS);
    }

    private synchronized void playBlock() {
        if (stopped) return;
        try {
            audioQueue.read(block);
            byte[] encoded = encoder.encode(block);
            for (LocationalAudioChannel channel : channels) channel.send(encoded);
            position += 20;
        } catch (Exception e) {
            BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Audio playback stopped", e);
            stop();
        }
    }

    public void queue(short sample) { write(new short[]{sample}); }
    public void queue(byte[] samples) {
        if (API != null) write(API.getAudioConverter().bytesToShorts(samples));
    }
    private void write(short[] samples) {
        try { audioQueue.write(samples); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    public int getAudioQueueSize() { return audioQueue.size(); }
    public void clear() { audioQueue.clear(); }
    public void stop() {
        // Wake producers before acquiring the encoder lock.
        audioQueue.close();
        synchronized (this) {
            if (stopped) return;
            stopped = true;
            if (playbackTask != null) playbackTask.cancel(false);
            if (encoder != null) encoder.close();
            channels.clear();
        }
    }
    public long getPosition() { return position; }
}
