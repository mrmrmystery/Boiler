package net.somewhatcity.boiler.core.remote;

import net.kyori.adventure.text.Component;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public final class RemoteManager implements Listener, AutoCloseable {
    private record Session(Player player, IBoilerDisplay display, RemoteKeys keys) {}
    private final BoilerPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Set<UUID> suspended = new HashSet<>();
    private final Set<UUID> inventories = new HashSet<>();
    private final BukkitTask timer;
    private long tick;
    private boolean closed;

    public RemoteManager(BoilerPlugin plugin) {
        this.plugin = plugin;
        timer = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1);
    }

    public boolean holding(Player player) { return RemoteItem.displayId(player.getInventory().getItemInMainHand()) != null; }

    private Session session(Player player) {
        UUID id = player.getUniqueId();
        Integer target = RemoteItem.displayId(player.getInventory().getItemInMainHand());
        IBoilerDisplay display = target == null ? null : plugin.displayManager().display(target);
        if (closed || target == null || display == null || !player.isOnline() || player.isDead()
                || player.isInsideVehicle() || !player.hasPermission("boiler.command.action")
                || suspended.contains(id) || inventories.contains(id)) {
            end(player, false);
            if (target == null) suspended.remove(id);
            return null;
        }
        Session current = sessions.get(id);
        if (current != null && current.display() != display) { end(player, false); current = null; }
        if (current == null) {
            current = new Session(player, display, new RemoteKeys());
            sessions.put(id, current);
            player.sendActionBar(Component.text("Display " + target + " · WASD · Springen: OK · Sneaken: Zurück"));
        }
        return current;
    }

    private void tick() {
        ++tick;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Session current = session(player);
            if (current == null) continue;
            current.keys().update(RemoteKeys.keys(player.getCurrentInput()), tick, key -> send(current, key));
            current.keys().tick(tick, key -> send(current, key));
        }
    }

    private void send(Session session, String key) {
        if (sessions.get(session.player().getUniqueId()) == session)
            session.display().onKey(session.player(), key);
    }

    public void end(Player player, boolean suspend) {
        if (suspend) suspended.add(player.getUniqueId());
        if (sessions.remove(player.getUniqueId()) != null && plugin.smartTvInputs() != null)
            plugin.smartTvInputs().cancelPlayer(player);
    }

    @EventHandler public void input(PlayerInputEvent event) {
        Session current = session(event.getPlayer());
        if (current != null) current.keys().update(RemoteKeys.keys(event.getInput()), tick, key -> send(current, key));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void move(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent || session(event.getPlayer()) == null || !event.hasChangedPosition()) return;
        Location to = event.getTo().clone();
        to.setX(event.getFrom().getX()); to.setY(event.getFrom().getY()); to.setZ(event.getFrom().getZ());
        event.setTo(to);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void sneak(PlayerToggleSneakEvent event) { if (session(event.getPlayer()) != null) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void sprint(PlayerToggleSprintEvent event) { if (session(event.getPlayer()) != null) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void flight(PlayerToggleFlightEvent event) { if (session(event.getPlayer()) != null) event.setCancelled(true); }
    @EventHandler public void interact(PlayerInteractEvent event) {
        if (!holding(event.getPlayer())) return;
        event.setCancelled(true);
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND || !event.getAction().isLeftClick() && !event.getAction().isRightClick()) return;
        Session current = session(event.getPlayer());
        if (current == null) return;
        var hit = BoilerPlugin.MAP_ENGINE.traceDisplayInView(event.getPlayer(), 10);
        if (hit != null && plugin.displayManager().display(hit.display()) == current.display())
            current.display().onClick(event.getPlayer(), hit.viewPos().x(), hit.viewPos().y(), event.getAction().isRightClick());
    }
    @EventHandler public void interactEntity(PlayerInteractEntityEvent event) { if (holding(event.getPlayer())) event.setCancelled(true); }
    @EventHandler public void damage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player && holding(player)) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true) public void held(PlayerItemHeldEvent event) { end(event.getPlayer(), false); suspended.remove(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void drop(PlayerDropItemEvent event) { end(event.getPlayer(), true); }
    @EventHandler(ignoreCancelled = true) public void swap(PlayerSwapHandItemsEvent event) { end(event.getPlayer(), true); }
    @EventHandler(ignoreCancelled = true) public void teleport(PlayerTeleportEvent event) { end(event.getPlayer(), true); }
    @EventHandler public void death(PlayerDeathEvent event) { end(event.getEntity(), true); }
    @EventHandler public void quit(PlayerQuitEvent event) {
        end(event.getPlayer(), false);
        suspended.remove(event.getPlayer().getUniqueId()); inventories.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(ignoreCancelled = true) public void open(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) { inventories.add(player.getUniqueId()); end(player, false); }
    }
    @EventHandler public void close(InventoryCloseEvent event) { inventories.remove(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true) public void inventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && holding(player)) end(player, true);
    }

    @Override public void close() {
        closed = true;
        timer.cancel();
        for (Session session : new ArrayList<>(sessions.values())) end(session.player(), false);
        suspended.clear(); inventories.clear();
        HandlerList.unregisterAll(this);
    }
}
