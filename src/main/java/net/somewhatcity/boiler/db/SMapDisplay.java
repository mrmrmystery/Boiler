package net.somewhatcity.boiler.db;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import net.somewhatcity.boiler.display.sources.Source;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

@Entity
public class SMapDisplay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int xA;
    private int yA;
    private int zA;
    private int xB;
    private int yB;
    private int zB;

    private String world;

    private BlockFace facing;

    private Source.Type type;
    private String savedData;


    public SMapDisplay() {
    }

    public SMapDisplay(Location locA, Location locB, BlockFace face, int width, int height, Source.Type type, String savedData) {
        this.xA = locA.getBlockX();
        this.yA = locA.getBlockY();
        this.zA = locA.getBlockZ();
        this.xB = locB.getBlockX();
        this.yB = locB.getBlockY();
        this.zB = locB.getBlockZ();
        this.world = locA.getWorld().getName();
        this.facing = face;
        this.type = type;
        this.savedData = savedData;
    }

    public int getId() {
        return id;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public Source.Type getType() {
        return type;
    }

    public String getSavedData() {
        return savedData;
    }

    public Location getLocationA() {
        return new Location(Bukkit.getWorld(world), xA, yA, zA);
    }

    public Location getLocationB() {
        return new Location(Bukkit.getWorld(world), xB, yB, zB);
    }

    public void setSavedData(String savedData) {
        this.savedData = savedData;
    }

    public void setType(Source.Type type) {
        this.type = type;
    }

    public void setLocationA(Location locationA) {
        this.xA = locationA.getBlockX();
        this.yA = locationA.getBlockY();
        this.zA = locationA.getBlockZ();
        this.world = locationA.getWorld().getName();
    }

    public void setLocationB(Location locationB) {
        this.xB = locationB.getBlockX();
        this.yB = locationB.getBlockY();
        this.zB = locationB.getBlockZ();
    }

    public void setFacing(BlockFace facing) {
        this.facing = facing;
    }
}
