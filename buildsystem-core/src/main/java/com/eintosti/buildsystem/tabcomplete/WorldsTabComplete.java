/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.tabcomplete;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.command.subcommand.Argument;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.generator.Generator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author einTosti
 */
public class WorldsTabComplete extends ArgumentSorter implements TabCompleter {

    private final BuildSystem plugin;
    private final WorldManager worldManager;

    public WorldsTabComplete(BuildSystem plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        plugin.getCommand("worlds").setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        ArrayList<String> arrayList = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return arrayList;
        }
        Player player = (Player) sender;

        switch (args.length) {
            case 1: {
                for (WorldsArgument argument : WorldsArgument.values()) {
                    String command = argument.getName();
                    String permission = argument.getPermission();

                    if (permission == null || player.hasPermission(permission)) {
                        addArgument(args[0], command, arrayList);
                    }
                }
                return arrayList;
            }

            case 2: {
                switch (args[0].toLowerCase()) {
                    case "builders":
                    case "edit":
                    case "info":
                    case "rename":
                    case "setcreator":
                    case "setitem":
                    case "setpermission":
                    case "setproject":
                    case "setstatus":
                    case "tp":
                    case "unimport":
                        worldManager.getBuildWorlds().stream()
                                .filter(world -> player.hasPermission(world.getPermission()) || world.getPermission().equalsIgnoreCase("-"))
                                .filter(world -> worldManager.isPermitted(player, "buildsystem." + args[0].toLowerCase(), world.getName()))
                                .forEach(world -> addArgument(args[1], world.getName(), arrayList));
                        break;

                    case "addbuilder":
                    case "delete":
                    case "removebuilder":
                        worldManager.getBuildWorlds().stream()
                                .filter(world -> worldManager.isPermitted(player, "buildsystem." + args[0].toLowerCase(), world.getName()))
                                .forEach(world -> addArgument(args[1], world.getName(), arrayList));
                        break;

                    case "import":
                        String[] directories = Bukkit.getWorldContainer().list((dir, name) -> {
                            for (String charString : name.split("")) {
                                if (charString.matches("[^A-Za-z0-9/_-]")) {
                                    return false;
                                }
                            }

                            File worldFolder = new File(dir, name);
                            if (!worldFolder.isDirectory()) {
                                return false;
                            }

                            File levelFile = new File(dir + File.separator + name + File.separator + "level.dat");
                            if (!levelFile.exists()) {
                                return false;
                            }

                            BuildWorld buildWorld = worldManager.getBuildWorld(name);
                            return buildWorld == null;
                        });

                        if (directories == null || directories.length == 0) {
                            return arrayList;
                        }

                        for (String projectName : directories) {
                            addArgument(args[1], projectName, arrayList);
                        }
                        break;
                }
                return arrayList;
            }

            case 3: {
                if (args[0].equalsIgnoreCase("import")) {
                    if (args[1].equalsIgnoreCase(" ")) {
                        return arrayList;
                    }

                    arrayList.add("-g");
                    return arrayList;
                }
            }

            case 4: {
                if (!args[2].equalsIgnoreCase("-g")) {
                    return arrayList;
                }

                for (Generator value : new Generator[]{Generator.NORMAL, Generator.FLAT, Generator.VOID}) {
                    addArgument(args[3], value.name(), arrayList);
                }
                return arrayList;
            }
        }

        return arrayList;
    }

    public enum WorldsArgument implements Argument {
        ADD_BUILDER("addBuilder", "buildsystem.addbuilder"),
        BUILDERS("builders", "buildsystem.builders"),
        DELETE("delete", "buildsystem.delete"),
        EDIT("edit", "buildsystem.edit"),
        HELP("help", null),
        IMPORT("import", "buildsystem.import"),
        IMPORT_ALL("importAll", "buildsystem.import.all"),
        INFO("info", "buildsystem.info"),
        ITEM("item", null),
        REMOVE_BUILDER("removeBuilder", "buildsystem.removebuilder"),
        RENAME("rename", "buildsystem.rename"),
        SET_CREATOR("setCreator", "buildsystem.setcreator"),
        SET_ITEM("setItem", "buildsystem.setitem"),
        SET_PERMISSION("setPermission", "buildsystem.setpermission"),
        SET_PROJECT("setProject", "buildsystem.setproject"),
        SET_STATUS("setStatus", "buildsystem.setstatus"),
        SET_SPAWN("setSpawn", "buildsystem.setspawn"),
        REMOVE_SPAWN("removeSpawn", "buildsystem.removespawn"),
        TP("tp", "buildsystem.worldtp"),
        UNIMPORT("unimport", "buildsystem.unimport");

        private final String command;
        private final String permission;

        WorldsArgument(String command, String permission) {
            this.command = command;
            this.permission = permission;
        }

        @Nullable
        public static WorldsArgument matchArgument(String input) {
            return Arrays.stream(values())
                    .filter(argument -> argument.getName().equalsIgnoreCase(input))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getName() {
            return command;
        }

        @Override
        public String getPermission() {
            return permission;
        }
    }
}