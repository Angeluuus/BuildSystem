/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.settings;

import com.cryptomorin.xseries.XMaterial;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author einTosti
 */
public class DesignInventory implements Listener {

    private final BuildSystem plugin;
    private final InventoryUtil inventoryUtil;

    public DesignInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.inventoryUtil = plugin.getInventoryUtil();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 36, Messages.getString("design_title"));
        fillGuiWithGlass(inventory, player);

        setItem(player, inventory, 10, XMaterial.RED_STAINED_GLASS, Messages.getString("design_red"), DesignColor.RED);
        setItem(player, inventory, 11, XMaterial.ORANGE_STAINED_GLASS, Messages.getString("design_orange"), DesignColor.ORANGE);
        setItem(player, inventory, 12, XMaterial.YELLOW_STAINED_GLASS, Messages.getString("design_yellow"), DesignColor.YELLOW);
        setItem(player, inventory, 13, XMaterial.PINK_STAINED_GLASS, Messages.getString("design_pink"), DesignColor.PINK);
        setItem(player, inventory, 14, XMaterial.MAGENTA_STAINED_GLASS, Messages.getString("design_magenta"), DesignColor.MAGENTA);
        setItem(player, inventory, 15, XMaterial.PURPLE_STAINED_GLASS, Messages.getString("design_purple"), DesignColor.PURPLE);
        setItem(player, inventory, 16, XMaterial.BROWN_STAINED_GLASS, Messages.getString("design_brown"), DesignColor.BROWN);

        setItem(player, inventory, 18, XMaterial.LIME_STAINED_GLASS, Messages.getString("design_lime"), DesignColor.LIME);
        setItem(player, inventory, 19, XMaterial.GREEN_STAINED_GLASS, Messages.getString("design_green"), DesignColor.GREEN);
        setItem(player, inventory, 20, XMaterial.BLUE_STAINED_GLASS, Messages.getString("design_blue"), DesignColor.BLUE);
        setItem(player, inventory, 21, XMaterial.CYAN_STAINED_GLASS, Messages.getString("design_aqua"), DesignColor.CYAN);
        setItem(player, inventory, 22, XMaterial.LIGHT_BLUE_STAINED_GLASS, Messages.getString("design_light_blue"), DesignColor.LIGHT_BLUE);
        setItem(player, inventory, 23, XMaterial.WHITE_STAINED_GLASS, Messages.getString("design_white"), DesignColor.WHITE);
        setItem(player, inventory, 24, XMaterial.LIGHT_GRAY_STAINED_GLASS, Messages.getString("design_grey"), DesignColor.LIGHT_GRAY);
        setItem(player, inventory, 25, XMaterial.GRAY_STAINED_GLASS, Messages.getString("design_dark_grey"), DesignColor.GRAY);
        setItem(player, inventory, 26, XMaterial.BLACK_STAINED_GLASS, Messages.getString("design_black"), DesignColor.BLACK);

        return inventory;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Inventory inventory, Player player) {
        for (int i = 0; i <= 8; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
        for (int i = 27; i <= 35; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void setItem(Player player, Inventory inventory, int position, XMaterial material, String displayName, DesignColor color) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        Settings settings = settingsManager.getSettings(player);

        ItemStack itemStack = inventoryUtil.getItemStack(material, settings.getDesignColor() == color ? "§a" + displayName : "§7" + displayName);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        itemStack.setItemMeta(itemMeta);
        if (settings.getDesignColor() == color) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, "design_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = plugin.getSettingsManager().getSettings(player);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack.getType().toString().contains("STAINED_GLASS_PANE")) {
            plugin.getSettingsInventory().openInventory(player);
            return;
        }

        switch (event.getSlot()) {
            case 10:
                settings.setDesignColor(DesignColor.RED);
                break;
            case 11:
                settings.setDesignColor(DesignColor.ORANGE);
                break;
            case 12:
                settings.setDesignColor(DesignColor.YELLOW);
                break;
            case 13:
                settings.setDesignColor(DesignColor.PINK);
                break;
            case 14:
                settings.setDesignColor(DesignColor.MAGENTA);
                break;
            case 15:
                settings.setDesignColor(DesignColor.PURPLE);
                break;
            case 16:
                settings.setDesignColor(DesignColor.BROWN);
                break;
            case 18:
                settings.setDesignColor(DesignColor.LIME);
                break;
            case 19:
                settings.setDesignColor(DesignColor.GREEN);
                break;
            case 20:
                settings.setDesignColor(DesignColor.BLUE);
                break;
            case 21:
                settings.setDesignColor(DesignColor.CYAN);
                break;
            case 22:
                settings.setDesignColor(DesignColor.LIGHT_BLUE);
                break;
            case 23:
                settings.setDesignColor(DesignColor.WHITE);
                break;
            case 24:
                settings.setDesignColor(DesignColor.LIGHT_GRAY);
                break;
            case 25:
                settings.setDesignColor(DesignColor.GRAY);
                break;
            case 26:
                settings.setDesignColor(DesignColor.BLACK);
                break;
        }

        openInventory(player);
    }
}