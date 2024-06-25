/*
 * Copyright (c) 2024.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.command.CommandSender;

import javax.swing.*;
import java.awt.*;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "link", type = CommandArgumentType.INTEGER)
})
public class KeyboardSource implements IBoilerSource {

    private JPanel panel = new JPanel();
    private int linkedId;
    private CommandSender lastInteractor;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        linkedId = data.get("link").getAsInt();

        Dimension dimension = new Dimension(display.width(), display.height());

        panel.setSize(dimension);
        panel.setMinimumSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setPreferredSize(dimension);
        panel.setLayout(null);

        //row 0
        createKey(1, 1, 15, 15, "ESC", "Escape");

        //row 1
        createKey(1, 17, 15, 15, "A", "A");
        
    }

    @Override
    public void unload() {

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        panel.paint(g2);
    }
    public void createKey(int x, int y, int width, int height, String label, String key) {
        KeyboardKey button = new KeyboardKey(label);
        button.addActionListener(e -> {
            sendKeystroke(key);
        });
        button.setBounds(x, y, width, height);
        panel.add(button);
    }
    public void sendKeystroke(String key) {
        IBoilerDisplay display = BoilerPlugin.getPlugin().displayManager().display(linkedId);
        if(display == null || lastInteractor == null) return;
        display.onKey(lastInteractor, key);
    }

    @Override
    public void onClick(CommandSender sender, int x, int y, boolean right) {
        lastInteractor = sender;
        Component clicked = panel.getComponentAt(x, y);

        if(clicked instanceof JButton button) {
            button.doClick(50);
        } else if(clicked instanceof JCheckBox checkBox) {
            checkBox.doClick(50);
        } else if(clicked instanceof JRadioButton radioButton) {
            radioButton.doClick(50);
        }
    }

    private static class KeyboardKey extends JButton {
        public KeyboardKey(String label) {
            super(label);
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;

            Rectangle rect = new Rectangle(0, 0, getWidth(), getHeight());

            g2.setColor(Color.BLACK);
            g2.fill(rect);

            g2.setColor(Color.WHITE);
            drawCenteredString(g2, getText(), rect, new Font("Arial", Font.PLAIN, 8));
        }
    }

    public static void drawCenteredString(Graphics g, String text, Rectangle rect, Font font) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.setFont(font);
        g.drawString(text, x, y);
    }
}
