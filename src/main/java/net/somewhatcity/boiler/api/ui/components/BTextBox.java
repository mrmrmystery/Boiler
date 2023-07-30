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

public class BTextBox extends BComponent {
    public BTextBox(int x, int y, int width, int height, String text) {
        super(x, y, width, height);
        this.text = text;
    }

    @Override
    public void paintComponent(Graphics2D g2) {
        int lineHeight = g2.getFontMetrics().getHeight();
        int ty = lineHeight;

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        g2.setColor(textColor);

        for (String word : words) {
            if (g2.getFontMetrics().stringWidth(currentLine + " " + word) <= width) {
                if(word.equals(words[0]))
                    currentLine.append(word);
                else
                    currentLine.append(" ").append(word);
            } else {
                g2.drawString(currentLine.toString(), 0, ty);
                ty += lineHeight;
                currentLine = new StringBuilder(word);
            }
        }

        g2.drawString(currentLine.toString(), 0, ty);
    }
}
