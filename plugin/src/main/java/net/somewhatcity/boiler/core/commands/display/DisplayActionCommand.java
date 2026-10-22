/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public final class DisplayActionCommand {

    private final BoilerPlugin plugin;

    public DisplayActionCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("action")
                .requires(source -> source.getSender().hasPermission("boiler.command.action"))
                .then(BoilerArguments.displayArgument(plugin)
                        .then(click())
                        .then(input())
                        .then(key())
                        .executes(context -> {
                            Util.sendMsg(context.getSource().getSender(), "Please specify an action");
                            return SINGLE_SUCCESS;
                        }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> click() {
        return Commands.literal("click")
                .then(Commands.argument("x", integer())
                        .then(Commands.argument("y", integer())
                                .executes(context -> executeClick(context, false))
                                .then(Commands.argument("rightClick", bool())
                                        .executes(context -> executeClick(context, getBool(context, "rightClick"))))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> input() {
        return Commands.literal("input")
                .then(Commands.argument("string", greedyString()).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    display.onInput(context.getSource().getSender(), getString(context, "string"));
                    Util.sendMsg(context.getSource().getSender(), "Executing input on display %s", display.id());
                    return SINGLE_SUCCESS;
                }));
    }

    private LiteralArgumentBuilder<CommandSourceStack> key() {
        return Commands.literal("key")
                .then(Commands.argument("key", greedyString()).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    display.onKey(context.getSource().getSender(), getString(context, "key"));
                    Util.sendMsg(context.getSource().getSender(), "Executing key on display %s", display.id());
                    return SINGLE_SUCCESS;
                }));
    }

    private int executeClick(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, boolean rightClick
    ) {
        IBoilerDisplay display = CommandContextUtil.display(context, plugin);
        if (display == null) return SINGLE_SUCCESS;
        display.onClick(context.getSource().getSender(), getInteger(context, "x"), getInteger(context, "y"), rightClick);
        Util.sendMsg(context.getSource().getSender(), "Executing click on display %s", display.id());
        return SINGLE_SUCCESS;
    }
}
