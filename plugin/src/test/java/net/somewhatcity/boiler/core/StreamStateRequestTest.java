package net.somewhatcity.boiler.core;

import com.google.common.io.ByteStreams;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.network.StreamStateRequestListener;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StreamStateRequestTest {
    @Test void requestNeverUsesBlockingHttpAndReplyKeepsWireFormat() throws Exception {
        BoilerPlugin previous = BoilerPlugin.getPlugin();
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        try {
            BoilerPlugin plugin = mock(BoilerPlugin.class);
            singleton.set(null, plugin);
            MediaMtx media = mock(MediaMtx.class);
            when(plugin.mediaMtx()).thenReturn(media);
            IDisplayManager manager = mock(IDisplayManager.class);
            when(plugin.displayManager()).thenReturn(manager);
            when(manager.display(42)).thenReturn(mock(IBoilerDisplay.class));
            CompletableFuture<MediaMtx.StreamState> response = new CompletableFuture<>();
            when(media.getStreamStateAsync("live/display_42")).thenReturn(response);
            ArrayDeque<Runnable> main = new ArrayDeque<>();
            doAnswer(invocation -> { main.add(invocation.getArgument(0)); return null; }).when(plugin).onMain(any());
            Player player = mock(Player.class);
            when(player.isOnline()).thenReturn(true);
            var message = ByteStreams.newDataOutput();
            message.writeInt(42);
            new StreamStateRequestListener().onPluginMessageReceived("boiler:stream_state_request", player, message.toByteArray());
            verify(media, never()).getStreamState(any());
            verify(player, never()).sendPluginMessage(any(), anyString(), any());
            response.complete(MediaMtx.StreamState.ONLINE);
            assertEquals(1, main.size());
            main.remove().run();
            var bytes = org.mockito.ArgumentCaptor.forClass(byte[].class);
            verify(player).sendPluginMessage(eq(plugin), eq("boiler:stream_state_response"), bytes.capture());
            assertEquals(8, bytes.getValue().length);
            var decoded = ByteStreams.newDataInput(bytes.getValue());
            assertEquals(42, decoded.readInt());
            assertEquals(MediaMtx.StreamState.ONLINE.ordinal(), decoded.readInt());
        } finally { singleton.set(null, previous); }
    }
}
