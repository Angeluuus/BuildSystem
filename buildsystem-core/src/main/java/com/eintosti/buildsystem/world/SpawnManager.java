/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.world;

import com.cryptomorin.xseries.messages.Titles;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.config.SpawnConfig;
import com.eintosti.buildsystem.world.data.WorldType;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author einTosti
 */
public class SpawnManager {

    private final WorldManager worldManager;
    private final SpawnConfig spawnConfig;

    private String spawnName;
    private Location spawn;

    public SpawnManager(BuildSystem plugin) {
        this.worldManager = plugin.getWorldManager();
        this.spawnConfig = new SpawnConfig(plugin);
    }

    public boolean teleport(Player player) {
        if (!spawnExists()) {
            return false;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(spawnName);
        if (buildWorld != null) {
            if (!buildWorld.isLoaded()) {
                buildWorld.load(player);
            }
        }

        player.setFallDistance(0);
        PaperLib.teleportAsync(player, spawn);
        Titles.clearTitle(player);

        return true;
    }

    public boolean spawnExists() {
        return spawn != null;
    }

    public Location getSpawn() {
        return spawn;
    }

    public World getSpawnWorld() {
        return spawn.getWorld();
    }

    public void set(Location location, String worldName) {
        this.spawn = location;
        this.spawnName = worldName;
    }

    public void remove() {
        this.spawn = null;
    }

    public void save() {
        spawnConfig.saveSpawn(spawn);
    }

    public void load() {
        FileConfiguration configuration = spawnConfig.getFile();
        String string = configuration.getString("spawn");

        if (string == null || string.trim().equals("")) {
            return;
        }

        String[] parts = string.split(":");
        if (parts.length != 6) {
            return;
        }

        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            buildWorld = new BuildWorld(worldName, "", UUID.randomUUID(), WorldType.UNKNOWN, System.currentTimeMillis(), false, null);
        }
        buildWorld.load();

        this.spawnName = worldName;
        this.spawn = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
}