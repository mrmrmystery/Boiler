package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CommandArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.api.BoilerCreateCommand;
import net.somewhatcity.boiler.display.LoadedMapDisplay;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ImageSource implements BoilerSource {

    private BufferedImage image;

    @Override
    public void load(LoadedMapDisplay display, JsonObject data) {
        String url = data.get("url").getAsString();
        try{
            image = ImageIO.read(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unload() {

    }

    @Override
    public BufferedImage image() {
        return image;
    }

    @BoilerCreateCommand
    public static List<Argument<?>> command() {
        return List.of(new GreedyStringArgument("url"));
    }
}
