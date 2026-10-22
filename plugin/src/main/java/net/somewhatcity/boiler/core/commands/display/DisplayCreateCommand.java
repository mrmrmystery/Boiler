/* Copyright (c) 2023. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DisplayCreateCommand {

    private final BoilerPlugin plugin;

    public DisplayCreateCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("create")
                .requires(source -> source.getSender().hasPermission("boiler.command.create"))
                .requires(source -> source.getExecutor() instanceof Player)
                .then(Commands.argument("posA", ArgumentTypes.blockPosition())
                        .then(Commands.argument("posB", ArgumentTypes.blockPosition())
                                .executes(context -> {
                                    Player player = (Player) context.getSource().getExecutor();
                                    BlockFace face = player.getTargetBlockFace(10);
                                    Location a = position(context, "posA").getBlock().getRelative(face).getLocation();
                                    Location b = position(context, "posB").getBlock().getRelative(face).getLocation();
                                    IBoilerDisplay display = plugin.displayManager().createDisplay(a, b, face);
                                    if (display == null) {
                                        Util.sendErrMsg(player, "Could not create display");
                                    } else {
                                        Util.sendMsg(player, "Created new display with id %s", display.id());
                                    }
                                    return SINGLE_SUCCESS;
                                })));
    }

    private static Location position(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, String name
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        BlockPositionResolver resolver = context.getArgument(name, BlockPositionResolver.class);
        BlockPosition position = resolver.resolve(context.getSource());
        Location origin = context.getSource().getLocation();
        return new Location(origin.getWorld(), position.x(), position.y(), position.z());
    }
}
