/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;
import net.somewhatcity.boiler.core.commands.CommandContextUtil;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public final class DisplaySettingsCommand {

    private static final String[] DIRECTIONS = {"UP", "DOWN", "NORTH", "EAST", "SOUTH", "WEST"};

    private final BoilerPlugin plugin;

    public DisplaySettingsCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("settings")
                .requires(source -> source.getSender().hasPermission("boiler.command.settings"))
                .then(BoilerArguments.displayArgument(plugin)
                        .then(booleanSetting("dither"))
                        .then(booleanSetting("buffer"))
                        .then(integerSetting("renderPeriod", "milliseconds", false))
                        .then(booleanSetting("bundle"))
                        .then(integerSetting("viewDistance", "distance", false))
                        .then(integerSetting("soundDistance", "distance", false))
                        .then(booleanSetting("glowing"))
                        .then(integerSetting("itemRotation", "rotation", true))
                        .then(directionSetting())
                        .then(rotationSetting())
                        .executes(context -> {
                            Util.sendErrMsg(context.getSource().getSender(), "Please specify an option");
                            return SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> booleanSetting(String name) {
        return Commands.literal(name)
                .then(Commands.argument("enabled", bool()).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    boolean value = getBool(context, "enabled");
                    display.settings().addProperty(name, value);
                    display.save();
                    if (name.equals("glowing")) display.respawn();
                    sendSettingMessage(context, display, name, value);
                    return SINGLE_SUCCESS;
                }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> integerSetting(String name, String argumentName, boolean respawn) {
        return Commands.literal(name)
                .then(Commands.argument(argumentName, integer()).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    int value = getInteger(context, argumentName);
                    display.settings().addProperty(name, value);
                    display.save();
                    if (respawn) display.respawn();
                    sendSettingMessage(context, display, name, value);
                    return SINGLE_SUCCESS;
                }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> directionSetting() {
        LiteralArgumentBuilder<CommandSourceStack> direction = Commands.literal("visualDirection");
        for (String value : DIRECTIONS) {
            direction.then(Commands.literal(value).executes(context -> {
                IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                if (display == null) return SINGLE_SUCCESS;
                display.settings().addProperty("visualDirection", value);
                display.save();
                display.respawn();
                sendSettingMessage(context, display, "visualDirection", value);
                return SINGLE_SUCCESS;
            }));
        }
        return direction;
    }

    private LiteralArgumentBuilder<CommandSourceStack> rotationSetting() {
        return Commands.literal("rotation")
                .then(Commands.argument("yaw", floatArg())
                        .then(Commands.argument("pitch", floatArg()).executes(context -> {
                            IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                            if (display == null) return SINGLE_SUCCESS;
                            float yaw = getFloat(context, "yaw");
                            float pitch = getFloat(context, "pitch");
                            JsonObject rotation = new JsonObject();
                            rotation.addProperty("yaw", yaw);
                            rotation.addProperty("pitch", pitch);
                            display.settings().add("rotation", rotation);
                            display.save();
                            display.respawn();
                            Util.sendMsg(context.getSource().getSender(), "Set %s to %s,%s for display %s",
                                    "rotation", yaw, pitch, display.id());
                            return SINGLE_SUCCESS;
                        })));
    }

    private static void sendSettingMessage(
            CommandContext<CommandSourceStack> context, IBoilerDisplay display, String name, Object value
    ) {
        Util.sendMsg(context.getSource().getSender(), "Set %s to %s for display %s", name, value, display.id());
    }
}
