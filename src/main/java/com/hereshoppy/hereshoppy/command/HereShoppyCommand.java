package com.hereshoppy.hereshoppy.command;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.gui.ShopGUI;
import com.hereshoppy.hereshoppy.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HereShoppyCommand implements CommandExecutor, TabCompleter {

    private final HereShoppyPlugin plugin;

    public HereShoppyCommand(HereShoppyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        switch (args[0].toLowerCase()) {
            case "shop":
                if (!player.hasPermission("hereshoppy.use")) {
                    player.sendMessage("§cYou don't have permission.");
                    return true;
                }
                ShopGUI.openMainMenu(player);
                return true;

            case "info":
                if (!player.hasPermission("hereshoppy.use")) {
                    player.sendMessage("§cYou don't have permission.");
                    return true;
                }
                showInfo(player);
                return true;

            case "reload":
                if (!player.hasPermission("hereshoppy.admin")) {
                    player.sendMessage("§cYou don't have permission.");
                    return true;
                }
                plugin.getItemManager().loadItems();
                player.sendMessage("§aHereShoppy configuration reloaded!");
                return true;
        }

        return false;
    }

    private void showInfo(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        player.sendMessage("§8§m---------------------------------------");
        player.sendMessage("§6§lHERESHOPPY PLAYER INFO");
        player.sendMessage("§7Kroin Balance: §e" + String.format("%.2f", data.getBalance()));
        player.sendMessage("§7Shop Level: §b" + data.getShopLevel());
        player.sendMessage("§7Lifetime Earned: §a" + String.format("%.2f", data.getLifetimeEarned()));
        player.sendMessage("§7Shop XP: §d" + data.getShopXp());
        
        player.sendMessage("");
        player.sendMessage("§6§lLEADERBOARD (Top 5 Lifetime Earned)");
        List<PlayerData> topPlayers = plugin.getDataManager().getAllPlayerData().values().stream()
                .sorted(Comparator.comparingDouble(PlayerData::getLifetimeEarned).reversed())
                .limit(5)
                .collect(Collectors.toList());

        int rank = 1;
        for (PlayerData topData : topPlayers) {
            String name = plugin.getServer().getOfflinePlayer(topData.getUuid()).getName();
            player.sendMessage("§e" + rank + ". §f" + (name != null ? name : "Unknown") + " §7- §a" + String.format("%.2f", topData.getLifetimeEarned()));
            rank++;
        }
        player.sendMessage("§8§m---------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("shop");
            options.add("info");
            if (sender.hasPermission("hereshoppy.admin")) {
                options.add("reload");
            }
            return options.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
