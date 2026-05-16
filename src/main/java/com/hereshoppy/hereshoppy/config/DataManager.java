package com.hereshoppy.hereshoppy.config;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.model.PlayerData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final HereShoppyPlugin plugin;
    private final File playerFile;
    private final File binFile;
    private FileConfiguration playerConfig;
    private FileConfiguration binConfig;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Map<org.bukkit.Location, UUID> physicalBins = new HashMap<>();

    public DataManager(HereShoppyPlugin plugin) {
        this.plugin = plugin;
        this.playerFile = new File(plugin.getDataFolder(), "data/players.yml");
        this.binFile = new File(plugin.getDataFolder(), "data/bins.yml");
        loadPlayers();
        loadBins();
    }

    public void loadPlayers() {
        if (!playerFile.exists()) {
            File dataDir = new File(plugin.getDataFolder(), "data");
            if (!dataDir.exists()) dataDir.mkdirs();
            try {
                playerFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        
        ConfigurationSection section = playerConfig.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerData data = new PlayerData(uuid);
                data.setBalance(playerConfig.getDouble("players." + key + ".balance", 0));
                data.setShopXp(playerConfig.getInt("players." + key + ".shop_xp", 0));
                data.setShopLevel(playerConfig.getInt("players." + key + ".shop_level", 0));
                data.setLifetimeEarned(playerConfig.getDouble("players." + key + ".lifetime_earned", 0));
                playerDataMap.put(uuid, data);
            }
        }
    }

    public void loadBins() {
        if (!binFile.exists()) {
            try {
                binFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        binConfig = YamlConfiguration.loadConfiguration(binFile);
        physicalBins.clear();
        ConfigurationSection section = binConfig.getConfigurationSection("bins");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String[] parts = key.split(",");
                if (parts.length == 4) {
                    org.bukkit.World world = org.bukkit.Bukkit.getWorld(parts[0]);
                    if (world != null) {
                        org.bukkit.Location loc = new org.bukkit.Location(world, 
                            Double.parseDouble(parts[1]), 
                            Double.parseDouble(parts[2]), 
                            Double.parseDouble(parts[3]));
                        UUID owner = UUID.fromString(binConfig.getString("bins." + key));
                        physicalBins.put(loc, owner);
                    }
                }
            }
        }
    }

    public void savePlayers() {
        for (PlayerData data : playerDataMap.values()) {
            String path = "players." + data.getUuid().toString();
            playerConfig.set(path + ".balance", data.getBalance());
            playerConfig.set(path + ".shop_xp", data.getShopXp());
            playerConfig.set(path + ".shop_level", data.getShopLevel());
            playerConfig.set(path + ".lifetime_earned", data.getLifetimeEarned());
        }
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml!");
            e.printStackTrace();
        }
    }

    public void saveBins() {
        binConfig.set("bins", null);
        for (Map.Entry<org.bukkit.Location, UUID> entry : physicalBins.entrySet()) {
            org.bukkit.Location loc = entry.getKey();
            String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            binConfig.set("bins." + key, entry.getValue().toString());
        }
        try {
            binConfig.save(binFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save bins.yml!");
            e.printStackTrace();
        }
    }

    public void addPhysicalBin(org.bukkit.Location loc, UUID owner) {
        physicalBins.put(loc, owner);
        saveBins();
    }

    public void removePhysicalBin(org.bukkit.Location loc) {
        physicalBins.remove(loc);
        saveBins();
    }

    public Map<org.bukkit.Location, UUID> getPhysicalBins() {
        return physicalBins;
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerDataMap;
    }
}
