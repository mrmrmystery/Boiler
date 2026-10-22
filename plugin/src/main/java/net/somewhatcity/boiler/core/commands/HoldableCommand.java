/* Copyright (c) 2024. */

package net.somewhatcity.boiler.core.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.pianoman911.mapengine.api.clientside.IHoldableDisplay;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public final class HoldableCommand {

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("holdable")
                .requires(source -> source.getExecutor() instanceof Player)
                .executes(context -> {
                    Player player = (Player) context.getSource().getExecutor();
                    IHoldableDisplay holdableDisplay = BoilerPlugin.MAP_ENGINE.displayProvider().createHoldableDisplay();
                    ItemStack mapItem = holdableDisplay.itemStack(1);
                    player.getInventory().addItem(mapItem);
                    return SINGLE_SUCCESS;
                });
    }
}
