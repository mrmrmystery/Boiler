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
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class SectionCloneSource implements IBoilerSource {

    private IBoilerDisplay display;
    private IBoilerDisplay toClone;

    private int x;
    private int y;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(BoilerPlugin.getPlugin(), () -> {
            new Thread(() -> {
                this.display = display;
                int cloneId = data.get("id").getAsInt();
                x = data.get("x").getAsInt();
                y = data.get("y").getAsInt();
                toClone = BoilerPlugin.getPlugin().displayManager().display(cloneId);
            }).start();
        }, 10);
    }

    @Override
    public void unload() {

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        if(toClone.source() != null) {
            BufferedImage snapshot = new BufferedImage(toClone.width(), toClone.height(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D snapg = snapshot.createGraphics();
            toClone.source().draw(snapg, toClone.viewport());
            snapg.dispose();
            g2.drawImage(snapshot.getSubimage(x, y, display.width(), display.height()), 0, 0, null);
        }

    }

    public static List<Argument<?>> creationArguments() {
        return List.of(
                new IntegerArgument("id"),
                new IntegerArgument("x"),
                new IntegerArgument("y")
        );
    }
}
