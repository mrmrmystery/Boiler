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
import de.pianoman911.mapengine.api.drawing.IDrawingSpace;
import de.pianoman911.mapengine.media.movingimages.FFmpegFrameSource;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.core.audio.BoilerAudioPlayer;
import net.somewhatcity.boiler.core.audio.SVCAudioPlayer;

import java.io.File;
import java.util.List;

public class VideoSource implements IBoilerSource {

    private FFmpegFrameSource source;
    private BoilerAudioPlayer bap;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        File file = new File(data.get("url").getAsString());
        if(!file.exists()) return;
        source = new FFmpegFrameSource(file, 10, display.drawingSpace(), true);
        bap = new SVCAudioPlayer(display.cornerA(), 100);
        bap.play(file.getPath());
        source.start();
    }

    @Override
    public void unload() {
        source.stop();
        bap.stop();
    }

    @Override
    public void draw(IDrawingSpace drawingSpace) {

    }

    @Override
    public byte[] provide20msAudio() {
        return null;
    }

    public static List<Argument<?>> creationArguments() {
        return List.of(new GreedyStringArgument("url"));
    }
}
