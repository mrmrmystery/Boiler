/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.util;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.somewhatcity.boiler.BoilerVoicechatPlugin;
import net.somewhatcity.boiler.display.MapDisplayManager;
import org.bukkit.Location;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class BoilerAudioPlayer {

    private LocationalAudioChannel channel;
    private AudioPlayer audioPlayer;
    private AudioTrack audioTrack;
    private Timer audioTimer;
    private AudioPlayerManager apm;
    private boolean ended = false;

    private List<TrackEndListener> listeners = new ArrayList<>();

    public BoilerAudioPlayer(Location location, File file) {

        VoicechatServerApi serverApi = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi;
        channel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(),
                serverApi.fromServerLevel(location.getWorld()),
                serverApi.createPosition(location.getX(), location.getY(), location.getZ())
        );

        if(channel == null) {
            return;
        }

        channel.setCategory("boiler");
        channel.setDistance(100);

        apm = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(apm);
        AudioSourceManagers.registerLocalSource(apm);
        audioPlayer = apm.createPlayer();

        String url = file.getPath();

        apm.loadItem(url, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                audioTrack = track;
                audioPlayer.playTrack(audioTrack);
                audioTimer = new Timer();
                audioTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            AudioFrame frame = audioPlayer.provide(20, TimeUnit.MILLISECONDS);
                            if (frame != null) {
                                channel.send(frame.getData());
                            }
                        } catch (Exception ex) {

                        }
                    }
                }, 0, 20);

            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {

            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });

        audioPlayer.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                ended = true;
                listeners.forEach(TrackEndListener::onTrackEnd);
            }
        });
    }

    public boolean hasEnded() {
        return ended;
    }

    public void stop() {
        audioPlayer.stopTrack();
        audioTimer.cancel();
        channel.flush();
    }

    public long getPosition() {
        if(audioPlayer.getPlayingTrack() == null) return 0;
        return audioPlayer.getPlayingTrack().getPosition();
    }

    public void setPaused(boolean paused) {
        audioPlayer.setPaused(paused);
    }

    public void addListener(TrackEndListener listener) {
        listeners.add(listener);
    }

    public interface TrackEndListener {
        void onTrackEnd();
    }
}
