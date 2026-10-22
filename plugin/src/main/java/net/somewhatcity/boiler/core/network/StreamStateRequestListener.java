/*
 * Copyright (c) 2025.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.MediaMtx;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class StreamStateRequestListener implements PluginMessageListener {
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (message.length != Integer.BYTES) return;
        BoilerPlugin plugin = BoilerPlugin.getPlugin();
        if (plugin.stopping() || plugin.mediaMtx() == null) return;
        int displayId = ByteStreams.newDataInput(message).readInt();
        if (plugin.displayManager().display(displayId) == null) return;
        plugin.mediaMtx().getStreamStateAsync("live/display_%s".formatted(displayId)).thenAccept(state ->
                plugin.onMain(() -> {
                    if (!player.isOnline()) return;
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeInt(displayId);
                    out.writeInt(state.ordinal());
                    player.sendPluginMessage(plugin, "boiler:stream_state_response", out.toByteArray());
                }));
    }
}
