/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.listener;

import com.cryptomorin.xseries.XSound;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.navigator.ArmorStandManager;
import com.eintosti.buildsystem.player.CachedValues;
import com.eintosti.buildsystem.player.PlayerManager;
import com.eintosti.buildsystem.settings.SettingsManager;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import com.eintosti.buildsystem.world.data.WorldStatus;
import com.eintosti.buildsystem.world.data.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayerChangedWorldListener implements Listener {

    private final ConfigValues configValues;

    private final ArmorStandManager armorStandManager;
    private final InventoryUtil inventoryUtil;
    private final PlayerManager playerManager;
    private final SettingsManager settingsManager;
    private final WorldManager worldManager;

    private final Map<UUID, GameMode> playerGamemode;
    private final Map<UUID, ItemStack[]> playerInventory;
    private final Map<UUID, ItemStack[]> playerArmor;

    public PlayerChangedWorldListener(BuildSystem plugin) {
        this.configValues = plugin.getConfigValues();

        this.armorStandManager = plugin.getArmorStandManager();
        this.inventoryUtil = plugin.getInventoryUtil();
        this.playerManager = plugin.getPlayerManager();
        this.settingsManager = plugin.getSettingsManager();
        this.worldManager = plugin.getWorldManager();

        this.playerGamemode = new HashMap<>();
        this.playerInventory = new HashMap<>();
        this.playerArmor = new HashMap<>();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        BuildWorld oldWorld = worldManager.getBuildWorld(event.getFrom().getName());
        if (oldWorld != null && configValues.isUnloadWorlds()) {
            oldWorld.resetUnloadTask();
        }

        BuildWorld newWorld = worldManager.getBuildWorld(worldName);
        if (newWorld != null) {
            if (!newWorld.isPhysics()) {
                if (player.hasPermission("buildsystem.physics.message")) {
                    Messages.sendMessage(player, "physics_deactivated_in_world", new AbstractMap.SimpleEntry<>("%world%", newWorld.getName()));
                }
            }
        }

        removeOldNavigator(player);
        removeBuildMode(player);
        setGoldBlock(newWorld);
        checkWorldStatus(player);

        if (settingsManager.getSettings(player).isScoreboard()) {
            playerManager.forceUpdateSidebar(player);
        }
    }

    private void removeOldNavigator(Player player) {
        armorStandManager.removeArmorStands(player);
        if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void removeBuildMode(Player player) {
        UUID playerUuid = player.getUniqueId();
        if (!playerManager.getBuildModePlayers().remove(playerUuid)) {
            return;
        }

        CachedValues cachedValues = playerManager.getBuildPlayer(playerUuid).getCachedValues();
        cachedValues.resetGameModeIfPresent(player);
        cachedValues.resetInventoryIfPresent(player);
        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player);
        Messages.sendMessage(player, "build_deactivated_self");
    }

    private void setGoldBlock(BuildWorld buildWorld) {
        if (buildWorld == null || buildWorld.getType() != WorldType.VOID || buildWorld.getStatus() != WorldStatus.NOT_STARTED) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        if (configValues.isVoidBlock()) {
            bukkitWorld.getBlockAt(0, 64, 0).setType(Material.GOLD_BLOCK);
        }
    }

    @SuppressWarnings("deprecation")
    private void checkWorldStatus(Player player) {
        String worldName = player.getWorld().getName();
        BuildWorld buildWorld = worldManager.getBuildWorld(worldName);
        if (buildWorld == null) {
            return;
        }

        UUID playerUUID = player.getUniqueId();
        PlayerInventory playerInventory = player.getInventory();

        if (this.playerGamemode.containsKey(playerUUID)) {
            player.setGameMode(this.playerGamemode.get(playerUUID));
            this.playerGamemode.remove(playerUUID);
        }

        if (this.playerInventory.containsKey(playerUUID)) {
            playerInventory.clear();
            playerInventory.setContents(this.playerInventory.get(playerUUID));
            this.playerInventory.remove(playerUUID);
        }

        if (this.playerArmor.containsKey(playerUUID)) {
            removeArmorContent(player);
            playerInventory.setArmorContents(this.playerArmor.get(playerUUID));
            this.playerArmor.remove(playerUUID);
        }

        if (buildWorld.getStatus() == WorldStatus.ARCHIVE) {
            this.playerGamemode.put(playerUUID, player.getGameMode());
            this.playerInventory.put(playerUUID, playerInventory.getContents());
            this.playerArmor.put(playerUUID, playerInventory.getArmorContents());

            removeArmorContent(player);
            playerInventory.clear();
            playerInventory.setItem(8, inventoryUtil.getItemStack(configValues.getNavigatorItem(), Messages.getString("navigator_item")));
            setSpectatorMode(player);

            if (configValues.isArchiveVanish()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false), false);
                Bukkit.getOnlinePlayers().forEach(pl -> pl.hidePlayer(player));
            }
        } else {
            playerInventory.setItem(8, inventoryUtil.getItemStack(configValues.getNavigatorItem(), Messages.getString("navigator_item")));
            if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            Bukkit.getOnlinePlayers().forEach(pl -> pl.showPlayer(player));
        }
    }

    private void setSpectatorMode(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setSaturation(20);
        player.setHealth(20);
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private void removeArmorContent(Player player) {
        PlayerInventory playerInventory = player.getInventory();
        playerInventory.setHelmet(null);
        playerInventory.setChestplate(null);
        playerInventory.setLeggings(null);
        playerInventory.setBoots(null);
    }
}