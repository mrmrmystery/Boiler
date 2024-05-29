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
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.UUID;

public class SwingTest implements IBoilerSource {

    private JPanel panel = new JPanel();
    private JFileChooser fileChooser;
    private Player player;

    private Point lastClick;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        if(data.has("gui_player")) {
            player = Bukkit.getPlayer(UUID.fromString(data.get("gui_player").getAsString()));
        }

        Dimension dimension = new Dimension(display.width(), display.height());

        panel.setSize(dimension);
        panel.setMinimumSize(dimension);
        panel.setMaximumSize(dimension);
        panel.setPreferredSize(dimension);
        panel.setLayout(new FlowLayout());

        JButton button = new JButton("click me");
        button.addActionListener(e -> {
            player.sendMessage("click");
        });
        panel.add(button);

        JCheckBox checkbox = new JCheckBox();
        panel.add(checkbox);

        JTextField textField = new JTextField("kek");
        panel.add(textField);

        //JScrollPane scrollPane = new JScrollPane(list);
        //panel.add(scrollPane);

        JRadioButton radio0 = new JRadioButton();
        JRadioButton radio1 = new JRadioButton();
        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radio0);
        radioGroup.add(radio1);
        panel.add(radio0);
        panel.add(radio1);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setValue(50);
        panel.add(progressBar);

        JColorChooser colorChooser = new JColorChooser();
        panel.add(colorChooser);


        panel.doLayout();
        panel.validate();
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        panel.paint(g2);
    }

    @Override
    public void unload() {

    }

    @Override
    public void onClick(CommandSender sender, int x, int y, boolean right) {
        Component clicked = panel.getComponentAt(x, y);
        lastClick = new Point(x, y);

        if(clicked instanceof JButton button) {
            button.doClick(50);
        } else if(clicked instanceof JCheckBox checkBox) {
            checkBox.doClick(50);
        } else if(clicked instanceof JRadioButton radioButton) {
            radioButton.doClick(50);
        }
    }

    @Override
    public void onInput(CommandSender sender, String input) {
        Component selected = panel.getComponentAt(lastClick);

        if(selected instanceof JTextField textField) {
            textField.setText(input);
        }
    }
}
