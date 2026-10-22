package net.somewhatcity.boiler.core.display;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.SourceConfig;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.SourceType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.runtime.LatestTaskQueue;
import net.somewhatcity.boiler.core.sources.CancellableLoad;

import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

/** A bounded, generation-scoped child source, sharing the display's loaders and rendering adapters. */
public final class SourceSession implements AutoCloseable {
    private final BoilerPlugin plugin;
    private final IBoilerDisplay display;
    private final Runnable changed;
    private final BiConsumer<String, JsonObject> saved;
    private final SourceGeneration generations = new SourceGeneration();
    private final LatestTaskQueue loads;
    private final ReentrantLock lock = new ReentrantLock();
    private final RasterRenderer raster = new RasterRenderer();
    private final Rectangle viewport;
    private volatile IBoilerSource source;
    private volatile String name;
    private volatile JsonObject data = new JsonObject();
    private volatile Thread loadingThread;
    private volatile IBoilerSource loadingSource;
    private volatile SourceType sourceType;
    private volatile String status = "";
    private boolean closed;

    public SourceSession(BoilerPlugin plugin, IBoilerDisplay display, Runnable changed, BiConsumer<String, JsonObject> saved) {
        this.plugin = plugin; this.display = display; this.changed = changed; this.saved = saved;
        viewport = new Rectangle(display.viewport());
        loads = new LatestTaskQueue(plugin.runtime().loaders());
    }

    public SourceType sourceType() { return sourceType; }
    public String status() { return status; }
    public IBoilerSource source() { return source; }

    /** Called on the server thread; even redirects from a child remain within this session. */
    public void source(String next, JsonObject parameters) {
        if (closed) return;
        Class<? extends IBoilerSource> type = plugin.sourceManager().source(next);
        if (type == null || next.equals("smarttv")) {
            stop();
            status = "Unbekannte oder ungültige Quelle: " + next;
            changed.run();
            return;
        }
        long generation = generations.next();
        cancelLoading();
        retire();
        name = next;
        data = parameters == null ? new JsonObject() : parameters.deepCopy();
        JsonObject scopedData = data;
        status = "Lade " + next + " …";
        if (!(data.has("keepLastSourceData") && data.get("keepLastSourceData").getAsBoolean())) saved.accept(next, data.deepCopy());
        changed.run();
        try {
            loads.submit(() -> load(generation, next, type, scopedData));
        } catch (RejectedExecutionException e) {
            status = "Quellen-Ladewarteschlange voll. Bitte erneut versuchen.";
            changed.run();
        }
    }

    private void load(long generation, String next, Class<? extends IBoilerSource> type, JsonObject scopedData) {
        if (!generations.current(generation)) return;
        IBoilerSource candidate = null;
        loadingThread = Thread.currentThread();
        try {
            candidate = type.getDeclaredConstructor().newInstance();
            loadingSource = candidate;
            if (!generations.current(generation)) return;
            candidate.load(scopedDisplay(generation, next, scopedData), scopedData);
            lock.lock();
            try {
                if (generations.current(generation)) {
                    SourceConfig config = type.getAnnotation(SourceConfig.class);
                    sourceType = config == null ? null : config.sourceType();
                    status = "";
                    source = candidate;
                    candidate = null;
                }
            } finally { lock.unlock(); }
        } catch (Exception e) {
            if (generations.current(generation)) {
                plugin.getLogger().log(Level.WARNING, "Loading SmartTV source " + next, e);
                plugin.onMain(() -> {
                    if (!generations.current(generation)) return;
                    status = "Quelle konnte nicht geladen werden: " + next;
                    display.renderPaused(false);
                });
            }
        } finally {
            unload(candidate);
            loadingSource = null;
            loadingThread = null;
            Thread.interrupted();
            plugin.onMain(() -> { if (generations.current(generation)) changed.run(); });
        }
    }

    private IBoilerDisplay scopedDisplay(long generation, String scopedName, JsonObject scopedData) {
        return (IBoilerDisplay) Proxy.newProxyInstance(IBoilerDisplay.class.getClassLoader(), new Class<?>[]{IBoilerDisplay.class}, (proxy, method, args) -> {
            String operation = method.getName();
            if (operation.equals("sourceData")) return generations.current(generation) ? data : scopedData;
            if (operation.equals("sourceName")) return scopedName;
            if (operation.equals("source") && method.getParameterCount() == 0) return generations.current(generation) ? source : null;
            if (method.getReturnType() == void.class) {
                Runnable invoke = () -> {
                    if (!generations.current(generation)) return;
                    switch (operation) {
                        case "source" -> source((String) args[0], (JsonObject) args[1]);
                        case "saveSourceData" -> {
                            data = ((JsonObject) args[0]).deepCopy();
                            saved.accept(name, data.deepCopy());
                        }
                        case "save" -> saved.accept(name, data.deepCopy());
                        default -> {
                            try { method.invoke(display, args); }
                            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                        }
                    }
                };
                if (operation.equals("render")) invoke.run(); else plugin.onMain(invoke);
                return null;
            }
            try { return method.invoke(display, args); }
            catch (InvocationTargetException e) { throw e.getCause(); }
        });
    }

    private void cancelLoading() {
        IBoilerSource loading = loadingSource;
        if (loading instanceof CancellableLoad cancellable) {
            try { cancellable.cancelLoad(); }
            catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Cancelling SmartTV source load", e); }
        }
        Thread thread = loadingThread;
        if (thread != null) thread.interrupt();
    }

    private void unload(IBoilerSource old) {
        if (old == null) return;
        try { old.unload(); }
        catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Unloading SmartTV source", e); }
    }

    private void retire() {
        lock.lock();
        try {
            IBoilerSource old = source;
            source = null;
            unload(old);
            sourceType = null;
            raster.clear();
        } finally { lock.unlock(); }
        display.renderPaused(false);
        display.viewport(new Rectangle(viewport));
    }

    public void stop() {
        generations.next();
        cancelLoading();
        retire();
        status = "";
        changed.run();
    }

    public void draw(IDrawingSpace space, int width, int height) {
        lock.lock();
        try { if (source != null) raster.draw(source, space, width, height); }
        finally { lock.unlock(); }
    }

    public void draw(Graphics2D graphics, Rectangle viewport) { withSource(s -> s.draw(graphics, viewport)); }
    public void withSource(Consumer<IBoilerSource> action) {
        lock.lock();
        try { if (source != null) action.accept(source); }
        finally { lock.unlock(); }
    }
    public byte[] audio() {
        lock.lock();
        try { return source == null ? null : source.provide20msAudio(); }
        finally { lock.unlock(); }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        generations.close();
        loads.close();
        cancelLoading();
        retire();
    }
}
