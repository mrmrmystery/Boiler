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
import com.google.gson.JsonParser;
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;
import net.somewhatcity.boiler.api.util.GraphicUtils;
import net.somewhatcity.boiler.core.Util;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "quality", type = CommandArgumentType.STRING),
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class YoutubeSource implements IBoilerSource {
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        OkHttpClient client = new OkHttpClient();

        try {
            URI uri = new URI(data.get("url").getAsString());
            String vQuality = data.get("quality").getAsString();

            JsonObject send = new JsonObject();
            send.addProperty("url", uri.toString());
            //send.addProperty("vCodec", "h264");
            send.addProperty("vQuality", vQuality);

            RequestBody body = RequestBody.create(send.toString().getBytes(StandardCharsets.UTF_8));

            Request request = new Request.Builder()
                    .get()
                    .url("https://api.cobalt.tools/api/json")
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .post(body)
                    .build();

            try(Response response = client.newCall(request).execute()) {
                String res = response.body().string();
                System.out.println(res);
                JsonObject json = (JsonObject) JsonParser.parseString(res);
                if(json.has("url")) {
                    String url = json.get("url").getAsString();

                    if(Util.isGstreamerInstalled()) {
                        JsonObject load = new JsonObject();
                        load.addProperty("url", url);
                        display.source("gstreamer", load);
                    } else {
                        JsonObject load = new JsonObject();
                        load.addProperty("url", url);
                        load.addProperty("buffer", 10);
                        load.addProperty("keepLastSourceData", true);
                        display.source("ffmpeg-buffered", load);
                    }

                } else {
                    JsonObject err = new JsonObject();
                    err.addProperty("message", "Video unavailable");
                    display.source("error", err);
                }
            } catch (IOException e) {
                JsonObject err = new JsonObject();
                err.addProperty("message", e.getMessage());
                display.source("error", err);
            }
        } catch (URISyntaxException e) {
            JsonObject err = new JsonObject();
            err.addProperty("message", e.getMessage());
            display.source("error", err);
        }
    }

    @Override
    public void unload() {

    }
    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, viewport.width, viewport.height);
        g2.setColor(Color.WHITE);
        GraphicUtils.centeredString(g2, viewport, "Loading video...");
    }
}
