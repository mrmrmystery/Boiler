/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.display;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.api.util.Converter;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.SourceConfig;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.SourceType;
import net.somewhatcity.boiler.core.BoilerConfig;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class ImplBoilerDisplay implements IBoilerDisplay, Listener {
    private final int ID;
    private final IMapDisplay MAP_DISPLAY;
    private final Location CORNER_A;
    private final Location CORNER_B;
    private final BlockFace FACING;
    private final IDrawingSpace drawingSpace;
    private final RasterRenderer raster = new RasterRenderer();
    private final RasterRenderer sectionRaster = new RasterRenderer();
    private final ReentrantLock renderLock = new ReentrantLock();
    private final SourceGeneration generations = new SourceGeneration();
    private volatile IBoilerSource source;
    private volatile String sourceName;
    private volatile Rectangle viewport;
    private volatile boolean autoTick = true;
    private volatile boolean persistent = true;
    private volatile boolean renderPaused;
    private volatile boolean removed;
    private volatile JsonObject settings = new JsonObject();
    private volatile JsonObject sourceData = new JsonObject();
    private final List<Location> speakers = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastUpdates = new ConcurrentHashMap<>();
    private final Set<Player> receivers = ConcurrentHashMap.newKeySet();
    private volatile SourceType sourceType;
    private volatile int renderPeriod = 20;
    private final RenderLoop renderLoop;
    private final net.somewhatcity.boiler.core.runtime.LatestTaskQueue loads;
    private volatile Thread loadingThread;
    private volatile IBoilerSource loadingSource;

    private void cancelLoading() {
        IBoilerSource loading = loadingSource;
        if (loading instanceof net.somewhatcity.boiler.core.sources.CancellableLoad cancellable) cancellable.cancelLoad();
        Thread thread = loadingThread;
        if (thread != null) thread.interrupt();
    }

    public ImplBoilerDisplay(int id, Location a, Location b, BlockFace facing) {
        ID = id;
        CORNER_A = a;
        CORNER_B = b;
        FACING = facing;
        MAP_DISPLAY = MAP_ENGINE.displayProvider().createBasic(a.toVector().toBlockVector(), b.toVector().toBlockVector(), facing);
        viewport = new Rectangle(0, 0, width(), height());
        drawingSpace = MAP_ENGINE.pipeline().createDrawingSpace(MAP_DISPLAY);
        loads = new net.somewhatcity.boiler.core.runtime.LatestTaskQueue(BoilerPlugin.getPlugin().runtime().loaders());
        renderLoop = new RenderLoop(BoilerPlugin.getPlugin().runtime().render(), () -> {
            try { render(); }
            catch (Exception e) { BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Rendering display " + ID, e); }
        });
        settings.addProperty("buffer", true);
        drawingSpace.ctx().buffering(true);
        BoilerPlugin.getPlugin().getServer().getPluginManager().registerEvents(this, BoilerPlugin.getPlugin());
    }

    private void main(Runnable action) { BoilerPlugin.getPlugin().onMain(action); }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        receivers.remove(event.getPlayer());
        lastUpdates.remove(event.getPlayer().getUniqueId());
    }

    @Override public void tick() {
        if (!Bukkit.isPrimaryThread()) { main(this::tick); return; }
        Map<UUID, List<ImplDisplayManager.ViewerPosition>> players = ImplDisplayManager.viewerPositions();
        tick(players);
    }

    void tick(Map<UUID, List<ImplDisplayManager.ViewerPosition>> players) {
        if (removed) return;
        if (source instanceof CompositeSource composite) {
            SourceType effective = composite.effectiveSourceType();
            if (sourceType != effective) {
                sourceType = effective;
                respawn();
            }
        }
        if (source == null && sourceName == null) source("default", null);
        int distance = settings.has("viewDistance") ? settings.get("viewDistance").getAsInt() : BoilerConfig.viewDistance;
        double squared = (double) distance * distance;
        Set<Player> visible = new HashSet<>();
        for (ImplDisplayManager.ViewerPosition viewer : players.getOrDefault(CORNER_A.getWorld().getUID(), List.of())) {
            double dx = CORNER_A.getX() - viewer.x(), dy = CORNER_A.getY() - viewer.y(), dz = CORNER_A.getZ() - viewer.z();
            if (distance > 0 && dx * dx + dy * dy + dz * dz < squared) visible.add(viewer.player());
        }
        boolean client = BoilerConfig.clientEnabled && sourceType == SourceType.CLIENT;
        for (Player player : new ArrayList<>(receivers)) {
            if (!visible.contains(player) && receivers.remove(player)) {
                if (client) BoilerClientUtil.sendRemoveDisplay(player, this);
                else MAP_DISPLAY.despawn(player);
            }
        }
        for (Player player : visible) {
            if (!receivers.add(player)) continue;
            if (client) {
                String username = "bs_" + player.getName().replace("_", "").toLowerCase(Locale.ROOT);
                String password = UUID.randomUUID().toString().replace("-", "");
                BoilerPlugin.getPlugin().mediaMtxAuthServer().registerUser(username, password);
                String stream = "rtsp://%s:%s@%s:%s/live/display_%s".formatted(username, password, BoilerConfig.rtmpHost, BoilerConfig.rtmpPort, id());
                BoilerClientUtil.sendCreateDisplay(player, this, stream);
            } else {
                MAP_DISPLAY.spawn(player);
                if (settings.has("itemRotation")) MAP_DISPLAY.itemRotation(player, settings.get("itemRotation").getAsInt());
                if (settings.has("visualDirection")) MAP_DISPLAY.visualDirection(player, BlockFace.valueOf(settings.get("visualDirection").getAsString()));
                if (settings.has("rotation")) MAP_DISPLAY.rotation(player, settings.getAsJsonObject("rotation").get("yaw").getAsFloat(), settings.getAsJsonObject("rotation").get("pitch").getAsFloat());
            }
        }
        updateRenderTask();
    }

    private void updateRenderTask() {
        boolean needed = !removed && !renderPaused && !receivers.isEmpty() && sourceType != SourceType.SERVER;
        renderLoop.update(needed, renderPeriod);
    }

    @Override public void render() {
        if (removed || renderPaused || receivers.isEmpty() || !renderLock.tryLock()) return;
        try {
            IBoilerSource current = source;
            if (current == null || removed || renderPaused) return;
            long now = System.nanoTime();
            Set<Player> eligible = new HashSet<>();
            Set<UUID> viewerIds = new HashSet<>();
            for (Player player : receivers) viewerIds.add(player.getUniqueId());
            lastUpdates.keySet().retainAll(viewerIds);
            for (Player player : receivers) {
                int interval = BoilerPlugin.getPlugin().intervalManager().getInterval(player);
                Long last = lastUpdates.get(player.getUniqueId());
                if (interval <= 0 || last == null || now - last >= TimeUnit.MILLISECONDS.toNanos(interval)) eligible.add(player);
            }
            if (eligible.isEmpty()) return;
            drawingSpace.ctx().receivers(eligible);
            raster.draw(current, drawingSpace, width(), height());
            drawingSpace.flush();
            for (Player player : eligible)
                lastUpdates.compute(player.getUniqueId(), (id, previous) -> receivers.contains(player) ? now : null);
        } finally { renderLock.unlock(); }
    }

    public void drawSection(Graphics2D graphics, int x, int y, int width, int height) {
        // Avoid deadlocks from cyclic section clones; do not wait on another renderer.
        if (renderLock.isHeldByCurrentThread() || !renderLock.tryLock()) return;
        try {
            if (source == null || removed) return;
            BufferedImage snapshot = sectionRaster.drawImage(source, width(), height());
            graphics.drawImage(snapshot, 0, 0, width, height, x, y, x + width, y + height, null);
        } finally { renderLock.unlock(); }
    }

    @Override public void respawn() {
        if (!Bukkit.isPrimaryThread()) { main(this::respawn); return; }
        for (Player player : receivers) {
            MAP_DISPLAY.despawn(player);
            if (BoilerConfig.clientEnabled) BoilerClientUtil.sendRemoveDisplay(player, this);
        }
        receivers.clear();
        renderLock.lock();
        try { lastUpdates.clear(); }
        finally { renderLock.unlock(); }
        updateRenderTask();
    }

    @Override public void remove() {
        if (!Bukkit.isPrimaryThread()) { main(this::remove); return; }
        if (removed) return;
        removed = true;
        autoTick = false;
        generations.close();
        loads.close();
        cancelLoading();
        updateRenderTask();
        renderLoop.close();
        renderLock.lock();
        try {
            IBoilerSource old = source;
            source = null;
            unload(old);
            raster.clear();
            sectionRaster.clear();
        } finally { renderLock.unlock(); }
        respawn();
        MAP_DISPLAY.destroy();
        HandlerList.unregisterAll(this);
    }

    private void unload(IBoilerSource old) {
        if (old == null) return;
        try { old.unload(); }
        catch (Exception e) { BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Unloading display " + ID, e); }
    }

    @Override public void source(String name, JsonObject data) {
        if (!Bukkit.isPrimaryThread()) { main(() -> source(name, data)); return; }
        if (removed) return;
        Class<? extends IBoilerSource> type = BoilerPlugin.getPlugin().sourceManager().source(name);
        if (type == null) {
            BoilerPlugin.getPlugin().getLogger().warning("Unknown source: " + name);
            return;
        }
        long generation = generations.next();
        cancelLoading();
        renderLock.lock();
        try {
            IBoilerSource old = source;
            source = null;
            unload(old);
            renderPaused = false;
        } finally { renderLock.unlock(); }
        sourceName = name;
        sourceData = data;
        if (persistent && !(data != null && data.has("keepLastSourceData") && data.get("keepLastSourceData").getAsBoolean())) save();
        respawn();
        SourceConfig config = type.getAnnotation(SourceConfig.class);
        sourceType = config == null ? null : config.sourceType();
        try { loads.submit(() -> {
            if (!generations.current(generation)) return;
            IBoilerSource candidate = null;
            loadingThread = Thread.currentThread();
            try {
                if (!generations.current(generation)) return;
                candidate = type.getDeclaredConstructor().newInstance();
                loadingSource = candidate;
                if (!generations.current(generation)) return;
                candidate.load(scopedDisplay(generation, data), data);
                renderLock.lock();
                try {
                    if (generations.current(generation)) {
                        source = candidate;
                        candidate = null;
                    }
                } finally { renderLock.unlock(); }
            } catch (Exception e) {
                if (generations.current(generation)) {
                    BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Loading source " + name + " for display " + ID, e);
                    JsonObject error = new JsonObject();
                    error.addProperty("message", "Failed to load " + name);
                    if (!name.equals("error")) main(() -> { if (generations.current(generation)) source("error", error); });
                }
            } finally {
                unload(candidate);
                loadingSource = null;
                loadingThread = null;
                Thread.interrupted();
            }
        }); } catch (RejectedExecutionException e) {
            sourceType = null;
            BoilerPlugin.getPlugin().getLogger().warning("Source loading queue is full for display " + ID);
            net.somewhatcity.boiler.core.sources.hidden.ErrorSource error = new net.somewhatcity.boiler.core.sources.hidden.ErrorSource();
            JsonObject message = new JsonObject();
            message.addProperty("message", "Source loading is busy; please retry");
            error.load(scopedDisplay(generation, data), message);
            renderLock.lock();
            try { source = error; }
            finally { renderLock.unlock(); }
        }
    }

    // A source may retain its display and call back after a network response or EOF.
    // Keep those callbacks scoped to that source, even after a newer source has loaded.
    private IBoilerDisplay scopedDisplay(long generation, JsonObject data) {
        return (IBoilerDisplay) Proxy.newProxyInstance(IBoilerDisplay.class.getClassLoader(), new Class<?>[]{IBoilerDisplay.class}, (proxy, method, args) -> {
            if (method.getName().equals("sourceData")) return generations.current(generation) ? sourceData : data;
            if (method.getReturnType() == void.class) {
                if (method.getName().equals("render")) {
                    if (generations.current(generation)) render();
                    return null;
                }
                Runnable invoke = () -> {
                    if (!generations.current(generation)) return;
                    try { method.invoke(this, args); }
                    catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                };
                main(invoke);
                return null;
            }
            try { return method.invoke(this, args); }
            catch (InvocationTargetException e) { throw e.getCause(); }
        });
    }

    @Override public void save() {
        if (!Bukkit.isPrimaryThread()) { main(this::save); return; }
        if (removed) return;
        renderLock.lock();
        try {
            drawingSpace.ctx().converter(settings.has("dither") && settings.get("dither").getAsBoolean() ? Converter.FLOYD_STEINBERG : Converter.DIRECT);
            drawingSpace.ctx().buffering(settings.has("buffer") && settings.get("buffer").getAsBoolean());
            drawingSpace.ctx().bundling(settings.has("bundle") && settings.get("bundle").getAsBoolean());
            MAP_DISPLAY.glowing(settings.has("glowing") && settings.get("glowing").getAsBoolean());
            int period = Math.max(1, settings.has("renderPeriod") ? settings.get("renderPeriod").getAsInt() : 20);
            renderPeriod = period;
        } finally { renderLock.unlock(); }
        updateRenderTask();
        BoilerPlugin.getPlugin().displayManager().saveDisplay(this);
    }

    @Override public void saveSourceData(JsonObject data) {
        if (!Bukkit.isPrimaryThread()) { main(() -> saveSourceData(data)); return; }
        sourceData = data;
        save();
    }
    @Override public void onClick(CommandSender sender, int x, int y, boolean right) { withSource(s -> s.onClick(sender, x, y, right)); }
    @Override public void onScroll(CommandSender sender, int x, int y, int delta) { withSource(s -> s.onScroll(sender, x, y, delta)); }
    @Override public void onInput(CommandSender sender, String input) { withSource(s -> s.onInput(sender, input)); }
    @Override public void onKey(CommandSender sender, String key) { withSource(s -> s.onKey(sender, key)); }
    private void withSource(java.util.function.Consumer<IBoilerSource> action) {
        renderLock.lock();
        try { if (source != null && !removed) action.accept(source); }
        finally { renderLock.unlock(); }
    }
    @Override public int id() { return ID; }
    @Override public boolean autoTick() { return autoTick; }
    @Override public void autoTick(boolean value) { autoTick = value; }
    @Override public boolean persistent() { return persistent; }
    @Override public void persistent(boolean value) { persistent = value; }
    @Override public boolean renderPaused() { return renderPaused; }
    @Override public void renderPaused(boolean value) { renderPaused = value; updateRenderTask(); }
    @Override public String rtspPublishUrl() { return "rtsp://localhost:8554/live/display_%s".formatted(ID); }
    @Override public Set<Player> viewers() { return receivers; }
    @Override public List<Location> speakers() { return Collections.unmodifiableList(speakers); }
    @Override public void addSpeaker(Location location) { speakers.add(location); }
    @Override public void clearSpeakers() { speakers.clear(); }
    @Override public String sourceName() { return sourceName; }
    @Override public IBoilerSource source() { return source; }
    @Override public JsonObject sourceData() { return sourceData; }
    @Override public Location cornerA() { return CORNER_A; }
    @Override public Location cornerB() { return CORNER_B; }
    @Override public Location center() { return CORNER_A.clone().add(CORNER_B).multiply(0.5); }
    @Override public JsonObject settings() { return settings; }
    @Override public void settings(JsonObject value) {
        if (!Bukkit.isPrimaryThread()) { main(() -> settings(value)); return; }
        settings = Objects.requireNonNull(value);
        save();
    }
    @Override public BlockFace facing() { return FACING; }
    @Override public IMapDisplay mapDisplay() { return MAP_DISPLAY; }
    @Override public IDrawingSpace drawingSpace() { return drawingSpace; }
    @Override public void viewport(Rectangle value) { viewport = Objects.requireNonNull(value, "Viewport cannot be null"); }
    @Override public Rectangle viewport() { return viewport; }
}
