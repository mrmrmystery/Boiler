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

import org.bukkit.entity.Player;

import java.awt.*;

public class BCheckBox extends BComponent {

    public boolean checked = false;

    public BCheckBox(int x, int y) {
        super(x, y, 10, 10);
    }

    public BCheckBox(int x, int y, String text) {
        super(x, y, 10, 10);
        this.text = text;

    }

    public BCheckBox(int x, int y, String text, boolean checked) {
        super(x, y, 10, 10);
        this.text = text;
        this.checked = checked;
    }

    @Override
    public void paintComponent(Graphics2D g2) {

        g2.setColor(textColor);
        g2.fillRect(0, 0,10, 10);
        if(checked) {
            g2.setColor(primaryColor);
            g2.fillRect(1, 1, 8, 8);
        }

        if(!text.isEmpty()) {
            FontMetrics metrics = g2.getFontMetrics(font);
            width = metrics.stringWidth(text) + 15;
            g2.setColor(textColor);
            g2.setFont(font);
            g2.drawString(text, 15, 9);
        }
    }

    @Override
    public void handleClick(int x, int y, Player player) {
        checked = !checked;
        super.handleClick(x, y, player);
    }
}
