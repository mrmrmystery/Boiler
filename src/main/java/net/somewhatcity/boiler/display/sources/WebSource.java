package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.util.Webserver;

import java.awt.image.BufferedImage;

public class WebSource extends Source {

    private BufferedImage image;
    private LoadedMapDisplay loadedMapDisplay;

    @Override
    public void load(LoadedMapDisplay loadedMapDisplay, IMapDisplay display, JsonObject data) {
        this.loadedMapDisplay = loadedMapDisplay;
        Webserver.activeDisplays.put(loadedMapDisplay.getId(), loadedMapDisplay);
    }

    @Override
    public void unload() {
        Webserver.activeDisplays.remove(loadedMapDisplay.getId());
    }

    @Override
    public BufferedImage getFrame() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
