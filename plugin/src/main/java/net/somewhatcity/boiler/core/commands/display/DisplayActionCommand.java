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
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;

import java.util.Collections;

public class DisplayActionCommand extends CommandAPICommand {
    public DisplayActionCommand() {
        super("action");
        withPermission("boiler.command.action");
        withArguments(BoilerArguments.displayArgument("display"));
        withSubcommand(new CommandAPICommand("click")
                .withArguments(new IntegerArgument("x"))
                .withArguments(new IntegerArgument("y"))
                .withOptionalArguments(new BooleanArgument("rightClick"))
                .executes((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);

                    int x = (int) args.get(1);
                    int y = (int) args.get(2);
                    boolean rightClick = false;
                    if(args.get(3) != null) rightClick = (boolean) args.get(3);

                    display.onClick(sender, x, y, rightClick);
                    Util.sendMsg(sender, "Executing click on display %s", display.id());
                })
        );
        withSubcommand(new CommandAPICommand("input")
                .withArguments(new GreedyStringArgument("string"))
                .executes((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);

                    String input = (String) args.get(1);

                    display.onInput(sender, input);
                    Util.sendMsg(sender, "Executing input on display %s", display.id());
                })
        );
        withSubcommand(new CommandAPICommand("key")
                .withArguments(new GreedyStringArgument("key"))
                .executes((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    String key = (String) args.get(1);

                    display.onKey(sender, key);
                    Util.sendMsg(sender, "Executing key on display %s", display.id());
                })
        );
        executes((sender, args) -> {
            sender.sendMessage("please specify an action!");
        });
    }
}
