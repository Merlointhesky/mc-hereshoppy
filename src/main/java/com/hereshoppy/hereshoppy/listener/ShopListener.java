package com.hereshoppy.hereshoppy.listener;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.api.HereshoppyAPI;
import com.hereshoppy.hereshoppy.gui.ShopGUI;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ShopListener implements Listener {

    private final Map<UUID, Boolean> searchExpectant = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory inv = event.getInventory();
        if (inv.getHolder() == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (inv.getHolder() instanceof ShopGUI.MainMenuHolder) {
            event.setCancelled(true);
            if (clicked.getType() == Material.ANVIL) {
                player.closeInventory();
                player.sendMessage(Component.text("Please type your search query in chat:", NamedTextColor.YELLOW));
                searchExpectant.put(player.getUniqueId(), true);
                return;
            }
            
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String displayName = plainText(meta.displayName());
                // This is a bit hacky, but let's find the category from the name
                String category = findCategoryByName(clicked);
                if (category != null) {
                    ShopGUI.openCategoryMenu(player, category, 0);
                }
            }
        } else if (inv.getHolder() instanceof ShopGUI.CategoryMenuHolder holder) {
            event.setCancelled(true);
            handleCategoryClick(player, clicked, holder, event.getSlot());
        } else if (inv.getHolder() instanceof ShopGUI.SearchMenuHolder holder) {
            event.setCancelled(true);
            handleSearchClick(player, clicked, holder, event.getSlot());
        }
    }

    private String findCategoryByName(ItemStack item) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        Map<String, org.bukkit.inventory.ItemStack> categoryIcons = new HashMap<>();
        // In a real plugin, we'd store the mapping or use PersistentDataContainer
        // For now, let's compare display names or material
        for (String cat : plugin.getItemManager().getCategories().keySet()) {
            if (getCategoryMaterial(cat) == item.getType()) return cat;
        }
        return null;
    }

    private Material getCategoryMaterial(String category) {
        return switch (category) {
            case "weapons_and_shields" -> Material.DIAMOND_SWORD;
            case "armor" -> Material.DIAMOND_CHESTPLATE;
            case "tools_and_elytra" -> Material.NETHERITE_PICKAXE;
            case "fruit_and_veg" -> Material.APPLE;
            case "meat_and_fish" -> Material.COOKED_BEEF;
            case "wooden_items" -> Material.OAK_LOG;
            case "metal_items" -> Material.IRON_INGOT;
            case "ores_and_minerals" -> Material.DIAMOND;
            case "seeds_and_saplings" -> Material.OAK_SAPLING;
            case "potions" -> Material.POTION;
            case "building_blocks" -> Material.BRICKS;
            case "redstone" -> Material.REDSTONE;
            case "mob_drops" -> Material.ROTTEN_FLESH;
            default -> Material.CHEST;
        };
    }

    private void handleCategoryClick(Player player, ItemStack clicked, ShopGUI.CategoryMenuHolder holder, int slot) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        
        if (slot >= 45) { // Navigation or Preview
            if (clicked.getType() == Material.ARROW) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta.displayName() != null) {
                    String name = plainText(meta.displayName());
                    if (name.contains("Next")) {
                        ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage() + 1);
                    } else if (name.contains("Previous")) {
                        ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage() - 1);
                    }
                }
            } else if (clicked.getType() == Material.BARRIER) {
                ShopGUI.openMainMenu(player);
            } else if (clicked.getType() == Material.SUNFLOWER && ShopGUI.isRandomizableCategory(holder.getCategory())) {
                player.sendMessage(Component.text("Stock randomised!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage());
            }
            return;
        }

        // Purchase logic
        purchaseItem(player, clicked);
    }

    private void handleSearchClick(Player player, ItemStack clicked, ShopGUI.SearchMenuHolder holder, int slot) {
        if (slot == 48 && clicked.getType() == Material.BARRIER) {
            ShopGUI.openMainMenu(player);
            return;
        }
        if (slot < 45) {
            purchaseItem(player, clicked);
        }
    }

    private void purchaseItem(Player player, ItemStack clicked) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(clicked.getType());
        if (shopItem == null) return;

        double price = plugin.getItemManager().calculateBuyPrice(clicked);
        double balance = HereshoppyAPI.getKroins(player.getUniqueId());

        if (balance < price) {
            player.sendMessage(Component.text("You don't have enough Kroins!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
            return;
        }

        HereshoppyAPI.removeKroins(player.getUniqueId(), price);
        HereshoppyAPI.addShopXp(player.getUniqueId(), (int) price);
        
        ItemStack toGive = clicked.clone();
        ItemMeta meta = toGive.getItemMeta();
        // Remove the shop lore
        meta.lore(null);
        toGive.setItemMeta(meta);
        
        player.getInventory().addItem(toGive).values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        
        player.sendMessage(Component.text("Purchased " + clicked.getAmount() + "x " + formatMaterialName(clicked.getType()) + " for " + String.format("%.2f", price) + " Kroins.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        
        // Refresh GUI to update balance/XP if shown (currently only in title or info)
    }

    private String plainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private String formatMaterialName(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (searchExpectant.getOrDefault(player.getUniqueId(), false)) {
            event.setCancelled(true);
            searchExpectant.remove(player.getUniqueId());
            String query = plainText(event.message());
            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, query));
        }
    }
}
