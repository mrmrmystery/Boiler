/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.commands.display.*;

public final class DisplayCommand {

    private final BoilerPlugin plugin;

    public DisplayCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("display")
                .then(new DisplayCreateCommand(plugin).build())
                .then(new DisplayRemoveCommand(plugin).build())
                .then(new DisplayListCommand(plugin).build())
                .then(new DisplayIdentifyCommand(plugin).build())
                .then(new DisplaySourceCommand(plugin).build())
                .then(new DisplaySettingsCommand(plugin).build())
                .then(new DisplayActionCommand(plugin).build())
                .then(new DisplaySpeakerCommand(plugin).build())
                .then(new DisplayInfoCommand(plugin).build());
    }
}
