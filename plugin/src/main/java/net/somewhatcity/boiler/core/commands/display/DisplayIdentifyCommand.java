/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.mapengine.api.util.MapTraceResult;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public final class DisplayIdentifyCommand {

    private final BoilerPlugin plugin;

    public DisplayIdentifyCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("identify")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getExecutor();
                    MapTraceResult result = MAP_ENGINE.traceDisplayInView(player, 10);
                    if (result == null) {
                        Util.sendErrMsg(player, "No display in view");
                        return SINGLE_SUCCESS;
                    }
                    IBoilerDisplay display = plugin.displayManager().display(result.display());
                    if (display == null) {
                        Util.sendErrMsg(player, "No boiler display (could be from another plugin)");
                        return SINGLE_SUCCESS;
                    }
                    Util.sendMsg(player, "Found display with id %s", display.id());
                    return SINGLE_SUCCESS;
                });
    }
}
