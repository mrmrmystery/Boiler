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
        withSubcommand(new CommandAPICommand("bundle")
                .withArguments(new BooleanArgument("enabled"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    boolean value = (boolean) args.get(1);
                    display.settings().addProperty("bundle", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "bundle", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("viewDistance")
                .withArguments(new IntegerArgument("distance"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    int value = (int) args.get(1);
                    display.settings().addProperty("viewDistance", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "viewDistance", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("soundDistance")
                .withArguments(new IntegerArgument("distance"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    int value = (int) args.get(1);
                    display.settings().addProperty("soundDistance", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "soundDistance", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("glowing")
                .withArguments(new BooleanArgument("enabled"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    int value = (int) args.get(1);
                    display.settings().addProperty("glowing", value);
                    display.save();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "glowing", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("glowing")
                .withArguments(new BooleanArgument("enabled"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    boolean value = (boolean) args.get(1);
                    display.settings().addProperty("glowing", value);
                    display.save();
                    display.respawn();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "glowing", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("itemRotation")
                .withArguments(new IntegerArgument("rotation"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    int value = (int) args.get(1);
                    display.settings().addProperty("itemRotation", value);
                    display.save();
                    display.respawn();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "itemRotation", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("visualDirection")
                .withArguments(new MultiLiteralArgument("direction", "UP", "DOWN", "NORTH", "EAST", "SOUTH", "WEST"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);
                    String value = (String) args.get(1);
                    display.settings().addProperty("visualDirection", value);
                    display.save();
                    display.respawn();

                    Util.sendMsg(sender, "Set %s to %s for display %s", "visualDirection", value, display.id());
                }))
        );
        withSubcommand(new CommandAPICommand("rotation")
                .withArguments(new FloatArgument("yaw"))
                .withArguments(new FloatArgument("pitch"))
                .executes(((sender, args) -> {
                    IBoilerDisplay display = (IBoilerDisplay) args.get(0);

                    float yaw = (float) args.get(1);
                    float pitch = (float) args.get(2);

                    JsonObject rotation = new JsonObject();
                    rotation.addProperty("yaw", yaw);
                    rotation.addProperty("pitch", pitch);

                    display.settings().add("rotation", rotation);
                    display.save();
                    display.respawn();
                    Util.sendMsg(sender, "Set %s to %s,%s for display %s", "rotation", yaw, pitch, display.id());
                }))
        );
        executes(((sender, args) -> {
            Util.sendErrMsg(sender, "Please specify an option");
        }));
    }
}
