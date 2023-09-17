package net.somewhatcity.boiler.commands;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.util.MapTraceResult;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.api.BoilerCreateCommand;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

public class BoilerCommand extends CommandAPICommand {

    public static final MapEngineApi MAP_ENGINE = Bukkit.getServicesManager().getRegistration(MapEngineApi.class).getProvider();

    public BoilerCommand() {
        super("boiler");
        withSubcommand(new CommandAPICommand("create")
                .withPermission("boiler.command.create")
                .withArguments(new LocationArgument("locationA", LocationType.BLOCK_POSITION))
                .withArguments(new LocationArgument("locationB", LocationType.BLOCK_POSITION))
                .executesPlayer((player, args) -> {
                    BlockFace facing = player.getTargetBlockFace(10);

                    Location lookloc1 = (Location) args.get(0);
                    Location loc1 = lookloc1.getBlock().getRelative(facing).getLocation();

                    Location lookloc2 = (Location) args.get(1);
                    Location loc2 = lookloc2.getBlock().getRelative(facing).getLocation();

                    MapDisplayManager.createNew(loc1, loc2, facing, "NONE", "{}");
                })
        );
        withSubcommand(new CommandAPICommand("delete")
                .withPermission("boiler.command.delete")
                .withArguments(new IntegerArgument("id"))
                .executesPlayer((player, args) -> {
                    MapDisplayManager.delete((int) args.get(0));
                    MessageUtil.sendGreen(player, "MapDisplay %s has been deleted.", args.get(0));
                })
        );
        withSubcommand(new CommandAPICommand("list")
                .withPermission("boiler.command.list")
                .executesPlayer((player, args) -> {
                    String displays = "";
                    for(LoadedMapDisplay display : MapDisplayManager.getLoadedMapDisplays()) {
                        displays += " [" + display.getId() + "] ";
                    }
                    MessageUtil.sendGreen(player, "MapDisplays: %s", displays);
                })
        );

        List<CommandAPICommand> subCommands = new ArrayList<>();
        for (Map.Entry<String, Class<?>> sources : MapDisplayManager.getSourceList().entrySet()) {
            Class<?> source = sources.getValue();
            Method method = null;
            for(Method m : source.getMethods()) {
                if(m.isAnnotationPresent(BoilerCreateCommand.class)) {
                    method = m;
                    break;
                }
            }

            try {
                List<Argument<?>> commandArguments = new ArrayList<>();
                if(method != null) commandArguments = (List<Argument<?>>) method.invoke(null);
                subCommands.add(new CommandAPICommand(sources.getKey())
                        .withArguments(commandArguments)
                        .executesPlayer((player, args) -> {
                            LoadedMapDisplay display = (LoadedMapDisplay) args.get(0);
                            Object[] arguments = args.args();
                            JsonObject data = new JsonObject();
                            args.argsMap().forEach((key, value) -> {
                                if(!key.equals("display")) data.addProperty(key, String.valueOf(value));
                            });
                            MapDisplayManager.setSource(display.getId(), sources.getKey(), data.toString());
                        })
                );
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        withSubcommand(new CommandAPICommand("setsource")
                .withPermission("boiler.command.setsource")
                .withArguments(mapDisplayArgument("display"))
                .withSubcommands(subCommands.toArray(new CommandAPICommand[0]))
                .executesPlayer((player, args) -> {
                    LoadedMapDisplay display = (LoadedMapDisplay) args.get(0);
                    MapDisplayManager.setSource(display.getId(), null, "{}");
                })
        );
        withSubcommand(new CommandAPICommand("identify")
                .withPermission("boiler.command.identify")
                .executesPlayer((player, args) -> {
                    MapTraceResult result = MAP_ENGINE.traceDisplayInView(player, 10);
                    if(result == null) {
                        MessageUtil.sendRed(player, "No MapDisplay found.");
                    } else {
                        IMapDisplay display = result.display();
                        LoadedMapDisplay ldm = MapDisplayManager.getLoadedMapDisplay(display);
                        if(ldm == null) {
                            MessageUtil.sendRed(player, "This display was not created by boiler");
                        } else {
                            MessageUtil.sendGreen(player, "Looking at MapDisplay %s", ldm.getId());
                        }
                    }
                })
        );

        register();
    }

    public Argument<LoadedMapDisplay> mapDisplayArgument(String nodeName) {
        return new CustomArgument<LoadedMapDisplay, String>(new StringArgument(nodeName), info -> {
            LoadedMapDisplay display = MapDisplayManager.getLoadedMapDisplay(Integer.parseInt(info.input()));
            if (display == null) {
                throw CustomArgument.CustomArgumentException.fromMessageBuilder(new CustomArgument.MessageBuilder("Unknown display: ").appendArgInput());
            } else {
                return display;
            }
        }).replaceSuggestions(ArgumentSuggestions.strings(info ->
                MapDisplayManager.getLoadedMapDisplays().stream().map(LoadedMapDisplay::getId).map(String::valueOf).toArray(String[]::new)
        ));
    }
}
