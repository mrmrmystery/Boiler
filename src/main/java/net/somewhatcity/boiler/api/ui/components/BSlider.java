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

public class BSlider extends BComponent {

    public int min = 0;
    public int max = 100;
    public int val = 0;

    public BSlider(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void paintComponent(Graphics2D g2) {
        g2.setColor(textColor);
        g2.fillRect(0, 0, width, height);
        g2.setColor(primaryColor);
        g2.fillRect(0, 0, (int) ((double) val / (double) max * width), height);
    }

    @Override
    public void handleClick(int x, int y, Player player) {
        val = (int) ((double) x / (double) width * max);
    }
}
