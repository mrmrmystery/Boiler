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

import java.awt.*;

public class ErrorSource implements IBoilerSource {
    private static final String FALLBACK_MESSAGE = "An unexpected error occurred while loading the source.";
    private final StatusScreen screen = new StatusScreen();
    private int displayId;
    private String error = FALLBACK_MESSAGE;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        displayId = display.id();
        error = FALLBACK_MESSAGE;
        if (data != null && data.has("message") && data.get("message").isJsonPrimitive()) {
            String message = data.get("message").getAsString();
            if (!message.isBlank()) error = message;
        }
        screen.clear();
    }

    @Override
    public void unload() {
        screen.clear();
    }

    @Override
    public void draw(Graphics2D g, Rectangle viewport) {
        screen.draw(g, viewport, displayId, error);
    }
}
