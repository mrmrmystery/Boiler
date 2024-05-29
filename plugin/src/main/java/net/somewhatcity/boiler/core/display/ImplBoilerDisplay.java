/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.display;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.api.util.Converter;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerConfig;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class ImplBoilerDisplay implements IBoilerDisplay {

    private static final Executor SCENE_EXECUTOR = Executors.newCachedThreadPool();
    private final int ID;
    private IMapDisplay MAP_DISPLAY;
    private final Location CORNER_A;
    private final Location CORNER_B;
    private final BlockFace FACING;
    private IDrawingSpace drawingSpace;
    private BufferedImage image;
    private Graphics2D g2;
    private IBoilerSource source;
    private String sourceName;
    private Rectangle viewport;
    private boolean autoTick = true;
    private boolean persistent = true;
    private boolean renderPaused = false;
    private JsonObject settings = new JsonObject();
    private JsonObject sourceData = new JsonObject();
    private List<Location> speakers = new ArrayList<>();
    private HashMap<UUID, Long> lastUpdates = new HashMap<>();
    private final Set<Player> receivers = new HashSet<>();
    private long lastRender;
    private int renderPeriod = 20;
    private Timer renderTimer;

    public ImplBoilerDisplay(int id, Location cornerA, Location cornerB, BlockFace facing) {
        this.ID = id;
        this.CORNER_A = cornerA;
        this.CORNER_B = cornerB;
        this.FACING = facing;

        this.MAP_DISPLAY = MAP_ENGINE.displayProvider().createBasic(cornerA.toVector().toBlockVector(), cornerB.toVector().toBlockVector(), facing);
        this.viewport = new Rectangle(0, 0, width(), height());
        this.drawingSpace = MAP_ENGINE.pipeline().createDrawingSpace(MAP_DISPLAY);
        this.image = new BufferedImage(width(), height(), BufferedImage.TYPE_INT_ARGB);
        this.g2 = this.image.createGraphics();

        //this.renderTimer = new Timer();
        this.settings.addProperty("buffer", true);

        save();
    }
    @Override
    public void tick(Player player) {
        int viewDistance = BoilerConfig.viewDistance;
        if(settings.has("viewDistance")) {
            viewDistance = settings.get("viewDistance").getAsInt();
        }

        if(CORNER_A.getWorld().equals(player.getWorld()) && CORNER_A.distance(player.getLocation()) < viewDistance) {
            if(!receivers.contains(player)) {
                receivers.add(player);
                MAP_DISPLAY.spawn(player);
            }
        }else {
            if(receivers.contains(player)) {
                receivers.remove(player);
                MAP_DISPLAY.despawn(player);
            }
        }
        if(!player.isOnline()) receivers.remove(player);

        if(receivers.isEmpty() && renderTimer != null) {
            renderTimer.cancel();
            renderTimer = null;
        } else if(renderTimer == null && !receivers.isEmpty()) {
            renderTimer = new Timer();
            renderTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    render();
                }
            }, 0, renderPeriod);
        }
    }

    @Override
    public boolean autoTick() {
        return autoTick;
    }

    @Override
    public void autoTick(boolean value) {
        this.autoTick = value;
    }

    @Override
    public boolean persistent() {
        return persistent;
    }

    @Override
    public void persistent(boolean value) {
        this.persistent = value;
    }

    @Override
    public void renderPaused(boolean value) {
        this.renderPaused = value;
    }

    @Override
    public boolean renderPaused() {
        return renderPaused;
    }

    @Override
    public void render() {
        //if(lastRender + renderPeriod > System.currentTimeMillis()) return;

        if(renderPaused) return;
        if(receivers.isEmpty()) return;

        if(source == null) {
            source("default", null);
            return;
        }

        Set<Player> actualReceivers = new HashSet<>(receivers);
        Set<Player> toRemove = new HashSet<>();

        actualReceivers.forEach(player -> {
            int interval = BoilerPlugin.getPlugin().intervalManager().getInterval(player);
            if(interval > 0 && lastUpdates.containsKey(player.getUniqueId())) {
                long lastUpdate = lastUpdates.get(player.getUniqueId());
                if(lastUpdate + interval > System.currentTimeMillis()) {
                    toRemove.add(player);
                }
            }
        });
        actualReceivers.removeAll(toRemove);

        drawingSpace.ctx().receivers(actualReceivers);
        source.draw(drawingSpace);
        drawingSpace.flush();

        actualReceivers.forEach(player -> {
            lastUpdates.put(player.getUniqueId(), System.currentTimeMillis());
        });

        lastRender = System.currentTimeMillis();
    }

    @Override
    public byte[] provide20msAudio() {
        return source.provide20msAudio();
    }

    @Override
    public void remove() {
        if(this.source != null) {
            try {
                source.unload();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        drawingSpace.ctx().receivers().forEach(MAP_DISPLAY::despawn);
        MAP_DISPLAY = null;
    }

    @Override
    public Set<Player> viewers() {
        return receivers;
    }

    @Override
    public List<Location> speakers() {
        return Collections.emptyList();
    }

    @Override
    public void onClick(CommandSender sender, int x, int y, boolean right) {
        if(source != null) source.onClick(sender, x, y, right);
    }

    @Override
    public void onScroll(CommandSender sender, int x, int y, int delta) {
        if(source != null) source.onScroll(sender, x, y, delta);
    }

    @Override
    public void onInput(CommandSender sender, String string) {
        if(source != null) source.onInput(sender, string);
    }

    @Override
    public void onKey(CommandSender sender, String key) {
        if(source != null) source.onKey(sender, key);
    }

    @Override
    public void save() {
        BoilerPlugin.getPlugin().displayManager().saveDisplay(this);

        drawingSpace.ctx().converter(settings.get("dither") != null && settings.get("dither").getAsBoolean() ? Converter.FLOYD_STEINBERG : Converter.DIRECT);
        drawingSpace.ctx().buffering(settings.get("buffer") != null && settings.get("buffer").getAsBoolean());
        drawingSpace.ctx().bundling(settings.get("bundle") != null && settings.get("bundle").getAsBoolean());

        renderPeriod = settings.get("renderPeriod") != null ? settings.get("renderPeriod").getAsInt() : 20;
    }

    @Override
    public void saveSourceData(JsonObject data) {
        this.sourceData = data;
        save();
    }

    @Override
    public int id() {
        return this.ID;
    }

    @Override
    public void source(String name, JsonObject data) {
        Class<? extends IBoilerSource> sourceClass = BoilerPlugin.getPlugin().sourceManager().source(name);
        if(sourceClass == null) {
            BoilerPlugin.getPlugin().getLogger().log(Level.WARNING, "Failed to load source for map display " + this.id() + " (source not found)");
            return;
        }

        if(this.source != null) {
            try {
                this.source.unload();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        boolean keepLastSourceData = data != null && data.has("keepLastSourceData") && data.get("keepLastSourceData").getAsBoolean();

        this.sourceData = data;
        this.sourceName = name;

        g2.clearRect(0, 0, width(), height());

        if(persistent && !keepLastSourceData) {
            save();
        }

        BoilerPlugin.EXECUTOR.execute(() -> {
            try {
                source = sourceClass.getDeclaredConstructor().newInstance();
                source.load(this, data);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });

    }

    @Override
    public String sourceName() {
        return sourceName;
    }

    @Override
    public IBoilerSource source() {
        return source;
    }

    @Override
    public JsonObject sourceData() {
        return sourceData;
    }

    @Override
    public Location cornerA() {
        return CORNER_A;
    }

    @Override
    public Location cornerB() {
        return CORNER_B;
    }

    @Override
    public Location center() {
        return CORNER_A.clone().add(CORNER_B).multiply(0.5);
    }

    @Override
    public JsonObject settings() {
        return settings;
    }

    @Override
    public void settings(JsonObject obj) {
        settings = obj;
        save();
    }

    @Override
    public BlockFace facing() {
        return FACING;
    }

    @Override
    public IMapDisplay mapDisplay() {
        return MAP_DISPLAY;
    }

    @Override
    public IDrawingSpace drawingSpace() {
        return drawingSpace;
    }

    @Override
    public void viewport(Rectangle viewport) {
        if(viewport == null) throw new NullPointerException("Viewport cannot be null");
        this.viewport = viewport;
    }

    @Override
    public Rectangle viewport() {
        return viewport;
    }

}
