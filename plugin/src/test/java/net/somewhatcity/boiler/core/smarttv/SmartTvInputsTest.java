package net.somewhatcity.boiler.core.smarttv;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartTvInputsTest {
    private SmartTvInputs inputs;
    private final ConcurrentLinkedQueue<Runnable> main = new ConcurrentLinkedQueue<>();

    @BeforeEach void setup() {
        BoilerPlugin plugin = mock(BoilerPlugin.class);
        Server server = mock(Server.class); BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(org.bukkit.scheduler.BukkitTask.class));
        doAnswer(invocation -> { main.add(invocation.getArgument(0)); return null; }).when(plugin).onMain(any());
        inputs = new SmartTvInputs(plugin);
    }
    @AfterEach void teardown() { inputs.close(); }
    private Player player() {
        Player player = mock(Player.class); when(player.getUniqueId()).thenReturn(UUID.randomUUID()); when(player.isOnline()).thenReturn(true); return player;
    }
    private AsyncChatEvent chat(Player player, String text) {
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player); when(event.message()).thenReturn(Component.text(text)); return event;
    }
    private void drain() { Runnable task; while ((task = main.poll()) != null) task.run(); }

    @Test void capturesOnlyOwnersTextAndDeliversOnServerThread() throws Exception {
        Object owner = new Object(); Player player = player(), other = player();
        AtomicReference<String> value = new AtomicReference<>();
        assertTrue(inputs.request(owner, player, value::set, () -> {}));
        assertFalse(inputs.request(new Object(), player, value::set, () -> {}));
        AsyncChatEvent normal = chat(other, "public"); inputs.chat(normal); verify(normal, never()).setCancelled(true);
        AsyncChatEvent privateText = chat(player, "private URL");
        Thread worker = new Thread(() -> inputs.chat(privateText)); worker.start(); worker.join(2000);
        verify(privateText).setCancelled(true); assertNull(value.get());
        drain(); assertEquals("private URL", value.get());
    }

    @Test void queuedChatCannotBeDeliveredToAReplacementPrompt() {
        Player player = player(); Object first = new Object();
        AtomicInteger oldCalls = new AtomicInteger(), newCalls = new AtomicInteger();
        inputs.request(first, player, input -> oldCalls.incrementAndGet(), () -> {});
        inputs.chat(chat(player, "stale"));
        inputs.release(first);
        inputs.request(new Object(), player, input -> newCalls.incrementAndGet(), () -> {});
        drain(); assertEquals(0, oldCalls.get()); assertEquals(0, newCalls.get());
        inputs.chat(chat(player, "fresh")); drain(); assertEquals(1, newCalls.get());
    }

    @Test void timeoutAndCancellationReleaseExactlyOnceWithoutStealingOtherSessions() {
        Player first = player(), second = player(); Object owner = new Object();
        AtomicInteger cancelled = new AtomicInteger();
        inputs.request(owner, first, input -> {}, cancelled::incrementAndGet);
        inputs.request(new Object(), second, input -> {}, cancelled::incrementAndGet);
        inputs.cancelPlayer(first); inputs.cancelPlayer(first); assertEquals(1, cancelled.get());
        inputs.expire(Long.MAX_VALUE); inputs.expire(Long.MAX_VALUE); assertEquals(2, cancelled.get());
        AsyncChatEvent normal = chat(second, "public"); inputs.chat(normal); verify(normal, never()).setCancelled(true);
    }
}
