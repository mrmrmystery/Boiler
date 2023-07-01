package net.somewhatcity.boiler.display;

import com.google.gson.JsonObject;
import de.pianoman911.mapengine.api.clientside.IMapDisplay;
import net.somewhatcity.boiler.db.DB;
import net.somewhatcity.boiler.db.SMapDisplay;
import net.somewhatcity.boiler.display.sources.Source;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapDisplayManager {

    private static Timer mapDisplayTickTimer;
    private static List<LoadedMapDisplay> loadedMapDisplays = new ArrayList<>();
    private static List<LoadedMapDisplay> loadedMapDisplaysToAdd = new ArrayList<>();
    private static List<LoadedMapDisplay> loadedMapDisplaysToRemove = new ArrayList<>();

    public static void loadAll() {

        try(Session session = DB.openSession()) {
            session.beginTransaction();
            List<SMapDisplay> displays = session.createQuery("from SMapDisplay", SMapDisplay.class).list();
            displays.forEach(display -> {
                LoadedMapDisplay loadedMapDisplay = new LoadedMapDisplay(display);
                loadedMapDisplays.add(loadedMapDisplay);
            });
            session.getTransaction().commit();
        }
        startTickTimer();
    }

    public static void startTickTimer() {
        mapDisplayTickTimer = new Timer();
        mapDisplayTickTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                loadedMapDisplays.forEach(LoadedMapDisplay::tick);
                loadedMapDisplays.addAll(loadedMapDisplaysToAdd);
                loadedMapDisplaysToAdd.clear();
                loadedMapDisplays.removeAll(loadedMapDisplaysToRemove);
                loadedMapDisplaysToRemove.clear();
            }
        }, 0, 50);
    }

    public static void createNew(Location locationA, Location locationB, BlockFace face, String sourceType, String savedData) {
        try(Session session = DB.openSession()) {
            session.beginTransaction();
            SMapDisplay sMapDisplay = new SMapDisplay();
            sMapDisplay.setLocationA(locationA);
            sMapDisplay.setLocationB(locationB);
            sMapDisplay.setFacing(face);
            sMapDisplay.setSourceType(sourceType);
            sMapDisplay.setSavedData(savedData);
            sMapDisplay.setDisplaySettings("{\"caching\": true,\"dithering\": false}");
            session.persist(sMapDisplay);
            loadedMapDisplaysToAdd.add(new LoadedMapDisplay(sMapDisplay));
            session.getTransaction().commit();
        }
    }

    public static void delete(int id) {
        try(Session session = DB.openSession()) {
            session.beginTransaction();
            SMapDisplay sMapDisplay = session.get(SMapDisplay.class, id);
            session.remove(sMapDisplay);
            LoadedMapDisplay loadedMapDisplay = loadedMapDisplays.stream().filter(display -> display.getId() == id).findFirst().orElse(null);
            if(loadedMapDisplay != null){
                loadedMapDisplaysToRemove.add(loadedMapDisplay);
                loadedMapDisplay.delete();
            }
            session.getTransaction().commit();
        }
    }

    public static void setSource(int id, String sourceType, String savedData) {
        try(Session session = DB.openSession()) {
            session.beginTransaction();
            SMapDisplay sMapDisplay = session.get(SMapDisplay.class, id);
            sMapDisplay.setSourceType(sourceType);
            sMapDisplay.setSavedData(savedData);
            session.update(sMapDisplay);
            LoadedMapDisplay loadedMapDisplay = loadedMapDisplays.stream().filter(display -> display.getId() == id).findFirst().orElse(null);
            if(loadedMapDisplay != null){
                loadedMapDisplay.setSource(sourceType, savedData);
            }
            session.getTransaction().commit();
        }
    }

    public static void setSettings(int id, JsonObject object) {
        try(Session session = DB.openSession()) {
            session.beginTransaction();
            SMapDisplay sMapDisplay = session.get(SMapDisplay.class, id);
            sMapDisplay.setDisplaySettings(object.toString());
            session.update(sMapDisplay);
            LoadedMapDisplay loadedMapDisplay = loadedMapDisplays.stream().filter(display -> display.getId() == id).findFirst().orElse(null);
            if(loadedMapDisplay != null){
                loadedMapDisplay.setSettings(object);
            }
            session.getTransaction().commit();
        }
    }

    public static LoadedMapDisplay getLoadedMapDisplay(int id) {
        return loadedMapDisplays.stream().filter(display -> display.getId() == id).findFirst().orElse(null);
    }

    public static LoadedMapDisplay getLoadedMapDisplay(IMapDisplay mapDisplay) {
        return loadedMapDisplays.stream().filter(display -> display.getMapDisplay().equals(mapDisplay)).findFirst().orElse(null);
    }

    public static List<LoadedMapDisplay> getLoadedMapDisplays() {
        return loadedMapDisplays;
    }
}
