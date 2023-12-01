/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.commands.display;

import de.pianoman911.mapengine.api.util.MapTraceResult;
import dev.jorel.commandapi.CommandAPICommand;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class DisplayIdentifyCommand extends CommandAPICommand {
    public DisplayIdentifyCommand() {
        super("identify");
        executesPlayer((player, args) -> {
            MapTraceResult result = MAP_ENGINE.traceDisplayInView(player, 10);
            if(result == null) {
                Util.sendErrMsg(player, "No display in view");
                return;
            }
            IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().display(result.display());
            if(display == null) {
                Util.sendErrMsg(player, "No boiler display (could be from another plugin)");
                return;
            }
            Util.sendMsg(player, "Found display with id %s", display.id());
        });
    }
}
