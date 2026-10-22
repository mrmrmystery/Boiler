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

import de.pianoman911.mapengine.api.MapEngineApi;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.somewhatcity.boiler.api.BoilerApi;
import net.somewhatcity.boiler.api.IDisplayManager;
import net.somewhatcity.boiler.api.ISourceManager;
import net.somewhatcity.boiler.api.display.IGuiManager;
import net.somewhatcity.boiler.core.api.ImplBoilerApi;
import net.somewhatcity.boiler.core.audio.plasmovoice.PlasmoLoader;
import net.somewhatcity.boiler.core.audio.simplevoicechat.SvcLoader;
import net.somewhatcity.boiler.core.commands.BoilerCommand;
import net.somewhatcity.boiler.core.display.BoilerSourceManager;
import net.somewhatcity.boiler.core.display.ImplDisplayManager;
import net.somewhatcity.boiler.core.display.UpdateIntervalManager;
import net.somewhatcity.boiler.core.listener.BoilerListener;
import net.somewhatcity.boiler.core.listener.PlayerJoinListener;
import net.somewhatcity.boiler.core.network.HandshakeResponseListener;
import net.somewhatcity.boiler.core.network.StreamStateRequestListener;
import net.somewhatcity.boiler.core.sources.*;
import net.somewhatcity.boiler.core.sources.hidden.DefaultSource;
import net.somewhatcity.boiler.core.sources.hidden.ErrorSource;
import net.somewhatcity.boiler.core.sources.VideoConverterSource;
import net.somewhatcity.boiler.core.remote.RemoteManager;
import net.somewhatcity.boiler.core.smarttv.SmartTvInputs;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegLogCallback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import net.somewhatcity.boiler.core.runtime.BoilerRuntime;

public class BoilerPlugin extends JavaPlugin {

    public static final Executor EXECUTOR = task -> getPlugin().runtime().loaders().execute(task);
    public static MapEngineApi MAP_ENGINE;
    public static Logger LOGGER;
    private static BoilerPlugin plugin;
    private ImplBoilerApi api;
    private IDisplayManager displayManager;
    private ISourceManager sourceManager;
    private UpdateIntervalManager intervalManager;

    private MediaMtx mediaMtx;
    private MediaMtxAuthServer mediaMtxAuthServer;
    private BoilerRuntime runtime;
    private Metrics metrics;
    private RemoteManager remoteManager;
    private SmartTvInputs smartTvInputs;
    private volatile boolean stopping;

    public BoilerRuntime runtime() { return runtime; }
    public boolean stopping() { return stopping; }
    public <T> T callOnMain(java.util.concurrent.Callable<T> action) {
        if (stopping) throw new IllegalStateException("Boiler is stopping");
        if (Bukkit.isPrimaryThread()) {
            try { return action.call(); }
            catch (Exception e) { throw new IllegalStateException(e); }
        }
        java.util.concurrent.CompletableFuture<T> result = new java.util.concurrent.CompletableFuture<>();
        onMain(() -> {
            if (result.isCancelled()) return;
            try {
                T value = action.call();
                // Cancellation can race with a resource being created on the server thread.
                if (!result.complete(value)) {
                    if (value instanceof AutoCloseable resource) resource.close();
                    else if (value instanceof org.bukkit.scheduler.BukkitTask task) task.cancel();
                }
            }
            catch (Exception e) { result.completeExceptionally(e); }
        });
        try { return result.get(15, java.util.concurrent.TimeUnit.SECONDS); }
        catch (InterruptedException e) {
            result.cancel(false);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Source loading cancelled", e);
        } catch (Exception e) {
            result.cancel(false);
            throw new IllegalStateException(e);
        }
    }
    public void onMain(Runnable action) {
        if (stopping) return;
        if (Bukkit.isPrimaryThread()) action.run();
        else if (isEnabled()) Bukkit.getScheduler().runTask(this, () -> { if (!stopping) action.run(); });
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        stopping = false;
        runtime = new BoilerRuntime();

        FFmpegLogCallback.set();
        avutil.av_log_set_level(avutil.AV_LOG_ERROR);

        metrics = new Metrics(this,18926);
        new BoilerConfig(this);

        if(BoilerConfig.clientEnabled) {
            mediaMtxAuthServer = new MediaMtxAuthServer();
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if(os.contains("win")) {
                saveResource("mediamtx.exe", true);
            }
            else if(os.contains("nux")) {
                saveResource("mediamtx", true);

                try {
                    File mediaMtx = new File(BoilerPlugin.getPlugin().getDataFolder(), "mediamtx");
                    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
                    Files.setPosixFilePermissions(mediaMtx.toPath(), permissions);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            saveResource("mediamtx.yml", false);
            mediaMtx = new MediaMtx();
            mediaMtx.start();
        }


        Util.initGstreamer();

        LOGGER = Bukkit.getLogger();
        MAP_ENGINE = Bukkit.getServicesManager().getRegistration(MapEngineApi.class).getProvider();

        //this.platform = PlatformUtil.getPlatform(this, this.getClassLoader(), new ImplListenerBridge());

        intervalManager = new UpdateIntervalManager(this);

        if(Util.isPluginInstalled("voicechat")) SvcLoader.loadSimpleVoiceChat();
        if(Util.isPluginInstalled("PlasmoVoice")) PlasmoLoader.loadPlasmoVoice();


        this.displayManager = new ImplDisplayManager(this);

        this.sourceManager = new BoilerSourceManager(this);
        this.sourceManager.register("default", DefaultSource.class, false);
        this.sourceManager.register("error", ErrorSource.class, false);

        this.sourceManager.register("image", ImageSource.class);
        this.sourceManager.register("ffmpeg", FFMPEGSource.class);
        this.sourceManager.register("ffmpeg-buffered", BufferedFFMPEGSource.class);
        this.sourceManager.register("twitch", TwitchSource.class);
        this.sourceManager.register("youtube", YoutubeSource.class);
        this.sourceManager.register("rtmp-server", RTMPSource.class);
        this.sourceManager.register("clone", CloneSource.class);
        this.sourceManager.register("swing", SwingTest.class);
        this.sourceManager.register("section-clone", SectionCloneSource.class);
        this.sourceManager.register("keyboard", KeyboardSource.class);
        this.sourceManager.register("vlc", VlcSource.class);
        this.sourceManager.register("converter", VideoConverterSource.class);
        this.sourceManager.register("gstreamer", GstreamerSource.class);
        this.sourceManager.register("sysinfo", SystemInfoSource.class);
        this.sourceManager.register("client", BoilerClientSource.class);
        this.sourceManager.register("client-stream", BoilerClientStreamSource.class);
        this.sourceManager.register("smarttv", SmartTVSource.class);


        this.api = new ImplBoilerApi(this);
        Bukkit.getServicesManager().register(BoilerApi.class, api, this, ServicePriority.Normal);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new BoilerListener(), this);
        pm.registerEvents(new PlayerJoinListener(), this);
        smartTvInputs = new SmartTvInputs(this);
        remoteManager = new RemoteManager(this);
        pm.registerEvents(smartTvInputs, this);
        pm.registerEvents(remoteManager, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands ->
                commands.registrar().register(BoilerCommand.create(this))
        );

        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "boiler:handshake_request");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "mymod:test");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "boiler:display_create");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "boiler:display_remove");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "boiler:stream_data");
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(this, "boiler:stream_state_response");

        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "boiler:stream_state_request", new StreamStateRequestListener());
        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(this, "boiler:handshake_response", new HandshakeResponseListener());
    }

    public static boolean hasBoilerMod(Player player) {
        return player.hasMetadata("boiler_mod") && player.getMetadata("boiler_mod").get(0).asBoolean();
    }

    @Override
    public void onDisable() {
        stopping = true;
        if (remoteManager != null) closeSafely("remote controls", remoteManager::close);
        if (smartTvInputs != null) closeSafely("SmartTV inputs", smartTvInputs::close);
        if (displayManager instanceof ImplDisplayManager manager) closeSafely("displays", manager::close);
        closeSafely("GStreamer", Util::shutdownGstreamer);
        if (mediaMtx != null) closeSafely("MediaMTX", mediaMtx::stop);
        if (mediaMtxAuthServer != null) closeSafely("authentication server", mediaMtxAuthServer::stop);
        if (metrics != null) closeSafely("metrics", metrics::shutdown);
        if (runtime != null) closeSafely("background runtime", runtime::close);
    }
    private void closeSafely(String resource, Runnable close) {
        try { close.run(); }
        catch (Exception e) { getLogger().log(java.util.logging.Level.WARNING, "Closing " + resource, e); }
    }
    public IDisplayManager displayManager() {
        return displayManager;
    }
    public ISourceManager sourceManager() {
        return sourceManager;
    }
    public RemoteManager remoteManager() { return remoteManager; }
    public SmartTvInputs smartTvInputs() { return smartTvInputs; }
    public UpdateIntervalManager intervalManager() {
        return intervalManager;
    }

    public MediaMtxAuthServer mediaMtxAuthServer() {
        return mediaMtxAuthServer;
    }
    public MediaMtx mediaMtx() { return mediaMtx; }

    public static BoilerPlugin getPlugin() {
        return plugin;
    }
}
