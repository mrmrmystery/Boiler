/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.ui.BoilerUI;
import net.somewhatcity.boiler.api.ui.components.BButton;
import org.bukkit.entity.Player;

import java.awt.*;

public class TestSource implements IBoilerSource {

    private BoilerUI ui;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        ui = new BoilerUI(display);

        BButton button = new BButton(20, 20, 100, 30);
        button.text("Hallo!");
        button.addClickListener(e -> {
            e.player().sendMessage("click!");
        });
        ui.add(button);
    }

    @Override
    public void unload() {

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        if(ui != null) ui.draw(g2);
    }

    @Override
    public void onClick(Player player, int x, int y, boolean right) {
        ui.onClick(player, x, y, right);
    }
}
