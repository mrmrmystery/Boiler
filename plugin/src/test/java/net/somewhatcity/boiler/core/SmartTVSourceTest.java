package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.somewhatcity.boiler.api.*;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.core.smarttv.SmartTvMenu;
import net.somewhatcity.boiler.core.smarttv.SmartTvSettings;
import net.somewhatcity.boiler.core.smarttv.SmartTvStyle;
import net.somewhatcity.boiler.core.smarttv.DisplaySetting;
import net.somewhatcity.boiler.core.sources.SmartTVSource;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SmartTVSourceTest {
    private SourceTestEnvironment env;
    private SmartTVSource tv;
    private Player player;
    private final AtomicReference<JsonObject> saved = new AtomicReference<>();

    @BeforeEach void setup() throws Exception {
        env = new SourceTestEnvironment();
        env.register("a", Recording.class); env.register("b", Parameters.class);
        env.registry.register("error", Recording.class, false);
        env.registry.register("default", Recording.class, false);
        env.register("smarttv", SmartTVSource.class);
        player = env.player();
        Recording.loads.set(0); Recording.unloads.set(0); Recording.keys.clear(); Recording.last = null;
        doAnswer(invocation -> { saved.set(((JsonObject) invocation.getArgument(0)).deepCopy()); return null; })
                .when(env.display).saveSourceData(any());
        doAnswer(invocation -> { tv.onInput(invocation.getArgument(0), invocation.getArgument(1)); return null; })
                .when(env.display).onInput(any(), anyString());
        tv = new SmartTVSource(); tv.load(env.display, new JsonObject());
    }
    @AfterEach void teardown() throws Exception { tv.unload(); env.close(); }

    private void awaitPlayback(int loads) throws Exception {
        env.await(() -> {
            tv.onKey(player, "TEST");
            return Recording.loads.get() == loads && Recording.keys.contains("TEST");
        });
        Recording.keys.clear();
    }

    private AsyncChatEvent chat(Player sender, String input) {
        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(sender); when(event.message()).thenReturn(Component.text(input));
        env.inputs.chat(event); return event;
    }

    @Test void navigatesPublicSourcesAndStopsPlaybackExactlyOnceOnBack() throws Exception {
        tv.onKey(player, "SPACE"); awaitPlayback(1);
        assertEquals("a", Recording.last.display.sourceName());
        assertEquals("a", saved.get().get("selected").getAsString());
        tv.onKey(player, "W"); assertEquals(List.of("W"), Recording.keys);
        tv.onKey(player, "SHIFT");
        assertEquals(1, Recording.unloads.get()); assertFalse(Recording.keys.contains("SHIFT"));
        tv.onKey(player, "A"); tv.onKey(player, "A"); tv.onKey(player, "SPACE");
        assertEquals(1, Recording.loads.get()); // Wraps to b; hidden/default/smarttv never appear.
        verify(chat(player, "abbrechen")).setCancelled(true);
        tv.onKey(player, "D"); tv.onKey(player, "D"); tv.onKey(player, "SPACE"); awaitPlayback(2);
        assertEquals("a", Recording.last.display.sourceName());
    }

    @Test void privatePromptsValidateInputRejectOtherPlayersAndRememberParameters() throws Exception {
        tv.onKey(player, "D"); tv.onKey(player, "SPACE");
        Player other = env.player();
        AsyncChatEvent ordinary = chat(other, "not a parameter"); verify(ordinary, never()).setCancelled(true);
        tv.onInput(other, "https://wrong.invalid");
        verify(chat(player, "https://example.invalid/video")).setCancelled(true);
        chat(player, "not-an-integer");
        chat(player, "8"); chat(player, "not-a-boolean"); chat(player, "true");
        awaitPlayback(1);
        assertEquals("b", Recording.last.display.sourceName());
        JsonObject data = Recording.last.data;
        assertEquals("https://example.invalid/video", data.get("url").getAsString());
        assertEquals(8, data.get("buffer").getAsInt()); assertTrue(data.get("loop").getAsBoolean());
        assertEquals(data, saved.get().getAsJsonObject("sources").getAsJsonObject("b"));
        assertEquals("b", saved.get().getAsJsonArray("recent").get(0).getAsString());
        tv.onKey(player, "SHIFT"); tv.onKey(player, "SPACE"); awaitPlayback(2);
        assertEquals(data, Recording.last.data);
        JsonObject restored = saved.get().deepCopy();
        tv.unload(); tv = new SmartTVSource(); tv.load(env.display, restored);
        assertEquals(2, Recording.loads.get()); // Restart opens the menu, not a media process.
        tv.onKey(player, "SPACE"); awaitPlayback(3);
        assertEquals(data, Recording.last.data);
    }

    @Test void permissionsAreCheckedForActionsAndSourceChanges() throws Exception {
        when(player.hasPermission("boiler.command.source")).thenReturn(false);
        tv.onKey(player, "SPACE"); assertEquals(0, Recording.loads.get());
        when(player.hasPermission("boiler.command.source")).thenReturn(true);
        when(player.hasPermission("boiler.command.action")).thenReturn(false);
        tv.onKey(player, "SPACE"); assertEquals(0, Recording.loads.get());
        when(player.hasPermission("boiler.command.action")).thenReturn(true);
        tv.onKey(player, "D"); tv.onKey(player, "SPACE");
        chat(player, "https://example.invalid");
        when(player.hasPermission("boiler.command.source")).thenReturn(false);
        chat(player, "4");
        verify(chat(player, "normal chat again"), never()).setCancelled(true);
        assertEquals(0, Recording.loads.get());
    }

    @Test void closingSessionAndUnloadReleaseChatCapture() {
        tv.onKey(player, "D"); tv.onKey(player, "SPACE");
        env.inputs.cancelPlayer(player);
        verify(chat(player, "normal"), never()).setCancelled(true);
        tv.onKey(player, "SPACE");
        tv.unload();
        verify(chat(player, "normal"), never()).setCancelled(true);
    }

    @Test void clickHitTestingUsesTheSameTilesAsPainting() throws Exception {
        SmartTvMenu.Layout layout = SmartTvMenu.layout(128, 128);
        Rectangle first = layout.tile(0);
        tv.onClick(player, first.x + 3, first.y + 3, false);
        awaitPlayback(1); assertEquals("a", Recording.last.display.sourceName());
    }

    @Test void parsesEveryArgumentTypeAndRejectsInvalidOrNonFiniteValues() {
        for (CreateArgument argument : AllTypes.class.getAnnotation(CreateCommandArguments.class).arguments()) {
            JsonObject data = new JsonObject();
            String value = switch (argument.type()) {
                case INTEGER -> "-42";
                case BOOLEAN -> "false";
                case DOUBLE -> "1.25";
                default -> "value with spaces";
            };
            SmartTVSource.parse(value, argument, data);
            assertTrue(data.has(argument.name()));
            if (argument.type() == CommandArgumentType.DOUBLE) {
                assertThrows(IllegalArgumentException.class, () -> SmartTVSource.parse("NaN", argument, data));
                assertThrows(IllegalArgumentException.class, () -> SmartTVSource.parse("Infinity", argument, data));
            }
            if (argument.type() == CommandArgumentType.INTEGER)
                assertThrows(IllegalArgumentException.class, () -> SmartTVSource.parse("2147483648", argument, data));
        }
    }

    @Test void rendersReadableMenuAtMapAndWideDisplaySizes() throws Exception {
        SmartTvMenu menu = new SmartTvMenu();
        List<String> apps = List.of("youtube", "twitch", "image", "client", "ffmpeg", "ffmpeg-buffered", "sysinfo", "keyboard", SmartTvStyle.SETTINGS);
        menu.view(new SmartTvMenu.View(apps, 1, "", false, List.of("youtube", "image", "twitch"), 0, SmartTvMenu.Focus.APPS, 0, null));
        Path directory = Path.of("build", "reports", "smarttv-preview"); Files.createDirectories(directory);
        for (int[] size : new int[][]{{128,128}, {384,128}, {384,256}, {256,384}, {768,512}, {1024,576}}) {
            int width = size[0], height = size[1];
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try { menu.draw(graphics, new Rectangle(0, 0, width, height)); } finally { graphics.dispose(); }
            javax.imageio.ImageIO.write(image, "png", directory.resolve("menu-" + width + "x" + height + ".png").toFile());
            SmartTvMenu.Layout layout = SmartTvMenu.layout(width, height);
            for (int slot = 0; slot < layout.pageSize(); slot++) {
                Rectangle tile = layout.tile(slot);
                assertTrue(new Rectangle(0, 0, width, height).contains(tile));
                assertEquals(slot, layout.hit(tile.x + 1, tile.y + 1, 0, 100));
            }
            SmartTvSettings.View settings = new SmartTvSettings.View(Arrays.stream(DisplaySetting.values()).map(option -> option.value(env.display)).toList(), 0);
            menu.view(new SmartTvMenu.View(apps, 1, "", false, List.of("youtube", "image", "twitch"), 0, SmartTvMenu.Focus.APPS, 0, settings));
            graphics = image.createGraphics();
            try { menu.draw(graphics, new Rectangle(0, 0, width, height)); } finally { graphics.dispose(); }
            javax.imageio.ImageIO.write(image, "png", directory.resolve("settings-" + width + "x" + height + ".png").toFile());
            menu.view(new SmartTvMenu.View(apps, 1, "", false, List.of("youtube", "image", "twitch"), 0, SmartTvMenu.Focus.APPS, 0, null));
        }
    }

    @Test void settingsAppWorksWithoutSourcePermissionAndPreservesSourceData() {
        when(player.hasPermission("boiler.command.source")).thenReturn(false);
        tv.onKey(player, "A"); tv.onKey(player, "SPACE"); // Settings is the last app.
        tv.onKey(player, "SPACE");
        assertTrue(env.display.settings().get("dither").getAsBoolean());
        assertNull(saved.get()); assertEquals(0, Recording.loads.get());
        when(player.hasPermission("boiler.command.settings")).thenReturn(false);
        tv.onKey(player, "SPACE");
        assertTrue(env.display.settings().get("dither").getAsBoolean());
        tv.onKey(player, "SHIFT");
        when(player.hasPermission("boiler.command.settings")).thenReturn(true);
        tv.onKey(player, "SPACE"); tv.onKey(player, "SPACE");
        assertFalse(env.display.settings().get("dither").getAsBoolean());
    }

    @Test void preciseSettingsInputIsPrivateValidatedAndCancelledWithTheSession() {
        tv.onKey(player, "A"); tv.onKey(player, "SPACE");
        for (int index = 0; index < DisplaySetting.RENDER_PERIOD.ordinal(); index++) tv.onKey(player, "S");
        tv.onKey(player, "SPACE");
        tv.onInput(env.player(), "1"); assertFalse(env.display.settings().has("renderPeriod"));
        verify(chat(player, "NaN")).setCancelled(true);
        chat(player, "0"); assertFalse(env.display.settings().has("renderPeriod"));
        chat(player, "33"); assertEquals(33, env.display.settings().get("renderPeriod").getAsInt());
        verify(chat(player, "normal"), never()).setCancelled(true);
        tv.onKey(player, "SPACE"); env.inputs.cancelPlayer(player);
        verify(chat(player, "normal"), never()).setCancelled(true);
    }

    @Test void largeHomeFiltersAppsAndOpensSettingsUsingSidebarOrRemote() throws Exception {
        when(env.display.width()).thenReturn(384); when(env.display.height()).thenReturn(256);
        env.register("youtube", Recording.class);
        tv.onKey(player, "W"); tv.onKey(player, "D"); tv.onKey(player, "SPACE"); // Video filter.
        tv.onKey(player, "SPACE"); awaitPlayback(1);
        assertEquals("youtube", Recording.last.display.sourceName());
        tv.onKey(player, "SHIFT");
        SmartTvMenu.Layout layout = SmartTvMenu.layout(384,256);
        Rectangle gear = layout.sidebar(2);
        tv.onClick(player, gear.x + 2, gear.y + 2, false);
        tv.onKey(player, "SPACE"); assertTrue(env.display.settings().get("dither").getAsBoolean());
        tv.onKey(player, "SHIFT");
        tv.onKey(player, "W"); tv.onKey(player, "W"); tv.onKey(player, "W"); tv.onKey(player, "SPACE"); // Sidebar wraps Home -> Settings.
        tv.onKey(player, "SPACE"); assertFalse(env.display.settings().get("dither").getAsBoolean());
    }

    public static class Recording implements IBoilerSource {
        static final AtomicInteger loads = new AtomicInteger(), unloads = new AtomicInteger();
        static final List<String> keys = new CopyOnWriteArrayList<>();
        static volatile Recording last;
        IBoilerDisplay display;
        JsonObject data;
        @Override public void load(IBoilerDisplay display, JsonObject data) {
            this.display = display; this.data = data.deepCopy(); last = this; loads.incrementAndGet();
        }
        @Override public void unload() { unloads.incrementAndGet(); }
        @Override public void onKey(CommandSender sender, String key) { keys.add(key); }
    }
    @CreateCommandArguments(arguments = {
            @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING),
            @CreateArgument(name = "buffer", type = CommandArgumentType.INTEGER),
            @CreateArgument(name = "loop", type = CommandArgumentType.BOOLEAN)
    })
    public static class Parameters extends Recording {}
    @CreateCommandArguments(arguments = {
            @CreateArgument(name = "string", type = CommandArgumentType.STRING),
            @CreateArgument(name = "greedy", type = CommandArgumentType.GREEDY_STRING),
            @CreateArgument(name = "integer", type = CommandArgumentType.INTEGER),
            @CreateArgument(name = "bool", type = CommandArgumentType.BOOLEAN),
            @CreateArgument(name = "double", type = CommandArgumentType.DOUBLE)
    })
    static class AllTypes {}
}
