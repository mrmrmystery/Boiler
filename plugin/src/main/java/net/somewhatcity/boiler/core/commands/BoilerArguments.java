/* Copyright (c) 2024. */

package net.somewhatcity.boiler.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.core.BoilerPlugin;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg;
import static com.mojang.brigadier.arguments.DoubleArgumentType.getDouble;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public final class BoilerArguments {

    private BoilerArguments() {
    }

    public static RequiredArgumentBuilder<CommandSourceStack, Integer> displayArgument(BoilerPlugin plugin) {
        return Commands.argument("display", integer())
                .suggests((context, builder) -> suggestDisplays(plugin, builder));
    }

    private static CompletableFuture<Suggestions> suggestDisplays(BoilerPlugin plugin, SuggestionsBuilder builder) {
        plugin.displayManager().displays().forEach(display -> builder.suggest(display.id()));
        return builder.buildFuture();
    }

    public static ArgumentBuilder<CommandSourceStack, ?> sourceArgument(CreateArgument argument) {
        return switch (argument.type()) {
            case STRING -> Commands.argument(argument.name(), string());
            case GREEDY_STRING -> Commands.argument(argument.name(), greedyString());
            case INTEGER -> Commands.argument(argument.name(), integer());
            case BOOLEAN -> Commands.argument(argument.name(), bool());
            case DOUBLE -> Commands.argument(argument.name(), doubleArg());
        };
    }

    public static JsonObject sourceData(CommandContext<CommandSourceStack> context, CreateArgument[] arguments) {
        JsonObject data = new JsonObject();
        for (CreateArgument argument : arguments) {
            switch (argument.type()) {
                case STRING, GREEDY_STRING -> data.addProperty(argument.name(), getString(context, argument.name()));
                case INTEGER -> data.addProperty(argument.name(), getInteger(context, argument.name()));
                case BOOLEAN -> data.addProperty(argument.name(), getBool(context, argument.name()));
                case DOUBLE -> data.addProperty(argument.name(), getDouble(context, argument.name()));
            }
        }
        return data;
    }
}
