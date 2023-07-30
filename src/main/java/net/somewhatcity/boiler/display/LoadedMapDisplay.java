package net.somewhatcity.boiler.display;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.api.event.MapClickEvent;
import de.pianoman911.mapengine.api.util.Converter;
import de.pianoman911.mapengine.api.util.ImageUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.db.SMapDisplay;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.BlockVector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

public class LoadedMapDisplay implements Listener {

    public static final MapEngineApi MAP_ENGINE = Bukkit.getServicesManager().getRegistration(MapEngineApi.class).getProvider();

    private int id;
    private Location locationA;
    private Location locationB;
    private BlockFace facing;

    private BoilerSource selectedSource;
    private IMapDisplay mapDisplay;
    private IDrawingSpace drawingSpace;
    public List<Player> VISIBLE_FOR = new ArrayList<>();
    private int ping_limit = 100;
    private int viewDistance = 100;
    private JsonObject settings;
    private JsonObject options;
    private Thread displayUpdateThread;
    private boolean defaultRendering = true;

    public LoadedMapDisplay(SMapDisplay sMapDisplay) {
        this.locationA = sMapDisplay.getLocationA();
        this.locationB = sMapDisplay.getLocationB();
        this.facing = sMapDisplay.getFacing();
        this.id = sMapDisplay.getId();
        //settings = JsonParser.parseString(sMapDisplay.getDisplaySettings()).getAsJsonObject();
        options = JsonParser.parseString(sMapDisplay.getDisplaySettings()).getAsJsonObject();
        viewDistance = Boiler.getPlugin().getConfig().getInt("view_distance", 100);
        ping_limit = Boiler.getPlugin().getConfig().getInt("ping_limit", 100);

        BlockVector pointA = locationA.toVector().toBlockVector();
        BlockVector pointB = locationB.toVector().toBlockVector();
        mapDisplay = MAP_ENGINE.displayProvider().createBasic(pointA, pointB, facing);
        drawingSpace = MAP_ENGINE.pipeline().drawingSpace(mapDisplay);

        reloadOptions();

        setSource(sMapDisplay.getSourceName(), sMapDisplay.getSavedData());

        displayUpdateThread = new Thread(() -> {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        render();
                    } catch (Exception e) {
                        Boiler.getPlugin().getLogger().log(Level.SEVERE, "Error while rendering map display " + id, e);
                    }
                }
            }, 0, 1000 / 20);
        });
        displayUpdateThread.setDaemon(true);
        displayUpdateThread.start();

        Bukkit.getPluginManager().registerEvents(this, Boiler.getPlugin());
    }

    public void reloadOptions() {
        drawingSpace.ctx().buffering(getOption("caching", true));

        if(getOption("dithering", false)) {
            drawingSpace.ctx().converter(Converter.FLOYD_STEINBERG);
        } else {
            drawingSpace.ctx().converter(Converter.DIRECT);
        }
    }

    public void setOption(String key, Object value) {
        options.addProperty(key, String.valueOf(value));
        MapDisplayManager.setSettings(id, options);
    }

    public Object getOption(String key, Object defaultValue) {
        if(!options.has(key)) return defaultValue;
        return options.get(key);
    }

    public boolean getOption(String key, boolean defaultValue) {
        if(!options.has(key)) return defaultValue;
        return options.get(key).getAsBoolean();
    }

    public int getOption(String key, int defaultValue) {
        if(!options.has(key)) return defaultValue;
        return options.get(key).getAsInt();
    }

    public String getOption(String key, String defaultValue) {
        if(!options.has(key)) return defaultValue;
        return options.get(key).getAsString();
    }

    public int getId() {
        return id;
    }

    public void tick() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(locationA.distance(player.getLocation()) < viewDistance) {
                if(!VISIBLE_FOR.contains(player)) {
                    VISIBLE_FOR.add(player);
                    mapDisplay.spawn(player);
                }
            }else {
                VISIBLE_FOR.remove(player);

                mapDisplay.despawn(player);
            }
        });

        VISIBLE_FOR.removeIf(player -> !player.isOnline());
    }

    public void render() {
        if(VISIBLE_FOR.isEmpty()) return;

        drawingSpace.ctx().receivers().clear();
        for(Player player : VISIBLE_FOR) {
            if(player.getPing() < ping_limit) {
                drawingSpace.ctx().receivers().add(player);
            } else {
                player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Ping too high to render map display!"));
            }
        }
        int[] rgb;

        if(!defaultRendering) return;

        if(selectedSource == null) {
            BufferedImage info = new BufferedImage(mapDisplay.width() * 128, mapDisplay.height() * 128, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = info.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, mapDisplay.width() * 128, mapDisplay.height() * 128);
            Font font = new Font("Arial", Font.BOLD, 10);
            graphics.setFont(font);
            graphics.setColor(Color.WHITE);
            graphics.drawString("No source selected", 10, 20);
            graphics.drawString("MapDisplay " + id, 10, 50);
            graphics.dispose();

            rgb = ImageUtils.rgb(info);
        }else {
            BufferedImage image = selectedSource.image();
            if(image == null) {
                image = new BufferedImage(mapDisplay.width() * 128, mapDisplay.height() * 128, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, mapDisplay.width() * 128, mapDisplay.height() * 128);
                Font font = new Font("Arial", Font.BOLD, 10);
                graphics.setFont(font);
                graphics.setColor(Color.WHITE);
                graphics.drawString("Waiting for image...", 10, 20);
                graphics.dispose();
            }

            image = ImageUtils.resize(image, mapDisplay.width() * 128, mapDisplay.height() * 128);
            rgb = ImageUtils.rgb(image);
        }

        drawingSpace.pixels(rgb, 0, 0, mapDisplay.width() * 128, mapDisplay.height() * 128);
        drawingSpace.flush();
    }

    @EventHandler
    public void onClick(MapClickEvent e) {
        if(!e.display().equals(mapDisplay)) return;
        selectedSource.onclick(e.x(), e.y(), e.player());
    }

    public void delete() {
        if(selectedSource != null) selectedSource.unload();
        displayUpdateThread.interrupt();
        VISIBLE_FOR.forEach(player -> mapDisplay.despawn(player));
    }

    public void setSource(String sourceName, String savedData) {
        if(sourceName == null) {
            if(selectedSource != null) selectedSource.unload();
            selectedSource = null;
            return;
        }

        try {
            if(selectedSource != null) selectedSource.unload();

            Class<?> sourceClass = MapDisplayManager.getSource(sourceName);
            if(sourceClass == null) {
                Boiler.getPlugin().getLogger().log(Level.WARNING, "Failed to load source for map display " + id + " (source not found)");
                return;
            }

            JsonObject data = JsonParser.parseString(savedData).getAsJsonObject();
            BoilerSource source = (BoilerSource) sourceClass.getDeclaredConstructor().newInstance();
            source.load(this, data);
            selectedSource = source;
        } catch (Exception ex) {
            Boiler.getPlugin().getLogger().log(Level.WARNING, "Failed to load source for map display " + id, ex);
        }
    }

    public IDrawingSpace getDrawingSpace() {
        return drawingSpace;
    }

    public BoilerSource getSelectedSource() {
        return selectedSource;
    }

    public IMapDisplay getMapDisplay() {
        return mapDisplay;
    }

    public JsonObject getSettings() {
        return settings;
    }

    public Location getCenter() {
        return locationA.clone().add(locationB).multiply(0.5);
    }

    public World getWorld() {
        return locationA.getWorld();
    }

    public void setDefaultRendering(boolean defaultRendering) {
        this.defaultRendering = defaultRendering;
    }
}
