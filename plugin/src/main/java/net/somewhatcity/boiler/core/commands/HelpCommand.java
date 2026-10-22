/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.Util;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class HelpCommand {

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("help").executes(context -> {
            Util.sendMsg(context.getSource().getSender(), "Visit the docs %s",
                    "<u><click:open_url:'https://somewhatcity.gitbook.io/boiler-plugin/'>here</click></u>");
            return SINGLE_SUCCESS;
        });
    }
}
