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
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;

public class DisplaySettingsCommand extends CommandAPICommand {
    public DisplaySettingsCommand() {
        super("settings");
        withPermission("boiler.command.settings");
        withArguments(BoilerArguments.displayArgument("display"));
        withSubcommand(new CommandAPICommand("dither")
                .withArguments(new BooleanArgument("enabled"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    boolean value = (boolean) args.get(1);
                    display.settings().addProperty("dither", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "dither", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("buffer")
                .withArguments(new BooleanArgument("enabled"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    boolean value = (boolean) args.get(1);
                    display.settings().addProperty("buffer", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "buffer", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("renderPeriod")
                .withArguments(new IntegerArgument("milliseconds"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    int value = (int) args.get(1);
                    display.settings().addProperty("renderPeriod", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "renderPeriod", value, display.id());
                }))
        );
        executes(((sender, args) -> {
            Util.sendErrMsg(sender, "Please specify an option");
        }));
    }
}
