package net.somewhatcity.boiler.core.smarttv;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;

import static net.somewhatcity.boiler.core.smarttv.SmartTvStyle.*;

/** Pure, adaptive home screen; rendering and input share every interactive rectangle. */
public final class SmartTvMenu implements IBoilerSource {
    public enum Focus { APPS, CATEGORIES, RECENT, SIDEBAR }
    public record View(List<String> sources, int selected, String message, boolean playing,
                       List<String> recent, int category, Focus focus, int focusIndex, SmartTvSettings.View settings) {
        public View { sources = List.copyOf(sources); recent = List.copyOf(recent); }
        public View(List<String> sources, int selected, String message, boolean playing) {
            this(sources, selected, message, playing, List.of(), 0, Focus.APPS, 0, null);
        }
    }
    public record Layout(int columns, int rows, int width, int height) {
        public boolean large() { return width >= 384 && height >= 256 || width >= 256 && height >= 384; }
        public float scale() { return Math.max(.5f, Math.min(width / 768f, height / 512f)); }
        private int px(float value) { return Math.round(value * scale()); }
        public int pageSize() { return columns * rows; }
        public int left() { return px(72); }
        public int contentWidth() { return width - left() - px(16); }
        public Rectangle hero() { return new Rectangle(left(), px(16), contentWidth(), Math.round(height * .35f)); }
        public Rectangle category(int index) {
            int gap = px(10), cell = (contentWidth() - gap * 3) / 4;
            return new Rectangle(left() + index * (cell + gap), hero().y + hero().height + px(14), cell, px(38));
        }
        public Rectangle sidebar(int index) { return new Rectangle(px(12), px(100 + index * 70), px(44), px(48)); }
        public Rectangle tile(int slot) {
            if (large()) {
                int cell = contentWidth() / columns, size = Math.min(px(72), cell - px(12));
                return new Rectangle(left() + slot * cell, category(0).y + category(0).height + px(40), size, size);
            }
            int cellWidth = Math.max(1, (width - 12) / columns);
            int cellHeight = Math.max(1, (height - 54) / rows);
            return new Rectangle(6 + slot % columns * cellWidth, 24 + slot / columns * cellHeight,
                    Math.max(1, cellWidth - 4), Math.max(1, cellHeight - 4));
        }
        public Rectangle recent(int index) {
            Rectangle tile = tile(0);
            int count = recentCount(), gap = px(12);
            int cell = (contentWidth() - (count - 1) * gap) / count;
            int y = tile.y + tile.height + px(64);
            return new Rectangle(left() + index * (cell + gap), y, cell, Math.max(px(36), height - y - px(38)));
        }
        public int recentCount() { return Math.min(3, Math.max(1, contentWidth() / px(190))); }
        public Rectangle pages() { return new Rectangle(width - px(120), tile(0).y - px(34), px(104), px(28)); }
        public int hit(int x, int y, int page, int count) {
            for (int slot = 0; slot < pageSize(); slot++) {
                int index = page * pageSize() + slot;
                Rectangle target = tile(slot);
                if (large()) target.height += px(22);
                if (index < count && target.contains(x, y)) return index;
            }
            return -1;
        }
    }
    private volatile View view = new View(List.of(), 0, "", false);
    private View rendered;
    private BufferedImage frame;
    public void view(View view) { this.view = view; }
    public static Layout layout(int width, int height) {
        boolean large = width >= 384 && height >= 256 || width >= 256 && height >= 384;
        if (large) {
            float scale = Math.max(.5f, Math.min(width / 768f, height / 512f));
            return new Layout(Math.max(3, Math.min(12, (int) ((width - 88 * scale) / (84 * scale)))), 1, width, height);
        }
        return new Layout(Math.max(1, Math.min(4, width / 120)), Math.max(1, Math.min(5, (height - 54) / 28)), width, height);
    }
    @Override public void load(IBoilerDisplay display, JsonObject data) {}
    @Override public void unload() { frame = null; rendered = null; }

    @Override public void draw(Graphics2D graphics, Rectangle viewport) {
        View current = view;
        if (frame == null || frame.getWidth() != viewport.width || frame.getHeight() != viewport.height || !current.equals(rendered)) {
            if (frame == null || frame.getWidth() != viewport.width || frame.getHeight() != viewport.height)
                frame = new BufferedImage(viewport.width, viewport.height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                Layout layout = layout(viewport.width, viewport.height);
                if (layout.large()) home(g, layout, current); else compact(g, layout, current);
                if (current.settings() != null) SmartTvSettings.draw(g, viewport.width, viewport.height, current.settings(), current.message());
            } finally { g.dispose(); }
            rendered = current;
        }
        graphics.drawImage(frame, viewport.x, viewport.y, null);
    }

    private static void compact(Graphics2D g, Layout layout, View current) {
        int width = layout.width(), height = layout.height();
        g.setColor(new Color(13, 19, 31)); g.fillRect(0, 0, width, height);
        font(g, Font.BOLD, 12); g.setColor(new Color(111, 215, 255)); g.drawString("Boiler SmartTV", 6, 16);
        font(g, Font.PLAIN, 9);
        if (current.playing()) { g.setColor(WHITE); text(g, current.message(), 6, 44, width - 12); g.drawString("Sneaken: Quellenmenü", 6, height - 8); return; }
        int page = current.selected() / layout.pageSize();
        for (int slot = 0; slot < layout.pageSize(); slot++) {
            int index = page * layout.pageSize() + slot;
            if (index >= current.sources().size()) break;
            Rectangle tile = layout.tile(slot); boolean selected = index == current.selected();
            g.setColor(selected ? new Color(29, 103, 144) : new Color(31, 42, 61));
            g.fillRoundRect(tile.x, tile.y, tile.width, tile.height, 6, 6);
            if (selected) { g.setColor(new Color(111, 215, 255)); g.drawRoundRect(tile.x, tile.y, tile.width, tile.height, 6, 6); }
            g.setColor(WHITE); text(g, label(current.sources().get(index)), tile.x + 5, tile.y + tile.height / 2 + 3, tile.width - 10);
        }
        g.setColor(MUTED); text(g, current.message().isBlank() ? "WASD · Springen: OK" : current.message(), 6, height - 18, width - 12);
        g.drawString("<", 6, height - 6); g.drawString(page + 1 + "/" + Math.max(1, (current.sources().size() + layout.pageSize() - 1) / layout.pageSize()), width / 2 - 8, height - 6); g.drawString(">", width - 12, height - 6);
    }

    private static void home(Graphics2D g, Layout layout, View current) {
        int width = layout.width(), height = layout.height(); float s = layout.scale();
        g.setPaint(new GradientPaint(0, 0, new Color(12, 12, 18), width, height, new Color(21, 18, 29)));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(6, 7, 11)); g.fillRect(0, 0, Math.round(62 * s), height);
        g.setPaint(new GradientPaint(0, 0, new Color(163, 141, 253), Math.round(48 * s), Math.round(70 * s), new Color(82, 70, 170)));
        g.fillOval(Math.round(18 * s), Math.round(24 * s), Math.round(32 * s), Math.round(32 * s));
        g.setColor(WHITE); font(g, Font.BOLD, 19 * s); g.drawString("B", Math.round(27 * s), Math.round(47 * s));
        for (int i = 0; i < 3; i++) {
            Rectangle target = layout.sidebar(i);
            boolean active = current.focus() == Focus.SIDEBAR && current.focusIndex() == i;
            if (active) { g.setColor(new Color(81, 69, 110)); g.fillRoundRect(target.x, target.y, target.width, target.height, Math.round(10 * s), Math.round(10 * s)); }
            icon(g, i == 0 ? "home" : i == 1 ? "apps" : "settings", target.x + Math.round(9 * s), target.y + Math.round(11 * s), Math.round(26 * s), active ? WHITE : MUTED);
        }
        Rectangle hero = layout.hero();
        Graphics2D art = (Graphics2D) g.create();
        try {
            art.clip(new RoundRectangle2D.Double(hero.x, hero.y, hero.width, hero.height, 14 * s, 14 * s));
            art.setPaint(new GradientPaint(hero.x, hero.y, new Color(35, 25, 50), hero.x + hero.width, hero.y + hero.height, new Color(112, 43, 87))); art.fill(hero);
            float cx = hero.x + hero.width * .83f, cy = hero.y + hero.height * .46f, radius = hero.height * .95f;
            art.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radius, new float[]{0, .35f, .66f, 1}, new Color[]{new Color(255, 179, 147), new Color(217, 92, 141), new Color(111, 59, 141), new Color(39, 27, 54)}));
            art.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
            for (int ring = 0; ring < 6; ring++) {
                float r = radius * (.35f + ring * .13f);
                art.setColor(new Color(255, 208, 190, 65 - ring * 8)); art.setStroke(new BasicStroke(Math.max(1, 2 * s)));
                art.draw(new Ellipse2D.Float(cx - r, cy - r * .66f, r * 2, r * 1.32f));
            }
            art.setPaint(new GradientPaint(hero.x, 0, new Color(18, 17, 28, 250), hero.x + hero.width * .8f, 0, new Color(18, 17, 28, 0))); art.fill(hero);
        } finally { art.dispose(); }
        int x = hero.x + Math.round(24 * s), y = hero.y + Math.round(32 * s);
        g.setColor(new Color(223, 200, 236)); font(g, Font.BOLD, 15 * s); g.drawString("BOILER  /  HOME", x, y);
        g.setColor(WHITE); font(g, Font.BOLD, 36 * s);
        g.drawString("Dein Bildschirm.", x, y + Math.round(46 * s)); g.drawString("Deine Welt.", x, y + Math.round(88 * s));
        font(g, Font.PLAIN, 17 * s); g.setColor(new Color(221, 211, 229));
        text(g, "Streams, Bilder und mehr. Alles an einem Ort.", x, hero.y + hero.height - Math.round(22 * s), Math.round(hero.width * .7f));
        if (current.playing()) {
            g.setColor(new Color(0, 0, 0, 200)); g.fillRect(layout.left(), hero.y, layout.contentWidth(), height - hero.y);
            font(g, Font.BOLD, 24 * s); g.setColor(WHITE); text(g, current.message(), x, hero.y + hero.height / 2, hero.width - Math.round(48 * s));
            font(g, Font.PLAIN, 18 * s); g.drawString("Sneaken: Quellenmenü", x, hero.y + hero.height / 2 + Math.round(40 * s)); return;
        }
        for (int index = 0; index < CATEGORIES.size(); index++) {
            Rectangle chip = layout.category(index); g.setColor(CATEGORY_COLORS[index]);
            g.fillRoundRect(chip.x, chip.y, chip.width, chip.height, Math.round(8 * s), Math.round(8 * s));
            if (current.category() == index || current.focus() == Focus.CATEGORIES && current.focusIndex() == index)
                outline(g, chip, s, current.focus() == Focus.CATEGORIES && current.focusIndex() == index ? WHITE : new Color(255, 255, 255, 100));
            g.setColor(WHITE); font(g, Font.PLAIN, 17 * s);
            text(g, CATEGORIES.get(index), chip.x + Math.round(12 * s), chip.y + chip.height / 2 + Math.round(6 * s), chip.width - Math.round(24 * s));
        }
        Rectangle first = layout.tile(0);
        g.setColor(WHITE); font(g, Font.BOLD, 18 * s); g.drawString("Deine Apps", layout.left(), first.y - Math.round(12 * s));
        int page = current.selected() / layout.pageSize();
        for (int slot = 0; slot < layout.pageSize(); slot++) {
            int index = page * layout.pageSize() + slot;
            if (index >= current.sources().size()) break;
            String source = current.sources().get(index); Rectangle tile = layout.tile(slot);
            boolean active = current.focus() == Focus.APPS && index == current.selected();
            if (active) { g.setColor(new Color(255, 255, 255, 32)); g.fillRoundRect(tile.x - Math.round(6 * s), tile.y - Math.round(6 * s), tile.width + Math.round(12 * s), tile.height + Math.round(12 * s), Math.round(18 * s), Math.round(18 * s)); }
            Color color = color(source); g.setPaint(new GradientPaint(tile.x, tile.y, color.brighter(), tile.x + tile.width, tile.y + tile.height, color.darker()));
            g.fillRoundRect(tile.x, tile.y, tile.width, tile.height, Math.round(12 * s), Math.round(12 * s));
            icon(g, icon(source), tile.x + tile.width / 4, tile.y + tile.height / 4, tile.width / 2, WHITE);
            if (active) outline(g, tile, s, WHITE);
            g.setColor(active ? WHITE : MUTED); font(g, active ? Font.BOLD : Font.PLAIN, 14 * s);
            text(g, label(source), tile.x, tile.y + tile.height + Math.round(20 * s), layout.contentWidth() / layout.columns() - Math.round(8 * s));
        }
        int pages = Math.max(1, (current.sources().size() + layout.pageSize() - 1) / layout.pageSize());
        g.setColor(MUTED); font(g, Font.PLAIN, 14 * s);
        String pageLabel = "‹  " + (page + 1) + " / " + pages + "  ›";
        g.drawString(pageLabel, width - Math.round(16 * s) - g.getFontMetrics().stringWidth(pageLabel), first.y - Math.round(12 * s));
        Rectangle firstRecent = layout.recent(0);
        g.setColor(WHITE); font(g, Font.BOLD, 18 * s); g.drawString(current.recent().isEmpty() ? "Quellen entdecken" : "Zuletzt verwendet", layout.left(), firstRecent.y - Math.round(12 * s));
        List<String> recent = current.recent().isEmpty() ? current.sources().stream().filter(source -> !source.equals(SETTINGS)).limit(layout.recentCount()).toList() : current.recent();
        for (int index = 0; index < Math.min(recent.size(), layout.recentCount()); index++) {
            String source = recent.get(index); Rectangle card = layout.recent(index); Color color = color(source);
            g.setPaint(new GradientPaint(card.x, card.y, color.darker().darker(), card.x + card.width, card.y + card.height, color));
            g.fillRoundRect(card.x, card.y, card.width, card.height, Math.round(10 * s), Math.round(10 * s));
            int iconSize = Math.min(card.height - Math.round(14 * s), Math.round(44 * s));
            icon(g, icon(source), card.x + card.width - iconSize - Math.round(12 * s), card.y + (card.height - iconSize) / 2, iconSize, new Color(255, 255, 255, 150));
            g.setColor(WHITE); font(g, Font.BOLD, 20 * s);
            text(g, label(source), card.x + Math.round(14 * s), card.y + card.height / 2 + Math.round(5 * s), card.width - iconSize - Math.round(35 * s));
            if (current.focus() == Focus.RECENT && current.focusIndex() == index) outline(g, card, s, WHITE);
        }
        g.setColor(MUTED); font(g, Font.PLAIN, 14 * s);
        text(g, current.message().isBlank() ? "WASD: Navigation   Springen: Öffnen   Sneaken: Zurück" : current.message(), layout.left(), height - Math.round(10 * s), layout.contentWidth());
    }

    private static void outline(Graphics2D g, Rectangle rectangle, float scale, Color color) {
        g.setColor(color); g.setStroke(new BasicStroke(Math.max(1, 2 * scale)));
        g.drawRoundRect(rectangle.x - 1, rectangle.y - 1, rectangle.width + 2, rectangle.height + 2, Math.round(12 * scale), Math.round(12 * scale));
    }
}
