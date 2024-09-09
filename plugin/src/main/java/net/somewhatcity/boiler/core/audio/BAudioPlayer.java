/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.audio;

import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.audio.plasmovoice.PlasmoAudioPlayer;
import net.somewhatcity.boiler.core.audio.simplevoicechat.SvcAudioPlayer;

public class BAudioPlayer {

    private SvcAudioPlayer simpleVCAudioPlayer;
    private PlasmoAudioPlayer plasmoAudioPlayer;
    public BAudioPlayer(IBoilerDisplay display) {

        if(Util.isPluginInstalled("voicechat")) {
            simpleVCAudioPlayer = new SvcAudioPlayer(display);
        }

        if(Util.isPluginInstalled("PlasmoVoice")) {
            plasmoAudioPlayer = new PlasmoAudioPlayer();
            plasmoAudioPlayer.create(display);
        }

    }

    public void play(byte[] samples) {
        if(simpleVCAudioPlayer != null) {
            simpleVCAudioPlayer.queue(samples);
        }

        if(plasmoAudioPlayer != null) {
            plasmoAudioPlayer.play(samples);
        }
    }

    public void stop() {
        if(simpleVCAudioPlayer != null) {
            simpleVCAudioPlayer.stop();
        }

        if(plasmoAudioPlayer != null) {
            plasmoAudioPlayer.destroy();
        }
    }

    public int getAudioQueueSize() {
        if(simpleVCAudioPlayer != null) {
            return simpleVCAudioPlayer.getAudioQueueSize();
        } else if(plasmoAudioPlayer != null) {
            return 900;
        } else {
            return -1;
        }
    }
}
