/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.command.subcommand.worlds;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.command.subcommand.SubCommand;
import com.eintosti.buildsystem.tabcomplete.WorldsTabComplete;
import com.eintosti.buildsystem.util.UUIDFetcher;
import com.eintosti.buildsystem.util.external.PlayerChatInput;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.UUID;

/**
 * @author einTosti
 */
public class RemoveBuilderSubCommand extends SubCommand {

    private final BuildSystem plugin;
    private final String worldName;

    public RemoveBuilderSubCommand(BuildSystem plugin, String worldName) {
        super(WorldsTabComplete.WorldsArgument.REMOVE_BUILDER);

        this.plugin = plugin;
        this.worldName = worldName;
    }

    @Override
    public void execute(Player player, String[] args) {
        WorldManager worldManager = plugin.getWorldManager();
        if (!worldManager.isPermitted(player, getArgument().getPermission(), worldName)) {
            plugin.sendPermissionMessage(player);
            return;
        }

        if (args.length > 2) {
            Messages.sendMessage(player, "worlds_removebuilder_usage");
            return;
        }

        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            Messages.sendMessage(player, "worlds_removebuilder_unknown_world");
            return;
        }

        getRemoveBuilderInput(player, buildWorld);
    }

    private void getRemoveBuilderInput(Player player, BuildWorld buildWorld) {
        new PlayerChatInput(plugin, player, "enter_player_name", input -> {
            String builderName = input.trim();
            Player builderPlayer = Bukkit.getPlayerExact(builderName);
            UUID builderId;

            if (builderPlayer == null) {
                builderId = UUIDFetcher.getUUID(builderName);
                if (builderId == null) {
                    Messages.sendMessage(player, "worlds_removebuilder_player_not_found");
                    player.closeInventory();
                    return;
                }
            } else {
                builderId = builderPlayer.getUniqueId();
            }

            if (builderId.equals(player.getUniqueId()) && buildWorld.isCreator(player)) {
                Messages.sendMessage(player, "worlds_removebuilder_not_yourself");
                player.closeInventory();
                return;
            }

            if (!buildWorld.isBuilder(builderId)) {
                Messages.sendMessage(player, "worlds_removebuilder_not_builder");
                player.closeInventory();
                return;
            }

            buildWorld.removeBuilder(builderId);
            XSound.ENTITY_PLAYER_LEVELUP.play(player);
            Messages.sendMessage(player, "worlds_removebuilder_removed", new AbstractMap.SimpleEntry<>("%builder%", builderName));

            player.closeInventory();
        });
    }
}