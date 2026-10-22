/* Copyright (c) 2026. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;

public final class CommandContextUtil {

    private CommandContextUtil() {
    }

    public static Player player(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getExecutor() instanceof Player player) {
            return player;
        }
        Util.sendErrMsg(context.getSource().getSender(), "This command can only be used by a player");
        return null;
    }

    public static IBoilerDisplay display(CommandContext<CommandSourceStack> context, BoilerPlugin plugin) {
        int id = getInteger(context, "display");
        IBoilerDisplay display = plugin.displayManager().display(id);
        if (display == null) {
            Util.sendErrMsg(context.getSource().getSender(), "Unknown display: %s", id);
        }
        return display;
    }
}
