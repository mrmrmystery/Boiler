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

        sender.sendMessage(PREFIX.append(MM.deserialize(msg.formatted(colored))));
    }

    public static void sendErrMsg(CommandSender sender, String msg, Object... args) {
        String[] colored = new String[args.length];
        for(int i = 0; i < args.length; i++) {
            colored[i] = "<b><color:#52a3ff>" + args[i].toString() + "</color></b>";
        }

        sender.sendMessage(PREFIX.append(MM.deserialize("<b><color:#ff4a56>[Error]</color></b> ")).append(MM.deserialize(msg.formatted(colored))));
    }

    public static boolean isPluginInstalled(String plugin) {
        return (Bukkit.getPluginManager().getPlugin(plugin) != null && Bukkit.getPluginManager().getPlugin(plugin).isEnabled());
    }

    private static boolean gstreamerInstalled = false;

    public static void initGstreamer() {
        new Thread(() -> {
            try {
                if(Platform.isWindows()) GstreamerUtils.configurePaths();
                Gst.init("VideoFrameExtractor");

                Path target = Path.of(Bukkit.getPluginsFolder().getPath(), "Boiler/temp/videotest.mp4");

                try(InputStream inputStream = Util.class.getResourceAsStream("/assets/videotest.mp4")) {
                    if(inputStream == null) {
                        System.err.println("Could not find videotest.mp4");
                        return;
                    }
                    Files.createDirectories(target.getParent());
                    Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                PlayBin playBin = new PlayBin("VideoPlayer");
                playBin.setInputFile(target.toFile());
                AppSink appSink = (AppSink) playBin.getElementByName("appsink");
                if (appSink == null) {
                    appSink = new AppSink("appsink");
                    playBin.setVideoSink(appSink);
                }

                Caps caps = Caps.fromString("video/x-raw,format=RGB");
                appSink.setCaps(caps);
                appSink.set("emit-signals", true);
                appSink.set("sync", false);

                appSink.connect(new AppSink.NEW_SAMPLE() {
                    @Override
                    public FlowReturn newSample(AppSink appSink) {
                        return FlowReturn.OK;
                    }
                });

                playBin.play();
                gstreamerInstalled = true;
                Gst.main();
            } catch (GstException ex) {
                ex.printStackTrace();
                gstreamerInstalled = false;
            }
        }).start();

        try {
            Thread.sleep(5000);
            System.out.println("Initialized GStreamer");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isGstreamerInstalled() {
        return gstreamerInstalled;
    }
}
