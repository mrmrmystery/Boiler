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

import net.somewhatcity.boiler.api.ui.components.BFrame;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.Time;
import java.util.Timer;

public class BoilerUI {

    private BufferedImage image;
    private BFrame frame;
    private Timer renderTimer;

    public BoilerUI(LoadedMapDisplay display) {
        frame = new BFrame(0, 0, display.getMapDisplay().pixelWidth(), display.getMapDisplay().pixelHeight());
        renderTimer = new Timer();
        renderTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                render();
            }
        }, 0, 20);
    }

    public BFrame getFrame() {
        return frame;
    }

    public void render() {
        BufferedImage canvas = new BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, frame.width, frame.height);
        frame.paintComponent(g);
        g.dispose();
        image = canvas;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void handleClick(int x, int y, Player player) {
        frame.handleClick(x, y, player);

    }

    public void close() {
        renderTimer.cancel();
    }

}
