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

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class DisplayCreateCommand extends CommandAPICommand {
    public DisplayCreateCommand() {
        super("create");
        withPermission("boiler.command.create");
        withArguments(new LocationArgument("posA", LocationType.BLOCK_POSITION));
        withArguments(new LocationArgument("posB", LocationType.BLOCK_POSITION));
        executesPlayer((player, args) -> {
            BlockFace face = player.getTargetBlockFace(10);
            Location a = ((Location) args.get(0)).getBlock().getRelative(face).getLocation();
            Location b = ((Location) args.get(1)).getBlock().getRelative(face).getLocation();
            IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().createDisplay(a, b, face);
            if(display == null) {
                Util.sendErrMsg(player, "Could not create display");
            } else {
                Util.sendMsg(player, "Created new display with id %s", display.id());
            }
        });
    }
}
