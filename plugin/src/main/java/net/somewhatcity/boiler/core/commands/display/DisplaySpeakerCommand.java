/* Copyright (c) 2025. */

package net.somewhatcity.boiler.core.commands.display;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.commands.BoilerArguments;
import net.somewhatcity.boiler.core.commands.CommandContextUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class DisplaySpeakerCommand {

    private final BoilerPlugin plugin;

    public DisplaySpeakerCommand(BoilerPlugin plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("speaker")
                .requires(source -> source.getExecutor() instanceof Player)
                .then(BoilerArguments.displayArgument(plugin)
                        .then(Commands.literal("add")
                                .then(Commands.argument("location", ArgumentTypes.blockPosition())
                                        .executes(context -> {
                                            IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                                            if (display == null) return SINGLE_SUCCESS;
                                            BlockPositionResolver resolver = context.getArgument("location", BlockPositionResolver.class);
                                            BlockPosition position = resolver.resolve(context.getSource());
                                            Location origin = context.getSource().getLocation();
                                            display.addSpeaker(new Location(origin.getWorld(), position.x(), position.y(), position.z()));
                                            display.save();
                                            return SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("clear").executes(context -> {
                            IBoilerDisplay display = CommandContextUtil.display(context, plugin);
                            if (display == null) return SINGLE_SUCCESS;
                            display.clearSpeakers();
                            display.save();
                            return SINGLE_SUCCESS;
                        }))
                        .executes(context -> {
                            context.getSource().getSender().sendMessage("please specify an action!");
                            return SINGLE_SUCCESS;
                        }));
    }
}
