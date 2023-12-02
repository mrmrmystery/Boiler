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

import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;
import net.somewhatcity.boiler.core.commands.BoilerCommand;
import net.somewhatcity.boiler.core.sources.hidden.DefaultSource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisplaySourceCommand extends CommandAPICommand {
    public DisplaySourceCommand() {
        super("source");
        withPermission("boiler.command.source");
        withArguments(BoilerArguments.displayArgument("display"));

        BoilerPlugin.getPlugin().sourceManager().sources().forEach((name, source) -> {
            if(!BoilerPlugin.getPlugin().sourceManager().commandVisibleSourceNames().contains(name)) return;

            try {
                Method method = source.getMethod("creationArguments");
                List<Argument<?>> Carguments = (List<Argument<?>>) method.invoke(null);
                withSubcommand(new CommandAPICommand(name)
                        .withArguments(Carguments)
                        .executes((sender, args) -> {
                            IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                            JsonObject data = new JsonObject();

                            List<Map.Entry<String, Object>> entries = args.argsMap().entrySet().stream().toList();
                            for(int i = 1; i < entries.size(); i++) {
                                Map.Entry<String, Object> entry = entries.get(i);
                                data.addProperty(entry.getKey(), entry.getValue().toString());
                            }

                            display.source(name, data);
                            Util.sendMsg(sender,"Setting source for display %s to %s", display.id(), name);
                        })
                );
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                withSubcommand(new CommandAPICommand(name)
                        .executes((sender, args) -> {
                            IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                            JsonObject data = new JsonObject();

                            display.source(name, data);
                            Util.sendMsg(sender,"Setting source for display %s to %s", display.id(), name);
                        })
                );
            }
        });

        executes(((sender, args) -> {
            IBoilerDisplay display = (IBoilerDisplay) args.get(0);
            display.source("default", new JsonObject());
            Util.sendMsg(sender, "Reset source of display %s", display.id());
        }));
    }
}
