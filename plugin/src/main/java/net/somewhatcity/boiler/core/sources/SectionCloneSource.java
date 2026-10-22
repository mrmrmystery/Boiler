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
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;

import java.awt.*;
import java.awt.image.BufferedImage;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "id", type = CommandArgumentType.INTEGER),
        @CreateArgument(name = "x", type = CommandArgumentType.INTEGER),
        @CreateArgument(name = "y", type = CommandArgumentType.INTEGER)
})
public class SectionCloneSource implements IBoilerSource {

    private IBoilerDisplay display;
    private IBoilerDisplay toClone;
    private final net.somewhatcity.boiler.core.display.RasterRenderer raster = new net.somewhatcity.boiler.core.display.RasterRenderer();

    private int x;
    private int y;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        this.display = display;
        x = data.get("x").getAsInt();
        y = data.get("y").getAsInt();
        toClone = BoilerPlugin.getPlugin().displayManager().display(data.get("id").getAsInt());
        if (toClone == null || toClone.id() == display.id() || x < 0 || y < 0 || (long) x + display.width() > toClone.width() || (long) y + display.height() > toClone.height())
            throw new IllegalArgumentException("Invalid section clone target or bounds");

    }

    @Override
    public void unload() {
        raster.clear();

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        if (toClone instanceof net.somewhatcity.boiler.core.display.ImplBoilerDisplay target) {
            target.drawSection(g2, x, y, display.width(), display.height());
        } else if (toClone != null && toClone.source() != null) {
            BufferedImage snapshot = raster.drawImage(toClone.source(), toClone.width(), toClone.height());
            g2.drawImage(snapshot, 0, 0, display.width(), display.height(), x, y, x + display.width(), y + display.height(), null);
        }
    }
}
