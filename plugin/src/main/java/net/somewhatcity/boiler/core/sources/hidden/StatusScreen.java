package net.somewhatcity.boiler.core.sources.hidden;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Shared, resolution-aware artwork for the idle and error sources. */
final class StatusScreen {
    private static final Color BACKGROUND = new Color(12, 19, 30);
    private static final Color PANEL = new Color(22, 32, 46);
    private static final Color BORDER = new Color(43, 57, 74);
    private static final Color TEXT = new Color(238, 244, 250);
    private static final Color MUTED = new Color(157, 175, 195);
    private static final Color READY = new Color(85, 218, 194);
    private static final Color ERROR = new Color(255, 139, 119);

    private BufferedImage image;
    private int cachedId;
    private String cachedError;

    synchronized void clear() {
        image = null;
    }

    synchronized void draw(Graphics2D graphics, Rectangle viewport, int displayId, String error) {
        if (viewport.width <= 0 || viewport.height <= 0) return;
        // These screens are static. Rebuild only when their content or resolution changes.
        if (image == null || image.getWidth() != viewport.width || image.getHeight() != viewport.height
                || cachedId != displayId || !Objects.equals(cachedError, error)) {
            image = render(viewport.width, viewport.height, displayId, error);
            cachedId = displayId;
            cachedError = error;
        }
        graphics.drawImage(image, viewport.x, viewport.y, null);
    }

    private static BufferedImage render(int width, int height, int displayId, String error) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);
            boolean failed = error != null;
            Color accent = failed ? ERROR : READY;
            double scale = Math.max(0.6, Math.min(2.0, Math.min(width / 480.0, height / 320.0)));
            int margin = Math.max(4, (int) Math.min(24 * scale, Math.min(width, height) / 16.0));
            int contentWidth = Math.min(width - margin * 2, (int) (720 * scale));
            int left = (width - contentWidth) / 2;
            if (contentWidth < 24 || height < 60) {
                g.setColor(accent);
                g.fillRect(0, 0, width, Math.min(3, height));
                return result;
            }

            int headerHeight = (int) (34 * scale);
            int footerHeight = (int) (26 * scale);
            font(g, Font.BOLD, Math.max(9, (int) (13 * scale)));
            g.setColor(TEXT);
            g.drawString("BOILER", left, margin + g.getFontMetrics().getAscent());
            String status = failed ? "ERROR" : "READY";
            int badgeWidth = g.getFontMetrics().stringWidth(status) + (int) (22 * scale);
            int badgeHeight = g.getFontMetrics().getHeight() + 6;
            if (contentWidth > g.getFontMetrics().stringWidth("BOILER") + badgeWidth + 12) {
                g.setColor(failed ? new Color(65, 39, 42) : new Color(25, 59, 58));
                g.fillRoundRect(left + contentWidth - badgeWidth, margin - 3, badgeWidth, badgeHeight, 10, 10);
                g.setColor(accent);
                g.drawString(status, left + contentWidth - badgeWidth + (int) (11 * scale),
                        margin + g.getFontMetrics().getAscent());
            }

            int top = margin + headerHeight;
            int cardHeight = height - top - margin - footerHeight;
            if (cardHeight <= 0) return result;
            int radius = (int) (20 * scale);
            g.setColor(PANEL);
            g.fillRoundRect(left, top, contentWidth, cardHeight, radius, radius);
            g.setColor(BORDER);
            g.drawRoundRect(left, top, contentWidth - 1, cardHeight - 1, radius, radius);
            g.setColor(accent);
            int railWidth = Math.min(contentWidth / 3, (int) (64 * scale));
            g.fillRoundRect((width - railWidth) / 2, top, railWidth, Math.max(2, (int) (3 * scale)), 3, 3);

            int padding = Math.max(6, (int) Math.min(26 * scale, contentWidth / 14.0));
            int textWidth = contentWidth - padding * 2;
            int bodySize = Math.max(9, (int) (14 * scale));
            font(g, Font.PLAIN, bodySize);
            int lineHeight = g.getFontMetrics().getHeight() + Math.max(1, (int) (3 * scale));
            int titleSize = Math.max(12, (int) (28 * scale));
            String title = failed ? "Source unavailable" : "Ready to display";
            font(g, Font.BOLD, titleSize);
            while (g.getFontMetrics().stringWidth(title) > textWidth && titleSize > 9) {
                font(g, Font.BOLD, --titleSize);
            }
            int titleHeight = g.getFontMetrics().getHeight();
            int gap = Math.max(5, (int) (12 * scale));
            int iconSize = cardHeight >= (int) (210 * scale) ? (int) (56 * scale) : 0;
            int iconSpace = iconSize == 0 ? 0 : iconSize + gap;
            int available = cardHeight - padding * 2 - iconSpace - titleHeight - gap;
            int maxLines = Math.max(0, available / lineHeight);
            font(g, Font.PLAIN, bodySize);
            List<String> lines = maxLines == 0 ? List.of()
                    : wrap(g, failed ? error : "Choose a source to get started.", textWidth, maxLines);
            int blockHeight = iconSpace + titleHeight + (lines.isEmpty() ? 0 : gap + lines.size() * lineHeight);
            int y = top + Math.max(padding / 2, (cardHeight - blockHeight) / 2);

            // Clip the content as a final safeguard for unusually small viewports.
            Shape clip = g.getClip();
            g.clipRect(left + padding, top + 4, Math.max(0, textWidth), Math.max(0, cardHeight - 8));
            if (iconSize > 0) {
                drawIcon(g, width / 2 - iconSize / 2, y, iconSize, accent, failed);
                y += iconSpace;
            }
            font(g, Font.BOLD, titleSize);
            g.setColor(TEXT);
            centered(g, ellipsize(g, title, textWidth), width / 2, y + g.getFontMetrics().getAscent());
            y += titleHeight + gap;
            font(g, Font.PLAIN, bodySize);
            g.setColor(failed ? new Color(240, 187, 179) : MUTED);
            for (String line : lines) {
                centered(g, line, width / 2, y + g.getFontMetrics().getAscent());
                y += lineHeight;
            }
            g.setClip(clip);

            font(g, Font.PLAIN, Math.max(8, (int) (11 * scale)));
            g.setColor(MUTED);
            String identity = "DISPLAY #" + displayId;
            int baseline = height - margin - g.getFontMetrics().getDescent();
            g.drawString(ellipsize(g, identity, contentWidth), left, baseline);
            String hint = failed ? "Check the source and try again" : "Waiting for a source";
            if (contentWidth > g.getFontMetrics().stringWidth(identity + "    " + hint)) {
                g.drawString(hint, left + contentWidth - g.getFontMetrics().stringWidth(hint), baseline);
            }
        } finally {
            g.dispose();
        }
        return result;
    }

    private static void drawIcon(Graphics2D g, int x, int y, int size, Color accent, boolean failed) {
        g.setColor(failed ? new Color(65, 39, 42) : new Color(25, 59, 58));
        g.fillRoundRect(x, y, size, size, size / 3, size / 3);
        g.setColor(accent);
        g.setStroke(new BasicStroke(Math.max(2, size / 22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (failed) {
            g.drawLine(x + size / 2, y + size / 4, x + size / 2, y + size / 2);
            g.fillOval(x + size / 2 - 2, y + size * 2 / 3, 4, 4);
        } else {
            g.drawRoundRect(x + size / 4, y + size / 4, size / 2, size * 3 / 8, 3, 3);
            g.drawLine(x + size / 2, y + size * 5 / 8, x + size / 2, y + size * 3 / 4);
            g.drawLine(x + size * 3 / 8, y + size * 3 / 4, x + size * 5 / 8, y + size * 3 / 4);
        }
    }

    private static void font(Graphics2D g, int style, int size) {
        g.setFont(new Font(Font.SANS_SERIF, style, size));
    }

    private static void centered(Graphics2D g, String text, int centerX, int baseline) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static List<String> wrap(Graphics2D g, String text, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        String remaining = text.replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ').strip();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            int newline = remaining.indexOf('\n');
            int end = fittingEnd(g, remaining, width);
            if (newline >= 0 && newline <= end) {
                lines.add(remaining.substring(0, newline).stripTrailing());
                remaining = remaining.substring(newline + 1);
                continue;
            }
            if (end == remaining.length()) {
                lines.add(remaining);
                remaining = "";
            } else {
                if (end == 0) break;
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) end = space;
                lines.add(remaining.substring(0, end).stripTrailing());
                remaining = remaining.substring(end).stripLeading();
            }
        }
        if (!remaining.isEmpty()) {
            if (lines.isEmpty()) lines.add("");
            int last = lines.size() - 1;
            lines.set(last, ellipsize(g, lines.get(last) + "...", width));
        }
        return lines;
    }

    private static String ellipsize(Graphics2D g, String text, int width) {
        if (g.getFontMetrics().stringWidth(text) <= width) return text;
        String suffix = "...";
        if (g.getFontMetrics().stringWidth(suffix) > width) return "";
        return text.substring(0, fittingEnd(g, text, width - g.getFontMetrics().stringWidth(suffix))).stripTrailing() + suffix;
    }

    private static int fittingEnd(Graphics2D g, String text, int width) {
        int end = 0;
        while (end < text.length()) {
            int next = text.offsetByCodePoints(end, 1);
            if (g.getFontMetrics().stringWidth(text.substring(0, next)) > width) break;
            end = next;
        }
        return end;
    }
}
