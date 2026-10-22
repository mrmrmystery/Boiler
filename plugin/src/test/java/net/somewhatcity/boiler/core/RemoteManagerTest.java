package net.somewhatcity.boiler.core;

import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.remote.RemoteItem;
import net.somewhatcity.boiler.core.remote.RemoteManager;
import net.somewhatcity.boiler.core.smarttv.SmartTvInputs;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteManagerTest {
    private MockedStatic<RemoteItem> items;
    private RemoteManager manager;
    private BoilerPlugin plugin;
    private Player player;
    private PlayerInventory inventory;
    private ItemStack remote;
    private IBoilerDisplay display;
    private Runnable tick;

    @BeforeEach void setup() {
        plugin = mock(BoilerPlugin.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(1L), eq(1L))).thenAnswer(invocation -> {
            tick = invocation.getArgument(1); return mock(org.bukkit.scheduler.BukkitTask.class);
        });
        IDisplayManager displays = mock(IDisplayManager.class);
        when(plugin.displayManager()).thenReturn(displays);
        when(plugin.smartTvInputs()).thenReturn(mock(SmartTvInputs.class));
        display = mock(IBoilerDisplay.class);
        when(displays.display(7)).thenReturn(display);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("boiler.command.action")).thenReturn(true);
        inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        remote = mock(ItemStack.class);
        when(inventory.getItemInMainHand()).thenReturn(remote);
        when(player.getCurrentInput()).thenReturn(mock(Input.class));
        when(server.getOnlinePlayers()).thenAnswer(invocation -> List.of(player));
        items = mockStatic(RemoteItem.class);
        items.when(() -> RemoteItem.displayId(remote)).thenReturn(7);
        manager = new RemoteManager(plugin);
    }

    @AfterEach void teardown() { manager.close(); items.close(); }

    private PlayerMoveEvent movement() {
        World world = mock(World.class);
        return new PlayerMoveEvent(player, new Location(world, 1, 2, 3, 10, 20), new Location(world, 2, 3, 4, 30, 40));
    }

    @Test void locksPositionIncludingJumpButKeepsLookDirectionAndRestoresOnUnequip() {
        PlayerMoveEvent move = movement();
        manager.move(move);
        assertEquals(move.getFrom().toVector(), move.getTo().toVector());
        assertEquals(30, move.getTo().getYaw()); assertEquals(40, move.getTo().getPitch());
        when(inventory.getItemInMainHand()).thenReturn(null);
        PlayerMoveEvent unlocked = movement();
        manager.move(unlocked);
        assertEquals(2, unlocked.getTo().getX());
        verify(plugin.smartTvInputs()).cancelPlayer(player);
        verify(player, never()).setWalkSpeed(anyFloat());
        verify(player, never()).setFlySpeed(anyFloat());
    }

    @Test void capturesInputAndStopsAfterTeleportUntilReselection() {
        Input input = mock(Input.class);
        when(input.isForward()).thenReturn(true);
        manager.input(new PlayerInputEvent(player, input));
        verify(display).onKey(player, "W");
        PlayerMoveEvent move = movement();
        manager.teleport(new PlayerTeleportEvent(player, move.getFrom(), move.getTo()));
        manager.input(new PlayerInputEvent(player, input));
        verify(display, times(1)).onKey(player, "W");
        PlayerMoveEvent unlocked = movement(); manager.move(unlocked);
        assertEquals(2, unlocked.getTo().getX());
        manager.held(new PlayerItemHeldEvent(player, 0, 1));
        manager.input(new PlayerInputEvent(player, input));
        verify(display, times(2)).onKey(player, "W");
    }

    @Test void deletionAndPermissionLossEndTheSession() {
        tick.run();
        when(plugin.displayManager().display(7)).thenReturn(null);
        tick.run();
        verify(plugin.smartTvInputs()).cancelPlayer(player);
        PlayerMoveEvent move = movement(); manager.move(move);
        assertEquals(2, move.getTo().getX());
        when(plugin.displayManager().display(7)).thenReturn(display);
        tick.run();
        when(player.hasPermission("boiler.command.action")).thenReturn(false);
        tick.run();
        verify(plugin.smartTvInputs(), times(2)).cancelPlayer(player);
    }

    @Test void shutdownCancelsInputAndUnlocksPlayer() {
        tick.run(); manager.close();
        PlayerMoveEvent move = movement(); manager.move(move);
        assertEquals(2, move.getTo().getX());
        verify(plugin.smartTvInputs()).cancelPlayer(player);
    }

    @Test void dropSwapAndDeathSuspendUntilAHotbarSelection() {
        tick.run();
        PlayerDropItemEvent drop = mock(PlayerDropItemEvent.class); when(drop.getPlayer()).thenReturn(player);
        manager.drop(drop);
        PlayerMoveEvent move = movement(); manager.move(move); assertEquals(2, move.getTo().getX());
        manager.held(new PlayerItemHeldEvent(player, 0, 1)); tick.run();
        PlayerSwapHandItemsEvent swap = mock(PlayerSwapHandItemsEvent.class); when(swap.getPlayer()).thenReturn(player);
        manager.swap(swap);
        move = movement(); manager.move(move); assertEquals(2, move.getTo().getX());
        manager.held(new PlayerItemHeldEvent(player, 1, 0)); tick.run();
        org.bukkit.event.entity.PlayerDeathEvent death = mock(org.bukkit.event.entity.PlayerDeathEvent.class);
        when(death.getEntity()).thenReturn(player); manager.death(death);
        move = movement(); manager.move(move); assertEquals(2, move.getTo().getX());
        verify(plugin.smartTvInputs(), times(3)).cancelPlayer(player);
    }

    @Test void inventoryOpeningPausesControlUntilClosedAndQuitCleansUp() {
        tick.run();
        org.bukkit.event.inventory.InventoryOpenEvent open = mock(org.bukkit.event.inventory.InventoryOpenEvent.class);
        when(open.getPlayer()).thenReturn(player); manager.open(open);
        tick.run();
        PlayerMoveEvent move = movement(); manager.move(move); assertEquals(2, move.getTo().getX());
        org.bukkit.event.inventory.InventoryCloseEvent close = mock(org.bukkit.event.inventory.InventoryCloseEvent.class);
        when(close.getPlayer()).thenReturn(player); manager.close(close);
        tick.run();
        move = movement(); manager.move(move); assertEquals(1, move.getTo().getX());
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class); when(quit.getPlayer()).thenReturn(player);
        manager.quit(quit); when(player.isOnline()).thenReturn(false);
        move = movement(); manager.move(move); assertEquals(2, move.getTo().getX());
        verify(plugin.smartTvInputs(), times(2)).cancelPlayer(player);
    }
}
