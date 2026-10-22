package net.somewhatcity.boiler.core.smarttv;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

import static net.somewhatcity.boiler.core.smarttv.SmartTvStyle.*;

/** Settings panels and their pointer targets use the same adaptive geometry. */
public final class SmartTvSettings {
    public record View(List<DisplaySetting.Value> values, int selected) {
        public View { values = List.copyOf(values); }
    }
    public record Layout(int width, int height, int selected) {
        public float scale() { return Math.max(.45f, Math.min(width / 768f, height / 512f)); }
        private int px(float value) { return Math.round(value * scale()); }
        public Rectangle rail() { return new Rectangle(px(12), px(16), px(66), height - px(32)); }
        public Rectangle panel() {
            int x = rail().x + rail().width + px(10);
            return new Rectangle(x, px(16), width >= 360 ? Math.round(width * .52f) : width - x - px(12), height - px(32));
        }
        public Rectangle help() {
            Rectangle panel = panel();
            int x = panel.x + panel.width + px(10);
            return new Rectangle(x, panel.y, Math.max(0, width - x - px(12)), panel.height);
        }
        public Rectangle back() { return new Rectangle(rail().x, rail().y, rail().width, px(54)); }
        public Rectangle category(int index) { return new Rectangle(rail().x, rail().y + px(68 + index * 68), rail().width, px(54)); }
        public List<DisplaySetting> options() {
            DisplaySetting.Group group = DisplaySetting.values()[selected].group;
            return Arrays.stream(DisplaySetting.values()).filter(option -> option.group == group).toList();
        }
        public int rowHeight() { return Math.max(20, px(48)); }
        public int rows() { return Math.max(1, (panel().height - px(100)) / rowHeight()); }
        private int start() { return options().indexOf(DisplaySetting.values()[selected]) / rows() * rows(); }
        public Rectangle row(int optionIndex) {
            if (DisplaySetting.values()[optionIndex].group != DisplaySetting.values()[selected].group) return new Rectangle();
            int local = options().indexOf(DisplaySetting.values()[optionIndex]) - start();
            if (local < 0 || local >= rows()) return new Rectangle();
            Rectangle panel = panel();
            return new Rectangle(panel.x + px(10), panel.y + px(70) + local * rowHeight(), panel.width - px(20), rowHeight() - px(3));
        }
        public int hit(int x, int y) {
            for (DisplaySetting option : options()) if (row(option.ordinal()).contains(x, y)) return option.ordinal();
            return -1;
        }
        public int adjustment(int option, int x) {
            Rectangle row = row(option);
            if (x >= row.x + row.width - px(28)) return 1;
            if (x >= row.x + row.width - px(116) && x < row.x + row.width - px(88)) return -1;
            return 0;
        }
    }
    private SmartTvSettings() {}

    public static void draw(Graphics2D graphics, int width, int height, View view, String message) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setColor(new Color(0, 0, 0, 155)); g.fillRect(0, 0, width, height);
            Layout layout = new Layout(width, height, view.selected());
            float s = layout.scale();
            Rectangle rail = layout.rail(), panel = layout.panel(), help = layout.help();
            g.setColor(new Color(83, 116, 163, 245)); g.fillRoundRect(rail.x, rail.y, rail.width, rail.height, Math.round(14 * s), Math.round(14 * s));
            g.setColor(new Color(72, 103, 150, 248)); g.fillRoundRect(panel.x, panel.y, panel.width, panel.height, Math.round(14 * s), Math.round(14 * s));
            DisplaySetting selected = DisplaySetting.values()[view.selected()];
            for (DisplaySetting.Group group : DisplaySetting.Group.values()) {
                Rectangle target = layout.category(group.ordinal());
                if (group == selected.group) { g.setColor(new Color(36, 63, 107)); g.fillRoundRect(target.x + 3, target.y, target.width - 6, target.height, 5, 5); }
                int size = Math.round(32 * s);
                icon(g, group.icon, target.x + (target.width - size) / 2, target.y + (target.height - size) / 2, size, WHITE);
            }
            Rectangle back = layout.back();
            int backSize = Math.round(26 * s);
            icon(g, "back", back.x + (back.width - backSize) / 2, back.y + (back.height - backSize) / 2, backSize, WHITE);
            g.setColor(new Color(204, 221, 244)); font(g, Font.PLAIN, 18 * s);
            text(g, "Einstellungen", panel.x + Math.round(16 * s), panel.y + Math.round(25 * s), panel.width - Math.round(32 * s));
            font(g, Font.PLAIN, 28 * s); g.setColor(WHITE);
            text(g, selected.group.label, panel.x + Math.round(16 * s), panel.y + Math.round(56 * s), panel.width - Math.round(32 * s));
            for (DisplaySetting.Value value : view.values()) {
                Rectangle row = layout.row(value.option().ordinal());
                if (row.isEmpty()) continue;
                boolean active = value.option() == selected;
                if (active) { g.setColor(new Color(225, 245, 255)); g.fillRoundRect(row.x, row.y, row.width, row.height, Math.round(7 * s), Math.round(7 * s)); }
                g.setColor(active ? new Color(37, 67, 100) : WHITE);
                font(g, active ? Font.BOLD : Font.PLAIN, 20 * s);
                int valueWidth = Math.round((value.option().kind == DisplaySetting.Kind.TOGGLE ? 66 : 112) * s);
                text(g, value.option().label, row.x + Math.round(12 * s), row.y + row.height / 2 + Math.round(7 * s), row.width - valueWidth - Math.round(20 * s));
                if (value.option().kind == DisplaySetting.Kind.TOGGLE) {
                    int toggleWidth = Math.round(48 * s), toggleHeight = Math.round(26 * s);
                    int x = row.x + row.width - toggleWidth - Math.round(10 * s), y = row.y + (row.height - toggleHeight) / 2;
                    g.setColor(value.enabled() ? new Color(30, 226, 171) : new Color(122, 148, 178));
                    g.fillRoundRect(x, y, toggleWidth, toggleHeight, toggleHeight, toggleHeight);
                    g.setColor(WHITE);
                    int knob = toggleHeight - 4;
                    g.fillOval(x + (value.enabled() ? toggleWidth - knob - 2 : 2), y + 2, knob, knob);
                } else {
                    font(g, Font.PLAIN, 18 * s);
                    int baseline = row.y + row.height / 2 + Math.round(6 * s);
                    g.drawString("‹", row.x + row.width - Math.round(108 * s), baseline);
                    g.drawString("›", row.x + row.width - Math.round(20 * s), baseline);
                    int valueX = row.x + row.width - Math.round(86 * s);
                    text(g, value.text(), valueX, baseline, Math.round(58 * s));
                }
            }
            if (width >= 360) {
                g.setColor(new Color(42, 65, 104, 248)); g.fillRoundRect(help.x, help.y, help.width, help.height, Math.round(14 * s), Math.round(14 * s));
                g.setColor(WHITE); font(g, Font.BOLD, 22 * s);
                lines(g, selected.label, help.x + Math.round(18 * s), help.y + Math.round(36 * s), help.width - Math.round(36 * s), 2, Math.round(26 * s));
                font(g, Font.PLAIN, 18 * s);
                lines(g, selected.description, help.x + Math.round(18 * s), help.y + Math.round(100 * s), help.width - Math.round(36 * s), Math.max(2, (help.height - Math.round(190 * s)) / Math.round(26 * s)), Math.round(26 * s));
                g.setColor(new Color(174, 199, 227)); font(g, Font.PLAIN, 16 * s);
                lines(g, "Rechtsklick setzt die Option auf den Standard zurück.", help.x + Math.round(18 * s), help.y + help.height - Math.round(72 * s), help.width - Math.round(36 * s), 3, Math.round(22 * s));
            }
            g.setColor(new Color(217, 233, 253)); font(g, Font.PLAIN, 16 * s);
            text(g, message.isBlank() ? "W/S: Auswahl  A/D: Ändern" : message, panel.x + Math.round(14 * s), panel.y + panel.height - Math.round(30 * s), panel.width - Math.round(28 * s));
            text(g, "Springen: OK · Sneaken: Zurück", panel.x + Math.round(14 * s), panel.y + panel.height - Math.round(10 * s), panel.width - Math.round(28 * s));
        } finally { g.dispose(); }
    }
}
