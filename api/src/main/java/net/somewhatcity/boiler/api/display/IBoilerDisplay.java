/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.api.display;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.IBoilerSource;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.Set;

public interface IBoilerDisplay {

    int id();
    void source(String name, JsonObject data);
    String sourceName();
    IBoilerSource source();
    JsonObject sourceData();
    Location cornerA();
    Location cornerB();
    Location center();
    JsonObject settings();
    void settings(JsonObject obj);
    BlockFace facing();
    IMapDisplay mapDisplay();
    IDrawingSpace drawingSpace();
    default int width() {
        return mapDisplay().pixelWidth();
    }
    default int height() {
        return mapDisplay().pixelHeight();
    }
    void viewport(Rectangle viewport);
    Rectangle viewport();
    void tick(Player player);
    boolean autoTick();
    void autoTick(boolean value);
    boolean persistent();
    void persistent(boolean value);
    void renderPaused(boolean value);
    boolean renderPaused();
    void render();
    byte[] provide20msAudio();
    void remove();
    Set<Player> viewers();
    List<Location> speakers();
    void onClick(CommandSender player, int x, int y, boolean right);
    void onScroll(CommandSender player, int x, int y, int delta);
    void onInput(CommandSender player, String string);
    void onKey(CommandSender player, String key);
    void save();

}
