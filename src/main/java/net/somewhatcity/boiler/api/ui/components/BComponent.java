/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.api.ui.components;

import net.somewhatcity.boiler.api.ui.UiUtils;
import net.somewhatcity.boiler.util.Assets;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BComponent {

    public int x;
    public int y;
    public int width;
    public int height;
    public String text = "";
    public boolean visible = true;

    public Color textColor = Color.decode("#fffcf5");
    public Color backgroundColor = Color.decode("#111111");
    public Color primaryColor = Color.decode("#4a80ff");
    public Color secondaryColor = Color.decode("#24357d");
    public Color accentColor = Color.decode("#3737dc");

    public Font font = Assets.MINECRAFTIA;

    protected List<BComponent> children = new ArrayList<>();
    protected List<ClickEvent> clickListeners = new ArrayList<>();

    public BComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
    }

    public void text(String text) {
        this.text = text;
    }

    public void paintComponent(Graphics2D g2) {
        if(!visible) return;
        g2.setFont(Assets.MINECRAFTIA);
        g2.setColor(backgroundColor);
        g2.fillRect(0, 0, width, height);
        children.forEach(child -> {
            Graphics2D childG = (Graphics2D) g2.create(child.x, child.y, child.width, child.height);
            child.paintComponent(childG);
            childG.dispose();
        });
    }

    public void add(BComponent component) {
        children.add(component);
    }

    public void addClickListener(ClickEvent event) {
        clickListeners.add(event);
    }

    public void handleClick(int x, int y, Player player) {
        clickListeners.forEach(event -> event.onClick(player));
        children.forEach(child -> {
            if(UiUtils.isInBounds(x, y, child.x, child.y, child.width, child.height)) child.handleClick(x - child.x, y - child.y, player);
        });
    }

}
