package net.somewhatcity.boiler.core.smarttv;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

/** Vector artwork remains sharp at map resolution and requires no external assets or fonts. */
public final class SmartTvStyle {
    public static final String SETTINGS = "@settings";
    public static final Color WHITE = new Color(248, 249, 255), MUTED = new Color(156, 165, 186);
    public static final List<String> CATEGORIES = List.of("Alle Apps", "Video & Bild", "Live-TV", "Werkzeuge");
    public static final Color[] CATEGORY_COLORS = {new Color(43, 139, 141), new Color(105, 81, 183), new Color(168, 66, 148), new Color(57, 129, 173)};
    private SmartTvStyle() {}

    public static int category(String source) {
        return switch (source) {
            case "youtube", "image", "ffmpeg", "ffmpeg-buffered", "vlc", "gstreamer", "converter" -> 1;
            case "twitch", "rtmp-server", "client", "client-stream" -> 2;
            default -> 3;
        };
    }
    public static String label(String source) {
        return switch (source) {
            case SETTINGS -> "Einstellungen";
            case "youtube" -> "YouTube";
            case "twitch" -> "Twitch";
            case "image" -> "Bilder";
            case "sysinfo" -> "Systeminfo";
            case "rtmp-server" -> "Live-Stream";
            case "client-stream" -> "Client Live";
            case "client" -> "Boiler Client";
            case "ffmpeg-buffered" -> "FFmpeg Puffer";
            case "section-clone" -> "Ausschnitt";
            case "clone" -> "Display Clone";
            case "keyboard" -> "Tastatur";
            case "vlc" -> "VLC";
            case "ffmpeg" -> "FFmpeg";
            case "gstreamer" -> "GStreamer";
            default -> source;
        };
    }
    public static Color color(String source) {
        return switch (source) {
            case "youtube" -> new Color(223, 36, 57);
            case "twitch" -> new Color(130, 77, 224);
            case "image" -> new Color(29, 163, 151);
            case SETTINGS -> new Color(80, 103, 143);
            case "vlc" -> new Color(226, 128, 42);
            case "keyboard" -> new Color(66, 111, 166);
            default -> CATEGORY_COLORS[category(source)];
        };
    }
    public static String icon(String source) {
        if (source.equals(SETTINGS)) return "settings";
        return switch (source) {
            case "image" -> "picture";
            case "keyboard" -> "keyboard";
            case "clone", "section-clone", "client", "client-stream" -> "screen";
            case "sysinfo", "swing", "converter" -> "tools";
            case "rtmp-server", "twitch" -> "live";
            default -> "play";
        };
    }
    public static void font(Graphics2D g, int style, float size) { g.setFont(new Font(Font.SANS_SERIF, style, Math.max(8, Math.round(size)))); }
    public static void text(Graphics2D g, String value, int x, int y, int width) {
        if (width <= 0) return;
        String result = value;
        if (g.getFontMetrics().stringWidth(result) > width) {
            while (!result.isEmpty() && g.getFontMetrics().stringWidth(result + "…") > width) result = result.substring(0, result.length() - 1);
            result += "…";
        }
        g.drawString(result, x, y);
    }
    public static void lines(Graphics2D g, String value, int x, int y, int width, int maxLines, int spacing) {
        String line = "";
        int count = 0;
        for (String word : value.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && g.getFontMetrics().stringWidth(next) > width) {
                text(g, line, x, y + count++ * spacing, width);
                line = word;
                if (count >= maxLines) return;
            } else line = next;
        }
        if (count < maxLines) text(g, line, x, y + count * spacing, width);
    }

    public static void icon(Graphics2D graphics, String name, int x, int y, int size, Color color) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.translate(x, y); g.scale(size / 24.0, size / 24.0);
            g.setColor(color); g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (name) {
                case "settings", "tools" -> {
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        g.draw(new Line2D.Double(12 + Math.cos(angle) * 8, 12 + Math.sin(angle) * 8, 12 + Math.cos(angle) * 10, 12 + Math.sin(angle) * 10));
                    }
                    g.draw(new Ellipse2D.Double(5, 5, 14, 14)); g.draw(new Ellipse2D.Double(9, 9, 6, 6));
                }
                case "home" -> {
                    Path2D path = new Path2D.Double(); path.moveTo(3, 11); path.lineTo(12, 3); path.lineTo(21, 11);
                    path.moveTo(6, 10); path.lineTo(6, 21); path.lineTo(18, 21); path.lineTo(18, 10);
                    g.draw(path); g.draw(new Rectangle2D.Double(10, 14, 4, 7));
                }
                case "apps" -> {
                    for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++)
                        g.fill(new RoundRectangle2D.Double(3 + col * 11, 3 + row * 11, 7, 7, 2, 2));
                }
                case "picture" -> {
                    g.draw(new RoundRectangle2D.Double(2, 3, 20, 18, 3, 3)); g.fill(new Ellipse2D.Double(6, 6, 4, 4));
                    Path2D path = new Path2D.Double(); path.moveTo(3, 18); path.lineTo(9, 12); path.lineTo(13, 16); path.lineTo(17, 10); path.lineTo(22, 16); g.draw(path);
                }
                case "audio" -> {
                    Path2D path = new Path2D.Double(); path.moveTo(3, 9); path.lineTo(7, 9); path.lineTo(13, 4); path.lineTo(13, 20); path.lineTo(7, 15); path.lineTo(3, 15); path.closePath(); g.draw(path);
                    g.draw(new Arc2D.Double(11, 6, 9, 12, -65, 130, Arc2D.OPEN)); g.draw(new Arc2D.Double(11, 2, 14, 20, -65, 130, Arc2D.OPEN));
                }
                case "live" -> {
                    g.fill(new Ellipse2D.Double(10, 10, 4, 4));
                    g.draw(new Arc2D.Double(5, 5, 14, 14, -60, 120, Arc2D.OPEN)); g.draw(new Arc2D.Double(5, 5, 14, 14, 120, 120, Arc2D.OPEN));
                    g.draw(new Arc2D.Double(1, 1, 22, 22, -60, 120, Arc2D.OPEN)); g.draw(new Arc2D.Double(1, 1, 22, 22, 120, 120, Arc2D.OPEN));
                }
                case "screen" -> {
                    g.draw(new RoundRectangle2D.Double(2, 3, 20, 14, 3, 3)); g.draw(new Line2D.Double(12, 17, 12, 21)); g.draw(new Line2D.Double(7, 21, 17, 21));
                }
                case "keyboard" -> {
                    g.draw(new RoundRectangle2D.Double(1, 5, 22, 15, 3, 3));
                    for (int row = 0; row < 2; row++) for (int col = 0; col < 5; col++) g.fill(new Rectangle2D.Double(4 + col * 3.4, 8 + row * 4, 1.6, 1.6));
                    g.draw(new Line2D.Double(8, 17, 16, 17));
                }
                case "back" -> {
                    g.draw(new Line2D.Double(4, 12, 21, 12)); g.draw(new Line2D.Double(4, 12, 11, 5)); g.draw(new Line2D.Double(4, 12, 11, 19));
                }
                default -> {
                    Path2D play = new Path2D.Double(); play.moveTo(8, 4); play.lineTo(21, 12); play.lineTo(8, 20); play.closePath(); g.fill(play);
                }
            }
        } finally { g.dispose(); }
    }
}
