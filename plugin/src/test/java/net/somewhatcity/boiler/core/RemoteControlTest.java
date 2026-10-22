package net.somewhatcity.boiler.core;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.Key;
import net.somewhatcity.boiler.core.commands.RemoteCommand;
import net.somewhatcity.boiler.core.remote.RemoteItem;
import net.somewhatcity.boiler.core.remote.RemoteKeys;
import org.bukkit.Input;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteControlTest {
    @Test void mapsAllMovementFlagsAndIgnoresSprint() {
        Input input = mock(Input.class);
        when(input.isForward()).thenReturn(true);
        when(input.isBackward()).thenReturn(true);
        when(input.isLeft()).thenReturn(true);
        when(input.isRight()).thenReturn(true);
        when(input.isJump()).thenReturn(true);
        when(input.isSneak()).thenReturn(true);
        when(input.isSprint()).thenReturn(true);
        assertEquals(EnumSet.allOf(Key.class), RemoteKeys.keys(input));
        assertEquals(Set.of(), RemoteKeys.keys(mock(Input.class)));
    }

    @Test void repeatsDirectionsAfterEightTicksButNeverRepeatsConfirmOrBack() {
        RemoteKeys keys = new RemoteKeys();
        List<String> sent = new ArrayList<>();
        keys.update(EnumSet.of(Key.W, Key.SPACE, Key.SHIFT), 0, sent::add);
        assertEquals(List.of("W", "SPACE", "SHIFT"), sent);
        for (int tick = 1; tick < 8; tick++) {
            keys.update(EnumSet.of(Key.W, Key.SPACE, Key.SHIFT), tick, sent::add);
            keys.tick(tick, sent::add);
        }
        assertEquals(3, sent.size());
        keys.tick(8, sent::add); keys.tick(9, sent::add); keys.tick(10, sent::add);
        assertEquals(List.of("W", "SPACE", "SHIFT", "W", "W"), sent);
        keys.update(Set.of(), 11, sent::add);
        keys.tick(30, sent::add);
        assertEquals(5, sent.size());
        keys.update(EnumSet.of(Key.SPACE), 31, sent::add);
        assertEquals("SPACE", sent.getLast());
    }

    @Test void recognisesPersistentMarkerIncludingTransientDisplayIds() {
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(item.getType()).thenReturn(Material.CARROT_ON_A_STICK);
        when(item.hasItemMeta()).thenReturn(true);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        assertNull(RemoteItem.displayId(item));
        when(data.get(new NamespacedKey("boiler", "remote_display"), PersistentDataType.INTEGER)).thenReturn(-7);
        assertEquals(-7, RemoteItem.displayId(item));
        when(item.getType()).thenReturn(Material.STICK);
        assertNull(RemoteItem.displayId(item));
        assertNull(RemoteItem.displayId(null));
    }

    @Test void commandValidatesPlayerDisplayPermissionAndFullInventory() throws Exception {
        BoilerPlugin plugin = mock(BoilerPlugin.class);
        IDisplayManager displays = mock(IDisplayManager.class);
        when(plugin.displayManager()).thenReturn(displays);
        IBoilerDisplay display = mock(IBoilerDisplay.class);
        when(display.id()).thenReturn(7);
        when(displays.display(7)).thenReturn(display);
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(new RemoteCommand(plugin).build());
        CommandSourceStack source = mock(CommandSourceStack.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(source.getSender()).thenReturn(player);
        when(source.getExecutor()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(player.hasPermission("boiler.command.remote")).thenReturn(true);
        ItemStack item = mock(ItemStack.class);
        try (MockedStatic<RemoteItem> items = mockStatic(RemoteItem.class)) {
            items.when(() -> RemoteItem.create(7)).thenReturn(item);
            dispatcher.execute("remote 999", source);
            verify(inventory, never()).addItem(any(ItemStack[].class));
            when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>(Map.of(0, item)));
            dispatcher.execute("remote 7", source);
            verify(inventory).addItem(item);
            when(inventory.addItem(any(ItemStack[].class))).thenReturn(new HashMap<>());
            dispatcher.execute("remote 7", source);
            verify(inventory, times(2)).addItem(item);
            when(player.hasPermission("boiler.command.remote")).thenReturn(false);
            assertThrows(com.mojang.brigadier.exceptions.CommandSyntaxException.class, () -> dispatcher.execute("remote 7", source));
            when(player.hasPermission("boiler.command.remote")).thenReturn(true);
            when(source.getExecutor()).thenReturn(null);
            dispatcher.execute("remote 7", source);
            verify(inventory, times(2)).addItem(item);
        }
    }
}
