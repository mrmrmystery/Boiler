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

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BoilerDisplayManager implements IDisplayManager {

    private final BoilerPlugin plugin;
    private FileConfiguration displayConfig = null;
    private File displayFile;
    private HashMap<Integer, IBoilerDisplay> displays;
    public BoilerDisplayManager(BoilerPlugin plugin) {
        this.plugin = plugin;

        reloadDisplayConfig();

        displays = new HashMap<>();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();
                displays.forEach((key, value) -> {
                    if(value.autoTick()) players.forEach(value::tick);
                });
            }
        }, 0, 1000);

        Bukkit.getScheduler().runTaskLater(BoilerPlugin.getPlugin(), this::loadDisplays, 20);
    }

    public void reloadDisplayConfig() {
        displayFile = new File(plugin.getDataFolder(), "displays.yml");

        if(!displayFile.exists()) {
            try {
                displayFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        displayConfig = YamlConfiguration.loadConfiguration(displayFile);
    }

    public void saveDisplayConfig() {
        try {
            displayConfig.save(displayFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDisplay(IBoilerDisplay display) {

    }

    public void loadDisplays() {

        if(displayConfig.getConfigurationSection("boiler.displays") == null) return;

        for(String key : displayConfig.getConfigurationSection("boiler.displays").getKeys(false)) {
            ConfigurationSection display = displayConfig.getConfigurationSection("boiler.displays." + key);

            if(displayConfig == null) continue;

            int id = Integer.parseInt(key);
            Location a = display.getLocation("cornerA");
            Location b = display.getLocation("cornerB");
            String face = display.getString("facing");

            if(a == null || b == null || face == null) continue;

            IBoilerDisplay boilerDisplay = new ImplBoilerDisplay(id, a, b, BlockFace.valueOf(face));

            if(display.get("source.name") != null) {
                String sourceName = display.getString("source.name", "default");
                String[] args = display.getStringList("source.data").toArray(new String[0]);

                //boilerDisplay.source(sourceName, args);
            }
            displays.put(boilerDisplay.id(), boilerDisplay);
        }
    }

    private int nextId() {
        reloadDisplayConfig();
        int current = displayConfig.getInt("boiler.nextId", 0);
        current++;
        displayConfig.set("boiler.nextId", current);
        saveDisplayConfig();
        return current;
    }
    @Override
    public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face) {
        return createDisplay(a, b, face, true, true);
    }

    @Override
    public IBoilerDisplay createDisplay(Location a, Location b, BlockFace face, boolean persistent, boolean autoTick) {
        int id = -1;
        if(persistent) id = nextId();
        IBoilerDisplay boilerDisplay = new ImplBoilerDisplay(id, a, b, face);
        boilerDisplay.autoTick(autoTick);

        if(persistent) {
            String path = "boiler.displays.%s.".formatted(id);


            displayConfig.set(path + "cornerA", boilerDisplay.cornerA());
            displayConfig.set(path + "cornerB", boilerDisplay.cornerB());
            displayConfig.set(path + "facing", boilerDisplay.facing().name());

            saveDisplayConfig();
            displays.put(boilerDisplay.id(), boilerDisplay);
        }

        return boilerDisplay;
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

        displayConfig.set("boiler.displays.%s".formatted(display.id()), null);
        saveDisplayConfig();
    }
    @Override
    public void removeDisplay(int id) {
        removeDisplay(display(id));
    }

    @Override
    public FileConfiguration displayConfig() {
        return displayConfig;
    }
}
