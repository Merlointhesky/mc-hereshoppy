package com.hereshoppy.hereshoppy.gui;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.model.PlayerData;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ShopGUI {

    public static void openMainMenu(Player player) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        Inventory inv = Bukkit.createInventory(new MainMenuHolder(), 27, Component.text("Shop Categories - Level " + data.getShopLevel()));
        
        Map<String, List<Material>> categories = plugin.getItemManager().getCategories();
        int slot = 0;
        
        // Add search item at the beginning
        ItemStack searchItem = new ItemStack(Material.ANVIL);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.displayName(Component.text("Search Items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        searchItem.setItemMeta(searchMeta);
        inv.setItem(26, searchItem); // Put search at the end

        List<String> sortedCategories = categories.keySet().stream().sorted().collect(Collectors.toList());
        
        for (String category : sortedCategories) {
            if (slot >= 26) break;
            
            Material icon = getCategoryIcon(category);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(formatCategoryName(category), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to view " + formatCategoryName(category), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }

    public static void openCategoryMenu(Player player, String category, int page) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getShopLevel();
        
        List<Material> allMaterials = plugin.getItemManager().getCategories().get(category);
        if (allMaterials == null) return;

        // Filter available items and preview items
        List<Material> available = new ArrayList<>();
        List<Material> preview = new ArrayList<>();
        
        for (Material mat : allMaterials) {
            ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(mat);
            if (shopItem == null) continue;
            
            if (shopItem.getRequiredLevel() <= currentLevel) {
                available.add(mat);
            } else if (shopItem.getRequiredLevel() == currentLevel + 1 && currentLevel < 100) {
                preview.add(mat);
            }
        }

        // If level >= 100, randomize available items if it's armor/weapons/tools
        if (currentLevel >= 100 && isRandomizableCategory(category)) {
            // In a real implementation we might want to store the "current stock" for the player
            // But for now we'll just show them. The "Randomise" button will refresh this.
        }

        Inventory inv = Bukkit.createInventory(new CategoryMenuHolder(category, page), 54, Component.text(formatCategoryName(category)));

        // Items are in slots 0-44 (5 rows)
        // Preview items are in slots 45-53 (bottom row)
        
        int start = page * 45;
        for (int i = 0; i < 45 && (start + i) < available.size(); i++) {
            Material mat = available.get(start + i);
            inv.setItem(i, createShopItem(player, mat, false));
        }

        // Bottom row preview
        if (currentLevel < 100) {
            for (int i = 0; i < 9 && i < preview.size(); i++) {
                inv.setItem(45 + i, createShopItem(player, preview.get(i), true));
            }
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createNavItem("Previous Page", Material.ARROW));
        }
        if (available.size() > (page + 1) * 45) {
            inv.setItem(53, createNavItem("Next Page", Material.ARROW));
        }
        
        // Randomise button for specific categories
        if (isRandomizableCategory(category)) {
            inv.setItem(49, createNavItem("Randomise Stock", Material.SUNFLOWER));
        }

        // Back button
        inv.setItem(48, createNavItem("Back to Categories", Material.BARRIER));

        player.openInventory(inv);
    }

    public static void openSearchMenu(Player player, String query) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getShopLevel();
        
        List<Material> matches = new ArrayList<>();
        for (Material mat : Material.values()) {
            ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(mat);
            if (shopItem == null) continue;
            
            if (mat.name().toLowerCase().contains(query.toLowerCase())) {
                if (shopItem.getRequiredLevel() <= currentLevel) {
                    matches.add(mat);
                }
            }
        }

        Inventory inv = Bukkit.createInventory(new SearchMenuHolder(query), 54, Component.text("Search: " + query));
        
        for (int i = 0; i < 45 && i < matches.size(); i++) {
            inv.setItem(i, createShopItem(player, matches.get(i), false));
        }
        
        inv.setItem(48, createNavItem("Back to Categories", Material.BARRIER));
        player.openInventory(inv);
    }

    private static ItemStack createShopItem(Player player, Material material, boolean isPreview) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(material);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        ItemStack item = new ItemStack(material);
        if (material.getMaxStackSize() > 1) {
            item.setAmount(material.getMaxStackSize());
        }
        
        // Add random enchants if it's a randomizable category
        if (!isPreview && isRandomizableCategory(findCategoryForMaterial(material))) {
            addRandomEnchantments(item, data.getShopLevel());
        }
        
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        
        if (isPreview) {
            lore.add(Component.text("LOCKED - Available at level " + shopItem.getRequiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            double price = plugin.getItemManager().calculateBuyPrice(item);
            lore.add(Component.text("Price: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.format("%.2f", price) + " Kroins", NamedTextColor.YELLOW)));
            lore.add(Component.text("Click to purchase stack", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String findCategoryForMaterial(Material material) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        for (Map.Entry<String, List<Material>> entry : plugin.getItemManager().getCategories().entrySet()) {
            if (entry.getValue().contains(material)) return entry.getKey();
        }
        return "other";
    }

    private static void addRandomEnchantments(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        Random random = new Random();
        
        // Only enchant if it's enchantable
        List<Enchantment> possible = Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(item))
                .collect(Collectors.toList());
        
        if (possible.isEmpty()) return;
        
        int numEnchants = random.nextInt(Math.min(3, level / 20 + 1)) + 1;
        for (int i = 0; i < numEnchants; i++) {
            Enchantment ench = possible.get(random.nextInt(possible.size()));
            int max = Math.min(ench.getMaxLevel(), level / 20 + 1);
            int enchLevel = random.nextInt(max) + 1;
            meta.addEnchant(ench, enchLevel, true);
        }
        item.setItemMeta(meta);
    }

    private static ItemStack createNavItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static String formatCategoryName(String key) {
        return Arrays.stream(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static Material getCategoryIcon(String category) {
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

    public static boolean isRandomizableCategory(String category) {
        return category.equals("weapons_and_shields") || category.equals("armor") || category.equals("tools_and_elytra");
    }

    public static class MainMenuHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public static class CategoryMenuHolder implements InventoryHolder {
        private final String category;
        private final int page;
        public CategoryMenuHolder(String category, int page) { this.category = category; this.page = page; }
        public String getCategory() { return category; }
        public int getPage() { return page; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public static class SearchMenuHolder implements InventoryHolder {
        private final String query;
        public SearchMenuHolder(String query) { this.query = query; }
        public String getQuery() { return query; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }
}
