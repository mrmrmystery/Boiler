/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core;

import com.sun.jna.Platform;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.GstException;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.PlayBin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Util {

    public static float map(float value, float inMin, float inMax, float outMin, float outMax) {
        return (value - inMin) / (inMax - inMin) * (outMax - outMin) + outMin;
    }
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final Component PREFIX = MM.deserialize("<b><color:#52a3ff>[Boiler]</color></b> ");

    public static void sendMsg(CommandSender sender, String msg, Object... args) {
        String[] colored = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            colored[i] = "<color:#52a3ff>" + args[i].toString() + "</color>";
        }

        sender.sendMessage(PREFIX.append(MM.deserialize(msg.formatted((Object[]) colored))));
    }

    public static void sendErrMsg(CommandSender sender, String msg, Object... args) {
        String[] colored = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            colored[i] = "<b><color:#52a3ff>" + args[i].toString() + "</color></b>";
        }

        sender.sendMessage(PREFIX.append(MM.deserialize("<b><color:#ff4a56>[Error]</color></b> ")).append(MM.deserialize(msg.formatted((Object[]) colored))));
    }

    public static boolean isPluginInstalled(String plugin) {
        return (Bukkit.getPluginManager().getPlugin(plugin) != null && Bukkit.getPluginManager().getPlugin(plugin).isEnabled());
    }

    private static volatile boolean gstreamerInstalled;

    private static boolean gstreamerAttempted;
    public static void initGstreamer() {
        BoilerPlugin.getPlugin().runtime().startWorker("gstreamer-init", Util::ensureGstreamer);
    }
    public static synchronized boolean ensureGstreamer() {
        if (gstreamerAttempted) return gstreamerInstalled;
        gstreamerAttempted = true;
        try {
            if (Platform.isWindows()) GstreamerUtils.configurePaths();
            Gst.init("Boiler");
            gstreamerInstalled = true;
        } catch (LinkageError | RuntimeException e) {
            BoilerPlugin.getPlugin().getLogger().fine("GStreamer unavailable: " + e.getMessage());
        }
        return gstreamerInstalled;
    }

    public static void shutdownGstreamer() {
        // Sources own their pipelines. Do not shut down GStreamer's JVM-wide context:
        // other plugins may use it, too.
        gstreamerInstalled = false;
    }

    public static boolean isGstreamerInstalled() {
        return gstreamerInstalled;
    }
}
