/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;
import net.somewhatcity.boiler.core.commands.CommandContextUtil;

import java.util.Map;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DisplaySourceCommand {

    private final BoilerPlugin plugin;

    public DisplaySourceCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> source = Commands.literal("source")
                .requires(commandSource -> commandSource.getSender().hasPermission("boiler.command.source"));
        ArgumentBuilder<CommandSourceStack, ?> display = BoilerArguments.displayArgument(plugin)
                .executes(context -> resetSource(context));

        for (Map.Entry<String, Class<? extends IBoilerSource>> entry : plugin.sourceManager().sources().entrySet()) {
            if (!plugin.sourceManager().commandVisibleSourceNames().contains(entry.getKey())) continue;
            display.then(sourceCommand(entry.getKey(), entry.getValue()));
        }
        return source.then(display);
    }

    private ArgumentBuilder<CommandSourceStack, ?> sourceCommand(
            String name, Class<? extends IBoilerSource> sourceClass
    ) {
        CreateCommandArguments annotation = sourceClass.getAnnotation(CreateCommandArguments.class);
        CreateArgument[] arguments = annotation == null ? new CreateArgument[0] : annotation.arguments();
        return appendArguments(Commands.literal(name), arguments, 0, name);
    }

    private ArgumentBuilder<CommandSourceStack, ?> appendArguments(
            ArgumentBuilder<CommandSourceStack, ?> parent,
            CreateArgument[] arguments,
            int index,
            String sourceName
    ) {
        if (index == arguments.length) {
            return parent.executes(context -> setSource(context, sourceName, arguments));
        }
        return parent.then(appendArguments(
                BoilerArguments.sourceArgument(arguments[index]), arguments, index + 1, sourceName
        ));
    }

    private int resetSource(CommandContext<CommandSourceStack> context) {
        IBoilerDisplay display = CommandContextUtil.display(context, plugin);
        if (display == null) return SINGLE_SUCCESS;
        display.source("default", new JsonObject());
        Util.sendMsg(context.getSource().getSender(), "Reset source of display %s", display.id());
        return SINGLE_SUCCESS;
    }

    private int setSource(
            CommandContext<CommandSourceStack> context, String sourceName, CreateArgument[] arguments
    ) {
        IBoilerDisplay display = CommandContextUtil.display(context, plugin);
        if (display == null) return SINGLE_SUCCESS;
        display.source(sourceName, BoilerArguments.sourceData(context, arguments));
        Util.sendMsg(context.getSource().getSender(), "Setting source for display %s to %s", display.id(), sourceName);
        return SINGLE_SUCCESS;
    }
}
