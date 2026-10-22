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

public final class DisplayRemoveCommand {

    private final BoilerPlugin plugin;

    public DisplayRemoveCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("remove")
                .requires(source -> source.getExecutor() instanceof org.bukkit.entity.Player)
                .then(BoilerArguments.displayArgument(plugin).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    plugin.displayManager().removeDisplay(display);
                    Util.sendMsg(context.getSource().getSender(), "Removed display with id %s", display.id());
                    return SINGLE_SUCCESS;
                }));
    }
}
