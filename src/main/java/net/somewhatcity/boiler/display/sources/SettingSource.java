/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.event.MapClickEvent;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.util.Assets;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SettingSource extends Source implements Listener {

    private BufferedImage image;
    private IMapDisplay display;
    private LoadedMapDisplay lmd;

    @Override
    public void load(LoadedMapDisplay loadedMapDisplay, IMapDisplay display, JsonObject data) {
        this.display = display;
        this.lmd = loadedMapDisplay;
        Bukkit.getPluginManager().registerEvents(this, Boiler.getPlugin());


        render();
    }

    private void render() {
        BufferedImage img = new BufferedImage(display.pixelWidth(), display.pixelHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(Assets.SETTINGS_BG, 0, 0, null);

        JsonObject settings = lmd.getSettings();
        if(settings.get("dithering") != null && settings.get("dithering").getAsBoolean()) {
            g.drawImage(Assets.SETTINGS_ON, 100, 12, null);
        } else {
            g.drawImage(Assets.SETTINGS_OFF, 100, 12, null);
        }

        if(settings.get("caching") != null && settings.get("caching").getAsBoolean()) {
            g.drawImage(Assets.SETTINGS_ON, 100, 26, null);
        } else {
            g.drawImage(Assets.SETTINGS_OFF, 100, 26, null);
        }

        image = img;
    }

    @Override
    public void unload() {
        MapClickEvent.getHandlerList().unregister(this);
    }

    @Override
    public BufferedImage getFrame() {
        return image;
    }

    @EventHandler
    public void onMapClick(MapClickEvent e) {
        if(!e.display().equals(display)) return;

        JsonObject settings = lmd.getSettings();

        int y = e.y();
        if(y > 11 && y < 25) {
            if(settings.get("dithering") != null && settings.get("dithering").getAsBoolean()) {
                settings.addProperty("dithering", false);
            } else {
                settings.addProperty("dithering", true);
            }
        } else if (y < 39) {
            if(settings.get("caching") != null && settings.get("caching").getAsBoolean()) {
                settings.addProperty("caching", false);
            } else {
                settings.addProperty("caching", true);
            }
        }

        MapDisplayManager.setSettings(lmd.getId(), settings);
        render();
    }
}
