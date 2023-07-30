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
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.media.movingimages.FFmpegFrameSource;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.api.BoilerCreateCommand;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.display.LoadedMapDisplay;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class TestSource implements BoilerSource {

    private LoadedMapDisplay display;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        this.display = display;
        display.setDefaultRendering(false);
        IDrawingSpace space = display.getDrawingSpace();

        String filePath = data.get("file").getAsString();
        File file = Boiler.getPlugin().getDataFolder().toPath().resolve("media/" + filePath).toFile();

        FFmpegFrameSource source = new FFmpegFrameSource(file, 10, space, true);
        source.start();
    }

    @Override
    public void unload() {
        display.setDefaultRendering(true);
    }

    @Override
    public BufferedImage image() {
        return null;
    }

    @BoilerCreateCommand
    public static List<Argument<?>> command() {
        return List.of(new GreedyStringArgument("file"));
    }
}
