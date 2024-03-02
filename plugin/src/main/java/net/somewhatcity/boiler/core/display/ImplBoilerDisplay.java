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
import java.util.logging.Level;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class ImplBoilerDisplay implements IBoilerDisplay {
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
    private Timer renderTimer;
    private boolean autoTick = true;
    private boolean persistent = true;
    private boolean renderPaused = false;
    private JsonObject settings = new JsonObject();
    private JsonObject sourceData = new JsonObject();
    private List<Location> speakers = new ArrayList<>();

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

        this.renderTimer = new Timer();
        this.settings.addProperty("buffering", true);


        save();
    }
    @Override
    public void tick(Player player) {

        if(CORNER_A.getWorld().equals(player.getWorld()) && CORNER_A.distance(player.getLocation()) < BoilerConfig.viewDistance) {
            if(!drawingSpace.ctx().isReceiver(player)) {
                drawingSpace.ctx().addReceiver(player);
                MAP_DISPLAY.spawn(player);
            }
        }else {
            drawingSpace.ctx().receivers().remove(player);
            MAP_DISPLAY.despawn(player);
        }
        if(!player.isOnline()) drawingSpace.ctx().removeReceiver(player);
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
        if(renderPaused) return;
        if(drawingSpace.ctx().receivers().isEmpty()) return;

        if(source == null) {
            source("default", null);
            return;
        }

        source.draw(drawingSpace);

        drawingSpace.flush();

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
        this.renderTimer.cancel();
        drawingSpace.ctx().receivers().forEach(MAP_DISPLAY::despawn);
        MAP_DISPLAY = null;
    }

    @Override
    public Set<Player> viewers() {
        return drawingSpace.ctx().receivers();
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

        int renderPeriod = settings.get("renderPeriod") != null ? settings.get("renderPeriod").getAsInt() : 50;

        this.renderTimer.cancel();
        this.renderTimer = new Timer();
        this.renderTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                render();
            }
        },0, renderPeriod);
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
        this.sourceData = data;
        this.sourceName = name;
        g2.clearRect(0, 0, width(), height());

        if(persistent) {
            save();
        }

        new Thread(() -> {
            try {
                source = sourceClass.getDeclaredConstructor().newInstance();
                source.load(this, data);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }).start();
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
