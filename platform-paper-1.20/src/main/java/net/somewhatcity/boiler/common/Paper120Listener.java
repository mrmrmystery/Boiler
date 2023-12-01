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

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.somewhatcity.boiler.common.platform.IListenerBridge;
import org.bukkit.entity.Player;

import java.util.List;

public class Paper120Listener extends MessageToMessageDecoder<Packet<?>> implements ServerboundInteractPacket.Handler {

    private final Player player;
    private final IListenerBridge bridge;
    private int entityId;

    public Paper120Listener(Player player, IListenerBridge bridge) {
        this.player = player;
        this.bridge = bridge;
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return msg instanceof ServerboundPlayerInputPacket || msg instanceof ServerboundInteractPacket;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, Packet<?> msg, List<Object> out) throws Exception {

        if(msg instanceof ServerboundPlayerInputPacket playerInputPacket) {
            bridge.handleInput(player, playerInputPacket.getXxa(), playerInputPacket.getZza(), playerInputPacket.isJumping(), playerInputPacket.isShiftKeyDown());
        } else if(msg instanceof ServerboundInteractPacket interactPacket) {
            entityId = interactPacket.getEntityId();
            interactPacket.dispatch(this);
        }

        out.add(msg);
    }

    @Override
    public void onInteraction(InteractionHand hand) {
        bridge.handleInteract(player, false);
    }

    @Override
    public void onInteraction(InteractionHand hand, Vec3 pos) {
        bridge.handleInteract(player, false);
    }

    @Override
    public void onAttack() {
        bridge.handleInteract(player, true);
    }
}
