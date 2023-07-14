/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.util;

import net.somewhatcity.boiler.Boiler;
import org.bukkit.configuration.file.FileConfiguration;

public class BoilerConfig {

    private Boiler plugin;
    private FileConfiguration config;

    public BoilerConfig(Boiler plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

        insertDefault("webserver.port", 7011);
        insertDefault("view_distance", 100);
        insertDefault("ping_limit", 100);
        insertDefault("database.type", "sqlite");
        insertDefault("database.file", "boiler.db");

    }

    public void insertDefault(String key, Object value) {
        if(config.contains(key)) return;
        config.set(key, value);
        plugin.saveConfig();
    }
}
