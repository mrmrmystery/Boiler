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

import de.pianoman911.mapengine.api.event.MapClickEvent;
import de.pianoman911.mapengine.api.util.MapClickType;
import de.pianoman911.mapengine.api.util.MapTraceResult;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.plugin.Plugin;

import static net.somewhatcity.boiler.core.BoilerPlugin.MAP_ENGINE;

public class BoilerListener implements Listener {
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        int b = e.getPreviousSlot();
        int n = e.getNewSlot();

        MapTraceResult result = MAP_ENGINE.traceDisplayInView(e.getPlayer(), 10);
        if(result == null) return;
        IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().display(result.display());
        if(display == null) return;

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

        display.onScroll(e.getPlayer(), result.viewPos().x(), result.viewPos().y(), delta);

    }

    @EventHandler
    public void onMapClick(MapClickEvent e) {
        IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().display(e.display());
        if(display != null) display.onClick(e.player(), e.x(), e.y(), e.clickType().equals(MapClickType.RIGHT_CLICK));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if(!e.getPlayer().getWorld().getName().equals("boilerui")) return;
        e.setCancelled(true);
    }

}
