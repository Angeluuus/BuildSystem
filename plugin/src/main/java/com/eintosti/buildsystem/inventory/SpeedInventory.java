/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.eintosti.buildsystem.inventory;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.manager.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * @author einTosti
 */
public class SpeedInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryManager inventoryManager;

    public SpeedInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryManager = plugin.getInventoryManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, plugin.getString("speed_title"));
        fillGuiWithGlass(player, inventory);

        inventoryManager.addUrlSkull(inventory, 11, plugin.getString("speed_1"), "https://textures.minecraft.net/texture/71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530");
        inventoryManager.addUrlSkull(inventory, 12, plugin.getString("speed_2"), "https://textures.minecraft.net/texture/4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847");
        inventoryManager.addUrlSkull(inventory, 13, plugin.getString("speed_3"), "https://textures.minecraft.net/texture/1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5");
        inventoryManager.addUrlSkull(inventory, 14, plugin.getString("speed_4"), "https://textures.minecraft.net/texture/d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5");
        inventoryManager.addUrlSkull(inventory, 15, plugin.getString("speed_5"), "https://textures.minecraft.net/texture/6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2");

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 26; i++) {
            inventoryManager.addGlassPane(plugin, player, inventory, i);
        }
    }

    @EventHandler
    public void oInventoryClick(InventoryClickEvent event) {
        if (!inventoryManager.checkIfValidClick(event, "speed_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!player.hasPermission("buildsystem.speed")) {
            player.closeInventory();
            return;
        }

        switch (event.getSlot()) {
            case 11:
                setSpeed(player, 0.2f, 1);
                break;
            case 12:
                setSpeed(player, 0.4f, 2);
                break;
            case 13:
                setSpeed(player, 0.6f, 3);
                break;
            case 14:
                setSpeed(player, 0.8f, 4);
                break;
            case 15:
                setSpeed(player, 1.0f, 5);
                break;
            default:
                return;
        }

        XSound.ENTITY_CHICKEN_EGG.play(player);
        player.closeInventory();
    }

    private void setSpeed(Player player, float speed, int num) {
        if (player.isFlying()) {
            player.setFlySpeed(speed - 0.1f);
            player.sendMessage(plugin.getString("speed_set_flying").replace("%speed%", String.valueOf(num)));
        } else {
            player.setWalkSpeed(speed);
            player.sendMessage(plugin.getString("speed_set_walking").replace("%speed%", String.valueOf(num)));
        }
    }
}
