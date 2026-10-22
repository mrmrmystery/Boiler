package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.remote.RemoteItem;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class RemoteCommand {
    private final BoilerPlugin plugin;
    public RemoteCommand(BoilerPlugin plugin) { this.plugin = plugin; }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("remote")
                .requires(source -> source.getSender().hasPermission("boiler.command.remote"))
                .then(BoilerArguments.displayArgument(plugin).executes(context -> {
                    Player player = CommandContextUtil.player(context);
                    if (player == null) return SINGLE_SUCCESS;
                    var display = CommandContextUtil.display(context, plugin);
                    if (display == null) return SINGLE_SUCCESS;
                    if (!player.getInventory().addItem(RemoteItem.create(display.id())).isEmpty()) {
                        Util.sendErrMsg(player, "Dein Inventar ist voll. Schaffe Platz für die Fernbedienung.");
                    } else Util.sendMsg(player, "Fernbedienung für Display %s erhalten. In der Haupthand halten; Slotwechsel beendet die Steuerung.", display.id());
                    return SINGLE_SUCCESS;
                }));
    }
}
