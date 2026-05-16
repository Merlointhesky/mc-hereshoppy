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
    private FileConfiguration playerConfig;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    public DataManager(HereShoppyPlugin plugin) {
        this.plugin = plugin;
        this.playerFile = new File(plugin.getDataFolder(), "data/players.yml");
        loadPlayers();
    }

    public void loadPlayers() {
        if (!playerFile.exists()) {
            plugin.saveResource("data/players.yml", false);
        }
        playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        
        ConfigurationSection section = playerConfig.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerData data = new PlayerData(uuid);
                data.setBalance(section.getDouble(key + ".balance", 0));
                data.setShopXp(section.getInt(key + ".shop_xp", 0));
                data.setShopLevel(section.getInt(key + ".shop_level", 0));
                data.setLifetimeEarned(section.getDouble(key + ".lifetime_earned", 0));
                playerDataMap.put(uuid, data);
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

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerDataMap;
    }
}
