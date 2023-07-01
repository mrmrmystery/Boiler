package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.display.LoadedMapDisplay;

import java.awt.image.BufferedImage;

public abstract class Source {

    public abstract void load(LoadedMapDisplay loadedMapDisplay, IMapDisplay display, JsonObject data);
    public abstract void unload();
    public abstract BufferedImage getFrame();

    public static String types[] = {
            "NONE",
            "IMAGE",
            "GIF",
            "WHITEBOARD",
            "WEB",
            "FILE"
    };
}
