/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DisplayListCommand {

    private final BoilerPlugin plugin;

    public DisplayListCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("list")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getExecutor();
                    StringBuilder message = new StringBuilder();
                    plugin.displayManager().displays().forEach(display -> {
                        String tpCommand = "/tp @s %s %s %s".formatted(
                                display.cornerA().x(), display.cornerA().y(), display.cornerA().z());
                        String tooltip = "<blue><b>source:</b> <white>%s".formatted(display.sourceName());
                        message.append("<click:run_command:'%s'><hover:show_text:'%s'><b>[%s]</b></hover></click> "
                                .formatted(tpCommand, tooltip, display.id()));
                    });

                    if (message.isEmpty()) {
                        Util.sendMsg(player, "There are no boiler displays");
                    } else {
                        Util.sendMsg(player, "All boiler displays:");
                        player.sendMessage(MiniMessage.miniMessage().deserialize(message.toString()));
                    }
                    return SINGLE_SUCCESS;
                });
    }
}
