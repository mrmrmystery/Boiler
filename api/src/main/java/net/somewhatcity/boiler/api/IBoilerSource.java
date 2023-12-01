/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.api;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IDisplay;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.GraphicUtils;
import net.somewhatcity.boiler.api.util.Key;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public interface IBoilerSource {
    void load(IBoilerDisplay display, JsonObject data);
    void unload();
    default void draw(IDrawingSpace drawingSpace) {
        drawingSpace.clear();
        IDisplay display = drawingSpace.ctx().getDisplay();
        BufferedImage img = new BufferedImage(display.pixelWidth(), display.pixelHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        draw(g, new Rectangle(0, 0, display.pixelWidth(), display.pixelHeight()));
        drawingSpace.image(img, 0, 0);
    }
    default void draw(Graphics2D g2, Rectangle viewport) {
        g2.setColor(Color.BLACK);
        g2.fill(viewport);
        g2.setColor(Color.WHITE);
        GraphicUtils.centeredString(g2, viewport, "Drawing not implemented!");
    }
    default byte[] provide20msAudio() {
        return null;
    }
    default void onResize() {}
    default void onClick(Player player, int x, int y, boolean right) {}
    default void onScroll(Player player, int x, int y, int delta) {}
    default void onKey(Player player, Set<Key> pressed) {}
    default void onInput(Player player, String input) {}
    default void onCursorMove(Player player, int x, int y) {}
}
