package net.somewhatcity.boiler.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class MessageUtil {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final Component PREFIX = MM.deserialize("<b><color:#2b6bff>ʙᴏɪʟᴇʀ |</color></b> ");

    private static final String LIGHT_GREEN = "<color:#00ff7b>";
    private static final String LIGHT_RED = "<color:#ff594a>";

    public static void sendGreen(CommandSender sender, String message, Object... args) {
        sender.sendMessage(format(message, LIGHT_GREEN, args));
    }

    public static void sendRed(CommandSender sender, String message, Object... args) {
        sender.sendMessage(format(message, LIGHT_RED, args));
    }

    public static void sendBox(CommandSender sender, String message, Object... args) {
        Component component = Component.empty();
        String[] lines = message.formatted(args).split("\n");
        component = component.append(MM.deserialize("<color:#0088ff>╔══\n"));
        for(int i = 0; i < lines.length; i++) {
            component =  component.append(MM.deserialize("<color:#0088ff>║ <reset>" + lines[i] + "\n"));
        }
        component = component.append(MM.deserialize("<color:#0088ff>╚══"));
        sender.sendMessage(component);
    }

    public static Component getGreen(String message, Object... args) {
        return format(message, LIGHT_GREEN, args);
    }

    public static Component getRed(String message, Object... args) {
        return format(message, LIGHT_RED, args);
    }

    private static Component format(String message, String color, Object[] args) {
        String[] colored = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            colored[i] = "<b>" + args[i].toString() + "</b>";
        }

        return PREFIX.append(MM.deserialize(color + message.formatted(colored)));
    }

    public static Component getBar(int length, int progress, String character, TextColor bar, TextColor background){
        Component component = Component.empty();
        for(int i = 0; i < length; i++) {
            if (i < progress) {
                component = component.append(Component.text(character).color(bar));
            } else {
                component = component.append(Component.text(character).color(background));
            }
        }
        return component;
    }
}
