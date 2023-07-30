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

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class BButton extends BComponent {

    public int cornerRadius = 0;

    public BButton(int x, int y, String text) {
        super(x, y, 60, 15);
        this.text = text;
    }

    public BButton(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    public BButton(int x, int y, int width, int height, String text, ClickEvent clickListener) {
        super(x, y, width, height);
        this.text = text;
        this.clickListeners.add(clickListener);
    }

    @Override
    public void paintComponent(Graphics2D g2) {
        //super.paintComponent(g2);
        g2.setColor(primaryColor);
        RoundRectangle2D rect = new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius);
        g2.fill(rect);
        g2.draw(rect);
        g2.setColor(textColor);
        UiUtils.drawCenteredString(g2, text, new Rectangle(0, 0, width, height), font);
    }
}
