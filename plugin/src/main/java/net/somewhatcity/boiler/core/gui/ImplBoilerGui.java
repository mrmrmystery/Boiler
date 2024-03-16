/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.gui;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.pipeline.IPipelineContext;
import de.pianoman911.mapengine.api.pipeline.IPipelineStream;
import de.pianoman911.mapengine.api.util.FullSpacedColorBuffer;
import de.pianoman911.mapengine.api.util.ImageUtils;
import de.pianoman911.mapengine.api.util.MapTraceResult;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.math.Rotations;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.display.IBoilerGui;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.listener.GuiClickEvent;
import net.somewhatcity.boiler.core.listener.GuiKeyEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class ImplBoilerGui implements IBoilerGui {

    private BoilerGuiListener guiListener;
    private Player player;
    private int armorStandEntityId;
    private IBoilerDisplay display;
    private int bukkitTaskId;
    private BufferedImage cursorImg;
    private int[] cursorRgb;
    private ArmorStand armorStand;
    private Location prevLocation;
    private boolean isClosed;
    public ImplBoilerGui(Player player, int width, int height, String source, JsonObject data) {
        this.player = player;

        guiListener = new BoilerGuiListener(this);

        Location location = player.getLocation().clone();
        prevLocation = location.clone();
        location.setY(400.0);
        player.teleport(location);
        location = location.toCenterLocation();

        armorStandEntityId = Bukkit.getUnsafe().nextEntityId();

        BoilerPlugin.getPlugin().platform().createArmorStandSpawnPacket(armorStandEntityId, location.clone().add(0, -1.75, 0)).send(player);
        BoilerPlugin.getPlugin().platform().createSetCameraPacket(armorStandEntityId).send(player);
        BoilerPlugin.getPlugin().platform().createSetPassengerPacket(armorStandEntityId, new int[]{player.getEntityId()}).send(player);

        armorStand = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0, -1.75, 0), EntityType.ARMOR_STAND);
        armorStand.setBodyYaw(0);
        armorStand.setHeadRotations(Rotations.ZERO);
        armorStand.setCanTick(false);
        armorStand.setVisible(false);
        armorStand.addScoreboardTag("boilerInteractionBlocker");

        display = BoilerPlugin.getPlugin().displayManager().createDisplay(
                location.clone().add(-width, -height, 2),
                location.clone().add(width, height, 2),
                BlockFace.NORTH,
                false,
                false
        );

        if(data == null) data = new JsonObject();
        data.addProperty("gui_player", player.getUniqueId().toString());

        display.source(source, data);

        try {
            cursorImg = ImageIO.read(ImplBoilerGui.class.getResourceAsStream("/assets/boiler_cursor.png"));
            cursorRgb = ImageUtils.rgb(cursorImg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bukkitTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BoilerPlugin.getPlugin(), () -> {
            if(display.mapDisplay() == null) return;

            double yaw = player.getLocation().getYaw();
            double pitch = player.getLocation().getPitch();

            if(yaw > 90) {
                yaw = 90;
                Location loc = player.getLocation();
                loc.setYaw(90);
                player.teleport(loc);
            } else if(yaw < -90) {
                yaw = -90;
                Location loc = player.getLocation();
                loc.setYaw(-90);
                player.teleport(loc);
            }

            guiListener.cursorPos.x  = (int) Util.map((float) yaw, -90, 90, 0, display.width());
            guiListener.cursorPos.y  = (int) Util.map((float) pitch, -90, 90, 0, display.height());
            display.tick(player);
        }, 0, 1);

        display.mapDisplay().pipeline().addStream(new IPipelineStream() {
            @Override
            public FullSpacedColorBuffer compute(FullSpacedColorBuffer buffer, IPipelineContext ctx) {
                buffer.pixels(cursorRgb, guiListener.cursorPos.x, guiListener.cursorPos.y, cursorImg.getWidth(), cursorImg.getHeight());
                return buffer;
            }
        });
    }

    public void exit() {
        if(isClosed) return;
        isClosed = true;
        BoilerPlugin.getPlugin().platform().createSetCameraPacket(player.getEntityId()).send(player);
        BoilerPlugin.getPlugin().platform().createRemoveEntityPacket(new int[]{armorStandEntityId}).send(player);
        player.setInvisible(false);
        display.remove();
        armorStand.remove();
        player.teleport(prevLocation);

        Bukkit.getScheduler().cancelTask(bukkitTaskId);
        BoilerPlugin.getPlugin().guiManager().close(this);
    }

    @Override
    public IBoilerDisplay display() {
        return display;
    }

    @Override
    public Player player() {
        return player;
    }

    private static class BoilerGuiListener implements Listener {
        private IBoilerGui gui;
        private Point cursorPos = new Point(0, 0);
        private long lastClick;
        public BoilerGuiListener(IBoilerGui gui) {
            this.gui = gui;
            Bukkit.getPluginManager().registerEvents(this, BoilerPlugin.getPlugin());
        }
        @EventHandler
        public void onGuiClick(GuiClickEvent e) {
            if(!e.player().equals(gui.player())) return;
            if(lastClick > System.currentTimeMillis() - 100) return;
            lastClick = System.currentTimeMillis();
            if(gui.display().source() != null) gui.display().source().onClick(gui.player(), cursorPos.x, cursorPos.y, e.isRightClick());
        }
        @EventHandler
        public void onGuiKey(GuiKeyEvent e) {
            if(!e.player().equals(gui.player())) return;

            String key = "";
            if(e.w()) key = "W";
            else if(e.a()) key = "A";
            else if(e.s()) key = "S";
            else if(e.d()) key = "D";
            else if(e.space()) key = "SPACE";
            else if(e.shift()) key = "SHIFT";

            if(key.equals("SHIFT")) {
                gui.exit();
                return;
            }

            if(key.isEmpty()) return;
            if(gui.display().source() != null) gui.display().source().onKey(gui.player(), key);
        }

        @EventHandler
        public void onChat(AsyncChatEvent e) {
            if(!e.getPlayer().equals(gui.player())) return;
            e.setCancelled(true);
            String input = MiniMessage.miniMessage().serialize(e.message());
            if(gui.display().source() != null) gui.display().source().onInput(e.getPlayer(), input);
        }

        @EventHandler
        public void onGuiScroll(PlayerItemHeldEvent e) {
            int b = e.getPreviousSlot();
            int n = e.getNewSlot();

            int delta = 0;

            if(b == 8 && n == 0){
                delta = 1;
            }else if(b == 0 && n == 8){
                delta = -1;
            }else if(b < n){
                delta = 1;
            }else if(n < b){
                delta = -1;
            }

            gui.display().onScroll(e.getPlayer(), cursorPos.x, cursorPos.y, delta);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent e) {
            gui.exit();
        }

        public void register() {
            Bukkit.getPluginManager().registerEvents(this, BoilerPlugin.getPlugin());
        }

        public void unregister() {
            GuiClickEvent.getHandlerList().unregister(this);
            GuiKeyEvent.getHandlerList().unregister(this);
            AsyncChatEvent.getHandlerList().unregister(this);
            PlayerItemHeldEvent.getHandlerList().unregister(this);
            PlayerQuitEvent.getHandlerList().unregister(this);
        }
    }


}
