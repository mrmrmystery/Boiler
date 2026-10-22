/* Copyright (c) 2025. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class BoilerClientCommand {

    private final BoilerPlugin plugin;

    public BoilerClientCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("client")
                .then(Commands.literal("credentials")
                        .requires(source -> source.getExecutor() instanceof Player)
                        .executes(context -> {
                            Player player = (Player) context.getSource().getExecutor();
                            if (plugin.mediaMtxAuthServer() == null) {
                                Util.sendErrMsg(player, "The client streaming feature is disabled");
                                return SINGLE_SUCCESS;
                            }

                            String username = "bsc_" + player.getName().replace("_", "").toLowerCase(Locale.ROOT);
                            String password = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
                            plugin.mediaMtxAuthServer().registerUser(username, password);

                            Util.sendMsg(player, "Your credentials for streaming to the server:");
                            Util.sendMsg(player, "Username: %s", username);
                            Util.sendMsg(player, "Password: %s", password);
                            return SINGLE_SUCCESS;
                        }));
    }
}
