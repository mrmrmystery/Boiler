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

import su.plo.voice.api.addon.AddonInitializer;
import su.plo.voice.api.addon.AddonLoaderScope;
import su.plo.voice.api.addon.InjectPlasmoVoice;
import su.plo.voice.api.addon.annotation.Addon;
import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.line.ServerSourceLine;

@Addon(
        id = "boiler",
        name = "Boiler Audio Addon",
        version = "1.0.0",
        authors = "mrmrmystery",
        scope = AddonLoaderScope.SERVER
)
public final class BoilerPlasmoAddon implements AddonInitializer {

    @InjectPlasmoVoice
    private PlasmoVoiceServer voiceServer;
    private static Object addon;
    private static PlasmoVoiceServer plasmoVoiceServer;
    @Override
    public void onAddonInitialize() {
        addon = this;
        plasmoVoiceServer = voiceServer;

        ServerSourceLine sourceLine = voiceServer.getSourceLineManager().createBuilder(
                BoilerPlasmoAddon.addon(),
                "boiler",
                "boiler",
                "plasmovoice:textures/icons/speaker_priority.png",
                10
        ).build();
    }

    @Override
    public void onAddonShutdown() {
        AddonInitializer.super.onAddonShutdown();
    }

    public static PlasmoVoiceServer plasmoVoiceServer() {
        return plasmoVoiceServer;
    }

    public static Object addon() {
        return addon;
    }
}
