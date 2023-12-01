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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.audio.simplevoicechat.BoilerVoicechatPlugin;
import org.bukkit.Location;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SVCAudioPlayer extends BoilerAudioPlayer {

    private LocationalAudioChannel audioChannel;
    private Queue<Short> audioQueue = new ArrayDeque<>();
    private AudioPlayer lavaAudioPlayer;
    private Timer audioTimer;
    public SVCAudioPlayer(Location location, int range) {
        super(location, range);

        VoicechatServerApi serverApi = (VoicechatServerApi) BoilerVoicechatPlugin.voicechatApi();
        audioChannel = serverApi.createLocationalAudioChannel(
                UUID.randomUUID(),
                serverApi.fromServerLevel(location.getWorld()),
                serverApi.createPosition(location.getX(), location.getY(), location.getZ())
        );

        lavaAudioPlayer = BoilerPlugin.getPlugin().getAudioPlayerManager().createPlayer();

        audioTimer = new Timer();
    }

    @Override
    public void queue(short data) {
        audioQueue.add(data);
    }

    @Override
    public void play(String url) {
        audioTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    AudioFrame frame = lavaAudioPlayer.provide(20, TimeUnit.MILLISECONDS);
                    audioChannel.send(frame.getData());
                } catch (TimeoutException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, 20);
        BoilerPlugin.getPlugin().getAudioPlayerManager().loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                lavaAudioPlayer.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                lavaAudioPlayer.playTrack(playlist.getSelectedTrack());
            }

            @Override
            public void noMatches() {

            }

            @Override
            public void loadFailed(FriendlyException exception) {

            }
        });
    }

    @Override
    public void stop() {
        audioTimer.cancel();
        lavaAudioPlayer.stopTrack();
    }

    @Override
    public void pause() {
        lavaAudioPlayer.setPaused(true);
    }

    @Override
    public void resume() {
        lavaAudioPlayer.setPaused(false);
    }

    @Override
    public long position() {
        return lavaAudioPlayer.getPlayingTrack().getPosition();
    }
}
