package net.somewhatcity.boiler.core.smarttv;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** The async chat listener only reserves text; source state is always changed on the server thread. */
public final class SmartTvInputs implements Listener, AutoCloseable {
    private static final long TIMEOUT = java.util.concurrent.TimeUnit.SECONDS.toNanos(120);
    private static final class Pending {
        final Object owner;
        final Player player;
        final Consumer<String> input;
        final Runnable cancel;
        long deadline = System.nanoTime() + TIMEOUT;
        Pending(Object owner, Player player, Consumer<String> input, Runnable cancel) {
            this.owner = owner; this.player = player; this.input = input; this.cancel = cancel;
        }
    }
    private final BoilerPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final BukkitTask timer;

    public SmartTvInputs(BoilerPlugin plugin) {
        this.plugin = plugin;
        timer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> expire(System.nanoTime()), 20, 20);
    }

    public boolean request(Object owner, Player player, Consumer<String> input, Runnable cancel) {
        return pending.putIfAbsent(player.getUniqueId(), new Pending(owner, player, input, cancel)) == null;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void chat(AsyncChatEvent event) {
        Pending capture = pending.get(event.getPlayer().getUniqueId());
        if (capture == null) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        plugin.onMain(() -> {
            if (pending.get(capture.player.getUniqueId()) != capture) return;
            if (!capture.player.isOnline() || System.nanoTime() >= capture.deadline) {
                cancelPlayer(capture.player);
                return;
            }
            capture.deadline = System.nanoTime() + TIMEOUT;
            capture.input.accept(text);
        });
    }

    public void cancelPlayer(Player player) {
        Pending capture = pending.remove(player.getUniqueId());
        if (capture != null) capture.cancel.run();
    }

    public void release(Object owner) {
        pending.entrySet().removeIf(entry -> entry.getValue().owner == owner);
    }

    public void touch(Object owner, Player player) {
        Pending capture = pending.get(player.getUniqueId());
        if (capture != null && capture.owner == owner) capture.deadline = System.nanoTime() + TIMEOUT;
    }

    void expire(long now) {
        for (Pending capture : pending.values()) if (now >= capture.deadline) {
            if (!pending.remove(capture.player.getUniqueId(), capture)) continue;
            capture.cancel.run();
            Util.sendMsg(capture.player, "SmartTV-Eingabe wegen Zeitüberschreitung abgebrochen.");
        }
    }

    @EventHandler public void quit(PlayerQuitEvent event) { cancelPlayer(event.getPlayer()); }
    @EventHandler public void death(PlayerDeathEvent event) { cancelPlayer(event.getEntity()); }
    @EventHandler(ignoreCancelled = true) public void teleport(PlayerTeleportEvent event) { cancelPlayer(event.getPlayer()); }
    @EventHandler(ignoreCancelled = true) public void inventory(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) cancelPlayer(player);
    }
    @EventHandler(ignoreCancelled = true) public void inventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) cancelPlayer(player);
    }

    @Override public void close() {
        timer.cancel();
        for (Pending capture : pending.values()) cancelPlayer(capture.player);
        pending.clear();
        HandlerList.unregisterAll(this);
    }
}
