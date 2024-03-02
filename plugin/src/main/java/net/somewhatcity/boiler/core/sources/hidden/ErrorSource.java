/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources.hidden;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.GraphicUtils;

import java.awt.*;

public class ErrorSource implements IBoilerSource {
    private IBoilerDisplay display;
    private String error = "none";
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        this.display = display;
        if(data == null) {
            error = "Error while loading error";
            return;
        }
        if(data.get("message") != null) error = data.get("message").getAsString();
    }

    @Override
    public void unload() {

    }

    @Override
    public void draw(Graphics2D g, Rectangle viewport) {
        Rectangle heading = new Rectangle(0, 0, viewport.width, 20);

        g.setColor(Color.RED);
        g.fill(heading);
        g.setColor(Color.BLACK);
        GraphicUtils.centeredString(g, heading, "ERROR - Display #" + display.id());

        Rectangle info = new Rectangle(0, 20, viewport.width, viewport.height - 20);
        g.setColor(Color.BLACK);
        g.fill(info);

        g.setColor(Color.WHITE);
        GraphicUtils.centeredString(g, info, error);

    }
}
