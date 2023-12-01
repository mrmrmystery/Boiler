/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuiKeyEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private Player player;
    private boolean w;
    private boolean a;
    private boolean s;
    private boolean d;
    private boolean space;
    private boolean shift;

    public GuiKeyEvent(Player player, float wa, float sd, boolean space, boolean shift) {
        this.player = player;
        this.w = wa > 0;
        this.a = wa < 0;
        this.s = sd > 0;
        this.d = sd < 0;
        this.space = space;
        this.shift = shift;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public Player player() {
        return player;
    }

    public boolean w() {
        return w;
    }
    public boolean a() {
        return a;
    }
    public boolean s() {
        return s;
    }
    public boolean d() {
        return d;
    }
    public boolean space() {
        return space;
    }
    public boolean shift() {
        return shift;
    }
}
