package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.util.Webserver;

import java.awt.image.BufferedImage;

public class WebSource implements BoilerSource {

    private BufferedImage image;
    private LoadedMapDisplay loadedMapDisplay;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        this.loadedMapDisplay = display;
        Webserver.activeDisplays.put(loadedMapDisplay.getId(), loadedMapDisplay);
    }

    @Override
    public void unload() {
        Webserver.activeDisplays.remove(loadedMapDisplay.getId());
    }

    @Override
    public BufferedImage image() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
