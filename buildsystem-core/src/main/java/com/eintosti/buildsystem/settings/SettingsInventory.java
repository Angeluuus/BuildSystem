/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.settings;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.navigator.NavigatorType;
import com.eintosti.buildsystem.navigator.WorldSort;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * @author einTosti
 */
public class SettingsInventory implements Listener {

    private final BuildSystem plugin;
    private final ConfigValues configValues;

    private final InventoryUtil inventoryUtil;
    private final SettingsManager settingsManager;

    public SettingsInventory(BuildSystem plugin) {
        this.plugin = plugin;
        this.configValues = plugin.getConfigValues();

        this.inventoryUtil = plugin.getInventoryUtil();
        this.settingsManager = plugin.getSettingsManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private Inventory getInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, Messages.getString("settings_title"));
        fillGuiWithGlass(player, inventory);

        Settings settings = settingsManager.getSettings(player);
        addDesignItem(inventory, player);
        addClearInventoryItem(inventory, player);
        addSettingsItem(inventory, 13, XMaterial.DIAMOND_AXE, settings.isDisableInteract(), Messages.getString("settings_disableinteract_item"), Messages.getStringList("settings_disableinteract_lore"));
        addSettingsItem(inventory, 14, XMaterial.ENDER_EYE, settings.isHidePlayers(), Messages.getString("settings_hideplayers_item"), Messages.getStringList("settings_hideplayers_lore"));
        addSettingsItem(inventory, 15, XMaterial.OAK_SIGN, settings.isInstantPlaceSigns(), Messages.getString("settings_instantplacesigns_item"), Messages.getStringList("settings_instantplacesigns_lore"));
        addSettingsItem(inventory, 20, XMaterial.SLIME_BLOCK, settings.isKeepNavigator(), Messages.getString("settings_keep_navigator_item"), Messages.getStringList("settings_keep_navigator_lore"));
        addSettingsItem(inventory, 21, configValues.getNavigatorItem(), settings.getNavigatorType().equals(NavigatorType.NEW), Messages.getString("settings_new_navigator_item"), Messages.getStringList("settings_new_navigator_lore"));
        addSettingsItem(inventory, 22, XMaterial.GOLDEN_CARROT, settings.isNightVision(), Messages.getString("settings_nightvision_item"), Messages.getStringList("settings_nightvision_lore"));
        addSettingsItem(inventory, 23, XMaterial.BRICKS, settings.isNoClip(), Messages.getString("settings_no_clip_item"), Messages.getStringList("settings_no_clip_lore"));
        addSettingsItem(inventory, 24, XMaterial.IRON_TRAPDOOR, settings.isTrapDoor(), Messages.getString("settings_open_trapdoors_item"), Messages.getStringList("settings_open_trapdoors_lore"));
        addSettingsItem(inventory, 29, XMaterial.FERN, settings.isPlacePlants(), Messages.getString("settings_placeplants_item"), Messages.getStringList("settings_placeplants_lore"));
        addSettingsItem(inventory, 30, XMaterial.PAPER, settings.isScoreboard(), configValues.isScoreboard() ? Messages.getString("settings_scoreboard_item") : Messages.getString("settings_scoreboard_disabled_item"),
                configValues.isScoreboard() ? Messages.getStringList("settings_scoreboard_lore") : Messages.getStringList("settings_scoreboard_disabled_lore"));
        addSettingsItem(inventory, 31, getSlabBreakingMaterial(), settings.isSlabBreaking(), Messages.getString("settings_slab_breaking_item"), Messages.getStringList("settings_slab_breaking_lore"));
        addSettingsItem(inventory, 32, XMaterial.MAGMA_CREAM, settings.isSpawnTeleport(), Messages.getString("settings_spawnteleport_item"), Messages.getStringList("settings_spawnteleport_lore"));
        addWorldSortItem(inventory, player);

        return inventory;
    }

    private XMaterial getSlabBreakingMaterial() {
        return XMaterial.supports(13) ? XMaterial.SMOOTH_STONE_SLAB : XMaterial.STONE_SLAB;
    }

    public void openInventory(Player player) {
        player.openInventory(getInventory(player));
    }

    private void fillGuiWithGlass(Player player, Inventory inventory) {
        for (int i = 0; i <= 44; i++) {
            inventoryUtil.addGlassPane(plugin, player, inventory, i);
        }
    }

    private void addSettingsItem(Inventory inventory, int position, XMaterial material, boolean enabled, String displayName, List<String> lore) {
        ItemStack itemStack = material.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.setDisplayName(displayName);
        itemMeta.setLore(lore);
        itemMeta.addItemFlags(ItemFlag.values());
        itemStack.setItemMeta(itemMeta);

        if (enabled) {
            itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }

        inventory.setItem(position, itemStack);
    }

    private void addClearInventoryItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);
        XMaterial xMaterial = settings.isClearInventory() ? XMaterial.MINECART : XMaterial.CHEST_MINECART;
        addSettingsItem(inventory, 12, xMaterial, settings.isClearInventory(), Messages.getString("settings_clear_inventory_item"), Messages.getStringList("settings_clear_inventory_lore"));
    }

    private void addDesignItem(Inventory inventory, Player player) {
        ItemStack itemStack = inventoryUtil.getItemStack(inventoryUtil.getColouredGlass(plugin, player), Messages.getString("settings_change_design_item"));
        ItemMeta itemMeta = itemStack.getItemMeta();

        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        itemMeta.setLore(Messages.getStringList("settings_change_design_lore"));
        itemStack.setItemMeta(itemMeta);
        itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

        inventory.setItem(11, itemStack);
    }

    private void addWorldSortItem(Inventory inventory, Player player) {
        Settings settings = settingsManager.getSettings(player);

        String url;
        List<String> lore;
        switch (settings.getWorldSort()) {
            default: //NAME_A_TO_Z
                url = "a67d813ae7ffe5be951a4f41f2aa619a5e3894e85ea5d4986f84949c63d7672e";
                lore = Messages.getStringList("settings_worldsort_lore_alphabetically_name_az");
                break;
            case NAME_Z_TO_A:
                url = "90582b9b5d97974b11461d63eced85f438a3eef5dc3279f9c47e1e38ea54ae8d";
                lore = Messages.getStringList("settings_worldsort_lore_alphabetically_name_za");
                break;
            case PROJECT_A_TO_Z:
                url = "2ac58b1a3b53b9481e317a1ea4fc5eed6bafca7a25e741a32e4e3c2841278c";
                lore = Messages.getStringList("settings_worldsort_lore_alphabetically_project_az");
                break;
            case PROJECT_Z_TO_A:
                url = "4e91200df1cae51acc071f85c7f7f5b8449d39bb32f363b0aa51dbc85d133e";
                lore = Messages.getStringList("settings_worldsort_lore_alphabetically_project_za");
                break;
            case STATUS_NOT_STARTED:
                url = "ed339d52393d5183a3664015c0b2c6c1012ea1b525ed952073311ca180a0e6";
                lore = Messages.getStringList("settings_worldsort_lore_status_not_started");
                break;
            case STATUS_FINISHED:
                url = "400b9fb7aab4e3b69a5474b1a05d0a4b1449f4080a6c0f6977d0e33271c9b029";
                lore = Messages.getStringList("settings_worldsort_lore_status_finished");
                break;
            case NEWEST_FIRST:
                url = "71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530";
                lore = Messages.getStringList("settings_worldsort_lore_date_newest");
                break;
            case OLDEST_FIRST:
                url = "e67caf7591b38e125a8017d58cfc6433bfaf84cd499d794f41d10bff2e5b840";
                lore = Messages.getStringList("settings_worldsort_lore_date_oldest");
                break;
        }

        ItemStack itemStack = inventoryUtil.getUrlSkull(Messages.getString("settings_worldsort_item"), url);
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setLore(lore);
        }

        itemStack.setItemMeta(itemMeta);
        inventory.setItem(33, itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!inventoryUtil.checkIfValidClick(event, "settings_title")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Settings settings = settingsManager.getSettings(player);

        switch (event.getSlot()) {
            case 11:
                plugin.getDesignInventory().openInventory(player);
                XSound.ENTITY_ITEM_PICKUP.play(player);
                return;
            case 12:
                settings.setClearInventory(!settings.isClearInventory());
                break;
            case 13:
                settings.setDisableInteract(!settings.isDisableInteract());
                break;
            case 14:
                settings.setHidePlayers(!settings.isHidePlayers());
                toggleHidePlayers(player, settings);
                break;
            case 15:
                settings.setInstantPlaceSigns(!settings.isInstantPlaceSigns());
                break;

            case 20:
                settings.setKeepNavigator(!settings.isKeepNavigator());
                break;
            case 21:
                if (settings.getNavigatorType().equals(NavigatorType.OLD)) {
                    settings.setNavigatorType(NavigatorType.NEW);
                } else {
                    settings.setNavigatorType(NavigatorType.OLD);
                    plugin.getArmorStandManager().removeArmorStands(player);
                    if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                    }
                }
                break;
            case 22:
                if (!settings.isNightVision()) {
                    settings.setNightVision(true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                } else {
                    settings.setNightVision(false);
                    if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    }
                }
                break;
            case 23:
                NoClipManager noClipManager = plugin.getNoClipManager();
                if (!settings.isNoClip()) {
                    settings.setNoClip(true);
                    noClipManager.startNoClip(player);
                } else {
                    settings.setNoClip(false);
                    noClipManager.stopNoClip(player.getUniqueId());
                }
                break;
            case 24:
                settings.setTrapDoor(!settings.isTrapDoor());
                break;

            case 29:
                settings.setPlacePlants(!settings.isPlacePlants());
                break;
            case 30:
                if (!configValues.isScoreboard()) {
                    XSound.ENTITY_ITEM_BREAK.play(player);
                    return;
                }
                if (settings.isScoreboard()) {
                    settings.setScoreboard(false);
                    settingsManager.stopScoreboard(player);
                } else {
                    settings.setScoreboard(true);
                    settingsManager.startScoreboard(player);
                    plugin.getPlayerManager().forceUpdateSidebar(player);
                }
                break;
            case 31:
                settings.setSlabBreaking(!settings.isSlabBreaking());
                break;
            case 32:
                settings.setSpawnTeleport(!settings.isSpawnTeleport());
                break;
            case 33:
                WorldSort newSort = event.isLeftClick() ? settings.getWorldSort().getNext() : settings.getWorldSort().getPrevious();
                settings.setWorldSort(newSort);
                break;
            default:
                return;
        }

        XSound.ENTITY_ITEM_PICKUP.play(player);
        plugin.getSettingsInventory().openInventory(player);
    }

    @SuppressWarnings("deprecation")
    private void toggleHidePlayers(Player player, Settings settings) {
        if (settings.isHidePlayers()) {
            Bukkit.getOnlinePlayers().forEach(player::hidePlayer);
        } else {
            Bukkit.getOnlinePlayers().forEach(player::showPlayer);
        }
    }
}