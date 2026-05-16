package com.hereshoppy.hereshoppy.api;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.config.DataManager;
import com.hereshoppy.hereshoppy.model.PlayerData;
import com.hereshoppy.hereshoppy.util.LevelingMath;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HereshoppyAPI {

    private static DataManager getDataManager() {
        return HereShoppyPlugin.getInstance().getDataManager();
    }

    public static double getKroins(UUID uuid) {
        return getDataManager().getPlayerData(uuid).getBalance();
    }

    public static void addKroins(UUID uuid, double amount) {
        PlayerData data = getDataManager().getPlayerData(uuid);
        data.addBalance(amount);
        checkLevelUp(uuid, data);
    }

    public static void removeKroins(UUID uuid, double amount) {
        PlayerData data = getDataManager().getPlayerData(uuid);
        data.removeBalance(amount);
    }

    public static void addShopXp(UUID uuid, int amount) {
        PlayerData data = getDataManager().getPlayerData(uuid);
        data.addShopXp(amount);
        checkLevelUp(uuid, data);
    }

    private static void checkLevelUp(UUID uuid, PlayerData data) {
        int oldLevel = data.getShopLevel();
        int newLevel = LevelingMath.calculateLevel(data.getShopXp());
        if (newLevel > oldLevel) {
            data.setShopLevel(newLevel);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage("§a§lLEVEL UP! §7You are now shop level §e" + newLevel + "§7!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }
    
    public static int getLevel(UUID uuid) {
        return getDataManager().getPlayerData(uuid).getShopLevel();
    }
}
