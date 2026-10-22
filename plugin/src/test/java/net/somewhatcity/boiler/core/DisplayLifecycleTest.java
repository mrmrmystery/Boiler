package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.ISourceManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.display.ImplBoilerDisplay;
import net.somewhatcity.boiler.core.display.UpdateIntervalManager;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DisplayLifecycleTest {
    private MockedStatic<Bukkit> bukkit;
    private BoilerRuntime runtime;
    private BoilerPlugin previous;
    private MapEngineApi previousEngine;
    private ImplBoilerDisplay display;
    private IMapDisplay map;
    private IDrawingSpace space;
    private UpdateIntervalManager intervals;
    private World world;
    private final ConcurrentLinkedQueue<Runnable> main = new ConcurrentLinkedQueue<>();
    private final Thread testThread = Thread.currentThread();

    @BeforeEach void setup() throws Exception {
        previous = BoilerPlugin.getPlugin();
        previousEngine = BoilerPlugin.MAP_ENGINE;
        runtime = new BoilerRuntime();
        BoilerPlugin plugin = mock(BoilerPlugin.class);
            org.bukkit.Server server = mock(org.bukkit.Server.class);
            when(plugin.getServer()).thenReturn(server);
            when(server.getPluginManager()).thenReturn(mock(org.bukkit.plugin.PluginManager.class));
            when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("BoilerTest"));
            when(plugin.displayManager()).thenReturn(mock(net.somewhatcity.boiler.api.IDisplayManager.class));
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        singleton.set(null, plugin);
        when(plugin.runtime()).thenReturn(runtime);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            if (Thread.currentThread() == testThread) action.run(); else main.add(action);
            return null;
        }).when(plugin).onMain(any());
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        MapEngineApi engine = mock(MapEngineApi.class, RETURNS_DEEP_STUBS);
        BoilerPlugin.MAP_ENGINE = engine;
        map = mock(IMapDisplay.class);
        when(map.pixelWidth()).thenReturn(128);
        when(map.pixelHeight()).thenReturn(128);
        when(engine.displayProvider().createBasic(any(), any(), any())).thenReturn(map);
        space = mock(IDrawingSpace.class, RETURNS_DEEP_STUBS);
        when(engine.pipeline().createDrawingSpace(map)).thenReturn(space);
        intervals = mock(UpdateIntervalManager.class);
        when(plugin.intervalManager()).thenReturn(intervals);
        ISourceManager sources = mock(ISourceManager.class);
        when(plugin.sourceManager()).thenReturn(sources);
        doReturn(PlainSource.class).when(sources).source("plain");
        doReturn(SlowSource.class).when(sources).source("slow");
        world = mock(World.class);
        display = new ImplBoilerDisplay(1, new Location(world, 0, 0, 0), new Location(world, 1, 1, 0), BlockFace.NORTH);
        display.persistent(false);
        SlowSource.entered = new CountDownLatch(1);
        SlowSource.release = new CountDownLatch(1);
        SlowSource.unloads.set(0);
        PlainSource.draws.set(0);
        PlainSource.unloads.set(0);
        PlainSource.blockDraw = false;
    }

    @AfterEach void teardown() throws Exception {
        SlowSource.release.countDown();
        PlainSource.releaseDraw.countDown();
        if (display != null) display.remove();
        runtime.close();
        bukkit.close();
        Field singleton = BoilerPlugin.class.getDeclaredField("plugin");
        singleton.setAccessible(true);
        singleton.set(null, previous);
        BoilerPlugin.MAP_ENGINE = previousEngine;
    }

    private void await(java.util.function.BooleanSupplier condition) throws Exception {
        PerformancePrimitivesTest.await(() -> {
            Runnable action;
            while ((action = main.poll()) != null) action.run();
            return condition.getAsBoolean();
        });
    }
    private Player viewer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        display.viewers().add(player);
        return player;
    }

    @Test void skipsDrawAndFlushWithoutEligibleRecipients() throws Exception {
        display.source("plain", new JsonObject());
        await(() -> display.source() instanceof PlainSource);
        display.render();
        assertEquals(0, PlainSource.draws.get());
        Player player = viewer();
        when(intervals.getInterval(player)).thenReturn(60_000);
        display.render();
        display.render();
        assertEquals(1, PlainSource.draws.get());
        verify(space, times(1)).flush();
        display.renderPaused(true);
        display.render();
        assertEquals(1, PlainSource.draws.get());
    }

    @Test void differentViewerIntervalsAndRejoinAreIndependent() throws Exception {
        display.source("plain", new JsonObject());
        await(() -> display.source() instanceof PlainSource);
        Player slow = viewer(), fast = viewer();
        when(intervals.getInterval(slow)).thenReturn(60_000);
        display.render();
        display.render();
        var receivers = org.mockito.ArgumentCaptor.forClass(java.util.Collection.class);
        verify(space.ctx(), times(2)).receivers(receivers.capture());
        assertEquals(2, receivers.getAllValues().get(0).size());
        assertEquals(java.util.Set.of(fast), receivers.getAllValues().get(1));
        display.viewers().remove(slow);
        display.render();
        display.viewers().add(slow);
        display.render();
        verify(space.ctx(), atLeastOnce()).receivers(java.util.Set.of(slow, fast));
    }

    @Test void rendersNeverOverlapAndUnloadingWaitsForRendering() throws Exception {
        display.source("plain", new JsonObject());
        await(() -> display.source() instanceof PlainSource);
        viewer();
        PlainSource.blockDraw = true;
        PlainSource.enteredDraw = new CountDownLatch(1);
        PlainSource.releaseDraw = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> first = executor.submit(display::render);
            assertTrue(PlainSource.enteredDraw.await(2, TimeUnit.SECONDS));
            executor.submit(display::render).get(2, TimeUnit.SECONDS);
            assertEquals(1, PlainSource.draws.get());
            PlainSource.releaseDraw.countDown();
            first.get(2, TimeUnit.SECONDS);
        }
        display.remove();
        display.render();
        display.remove();
        assertEquals(1, PlainSource.unloads.get());
        verify(map, times(1)).destroy();
    }

    @Test void obsoleteLoadingSourceCannotPublishOrSwitchBackLater() throws Exception {
        display.source("slow", new JsonObject());
        assertTrue(SlowSource.entered.await(2, TimeUnit.SECONDS));
        IBoilerDisplay obsolete = SlowSource.captured;
        display.source("plain", new JsonObject());
        SlowSource.release.countDown();
        await(() -> display.source() instanceof PlainSource);
        assertEquals(1, SlowSource.unloads.get());
        obsolete.source("slow", new JsonObject());
        assertEquals("plain", display.sourceName());
    }

    @Test void removalDuringLoadPreventsPublication() throws Exception {
        display.source("slow", new JsonObject());
        assertTrue(SlowSource.entered.await(2, TimeUnit.SECONDS));
        display.remove();
        SlowSource.release.countDown();
        await(() -> SlowSource.unloads.get() == 1);
        assertNull(display.source());
        display.remove();
        verify(map, times(1)).destroy();
    }

    @Test void changingRenderPeriodReschedulesImmediately() throws Exception {
        display.source("plain", new JsonObject());
        await(() -> display.source() instanceof PlainSource);
        viewer();
        JsonObject settings = new JsonObject();
        settings.addProperty("renderPeriod", 1000);
        display.settings(settings);
        await(() -> PlainSource.draws.get() >= 1);
        int first = PlainSource.draws.get();
        Thread.sleep(60);
        assertEquals(first, PlainSource.draws.get());
        JsonObject faster = new JsonObject();
        faster.addProperty("renderPeriod", 5);
        display.settings(faster);
        await(() -> PlainSource.draws.get() >= first + 4);
        display.renderPaused(true);
    }

    @Test void compositeSourceChangesClientRenderingModeWithoutReplacingTheOuterSource() throws Exception {
        ISourceManager sources = BoilerPlugin.getPlugin().sourceManager();
        doReturn(SwitchingSource.class).when(sources).source("composite");
        SwitchingSource.mode = null;
        display.source("composite", new JsonObject());
        await(() -> display.source() instanceof SwitchingSource);
        IBoilerSource outer = display.source();
        Field type = ImplBoilerDisplay.class.getDeclaredField("sourceType"); type.setAccessible(true);
        SwitchingSource.mode = net.somewhatcity.boiler.api.util.SourceType.CLIENT;
        display.tick();
        assertEquals(net.somewhatcity.boiler.api.util.SourceType.CLIENT, type.get(display));
        SwitchingSource.mode = null;
        display.tick();
        assertNull(type.get(display));
        assertEquals("composite", display.sourceName());
        assertSame(outer, display.source());
    }

    public static class SwitchingSource implements IBoilerSource, net.somewhatcity.boiler.core.display.CompositeSource {
        static volatile net.somewhatcity.boiler.api.util.SourceType mode;
        @Override public void load(IBoilerDisplay display, JsonObject data) {}
        @Override public void unload() {}
        @Override public net.somewhatcity.boiler.api.util.SourceType effectiveSourceType() { return mode; }
    }

    @Test void settingsAppOptionsUseTheExistingPipelineAndPersistence() throws Exception {
        display.source("plain", new JsonObject());
        await(() -> display.source() instanceof PlainSource);
        net.somewhatcity.boiler.core.smarttv.DisplaySetting.DITHER.apply(display, "true");
        net.somewhatcity.boiler.core.smarttv.DisplaySetting.BUFFER.apply(display, "false");
        net.somewhatcity.boiler.core.smarttv.DisplaySetting.BUNDLE.apply(display, "true");
        verify(space.ctx(), atLeastOnce()).converter(de.pianoman911.mapengine.api.util.Converter.FLOYD_STEINBERG);
        verify(space.ctx(), atLeastOnce()).buffering(false);
        verify(space.ctx(), atLeastOnce()).bundling(true);
        verify(BoilerPlugin.getPlugin().displayManager(), atLeastOnce()).saveDisplay(display);
        assertEquals("plain", display.sourceName());
    }

    public static class PlainSource implements IBoilerSource {
        static AtomicInteger draws = new AtomicInteger(), unloads = new AtomicInteger();
        static volatile boolean blockDraw;
        static CountDownLatch enteredDraw = new CountDownLatch(0), releaseDraw = new CountDownLatch(0);
        @Override public void load(IBoilerDisplay display, JsonObject data) {}
        @Override public void unload() { unloads.incrementAndGet(); }
        @Override public void draw(Graphics2D graphics, Rectangle viewport) {
            draws.incrementAndGet();
            if (blockDraw) {
                enteredDraw.countDown();
                try { releaseDraw.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }
    public static class SlowSource implements IBoilerSource {
        static CountDownLatch entered, release;
        static AtomicInteger unloads = new AtomicInteger();
        static volatile IBoilerDisplay captured;
        @Override public void load(IBoilerDisplay display, JsonObject data) {
            captured = display;
            entered.countDown();
            boolean done = false;
            while (!done) {
                try { release.await(); done = true; }
                catch (InterruptedException ignored) { /* emulate native code completing after interruption */ }
            }
        }
        @Override public void unload() { unloads.incrementAndGet(); }
    }
}
