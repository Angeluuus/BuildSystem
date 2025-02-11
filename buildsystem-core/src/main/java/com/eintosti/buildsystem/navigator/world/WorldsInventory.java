/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.navigator.world;

import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.player.PlayerManager;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.data.WorldStatus;
import com.google.common.collect.Sets;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class WorldsInventory extends FilteredWorldsInventory {

    private final BuildSystem plugin;
    private final PlayerManager playerManager;
    private final InventoryUtil inventoryUtil;

    public WorldsInventory(BuildSystem plugin) {
        super(plugin, "world_navigator_title", "world_navigator_no_worlds", Visibility.PUBLIC,
                Sets.newHashSet(WorldStatus.NOT_STARTED, WorldStatus.IN_PROGRESS, WorldStatus.ALMOST_FINISHED, WorldStatus.FINISHED)
        );

        this.plugin = plugin;
        this.inventoryUtil = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
    }

    @Override
    protected Inventory createInventory(Player player) {
        Inventory inventory = super.createInventory(player);
        if (playerManager.canCreateWorld(player, super.getVisibility())) {
            addWorldCreateItem(inventory, player);
        }
        return inventory;
    }

    private void addWorldCreateItem(Inventory inventory, Player player) {
        if (!player.hasPermission("buildsystem.create.public")) {
            inventoryUtil.addGlassPane(plugin, player, inventory, 49);
            return;
        }
        inventoryUtil.addUrlSkull(inventory, 49, Messages.getString("world_navigator_create_world"), "3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716");
    }
}