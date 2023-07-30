/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.api.ui.BoilerUI;
import net.somewhatcity.boiler.api.ui.components.*;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.util.Assets;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CustomUISource implements BoilerSource {

    private BoilerUI ui;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        ui = new BoilerUI(display);
        BFrame frame = ui.getFrame();

        BButton button = new BButton(10, 10, "Click me!");
        button.addClickListener(player -> {
            player.sendMessage("You clicked the button!");
        });
        frame.add(button);

        BTextBox textBox = new BTextBox(10, 30, 300, 100, "Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World! Hello World!");
        frame.add(textBox);

        BCheckBox checkBox1 = new BCheckBox(10, 280);
        frame.add(checkBox1);
        BCheckBox checkBox2 = new BCheckBox(10, 260, "CheckBox with Label");
        frame.add(checkBox2);

        BFrame topFrame = new BFrame(200, 100, 300, 100);
        topFrame.backgroundColor = Color.decode("#3737dc");
        topFrame.add(new BButton(10, 10, "Hi"));
        topFrame.add(new BTextBox(10, 30, 150, 20, "Another Frame on top"));
        topFrame.add(new BSlider(10, 60, 150, 20));
        frame.add(topFrame);

        BImage image = new BImage(10, 150, 100, 100, Assets.SETTINGS_BG);
        frame.add(image);
    }

    @Override
    public void unload() {
        ui.close();
    }

    @Override
    public void onclick(int x, int y, Player player) {
        ui.handleClick(x, y, player);
    }

    @Override
    public BufferedImage image() {
        return ui.getImage();
    }
}
