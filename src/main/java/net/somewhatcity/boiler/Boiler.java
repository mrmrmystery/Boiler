package net.somewhatcity.boiler;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.somewhatcity.boiler.commands.BoilerCommand;
import net.somewhatcity.boiler.db.DB;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.util.Metrics;
import net.somewhatcity.boiler.util.Webserver;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Boiler extends JavaPlugin {

    private static Boiler plugin;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info("Boiler is warming up!");
        new Metrics(this, 18926);
        saveDefaultConfig();
        Logger.getLogger("org.hibernate").setLevel(Level.OFF);
        DB.init();
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(false));
        new BoilerCommand();

        PluginManager m = Bukkit.getPluginManager();
        MapDisplayManager.loadAll();

        Webserver.start();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static Boiler getPlugin() {
        return plugin;
    }

}
