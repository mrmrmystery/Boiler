package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.SourceType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import net.somewhatcity.boiler.core.Util;
import net.somewhatcity.boiler.core.display.CompositeSource;
import net.somewhatcity.boiler.core.display.RasterRenderer;
import net.somewhatcity.boiler.core.display.SourceSession;
import net.somewhatcity.boiler.core.smarttv.SmartTvMenu;
import net.somewhatcity.boiler.core.smarttv.SmartTvSettings;
import net.somewhatcity.boiler.core.smarttv.SmartTvStyle;
import net.somewhatcity.boiler.core.smarttv.DisplaySetting;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class SmartTVSource implements IBoilerSource, CompositeSource {
    private record SettingPrompt(UUID player, DisplaySetting option) {}
    private static final class Prompt {
        final UUID player;
        final String source;
        final CreateArgument[] arguments;
        final JsonObject data;
        int index;
        Prompt(Player player, String source, CreateArgument[] arguments, JsonObject data) {
            this.player = player.getUniqueId(); this.source = source; this.arguments = arguments; this.data = data;
        }
    }
    private BoilerPlugin plugin;
    private IBoilerDisplay display;
    private SourceSession child;
    private final SmartTvMenu menu = new SmartTvMenu();
    private final RasterRenderer raster = new RasterRenderer();
    private JsonObject parameters;
    private String selectedName = "";
    private List<String> sources = List.of();
    private int selected;
    private Prompt prompt;
    private SettingPrompt settingPrompt;
    private final List<String> recent = new ArrayList<>();
    private SmartTvMenu.Focus focus = SmartTvMenu.Focus.APPS;
    private int focusIndex, category, settingsSelected;
    private boolean settingsOpen;
    private volatile boolean playing;
    private volatile boolean stopped;

    @Override public void load(IBoilerDisplay display, JsonObject data) {
        plugin = BoilerPlugin.getPlugin();
        this.display = display;
        parameters = data != null && data.has("sources") && data.get("sources").isJsonObject()
                ? data.getAsJsonObject("sources").deepCopy() : new JsonObject();
        if (data != null && data.has("selected") && data.get("selected").isJsonPrimitive()) selectedName = data.get("selected").getAsString();
        if (data != null && data.has("recent") && data.get("recent").isJsonArray())
            data.getAsJsonArray("recent").forEach(value -> { if (value.isJsonPrimitive() && !recent.contains(value.getAsString()) && recent.size() < 6) recent.add(value.getAsString()); });
        child = new SourceSession(plugin, display, this::changed, this::saveParameters);
        refreshSources();
        updateMenu("");
    }

    private void refreshSources() {
        sources = new ArrayList<>(availableSources().stream()
                .filter(name -> category == 0 || SmartTvStyle.category(name) == category).toList());
        sources.add(SmartTvStyle.SETTINGS);
        int remembered = sources.indexOf(selectedName);
        selected = remembered >= 0 ? remembered : Math.min(selected, Math.max(0, sources.size() - 1));
    }

    private List<String> availableSources() {
        return plugin.sourceManager().commandVisibleSourceNames().stream()
                .filter(name -> !name.equals("smarttv") && !name.equals("default") && !name.equals("error"))
                .filter(name -> plugin.sourceManager().source(name) != null).sorted().toList();
    }

    private void updateMenu(String message) {
        SmartTvSettings.View settings = settingsOpen ? new SmartTvSettings.View(
                Arrays.stream(DisplaySetting.values()).map(option -> option.value(display)).toList(), settingsSelected) : null;
        menu.view(new SmartTvMenu.View(sources, selected, message, playing,
                recent.stream().filter(availableSources()::contains).toList(), category, focus, focusIndex, settings));
    }

    private void changed() {
        if (stopped) return;
        updateMenu(child.status());
        display.respawn();
        display.tick();
    }

    private void saveParameters(String name, JsonObject values) {
        if (stopped) return;
        parameters.add(name, values.deepCopy());
        JsonObject state = new JsonObject();
        state.addProperty("selected", selectedName);
        state.add("sources", parameters.deepCopy());
        com.google.gson.JsonArray history = new com.google.gson.JsonArray();
        recent.forEach(history::add);
        state.add("recent", history);
        display.saveSourceData(state);
    }

    @Override public SourceType effectiveSourceType() { return playing && child != null ? child.sourceType() : null; }

    private boolean allowed(CommandSender sender) { return !stopped && sender.hasPermission("boiler.command.action"); }

    private boolean busy(CommandSender sender) {
        UUID owner = promptOwner();
        if (owner == null) return false;
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(owner))
            Util.sendErrMsg(sender, "Ein anderer Spieler verwendet gerade die SmartTV-Eingabe.");
        return true;
    }

    private UUID promptOwner() { return prompt != null ? prompt.player : settingPrompt != null ? settingPrompt.player() : null; }

    @Override public void onKey(CommandSender sender, String key) {
        if (!allowed(sender)) return;
        String normalized = key.equals(" ") ? "SPACE" : key.toUpperCase(Locale.ROOT);
        if (normalized.equals("SHIFT") || normalized.equals("ESCAPE")) {
            if (promptOwner() != null) {
                if (sender instanceof Player player && player.getUniqueId().equals(promptOwner())) cancelPrompt();
                else busy(sender);
            } else if (settingsOpen) { settingsOpen = false; updateMenu(""); }
            else if (playing) back();
            return;
        }
        if (busy(sender)) return;
        if (settingsOpen) { settingsKey(sender, normalized); return; }
        if (playing) { child.withSource(source -> source.onKey(sender, key)); return; }
        refreshSources();
        if (SmartTvMenu.layout(display.width(), display.height()).large()) { homeKey(sender, normalized); return; }
        int columns = SmartTvMenu.layout(display.width(), display.height()).columns();
        switch (normalized) {
            case "W", "ARROW_UP" -> navigate(-columns);
            case "S", "ARROW_DOWN" -> navigate(columns);
            case "A", "ARROW_LEFT" -> navigate(-1);
            case "D", "ARROW_RIGHT" -> navigate(1);
            case "SPACE", "ENTER" -> choose(sender, false);
            default -> { }
        }
    }

    private void navigate(int offset) {
        if (!sources.isEmpty()) {
            selected = Math.floorMod(selected + offset, sources.size());
            selectedName = sources.get(selected);
        }
        updateMenu("");
    }

    @Override public void onClick(CommandSender sender, int x, int y, boolean right) {
        if (!allowed(sender) || busy(sender)) return;
        if (settingsOpen) { settingsClick(sender, x, y, right); return; }
        if (playing) { child.withSource(source -> source.onClick(sender, x, y, right)); return; }
        refreshSources();
        SmartTvMenu.Layout layout = SmartTvMenu.layout(display.width(), display.height());
        if (layout.large()) {
            for (int index = 0; index < 3; index++) if (layout.sidebar(index).contains(x, y)) { sidebar(sender, index); return; }
            for (int index = 0; index < SmartTvStyle.CATEGORIES.size(); index++) if (layout.category(index).contains(x, y)) { filter(index); return; }
            List<String> cards = recentCards();
            for (int index = 0; index < Math.min(cards.size(), layout.recentCount()); index++)
                if (layout.recent(index).contains(x, y)) { selectSource(sender, cards.get(index), right); return; }
        }
        if (!layout.large() && y >= display.height() - 14 || layout.large() && layout.pages().contains(x, y)) {
            if (!sources.isEmpty()) {
                int pages = (sources.size() + layout.pageSize() - 1) / layout.pageSize();
                int middle = layout.large() ? layout.pages().x + layout.pages().width / 2 : display.width() / 2;
                int page = Math.floorMod(selected / layout.pageSize() + (x < middle ? -1 : 1), pages);
                selected = Math.min(sources.size() - 1, page * layout.pageSize() + selected % layout.pageSize());
                selectedName = sources.get(selected);
                updateMenu("");
            }
            return;
        }
        int index = layout.hit(x, y, selected / layout.pageSize(), sources.size());
        if (index < 0) return;
        selected = index;
        focus = SmartTvMenu.Focus.APPS;
        selectedName = sources.get(selected);
        choose(sender, right);
    }

    @Override public void onScroll(CommandSender sender, int x, int y, int delta) {
        if (!allowed(sender) || busy(sender)) return;
        if (settingsOpen) { settingsSelected = Math.floorMod(settingsSelected + Integer.signum(delta), DisplaySetting.values().length); updateMenu(""); }
        else if (playing) child.withSource(source -> source.onScroll(sender, x, y, delta));
        else navigate(Integer.signum(delta));
    }

    private List<String> recentCards() {
        List<String> available = availableSources();
        List<String> remembered = recent.stream().filter(available::contains).toList();
        if (!remembered.isEmpty()) return remembered;
        return sources.stream().filter(source -> !source.equals(SmartTvStyle.SETTINGS)).toList();
    }

    private void filter(int value) {
        category = value;
        selected = 0;
        selectedName = "";
        focus = SmartTvMenu.Focus.APPS;
        refreshSources();
        updateMenu("");
    }

    private void selectSource(CommandSender sender, String name, boolean edit) {
        category = 0;
        selectedName = name;
        refreshSources();
        focus = SmartTvMenu.Focus.APPS;
        choose(sender, edit);
    }

    private void sidebar(CommandSender sender, int index) {
        if (index == 2) openSettings(sender);
        else filter(0);
    }

    private void homeKey(CommandSender sender, String key) {
        boolean up = key.equals("W") || key.equals("ARROW_UP"), down = key.equals("S") || key.equals("ARROW_DOWN");
        boolean left = key.equals("A") || key.equals("ARROW_LEFT"), right = key.equals("D") || key.equals("ARROW_RIGHT");
        boolean confirm = key.equals("SPACE") || key.equals("ENTER");
        switch (focus) {
            case APPS -> {
                if (left || right) navigate(left ? -1 : 1);
                else if (up) { focus = SmartTvMenu.Focus.CATEGORIES; focusIndex = category; }
                else if (down && !recentCards().isEmpty()) { focus = SmartTvMenu.Focus.RECENT; focusIndex = 0; }
                else if (confirm) choose(sender, false);
            }
            case CATEGORIES -> {
                if (left || right) focusIndex = Math.floorMod(focusIndex + (left ? -1 : 1), SmartTvStyle.CATEGORIES.size());
                else if (up) { focus = SmartTvMenu.Focus.SIDEBAR; focusIndex = 0; }
                else if (down) focus = SmartTvMenu.Focus.APPS;
                else if (confirm) filter(focusIndex);
            }
            case RECENT -> {
                int count = Math.min(recentCards().size(), SmartTvMenu.layout(display.width(), display.height()).recentCount());
                if (count == 0 || up) focus = SmartTvMenu.Focus.APPS;
                else if (left || right) focusIndex = Math.floorMod(focusIndex + (left ? -1 : 1), count);
                else if (down) { focus = SmartTvMenu.Focus.CATEGORIES; focusIndex = category; }
                else if (confirm) selectSource(sender, recentCards().get(Math.min(focusIndex, count - 1)), false);
            }
            case SIDEBAR -> {
                if (up || down) focusIndex = Math.floorMod(focusIndex + (up ? -1 : 1), 3);
                else if (right) focus = SmartTvMenu.Focus.APPS;
                else if (confirm) sidebar(sender, focusIndex);
            }
        }
        // choose() may have opened an input prompt, which supplies its own status text.
        if (promptOwner() == null) updateMenu(playing ? child.status() : "");
    }

    private boolean settingsAllowed(CommandSender sender) {
        if (sender.hasPermission("boiler.command.settings")) return true;
        Util.sendErrMsg(sender, "Keine Berechtigung für Displayeinstellungen (boiler.command.settings).");
        return false;
    }

    private void openSettings(CommandSender sender) {
        if (!settingsAllowed(sender)) return;
        settingsOpen = true;
        settingsSelected = 0;
        updateMenu("");
    }

    private void settingsKey(CommandSender sender, String key) {
        switch (key) {
            case "W", "ARROW_UP" -> settingsSelected = Math.floorMod(settingsSelected - 1, DisplaySetting.values().length);
            case "S", "ARROW_DOWN" -> settingsSelected = (settingsSelected + 1) % DisplaySetting.values().length;
            case "A", "ARROW_LEFT" -> { adjustSetting(sender, -1); return; }
            case "D", "ARROW_RIGHT" -> { adjustSetting(sender, 1); return; }
            case "SPACE", "ENTER" -> { confirmSetting(sender); return; }
            default -> { }
        }
        updateMenu("");
    }

    private void adjustSetting(CommandSender sender, int delta) {
        if (!settingsAllowed(sender)) return;
        try {
            DisplaySetting.values()[settingsSelected].adjust(display, delta);
            updateMenu("Gespeichert");
        } catch (IllegalArgumentException e) {
            Util.sendErrMsg(sender, "Ungültiger Einstellungswert: %s", e.getMessage());
        }
    }

    private void confirmSetting(CommandSender sender) {
        if (!settingsAllowed(sender)) return;
        DisplaySetting option = DisplaySetting.values()[settingsSelected];
        if (option.kind == DisplaySetting.Kind.TOGGLE) {
            option.apply(display, Boolean.toString(!Boolean.parseBoolean(option.raw(display))));
            updateMenu("Gespeichert");
        } else if (option.kind == DisplaySetting.Kind.DIRECTION) adjustSetting(sender, 1);
        else if (sender instanceof Player player) {
            settingPrompt = new SettingPrompt(player.getUniqueId(), option);
            if (!plugin.smartTvInputs().request(this, player, input -> display.onInput(player, input), this::cancelPrompt)) {
                settingPrompt = null;
                Util.sendErrMsg(player, "Du hast bereits eine andere SmartTV-Eingabe geöffnet.");
                return;
            }
            Util.sendMsg(player, "%s: Neuen Zahlenwert im Chat eingeben (aktuell %s). 'abbrechen' beendet die Eingabe.", option.label, option.raw(display));
            updateMenu("Chat-Eingabe: " + option.label);
        } else Util.sendErrMsg(sender, "Die Eingabe benötigt einen Spieler.");
    }

    private void settingsClick(CommandSender sender, int x, int y, boolean right) {
        SmartTvSettings.Layout layout = new SmartTvSettings.Layout(display.width(), display.height(), settingsSelected);
        if (layout.back().contains(x, y)) { settingsOpen = false; updateMenu(""); return; }
        for (DisplaySetting.Group group : DisplaySetting.Group.values()) if (layout.category(group.ordinal()).contains(x, y)) {
            settingsSelected = Arrays.stream(DisplaySetting.values()).filter(option -> option.group == group).findFirst().orElseThrow().ordinal();
            updateMenu(""); return;
        }
        int option = layout.hit(x, y);
        if (option < 0) return;
        settingsSelected = option;
        if (right) {
            if (settingsAllowed(sender)) { DisplaySetting.values()[option].reset(display); updateMenu("Standard wiederhergestellt"); }
            return;
        }
        int delta = layout.adjustment(option, x);
        if (DisplaySetting.values()[option].kind != DisplaySetting.Kind.TOGGLE && delta != 0) adjustSetting(sender, delta);
        else confirmSetting(sender);
    }

    private void settingInput(CommandSender sender, String input) {
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(settingPrompt.player())) { busy(sender); return; }
        plugin.smartTvInputs().touch(this, player);
        if (!settingsAllowed(sender) || input.trim().equalsIgnoreCase("abbrechen")) { cancelPrompt(); return; }
        try { settingPrompt.option().apply(display, input); }
        catch (IllegalArgumentException e) {
            Util.sendErrMsg(sender, "Ungültiger Wert: %s. Bitte erneut eingeben oder 'abbrechen'.", e.getMessage());
            return;
        }
        settingPrompt = null;
        plugin.smartTvInputs().release(this);
        updateMenu("Gespeichert");
    }

    private void choose(CommandSender sender, boolean edit) {
        if (!sources.isEmpty() && sources.get(selected).equals(SmartTvStyle.SETTINGS)) { openSettings(sender); return; }
        if (!sender.hasPermission("boiler.command.source")) {
            Util.sendErrMsg(sender, "Keine Berechtigung zum Wechseln der Quelle (boiler.command.source)."); return;
        }
        if (sources.isEmpty()) { updateMenu("Keine Quellen verfügbar"); return; }
        selectedName = sources.get(selected);
        Class<? extends IBoilerSource> type = plugin.sourceManager().source(selectedName);
        if (type == null) { refreshSources(); updateMenu("Quelle nicht mehr verfügbar"); return; }
        CreateCommandArguments annotation = type.getAnnotation(CreateCommandArguments.class);
        CreateArgument[] arguments = annotation == null ? new CreateArgument[0] : annotation.arguments();
        JsonObject remembered = parameters.has(selectedName) && parameters.get(selectedName).isJsonObject()
                ? parameters.getAsJsonObject(selectedName).deepCopy() : new JsonObject();
        boolean complete = true;
        for (CreateArgument argument : arguments) {
            try {
                if (!remembered.has(argument.name())) { complete = false; break; }
                parse(remembered.get(argument.name()).getAsString(), argument, new JsonObject());
            } catch (RuntimeException e) { complete = false; break; }
        }
        if (arguments.length == 0 || (!edit && complete)) { play(selectedName, remembered); return; }
        if (!(sender instanceof Player player)) {
            Util.sendErrMsg(sender, "Die Einrichtung benötigt einen Spieler für die Chat-Abfrage."); return;
        }
        prompt = new Prompt(player, selectedName, arguments, remembered);
        if (!plugin.smartTvInputs().request(this, player, input -> display.onInput(player, input), this::cancelPrompt)) {
            prompt = null;
            Util.sendErrMsg(player, "Du hast bereits eine andere SmartTV-Eingabe geöffnet."); return;
        }
        ask(player);
    }

    private void ask(Player player) {
        CreateArgument argument = prompt.arguments[prompt.index];
        String previous = prompt.data.has(argument.name()) ? " (Punkt = bisherigen Wert übernehmen)" : "";
        Util.sendMsg(player, "SmartTV %s: %s (%s) im Chat eingeben%s. 'abbrechen' beendet die Eingabe.",
                prompt.source, argument.name(), argument.type(), previous);
        updateMenu("Chat: " + argument.name());
    }

    public static void parse(String input, CreateArgument argument, JsonObject target) {
        switch (argument.type()) {
            case STRING, GREEDY_STRING -> {
                if (input.isBlank()) throw new IllegalArgumentException("Text darf nicht leer sein");
                target.addProperty(argument.name(), input);
            }
            case INTEGER -> target.addProperty(argument.name(), Integer.parseInt(input.trim()));
            case DOUBLE -> {
                double value = Double.parseDouble(input.trim());
                if (!Double.isFinite(value)) throw new IllegalArgumentException("Endliche Zahl erwartet");
                target.addProperty(argument.name(), value);
            }
            case BOOLEAN -> {
                if (!input.trim().equalsIgnoreCase("true") && !input.trim().equalsIgnoreCase("false"))
                    throw new IllegalArgumentException("true oder false erwartet");
                target.addProperty(argument.name(), Boolean.parseBoolean(input.trim()));
            }
        }
    }

    @Override public void onInput(CommandSender sender, String input) {
        if (!allowed(sender)) {
            if (promptOwner() != null && sender instanceof Player player && player.getUniqueId().equals(promptOwner())) cancelPrompt();
            return;
        }
        if (settingPrompt != null) { settingInput(sender, input); return; }
        if (prompt == null) {
            if (playing) child.withSource(source -> source.onInput(sender, input));
            return;
        }
        if (!(sender instanceof Player player) || !player.getUniqueId().equals(prompt.player)) { busy(sender); return; }
        plugin.smartTvInputs().touch(this, player);
        if (!sender.hasPermission("boiler.command.source") || input.trim().equalsIgnoreCase("abbrechen")) { cancelPrompt(); return; }
        CreateArgument argument = prompt.arguments[prompt.index];
        try {
            String value = input.equals(".") && prompt.data.has(argument.name()) ? prompt.data.get(argument.name()).getAsString() : input;
            parse(value, argument, prompt.data);
        } catch (RuntimeException e) {
            Util.sendErrMsg(sender, "Ungültiger Wert für %s (%s). Bitte erneut eingeben.", argument.name(), argument.type());
            ask(player); return;
        }
        if (++prompt.index < prompt.arguments.length) { ask(player); return; }
        Prompt completed = prompt;
        prompt = null;
        plugin.smartTvInputs().release(this);
        play(completed.source, completed.data);
    }

    private void play(String name, JsonObject data) {
        recent.remove(name);
        recent.addFirst(name);
        if (recent.size() > 6) recent.removeLast();
        playing = true;
        child.source(name, data);
    }

    private void cancelPrompt() {
        prompt = null;
        settingPrompt = null;
        if (plugin.smartTvInputs() != null) plugin.smartTvInputs().release(this);
        updateMenu("Eingabe abgebrochen");
    }

    private void back() {
        playing = false;
        child.stop();
        refreshSources();
        updateMenu("");
    }

    @Override public void draw(IDrawingSpace drawingSpace) {
        if (playing && child.source() != null) child.draw(drawingSpace, display.width(), display.height());
        else raster.draw(menu, drawingSpace, display.width(), display.height());
    }
    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        if (playing && child.source() != null) child.draw(graphics, viewport); else menu.draw(graphics, viewport);
    }
    @Override public byte[] provide20msAudio() { return playing ? child.audio() : null; }
    @Override public void onResize() { child.withSource(IBoilerSource::onResize); }
    @Override public void onCursorMove(CommandSender sender, int x, int y) {
        if (allowed(sender) && playing) child.withSource(source -> source.onCursorMove(sender, x, y));
    }
    @Override public void unload() {
        if (stopped) return;
        stopped = true;
        plugin.onMain(() -> { if (plugin.smartTvInputs() != null) plugin.smartTvInputs().release(this); });
        if (child != null) child.close();
        raster.clear();
        menu.unload();
    }
}
