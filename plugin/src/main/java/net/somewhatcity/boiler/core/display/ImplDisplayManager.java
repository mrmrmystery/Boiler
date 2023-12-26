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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ImplDisplayManager implements IDisplayManager {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final BoilerPlugin plugin;
    private HashMap<Integer, IBoilerDisplay> displays;
    public ImplDisplayManager(BoilerPlugin plugin) {
        this.plugin = plugin;
        this.displays = new HashMap<>();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                displays.forEach((key, value) -> {
                    if(value.autoTick()) players.forEach(value::tick);
                });
            }
        }, 0, 1000);

        Bukkit.getScheduler().runTaskLater(plugin, this::loadDisplays, 20);
    }

    private int nextId() {
        BoilerPlugin.getPlugin().reloadConfig();
        int current = BoilerPlugin.getPlugin().getConfig().getInt("boiler.nextId", 0);
        current++;
        BoilerPlugin.getPlugin().getConfig().set("boiler.nextId", current);
        BoilerPlugin.getPlugin().saveConfig();
        return current;
    }
    @Override
    public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face) {
        return createDisplay(a,b, face, true, true);
    }

    @Override
    public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face, boolean persistent, boolean autoTick) {
        int id = -1;
        if(persistent) id = nextId();
        IBoilerDisplay boilerDisplay = new ImplBoilerDisplay(id, a, b, face);
        boilerDisplay.autoTick(autoTick);

        if(persistent) {
            File displayFile = new File(plugin.getDataFolder(), "displays/display_%s.json".formatted(id));
            try {
                if(!displayFile.exists()) displayFile.createNewFile();
                JsonObject obj = new JsonObject();
                obj.addProperty("id", id);

                JsonObject location = new JsonObject();
                location.addProperty("world", boilerDisplay.cornerA().getWorld().getName());
                location.addProperty("facing", boilerDisplay.facing().name());
                location.addProperty("x1", boilerDisplay.cornerA().getBlockX());
                location.addProperty("y1", boilerDisplay.cornerA().getBlockY());
                location.addProperty("z1", boilerDisplay.cornerA().getBlockZ());
                location.addProperty("x2", boilerDisplay.cornerB().getBlockX());
                location.addProperty("y2", boilerDisplay.cornerB().getBlockY());
                location.addProperty("z2", boilerDisplay.cornerB().getBlockZ());
                obj.add("location", location);

                Files.writeString(displayFile.toPath(), gson.toJson(obj));
                displays.put(boilerDisplay.id(), boilerDisplay);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

        return boilerDisplay;
    }
    public void saveDisplay(IBoilerDisplay display) {
        if(display.persistent()) {
            File displayFile = new File(plugin.getDataFolder(), "displays/display_%s.json".formatted(display.id()));
            if(!displayFile.exists()) return;
            try {
                JsonObject obj = (JsonObject) JsonParser.parseReader(new FileReader(displayFile));

                obj.add("settings", display.settings());

                JsonObject source = new JsonObject();
                source.addProperty("name", display.sourceName());
                source.add("data", display.sourceData());
                obj.add("source", source);

                Files.writeString(displayFile.toPath(), gson.toJson(obj));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public IBoilerDisplay display(int id) {
        return displays.get(id);
    }

    @Override
    public IBoilerDisplay display(IMapDisplay mapDisplay) {
        return displays.values().stream().filter(d -> d.mapDisplay().equals(mapDisplay)).findFirst().orElse(null);
    }

    @Override
    public List<IBoilerDisplay> displays() {
        return displays.values().stream().toList();
    }

    @Override
    public void removeDisplay(IBoilerDisplay display) {
        displays.remove(display.id());
        display.remove();

        File file = new File(plugin.getDataFolder(), "displays/display_%s.json".formatted(display.id()));
        if(file.exists()) {
            file.delete();
        }
    }

    @Override
    public void removeDisplay(int id) {
        removeDisplay(display(id));
    }

    @Override
    public FileConfiguration displayConfig() {
        return null;
    }

    @Override
    public void reloadDisplayConfig() {

    }

    @Override
    public void saveDisplayConfig() {

    }

    private void loadDisplays() {
        File displayFiles = new File(plugin.getDataFolder(), "displays");
        if(!displayFiles.exists()) displayFiles.mkdir();

        for(File displayFile : displayFiles.listFiles()) {
            try {
                JsonObject obj = (JsonObject) JsonParser.parseReader(new FileReader(displayFile));

                int id = obj.get("id").getAsInt();
                World world = Bukkit.getWorld(obj.getAsJsonObject("location").get("world").getAsString());

                Location a = new Location(
                        world,
                        obj.getAsJsonObject("location").get("x1").getAsInt(),
                        obj.getAsJsonObject("location").get("y1").getAsInt(),
                        obj.getAsJsonObject("location").get("z1").getAsInt()
                );

                Location b = new Location(
                        world,
                        obj.getAsJsonObject("location").get("x2").getAsInt(),
                        obj.getAsJsonObject("location").get("y2").getAsInt(),
                        obj.getAsJsonObject("location").get("z2").getAsInt()
                );

                String face = obj.getAsJsonObject("location").get("facing").getAsString();


                if(a == null || b == null || face == null) continue;

                IBoilerDisplay boilerDisplay = new ImplBoilerDisplay(id, a, b, BlockFace.valueOf(face));

                if(obj.getAsJsonObject("source") != null) {
                    String sourceName;
                    if(obj.getAsJsonObject("source").get("name") != null && !obj.getAsJsonObject("source").get("name").isJsonNull()) {
                        sourceName = obj.getAsJsonObject("source").get("name").getAsString();
                    } else {
                        sourceName = "default";
                    }

                    JsonObject sourceData;
                    if(obj.getAsJsonObject("source").get("data") != null && !obj.getAsJsonObject("source").getAsJsonObject("data").isJsonNull()) {
                        sourceData = obj.getAsJsonObject("source").getAsJsonObject("data");
                    } else {
                        sourceData = new JsonObject();
                    }


                    if(sourceName != null && sourceData != null) boilerDisplay.source(sourceName, sourceData);
                }

                if(obj.getAsJsonObject("settings") != null) {
                    boilerDisplay.settings(obj.getAsJsonObject("settings"));
                }

                displays.put(boilerDisplay.id(), boilerDisplay);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
