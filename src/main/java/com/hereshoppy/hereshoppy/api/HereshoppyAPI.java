package com.hereshoppy.hereshoppy.api;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.config.DataManager;
import com.hereshoppy.hereshoppy.model.PlayerData;
import com.hereshoppy.hereshoppy.util.LevelingMath;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
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

                // Level 1000 Milestone Dragon Egg Reward Fanfare
                if (oldLevel < 1000 && newLevel >= 1000) {
                    // Create Legendary Dragon Egg
                    ItemStack egg = new ItemStack(org.bukkit.Material.DRAGON_EGG);
                    ItemMeta meta = egg.getItemMeta();
                    if (meta != null) {
                        meta.displayName(net.kyori.adventure.text.Component.text("Legendary Dragon Egg", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                        lore.add(net.kyori.adventure.text.Component.text("Awarded for reaching Shop Level 1000!", net.kyori.adventure.text.format.NamedTextColor.GOLD).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                        lore.add(net.kyori.adventure.text.Component.text("A true shopping masterpiece.", net.kyori.adventure.text.format.NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                        meta.lore(lore);
                        egg.setItemMeta(meta);
                    }

                    // Give to player
                    player.getInventory().addItem(egg).values().forEach(remaining -> 
                        player.getWorld().dropItemNaturally(player.getLocation(), remaining)
                    );

                    // Flashy Screen Announcement
                    player.sendTitle("§d§lLEVEL 1000!", "§aYou received a §5§lLegendary Dragon Egg§a!", 10, 100, 20);
                    player.sendMessage("§d§l★ MILESTONE! §eYou have reached Shop Level 1000 and earned the §5§lLegendary Dragon Egg§e!");

                    // Epic multi-note winner music fanfare
                    HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
                    
                    // Tick 0: Play G3 chime & rocket launch spark
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.5f); // G3
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
                    
                    // Tick 4: Play B3 chime
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.63f); // B3
                        }
                    }, 4L);
                    
                    // Tick 8: Play D4 chime
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.75f); // D4
                        }
                    }, 8L);
                    
                    // Tick 12: Play G4 chime
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f); // G4
                        }
                    }, 12L);
                    
                    // Tick 16: Climax - Levelup + Exploding Firework blast!
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.5f, 1.0f);
                        }
                    }, 16L);
                }
            }
        }
    }
    
    public static int getLevel(UUID uuid) {
        return getDataManager().getPlayerData(uuid).getShopLevel();
    }
}
