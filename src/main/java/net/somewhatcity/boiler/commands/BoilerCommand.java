package net.somewhatcity.boiler.commands;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.util.MapTraceResult;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.display.sources.Source;
import net.somewhatcity.boiler.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class BoilerCommand extends CommandAPICommand {
    public BoilerCommand() {
        super("boiler");
        withSubcommand(new CommandAPICommand("create")
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
                .withArguments(new IntegerArgument("id"))
                .executesPlayer((player, args) -> {
                    MapDisplayManager.delete((int) args.get(0));
                    MessageUtil.sendGreen(player, "MapDisplay %s has been deleted.", args.get(0));
                })
        );
        withSubcommand(new CommandAPICommand("list")
                .executesPlayer((player, args) -> {
                    String displays = "";
                    for(LoadedMapDisplay display : MapDisplayManager.getLoadedMapDisplays()) {
                        displays += " [" + display.getId() + "] ";
                    }
                    MessageUtil.sendGreen(player, "MapDisplays: %s", displays);
                })
        );
        withSubcommand(new CommandAPICommand("setsource")
                .withSubcommand(new CommandAPICommand("image")
                        .withArguments(new IntegerArgument("id"))
                        .withArguments(new GreedyStringArgument("url"))
                        .executesPlayer((player, args) -> {

                            JsonObject json = new JsonObject();
                            json.addProperty("url", (String) args.get(1));
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "IMAGE", savedData);
                            }).start();
                            MessageUtil.sendGreen(player, "MapDisplay %s is now using %s as source.", args.get(0), "IMAGE");
                        })
                )
                .withSubcommand(new CommandAPICommand("gif")
                        .withArguments(new IntegerArgument("id"))
                        .withArguments(new GreedyStringArgument("url"))
                        .executesPlayer((player, args) -> {

                            JsonObject json = new JsonObject();
                            json.addProperty("url", (String) args.get(1));
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "GIF", savedData);
                            }).start();


                            MessageUtil.sendGreen(player, "MapDisplay %s is now using %s as source.", args.get(0), "GIF");
                        })
                )
                .withSubcommand(new CommandAPICommand("whiteboard")
                        .withArguments(new IntegerArgument("id"))
                        .executesPlayer((player, args) -> {

                            JsonObject json = new JsonObject();
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "WHITEBOARD", savedData);
                            }).start();
                            MessageUtil.sendGreen(player, "MapDisplay %s his now using %s as source.", args.get(0), "WHITEBOARD");
                        })
                )
                .withSubcommand(new CommandAPICommand("web")
                        .withArguments(new IntegerArgument("id"))
                        .executesPlayer((player, args) -> {

                            JsonObject json = new JsonObject();
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "WEB", savedData);
                            }).start();
                            MessageUtil.sendGreen(player, "MapDisplay %s is now using %s as source.", args.get(0), "WEB");
                        })
                )
                .withSubcommand(new CommandAPICommand("file")
                        .withArguments(new IntegerArgument("id"))
                        .withArguments(new GreedyStringArgument("file"))
                        .executesPlayer((player, args) -> {

                            JsonObject json = new JsonObject();
                            json.addProperty("file", args.get(1).toString());
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "FILE", savedData);
                            }).start();
                            MessageUtil.sendGreen(player, "MapDisplay %s is now using %s as source.", args.get(0), "FILE");
                        })
                )
                .withSubcommand(new CommandAPICommand("settings")
                        .withArguments(new IntegerArgument("id"))
                        .executesPlayer((player, args) -> {
                            JsonObject json = new JsonObject();
                            String savedData = json.toString();

                            new Thread(() -> {
                                MapDisplayManager.setSource((int) args.get(0), "SETTINGS", savedData);
                            }).start();
                            MessageUtil.sendGreen(player, "MapDisplay %s is now using %s as source.", args.get(0), "FILE");
                        })
                )
        );
        withSubcommand(new CommandAPICommand("identify")
                .withPermission("boiler.identify")
                .executesPlayer((player, args) -> {
                    MapTraceResult result = LoadedMapDisplay.MAP_ENGINE.traceDisplayInView(player, 10);
                    if(result == null) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No MapDisplay found!"));
                        return;
                    }
                    LoadedMapDisplay loadedMapDisplay = MapDisplayManager.getLoadedMapDisplay(result.display());
                    if(loadedMapDisplay == null) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<red>No MapDisplay found / not from this plugin"));
                        return;
                    }
                    player.sendMessage(MiniMessage.miniMessage().deserialize("<green>MapDisplay " + loadedMapDisplay.getId()));
                })
        );
        register();
    }
}
