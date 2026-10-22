package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.SourceConfig;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.SourceType;
import net.somewhatcity.boiler.core.display.SourceSession;
import net.somewhatcity.boiler.core.sources.CloneSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SourceSessionTest {
    private SourceTestEnvironment env;
    private SourceSession session;
    private final Map<String, JsonObject> saved = new ConcurrentHashMap<>();

    @BeforeEach void setup() throws Exception {
        env = new SourceTestEnvironment();
        env.register("leaf", Leaf.class); env.register("redirect", Redirect.class);
        env.register("slow", Slow.class); env.register("broken", Broken.class);
        env.register("client", Client.class); env.register("clone", CloneSource.class);
        session = new SourceSession(env.plugin, env.display, () -> {}, saved::put);
        Leaf.unloads.set(0); Leaf.draws.set(0);
        Slow.entered = new CountDownLatch(1); Slow.release = new CountDownLatch(1); Slow.unloads.set(0);
        Broken.unloads.set(0);
    }
    @AfterEach void teardown() throws Exception { Slow.release.countDown(); session.close(); env.close(); }

    @Test void redirectsStayInChildAndSeeTheirOwnData() throws Exception {
        JsonObject data = new JsonObject(); data.addProperty("url", "original");
        session.source("redirect", data);
        env.await(() -> session.source() instanceof Leaf);
        Leaf leaf = (Leaf) session.source();
        assertEquals("leaf", leaf.display.sourceName());
        assertEquals("resolved", leaf.display.sourceData().get("url").getAsString());
        assertSame(leaf, leaf.display.source());
        verify(env.display, never()).source(anyString(), any());
        JsonObject update = new JsonObject(); update.addProperty("width", 128);
        leaf.display.saveSourceData(update);
        assertEquals(128, saved.get("leaf").get("width").getAsInt());
        assertFalse(saved.get("redirect").has("width"));
    }

    @Test void obsoleteCallbacksCannotSwitchOrSaveAfterStop() throws Exception {
        session.source("leaf", new JsonObject()); env.await(() -> session.source() instanceof Leaf);
        IBoilerDisplay old = ((Leaf) session.source()).display;
        session.stop();
        assertEquals(1, Leaf.unloads.get());
        old.source("client", new JsonObject());
        JsonObject stale = new JsonObject(); stale.addProperty("stale", true);
        old.saveSourceData(stale); old.render();
        env.drain();
        assertNull(session.source()); assertNull(session.sourceType());
        assertFalse(saved.get("leaf").has("stale"));
        verify(env.display, never()).render();
        session.close(); assertEquals(1, Leaf.unloads.get());
    }

    @Test void cancelledLoadCompletingLateIsUnloadedOnceAndDoesNotReplaceNewSource() throws Exception {
        session.source("slow", new JsonObject());
        assertTrue(Slow.entered.await(2, TimeUnit.SECONDS));
        session.source("leaf", new JsonObject());
        Slow.release.countDown();
        env.await(() -> session.source() instanceof Leaf && Slow.unloads.get() == 1);
        Slow.captured.source("client", new JsonObject());
        env.drain();
        assertInstanceOf(Leaf.class, session.source());
        assertEquals(1, Slow.unloads.get());
    }

    @Test void errorsRemainRecoverableAndPartialCandidateIsUnloaded() throws Exception {
        session.source("broken", new JsonObject());
        env.await(() -> Broken.unloads.get() == 1);
        env.await(() -> session.status().contains("konnte nicht geladen"));
        assertNull(session.source());
        session.stop();
        session.source("leaf", new JsonObject()); env.await(() -> session.source() instanceof Leaf);
    }

    @Test void customDrawingAndClientModeArePreserved() throws Exception {
        session.source("client", new JsonObject()); env.await(() -> session.source() instanceof Client);
        assertEquals(SourceType.CLIENT, session.sourceType());
        IDrawingSpace space = mock(IDrawingSpace.class);
        session.draw(space, 128, 128);
        assertEquals(1, Leaf.draws.get());
        session.stop(); assertNull(session.sourceType());
    }

    @Test void cloneIsDetachedAndRenderingRestoredOnReturn() throws Exception {
        IBoilerDisplay target = mock(IBoilerDisplay.class, RETURNS_DEEP_STUBS);
        when(target.id()).thenReturn(2); when(target.width()).thenReturn(128); when(target.height()).thenReturn(128);
        when(env.plugin.displayManager().display(2)).thenReturn(target);
        JsonObject args = new JsonObject(); args.addProperty("id", 2);
        session.source("clone", args);
        env.await(() -> session.source() instanceof CloneSource);
        verify(env.display.mapDisplay()).cloneGroupIds(target.mapDisplay());
        verify(env.display).renderPaused(true);
        session.stop();
        verify(env.display.mapDisplay()).cutOffCloneGroupIds();
        session.close();
        verify(env.display.mapDisplay(), times(1)).cutOffCloneGroupIds();
        verify(env.display, atLeastOnce()).renderPaused(false);
        verify(env.display, atLeastOnce()).viewport(new Rectangle(0, 0, 128, 128));
    }

    public static class Leaf implements IBoilerSource {
        static final AtomicInteger unloads = new AtomicInteger(), draws = new AtomicInteger();
        IBoilerDisplay display;
        @Override public void load(IBoilerDisplay display, JsonObject data) { this.display = display; }
        @Override public void unload() { unloads.incrementAndGet(); }
        @Override public void draw(IDrawingSpace space) { draws.incrementAndGet(); }
    }
    public static class Redirect extends Leaf {
        @Override public void load(IBoilerDisplay display, JsonObject data) {
            JsonObject resolved = new JsonObject(); resolved.addProperty("url", "resolved"); display.source("leaf", resolved);
        }
    }
    @SourceConfig(sourceType = SourceType.CLIENT)
    public static class Client extends Leaf {}
    public static class Broken extends Leaf {
        static final AtomicInteger unloads = new AtomicInteger();
        @Override public void load(IBoilerDisplay display, JsonObject data) { throw new IllegalStateException("test load failure"); }
        @Override public void unload() { unloads.incrementAndGet(); }
    }
    public static class Slow extends Leaf {
        static CountDownLatch entered, release;
        static final AtomicInteger unloads = new AtomicInteger();
        static volatile IBoilerDisplay captured;
        @Override public void load(IBoilerDisplay display, JsonObject data) {
            captured = display; entered.countDown();
            boolean waiting = true;
            while (waiting) try { release.await(); waiting = false; } catch (InterruptedException ignored) { }
        }
        @Override public void unload() { unloads.incrementAndGet(); }
    }
}
