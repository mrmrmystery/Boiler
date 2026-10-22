/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.display;

import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.HashMap;
import java.util.UUID;

public class UpdateIntervalManager {
    private BoilerPlugin plugin;
    private java.util.concurrent.ConcurrentHashMap<UUID, Integer> intervals;
    public UpdateIntervalManager(BoilerPlugin plugin) {
        this.plugin = plugin;
        this.intervals = new java.util.concurrent.ConcurrentHashMap<>();

        load();
    }
    public int getInterval(Player player) {
        return intervals.getOrDefault(player.getUniqueId(), -1);
    }
    public void setInterval(Player player, int interval) {
        if(interval == -1) intervals.remove(player.getUniqueId());
        else intervals.put(player.getUniqueId(), interval);
        save();
    }
    public void load() {
        File file = new File(plugin.getDataFolder(), "intervals.ser");
        if(!file.exists()) return;
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(file))) {
            Object stored = input.readObject();
            if (!(stored instanceof java.util.Map<?, ?> values)) throw new StreamCorruptedException("Invalid interval data");
            java.util.concurrent.ConcurrentHashMap<UUID, Integer> loaded = new java.util.concurrent.ConcurrentHashMap<>();
            for (var entry : values.entrySet()) {
                if (!(entry.getKey() instanceof UUID id) || !(entry.getValue() instanceof Integer value))
                    throw new StreamCorruptedException("Invalid interval entry");
                loaded.put(id, value);
            }
            intervals = loaded;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public void save() {
        File file = new File(plugin.getDataFolder(), "intervals.ser");
        plugin.getDataFolder().mkdirs();
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(file))) {
            output.writeObject(new HashMap<>(intervals));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
