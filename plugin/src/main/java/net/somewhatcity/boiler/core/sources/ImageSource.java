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
import net.somewhatcity.boiler.api.CreateArgument;
import net.somewhatcity.boiler.api.CreateCommandArguments;
import net.somewhatcity.boiler.api.IBoilerSource;
import net.somewhatcity.boiler.api.display.IBoilerDisplay;
import net.somewhatcity.boiler.api.util.CommandArgumentType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

@CreateCommandArguments(arguments = {
        @CreateArgument(name = "url", type = CommandArgumentType.GREEDY_STRING)
})
public class ImageSource implements IBoilerSource {
    private BufferedImage image;
    @Override
    public void load(IBoilerDisplay display, JsonObject data) {
        JsonObject obj = new JsonObject();

        if(data == null) {
            obj.addProperty("message", "no error provided");
            display.source("error", obj);
            return;
        }

        try {
            String url = data.get("url").getAsString();
            if(url.startsWith("file:")) {
                url = url.substring(5);
                File imageFile = new File(url);
                if(imageFile.exists() && imageFile.isFile()) {
                    image = ImageIO.read(imageFile);
                } else {
                    obj.addProperty("message", "File not found");
                    display.source("error", obj);
                    return;
                }
            } else {
                if(data.has("base64")) {
                    String base64EncodedImage = data.get("base64").getAsString();
                    byte[] imageData = Base64.getDecoder().decode(base64EncodedImage);
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
                    image = ImageIO.read(bais);

                } else {
                    image = ImageIO.read(new URL(url));

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    boolean success = ImageIO.write(image, "jpg", baos);
                    if(!success) ImageIO.write(image, "png", baos);
                    byte[] imageData = baos.toByteArray();
                    String base64EncodedImage = Base64.getEncoder().encodeToString(imageData);

                    data.addProperty("base64", base64EncodedImage);
                    display.saveSourceData(data);
                }
            }
        } catch (IOException e) {
            display.source("error", obj);
        }
    }

    @Override
    public void unload() {

    }

    @Override
    public void draw(Graphics2D g2, Rectangle viewport) {
        g2.drawImage(image, 0, 0, viewport.width, viewport.height, null);
    }

}
