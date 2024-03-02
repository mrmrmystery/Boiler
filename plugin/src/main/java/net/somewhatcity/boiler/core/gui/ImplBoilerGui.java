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

import de.pianoman911.mapengine.api.pipeline.IPipelineContext;
import de.pianoman911.mapengine.api.pipeline.IPipelineStream;
import de.pianoman911.mapengine.api.util.FullSpacedColorBuffer;
import de.pianoman911.mapengine.api.util.ImageUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.math.Rotations;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
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
import org.bukkit.inventory.ItemStack;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
public class ImplBoilerGui implements Listener {

    private ItemStack[] previousInventory;
    private GameMode previousGameMode;
    private Location previousLocation;
    private IBoilerDisplay display;
    private Point cursorPos = new Point(0, 0);
    private int taskId;
    private BufferedImage cursorImg;
    private int[] cursor;
    private Player player;

    private ArmorStand armorStand;
    private ArmorStand clickStand;
    public ImplBoilerGui(BoilerPlugin plugin, Player player, String source) {
        previousInventory = player.getInventory().getContents().clone();
        previousGameMode = player.getGameMode();
        previousLocation = player.getLocation().clone();
        this.player = player;

        World world = plugin.guiManager().world();

        Location pos = new Location(world, 0.5, -1.5, 0.5);
        player.teleport(pos);
        player.setInvisible(true);
        //player.setGameMode(GameMode.SPECTATOR);

        armorStand = (ArmorStand) world.spawnEntity(pos, EntityType.ARMOR_STAND);
        armorStand.setBodyYaw(0);
        armorStand.setHeadRotations(Rotations.ZERO);
        armorStand.setCanTick(false);
        armorStand.addPassenger(player);
        armorStand.setVisible(false);

        clickStand = (ArmorStand) world.spawnEntity(pos, EntityType.ARMOR_STAND);
        clickStand.setCanTick(false);
        clickStand.setVisible(false);

        plugin.platform().createSetCameraPacket(armorStand).send(player);

        try {
            cursorImg = ImageIO.read(ImplBoilerGui.class.getResourceAsStream("/assets/boiler_cursor.png"));
            cursor = ImageUtils.rgb(cursorImg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        display = BoilerPlugin.getPlugin().displayManager().createDisplay(
                new Location(world, -4, -2, 2),
                new Location(world, 4, 2, 2),
                BlockFace.NORTH,
                false,
                false
        );
        display.source(source, null);
        display.tick(player);

        Bukkit.getPluginManager().registerEvents(this, BoilerPlugin.getPlugin());

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(BoilerPlugin.getPlugin(), () -> {
            if(!player.isOnline() || !player.getLocation().getWorld().equals(world)) {

            }

            plugin.platform().createSetCameraPacket(armorStand).send(player);

            if(display.mapDisplay() == null) return;

            double yaw = player.getLocation().getYaw();
            double pitch = player.getLocation().getPitch();

            if(yaw > 90) {
                yaw = 90;
                Location location = player.getLocation();
                location.setYaw(90);
                //player.teleport(location);
            } else if(yaw < -90) {
                yaw = -90;
                Location location = player.getLocation();
                location.setYaw(-90);
                //player.teleport(location);
            }


            cursorPos.x  = (int) Util.map((float) yaw, -90, 90, 0, display.width());
            cursorPos.y  = (int) Util.map((float) pitch, -90, 90, 0, display.height());

        }, 0, 1);

        display.mapDisplay().pipeline().addStream(new IPipelineStream() {
            @Override
            public FullSpacedColorBuffer compute(FullSpacedColorBuffer buffer, IPipelineContext ctx) {
                buffer.pixels(cursor, cursorPos.x, cursorPos.y, cursorImg.getWidth(), cursorImg.getHeight());
                return buffer;
            }
        });

        //display.source(source, new String[]{});

        //plugin.platform().createArmorStandSpawnPacket(Bukkit.getUnsafe().nextEntityId(), newWorld);
    }

    public void exit() {
        armorStand.removePassenger(player);
        player.setInvisible(false);
        BoilerPlugin.getPlugin().platform().createSetCameraPacket(player).send(player);
        display.remove();
        armorStand.remove();
        clickStand.remove();
        Bukkit.getScheduler().cancelTask(taskId);
        GuiClickEvent.getHandlerList().unregister(this);
        GuiKeyEvent.getHandlerList().unregister(this);
        AsyncChatEvent.getHandlerList().unregister(this);
    }


    @EventHandler
    public void onGuiClick(GuiClickEvent e) {
        if(!e.player().equals(player)) return;
        if(display.source() != null) display.source().onClick(player, cursorPos.x, cursorPos.y, e.isRightClick());
    }

    @EventHandler
    public void onGuiKey(GuiKeyEvent e) {
        if(!e.player().equals(player)) return;

        String key = "";
        if(e.w()) key = "W";
        else if(e.a()) key = "A";
        else if(e.s()) key = "S";
        else if(e.d()) key = "D";
        else if(e.space()) key = "SPACE";
        else if(e.shift()) key = "SHIFT";

        if(key.equals("SHIFT")) {
            exit();
            player.teleport(previousLocation);
            return;
        }

        if(display.source() != null) display.source().onKey(player, key);
    }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        if(!e.getPlayer().equals(player)) return;
        String input = MiniMessage.miniMessage().serialize(e.message());
        if(display.source() != null) display.source().onInput(e.getPlayer(), input);
    }
}
