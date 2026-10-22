/* Copyright (c) 2025. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.commands.BoilerArguments;
import net.somewhatcity.boiler.core.commands.CommandContextUtil;
import org.bukkit.entity.Player;

import java.util.Arrays;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DisplayInfoCommand {

    private final BoilerPlugin plugin;

    public DisplayInfoCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("info")
                .then(BoilerArguments.displayArgument(plugin).executes(context -> {
                    IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    String viewers = Arrays.toString(display.viewers().stream().map(Player::getName).toArray());
                    Util.sendMsg(context.getSource().getSender(), "Viewers: %s", viewers);
                    return SINGLE_SUCCESS;
                }));
    }
}
