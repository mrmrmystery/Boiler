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

import de.pianoman911.mapengine.api.util.MapTraceResult;
import dev.jorel.commandapi.arguments.*;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class BoilerArguments {
    public static Argument<IBoilerDisplay> displayArgument(String nodeName) {
        return new CustomArgument<IBoilerDisplay, Integer>(new IntegerArgument(nodeName), info -> {
            IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().display(Integer.parseInt(info.input()));
            if (display == null) {
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(new CustomArgument.MessageBuilder("Unknown display: ").appendArgInput());
            } else {
                return display;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info -> {
            return BoilerPlugin.getPlugin().displayManager().displays().stream().map(IBoilerDisplay::id).map(String::valueOf).toArray(String[]::new);
        }));
    }
}
