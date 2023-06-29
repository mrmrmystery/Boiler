package net.somewhatcity.boiler.util;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.util.JavalinLogger;
import net.somewhatcity.boiler.Boiler;
import net.somewhatcity.boiler.display.LoadedMapDisplay;
import net.somewhatcity.boiler.display.sources.WebSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;

public class Webserver {

    public static HashMap<Integer, LoadedMapDisplay> activeDisplays = new HashMap<>();

    public static void start() {
        new Thread(() -> {
            int port = Boiler.getPlugin().getConfig().getInt("webserver.port", 7011);

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(Webserver.class.getClassLoader());
            JavalinLogger.enabled = false;
            Javalin app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.staticFiles.add(staticFiles -> {
                    staticFiles.location = Location.CLASSPATH;
                    staticFiles.directory = "static";
                });
                config.jetty.wsFactoryConfig(wsfc -> {
                    wsfc.setIdleTimeout(Duration.of(10, java.time.temporal.ChronoUnit.SECONDS));
                    wsfc.setInputBufferSize(10000000);
                    wsfc.setMaxBinaryMessageSize(10000000);
                });
            }).start(port);
            Thread.currentThread().setContextClassLoader(classLoader);

            app.get("/", ctx -> {
                ctx.redirect("/index.html");
            });
            app.ws("/ws", ws -> {
                ws.onConnect(ctx -> {
                });
                ws.onMessage(ctx -> {
                });
                ws.onBinaryMessage(ctx -> {
                    int id = Integer.valueOf(ctx.queryParam("id"));
                    if(activeDisplays.get(id) == null) return;
                    LoadedMapDisplay display = activeDisplays.get(id);
                    if(display.getSelectedSource() instanceof WebSource wSource) {
                        ByteArrayInputStream bais = new ByteArrayInputStream(ctx.data());
                        BufferedImage image = ImageIO.read(bais);
                        wSource.setImage(image);
                    } else {
                        activeDisplays.remove(id);
                    }
                });
                ws.onClose(ctx -> {

                });
                ws.onError(ctx -> {
                    ctx.error().printStackTrace();
                });
            });
        }).start();
    }
}
