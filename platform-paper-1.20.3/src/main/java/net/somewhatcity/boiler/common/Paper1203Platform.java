/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.common;

import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import net.somewhatcity.boiler.common.platform.IListenerBridge;
import net.somewhatcity.boiler.common.platform.IPlatform;
import net.somewhatcity.boiler.common.platform.PacketContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class Paper1203Platform implements IPlatform<Packet<ClientGamePacketListener>>, Listener {

    private final IListenerBridge bridge;
    public Paper1203Platform(Plugin plugin, IListenerBridge bridge) {
        this.bridge = bridge;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Paper1203Listener listener = new Paper1203Listener(e.getPlayer(), bridge);
        ((CraftPlayer) e.getPlayer()).getHandle().connection.connection.channel.pipeline().addAfter("decoder", "boiler", listener);
    }

    @EventHandler
    public void onHotbarSwitch(PlayerItemHeldEvent e) {
        bridge.handleHotbarSwitch(e.getPlayer(), e.getNewSlot(), e.getPreviousSlot());
    }

    @Override
    public String name() {
        return MinecraftServer.getServer().getServerModName() + " " + SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public void sendPacket(Player player, PacketContainer<Packet<ClientGamePacketListener>> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet.getPacket());
    }

    @Override
    public void flush(Player player) {

    }

    @Override
    public PacketContainer<Packet<ClientGamePacketListener>> createArmorStandSpawnPacket(int entity, Location loc) {
        return PacketContainer.wrap(this, new ClientboundAddEntityPacket(
                Bukkit.getUnsafe().nextEntityId(),
                UUID.randomUUID(),
                loc.x(),
                loc.y(),
                loc.z(),
                0,
                0,
                EntityType.ARMOR_STAND,
                0,
                new Vec3(0, 0, 0),
                0
        ));
    }

    @Override
    public PacketContainer<Packet<ClientGamePacketListener>> createSetCameraPacket(Entity entity) {
        return PacketContainer.wrap(this, new ClientboundSetCameraPacket(((CraftEntity) entity).getHandle()));
    }

    @Override
    public PacketContainer<Packet<ClientGamePacketListener>> createPlayerMountPacket() {
        return null;
    }
}
