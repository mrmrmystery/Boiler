/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.awt.*;
import java.util.List;

public class SystemInfoSource implements IBoilerSource {
    private static final Font HEADING = new Font("Arial", Font.BOLD, 16);
    private static final Font TEXT = new Font("Arial", Font.PLAIN, 12);
    private volatile List<String> lines = List.of();
    private BukkitTask refresh;
    @Override public void load(IBoilerDisplay display, JsonObject data) {
        BoilerPlugin plugin = BoilerPlugin.getPlugin();
        refresh = plugin.callOnMain(() -> {
            snapshot();
            return Bukkit.getScheduler().runTaskTimer(plugin, this::snapshot, 20, 20);
        });
    }
    private void snapshot() {
        var mapEngine = Bukkit.getPluginManager().getPlugin("MapEngine");
        lines = List.of(
                "OS Name: " + System.getProperty("os.name"),
                "OS Version: " + System.getProperty("os.version"),
                "OS Architecture: " + System.getProperty("os.arch"),
                "Java Version: " + System.getProperty("java.version"),
                "Boiler Version: " + BoilerPlugin.getPlugin().getPluginMeta().getVersion(),
                "MapEngine Version: " + (mapEngine == null ? "unavailable" : mapEngine.getPluginMeta().getVersion()),
                "SimpleVoiceChat installed: " + Util.isPluginInstalled("voicechat"),
                "PlasmoVoice installed: " + Util.isPluginInstalled("PlasmoVoice"),
                "Gstreamer installed: " + Util.isGstreamerInstalled());
    }
    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, viewport.width, viewport.height);
        graphics.setColor(Color.WHITE);
        graphics.setFont(HEADING);
        graphics.drawString("System Info", 10, 20);
        graphics.setFont(TEXT);
        List<String> snapshot = lines;
        for (int i = 0; i < snapshot.size(); i++) graphics.drawString(snapshot.get(i), 10, 40 + i * 20 + (i >= 6 ? 20 : 0));
    }
    @Override public void unload() { if (refresh != null) refresh.cancel(); }
}
