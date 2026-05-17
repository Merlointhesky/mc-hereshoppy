package com.hereshoppy.hereshoppy.shop;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ItemManager {
    private final HereShoppyPlugin plugin;
    private final Map<Material, ShopItem> items = new HashMap<>();
    private final Map<String, List<Material>> categories = new HashMap<>();

    public ItemManager(HereShoppyPlugin plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        items.clear();
        categories.clear();
        File salesDir = new File(plugin.getDataFolder(), "sales");
        if (!salesDir.exists()) {
            salesDir.mkdirs();
            // Create default files if needed, but for now we expect them or will create empty ones
            createDefaultSalesFiles(salesDir);
        }

        File[] files = salesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String categoryName = file.getName().replace(".yml", "");
                List<Material> categoryItems = new ArrayList<>();
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    ConfigurationSection mainSection = config.getConfigurationSection(key);
                    if (mainSection != null) {
                        // Check if it's a direct item or a category of materials
                        if (mainSection.contains("material")) {
                            // Direct item
                            loadItem(mainSection, key, categoryItems, file.getName());
                        } else {
                            // Nested materials
                            for (String subKey : mainSection.getKeys(false)) {
                                ConfigurationSection subSection = mainSection.getConfigurationSection(subKey);
                                if (subSection != null) {
                                    loadItem(subSection, subKey, categoryItems, file.getName());
                                }
                            }
                        }
                    }
                }
                categories.put(categoryName, categoryItems);
            }
        }
    }

    private void loadItem(ConfigurationSection section, String key, List<Material> categoryItems, String fileName) {
        try {
            Material material = Material.valueOf(section.getString("material", key).toUpperCase());
            int requiredLevel = section.getInt("required_shop_level", 0);
            
            // Calculate buy price: Level 0 -> 1, Level N -> N
            double buyPrice = (requiredLevel == 0) ? 1 : requiredLevel;
            
            ShopItem item = new ShopItem(material, buyPrice, requiredLevel);
            items.put(material, item);
            categoryItems.add(material);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in " + fileName + ": " + key);
        }
    }

    private void createDefaultSalesFiles(File salesDir) {
        String[] defaultFiles = {
            "weapons_and_shields.yml", "armor.yml", "tools_and_elytra.yml",
            "fruit_and_veg.yml", "meat_and_fish.yml", "wooden_items.yml",
            "metal_items.yml", "ores_and_minerals.yml", "seeds_and_saplings.yml",
            "potions.yml", "building_blocks.yml", "redstone.yml",
            "mob_drops.yml", "other_items.yml"
        };
        for (String fileName : defaultFiles) {
            File file = new File(salesDir, fileName);
            if (!file.exists()) {
                try {
                    plugin.saveResource("sales/" + fileName, false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save " + fileName + " from resources, creating empty file.");
                    try {
                        file.createNewFile();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public double calculateBuyPrice(ItemStack itemStack) {
        ShopItem shopItem = items.get(itemStack.getType());
        if (shopItem == null) return -1;

        double basePrice = shopItem.getBuyPrice();
        double enchantBonus = 0;
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantBonus += 0.1 * entry.getValue();
            }
        }
        
        return basePrice * (1 + enchantBonus);
    }

    public double calculateSellPrice(ItemStack itemStack) {
        if (itemStack.getAmount() < itemStack.getMaxStackSize()) {
            return 0;
        }
        
        // Base sale is 1 Kroin per full stack
        double basePrice = 1.0;
        double enchantBonus = 0;
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantBonus += 0.1 * entry.getValue();
            }
        }
        
        return basePrice * (1 + enchantBonus);
    }

    public ShopItem getShopItem(Material material) {
        return items.get(material);
    }

    public Map<String, List<Material>> getCategories() {
        return categories;
    }

    public static class ShopItem {
        private final Material material;
        private final double buyPrice;
        private final int requiredLevel;

        public ShopItem(Material material, double buyPrice, int requiredLevel) {
            this.material = material;
            this.buyPrice = buyPrice;
            this.requiredLevel = requiredLevel;
        }

        public Material getMaterial() {
            return material;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }
    }
}
