package net.somewhatcity.boiler.display.sources;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.api.BoilerSource;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import org.bukkit.entity.Player;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.UUID;

public class WhiteboardSource implements BoilerSource {

    private IMapDisplay display;

    private BufferedImage image;
    private Graphics2D g;

    private BufferedImage canvasImage;
    private Graphics2D canvas;
    private Color selectedColor = Color.RED;

    int lastX = -1;
    int lastY = -1;

    private HashMap<UUID, Point> lastPoint = new HashMap<>();
    private HashMap<UUID, Long> lastAction = new HashMap<>();
    long paintTime = 0;



    @Override
    public void load(LoadedMapDisplay lmd, JsonObject data) {
        this.display = lmd.getMapDisplay();
        image = new BufferedImage(display.width() * 128, display.height() * 128, BufferedImage.TYPE_INT_RGB);
        g = image.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        //menu
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 50, 50);

        g.setColor(Color.RED);
        g.fillRect(50, 0, 50, 50);

        g.setColor(Color.ORANGE);
        g.fillRect(100, 0, 50, 50);

        g.setColor(Color.YELLOW);
        g.fillRect(150, 0, 50, 50);

        g.setColor(Color.GREEN);
        g.fillRect(200, 0, 50, 50);

        g.setColor(Color.BLUE);
        g.fillRect(250, 0, 50, 50);

        g.setColor(Color.MAGENTA);
        g.fillRect(300, 0, 50, 50);

        g.setColor(Color.BLACK);
        g.fillRect(350, 0, 50, 50);

        canvasImage = new BufferedImage(display.width() * 128, display.height() * 128 - 50, BufferedImage.TYPE_INT_ARGB);
        canvas = canvasImage.createGraphics();
        canvas.setColor(Color.WHITE);
        canvas.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
        g.drawImage(canvasImage, 0, 50, null);
    }

    @Override
    public void unload() {

    }

    @Override
    public void onclick(int x, int y, Player player) {
        if(y < 50) {
            selectedColor = new Color(image.getRGB(x, y));
        } else {
            if(lastPoint.containsKey(player.getUniqueId()) && lastAction.containsKey(player.getUniqueId()) && lastAction.get(player.getUniqueId()) > System.currentTimeMillis() -250) {
                Point last = lastPoint.get(player.getUniqueId());
                canvas.setColor(selectedColor);
                canvas.setStroke(new BasicStroke(5));
                canvas.drawLine(last.x, last.y, x, y - 50);
            } else {
                canvas.setColor(selectedColor);
                canvas.fillOval(x, y - 50, 5, 5);
            }

            Point now = new Point(x, y - 50);
            lastPoint.put(player.getUniqueId(), now);
            lastAction.put(player.getUniqueId(), System.currentTimeMillis());

            g.drawImage(canvasImage, 0, 50, null);
        }
    }

    @Override
    public BufferedImage image() {
        return image;
    }
}
