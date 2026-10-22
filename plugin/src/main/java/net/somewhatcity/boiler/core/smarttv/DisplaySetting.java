package net.somewhatcity.boiler.core.smarttv;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.BoilerConfig;

import java.util.List;
import java.util.Locale;

/** The display options exposed by /boiler display settings, with their actual runtime defaults. */
public enum DisplaySetting {
    DITHER(Group.PICTURE, "dither", "Farbdithering", "Gleicht Farben an die Minecraft-Kartenpalette an. Für Fotos und Verläufe, mit etwas höherem Rechenaufwand.", Kind.TOGGLE, 0, 1, false),
    GLOWING(Group.PICTURE, "glowing", "Leuchtende Rahmen", "Lässt die Kartenrahmen unabhängig vom Umgebungslicht leuchten.", Kind.TOGGLE, 0, 1, true),
    ITEM_ROTATION(Group.PICTURE, "itemRotation", "Kartendrehung", "Dreht die Karten innerhalb ihrer Rahmen. Acht Stufen von 0 bis 7.", Kind.INTEGER, 0, 1, true),
    DIRECTION(Group.PICTURE, "visualDirection", "Blickrichtung", "Bestimmt die sichtbare Ausrichtung des Displays: oben, unten oder eine Himmelsrichtung.", Kind.DIRECTION, 0, 1, true),
    YAW(Group.PICTURE, "yaw", "Drehung horizontal", "Horizontaler Drehwinkel des Displays in Grad. Präzise Werte mit Springen eingeben.", Kind.ANGLE, 0, 5, true),
    PITCH(Group.PICTURE, "pitch", "Drehung vertikal", "Vertikaler Drehwinkel des Displays in Grad. Präzise Werte mit Springen eingeben.", Kind.ANGLE, 0, 5, true),
    BUFFER(Group.PERFORMANCE, "buffer", "Bildpuffer", "Nutzt den MapEngine-Bildpuffer für die Übertragung der Displaybilder.", Kind.TOGGLE, 0, 1, false),
    BUNDLE(Group.PERFORMANCE, "bundle", "Paketbündelung", "Bündelt MapEngine-Pakete für die Übertragung an die Spieler.", Kind.TOGGLE, 0, 1, false),
    RENDER_PERIOD(Group.PERFORMANCE, "renderPeriod", "Renderintervall", "Zeit zwischen zwei Bildern in Millisekunden. Kleinere Werte liefern mehr Bilder und benötigen mehr Rechenleistung.", Kind.INTEGER, 1, 5, false),
    VIEW_DISTANCE(Group.PERFORMANCE, "viewDistance", "Sichtweite", "Entfernung in Blöcken, bis zu der Spieler das Display empfangen.", Kind.INTEGER, 1, 5, false),
    SOUND_DISTANCE(Group.AUDIO, "soundDistance", "Tonreichweite", "Reichweite der Display-Lautsprecher in Blöcken. Gilt beim nächsten Start einer Audioquelle.", Kind.INTEGER, 1, 5, false);

    public enum Group {
        PICTURE("Bild", "picture"), PERFORMANCE("Leistung", "settings"), AUDIO("Ton", "audio");
        public final String label, icon;
        Group(String label, String icon) { this.label = label; this.icon = icon; }
    }
    public enum Kind { TOGGLE, INTEGER, DIRECTION, ANGLE }
    public record Value(DisplaySetting option, String text, boolean enabled) {}
    private static final List<String> DIRECTIONS = List.of("UP", "DOWN", "NORTH", "EAST", "SOUTH", "WEST");
    private static final List<String> DIRECTION_LABELS = List.of("Oben", "Unten", "Nord", "Ost", "Süd", "West");
    public final Group group;
    public final String key, label, description;
    public final Kind kind;
    private final int minimum, step;
    private final boolean respawn;

    DisplaySetting(Group group, String key, String label, String description, Kind kind, int minimum, int step, boolean respawn) {
        this.group = group; this.key = key; this.label = label; this.description = description;
        this.kind = kind; this.minimum = minimum; this.step = step; this.respawn = respawn;
    }

    public String raw(IBoilerDisplay display) {
        JsonObject settings = display.settings();
        if (kind == Kind.ANGLE) {
            if (settings.has("rotation") && settings.get("rotation").isJsonObject() && settings.getAsJsonObject("rotation").has(key))
                return settings.getAsJsonObject("rotation").get(key).getAsString();
            return "0";
        }
        if (settings.has(key)) return settings.get(key).getAsString();
        return switch (this) {
            case BUFFER, DITHER, GLOWING, BUNDLE -> "false";
            case RENDER_PERIOD -> "20";
            case VIEW_DISTANCE -> Integer.toString(BoilerConfig.viewDistance);
            case SOUND_DISTANCE -> "100";
            case DIRECTION -> display.facing().name();
            default -> "0";
        };
    }

    public Value value(IBoilerDisplay display) {
        String raw = raw(display), formatted = raw;
        if (kind == Kind.TOGGLE) formatted = Boolean.parseBoolean(raw) ? "Ein" : "Aus";
        else if (kind == Kind.DIRECTION && DIRECTIONS.contains(raw)) formatted = DIRECTION_LABELS.get(DIRECTIONS.indexOf(raw));
        else if (kind == Kind.ANGLE) formatted = format(Float.parseFloat(raw)) + "°";
        else if (this == RENDER_PERIOD) formatted += " ms";
        else if (this == VIEW_DISTANCE || this == SOUND_DISTANCE) formatted += " Bl.";
        return new Value(this, formatted, kind == Kind.TOGGLE && Boolean.parseBoolean(raw));
    }

    public void adjust(IBoilerDisplay display, int delta) {
        String raw = raw(display);
        switch (kind) {
            case TOGGLE -> apply(display, Boolean.toString(delta > 0));
            case DIRECTION -> apply(display, DIRECTIONS.get(Math.floorMod(DIRECTIONS.indexOf(raw) + delta, DIRECTIONS.size())));
            case INTEGER -> {
                long value = Long.parseLong(raw) + (long) delta * step;
                apply(display, Long.toString(Math.max(minimum, Math.min(this == ITEM_ROTATION ? 7 : Integer.MAX_VALUE, value))));
            }
            case ANGLE -> apply(display, format(Float.parseFloat(raw) + delta * step));
        }
    }

    public void apply(IBoilerDisplay display, String input) {
        String trimmed = input.trim();
        JsonObject settings = display.settings().deepCopy();
        switch (kind) {
            case TOGGLE -> {
                if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) throw new IllegalArgumentException("true oder false erwartet");
                settings.addProperty(key, Boolean.parseBoolean(trimmed));
            }
            case INTEGER -> {
                int value = Integer.parseInt(trimmed);
                if (value < minimum || this == ITEM_ROTATION && value > 7) throw new IllegalArgumentException("Wert außerhalb des gültigen Bereichs");
                settings.addProperty(key, value);
            }
            case DIRECTION -> {
                String value = trimmed.toUpperCase(Locale.ROOT);
                if (!DIRECTIONS.contains(value)) throw new IllegalArgumentException("Ungültige Blickrichtung");
                settings.addProperty(key, value);
            }
            case ANGLE -> {
                float value = Float.parseFloat(trimmed);
                if (!Float.isFinite(value)) throw new IllegalArgumentException("Endlicher Winkel erwartet");
                JsonObject rotation = settings.has("rotation") && settings.get("rotation").isJsonObject()
                        ? settings.getAsJsonObject("rotation") : new JsonObject();
                if (!rotation.has("yaw")) rotation.addProperty("yaw", 0);
                if (!rotation.has("pitch")) rotation.addProperty("pitch", 0);
                rotation.addProperty(key, value);
                settings.add("rotation", rotation);
            }
        }
        commit(display, settings);
    }

    public void reset(IBoilerDisplay display) {
        if (kind == Kind.ANGLE) { apply(display, "0"); return; }
        if (this == BUFFER) { apply(display, "true"); return; }
        if (this == ITEM_ROTATION) { apply(display, "0"); return; }
        if (this == DIRECTION) { apply(display, display.facing().name()); return; }
        JsonObject settings = display.settings().deepCopy();
        settings.remove(key);
        commit(display, settings);
    }

    private void commit(IBoilerDisplay display, JsonObject settings) {
        display.settings(settings); // Applies the pipeline options and persists via the existing display setter.
        if (respawn) display.respawn();
        display.tick();
    }

    private static String format(float value) { return value == (int) value ? Integer.toString((int) value) : Float.toString(value); }
}
