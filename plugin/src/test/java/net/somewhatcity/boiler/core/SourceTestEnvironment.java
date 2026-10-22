package net.somewhatcity.boiler.core;

import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.display.BoilerSourceManager;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import net.somewhatcity.boiler.core.smarttv.SmartTvInputs;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Assertions;
import org.mockito.MockedStatic;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Runs callbacks from source workers on the test's server thread, as production does. */
final class SourceTestEnvironment implements AutoCloseable {
    final BoilerPlugin plugin = mock(BoilerPlugin.class);
    final BoilerRuntime runtime = new BoilerRuntime();
    final IBoilerDisplay display = mock(IBoilerDisplay.class, RETURNS_DEEP_STUBS);
    final BoilerSourceManager registry = new BoilerSourceManager(plugin);
    final SmartTvInputs inputs;
    private final Thread main = Thread.currentThread();
    private final ConcurrentLinkedQueue<Runnable> pending = new ConcurrentLinkedQueue<>();
    private final BoilerPlugin previous;
    private final MockedStatic<Bukkit> bukkit;

    SourceTestEnvironment() throws Exception {
        previous = BoilerPlugin.getPlugin();
        singleton().set(null, plugin);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(java.util.List.of());
        when(plugin.runtime()).thenReturn(runtime);
        when(plugin.sourceManager()).thenReturn(registry);
        when(plugin.displayManager()).thenReturn(mock(IDisplayManager.class));
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("SmartTvTest"));
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mock(org.bukkit.scheduler.BukkitTask.class));
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            if (Thread.currentThread() == main) action.run(); else pending.add(action);
            return null;
        }).when(plugin).onMain(any());
        when(plugin.callOnMain(any())).thenAnswer(invocation -> {
            Callable<?> action = invocation.getArgument(0);
            if (Thread.currentThread() == main) return action.call();
            FutureTask<?> result = new FutureTask<>(action);
            pending.add(result);
            return result.get(3, TimeUnit.SECONDS);
        });
        when(display.id()).thenReturn(1);
        when(display.width()).thenReturn(128);
        when(display.height()).thenReturn(128);
        when(display.viewport()).thenReturn(new Rectangle(0, 0, 128, 128));
        when(display.facing()).thenReturn(org.bukkit.block.BlockFace.NORTH);
        AtomicReference<com.google.gson.JsonObject> settings = new AtomicReference<>(new com.google.gson.JsonObject());
        when(display.settings()).thenAnswer(invocation -> settings.get());
        doAnswer(invocation -> { settings.set(invocation.getArgument(0)); return null; }).when(display).settings(any(com.google.gson.JsonObject.class));
        inputs = new SmartTvInputs(plugin);
        when(plugin.smartTvInputs()).thenReturn(inputs);
    }

    Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        return player;
    }

    void register(String name, Class<? extends IBoilerSource> source) { registry.register(name, source); }

    void drain() {
        Runnable action;
        while ((action = pending.poll()) != null) action.run();
    }

    void await(BooleanSupplier ready) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        do {
            drain();
            if (ready.getAsBoolean()) return;
            Thread.sleep(5);
        } while (System.nanoTime() < deadline);
        Assertions.fail("Timed out waiting for source worker");
    }

    private static Field singleton() throws Exception {
        Field field = BoilerPlugin.class.getDeclaredField("plugin"); field.setAccessible(true); return field;
    }

    @Override public void close() throws Exception {
        inputs.close();
        runtime.close();
        drain();
        bukkit.close();
        singleton().set(null, previous);
    }
}
