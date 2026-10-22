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

import com.google.gson.*;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.runtime.AtomicFiles;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ImplDisplayManager implements IDisplayManager, AutoCloseable {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final BoilerPlugin plugin;
    private final Map<Integer, IBoilerDisplay> displays = new ConcurrentHashMap<>();
    private final Map<IMapDisplay, IBoilerDisplay> byMap = new ConcurrentHashMap<>();
    private final BukkitTask tickTask;
    private final BukkitTask loadTask;
    private int nextTransientId = -1;
    private volatile boolean closed;

    public ImplDisplayManager(BoilerPlugin plugin) {
        this.plugin = plugin;
        loadTask = Bukkit.getScheduler().runTaskLater(plugin, this::loadDisplays, 20);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Map<UUID, List<ViewerPosition>> positions = viewerPositions();
            for (IBoilerDisplay display : displays.values()) {
                if (!display.autoTick()) continue;
                try {
                    if (display instanceof ImplBoilerDisplay impl) impl.tick(positions);
                    else display.tick();
                } catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Updating display " + display.id(), e); }
            }
        }, 20, 20);
    }

    record ViewerPosition(Player player, double x, double y, double z) {}
    static Map<UUID, List<ViewerPosition>> viewerPositions() {
        Map<UUID, List<ViewerPosition>> byWorld = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location location = player.getLocation();
            byWorld.computeIfAbsent(location.getWorld().getUID(), ignored -> new ArrayList<>())
                    .add(new ViewerPosition(player, location.getX(), location.getY(), location.getZ()));
        }
        return byWorld;
    }

    private int nextId() {
        int next = plugin.getConfig().getInt("boiler.nextId", 0) + 1;
        plugin.getConfig().set("boiler.nextId", next);
        plugin.saveConfig();
        return next;
    }

    @Override public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face) { return createDisplay(a, b, face, true, true); }
    @Override public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face, boolean persistent, boolean autoTick) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Create displays on the server thread");
        if (closed) throw new IllegalStateException("Display manager is closed");
        ImplBoilerDisplay display = new ImplBoilerDisplay(persistent ? nextId() : nextTransientId--, a, b, face);
        display.persistent(persistent);
        display.autoTick(autoTick);
        register(display);
        saveDisplay(display);
        return display;
    }

    private void register(IBoilerDisplay display) {
        displays.put(display.id(), display);
        byMap.put(display.mapDisplay(), display);
    }

    @Override public synchronized void saveDisplay(IBoilerDisplay display) {
        if (closed || !display.persistent() || displays.get(display.id()) != display) return;
        JsonObject object = new JsonObject();
        object.addProperty("id", display.id());
        JsonObject location = new JsonObject();
        location.addProperty("world", display.cornerA().getWorld().getName());
        location.addProperty("facing", display.facing().name());
        location.addProperty("x1", display.cornerA().getBlockX());
        location.addProperty("y1", display.cornerA().getBlockY());
        location.addProperty("z1", display.cornerA().getBlockZ());
        location.addProperty("x2", display.cornerB().getBlockX());
        location.addProperty("y2", display.cornerB().getBlockY());
        location.addProperty("z2", display.cornerB().getBlockZ());
        object.add("location", location);
        object.add("settings", display.settings());
        JsonObject source = new JsonObject();
        source.addProperty("name", display.sourceName());
        source.add("data", display.sourceData());
        object.add("source", source);
        JsonArray speakers = new JsonArray();
        for (Location speaker : display.speakers()) {
            JsonObject value = new JsonObject();
            value.addProperty("world", speaker.getWorld().getName());
            value.addProperty("x", speaker.getX());
            value.addProperty("y", speaker.getY());
            value.addProperty("z", speaker.getZ());
            speakers.add(value);
        }
        object.add("speakers", speakers);
        try { AtomicFiles.write(file(display.id()), GSON.toJson(object)); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    private Path file(int id) { return plugin.getDataFolder().toPath().resolve("displays/display_" + id + ".json"); }
    @Override public IBoilerDisplay display(int id) { return displays.get(id); }
    @Override public IBoilerDisplay display(IMapDisplay mapDisplay) { return mapDisplay == null ? null : byMap.get(mapDisplay); }
    @Override public List<IBoilerDisplay> displays() { return List.copyOf(displays.values()); }
    @Override public void removeDisplay(IBoilerDisplay display) {
        if (display == null) return;
        if (!Bukkit.isPrimaryThread()) { plugin.onMain(() -> removeDisplay(display)); return; }
        displays.remove(display.id(), display);
        byMap.remove(display.mapDisplay(), display);
        display.remove();
        if (display.persistent()) {
            try { Files.deleteIfExists(file(display.id())); }
            catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }
    @Override public void removeDisplay(int id) { removeDisplay(display(id)); }
    @Override public FileConfiguration displayConfig() { return null; }
    @Override public void reloadDisplayConfig() {}
    @Override public void saveDisplayConfig() {}

    private void loadDisplays() {
        File directory = plugin.getDataFolder().toPath().resolve("displays").toFile();
        directory.mkdirs();
        File[] files = directory.listFiles((dir, name) -> name.startsWith("display_") && name.endsWith(".json"));
        if (files == null || closed) return;
        for (File file : files) {
            ImplBoilerDisplay display = null;
            try (Reader reader = Files.newBufferedReader(file.toPath())) {
                JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
                int id = object.get("id").getAsInt();
                if (displays.containsKey(id)) continue;
                JsonObject loc = object.getAsJsonObject("location");
                World world = Bukkit.getWorld(loc.get("world").getAsString());
                if (world == null) { plugin.getLogger().warning("Skipping display in missing world: " + file.getName()); continue; }
                Location a = new Location(world, loc.get("x1").getAsInt(), loc.get("y1").getAsInt(), loc.get("z1").getAsInt());
                Location b = new Location(world, loc.get("x2").getAsInt(), loc.get("y2").getAsInt(), loc.get("z2").getAsInt());
                display = new ImplBoilerDisplay(id, a, b, BlockFace.valueOf(loc.get("facing").getAsString()));
                // Finish hydration before registering: setters must not overwrite the file mid-load.
                if (object.has("settings")) display.settings(object.getAsJsonObject("settings"));
                if (object.has("speakers")) for (JsonElement element : object.getAsJsonArray("speakers")) {
                    JsonObject speaker = element.getAsJsonObject();
                    World speakerWorld = Bukkit.getWorld(speaker.get("world").getAsString());
                    if (speakerWorld != null) display.addSpeaker(new Location(speakerWorld, speaker.get("x").getAsDouble(), speaker.get("y").getAsDouble(), speaker.get("z").getAsDouble()));
                }
                if (object.has("source")) {
                    JsonObject src = object.getAsJsonObject("source");
                    String name = src.has("name") && !src.get("name").isJsonNull() ? src.get("name").getAsString() : "default";
                    JsonObject data = src.has("data") && !src.get("data").isJsonNull() ? src.getAsJsonObject("data") : new JsonObject();
                    display.source(name, data);
                }
                register(display);
            } catch (Exception e) {
                if (display != null) display.remove();
                plugin.getLogger().log(Level.WARNING, "Loading " + file.getName(), e);
            }
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        tickTask.cancel();
        loadTask.cancel();
        for (IBoilerDisplay display : displays()) {
            try { display.remove(); }
            catch (Exception e) { plugin.getLogger().log(Level.WARNING, "Closing display " + display.id(), e); }
        }
        displays.clear();
        byMap.clear();
    }
}
