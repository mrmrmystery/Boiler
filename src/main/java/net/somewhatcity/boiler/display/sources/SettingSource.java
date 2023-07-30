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
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.api.ui.BoilerUI;
import net.somewhatcity.boiler.api.ui.components.BCheckBox;
import net.somewhatcity.boiler.api.ui.components.BFrame;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.util.Assets;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SettingSource implements BoilerSource {

    private BoilerUI ui;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        ui = new BoilerUI(display);
        BFrame frame = ui.getFrame();

        BCheckBox cbDithering = new BCheckBox(5, 5, "Dithering", display.getOption("dithering", false));
        cbDithering.addClickListener(player -> {
            player.sendMessage("Dithering is " + (cbDithering.checked ? "enabled" : "disabled") + " for this map.");
            display.setOption("dithering", cbDithering.checked);
            display.reloadOptions();
        });
        frame.add(cbDithering);

        BCheckBox cbCaching = new BCheckBox(5, 20, "Caching", display.getOption("caching", true));
        cbCaching.addClickListener(player -> {
            player.sendMessage("Caching is " + (cbCaching.checked ? "enabled" : "disabled") + " for this map.");
            display.setOption("caching", cbCaching.checked);
            display.reloadOptions();
        });
        frame.add(cbCaching);
    }

    @Override
    public void unload() {
        ui.close();
    }

    @Override
    public void onclick(int x, int y, Player player) {
        ui.handleClick(x, y, player);
    }

    @Override
    public BufferedImage image() {
        return ui.getImage();
    }
}
