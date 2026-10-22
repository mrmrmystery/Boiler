/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class ReloadCommand {

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload");
    }
}
