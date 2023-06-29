package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.display.LoadedMapDisplay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

public class ImageSource extends Source {

    private BufferedImage image;

    @Override
    public void load(LoadedMapDisplay loadedMapDisplay, IMapDisplay display, JsonObject data) {
        String url = data.get("url").getAsString();
        try{
            image = ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Loaded image from " + url);
    }

    @Override
    public void unload() {

    }

    @Override
    public BufferedImage getFrame() {
        return image;
    }
}
