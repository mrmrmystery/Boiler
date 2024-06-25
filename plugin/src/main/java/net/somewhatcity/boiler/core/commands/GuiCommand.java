/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.commands;

import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class GuiCommand extends CommandAPICommand {
    public GuiCommand() {
        super("gui");
        withArguments(new IntegerArgument("width"));
        withArguments(new IntegerArgument("height"));
        /*
        withSubcommand(new CommandAPICommand("test")
                .executesPlayer(((player, args) -> {
                    BoilerPlugin.getPlugin().guiManager().open(player, 3, 2, "swing", null);
                })));

         */

        BoilerPlugin.getPlugin().sourceManager().sources().forEach((name, source) -> {
            if(!BoilerPlugin.getPlugin().sourceManager().commandVisibleSourceNames().contains(name)) return;

            try {
                Method method = source.getMethod("creationArguments");
                List<Argument<?>> Carguments = (List<Argument<?>>) method.invoke(null);
                withSubcommand(new CommandAPICommand(name)
                        .withArguments(Carguments)
                        .executesPlayer((sender, args) -> {
                            int width = (int) args.get(0);
                            int height = (int) args.get(1);
                            JsonObject data = new JsonObject();

                            List<Map.Entry<String, Object>> entries = args.argsMap().entrySet().stream().toList();
                            for(int i = 1; i < entries.size(); i++) {
                                Map.Entry<String, Object> entry = entries.get(i);
                                data.addProperty(entry.getKey(), entry.getValue().toString());
                            }

                            BoilerPlugin.getPlugin().guiManager().open(sender, width, height, name, data);
                        })
                );
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                withSubcommand(new CommandAPICommand(name)
                        .executesPlayer((sender, args) -> {
                            int width = (int) args.get(0);
                            int height = (int) args.get(1);

                            JsonObject data = new JsonObject();

                            BoilerPlugin.getPlugin().guiManager().open(sender, width, height, name, data);
                        })
                );
            }
        });

        executes(((sender, args) -> {
            Util.sendMsg(sender, "No source selected");
        }));


        /*

        executesPlayer((player, args) -> {
            if(BoilerConfig.guiEnabled) {
                BoilerPlugin.getPlugin().guiManager().open(player, "browser");
                Util.sendMsg(player, "Leave gui by sneaking");
                Util.sendMsg(player, "Boiler GUI is still in early development!");
            } else {
                Util.sendErrMsg(player, "Gui is disabled. Please enable it in the config first");
                Util.sendMsg(player, "Boiler GUI is still in early development!");
            }
        });

         */


    }
}
