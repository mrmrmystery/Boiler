package net.somewhatcity.boiler.display;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pianoman911.mapengine.api.MapEngineApi;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.api.util.Converter;
import de.pianoman911.mapengine.api.util.ImageUtils;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.db.SMapDisplay;
import net.somewhatcity.boiler.display.sources.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class LoadedMapDisplay {

    public static final MapEngineApi MAP_ENGINE = Bukkit.getServicesManager().getRegistration(MapEngineApi.class).getProvider();

    private int id;
    private Location locationA;
    private Location locationB;
    private BlockFace facing;

    private Source selectedSource;
    private IMapDisplay mapDisplay;
    private IDrawingSpace drawingSpace;
    public List<Player> VISIBLE_FOR = new ArrayList<>();
    private int[] lastFrame;
    private int viewDistance = 100;
    private JsonObject settings;

    public LoadedMapDisplay(SMapDisplay sMapDisplay) {
        this.locationA = sMapDisplay.getLocationA();
        this.locationB = sMapDisplay.getLocationB();
        this.facing = sMapDisplay.getFacing();
        this.id = sMapDisplay.getId();
        settings = JsonParser.parseString(sMapDisplay.getDisplaySettings()).getAsJsonObject();
        viewDistance = Boiler.getPlugin().getConfig().getInt("view_distance", 100);

        BlockVector pointA = locationA.toVector().toBlockVector();
        BlockVector pointB = locationB.toVector().toBlockVector();
        mapDisplay = MAP_ENGINE.displayProvider().createBasic(pointA, pointB, facing);
        drawingSpace = MAP_ENGINE.pipeline().drawingSpace(mapDisplay);

        setSource(sMapDisplay.getSourceType(), sMapDisplay.getSavedData());


        setSettings(settings);

        new Thread(() -> {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    render();
                }
            }, 0, 1000 / 20);
        }).start();
    }

    public void setSettings(JsonObject settings) {
        if(settings.get("caching") != null && settings.get("caching").getAsBoolean()) {
            drawingSpace.ctx().buffering(true);
        } else {
            drawingSpace.ctx().buffering(false);
        }

        if(settings.get("dithering") != null && settings.get("dithering").getAsBoolean()) {
            drawingSpace.ctx().converter(Converter.FLOYD_STEINBERG);
        } else {
            drawingSpace.ctx().converter(Converter.DIRECT);
        }
    }


    public int getId() {
        return id;
    }

    public void tick() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(locationA.distance(player.getLocation()) < 100) {
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
            if(player.getPing() < 100) drawingSpace.ctx().receivers().add(player);
        }
        int[] rgb;

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
            BufferedImage image = selectedSource.getFrame();
            if(image == null) {
                image = new BufferedImage(mapDisplay.width() * 128, mapDisplay.height() * 128, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setColor(Color.BLACK);
                graphics.fillRect(0, 0, mapDisplay.width() * 128, mapDisplay.height() * 128);
                Font font = new Font("Arial", Font.BOLD, 10);
                graphics.setFont(font);
                graphics.setColor(Color.WHITE);
                graphics.drawString("Loading...", 10, 20);
                graphics.dispose();
            }

            image = ImageUtils.resize(image, mapDisplay.width() * 128, mapDisplay.height() * 128);
            rgb = ImageUtils.rgb(image);
        }

        drawingSpace.pixels(rgb, 0, 0, mapDisplay.width() * 128, mapDisplay.height() * 128);
        drawingSpace.flush();
    }

    public void delete() {
        VISIBLE_FOR.forEach(player -> mapDisplay.despawn(player));
    }

    public void setSource(String sourceType, String savedData) {
        try {
            if(selectedSource != null) selectedSource.unload();

            JsonObject data = JsonParser.parseString(savedData).getAsJsonObject();
            switch (sourceType) {
                case "IMAGE" -> {
                    selectedSource = new ImageSource();
                }
                case "GIF" -> {
                    selectedSource = new GIFSource();
                }
                case "WHITEBOARD" -> {
                    selectedSource = new WhiteboardSource();
                }
                case "WEB" -> {
                    selectedSource = new WebSource();
                }
                case "FILE" -> {
                    selectedSource = new LocalFileSource();
                }
                case "SETTINGS" -> {
                    selectedSource = new SettingSource();
                }
                case "NONE" -> {
                    selectedSource = null;
                }
            }
            if(selectedSource != null) selectedSource.load(this, mapDisplay, data);
        } catch (Exception e) {
            System.out.println("Failed to load source for map display " + id);
            e.printStackTrace();
        }
    }

    public IDrawingSpace getDrawingSpace() {
        return drawingSpace;
    }

    public Source getSelectedSource() {
        return selectedSource;
    }

    public IMapDisplay getMapDisplay() {
        return mapDisplay;
    }

    public JsonObject getSettings() {
        return settings;
    }
}
