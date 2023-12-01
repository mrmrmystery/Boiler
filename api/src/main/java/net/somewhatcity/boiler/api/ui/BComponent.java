/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.api.ui;

import lombok.Getter;
import lombok.Setter;
import net.somewhatcity.boiler.api.ui.events.UiClickEvent;
import net.somewhatcity.boiler.api.ui.events.UiClickListener;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class BComponent {

    private Color primaryColor = Color.BLUE;
    private Color secondaryColor = Color.CYAN;
    private Color textColor = Color.WHITE;

    private int x;
    private int y;
    private int width;
    private int height;
    private String text = "";
    private boolean visible = true;

    private List<BComponent> children = new ArrayList<>();

    private List<UiClickListener> listeners = new ArrayList<>();
    public BComponent(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public void draw(Graphics2D g) {
        if(!visible) return;

        children.forEach(child -> {
            Graphics2D clip = (Graphics2D) g.create(child.x, child.y, child.width, child.height);
            child.draw(clip);
            clip.dispose();
            //System.out.println("drawing " + this.getClass().getName());
        });
    }

    public void add(BComponent component) {
        children.add(component);
    }
    public List<BComponent> componentAt(int x, int y) {
        List<BComponent> found = new ArrayList<>();
        for(BComponent child : children) {
            int newX = x - child.x;
            int newY = y - child.y;
            if(newX > 0 && newY > 0 && newX < child.width && newY < child.height) {
                found.add(child);
                found.addAll(child.componentAt(newX, newY));
            }
        }
        return found;
    }

    public void addClickListener(UiClickListener listener) {
        listeners.add(listener);
    }

    public void onClick(Player player, boolean right) {
        listeners.forEach(listener -> {
            listener.onClick(new UiClickEvent(player, right));
        });
    }
}
