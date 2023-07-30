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

import java.awt.*;
import java.awt.image.BufferedImage;

public class BImage extends BComponent {

    public BufferedImage image;

    public BImage(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public BImage(int x, int y, int width, int height, BufferedImage image) {
        super(x, y, width, height);
        this.image = image;
    }

    public BImage(int x, int y, BufferedImage image) {
        super(x, y, image.getWidth(), image.getHeight());
        this.image = image;
    }

    @Override
    public void paintComponent(Graphics2D g2) {
        if(image == null) return;
        g2.drawImage(image, 0, 0, width, height, null);
    }
}
