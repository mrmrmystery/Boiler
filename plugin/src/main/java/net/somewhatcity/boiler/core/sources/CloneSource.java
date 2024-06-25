/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "id", type = CommandArgumentType.INTEGER)
})
public class CloneSource implements IBoilerSource {

    private IBoilerDisplay display;
    private IBoilerDisplay toClone;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(BoilerPlugin.getPlugin(), () -> {
            new Thread(() -> {
                this.display = display;
                int cloneId = data.get("id").getAsInt();
                toClone = BoilerPlugin.getPlugin().displayManager().display(cloneId);

                if(display.width() != toClone.width() || display.height() != toClone.height()) {

                    JsonObject err = new JsonObject();
                    err.addProperty("message", "Displays must have the same dimensions");
                    display.source("error", err);
                    return;
                }

                display.renderPaused(true);
                display.mapDisplay().cloneGroupIds(toClone.mapDisplay());

                Bukkit.getOnlinePlayers().forEach(player -> {
                    display.mapDisplay().mapId(player, 0);
                });
            }).start();
        }, 10);
    }

    @Override
    public void unload() {
        display.mapDisplay().cutOffCloneGroupIds();
        display.renderPaused(false);

        Bukkit.getOnlinePlayers().forEach(player -> {
            display.mapDisplay().mapId(player, 0);
        });
    }

    @Override
    public void draw(IDrawingSpace drawingSpace) {

    }

}
