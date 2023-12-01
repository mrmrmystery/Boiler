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

import net.somewhatcity.boiler.api.ui.BComponent;
import net.somewhatcity.boiler.api.util.GraphicUtils;

import java.awt.*;

public class BButton extends BComponent {

    public BButton(int x, int y, int width, int height) {
        super(x, y, width, height);
    }
    @Override
    public void draw(Graphics2D g) {
        super.draw(g);
        g.setColor(secondaryColor());
        g.fillRect(0, 0, width(), height());
        g.setColor(textColor());
        GraphicUtils.centeredString(g, new Rectangle(0, 0, width(), height()), text());

    }
}
