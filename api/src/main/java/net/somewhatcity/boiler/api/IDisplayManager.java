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

import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Set;

public interface IDisplayManager {

    IBoilerDisplay createDisplay(Location a, Location b, BlockFace face);
    IBoilerDisplay createDisplay(Location a, Location b, BlockFace face, boolean persistent, boolean autoTick);
    IBoilerDisplay display(int id);
    IBoilerDisplay display(IMapDisplay mapDisplay);
    List<IBoilerDisplay> displays();
    void removeDisplay(IBoilerDisplay display);
    void removeDisplay(int id);
    FileConfiguration displayConfig();
    void reloadDisplayConfig();
    void saveDisplayConfig();
    void saveDisplay(IBoilerDisplay display);

}
