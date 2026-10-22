/* Copyright (c) 2024. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public final class UpdateIntervalCommand {

    private final BoilerPlugin plugin;

    public UpdateIntervalCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("updateInterval")
                .requires(source -> source.getExecutor() instanceof Player)
                .then(Commands.argument("ms", integer()).executes(context -> {
                    Player player = (Player) context.getSource().getExecutor();
                    int interval = getInteger(context, "ms");
                    plugin.intervalManager().setInterval(player, interval);
                    Util.sendMsg(player, "Set update interval to %sms", interval);
                    return SINGLE_SUCCESS;
                }));
    }
}
