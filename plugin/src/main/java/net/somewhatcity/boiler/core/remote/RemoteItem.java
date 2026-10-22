package net.somewhatcity.boiler.core.remote;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class RemoteItem {
    private static final NamespacedKey DISPLAY = new NamespacedKey("boiler", "remote_display");
    private RemoteItem() {}

    public static ItemStack create(int displayId) {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Boiler Remote · Display " + displayId));
            meta.lore(List.of(Component.text("WASD: Navigation · Springen: OK"),
                    Component.text("Sneaken: Zurück · Slotwechsel: Beenden")));
            meta.getPersistentDataContainer().set(DISPLAY, PersistentDataType.INTEGER, displayId);
        });
        return item;
    }

    public static Integer displayId(ItemStack item) {
        if (item == null || item.getType() != Material.CARROT_ON_A_STICK || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(DISPLAY, PersistentDataType.INTEGER);
    }
}
