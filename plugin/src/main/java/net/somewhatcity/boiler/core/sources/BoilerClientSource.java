/*
 * Copyright (c) 2025.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.SourceConfig;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.api.util.SourceType;
import net.somewhatcity.boiler.core.BoilerConfig;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bytedeco.javacpp.Loader;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@SourceConfig(
        sourceType = SourceType.CLIENT
)
@CreateCommandArguments(arguments = {
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class BoilerClientSource implements IBoilerSource {

    private IBoilerDisplay display;
    private Thread thread;
    private Process ffmpegProcess;
    private boolean stopped;

    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        this.display = display;
        String streamOut = display.rtspPublishUrl();

        thread = BoilerPlugin.getPlugin().runtime().startWorker("client-stream-" + display.id(), () -> {
            String sourceUrl = data.get("url").getAsString();
            String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

            String[] args = {
                    ffmpeg, "-re", "-loglevel", BoilerConfig.ffmpegLogLevel, "-i", sourceUrl, "-preset", "ultrafast", "-maxrate", "10000k", "-b:v", "4000k", "-bufsize", "4000k", "-f", "rtsp", streamOut
            };

            ProcessBuilder pb = new ProcessBuilder(args);
            try {
                pb.inheritIO();
                Process process;
                synchronized (this) {
                    if (stopped) return;
                    process = pb.start();
                    ffmpegProcess = process;
                }
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                BoilerPlugin.getPlugin().getLogger().warning("FFmpeg process failed: " + e.getMessage());
            } finally {
                synchronized (this) {
                    if (ffmpegProcess != null && ffmpegProcess.isAlive()) ffmpegProcess.destroyForcibly();
                }
            }
        });
    }

    @Override
    public synchronized void unload() {
        if (stopped) return;
        stopped = true;
        if(ffmpegProcess != null) {
            ffmpegProcess.descendants().forEach(ProcessHandle::destroy);
            ffmpegProcess.destroy();
        }

        if(thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, viewport.width, viewport.height);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.drawString("Boiler client mod required to view this source", 10, 30);

        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("rootMapId: " + display.mapDisplay().frameAt(0, 0).mapId(0), 10, 80);
        g2.drawString("source: " + display.sourceName(), 10, 100);
    }
}
