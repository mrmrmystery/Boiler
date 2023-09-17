package net.somewhatcity.boiler;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import net.somewhatcity.boiler.commands.BoilerCommand;
import net.somewhatcity.boiler.db.DB;
import net.somewhatcity.boiler.display.MapDisplayManager;
import net.somewhatcity.boiler.display.sources.*;
import net.somewhatcity.boiler.display.sources.boilerClientSource.BoilerClientSource;
import net.somewhatcity.boiler.util.Assets;
import net.somewhatcity.boiler.util.BoilerConfig;
import net.somewhatcity.boiler.util.Metrics;
import net.somewhatcity.boiler.util.Webserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class Boiler extends JavaPlugin {

    public static final String PLUGIN_ID = "boiler";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    public static boolean DEBUG = false;

    private static Boiler plugin;

    private BoilerVoicechatPlugin voicechatPlugin;

    @Override
    public void onEnable() {
        plugin = this;
        getLogger().info("Boiler is warming up!");
        new Metrics(this, 18926);
        saveDefaultConfig();
        new BoilerConfig(this);
        DEBUG = getConfig().getBoolean("debug", false);

        BukkitVoicechatService vcService = getServer().getServicesManager().load(BukkitVoicechatService.class);
        if(vcService != null) {
            voicechatPlugin = new BoilerVoicechatPlugin();
            vcService.registerPlugin(voicechatPlugin);
            LOGGER.info("VoiceChat found, voice chat integration enabled");
        } else {
            LOGGER.info("VoiceChat not found, voice chat integration disabled");
        }

        File mediaFolder = new File(getDataFolder(), "media");
        if (!mediaFolder.exists()) mediaFolder.mkdirs();
        Assets.load();
        DB.init();

        MapDisplayManager.registerSource("image", ImageSource.class);
        MapDisplayManager.registerSource("gif", GIFSource.class);
        MapDisplayManager.registerSource("file", LocalFileSource.class);
        MapDisplayManager.registerSource("whiteboard", WhiteboardSource.class);
        MapDisplayManager.registerSource("web", WebSource.class);
        MapDisplayManager.registerSource("settings", SettingSource.class);
        MapDisplayManager.registerSource("uitest", CustomUISource.class);
        MapDisplayManager.registerSource("prerenderedVideo", PrerenderedVideoSource.class);
        MapDisplayManager.registerSource("boilerClient", BoilerClientSource.class);

        CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(DEBUG));
        new BoilerCommand();

        Webserver.start();

        if(vcService == null) {
            Bukkit.getScheduler().runTask(this, () -> {
                MapDisplayManager.loadAll();
                Boiler.LOGGER.info("Loaded " + MapDisplayManager.getLoadedMapDisplays().size() + " displays");
            });
        }
    }

    @Override
    public void onDisable() {
        MapDisplayManager.unloadAll();
        DB.disconnect();
    }

    public static Boiler getPlugin() {
        return plugin;
    }

}
