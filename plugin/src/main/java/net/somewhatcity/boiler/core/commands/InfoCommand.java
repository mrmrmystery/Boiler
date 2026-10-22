/* Copyright (c) 2025. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class InfoCommand {

    private final BoilerPlugin plugin;

    public InfoCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("info")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getExecutor();
                    List<IBoilerDisplay> displays = plugin.displayManager().displays().stream()
                            .filter(display -> display.viewers().contains(player))
                            .toList();
                    String displayList = Arrays.toString(displays.stream().map(IBoilerDisplay::id).toArray());
                    Util.sendMsg(player, "You are receiver of %s displays:\n%s", displays.size(), displayList);
                    return SINGLE_SUCCESS;
                });
    }
}
