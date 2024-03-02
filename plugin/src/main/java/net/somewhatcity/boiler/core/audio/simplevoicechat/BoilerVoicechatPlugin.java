/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.audio.simplevoicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import net.somewhatcity.boiler.core.BoilerConfig;

public class BoilerVoicechatPlugin implements VoicechatPlugin {

    private static VoicechatApi voicechatApi;
    @Override
    public String getPluginId() {
        return "boiler";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();

        VolumeCategory boiler = api.volumeCategoryBuilder()
                .setId(BoilerConfig.svcChannelName)
                .setName(BoilerConfig.svcChannelName)
                .setDescription("The volume of %s".formatted(BoilerConfig.svcChannelName))
                .build();

        api.registerVolumeCategory(boiler);
    }

    @Override
    public void initialize(VoicechatApi api) {
        voicechatApi = api;
    }

    public static VoicechatApi voicechatApi() {
        return voicechatApi;
    }
}
