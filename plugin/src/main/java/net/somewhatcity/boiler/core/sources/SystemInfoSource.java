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
import net.somewhatcity.boiler.core.Util;
import org.bukkit.Bukkit;

import java.awt.*;

public class SystemInfoSource implements IBoilerSource {

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, viewport.width, viewport.height);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("System Info", 10, 20);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.drawString("OS Name: " + System.getProperty("os.name"), 10, 40);
        g2.drawString("OS Version: " + System.getProperty("os.version"), 10, 60);
        g2.drawString("OS Architecture: " + System.getProperty("os.arch"), 10, 80);
        g2.drawString("Java Version: " + System.getProperty("java.version"), 10, 100);

        g2.drawString("Boiler Version: " + Bukkit.getPluginManager().getPlugin("boiler").getPluginMeta().getVersion(), 10, 120);
        g2.drawString("MapEngine Version: " + Bukkit.getPluginManager().getPlugin("MapEngine").getPluginMeta().getVersion(), 10, 140);
        g2.drawString("MapEngine-MediaExt Version: " + Bukkit.getPluginManager().getPlugin("MapMediaExt").getPluginMeta().getVersion(), 10, 160);
        g2.drawString("SimpleVoiceChat installed: " + Util.isPluginInstalled("voicechat"), 10, 180);
        g2.drawString("PlasmoVoice installed: " + Util.isPluginInstalled("PlasmoVoice"), 10, 200);
        g2.drawString("Gstreamer installed: " + Util.isGstreamerInstalled(), 10, 220);
    }

    @Override
    public void unload() {

    }

    private void text(String text) {

    }
}
