package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.ISourceManager;
import net.somewhatcity.boiler.core.display.ImplDisplayManager;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DisplayPersistenceTest {
    @Test void savesRoundTripWithoutRewritingDuringHydrationAndClosePreservesFiles(@TempDir Path directory) throws Exception {
        withManager(directory, fixture -> {
            var display = fixture.manager.createDisplay(fixture.a, fixture.b, BlockFace.NORTH);
            JsonObject settings = new JsonObject();
            settings.addProperty("renderPeriod", 75);
            settings.addProperty("dither", true);
            display.settings(settings);
            display.addSpeaker(new Location(fixture.world, 1.25, 2.5, 3.75));
            JsonObject data = new JsonObject();
            data.addProperty("url", "file:fixture.png");
            display.source("plain", data);
            display.save();
            Path file = directory.resolve("displays/display_1.json");
            String saved = Files.readString(file);
            var parsed = JsonParser.parseString(saved).getAsJsonObject();
            assertEquals(75, parsed.getAsJsonObject("settings").get("renderPeriod").getAsInt());
            assertEquals(1.25, parsed.getAsJsonArray("speakers").get(0).getAsJsonObject().get("x").getAsDouble());
            fixture.manager.close();
            assertTrue(Files.exists(file));
            fixture.replaceManager();
            fixture.load.run();
            var restored = fixture.manager.display(1);
            assertNotNull(restored);
            assertEquals("plain", restored.sourceName());
            assertEquals(75, restored.settings().get("renderPeriod").getAsInt());
            assertEquals(1, restored.speakers().size());
            assertEquals(saved, Files.readString(file), "Hydration rewrote persisted data");
        });
    }

    @Test void transientDisplaysHaveUniqueIdsAreIndexedAndNeverSaved(@TempDir Path directory) throws Exception {
        withManager(directory, fixture -> {
            var first = fixture.manager.createDisplay(fixture.a, fixture.b, BlockFace.NORTH, false, false);
            var second = fixture.manager.createDisplay(fixture.a, fixture.b, BlockFace.NORTH, false, false);
            assertNotEquals(first.id(), second.id());
            assertFalse(first.persistent());
            assertSame(first, fixture.manager.display(first.mapDisplay()));
            assertSame(second, fixture.manager.display(second.id()));
            first.save();
            assertFalse(Files.exists(directory.resolve("displays")));
            fixture.manager.removeDisplay(first);
            assertNull(fixture.manager.display(first.mapDisplay()));
            assertEquals(1, fixture.manager.displays().size());
        });
    }

    @Test void visibilityCapturesPlayerLocationOnceForAllDisplaysAndHonorsAutoTick(@TempDir Path directory) throws Exception {
        withManager(directory, fixture -> {
            Player player = mock(Player.class);
            when(player.getLocation()).thenReturn(new Location(fixture.world, 2, 2, 0));
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            fixture.bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));
            for (int i = 0; i < 10; i++) fixture.manager.createDisplay(fixture.a, fixture.b, BlockFace.NORTH, false, true);
            var manual = fixture.manager.createDisplay(fixture.a, fixture.b, BlockFace.NORTH, false, false);
            fixture.tick.run();
            verify(player, times(1)).getLocation();
            verify(manual.mapDisplay(), never()).spawn(any());
            assertTrue(fixture.manager.displays().stream().filter(display -> display.autoTick()).allMatch(display -> display.viewers().contains(player)));
        });
    }

    @FunctionalInterface interface Check { void run(Fixture fixture) throws Exception; }
    private void withManager(Path directory, Check check) throws Exception {
        BoilerPlugin previous = BoilerPlugin.getPlugin();
        MapEngineApi previousEngine = BoilerPlugin.MAP_ENGINE;
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        try (BoilerRuntime runtime = new BoilerRuntime(); MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            BoilerPlugin plugin = mock(BoilerPlugin.class);
            org.bukkit.Server server = mock(org.bukkit.Server.class);
            when(plugin.getServer()).thenReturn(server);
            when(server.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("BoilerTest"));
            when(plugin.displayManager()).thenReturn(mock(net.somewhatcity.boiler.api.IDisplayManager.class));
            singleton.set(null, plugin);
            when(plugin.runtime()).thenReturn(runtime);
            when(plugin.intervalManager()).thenReturn(mock(net.somewhatcity.boiler.core.display.UpdateIntervalManager.class));
            when(plugin.getDataFolder()).thenReturn(directory.toFile());
            when(plugin.getConfig()).thenReturn(new YamlConfiguration());
            doAnswer(invocation -> { ((Runnable) invocation.getArgument(0)).run(); return null; }).when(plugin).onMain(any());
            ISourceManager sources = mock(ISourceManager.class);
            when(plugin.sourceManager()).thenReturn(sources);
            doReturn(DisplayLifecycleTest.PlainSource.class).when(sources).source(anyString());
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            MapEngineApi engine = mock(MapEngineApi.class, RETURNS_DEEP_STUBS);
            BoilerPlugin.MAP_ENGINE = engine;
            when(engine.displayProvider().createBasic(any(), any(), any())).thenAnswer(invocation -> {
                IMapDisplay display = mock(IMapDisplay.class);
                when(display.pixelWidth()).thenReturn(128);
                when(display.pixelHeight()).thenReturn(128);
                return display;
            });
            Fixture fixture = new Fixture();
            fixture.plugin = plugin;
            fixture.bukkit = bukkit;
            fixture.world = mock(World.class);
            when(fixture.world.getUID()).thenReturn(UUID.randomUUID());
            when(fixture.world.getName()).thenReturn("world");
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(fixture.world);
            fixture.a = new Location(fixture.world, 0, 0, 0);
            fixture.b = new Location(fixture.world, 1, 1, 0);
            BukkitScheduler scheduler = mock(BukkitScheduler.class);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
                fixture.load = invocation.getArgument(1);
                return mock(BukkitTask.class);
            });
            when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenAnswer(invocation -> {
                fixture.tick = invocation.getArgument(1);
                return mock(BukkitTask.class);
            });
            fixture.replaceManager();
            try { check.run(fixture); }
            finally { fixture.manager.close(); }
        } finally {
            singleton.set(null, previous);
            BoilerPlugin.MAP_ENGINE = previousEngine;
        }
    }
    static class Fixture {
        BoilerPlugin plugin;
        MockedStatic<Bukkit> bukkit;
        ImplDisplayManager manager;
        World world;
        Location a, b;
        Runnable load, tick;
        void replaceManager() {
            manager = new ImplDisplayManager(plugin);
            when(plugin.displayManager()).thenReturn(manager);
        }
    }
}
