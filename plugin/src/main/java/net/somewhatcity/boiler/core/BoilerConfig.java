/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core;

import org.bukkit.configuration.file.FileConfiguration;

public class BoilerConfig {
    private BoilerPlugin plugin;
    public static int viewDistance = 100;
    public static boolean guiEnabled = false;
    public static String svcChannelName = "boiler";
    public static boolean useSvcGroups = false;

    public BoilerConfig(BoilerPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reloadConfig();
    }

    public void reloadConfig() {
        plugin.saveConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        viewDistance = config.getInt("boiler.settings.view_distance", 100);
        guiEnabled = config.getBoolean("boiler.settings.gui_enabled", false);
        svcChannelName = config.getString("boiler.settings.svc_channel_name", "boiler");
        //useSvcGroups = config.getBoolean("boiler.settings.use_svc_groups", false);
    }
}
