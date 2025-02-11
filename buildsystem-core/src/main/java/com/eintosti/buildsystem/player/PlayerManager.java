/*
 * Copyright (c) 2022, Thomas Meaney
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.eintosti.buildsystem.player;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.eintosti.buildsystem.BuildSystem;
import com.eintosti.buildsystem.Messages;
import com.eintosti.buildsystem.config.ConfigValues;
import com.eintosti.buildsystem.config.PlayersConfig;
import com.eintosti.buildsystem.navigator.NavigatorInventoryType;
import com.eintosti.buildsystem.navigator.NavigatorType;
import com.eintosti.buildsystem.navigator.WorldSort;
import com.eintosti.buildsystem.navigator.world.FilteredWorldsInventory.Visibility;
import com.eintosti.buildsystem.settings.DesignColor;
import com.eintosti.buildsystem.settings.Settings;
import com.eintosti.buildsystem.settings.SettingsManager;
import com.eintosti.buildsystem.util.InventoryUtil;
import com.eintosti.buildsystem.world.BuildWorld;
import com.eintosti.buildsystem.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author einTosti
 */
public class PlayerManager {

    private static final double MIN_HEIGHT = -0.16453003708696978;
    private static final double MAX_HEIGHT = 0.16481381407766063;

    private final BuildSystem plugin;
    private final PlayersConfig playersConfig;
    private final ConfigValues configValues;

    private final Map<UUID, BuildPlayer> buildPlayers;

    private final Set<Player> openNavigator;
    private final Set<UUID> buildModePlayers;

    public PlayerManager(BuildSystem plugin) {
        this.plugin = plugin;
        this.playersConfig = new PlayersConfig(plugin);
        this.configValues = plugin.getConfigValues();

        this.buildPlayers = new HashMap<>();

        this.openNavigator = new HashSet<>();
        this.buildModePlayers = new HashSet<>();

        initEntityChecker();
    }

    public BuildPlayer createBuildPlayer(UUID uuid, Settings settings) {
        BuildPlayer buildPlayer = this.buildPlayers.getOrDefault(uuid, new BuildPlayer(uuid, settings));
        this.buildPlayers.put(uuid, buildPlayer);
        return buildPlayer;
    }

    public BuildPlayer createBuildPlayer(Player player) {
        return createBuildPlayer(player.getUniqueId(), new Settings());
    }

    public Collection<BuildPlayer> getBuildPlayers() {
        return this.buildPlayers.values();
    }

    public BuildPlayer getBuildPlayer(UUID uuid) {
        return this.buildPlayers.get(uuid);
    }

    public BuildPlayer getBuildPlayer(Player player) {
        return this.buildPlayers.get(player.getUniqueId());
    }

    @Nullable
    public String getSelectedWorldName(Player player) {
        BuildWorld selectedWorld = getBuildPlayer(player.getUniqueId()).getCachedWorld();
        if (selectedWorld == null) {
            return null;
        }

        String selectedWorldName = selectedWorld.getName();
        if (selectedWorldName.length() > 17) {
            selectedWorldName = selectedWorldName.substring(0, 14) + "...";
        }
        return selectedWorldName;
    }

    public Set<Player> getOpenNavigator() {
        return openNavigator;
    }

    public Set<UUID> getBuildModePlayers() {
        return buildModePlayers;
    }

    public boolean isInBuildMode(Player player) {
        return buildModePlayers.contains(player.getUniqueId());
    }

    /**
     * Gets whether the given player is allowed to create a new {@link BuildWorld}.<br>
     * This depends on the following factors:
     * <ul>
     *  <li>Is the maximum amount of worlds set by the config less than the amount of existing worlds?</li>
     *  <li>Is the maximum amount of worlds created by the player less than the amount of worlds said player is allowed to create?</li>
     * <ul>
     *
     * @param player     The player trying to create a world
     * @param visibility The visibility of the world trying to be created
     * @return {@code true} if the player is allowed to create a world, otherwise {@code false}
     */
    public boolean canCreateWorld(Player player, Visibility visibility) {
        boolean showPrivateWorlds = visibility == Visibility.PRIVATE;
        WorldManager worldManager = plugin.getWorldManager();

        int maxWorldAmountConfig = configValues.getMaxWorldAmount(showPrivateWorlds);
        if (maxWorldAmountConfig >= 0 && worldManager.getBuildWorlds().size() >= maxWorldAmountConfig) {
            return false;
        }

        int maxWorldAmountPlayer = getMaxWorlds(player, showPrivateWorlds);
        return maxWorldAmountPlayer < 0 || worldManager.getBuildWorldsCreatedByPlayer(player, visibility).size() < maxWorldAmountPlayer;
    }

    /**
     * Returns the maximum amount of {@link BuildWorld}s a player can create.<br>
     * If the player has the permission {@code buildsystem.admin}</li>, unlimited worlds can be created.<br>
     * Otherwise, there are two different permissions to set said amount:<br>
     * To set the maximum of...
     * <ul>
     *  <li>...public worlds, use {@literal buildsystem.create.public.<amount>}</li>
     *  <li>...private worlds, use {@literal buildsystem.create.private.<amount>}</li>
     * <ul>
     *
     * @param player The player object
     * @return If set, the maximum amount of worlds a player can create, otherwise -1
     */
    public int getMaxWorlds(Player player, boolean privateWorld) {
        int max = -1;
        if (player.hasPermission(BuildSystem.ADMIN_PERMISSION)) {
            return max;
        }

        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            String permissionString = permission.getPermission();
            String[] splitPermission = permissionString.split("\\.");

            if (splitPermission.length != 4) {
                continue;
            }

            if (!splitPermission[1].equalsIgnoreCase("create")) {
                continue;
            }

            String worldVisibility = privateWorld ? "private" : "public";
            if (!splitPermission[2].equalsIgnoreCase(worldVisibility)) {
                continue;
            }

            String amountString = splitPermission[3];
            if (amountString.equals("*")) {
                return -1;
            }

            try {
                int amount = Integer.parseInt(amountString);
                if (amount > max) {
                    max = amount;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return max;
    }

    public void forceUpdateSidebar(BuildWorld buildWorld) {
        if (!configValues.isScoreboard()) {
            return;
        }

        World bukkitWorld = Bukkit.getWorld(buildWorld.getName());
        if (bukkitWorld == null) {
            return;
        }

        bukkitWorld.getPlayers().forEach(this::forceUpdateSidebar);
    }

    public void forceUpdateSidebar(Player player) {
        SettingsManager settingsManager = plugin.getSettingsManager();
        if (!configValues.isScoreboard() || !settingsManager.getSettings(player).isScoreboard()) {
            return;
        }
        settingsManager.updateScoreboard(player);
    }

    public void closeNavigator(Player player) {
        if (!openNavigator.contains(player)) {
            return;
        }

        BuildPlayer buildPlayer = getBuildPlayer(player.getUniqueId());
        buildPlayer.setLastLookedAt(null);
        plugin.getArmorStandManager().removeArmorStands(player);

        XSound.ENTITY_ITEM_BREAK.play(player);
        ActionBar.clearActionBar(player);
        replaceBarrier(player);

        CachedValues cachedValues = buildPlayer.getCachedValues();
        cachedValues.resetWalkSpeedIfPresent(player);
        cachedValues.resetFlySpeedIfPresent(player);
        player.removePotionEffect(PotionEffectType.JUMP);
        player.removePotionEffect(PotionEffectType.BLINDNESS);

        openNavigator.remove(player);
    }

    private void replaceBarrier(Player player) {
        if (!player.hasPermission("buildsystem.navigator.item")) {
            return;
        }

        InventoryUtil inventoryUtil = plugin.getInventoryUtil();
        String findItemName = Messages.getString("barrier_item");
        ItemStack replaceItem = inventoryUtil.getItemStack(plugin.getConfigValues().getNavigatorItem(), Messages.getString("navigator_item"));

        inventoryUtil.replaceItem(player, findItemName, XMaterial.BARRIER, replaceItem);
    }

    private void initEntityChecker() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkForEntity, 0L, 1L);
    }

    private void checkForEntity() {
        for (Player player : openNavigator) {
            if (getEntityName(player).isEmpty()) {
                continue;
            }

            BuildPlayer buildPlayer = getBuildPlayer(player.getUniqueId());
            double lookedPosition = player.getEyeLocation().getDirection().getY();
            if (lookedPosition >= MIN_HEIGHT && lookedPosition <= MAX_HEIGHT) {
                NavigatorInventoryType inventoryType = NavigatorInventoryType.matchInventoryType(player, getEntityName(player));
                NavigatorInventoryType lastLookedAt = buildPlayer.getLastLookedAt();

                if (lastLookedAt == null || lastLookedAt != inventoryType) {
                    buildPlayer.setLastLookedAt(inventoryType);
                    sendTypeInfo(player, inventoryType);
                }
            } else {
                ActionBar.clearActionBar(player);
                buildPlayer.setLastLookedAt(null);
            }
        }
    }

    @Nullable
    private <T extends Entity> T getTarget(Entity entity, Iterable<T> entities) {
        if (entity == null) {
            return null;
        }

        T target = null;
        final Location entityLocation = entity.getLocation();
        final double threshold = 0.5;

        for (T other : entities) {
            final Location otherLocation = other.getLocation();
            final Vector vector = otherLocation.toVector().subtract(entityLocation.toVector());

            if (entityLocation.getDirection().normalize().crossProduct(vector).lengthSquared() < threshold && vector.normalize().dot(entityLocation.getDirection().normalize()) >= 0) {
                if (target == null || target.getLocation().distanceSquared(entityLocation) > otherLocation.distanceSquared(entityLocation)) {
                    target = other;
                }
            }
        }

        return target;
    }

    @Nullable
    private Entity getTargetEntity(Entity entity) {
        return getTarget(entity, entity.getNearbyEntities(3, 3, 3));
    }

    @NotNull
    private String getEntityName(Player player) {
        Entity targetEntity = getTargetEntity(player);
        if (targetEntity == null || targetEntity.getType() != EntityType.ARMOR_STAND) {
            return "";
        }

        Entity entity = getTargetEntity(player);
        if (entity == null || entity.getCustomName() == null) {
            return "";
        }

        return entity.getCustomName();
    }

    private void sendTypeInfo(Player player, NavigatorInventoryType inventoryType) {
        if (inventoryType == null) {
            ActionBar.clearActionBar(player);
            return;
        }

        String message;
        switch (inventoryType) {
            case ARCHIVE:
                message = "new_navigator_world_archive";
                break;
            case PRIVATE:
                message = "new_navigator_private_worlds";
                break;
            default:
                message = "new_navigator_world_navigator";
                break;
        }

        ActionBar.sendActionBar(player, Messages.getString(message));
        XSound.ENTITY_CHICKEN_EGG.play(player);
    }

    public void save() {
        getBuildPlayers().forEach(buildPlayer -> playersConfig.savePlayer(buildPlayer.getUniqueId(), buildPlayer));
    }

    public void load() {
        FileConfiguration configuration = playersConfig.getFile();
        ConfigurationSection configurationSection = configuration.getConfigurationSection("players");
        if (configurationSection == null) {
            return;
        }

        Set<String> uuids = configurationSection.getKeys(false);
        uuids.forEach(uuid -> {
            BuildPlayer buildPlayer = createBuildPlayer(
                    UUID.fromString(uuid),
                    loadSettings(configuration, "players." + uuid + ".settings.")
            );
            buildPlayer.setLogoutLocation(loadLogoutLocation(configuration, "players." + uuid + ".logout-location"));
        });
    }

    private Settings loadSettings(FileConfiguration configuration, String pathPrefix) {
        NavigatorType navigatorType = NavigatorType.valueOf(configuration.getString(pathPrefix + "type"));
        DesignColor glassColor = DesignColor.matchColor(configuration.getString(pathPrefix + "glass"));
        WorldSort worldSort = WorldSort.matchWorldSort(configuration.getString(pathPrefix + "world-sort"));
        boolean clearInventory = configuration.getBoolean(pathPrefix + "clear-inventory", false);
        boolean disableInteract = configuration.getBoolean(pathPrefix + "disable-interact", false);
        boolean hidePlayers = configuration.getBoolean(pathPrefix + "hide-players", false);
        boolean instantPlaceSigns = configuration.getBoolean(pathPrefix + "instant-place-signs", false);
        boolean keepNavigator = configuration.getBoolean(pathPrefix + "keep-navigator", false);
        boolean nightVision = configuration.getBoolean(pathPrefix + "nightvision", false);
        boolean noClip = configuration.getBoolean(pathPrefix + "no-clip", false);
        boolean placePlants = configuration.getBoolean(pathPrefix + "place-plants", false);
        boolean scoreboard = configuration.getBoolean(pathPrefix + "scoreboard", true);
        boolean slabBreaking = configuration.getBoolean(pathPrefix + "slab-breaking", false);
        boolean spawnTeleport = configuration.getBoolean(pathPrefix + "spawn-teleport", true);
        boolean trapDoor = configuration.getBoolean(pathPrefix + "trapdoor", false);

        return new Settings(
                navigatorType, glassColor, worldSort, clearInventory, disableInteract, hidePlayers, instantPlaceSigns,
                keepNavigator, nightVision, noClip, placePlants, scoreboard, slabBreaking, spawnTeleport, trapDoor
        );
    }

    @Nullable
    private LogoutLocation loadLogoutLocation(FileConfiguration configuration, String pathPrefix) {
        String location = configuration.getString(pathPrefix);
        if (location == null || location.trim().equals("")) {
            return null;
        }

        String[] parts = location.split(":");
        if (parts.length != 6) {
            return null;
        }

        String worldName = parts[0];
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);

        return new LogoutLocation(worldName, x, y, z, yaw, pitch);
    }
}