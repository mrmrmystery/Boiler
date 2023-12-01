/*
 * Copyright (c) 2023.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core.gui;

import net.somewhatcity.boiler.api.display.IGuiManager;
import net.somewhatcity.boiler.core.BoilerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class ImplGuiManager implements IGuiManager {

    private final BoilerPlugin plugin;
    private World world;
    public ImplGuiManager(BoilerPlugin plugin) {
        this.plugin = plugin;

        world = Bukkit.getWorld("boilerui");
        if(world != null) return;

        WorldCreator creator = new WorldCreator("world_boilerui");
        creator.environment(World.Environment.NORMAL);
        creator.generator(new ChunkGenerator() {
            @Override
            public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
                for(int y = chunkData.getMinHeight(); y < 65 && y < chunkData.getMaxHeight(); y++) {
                    for(int x = 0; x < 16; x++) {
                        for(int z = 0; z < 16; z++) {
                            chunkData.setBlock(x, 65, z, Material.AIR);
                        }
                    }
                }
            }
        });

        creator.generateStructures(false);
        world = Bukkit.createWorld(creator);

    }


    @Override
    public void open(Player player, String source) {
        new ImplBoilerGui(plugin, player, source);
    }

    public World world() {
        return world;
    }
}
